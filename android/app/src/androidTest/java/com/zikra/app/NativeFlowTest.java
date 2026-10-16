package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.widget.Spinner;
import android.widget.TextView;

/** Device smoke checks use real Activities, views and SharedPreferences. */
@SuppressWarnings("deprecation")
public final class NativeFlowTest extends InstrumentationTestCase {
  private Activity launched;

  private <T extends Activity> T launch(Class<T> type) {
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .commit();
    Intent intent = new Intent(getInstrumentation().getTargetContext(), type);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    launched = getInstrumentation().startActivitySync(intent);
    getInstrumentation().waitForIdleSync();
    UiEvidence.capture(getInstrumentation(), type.getSimpleName());
    return type.cast(launched);
  }

  @Override
  protected void tearDown() throws Exception {
    if (launched != null) getInstrumentation().runOnMainSync(() -> launched.finish());
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }

  public void testHomeHasAllEightNativeShortcutsAndDatabaseProgress() {
    HomeActivity home = launch(HomeActivity.class);
    int[] shortcuts = {
      R.id.shortcutQuran,
      R.id.shortcutDua,
      R.id.shortcutQibla,
      R.id.shortcutPrayer,
      R.id.shortcutAi,
      R.id.shortcutProtector,
      R.id.shortcutStudy,
      R.id.shortcutHadith
    };
    for (int id : shortcuts) assertNotNull(home.findViewById(id));
    assertNotNull(home.findViewById(R.id.homeProgressTicks));
    assertTrue(
        ((TextView) home.findViewById(R.id.homeProgressPercent))
            .getText()
            .toString()
            .endsWith("%"));
  }

  public void testQuranSurahSelectionPersistsAcrossRelaunch() {
    QuranActivity quran = launch(QuranActivity.class);
    getInstrumentation()
        .runOnMainSync(() -> ((Spinner) quran.findViewById(R.id.surahSelector)).setSelection(1));
    getInstrumentation().waitForIdleSync();
    assertEquals(
        "Al-Ikhlas",
        quran.getSharedPreferences("zikra_preferences", 0).getString("last_surah", ""));
    getInstrumentation().runOnMainSync(quran::finish);
    getInstrumentation().waitForIdleSync();
    QuranActivity reopened = launch(QuranActivity.class);
    assertEquals(
        1, ((Spinner) reopened.findViewById(R.id.surahSelector)).getSelectedItemPosition());
  }

  public void testQiblaInflatesWithoutPhysicalSensor() {
    assertNotNull(launch(QiblaActivity.class).findViewById(R.id.toolbarBack));
  }

  public void testStudyInflatesAndShowsDailyDatabase() {
    assertNotNull(launch(StudyActivity.class).findViewById(R.id.study_tasks));
  }

  public void testProtectorInflatesWhileNetworkIsUnavailable() {
    assertNotNull(launch(ProtectorActivity.class).findViewById(R.id.protectorDial));
  }

  public void testDuaInflatesWithoutArabicTtsEngine() {
    assertNotNull(launch(DuaActivity.class).findViewById(R.id.toolbarBack));
  }

  public void testSecondaryActivitiesInflate() {
    Class<?>[] types = {
      PrayerActivity.class, ProfileActivity.class, AiActivity.class, InfaqActivity.class
    };
    for (Class<?> type : types) {
      Activity page = launch(type.asSubclass(Activity.class));
      assertNotNull(page.findViewById(R.id.detailTitle));
      getInstrumentation().runOnMainSync(page::finish);
      getInstrumentation().waitForIdleSync();
    }
  }
}
