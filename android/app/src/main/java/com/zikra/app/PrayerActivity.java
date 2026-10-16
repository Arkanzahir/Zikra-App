package com.zikra.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * Native reference timetable and persisted reminder preferences, without background alarm claims.
 */
public final class PrayerActivity extends BaseActivity {
  private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
  private static final Locale ID = Locale.forLanguageTag("id-ID");
  private static final int[] PRAYER_INDEXES = {0, 2, 3, 4, 5};
  private static final String[] PRAYER_KEYS = {"subuh", "dzuhur", "ashar", "maghrib", "isya"};
  private final Handler clock = new Handler(Looper.getMainLooper());
  private final Runnable updateClock =
      new Runnable() {
        @Override
        public void run() {
          LocalDate today = LocalDate.now(WIB);
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

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_prayer);
    setupScreen("Waktu Sholat");
    getWindow().setStatusBarColor(Color.parseColor("#FCFDFB"));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    selectedDate = LocalDate.now(WIB);
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
    ((TextView) findViewById(R.id.prayerSunrise)).setText(PrayerSchedule.TIMES[1]);
    ((TextView) findViewById(R.id.prayerSunset)).setText(PrayerSchedule.TIMES[4]);
    todayTab.setOnClickListener(
        view -> {
          selectedDate = LocalDate.now(WIB);
          selectMode(false);
        });
    weekTab.setOnClickListener(view -> selectMode(true));
    findViewById(R.id.prayerOpenQibla).setOnClickListener(view -> open(QiblaActivity.class));
    Switch master = findViewById(R.id.prayerReminderEnabled);
    master.setChecked(preferences.getBoolean("reminder_enabled", true));
    master.setOnCheckedChangeListener(
        (view, enabled) -> {
          preferences.edit().putBoolean("reminder_enabled", enabled).apply();
          renderRows();
        });
    selectMode(weekMode);
    NavDock.attach(this, (FrameLayout) findViewById(R.id.prayerRoot), "prayer");
  }

  @Override
  protected void onResume() {
    super.onResume();
    ((Switch) findViewById(R.id.prayerReminderEnabled))
        .setChecked(preferences.getBoolean("reminder_enabled", true));
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
    todayTab.setTextColor(Color.parseColor(week ? "#7E9185" : "#FFFFFF"));
    weekTab.setTextColor(Color.parseColor(week ? "#FFFFFF" : "#7E9185"));
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
      TextView name = item.findViewById(R.id.prayerDayName);
      TextView number = item.findViewById(R.id.prayerDayNumber);
      name.setText(day.format(DateTimeFormatter.ofPattern("EEE", ID)).toUpperCase(ID));
      number.setText(Integer.toString(day.getDayOfMonth()));
      boolean selected = day.equals(selectedDate);
      item.setBackgroundResource(selected ? R.drawable.prayer_bg_green : 0);
      name.setTextColor(Color.parseColor(selected ? "#DDEDE3" : "#96A69B"));
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
                ? "Jadwal pekan ini · referensi"
                : selectedDate.equals(LocalDate.now(WIB))
                    ? "Jadwal hari ini"
                    : selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)));
  }

  private void renderRows() {
    rows.removeAllViews();
    LocalTime now = LocalTime.now(WIB);
    int next = PrayerSchedule.nextPrayerIndex(now);
    boolean today = selectedDate.equals(LocalDate.now(WIB));
    boolean master = preferences.getBoolean("reminder_enabled", true);
    for (int item = 0; item < PRAYER_INDEXES.length; item++) {
      final String key = "reminder_" + PRAYER_KEYS[item];
      final int index = PRAYER_INDEXES[item];
      LinearLayout row =
          (LinearLayout) getLayoutInflater().inflate(R.layout.prayer_item, rows, false);
      row.setBackgroundResource(
          today && next == index ? R.drawable.prayer_bg_active : R.drawable.prayer_bg_row);
      ((TextView) row.findViewById(R.id.prayerRowName)).setText(PrayerSchedule.NAMES[index]);
      ((TextView) row.findViewById(R.id.prayerRowTime)).setText(PrayerSchedule.TIMES[index]);
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
            toast(
                "Preferensi "
                    + PrayerSchedule.NAMES[index]
                    + " "
                    + (enabledNow ? "diaktifkan" : "dinonaktifkan")
                    + ". Notifikasi otomatis belum diaktifkan.");
          });
      rows.addView(row);
    }
    previousNext = next;
  }

  private void renderCountdown() {
    LocalDateTime now = LocalDateTime.now(WIB);
    boolean today = selectedDate.equals(now.toLocalDate());
    int next = today ? PrayerSchedule.nextPrayerIndex(now.toLocalTime()) : 0;
    if (today && previousNext != next) renderRows();
    ((TextView) findViewById(R.id.prayerNextLabel))
        .setText(
            today
                ? "Sholat selanjutnya · jadwal referensi"
                : selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID))
                    + " · jadwal referensi");
    ((TextView) findViewById(R.id.prayerNextName))
        .setText(PrayerSchedule.NAMES[next] + " · " + PrayerSchedule.TIMES[next]);
    TextView countdown = findViewById(R.id.prayerCountdown);
    if (!today) {
      countdown.setText("Sukolilo · WIB");
      countdown.setTextSize(21);
      return;
    }
    LocalDateTime target = now.toLocalDate().atTime(LocalTime.parse(PrayerSchedule.TIMES[next]));
    if (!target.isAfter(now)) target = target.plusDays(1);
    long seconds = Duration.between(now, target).getSeconds();
    countdown.setText(
        String.format(ID, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60));
    countdown.setTextSize(27);
  }

  private void renderWeek() {
    weekSummary.removeAllViews();
    LocalDate monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    for (int day = 0; day < 7; day++) {
      LocalDate date = monday.plusDays(day);
      TextView item = new TextView(this);
      item.setText(
          date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", ID))
              + "\nSubuh  "
              + PrayerSchedule.TIMES[0]
              + "    Dzuhur  "
              + PrayerSchedule.TIMES[2]
              + "    Ashar  "
              + PrayerSchedule.TIMES[3]
              + "\nMaghrib  "
              + PrayerSchedule.TIMES[4]
              + "    Isya  "
              + PrayerSchedule.TIMES[5]);
      item.setTextSize(11);
      item.setTextColor(Color.parseColor("#526F5C"));
      item.setLineSpacing(dp(5), 1);
      item.setPadding(dp(14), dp(11), dp(14), dp(11));
      item.setBackgroundResource(
          date.equals(LocalDate.now(WIB)) ? R.drawable.prayer_bg_active : R.drawable.prayer_bg_row);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
      params.bottomMargin = dp(7);
      weekSummary.addView(item, params);
    }
  }
}
