package com.zikra.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.reminders.PrayerReminders;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** Daily native timetable with per-prayer Android notification reminders. */
public final class PrayerActivity extends BaseActivity {
  private LocationPoint location = LocationPoint.INITIAL;
  private ZoneId localZone = location.zone;
  private static final Locale ID = Locale.forLanguageTag("id-ID");
  private static final int[] PRAYER_INDEXES = {0, 2, 3, 4, 5};
  private static final String[] PRAYER_KEYS = {"subuh", "dzuhur", "ashar", "maghrib", "isya"};
  private final Handler clock = new Handler(Looper.getMainLooper());
  private final Runnable updateClock =
      new Runnable() {
        @Override
        public void run() {
          LocalDate today = LocalDate.now(localZone);
          if (!today.equals(lastClockDate)) {
            lastClockDate = today;
            selectedDate = today;
            renderDates();
            renderRows();
          }
          renderCountdown();
          clock.postDelayed(this, 1000);
        }
      };
  private SharedPreferences preferences;
  private LinearLayout dates, rows, weekSummary;
  private TextView todayTab, weekTab;
  private LocalDate selectedDate;
  private LocalDate lastClockDate;
  private boolean weekMode;
  private int previousNext = -1;
  private boolean applyingMaster;
  private static final int REQUEST_NOTIFICATIONS = 401;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_prayer);
    location = LocationStore.current(this);
    localZone = location.zone;
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.sharedFeatureHeader))
        .bind(
            R.id.toolbarBack,
            R.id.detailTitle,
            R.id.featureSubtitle,
            R.id.prayerOpenQibla,
            "Waktu Sholat",
            location.caption(),
            R.drawable.ic_compass,
            "Buka arah kiblat");
    setupScreen("Waktu Sholat");
    getWindow().setStatusBarColor(Color.parseColor("#FCFDFB"));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    selectedDate = LocalDate.now(localZone);
    lastClockDate = selectedDate;
    if (state != null) {
      try {
        selectedDate = LocalDate.parse(state.getString("selected_date", selectedDate.toString()));
      } catch (java.time.DateTimeException ignored) {
      }
      weekMode = state.getBoolean("week_mode", false);
    }
    dates = findViewById(R.id.prayerWeekDates);
    rows = findViewById(R.id.prayerRows);
    weekSummary = findViewById(R.id.prayerWeekSummary);
    todayTab = findViewById(R.id.prayerTodayTab);
    weekTab = findViewById(R.id.prayerWeekTab);

    todayTab.setOnClickListener(
        view -> {
          selectedDate = LocalDate.now(localZone);
          selectMode(false);
        });
    weekTab.setOnClickListener(view -> selectMode(true));
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.sharedFeatureHeader))
        .action()
        .setOnClickListener(view -> open(QiblaActivity.class));
    findViewById(R.id.prayerLocation).setOnClickListener(v -> open(LocationActivity.class));
    findViewById(R.id.prayerChecklist)
        .setOnClickListener(
            view ->
                startActivity(
                    new Intent(this, StudyActivity.class)
                        .putExtra("category", com.zikra.app.data.ZikraDbHelper.CATEGORY_IBADAH)));
    Switch master = findViewById(R.id.prayerReminderEnabled);
    master.setOnCheckedChangeListener(
        (view, enabled) -> {
          if (applyingMaster) return;
          if (enabled
              && Build.VERSION.SDK_INT >= 33
              && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                  != PackageManager.PERMISSION_GRANTED) {
            applyingMaster = true;
            master.setChecked(false);
            applyingMaster = false;
            requestPermissions(
                new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
          }
          preferences.edit().putBoolean("reminder_enabled", enabled).apply();
          PrayerReminders.sync(this);
          renderRows();
          updateReminderUi();
        });
    findViewById(R.id.prayerAlarmSettings).setOnClickListener(view -> openReminderSettings());
    selectMode(weekMode);
    NavDock.attach(this, (FrameLayout) findViewById(R.id.prayerRoot), "prayer");
  }

  @Override
  protected void onResume() {
    super.onResume();
    LocationPoint selected = LocationStore.current(this);
    if (!selected.key().equals(location.key())) selectedDate = LocalDate.now(selected.zone);
    location = selected;
    localZone = location.zone;
    ((TextView) findViewById(R.id.featureSubtitle)).setText(location.caption());
    ((TextView) findViewById(R.id.prayerMethod))
        .setText(
            "Perhitungan "
                + location.caption()
                + ". Subuh 20°, Isya 18°, Ashar bayangan 1×; ihtiyati 2 menit (terbit tanpa"
                + " ihtiyati). Asumsi dekat permukaan laut. Bukan jadwal resmi Kemenag; cocokkan"
                + " dengan masjid setempat. Tanda — berarti peristiwa belum dapat dihitung untuk"
                + " tanggal/lokasi ini.");
    PrayerReminders.sync(this);
    updateReminderUi();
    renderDates();
    renderWeek();
    renderRows();
    clock.removeCallbacks(updateClock);
    clock.post(updateClock);
  }

  @Override
  protected void onPause() {
    clock.removeCallbacks(updateClock);
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("selected_date", selectedDate.toString());
    state.putBoolean("week_mode", weekMode);
    super.onSaveInstanceState(state);
  }

  private void selectMode(boolean week) {
    weekMode = week;
    todayTab.setBackgroundResource(week ? 0 : R.drawable.prayer_bg_green);
    weekTab.setBackgroundResource(week ? R.drawable.prayer_bg_green : 0);
    todayTab.setTextColor(Color.parseColor(week ? "#53665E" : "#FFFFFF"));
    weekTab.setTextColor(Color.parseColor(week ? "#FFFFFF" : "#53665E"));
    rows.setVisibility(week ? View.GONE : View.VISIBLE);
    weekSummary.setVisibility(week ? View.VISIBLE : View.GONE);
    renderDates();
    renderRows();
    renderWeek();
    renderCountdown();
  }

  private void renderDates() {
    dates.removeAllViews();
    LocalDate weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    for (int i = 0; i < 7; i++) {
      LocalDate day = weekStart.plusDays(i);
      LinearLayout item =
          (LinearLayout) getLayoutInflater().inflate(R.layout.prayer_day, dates, false);
      item.getLayoutParams().width =
          dp(getResources().getConfiguration().fontScale > 1.3f ? 64 : 52);
      TextView name = item.findViewById(R.id.prayerDayName);
      TextView number = item.findViewById(R.id.prayerDayNumber);
      name.setText(day.format(DateTimeFormatter.ofPattern("EEE", ID)).toUpperCase(ID));
      number.setText(Integer.toString(day.getDayOfMonth()));
      boolean selected = day.equals(selectedDate);
      item.setBackgroundResource(selected ? R.drawable.prayer_bg_green : 0);
      name.setTextColor(Color.parseColor(selected ? "#DDEDE3" : "#53665E"));
      number.setTextColor(Color.parseColor(selected ? "#FFFFFF" : "#2E4837"));
      item.setContentDescription(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)));
      item.setOnClickListener(
          view -> {
            selectedDate = day;
            renderDates();
            renderRows();
            renderCountdown();
          });
      dates.addView(item);
    }
    dates.post(
        () -> {
          if (dates.getParent() instanceof android.widget.HorizontalScrollView) {
            int selected = selectedDate.getDayOfWeek().getValue() - 1;
            View child = dates.getChildAt(selected);
            if (child != null)
              ((android.widget.HorizontalScrollView) dates.getParent())
                  .smoothScrollTo(
                      Math.max(
                          0,
                          child.getLeft()
                              - (dates.getResources().getDisplayMetrics().widthPixels
                                      - child.getWidth())
                                  / 2),
                      0);
          }
        });
    String caption = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID));
    try {
      caption +=
          " · "
              + DateTimeFormatter.ofPattern("d MMMM yyyy", ID).format(HijrahDate.from(selectedDate))
              + " H";
    } catch (java.time.DateTimeException ignored) {
      /* Gregorian caption is still useful outside the Hijrah calendar range. */
    }
    ((TextView) findViewById(R.id.prayerDateCaption)).setText(caption);
    ((TextView) findViewById(R.id.prayerListTitle))
        .setText(
            weekMode
                ? "Jadwal pekan ini"
                : selectedDate.equals(LocalDate.now(localZone))
                    ? "Jadwal hari ini"
                    : selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)));
  }

  private void renderRows() {
    rows.removeAllViews();
    ZonedDateTime now = ZonedDateTime.now(localZone);
    PrayerSchedule.NextPrayer upcoming = PrayerSchedule.nextAfter(now, location);
    int next = upcoming == null ? -1 : upcoming.index;
    PrayerSchedule.Day timetable = PrayerSchedule.forDate(selectedDate, location);
    ((TextView) findViewById(R.id.prayerSunrise)).setText(timetable.time(1));
    ((TextView) findViewById(R.id.prayerSunset)).setText(timetable.time(4));
    boolean today = selectedDate.equals(LocalDate.now(localZone));
    boolean master = preferences.getBoolean("reminder_enabled", false);
    for (int item = 0; item < PRAYER_INDEXES.length; item++) {
      final String key = "reminder_" + PRAYER_KEYS[item];
      final int index = PRAYER_INDEXES[item];
      LinearLayout row =
          (LinearLayout) getLayoutInflater().inflate(R.layout.prayer_item, rows, false);
      row.setBackgroundResource(
          today
                  && upcoming != null
                  && upcoming.time.toLocalDate().equals(selectedDate)
                  && next == index
              ? R.drawable.prayer_bg_active
              : R.drawable.prayer_bg_row);
      ((TextView) row.findViewById(R.id.prayerRowName)).setText(PrayerSchedule.NAMES[index]);
      ((TextView) row.findViewById(R.id.prayerRowTime)).setText(timetable.time(index));
      ((ImageView) row.findViewById(R.id.prayerRowIcon))
          .setImageResource(index == 0 || index == 5 ? R.drawable.ic_moon : R.drawable.ic_sun);
      ImageButton bell = row.findViewById(R.id.prayerRowBell);
      boolean enabled = preferences.getBoolean(key, true);
      bell.setAlpha(master && enabled ? 1f : .3f);
      bell.setSelected(enabled);
      bell.setContentDescription(
          "Preferensi pengingat "
              + PrayerSchedule.NAMES[index]
              + (master && enabled ? " aktif" : " nonaktif"));
      bell.setOnClickListener(
          view -> {
            boolean enabledNow = !preferences.getBoolean(key, true);
            preferences.edit().putBoolean(key, enabledNow).apply();
            renderRows();
            PrayerReminders.sync(this);
            updateReminderUi();
            toast(
                "Pilihan "
                    + PrayerSchedule.NAMES[index]
                    + " disimpan"
                    + (PrayerReminders.enabled(this)
                        ? "."
                        : ". Aktifkan pengingat untuk menerima notifikasi."));
          });
      rows.addView(row);
    }
    previousNext = next;
  }

  private void renderCountdown() {
    ZonedDateTime now = ZonedDateTime.now(localZone);
    boolean today = selectedDate.equals(now.toLocalDate());
    PrayerSchedule.NextPrayer next = PrayerSchedule.nextAfter(now, location);
    PrayerSchedule.Day selected = PrayerSchedule.forDate(selectedDate, location);
    if (next == null) {
      ((TextView) findViewById(R.id.prayerNextLabel)).setText("Perhitungan belum tersedia");
      ((TextView) findViewById(R.id.prayerNextName))
          .setText("Cocokkan jadwal dengan masjid setempat");
      ((TextView) findViewById(R.id.prayerCountdown)).setText("—");
      return;
    }
    if (today && previousNext != next.index) renderRows();
    ((TextView) findViewById(R.id.prayerNextLabel))
        .setText(
            today
                ? "Sholat selanjutnya"
                    + (next.time.toLocalDate().isAfter(now.toLocalDate()) ? " · besok" : "")
                : selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)));
    ((TextView) findViewById(R.id.prayerNextName))
        .setText(
            today
                ? PrayerSchedule.NAMES[next.index]
                    + " · "
                    + next.time.format(DateTimeFormatter.ofPattern("HH:mm"))
                : PrayerSchedule.NAMES[0] + " · " + selected.time(0));
    TextView countdown = findViewById(R.id.prayerCountdown);
    if (!today) {
      countdown.setText(location.zoneLabel(java.time.Instant.now()));
      countdown.setTextSize(21);
      return;
    }
    long seconds = Duration.between(now, next.time).getSeconds();
    countdown.setText(
        String.format(ID, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60));
    countdown.setTextSize(27);
  }

  private void updateReminderUi() {
    applyingMaster = true;
    ((Switch) findViewById(R.id.prayerReminderEnabled)).setChecked(PrayerReminders.enabled(this));
    applyingMaster = false;
    boolean allowed = PrayerReminders.canNotify(this);
    ((TextView) findViewById(R.id.prayerReminderStatus))
        .setText(
            !PrayerReminders.enabled(this)
                ? "Aktifkan untuk menerima notifikasi waktu sholat."
                : !allowed
                    ? "Notifikasi belum diizinkan di pengaturan perangkat."
                    : PrayerReminders.exactAllowed(this)
                        ? "Pengingat aktif sesuai jadwal."
                        : "Pengingat aktif; ketepatan waktu mengikuti pengaturan perangkat.");
    ((TextView) findViewById(R.id.prayerAlarmSettings))
        .setText(
            !allowed
                ? "Atur izin notifikasi"
                : !PrayerReminders.exactAllowed(this)
                    ? "Atur ketepatan pengingat"
                    : "Pengaturan notifikasi");
  }

  private void openReminderSettings() {
    Intent intent;
    if (PrayerReminders.canNotify(this)
        && !PrayerReminders.exactAllowed(this)
        && Build.VERSION.SDK_INT >= 31) {
      intent =
          new Intent(
              Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
              Uri.parse("package:" + getPackageName()));
    } else {
      intent =
          new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
              .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
    }
    try {
      startActivity(intent);
    } catch (ActivityNotFoundException unavailable) {
      toast("Pengaturan tidak tersedia pada perangkat ini.");
    }
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(request, permissions, grants);
    if (request == REQUEST_NOTIFICATIONS) {
      preferences
          .edit()
          .putBoolean(
              "reminder_enabled",
              grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED)
          .apply();
      PrayerReminders.sync(this);
      updateReminderUi();
      renderRows();
    }
  }

  private void renderWeek() {
    weekSummary.removeAllViews();
    LocalDate monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    for (int day = 0; day < 7; day++) {
      LocalDate date = monday.plusDays(day);
      PrayerSchedule.Day timetable = PrayerSchedule.forDate(date, location);
      TextView item = new TextView(this);
      item.setText(
          date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", ID))
              + "\nSubuh  "
              + timetable.time(0)
              + "    Dzuhur  "
              + timetable.time(2)
              + "    Ashar  "
              + timetable.time(3)
              + "\nMaghrib  "
              + timetable.time(4)
              + "    Isya  "
              + timetable.time(5));
      item.setTextSize(14);
      item.setTextColor(Color.parseColor("#526F5C"));
      item.setLineSpacing(dp(5), 1);
      item.setPadding(dp(14), dp(11), dp(14), dp(11));
      item.setBackgroundResource(
          date.equals(LocalDate.now(localZone))
              ? R.drawable.prayer_bg_active
              : R.drawable.prayer_bg_row);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
      params.bottomMargin = dp(7);
      weekSummary.addView(item, params);
    }
  }
}
