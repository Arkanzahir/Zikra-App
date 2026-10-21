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
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.reminders.PrayerReminders;
import com.zikra.app.ui.SegmentedProgressView;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Locale;

public final class HomeActivity extends BaseActivity {
  private LocationPoint location = LocationPoint.INITIAL;
  private ZikraDbHelper db;
  private com.zikra.app.sky.HomeSkyView sky;
  private com.zikra.app.sky.SkyRepository skyRepository;
  private com.zikra.app.sky.SkyDay skyDay;
  private java.time.LocalDate requestedSkyDate;
  private com.zikra.app.ui.AmbientMotionPolicy entrancePolicy;
  private boolean entrancePlayed;
  private static final int[] ENTRANCE_IDS = {
    R.id.homeLocation,
    R.id.homeClock,
    R.id.homePrayerStrip,
    R.id.homeShortcuts,
    R.id.homeTargetCard,
    R.id.homeContinueReading
  };
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
    findViewById(R.id.homeFocusCard).setOnClickListener(v -> open(FocusTimerActivity.class));
    adaptHomeForTextSize();
    NavDock.attach(this, findViewById(R.id.homeRoot), "home");
    setupScreen("Zikra");
    db = new ZikraDbHelper(this);
    sky = findViewById(R.id.homeSky);
    skyRepository = new com.zikra.app.sky.SkyRepository(this);
    entrancePlayed = state != null && state.getBoolean("entrance_played", false);
    entrancePolicy =
        new com.zikra.app.ui.AmbientMotionPolicy(
            this,
            () -> {
              if (!entrancePolicy.enabled()) finishEntrance();
            });
    findViewById(R.id.homeSkySource)
        .setOnClickListener(
            v -> {
              if (skyDay == null) return;
              new android.app.AlertDialog.Builder(this)
                  .setTitle("Langit · " + location.label)
                  .setMessage(
                      (skyDay.usno
                              ? "USNO, data harian "
                              : skyDay.solarAvailable
                                  ? "Perkiraan lokal, "
                                  : "Ilustrasi waktu; peristiwa matahari tidak tersedia, ")
                          + skyDay.date
                          + (skyDay.solarAvailable
                              ? "\nTerbit "
                                  + skyDay.rise
                                  + " · transit "
                                  + skyDay.noon
                                  + " · terbenam "
                                  + skyDay.set
                              : "\nWaktu terbit/terbenam belum tersedia untuk lokasi/tanggal ini.")
                          + "\nBulan diterangi "
                          + Math.round(skyDay.illumination * 100)
                          + "%.\n\n"
                          + "Posisi matahari mengikuti waktu setempat. Bentuk bulan menggambarkan"
                          + " fraksi terang harian; arah dan penempatannya ilustratif. Bukan"
                          + " penetapan awal bulan Hijriah.")
                  .setPositiveButton("Tutup", null)
                  .setNeutralButton("Perbarui", (d, w) -> loadSky(true))
                  .show();
            });
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
    findViewById(R.id.homeChecklist)
        .setOnClickListener(
            v ->
                startActivity(
                    new android.content.Intent(this, StudyActivity.class)
                        .putExtra("category", ZikraDbHelper.CATEGORY_IBADAH)));
    findViewById(R.id.homeReminders).setOnClickListener(v -> open(PrayerActivity.class));
    findViewById(R.id.homeSearch).setOnClickListener(v -> open(QuranActivity.class));
    findViewById(R.id.homeHijri).setOnClickListener(v -> open(CalendarActivity.class));
    findViewById(R.id.homeContinueReading)
        .setOnClickListener(
            v ->
                startActivity(
                    new android.content.Intent(this, QuranActivity.class)
                        .putExtra(QuranActivity.EXTRA_RESUME_READING, true)));
    findViewById(R.id.homeLocation).setOnClickListener(v -> open(LocationActivity.class));
    findViewById(R.id.homePrayerStrip).setOnClickListener(v -> open(PrayerActivity.class));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (db == null) return;
    LocationPoint selected = LocationStore.current(this);
    if (!selected.key().equals(location.key())) {
      requestedSkyDate = null;
      skyDay = null;
      activePrayer = -1;
      prayerDisplayDate = null;
    }
    location = selected;
    ((TextView) findViewById(R.id.homeLocationLabel))
        .setText(
            location.label.split(",")[0]
                + " · "
                + location.zoneLabel(java.time.Instant.now())
                + (location.source.equals("initial") ? " · Pilih lokasi" : ""));
    findViewById(R.id.homeLocation)
        .setContentDescription("Lokasi " + location.caption() + ". Ketuk untuk mengubah.");
    sky.setRunning(true);
    entrancePolicy.start();
    if (!entrancePlayed) {
      entrancePlayed = true;
      if (entrancePolicy.enabled()) {
        for (int i = 0; i < ENTRANCE_IDS.length; i++) {
          android.view.View v = findViewById(ENTRANCE_IDS[i]);
          v.setAlpha(0);
          v.setTranslationY(10 * getResources().getDisplayMetrics().density);
          v.animate().alpha(1).translationY(0).setStartDelay(i * 45L).setDuration(400).start();
        }
      }
    }
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
    ZikraDbHelper.Progress progress = db.getProgress(ZikraDbHelper.CATEGORY_IBADAH);
    int done = progress.completed, total = progress.total;
    progressDate = java.time.LocalDate.now();
    ((TextView) findViewById(R.id.homeProgressPercent)).setText(progress.percent + "%");
    ((TextView) findViewById(R.id.homeProgressSubtitle))
        .setText(
            total == 0
                ? "Mulai dari satu ibadah yang ingin kamu jaga."
                : done + " dari " + total + " ibadah selesai");
    ((TextView) findViewById(R.id.homeChecklist))
        .setText(total == 0 ? "Catat ibadah pertama" : "Buka checklist ibadah");
    findViewById(R.id.homeProgressPercent)
        .setVisibility(total == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
    findViewById(R.id.homeProgressTicks)
        .setVisibility(total == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
    ((SegmentedProgressView) findViewById(R.id.homeProgressTicks)).setProgress(done, total);
  }

  /** Keep times readable and reflow shortcuts instead of shrinking the user's larger text. */
  private void adaptHomeForTextSize() {
    float scale = getResources().getConfiguration().fontScale;
    findViewById(R.id.homePrayerStrip).setMinimumWidth(dp(6 * 48 * Math.max(1f, scale)));
    if (scale <= 1.3f) return;
    LinearLayout shortcuts = findViewById(R.id.homeShortcuts);
    java.util.List<android.view.View> tiles = new java.util.ArrayList<>();
    for (int i = 0; i < shortcuts.getChildCount(); i++) {
      LinearLayout row = (LinearLayout) shortcuts.getChildAt(i);
      while (row.getChildCount() > 0) {
        android.view.View tile = row.getChildAt(0);
        row.removeViewAt(0);
        tiles.add(tile);
      }
    }
    shortcuts.removeAllViews();
    for (int i = 0; i < tiles.size(); i += 2) {
      LinearLayout row = new LinearLayout(this);
      row.setGravity(android.view.Gravity.CENTER_VERTICAL);
      for (int j = i; j < Math.min(i + 2, tiles.size()); j++)
        row.addView(tiles.get(j), new LinearLayout.LayoutParams(0, -2, 1));
      shortcuts.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }
  }

  private void updateClock() {
    if (!java.time.LocalDate.now().equals(progressDate)) updateProgress();
    ZonedDateTime now = ZonedDateTime.now(location.zone);
    loadSky(false);
    if (skyDay != null) sky.setSky(skyDay, now.toLocalTime());
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
    PrayerSchedule.NextPrayer event = PrayerSchedule.nextAfter(now, location);
    int next = event == null ? -1 : event.index;
    PrayerSchedule.Day timetable = PrayerSchedule.forDate(now.toLocalDate(), location);
    if (event == null) {
      ((TextView) findViewById(R.id.homeCountdown))
          .setText("Jadwal belum tersedia untuk lokasi ini");
    } else {
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
              content
                  + ". Perhitungan harian "
                  + location.caption()
                  + ". Buka Jadwal Sholat untuk metode.");
    }
    if (activePrayer != next || !timetable.date.equals(prayerDisplayDate)) {
      activePrayer = next;
      prayerDisplayDate = timetable.date;
      findViewById(R.id.homePrayerStrip)
          .setContentDescription(
              "Jadwal sholat "
                  + location.caption()
                  + " "
                  + timetable.date
                  + ". Ketuk untuk rincian.");
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

  private void loadSky(boolean force) {
    java.time.LocalDate date = java.time.LocalDate.now(location.zone);
    if (!force && date.equals(requestedSkyDate)) return;
    requestedSkyDate = date;
    skyRepository.load(
        date,
        force,
        location,
        day -> {
          skyDay = day;
          sky.setSky(day, java.time.LocalTime.now(location.zone));
          ((TextView) findViewById(R.id.homeSkySource))
              .setText(
                  "Langit · "
                      + (day.usno ? "USNO" : day.solarAvailable ? "perkiraan lokal" : "ilustrasi"));
        });
  }

  private void finishEntrance() {
    for (int id : ENTRANCE_IDS) {
      android.view.View v = findViewById(id);
      v.animate().cancel();
      v.setAlpha(1);
      v.setTranslationY(0);
    }
  }

  @Override
  public boolean dispatchTouchEvent(android.view.MotionEvent e) {
    if (sky != null) sky.observeTouch(e);
    return super.dispatchTouchEvent(e);
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putBoolean("entrance_played", entrancePlayed);
    super.onSaveInstanceState(out);
  }

  @Override
  protected void onPause() {
    timer.removeCallbacks(tick);
    if (sky != null) {
      sky.setRunning(false);
      finishEntrance();
      entrancePolicy.stop();
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (skyRepository != null) skyRepository.close();
    if (db != null) db.close();
    super.onDestroy();
  }
}
