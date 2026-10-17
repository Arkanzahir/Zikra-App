package com.zikra.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.zikra.app.reading.CompassMath;
import com.zikra.app.reading.QiblaCompassView;
import java.util.Locale;

public class QiblaActivity extends BaseActivity implements SensorEventListener {
  private static final float QIBLA_BEARING = 294f;
  private SensorManager sensorManager;
  private Sensor rotationSensor;
  private QiblaCompassView compass;
  private TextView sensorStatus;
  private final float[] rotation = new float[9];
  private final float[] remapped = new float[9];
  private final float[] orientation = new float[3];
  private float declination;
  private float heading;
  private boolean receivedHeading;
  private boolean aligned;
  private long lastHaptic = -2000;
  private long lastStatus;
  private long lastSensorRender;
  private String lastStatusText = "";
  private int sensorAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_qibla);
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
    declination =
        new GeomagneticField(-7.28f, 112.79f, 10f, System.currentTimeMillis()).getDeclination();
    if (rotationSensor == null) showMissingSensor();
    findViewById(R.id.qiblaCalibrate)
        .setOnClickListener(
            view ->
                new AlertDialog.Builder(this)
                    .setTitle("Kalibrasi kompas")
                    .setMessage(
                        "Gerakkan ponsel membentuk angka 8 beberapa kali. Lepaskan casing bermagnet"
                            + " dan jauhi logam. Setelah itu pegang ponsel mendatar.\n\n"
                            + "Arah kiblat panduan Sukolilo: 294° dari utara sejati.")
                    .setPositiveButton("Mengerti", null)
                    .show());
    findViewById(R.id.qiblaMaps)
        .setOnClickListener(
            view -> {
              Uri maps =
                  Uri.parse(
                      "https://www.google.com/maps/dir/?api=1&destination=Masjid+Manarul+Ilmi+ITS+Surabaya&travelmode=walking");
              try {
                startActivity(new Intent(Intent.ACTION_VIEW, maps));
              } catch (ActivityNotFoundException exception) {
                toast("Aplikasi peta atau browser tidak tersedia");
              }
            });
    NavDock.attach(this, (FrameLayout) findViewById(R.id.qiblaRoot), "qibla");
  }

  private void showMissingSensor() {
    sensorStatus.setText(
        "Sensor kompas tidak tersedia. Gunakan panduan 294° dari utara sejati; arah pada diagram"
            + " bersifat statis.");
  }

  @Override
  protected void onResume() {
    super.onResume();
    receivedHeading = false;
    aligned = false;
    lastSensorRender = 0;
    lastStatus = 0;
    lastStatusText = "";
    compass.setHeading(0, false);
    sensorAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    if (rotationSensor != null)
      sensorStatus.setText("Menunggu sensor · pegang ponsel mendatar.");
    if (rotationSensor != null && !sensorManager.registerListener(this, rotationSensor, 100_000)) {
      rotationSensor = null;
      showMissingSensor();
    }
  }

  @Override
  protected void onPause() {
    if (sensorManager != null) sensorManager.unregisterListener(this);
    super.onPause();
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;
    long now = SystemClock.elapsedRealtime();
    // Bound redraws to 10 Hz; stable readings must not keep the UI rendering.
    if (receivedHeading && now - lastSensorRender < 100) return;
    lastSensorRender = now;
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
    SensorManager.remapCoordinateSystem(rotation, axisX, axisY, remapped);
    SensorManager.getOrientation(remapped, orientation);
    float trueHeading = CompassMath.normalize((float) Math.toDegrees(orientation[0]) + declination);
    heading = receivedHeading ? CompassMath.smooth(heading, trueHeading, 0.2f) : trueHeading;
    receivedHeading = true;
    float distance = Math.abs(CompassMath.delta(QIBLA_BEARING, heading));
    boolean nextAligned =
        sensorAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
            && distance <= (aligned ? 8f : 5f);
    if (nextAligned && !aligned && now - lastHaptic >= 2000) {
      Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
      if (vibrator != null && vibrator.hasVibrator())
        vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
      lastHaptic = now;
    }
    boolean stateChanged = nextAligned != aligned;
    aligned = nextAligned;
    compass.setHeading(heading, aligned);
    if (stateChanged || now - lastStatus > 350) {
      String text;
      if (sensorAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
        text = "Akurasi sensor rendah · kalibrasi kompas terlebih dahulu";
      else if (aligned) text = "✓ Sejajar dengan arah kiblat · 294°";
      else
        text =
            String.format(
                Locale.forLanguageTag("id-ID"),
                "Sensor aktif · arah ponsel %.0f° · putar %.0f° %s",
                heading,
                distance,
                CompassMath.delta(QIBLA_BEARING, heading) >= 0 ? "ke kanan" : "ke kiri");
      if (!text.equals(lastStatusText)) {
        sensorStatus.setText(text);
        sensorStatus.setTextColor(getColor(aligned ? R.color.sage_active : R.color.pine));
        lastStatusText = text;
      }
      lastStatus = now;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    sensorAccuracy = accuracy;
  }
}
