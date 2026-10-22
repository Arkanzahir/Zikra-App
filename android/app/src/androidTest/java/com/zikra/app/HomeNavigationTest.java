package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.TextView;
import com.zikra.app.data.ZikraDbHelper;
import java.util.concurrent.atomic.AtomicBoolean;

/** Verifies that the replacement shortcut and the Hijri date open working native pages. */
@SuppressWarnings("deprecation")
public final class HomeNavigationTest extends InstrumentationTestCase {
  private HomeActivity home;
  private Activity destination;
  private String oldSurah;
  private int oldAyah;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    SharedPreferences saved =
        getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    oldSurah = saved.getString("last_surah", "Al-Fatihah");
    oldAyah = saved.getInt("last_ayah", 1);
    saved.edit().putString("last_surah", "Al-Falaq").putInt("last_ayah", 4).commit();
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .commit();
    home =
        (HomeActivity)
            getInstrumentation()
                .startActivitySync(
                    new Intent(getInstrumentation().getTargetContext(), HomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    getInstrumentation().waitForIdleSync();
    awaitWindowFocus(home);
  }

  public void testComfortableHomeOrdersPrayerBeforeCountdown() throws Exception {
    View name = home.findViewById(R.id.homeNextPrayer),
        time = home.findViewById(R.id.homeNextTime),
        clock = home.findViewById(R.id.homeClock);
    assertTrue("Prayer name comes before time", name.getTop() < time.getTop());
    assertTrue("Countdown follows prayer time", time.getBottom() <= clock.getTop());
    assertTrue(
        "Illustrated hero retains reference height",
        home.findViewById(R.id.homeHero).getHeight()
            >= 304 * home.getResources().getDisplayMetrics().density - 1);
    UiEvidence.capture(getInstrumentation(), "ComfortableHome");
  }

  public void testHadithShortcutOpensSearchableHadithPage() {
    openFromHome(R.id.shortcutHadith, HadithActivity.class);
    assertNotNull(destination.findViewById(R.id.hadithSearch));
    assertNotNull(destination.findViewById(R.id.hadithList));
  }

  public void testHijriDateOpensInteractiveCalendar() {
    openFromHome(R.id.homeHijri, CalendarActivity.class);
    assertNotNull(destination.findViewById(R.id.calendarMonth));
    assertTrue(destination.findViewById(R.id.calendarNext).isEnabled());
  }

  public void testDiaryShortcutOpensHifzMode() {
    openFromHome(R.id.shortcutHifz, StudyActivity.class);
    assertEquals(
        "Diary Hifz",
        ((TextView) destination.findViewById(R.id.study_screen_title)).getText().toString());
    assertEquals(ZikraDbHelper.CATEGORY_HIFZ, destination.getIntent().getStringExtra("category"));
    assertEquals(View.GONE, destination.findViewById(R.id.study_filter_study).getVisibility());
    assertTrue(
        ((TextView) destination.findViewById(R.id.study_progress_label))
            .getText()
            .toString()
            .contains("target hafalan"));
    UiEvidence.capture(getInstrumentation(), "DiaryHifz");
  }

  public void testContinueReadingOpensSavedSurahAndAyah() {
    openFromHome(R.id.homeContinueReading, QuranActivity.class);
    assertEquals(View.VISIBLE, destination.findViewById(R.id.quranReader).getVisibility());
    assertEquals(View.GONE, destination.findViewById(R.id.quranCatalog).getVisibility());
    assertTrue(
        ((TextView) destination.findViewById(R.id.surahMetadata))
            .getText()
            .toString()
            .contains("Al-Falaq"));
    android.widget.LinearLayout verses = destination.findViewById(R.id.ayahContainer);
    assertEquals(
        "Bacaan terakhir",
        ((TextView) verses.getChildAt(3).findViewById(R.id.ayahLastReadStatus))
            .getText()
            .toString());
    assertEquals(
        4, destination.getSharedPreferences("zikra_preferences", 0).getInt("last_ayah", 0));
    assertTrue(
        "Resume should scroll to the saved verse",
        ((android.widget.ScrollView) destination.findViewById(R.id.quranScroll)).getScrollY() > 0);
    UiEvidence.capture(getInstrumentation(), "QuranResume");
  }

  public void testPrayerStripOpensDailyCalculatedTimes() {
    openFromHome(R.id.homePrayerStrip, PrayerActivity.class);
    android.widget.LinearLayout rows = destination.findViewById(R.id.prayerRows);
    assertEquals(
        PrayerSchedule.forDate(java.time.LocalDate.now(PrayerSchedule.ZONE)).time(0),
        ((TextView) rows.getChildAt(0).findViewById(R.id.prayerRowTime)).getText().toString());
    assertNotNull(destination.findViewById(R.id.prayerAlarmSettings));
    UiEvidence.capture(getInstrumentation(), "PrayerActivity");
  }

  private void openFromHome(int id, Class<? extends Activity> target) {
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(target.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(() -> home.findViewById(id).performClick());
      destination = getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull("Destination did not open: " + target.getSimpleName(), destination);
      getInstrumentation().waitForIdleSync();
      awaitWindowFocus(destination);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  private void awaitWindowFocus(Activity activity) {
    long deadline = SystemClock.uptimeMillis() + 20000;
    AtomicBoolean focused = new AtomicBoolean();
    while (SystemClock.uptimeMillis() < deadline) {
      getInstrumentation().runOnMainSync(() -> focused.set(activity.hasWindowFocus()));
      if (focused.get()) return;
      SystemClock.sleep(100);
    }
    fail("Window did not receive focus: " + activity.getClass().getSimpleName());
  }

  @Override
  protected void tearDown() throws Exception {
    if (destination != null) {
      getInstrumentation().runOnMainSync(destination::finish);
      getInstrumentation().waitForIdleSync();
      awaitWindowFocus(home);
    }
    if (home != null) getInstrumentation().runOnMainSync(home::finish);
    getInstrumentation().waitForIdleSync();
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putString("last_surah", oldSurah)
        .putInt("last_ayah", oldAyah)
        .commit();
    super.tearDown();
  }
}
