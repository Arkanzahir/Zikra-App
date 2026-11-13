package com.zikra.app;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.widget.*;
import com.zikra.app.safety.*;
import java.util.Locale;

/** User-controlled contextual reminders, using the existing Protector chrome. */
public final class ContextualActivity extends BaseActivity {
  private boolean binding;
  private final int[] switches = {R.id.contextWeather, R.id.contextBriefing, R.id.contextMorning, R.id.contextEvening};
  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_contextual);
    setupScreen(getString(R.string.contextual_title));
    ProtectorChrome.apply(this); ProtectorNavigation.bind(this);
    ((TextView)findViewById(R.id.protectorToolbarTitle)).setText(R.string.contextual_title);
    ((TextView)findViewById(R.id.protectorToolbarSubtitle)).setText(R.string.contextual_subtitle);
    findViewById(R.id.protectorRefresh).setVisibility(android.view.View.GONE);
    for (int i = 0; i < switches.length; i++) {
      final String key = ContextualReminders.OPTIONS[i];
      ((Switch)findViewById(switches[i])).setOnCheckedChangeListener((view, enabled) -> {
        if (binding) return;
        if (enabled && !EarthquakeAlerts.permission(this)) {
          bind();
          if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 803);
          Toast.makeText(this, R.string.contextual_permission, Toast.LENGTH_LONG).show();
          return;
        }
        if (enabled && "weather".equals(key)) {
          bind();
          new AlertDialog.Builder(this).setTitle(R.string.contextual_weather)
              .setMessage(R.string.contextual_location_consent)
              .setNegativeButton(R.string.contextual_cancel, null)
              .setPositiveButton(R.string.contextual_enable, (dialog, which) -> {
                ContextualReminders.set(this, key, true); bind();
              }).show();
        } else { ContextualReminders.set(this, key, enabled); bind(); }
      });
    }
    findViewById(R.id.contextMorningTime).setOnClickListener(v -> chooseTime("morning"));
    findViewById(R.id.contextEveningTime).setOnClickListener(v -> chooseTime("evening"));
    findViewById(R.id.contextTravel).setOnClickListener(v -> openDua("Perjalanan"));
    findViewById(R.id.contextDua).setOnClickListener(v -> openDua("Pagi & petang"));
    findViewById(R.id.contextStudy).setOnClickListener(v -> startActivity(new Intent(this, StudyActivity.class)));
    findViewById(R.id.contextQuake).setOnClickListener(v -> startActivity(new Intent(this, EarthquakeAlertsActivity.class)));
    findViewById(R.id.contextAndroidSettings).setOnClickListener(v -> startActivity(
        new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName())));
  }
  private void openDua(String category) {
    startActivity(new Intent(this, DuaActivity.class).putExtra("dua_category_name", category));
  }
  private void chooseTime(String period) {
    int minute = ContextualReminders.minute(this, period);
    new TimePickerDialog(this, (view, hour, minutes) -> {
      ContextualReminders.time(this, period, hour * 60 + minutes); bind();
    }, minute / 60, minute % 60, true).show();
  }
  @Override protected void onResume() { super.onResume(); bind(); ContextualReminders.schedule(this); }
  private void bind() {
    binding = true;
    for (int i = 0; i < switches.length; i++)
      ((Switch)findViewById(switches[i])).setChecked(ContextualReminders.enabled(this, ContextualReminders.OPTIONS[i]));
    binding = false;
    int morning = ContextualReminders.minute(this, "morning"), evening = ContextualReminders.minute(this, "evening");
    ((TextView)findViewById(R.id.contextMorningTime)).setText(getString(R.string.contextual_morning_time,
        String.format(Locale.ROOT, "%02d:%02d", morning / 60, morning % 60)));
    ((TextView)findViewById(R.id.contextEveningTime)).setText(getString(R.string.contextual_evening_time,
        String.format(Locale.ROOT, "%02d:%02d", evening / 60, evening % 60)));
    ((TextView)findViewById(R.id.contextPreview)).setText(ContextualReminders.briefing(this));
    boolean allowed = EarthquakeAlerts.permission(this)
        && getSystemService(NotificationManager.class).areNotificationsEnabled();
    ((TextView)findViewById(R.id.contextStatus)).setText(allowed
        ? getString(R.string.contextual_status, com.zikra.app.location.LocationStore.current(this).caption())
        : getString(R.string.contextual_permission));
  }
}
