package com.zikra.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.reminders.PrayerReminders;
import com.zikra.app.ui.SegmentedProgressView;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Locale;

public final class HomeActivity extends BaseActivity {
  private ZikraDbHelper db;
  private java.time.LocalDate progressDate;
  private int activePrayer = -1;
  private java.time.LocalDate prayerDisplayDate;
  private static final int[] PRAYER_SLOTS = {
    R.id.prayerSlot0, R.id.prayerSlot1, R.id.prayerSlot2,
    R.id.prayerSlot3, R.id.prayerSlot4, R.id.prayerSlot5
  };
  private final Handler timer = new Handler(Looper.getMainLooper());
  private final Runnable tick =
      new Runnable() {
        @Override
        public void run() {
          updateClock();
          timer.postDelayed(this, 60000 - System.currentTimeMillis() % 60000);
        }
      };

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    if (!getSharedPreferences("zikra_preferences", MODE_PRIVATE)
        .getBoolean("onboarding_complete", false)) {
      startActivity(new android.content.Intent(this, OnboardingActivity.class));
      finish();
      return;
    }
    setContentView(R.layout.activity_home);
    NavDock.attach(this, findViewById(R.id.homeRoot), "home");
    setupScreen("Zikra");
    db = new ZikraDbHelper(this);
    findViewById(R.id.shortcutQuran).setOnClickListener(v -> open(QuranActivity.class));
    findViewById(R.id.shortcutDua).setOnClickListener(v -> open(DuaActivity.class));
    findViewById(R.id.shortcutQibla).setOnClickListener(v -> open(QiblaActivity.class));
    findViewById(R.id.shortcutHifz)
        .setOnClickListener(
            v ->
                startActivity(
                    new android.content.Intent(this, StudyActivity.class)
                        .putExtra("category", ZikraDbHelper.CATEGORY_HIFZ)));
    findViewById(R.id.shortcutAi).setOnClickListener(v -> open(AiActivity.class));
    findViewById(R.id.shortcutProtector).setOnClickListener(v -> open(ProtectorActivity.class));
    findViewById(R.id.shortcutStudy).setOnClickListener(v -> open(StudyActivity.class));
    findViewById(R.id.shortcutHadith).setOnClickListener(v -> open(HadithActivity.class));
    findViewById(R.id.homeChecklist).setOnClickListener(v -> open(StudyActivity.class));
    findViewById(R.id.homeReminders).setOnClickListener(v -> open(PrayerActivity.class));
    findViewById(R.id.homeSearch).setOnClickListener(v -> open(QuranActivity.class));
    findViewById(R.id.homeHijri).setOnClickListener(v -> open(CalendarActivity.class));
    findViewById(R.id.homeContinueReading)
        .setOnClickListener(
            v ->
                startActivity(
                    new android.content.Intent(this, QuranActivity.class)
                        .putExtra(QuranActivity.EXTRA_RESUME_READING, true)));
    findViewById(R.id.homeLocation)
        .setOnClickListener(
            v -> external(android.content.Intent.ACTION_VIEW, "geo:0,0?q=Sukolilo,Surabaya"));
    findViewById(R.id.homePrayerStrip).setOnClickListener(v -> open(PrayerActivity.class));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (db == null) return;
    updateProgress();
    PrayerReminders.sync(this);
    SharedPreferences p = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    ((TextView) findViewById(R.id.homeLastRead))
        .setText(p.getString("last_surah", "Al-Fatihah") + " · Ayat " + p.getInt("last_ayah", 1));
    findViewById(R.id.homeContinueReading)
        .setContentDescription(
            "Lanjutkan bacaan " + ((TextView) findViewById(R.id.homeLastRead)).getText());
    timer.removeCallbacks(tick);
    timer.post(tick);
  }

  private void updateProgress() {
    ZikraDbHelper.Progress progress = db.getProgress();
    int done = progress.completed, total = progress.total;
    progressDate = java.time.LocalDate.now();
    ((TextView) findViewById(R.id.homeProgressPercent)).setText(progress.percent + "%");
    ((TextView) findViewById(R.id.homeProgressSubtitle))
        .setText(
            total == 0
                ? "Belum ada checklist hari ini"
                : done + " dari " + total + " checklist selesai");
    ((SegmentedProgressView) findViewById(R.id.homeProgressTicks)).setProgress(done, total);
  }

  private void updateClock() {
    if (!java.time.LocalDate.now().equals(progressDate)) updateProgress();
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
    ((TextView) findViewById(R.id.homeClock))
        .setText(now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)));
    HijrahDate date = HijrahDate.from(now.toLocalDate());
    String[] months = {
      "Muharram",
      "Safar",
      "Rabiul Awal",
      "Rabiul Akhir",
      "Jumadil Awal",
      "Jumadil Akhir",
      "Rajab",
      "Syaban",
      "Ramadan",
      "Syawal",
      "Zulkaidah",
      "Zulhijah"
    };
    ((TextView) findViewById(R.id.homeHijri))
        .setText(
            date.get(ChronoField.DAY_OF_MONTH)
                + " "
                + months[date.get(ChronoField.MONTH_OF_YEAR) - 1]
                + ", "
                + date.get(ChronoField.YEAR)
                + " H");
    PrayerSchedule.NextPrayer event = PrayerSchedule.nextAfter(now);
    int next = event.index;
    PrayerSchedule.Day timetable = PrayerSchedule.forDate(event.time.toLocalDate());
    int minutes = event.minutesFrom(now);
    String interval =
        minutes >= 60 ? (minutes / 60 + " jam " + minutes % 60 + " menit") : (minutes + " menit");
    String content =
        PrayerSchedule.NAMES[next]
            + (event.time.toLocalDate().isAfter(now.toLocalDate()) ? " besok" : "")
            + " tinggal "
            + interval
            + " lagi";
    SpannableString label = new SpannableString(content);
    int start = content.indexOf(interval);
    label.setSpan(
        new ForegroundColorSpan(getColor(R.color.camel)),
        start,
        start + interval.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    ((TextView) findViewById(R.id.homeCountdown)).setText(label);
    findViewById(R.id.homeCountdown)
        .setContentDescription(
            content + ". Perhitungan harian Sukolilo, WIB. Buka Jadwal Sholat untuk metode.");
    if (activePrayer != next || !timetable.date.equals(prayerDisplayDate)) {
      activePrayer = next;
      prayerDisplayDate = timetable.date;
      findViewById(R.id.homePrayerStrip)
          .setContentDescription(
              "Jadwal sholat Sukolilo " + timetable.date + ", WIB. Ketuk untuk rincian.");
      for (int i = 0; i < PRAYER_SLOTS.length; i++) {
        LinearLayout slot = findViewById(PRAYER_SLOTS[i]);
        boolean active = i == next;
        ((ImageView) slot.getChildAt(1))
            .setColorFilter(getColor(active ? R.color.camel : R.color.home_prayer_text));
        ((TextView) slot.getChildAt(0)).setTextColor(getColor(R.color.home_prayer_text));
        ((TextView) slot.getChildAt(2)).setText(timetable.time(i));
        ((TextView) slot.getChildAt(2)).setTextColor(getColor(R.color.white));
        slot.setContentDescription(
            PrayerSchedule.NAMES[i]
                + " "
                + timetable.time(i)
                + (active ? ", sholat berikutnya" : ""));
      }
    }
  }

  @Override
  protected void onPause() {
    timer.removeCallbacks(tick);
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (db != null) db.close();
    super.onDestroy();
  }
}
