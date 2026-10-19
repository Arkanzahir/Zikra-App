package com.zikra.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

/** Shared native chrome and foreground-only, debounced shake emergency shortcut. */
public abstract class BaseActivity extends Activity {
  private SensorManager motion;
  private Sensor acceleration;
  private long lastPeak, lastDialog;
  private int peaks;
  private AlertDialog emergency;
  private final SensorEventListener shakeListener =
      new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
          float[] a = event.values;
          double force =
              Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]) / SensorManager.GRAVITY_EARTH;
          long now = SystemClock.elapsedRealtime();
          if (force < 2.65 || now - lastPeak < 200 || now - lastDialog < 2500) return;
          peaks = now - lastPeak < 1100 ? peaks + 1 : 1;
          lastPeak = now;
          if (peaks >= 2) {
            peaks = 0;
            lastDialog = now;
            showEmergencyActions();
          }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    motion = (SensorManager) getSystemService(SENSOR_SERVICE);
    if (motion != null) acceleration = motion.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
  }

  protected void setupScreen(String title) {
    setTitle(title);
    getWindow().setStatusBarColor(getColor(R.color.pine));
    getWindow().setNavigationBarColor(getColor(R.color.background));
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    if (android.os.Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
    int backId = getResources().getIdentifier("toolbarBack", "id", getPackageName());
    if (backId != 0 && findViewById(backId) != null)
      findViewById(backId).setOnClickListener(v -> finish());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (usesShakeShortcut()
        && motion != null
        && acceleration != null
        && getSharedPreferences("zikra_preferences", MODE_PRIVATE)
            .getBoolean("shake_enabled", true))
      motion.registerListener(shakeListener, acceleration, SensorManager.SENSOR_DELAY_UI);
  }

  @Override
  protected void onPause() {
    if (motion != null) motion.unregisterListener(shakeListener);
    peaks = 0;
    if (emergency != null) emergency.dismiss();
    super.onPause();
  }

  protected boolean usesShakeShortcut() {
    return true;
  }

  protected void open(Class<? extends Activity> activity) {
    startActivity(new Intent(this, activity));
  }

  protected int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  protected void toast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }

  protected void external(String action, String uri) {
    try {
      startActivity(new Intent(action, Uri.parse(uri)));
    } catch (ActivityNotFoundException e) {
      toast("Aplikasi yang diperlukan belum tersedia di perangkat.");
    }
  }

  protected void showEmergencyActions() {
    if (isFinishing() || (emergency != null && emergency.isShowing())) return;
    emergency =
        new AlertDialog.Builder(this)
            .setTitle("Bantuan cepat")
            .setItems(
                new String[] {
                  "Telepon darurat 112",
                  "Kabari keluarga lewat WhatsApp",
                  "Cari masjid terdekat",
                  "Buka Protector BMKG"
                },
                (d, which) -> {
                  if (which == 0) external(Intent.ACTION_DIAL, "tel:112");
                  if (which == 1)
                    external(
                        Intent.ACTION_VIEW,
                        "https://wa.me/?text="
                            + Uri.encode("Aku membutuhkan bantuan. Tolong hubungi aku."));
                  if (which == 2) open(NearbyMosquesActivity.class);
                  if (which == 3) open(ProtectorActivity.class);
                })
            .setNegativeButton("Tutup", null)
            .show();
  }
}
