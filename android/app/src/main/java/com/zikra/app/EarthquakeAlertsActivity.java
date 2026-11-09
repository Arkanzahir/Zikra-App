package com.zikra.app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.TextView;
import com.zikra.app.safety.*;

public final class EarthquakeAlertsActivity extends BaseActivity {
  private Switch toggle;
  private boolean binding;
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changes =
      (prefs, key) -> runOnUiThread(this::bind);

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    setContentView(R.layout.activity_earthquake_alerts);
    setupScreen(getString(R.string.quake_alert_settings));
    ProtectorChrome.apply(this);
    ProtectorNavigation.bind(this);
    ((TextView) findViewById(R.id.protectorToolbarTitle)).setText(R.string.quake_alert_settings);
    ((TextView) findViewById(R.id.protectorToolbarSubtitle)).setText(R.string.quake_alert_channel);
    findViewById(R.id.protectorRefresh).setVisibility(android.view.View.GONE);
    toggle = findViewById(R.id.quakeAlertSwitch);
    toggle.setOnCheckedChangeListener(
        (button, enabled) -> {
          if (binding) return;
          if (enabled && !EarthquakeAlerts.permission(this)) {
            binding = true;
            toggle.setChecked(false);
            binding = false;
            if (android.os.Build.VERSION.SDK_INT >= 33)
              requestPermissions(
                  new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 802);
          } else EarthquakeAlerts.setEnabled(this, enabled);
          bind();
        });
    findViewById(R.id.quakeAlertAndroidSettings)
        .setOnClickListener(
            v ->
                startActivity(
                    new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
  }

  @Override
  protected void onResume() {
    super.onResume();
    getSharedPreferences(EarthquakeAlerts.PREFS, 0)
        .registerOnSharedPreferenceChangeListener(changes);
    if (EarthquakeAlerts.enabled(this)) EarthquakeAlerts.sync(getApplicationContext());
    bind();
  }

  @Override
  protected void onPause() {
    getSharedPreferences(EarthquakeAlerts.PREFS, 0)
        .unregisterOnSharedPreferenceChangeListener(changes);
    super.onPause();
  }

  private void bind() {
    binding = true;
    toggle.setEnabled(EarthquakeAlerts.configured(this));
    toggle.setChecked(EarthquakeAlerts.enabled(this));
    binding = false;
    int status =
        !EarthquakeAlerts.configured(this)
            ? R.string.quake_alert_not_configured
            : !EarthquakeAlerts.allowed(this)
                ? R.string.quake_alert_permission
                : !EarthquakeAlerts.enabled(this)
                    ? R.string.quake_alert_off
                    : "ready"
                            .equals(
                                getSharedPreferences(EarthquakeAlerts.PREFS, 0)
                                    .getString("subscription", ""))
                        ? R.string.quake_alert_ready
                        : R.string.quake_alert_pending;
    ((TextView) findViewById(R.id.quakeAlertStatus)).setText(status);
  }

  @Override
  public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(code, permissions, results);
    if (code == 802) {
      if (EarthquakeAlerts.permission(this)) EarthquakeAlerts.setEnabled(this, true);
      bind();
    }
  }
}
