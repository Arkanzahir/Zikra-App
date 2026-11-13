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
  private String appliedAppearance;
  private int appliedNightMode;

  @Override
  protected void attachBaseContext(android.content.Context base) {
    super.attachBaseContext(com.zikra.app.ui.AppAppearance.wrap(base));
  }
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
    appliedAppearance = com.zikra.app.ui.AppAppearance.selected(this);
    appliedNightMode = getResources().getConfiguration().uiMode
        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
    motion = (SensorManager) getSystemService(SENSOR_SERVICE);
    if (motion != null) acceleration = motion.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
  }

  protected void setupScreen(String title) {
    setTitle(title);
    getWindow().setStatusBarColor(getColor(R.color.pine));
    getWindow().setNavigationBarColor(getColor(R.color.background));
    getWindow().getDecorView().setSystemUiVisibility(
        com.zikra.app.ui.AppAppearance.dark(this) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    if (android.os.Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
    int backId = getResources().getIdentifier("toolbarBack", "id", getPackageName());
    if (backId != 0 && findViewById(backId) != null)
      findViewById(backId).setOnClickListener(v -> finish());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!com.zikra.app.ui.AppAppearance.selected(this).equals(appliedAppearance)
        || com.zikra.app.ui.AppAppearance.resolvedNightMode(this) != appliedNightMode) {
      recreate();
      return;
    }
    if (usesShakeShortcut()
        && motion != null
        && acceleration != null
        && getSharedPreferences("zikra_preferences", MODE_PRIVATE)
            .getBoolean("shake_enabled", true))
      motion.registerListener(shakeListener, acceleration, SensorManager.SENSOR_DELAY_UI);
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    int lightStatus = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    int lightNavigation = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    int flags = getWindow().getDecorView().getSystemUiVisibility() & ~(lightStatus | lightNavigation);
    int status = getWindow().getStatusBarColor();
    int navigation = getWindow().getNavigationBarColor();
    if (android.graphics.Color.alpha(navigation) == 0) navigation = getColor(R.color.background);
    if (android.graphics.Color.valueOf(status).luminance() > 0.5f) flags |= lightStatus;
    if (android.graphics.Color.valueOf(navigation).luminance() > 0.5f) flags |= lightNavigation;
    getWindow().getDecorView().setSystemUiVisibility(flags);
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
    if (!tryExternal(action, Uri.parse(uri))) toast(getString(R.string.emergency_no_app));
  }

  protected boolean tryExternal(String action, Uri uri) {
    try {
      startActivity(new Intent(action, uri));
      return true;
    } catch (ActivityNotFoundException | SecurityException unavailable) { return false; }
  }

  protected void showEmergencyActions() {
    if (isFinishing() || (emergency != null && emergency.isShowing())) return;
    emergency =
        new AlertDialog.Builder(this)
            .setTitle(R.string.emergency_title)
            .setItems(
                new String[] {
                  getString(R.string.emergency_112), getString(R.string.emergency_119),
                  getString(R.string.emergency_110), getString(R.string.emergency_family),
                  getString(R.string.emergency_mosques), getString(R.string.emergency_protector),
                  getString(R.string.emergency_scope)
                },
                (d, which) -> {
                  if (which < 3) external(Intent.ACTION_DIAL, "tel:" + new String[]{"112", "119", "110"}[which]);
                  if (which == 3) familyDraft();
                  if (which == 4) open(NearbyMosquesActivity.class);
                  if (which == 5) open(ProtectorActivity.class);
                  if (which == 6) emergencyScope();
                })
            .setNegativeButton(R.string.emergency_close, null)
            .show();
  }

  private void familyDraft() {
    emergency = new AlertDialog.Builder(this).setTitle(R.string.emergency_family)
        .setItems(new String[]{getString(R.string.emergency_family_sms), getString(R.string.emergency_family_share)},
            (dialog, which) -> {
              if (which == 0) {
                Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                    .putExtra("sms_body", getString(R.string.emergency_family_body));
                try { startActivity(sms); return; }
                catch (ActivityNotFoundException | SecurityException unavailable) { /* Offer a share draft below. */ }
              }
              Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                  .putExtra(Intent.EXTRA_TEXT, getString(R.string.emergency_family_body));
              try { startActivity(Intent.createChooser(share, getString(R.string.emergency_share_chooser))); }
              catch (ActivityNotFoundException | SecurityException unavailable) { toast(getString(R.string.emergency_no_app)); }
            }).setNegativeButton(R.string.emergency_close, null).show();
  }

  private void emergencyScope() {
    emergency = new AlertDialog.Builder(this).setTitle(R.string.emergency_scope)
        .setMessage(R.string.emergency_scope_body)
        .setPositiveButton(R.string.emergency_sources, (dialog, which) -> {
          String[] labels = {"112 · Komdigi", "119 · Kementerian Kesehatan", "110 · Polri"};
          String[] urls = {"https://layanan112.komdigi.go.id/alur", "https://kemkes.go.id/id/layanan/psc-119", "https://polri.go.id/kontak/call-center-110"};
          emergency = new AlertDialog.Builder(this).setTitle(R.string.emergency_sources)
              .setItems(labels, (source, selected) -> external(Intent.ACTION_VIEW, urls[selected]))
              .setNegativeButton(R.string.emergency_close, null).show();
        }).setNegativeButton(R.string.emergency_close, null).show();
  }
}
