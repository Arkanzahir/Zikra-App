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
  private java.time.Clock clock = java.time.Clock.systemUTC();
  private ZikraDbHelper db;
  private String[] prayerNames;
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
          timer.postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
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
    prayerNames = getResources().getStringArray(R.array.home_prayer_labels);
    findViewById(R.id.homeFocusCard).setOnClickListener(v -> open(FocusTimerActivity.class));
    LinearLayout brand = findViewById(R.id.homeRoot).findViewWithTag("zikraBrand");
    android.view.View symbol = brand.getChildAt(0);
    symbol.setLayoutParams(new LinearLayout.LayoutParams(dp(58), dp(58)));
    symbol.setScaleX(1.20f); symbol.setScaleY(1.20f);
    symbol.setTranslationX(-dp(4.5f)); symbol.setTranslationY(dp(4));
    TextView wordmark = (TextView) brand.getChildAt(1);
    wordmark.setTypeface(android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD));
    wordmark.setTextSize(24);
    LinearLayout.LayoutParams brandParams = (LinearLayout.LayoutParams) brand.getLayoutParams();
    brandParams.leftMargin = -dp(6); brand.setLayoutParams(brandParams);
    LinearLayout.LayoutParams wordParams = (LinearLayout.LayoutParams) wordmark.getLayoutParams();
    wordParams.leftMargin = dp(4); wordmark.setLayoutParams(wordParams);
    android.graphics.Typeface headingFace=android.os.Build.VERSION.SDK_INT>=28
        ?android.graphics.Typeface.create(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL),600,false)
        :android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL);
    for(int textId:new int[]{R.id.homeGreeting,R.id.homeClock,R.id.homeRhythmTitle,R.id.homeLastRead})
      ((TextView)findViewById(textId)).setTypeface(headingFace);
    findViewById(R.id.homeHero).setOnLongClickListener(v -> {
      findViewById(R.id.homeSkySource).performClick(); return true;
    });
    adaptHomeForTextSize();
    NavDock.attach(this, findViewById(R.id.homeRoot), "home");
    setupScreen("Zikra");
    getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
    if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
    android.view.View homeRoot = findViewById(R.id.homeRoot);
    homeRoot.setBackgroundColor(getColor(R.color.pine));
    findViewById(R.id.homeScroll).setBackgroundColor(getColor(R.color.studio_cream));
    homeRoot.setOnApplyWindowInsetsListener((view, insets) -> {
      int top = android.os.Build.VERSION.SDK_INT >= 30
          ? insets.getInsets(android.view.WindowInsets.Type.statusBars()).top : insets.getSystemWindowInsetTop();
      int bottom = android.os.Build.VERSION.SDK_INT >= 30
          ? insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom : insets.getSystemWindowInsetBottom();
      view.setPadding(0, top, 0, bottom > dp(30) ? bottom : 0);
      return insets;
    });
    homeRoot.requestApplyInsets();
    getWindow().getDecorView().setSystemUiVisibility(
        android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    db = new ZikraDbHelper(this);
    sky = findViewById(R.id.homeSky);
    findViewById(R.id.homeHero).addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
      int wanted = Math.max(dp(1), bottom - top - dp(14));
      if (sky.getLayoutParams().height != wanted) {
        android.view.ViewGroup.LayoutParams params = sky.getLayoutParams();
        params.height = wanted; sky.setLayoutParams(params);
      }
    });
    android.widget.ScrollView homeScroll = findViewById(R.id.homeScroll);
    homeScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
      sky.setScrollOffset(y);
      sky.setRunning(y < findViewById(R.id.homeHero).getBottom());
    });
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
                  .setNegativeButton("Pengingat salat", (d, w) -> open(PrayerActivity.class))
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
    findViewById(R.id.homeGregorian).setOnClickListener(v -> open(CalendarActivity.class));
    findViewById(R.id.homeContinueReading)
        .setOnClickListener(
            v ->
                startActivity(
                    new android.content.Intent(this, QuranActivity.class)
                        .putExtra(QuranActivity.EXTRA_RESUME_READING, true)));
    findViewById(R.id.homeAgendaAll).setOnClickListener(v -> open(StudyActivity.class));
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
    ((TextView) findViewById(R.id.homeLocationLabel)).setText(location.label.split(",")[0]);
    findViewById(R.id.homeLocation)
        .setContentDescription("Lokasi " + location.caption() + ". Ketuk untuk mengubah.");
    sky.post(() -> {
      android.widget.ScrollView homeScroll = findViewById(R.id.homeScroll);
      sky.setRunning(homeScroll.getScrollY() < findViewById(R.id.homeHero).getBottom());
    });
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
    updateAgenda();
    PrayerReminders.sync(this);
    SharedPreferences p = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    ((TextView) findViewById(R.id.homeReadingLabel)).setText(p.contains("last_surah")
            ? R.string.reference_continue : R.string.refresh_home_start_reading);
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
    progressDate = LocationStore.today(this);
    ((TextView) findViewById(R.id.homeProgressPercent)).setText(progress.percent + "%");
    ((TextView) findViewById(R.id.homeProgressSubtitle))
        .setText(
            total == 0
                ? "Mulai dari satu ibadah yang ingin kamu jaga."
                : done + " dari " + total + " ibadah selesai");
    ((TextView) findViewById(R.id.homeChecklist))
        .setText(total == 0 ? R.string.reference_target_first : R.string.reference_target_open);
    findViewById(R.id.homeProgressPercent)
        .setVisibility(total == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
    findViewById(R.id.homeProgressTicks)
        .setVisibility(total == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
    ((SegmentedProgressView) findViewById(R.id.homeProgressTicks)).setProgress(done, total);
    com.zikra.app.faith.FaithSummary faith = com.zikra.app.faith.FaithSummary.load(this, db);
    TextView journal = findViewById(R.id.homeFaithSummary);
    journal.setText(getString(R.string.faith_title) + "\n" + (faith.available
        ? getString(R.string.faith_summary, faith.streak, faith.week)
          + (faith.plan == null ? "" : " · " + faith.read + "/6.236 ayat")
        : getString(R.string.faith_unavailable)));
    journal.setOnClickListener(v -> open(FaithActivity.class));
  }

  private void updateAgenda() {
    com.zikra.app.study.ClassSummary.bind(this, findViewById(R.id.homeClasses));
    findViewById(R.id.homeClassesOpen).setOnClickListener(v -> open(WeeklyClassesActivity.class));
    java.util.List<com.zikra.app.data.Task> tasks = new java.util.ArrayList<>();
    for (com.zikra.app.data.Task task : db.getTasks())
      if (ZikraDbHelper.CATEGORY_STUDY.equals(task.category) && !task.completed) tasks.add(task);
    tasks.sort(com.zikra.app.study.TaskOrder.ACADEMIC);
    LinearLayout container = findViewById(R.id.homeAgendaItems);
    container.removeAllViews();
    ((TextView) findViewById(R.id.homeAgendaCount)).setText(tasks.isEmpty()
        ? getString(R.string.refresh_home_empty_title)
        : getString(R.string.refresh_home_agenda_count, tasks.size()));
    if (tasks.isEmpty()) {
      TextView empty = new TextView(this);
      empty.setText(R.string.refresh_home_empty_caption);
      empty.setTextSize(14);
      empty.setTextColor(getColor(R.color.muted));
      empty.setPadding(dp(16), dp(20), dp(16), dp(20));
      container.addView(empty);
      return;
    }
    for (int i = 0; i < Math.min(3, tasks.size()); i++) {
      com.zikra.app.data.Task task = tasks.get(i);
      if (i > 0) {
        android.view.View divider = new android.view.View(this);
        divider.setBackgroundColor(getColor(R.color.concept_line));
        LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(-1, dp(1));
        line.setMargins(dp(16), 0, dp(16), 0);
        container.addView(divider, line);
      }
      LinearLayout row = new LinearLayout(this);
      row.setOrientation(LinearLayout.VERTICAL);
      row.setPadding(dp(16), dp(16), dp(16), dp(16));
      row.setMinimumHeight(dp(64));
      TextView title = new TextView(this);
      title.setText(task.title);
      title.setTextSize(16);
      title.setTextColor(getColor(R.color.pine));
      title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
      row.addView(title);
      TextView detail = new TextView(this);
      detail.setTextSize(13);
      detail.setTextColor(getColor(R.color.muted));
      detail.setPadding(0, dp(6), 0, 0);
      detail.setText(task.dueAt == 0 ? getString(R.string.refresh_home_no_deadline)
          : getString(R.string.refresh_home_deadline, java.time.Instant.ofEpochMilli(task.dueAt)
              .atZone(location.zone).format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("id-ID")))));
      row.addView(detail);
      row.setBackgroundResource(android.R.drawable.list_selector_background);
      row.setFocusable(true);
      row.setOnClickListener(v -> open(StudyActivity.class));
      container.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }
  }

  /** Keep times readable and reflow shortcuts instead of shrinking the user's larger text. */
  private void adaptHomeForTextSize() {
    float scale = getResources().getConfiguration().fontScale;
    findViewById(R.id.homePrayerStrip).setMinimumWidth(dp(5 * 48 * Math.max(1f, scale)));
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
    if (!LocationStore.today(this).equals(progressDate)) { updateProgress(); updateAgenda(); }
    ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(location.zone);
    loadSky(false);
    if (skyDay != null) sky.setSky(skyDay, now.toLocalTime());
    int hour = now.getHour();
    ((TextView) findViewById(R.id.homeGreeting))
        .setText(
            hour < 11 && hour >= 4
                ? R.string.concept_greeting_morning
                : hour < 15 && hour >= 11
                    ? R.string.concept_greeting_noon
                    : hour < 18 && hour >= 15
                        ? R.string.concept_greeting_afternoon
                        : R.string.concept_greeting_evening);
    ((TextView) findViewById(R.id.homeGregorian))
        .setText(
            now.format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("id-ID"))));
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
    PrayerSchedule.Day timetable = PrayerSchedule.forDate(
        event == null ? now.toLocalDate() : event.time.toLocalDate(), location);
    if (event == null) {
      ((TextView) findViewById(R.id.homeNextPrayer)).setText("—");
      ((TextView) findViewById(R.id.homeNextTime)).setText("");
      ((TextView) findViewById(R.id.homeClock)).setText("—");
      ((TextView) findViewById(R.id.homeCountdown)).setText("Jadwal belum tersedia untuk lokasi ini");
    } else {
      ((TextView) findViewById(R.id.homeNextPrayer)).setText(getString(
          R.string.refresh_home_towards, prayerNames[next]));
      ((TextView) findViewById(R.id.homeNextTime)).setText(getString(
          event.time.toLocalDate().isAfter(now.toLocalDate())
              ? R.string.refresh_home_tomorrow : R.string.refresh_home_at,
          event.time.format(DateTimeFormatter.ofPattern("HH.mm", Locale.ROOT))));
      long seconds = Math.max(0, java.time.Duration.between(now, event.time).getSeconds());
      ((TextView) findViewById(R.id.homeClock))
          .setText(
              String.format(
                  Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60));
      int minutes = event.minutesFrom(now);
      String interval =
          minutes >= 60 ? (minutes / 60 + " jam " + minutes % 60 + " menit") : (minutes + " menit");
      String content =
          prayerNames[next]
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
        slot.getChildAt(3).setVisibility(active ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        slot.setBackgroundResource(0);
        ((ImageView) slot.getChildAt(1))
            .setColorFilter(getColor(active ? R.color.home_prayer_active : R.color.home_prayer_text));
        ((TextView) slot.getChildAt(0))
            .setTextColor(getColor(active ? R.color.home_prayer_active : R.color.home_prayer_text));
        ((TextView) slot.getChildAt(2)).setText(timetable.time(i).replace(':', '.'));
        ((TextView) slot.getChildAt(2))
            .setTextColor(getColor(active ? R.color.home_prayer_active : R.color.white));
        slot.setContentDescription(
            prayerNames[i]
                + " "
                + timetable.time(i)
                + (active ? ", sholat berikutnya" : ""));
      }
    }
  }

  private void loadSky(boolean force) {
    java.time.LocalDate date = ZonedDateTime.now(clock).withZoneSameInstant(location.zone).toLocalDate();
    if (!force && date.equals(requestedSkyDate)) return;
    requestedSkyDate = date;
    skyRepository.load(
        date,
        force,
        location,
        day -> {
          skyDay = day;
          sky.setSky(day, ZonedDateTime.now(clock).withZoneSameInstant(location.zone).toLocalTime());
          findViewById(R.id.homeSkySource)
              .setContentDescription(
                  "Langit · "
                      + (day.usno ? "USNO" : day.solarAvailable ? "perkiraan lokal" : "ilustrasi")
                      + ". Ketuk untuk sumber dan pengaturan pengingat.");
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
