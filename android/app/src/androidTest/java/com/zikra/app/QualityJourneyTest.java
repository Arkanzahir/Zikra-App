package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.data.HifzTarget;
import com.zikra.app.data.ZikraDbHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Actual APK screens; network failures/loading remain visible and are never replaced by fixtures.
 */
@SuppressWarnings("deprecation")
public final class QualityJourneyTest extends InstrumentationTestCase {
  private Activity page;
  private SharedPreferences preferences;
  private Map<String, ?> originalPreferences;
  private ZikraDbHelper database;
  private final ArrayList<Long> fixtureIds = new ArrayList<>();
  private final StringBuilder evidence =
      new StringBuilder(
          "Native APK screenshot journey. [Uji] checklist entries are temporary test data.\n"
              + "Network views use actual live/cache/loading/failure state; screenshots do not"
              + " prove network success.\n");

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    preferences =
        getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    originalPreferences = new HashMap<>(preferences.getAll());
    preferences
        .edit()
        .putBoolean("onboarding_complete", true)
        .putString("reading_theme", "light")
        .commit();
    database = new ZikraDbHelper(getInstrumentation().getTargetContext());
  }

  public void testCaptureActualFeatureJourney() throws Exception {
    long task = fixture("[Uji] Ringkas materi untuk besok", ZikraDbHelper.CATEGORY_STUDY);
    long done = fixture("[Uji] Latihan soal 20 menit", ZikraDbHelper.CATEGORY_STUDY);
    assertTrue(database.setCompleted(done, true));
    fixture("[Uji] Checklist ibadah harian", ZikraDbHelper.CATEGORY_IBADAH);
    fixtureIds.add(
        database.addHifzTask(
            "[Uji] Ulangi Al-Mulk ayat 1–5", new HifzTarget(67, "Al-Mulk", 30, 1, 5)));
    assertTrue(task > 0);

    launch(HomeActivity.class);
    capture("QualityHome");
    launch(StudyActivity.class);
    capture("QualityStudy");
    launch(scopedStudy(ZikraDbHelper.CATEGORY_HIFZ));
    capture("QualityHifz");
    launch(scopedStudy(ZikraDbHelper.CATEGORY_IBADAH));
    capture("QualityIbadah");
    launch(PrayerActivity.class);
    capture("QualityPrayer");

    launch(QuranActivity.class);
    assertEquals(114, ((LinearLayout) page.findViewById(R.id.quranSurahList)).getChildCount());
    capture("QualityQuranCatalog");
    launch(
        new Intent(getInstrumentation().getTargetContext(), QuranActivity.class)
            .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 1)
            .putExtra(QuranActivity.EXTRA_TARGET_VERSE, 1));
    assertTrue(
        "Al-Fatihah is available from the bundled offline content",
        waitUntil(
            15000,
            () -> {
              LinearLayout rows = page.findViewById(R.id.ayahContainer);
              return rows.findViewWithTag("1:1") != null;
            }));
    capture("QualityQuranReader");

    launch(DuaActivity.class);
    LinearLayout duas = page.findViewById(R.id.duaContainer);
    assertTrue(duas.getChildCount() > 0);
    getInstrumentation()
        .runOnMainSync(() -> duas.getChildAt(0).findViewById(R.id.duaExpand).performClick());
    capture("QualityDua");
    launch(HadithActivity.class);
    capture("QualityHadith");
    launch(CalendarActivity.class);
    capture("QualityCalendar");
    launch(ProfileActivity.class);
    capture("QualityProfile");
    launch(AiActivity.class);
    capture("QualityAssistant");
    launch(QiblaActivity.class);
    capture("QualityQibla");
    launch(InfaqActivity.class);
    capture("QualityInfaq");

    launch(ProtectorActivity.class);
    waitUntil(
        12000,
        () ->
            !pending(text(R.id.protectorWeatherStatus))
                && !text(R.id.protectorQuakeArea).startsWith("Memuat"));
    evidence.append("Protector weather: ").append(text(R.id.protectorWeatherStatus)).append('\n');
    evidence.append("Protector earthquake: ").append(text(R.id.protectorQuakeStatus)).append('\n');
    capture("QualityProtector");
    launch(WeatherActivity.class);
    waitUntil(12000, () -> !pending(text(R.id.weatherStatus)));
    evidence.append("Weather: ").append(text(R.id.weatherStatus)).append('\n');
    capture("QualityWeather");
    launch(EarthquakeActivity.class);
    waitUntil(12000, () -> !pending(text(R.id.quakeStatus)));
    evidence.append("Earthquake: ").append(text(R.id.quakeStatus)).append('\n');
    capture("QualityEarthquake");
  }

  private long fixture(String title, String category) {
    long id = database.addTask(title, category);
    fixtureIds.add(id);
    return id;
  }

  private Intent scopedStudy(String category) {
    return new Intent(getInstrumentation().getTargetContext(), StudyActivity.class)
        .putExtra("category", category);
  }

  private void launch(Class<? extends Activity> type) {
    launch(new Intent(getInstrumentation().getTargetContext(), type));
  }

  private void launch(Intent intent) {
    finishPage();
    page = getInstrumentation().startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    assertTrue(
        "Launched Activity must receive focus", waitUntil(15000, () -> page.hasWindowFocus()));
    SystemClock.sleep(800);
  }

  private void capture(String name) {
    UiEvidence.capture(getInstrumentation(), name);
    File image =
        new File(
            getInstrumentation().getTargetContext().getExternalFilesDir(null),
            "validation/" + name + ".png");
    assertTrue("Native screenshot must exist: " + name, image.isFile() && image.length() > 0);
    evidence.append(name).append(" → ").append(page.getClass().getSimpleName()).append('\n');
    Bundle status = new Bundle();
    status.putString("quality_screenshot", name);
    getInstrumentation().sendStatus(0, status);
  }

  private interface Condition {
    boolean ready();
  }

  private boolean waitUntil(long timeout, Condition condition) {
    long deadline = SystemClock.uptimeMillis() + timeout;
    do {
      boolean[] ready = {false};
      getInstrumentation().runOnMainSync(() -> ready[0] = condition.ready());
      if (ready[0]) return true;
      SystemClock.sleep(200);
    } while (SystemClock.uptimeMillis() < deadline);
    return false;
  }

  private String text(int id) {
    if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
      View value = page.findViewById(id);
      return value instanceof TextView ? ((TextView) value).getText().toString() : "";
    }
    String[] result = {""};
    getInstrumentation().runOnMainSync(() -> result[0] = text(id));
    return result[0];
  }

  private boolean pending(String status) {
    String value = status.toLowerCase(java.util.Locale.ROOT);
    return value.contains("menghubung")
        || value.contains("memperbarui")
        || value.startsWith("memuat");
  }

  private void finishPage() {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    page = null;
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      finishPage();
      if (database != null) {
        // Remove only this test's newly inserted IDs, including if midnight passed during capture.
        for (long id : fixtureIds)
          database
              .getWritableDatabase()
              .delete(
                  "tasks", "id = ? AND title LIKE ?", new String[] {Long.toString(id), "[Uji]%"});
        database.close();
      }
      if (preferences != null && originalPreferences != null) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : originalPreferences.entrySet()) {
          String key = entry.getKey();
          Object value = entry.getValue();
          if (value instanceof String) editor.putString(key, (String) value);
          else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
          else if (value instanceof Integer) editor.putInt(key, (Integer) value);
          else if (value instanceof Long) editor.putLong(key, (Long) value);
          else if (value instanceof Float) editor.putFloat(key, (Float) value);
          else if (value instanceof Set)
            editor.putStringSet(key, new HashSet<>((Set<String>) value));
        }
        editor.commit();
      }
      File directory =
          new File(getInstrumentation().getTargetContext().getExternalFilesDir(null), "validation");
      if (directory.exists() || directory.mkdirs())
        try (FileOutputStream output =
            new FileOutputStream(new File(directory, "QualityJourney-status.txt"))) {
          output.write(evidence.toString().getBytes(StandardCharsets.UTF_8));
        }
    } finally {
      super.tearDown();
    }
  }
}
