package com.zikra.app;

import android.app.AlertDialog;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.zikra.app.reading.CompassMath;
import com.zikra.app.reading.CompassReading;
import com.zikra.app.reading.QiblaCompassView;

public class QiblaActivity extends BaseActivity implements SensorEventListener {
  private float qiblaBearing;
  private com.zikra.app.location.LocationPoint location;
  private SensorManager sensorManager;
  private Sensor rotationSensor;
  private QiblaCompassView compass;
  private TextView sensorStatus;
  private final float[] rotation = new float[9];
  private final float[] remapped = new float[9];
  private final float[] orientation = new float[3];
  private float declination;
  private final CompassReading reading = new CompassReading();
  private final Handler clock = new Handler(Looper.getMainLooper());
  private boolean listening;
  private AlertDialog calibrationDialog;
  private final Runnable freshnessCheck = new Runnable() {
    @Override public void run() {
      if(!listening)return;
      renderReading(SystemClock.elapsedRealtime());
      clock.postDelayed(this,500);
    }
  };
  private boolean aligned;
  private long lastHaptic = -2000;
  private long lastSensorRender;
  private String lastStatusText = "";
  private int lastAnnouncementState = -1;
  private long lastAnnouncementAt;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_qibla);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.sharedFeatureHeader))
        .bind(
            R.id.toolbarBack,
            R.id.featureTitle,
            R.id.featureSubtitle,
            0,
            "Arah Kiblat",
            com.zikra.app.location.LocationStore.current(this).caption(),
            0,
            null);
    setupScreen("Arah Kiblat");
    getWindow().setStatusBarColor(getColor(R.color.ivory));
    getWindow().setNavigationBarColor(getColor(R.color.ivory));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    compass = findViewById(R.id.qiblaCompass);
    sensorStatus = findViewById(R.id.qiblaSensorStatus);
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    rotationSensor =
        sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    updateLocation();
    if (rotationSensor == null) showMissingSensor();
    findViewById(R.id.qiblaCalibrate)
        .setOnClickListener(
            view ->
                calibrationDialog = new AlertDialog.Builder(this)
                    .setTitle("Kalibrasi kompas")
                    .setMessage(
                        "Gerakkan ponsel membentuk angka 8 beberapa kali. Lepaskan casing bermagnet"
                            + " dan jauhi logam. Setelah itu pegang ponsel mendatar.\n\n"
                            + "Arah Kiblat: "
                            + Math.round(qiblaBearing)
                            + "° dari utara sejati untuk "
                            + location.label
                            + ".")
                    .setPositiveButton("Mengerti", null)
                    .show());
    findViewById(R.id.qiblaMaps).setOnClickListener(v -> open(NearbyMosquesActivity.class));
    findViewById(R.id.qiblaLocation).setOnClickListener(v -> open(LocationActivity.class));
    NavDock.attach(this, (FrameLayout) findViewById(R.id.qiblaRoot), "qibla");
  }

  private void updateLocation() {
    location = com.zikra.app.location.LocationStore.current(this);
    qiblaBearing = location.qiblaBearing();
    compass.setBearing(qiblaBearing);
    declination =
        new GeomagneticField(
                (float) location.latitude,
                (float) location.longitude,
                0f,
                System.currentTimeMillis())
            .getDeclination();
    ((TextView) findViewById(R.id.featureSubtitle)).setText(location.caption());
    ((TextView) findViewById(R.id.qiblaBearing)).setText(Math.round(qiblaBearing) + "°");
    ((TextView) findViewById(R.id.qiblaLocationLabel)).setText(location.label);
    ((TextView) findViewById(R.id.qiblaMethod))
        .setText(
            "Acuan "
                + Math.round(qiblaBearing)
                + "° dari utara sejati untuk "
                + location.label
                + ". Jauhkan ponsel dari logam dan magnet.");
    compass.setContentDescription(
        "Kompas Kiblat " + Math.round(qiblaBearing) + " derajat dari utara sejati");
  }

  private void showMissingSensor() {
    aligned=false;compass.setHeading(0,false);
    sensorStatus.setText(getString(R.string.qibla_no_sensor,Math.round(qiblaBearing)));
    sensorStatus.setTextColor(getColor(R.color.pine));
    lastStatusText="";
    compass.setContentDescription(getString(R.string.qibla_static_description,Math.round(qiblaBearing)));
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateLocation();
    if (rotationSensor == null) showMissingSensor();
    reading.reset();
    aligned = false;
    lastSensorRender = 0;
    lastStatusText = "";
    lastAnnouncementState = -1;
    compass.setHeading(0, false);
    listening=false;
    try { listening=rotationSensor!=null&&sensorManager.registerListener(this,rotationSensor,100_000); }
    catch(SecurityException unavailable){listening=false;}
    clock.removeCallbacks(freshnessCheck);
    if(listening){renderReading(SystemClock.elapsedRealtime());clock.postDelayed(freshnessCheck,500);}
    else showMissingSensor();
  }

  @Override
  protected void onPause() {
    listening=false;clock.removeCallbacks(freshnessCheck);reading.reset();
    aligned=false;compass.setHeading(0,false);
    if (sensorManager != null) sensorManager.unregisterListener(this);
    renderReading(SystemClock.elapsedRealtime());
    super.onPause();
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (!listening||event==null||event.sensor==null||event.sensor.getType()!=Sensor.TYPE_ROTATION_VECTOR) return;
    long now = SystemClock.elapsedRealtime();
    // Bound redraws to 10 Hz; stable readings must not keep the UI rendering.
    if (lastSensorRender>0 && now-lastSensorRender<100) return;
    lastSensorRender = now;
    if(!CompassReading.validVector(event.values)){
      reading.invalidate();renderReading(now);return;
    }
    SensorManager.getRotationMatrixFromVector(rotation, event.values);
    int axisX = SensorManager.AXIS_X;
    int axisY = SensorManager.AXIS_Y;
    int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
    if (displayRotation == Surface.ROTATION_90) {
      axisX = SensorManager.AXIS_Y;
      axisY = SensorManager.AXIS_MINUS_X;
    } else if (displayRotation == Surface.ROTATION_180) {
      axisX = SensorManager.AXIS_MINUS_X;
      axisY = SensorManager.AXIS_MINUS_Y;
    } else if (displayRotation == Surface.ROTATION_270) {
      axisX = SensorManager.AXIS_MINUS_Y;
      axisY = SensorManager.AXIS_X;
    }
    if(!SensorManager.remapCoordinateSystem(rotation,axisX,axisY,remapped)){
      reading.invalidate();renderReading(now);return;
    }
    SensorManager.getOrientation(remapped, orientation);
    float trueHeading = CompassMath.normalize((float) Math.toDegrees(orientation[0]) + declination);
    boolean flat=Float.isFinite(orientation[1])&&Float.isFinite(orientation[2])
        &&Math.abs(orientation[1])<=Math.PI/4&&Math.abs(orientation[2])<=Math.PI/4;
    reading.accept(trueHeading,qiblaBearing,event.accuracy>=SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM,
        flat,event.timestamp/1_000_000L,now);
    renderReading(now);
  }

  private void renderReading(long now) {
    CompassReading.Status state=reading.status(now);
    boolean live=state==CompassReading.Status.LIVE;
    float heading=live?reading.heading():0;
    float distance = Math.abs(CompassMath.delta(qiblaBearing, heading));
    boolean nextAligned=reading.aligned(now);
    if (nextAligned && !aligned && now - lastHaptic >= 2000) {
      Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
      if (vibrator != null && vibrator.hasVibrator())
        vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
      lastHaptic = now;
    }
    aligned = nextAligned;
    compass.setHeading(heading, aligned);
      String text;
      if(!live){
        int message=state==CompassReading.Status.STALE?R.string.qibla_stale
            :state==CompassReading.Status.INVALID?R.string.qibla_invalid
            :state==CompassReading.Status.TILTED?R.string.qibla_tilted
            :state==CompassReading.Status.LOW_ACCURACY?R.string.qibla_low_accuracy:R.string.qibla_waiting;
        text=getString(message);
      }
      else if (aligned) text=getString(R.string.qibla_aligned,Math.round(qiblaBearing));
      else
        text=getString(R.string.qibla_live,heading,distance,
            getString(CompassMath.delta(qiblaBearing,heading)>=0?R.string.qibla_right:R.string.qibla_left));
      if (!text.equals(lastStatusText)) {
        sensorStatus.setText(text);
        sensorStatus.setTextColor(getColor(aligned ? R.color.sage_active : R.color.pine));
        lastStatusText = text;
        compass.setContentDescription(live?getString(R.string.qibla_live_description,Math.round(qiblaBearing),Math.round(heading))
            :getString(R.string.qibla_static_description,Math.round(qiblaBearing)));
      }
      // Continuous headings remain readable but should not flood TalkBack.
      int announcement=live?(aligned?2:1):state.ordinal()+3;
      if (announcement!=lastAnnouncementState&&now-lastAnnouncementAt>=2500) {
        if(announcement!=1)sensorStatus.announceForAccessibility(text);
        lastAnnouncementState=announcement;
        lastAnnouncementAt = now;
      }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    if(listening&&sensor!=null&&sensor.getType()==Sensor.TYPE_ROTATION_VECTOR
        &&accuracy<SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM){
      reading.downgradeAccuracy();renderReading(SystemClock.elapsedRealtime());
    }
  }

  @Override protected void onDestroy(){
    clock.removeCallbacksAndMessages(null);
    if(calibrationDialog!=null)calibrationDialog.dismiss();
    super.onDestroy();
  }
}
