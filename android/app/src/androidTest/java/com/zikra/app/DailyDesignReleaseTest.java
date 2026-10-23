package com.zikra.app;

import android.app.*;
import android.content.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("deprecation")
public final class DailyDesignReleaseTest extends InstrumentationTestCase {
  private Activity page, reader;

  private <T extends Activity> T launch(Class<T> type) {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), type)
                    .putExtra("edit", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    return type.cast(page);
  }

  private SharedPreferences prefs() {
    return getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
  }

  private void capture(String name) {
    getInstrumentation().waitForIdleSync();
    UiEvidence.capture(getInstrumentation(), name);
  }

  private void click(int id) {
    getInstrumentation().runOnMainSync(() -> page.findViewById(id).performClick());
    getInstrumentation().waitForIdleSync();
  }

  private EditText input(View v) {
    if (v instanceof EditText) return (EditText) v;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        EditText e = input(((ViewGroup) v).getChildAt(i));
        if (e != null) return e;
      }
    return null;
  }

  public void testOnboardingThreeScenesPreservePreferences() {
    String education = prefs().getString("education_level", null);
    launch(OnboardingActivity.class);
    capture("DailyOnboardingWelcome");
    assertTrue(
        page.findViewById(R.id.onboardIllustration)
            .getContentDescription()
            .toString()
            .contains("Masjid"));
    click(R.id.onboardContinue);
    capture("DailyOnboardingRead");
    assertTrue(
        page.findViewById(R.id.onboardIllustration)
            .getContentDescription()
            .toString()
            .contains("membaca"));
    click(R.id.onboardContinue);
    capture("DailyOnboardingStudy");
    assertTrue(
        page.findViewById(R.id.onboardIllustration)
            .getContentDescription()
            .toString()
            .contains("menulis"));
    assertEquals(
        page.getString(R.string.daily_onboard_finish),
        ((TextView) page.findViewById(R.id.onboardContinue)).getText().toString());
    click(R.id.onboardBack);
    assertEquals("02 / 03", ((TextView) page.findViewById(R.id.onboardStep)).getText().toString());
    assertEquals(education, prefs().getString("education_level", null));
  }

  public void testHadithCollectionsSearchReaderAndPersistence() {
    Set<String> original = new HashSet<>(prefs().getStringSet("hadith_bookmarks", new HashSet<>()));
    try {
      launch(HadithActivity.class);
      capture("DailyHadithOverview");
      click(R.id.dailyBookMuslim);
      assertTrue(
          ((TextView) page.findViewById(R.id.hadithCount)).getText().toString().contains("5"));
      getInstrumentation()
          .runOnMainSync(
              () -> ((EditText) page.findViewById(R.id.hadithSearch)).setText("not-a-hadith-9842"));
      assertEquals(View.VISIBLE, page.findViewById(R.id.hadithEmpty).getVisibility());
      getInstrumentation()
          .runOnMainSync(() -> ((EditText) page.findViewById(R.id.hadithSearch)).setText(""));
      LinearLayout list = page.findViewById(R.id.hadithList);
      assertEquals(5, list.getChildCount());
      Instrumentation.ActivityMonitor monitor =
          getInstrumentation().addMonitor(HadithReaderActivity.class.getName(), null, false);
      try {
        getInstrumentation().runOnMainSync(() -> list.getChildAt(0).performClick());
        reader = getInstrumentation().waitForMonitorWithTimeout(monitor, 10000);
        assertNotNull(reader);
        getInstrumentation().waitForIdleSync();
        capture("DailyHadithReader");
        assertTrue(((TextView) reader.findViewById(R.id.dailyReaderArabic)).getText().length() > 0);
        String id = reader.getIntent().getStringExtra("entry");
        boolean before = prefs().getStringSet("hadith_bookmarks", new HashSet<>()).contains(id);
        getInstrumentation()
            .runOnMainSync(() -> reader.findViewById(R.id.dailyReaderSave).performClick());
        assertEquals(
            !before, prefs().getStringSet("hadith_bookmarks", new HashSet<>()).contains(id));
        getInstrumentation().runOnMainSync(reader::finish);
        reader = null;
        getInstrumentation().waitForIdleSync();
        assertTrue(
            ((TextView) page.findViewById(R.id.dailyHadithResume))
                .getText()
                .toString()
                .contains("Niat"));
      } finally {
        getInstrumentation().removeMonitor(monitor);
      }
    } finally {
      prefs().edit().putStringSet("hadith_bookmarks", original).commit();
    }
  }

  public void testDuaFilterAndReadWithoutVoice() {
    launch(DuaActivity.class);
    capture("DailyDuaOverview");
    LinearLayout list = page.findViewById(R.id.duaContainer);
    View first = list.getChildAt(0);
    getInstrumentation().runOnMainSync(() -> first.findViewById(R.id.duaExpand).performClick());
    assertEquals(View.VISIBLE, first.findViewById(R.id.duaContent).getVisibility());
    assertTrue(
        ((TextView) first.findViewById(R.id.duaSource)).getText().toString().contains("20:114"));
    capture("DailyDuaExpanded");
    getInstrumentation()
        .runOnMainSync(
            () -> ((EditText) page.findViewById(R.id.duaSearch)).setText("not-a-dua-7419"));
    assertEquals(View.VISIBLE, page.findViewById(R.id.duaEmpty).getVisibility());
    getInstrumentation()
        .runOnMainSync(() -> ((EditText) page.findViewById(R.id.duaSearch)).setText(""));
    assertEquals(View.VISIBLE, first.getVisibility());
  }

  public void testProfileNameValidationPersistenceAndRealProgress() throws Exception {
    String old = prefs().getString("profile_display_name", null);
    try {
      launch(ProfileActivity.class);
      capture("DailyProfile");
      assertTrue(
          ((TextView) page.findViewById(R.id.profileProgressCaption))
              .getText()
              .toString()
              .contains("tugas akademik"));
      click(R.id.dailyProfileEdit);
      Field field = ProfileActivity.class.getDeclaredField("profileDialog");
      field.setAccessible(true);
      AlertDialog dialog = (AlertDialog) field.get(page);
      EditText edit = input(dialog.getWindow().getDecorView());
      assertNotNull(edit);
      getInstrumentation()
          .runOnMainSync(
              () -> {
                edit.setText("   ");
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
              });
      assertTrue("Blank name stays in editor", dialog.isShowing());
      getInstrumentation()
          .runOnMainSync(
              () -> {
                edit.setText("Pembaca Zikra");
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
              });
      assertEquals("Pembaca Zikra", prefs().getString("profile_display_name", ""));
      assertEquals(
          "Pembaca Zikra",
          ((TextView) page.findViewById(R.id.profileDisplayName)).getText().toString());
      getInstrumentation().runOnMainSync(page::finish);
      launch(ProfileActivity.class);
      assertEquals(
          "Pembaca Zikra",
          ((TextView) page.findViewById(R.id.profileDisplayName)).getText().toString());
      capture("DailyProfileNamed");
    } finally {
      if (old == null) prefs().edit().remove("profile_display_name").commit();
      else prefs().edit().putString("profile_display_name", old).commit();
    }
  }

  @Override
  protected void tearDown() throws Exception {
    if (reader != null) getInstrumentation().runOnMainSync(reader::finish);
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
