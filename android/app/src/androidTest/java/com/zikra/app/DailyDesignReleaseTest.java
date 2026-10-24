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

  private void waitForHadithUi(java.util.function.BooleanSupplier condition) {
    long deadline = android.os.SystemClock.uptimeMillis() + 10000;
    while (!condition.getAsBoolean() && android.os.SystemClock.uptimeMillis() < deadline) {
      getInstrumentation().waitForIdleSync();
      android.os.SystemClock.sleep(50);
    }
    assertTrue("Hadith UI finishes its asynchronous data request", condition.getAsBoolean());
  }

  public void testHadithCollectionsSearchReaderAndPersistence() throws Exception {
    Set<String> original = new HashSet<>(prefs().getStringSet("hadith_bookmarks", new HashSet<>()));
    String originalLast = prefs().getString("last_hadith_id", null);
    boolean hadSize = prefs().contains("hadith_reader_size");
    int originalSize = prefs().getInt("hadith_reader_size", 0);
    try {
      launch(HadithActivity.class);
      LinearLayout list = page.findViewById(R.id.hadithList);
      waitForHadithUi(() -> list.getChildCount() > 0);
      capture("DailyHadithOverview");
      click(R.id.dailyBookMuslim);
      getInstrumentation().runOnMainSync(
          () -> ((EditText) page.findViewById(R.id.hadithSearch)).setText("not-a-hadith-9842"));
      waitForHadithUi(() -> list.getChildCount() == 0
          && page.findViewById(R.id.hadithEmpty).getVisibility() == View.VISIBLE);
      assertTrue(page.findViewById(R.id.dailyBookMuslim).isActivated());
      getInstrumentation().runOnMainSync(() -> {
        ((EditText) page.findViewById(R.id.hadithSearch)).setText("");
        ((LinearLayout) page.findViewById(R.id.hadithTabs)).getChildAt(0).performClick();
      });
      waitForHadithUi(() -> list.getChildCount() > 0
          && page.findViewById(R.id.hadithEmpty).getVisibility() == View.GONE);
      assertFalse(page.findViewById(R.id.dailyBookMuslim).isActivated());
      String selectedTitle = ((TextView) list.getChildAt(0).findViewById(R.id.hadithItemTitle)).getText().toString();
      Instrumentation.ActivityMonitor monitor =
          getInstrumentation().addMonitor(HadithReaderActivity.class.getName(), null, false);
      try {
        getInstrumentation().runOnMainSync(() -> list.getChildAt(0).performClick());
        reader = getInstrumentation().waitForMonitorWithTimeout(monitor, 10000);
        assertNotNull(reader);
        waitForHadithUi(() -> ((TextView) reader.findViewById(R.id.dailyReaderArabic)).getText().length() > 0);
        capture("DailyHadithReader");
        float beforeSize = ((TextView) reader.findViewById(R.id.dailyReaderArabic)).getTextSize();
        getInstrumentation().runOnMainSync(() -> reader.findViewById(R.id.dailyReaderSize).performClick());
        Field sizeField = HadithReaderActivity.class.getDeclaredField("sizeDialog");
        sizeField.setAccessible(true);
        AlertDialog sizes = (AlertDialog) sizeField.get(reader);
        assertTrue(sizes.isShowing());
        int choice = prefs().getInt("hadith_reader_size", 0) == 0 ? 1 : 0;
        getInstrumentation().runOnMainSync(() -> sizes.getListView().performItemClick(null, choice, choice));
        assertFalse(sizes.isShowing());
        assertTrue(beforeSize != ((TextView) reader.findViewById(R.id.dailyReaderArabic)).getTextSize());
        String arabic = ((TextView) reader.findViewById(R.id.dailyReaderArabic)).getText().toString();
        String reference = ((TextView) reader.findViewById(R.id.dailyReaderSource)).getText().toString();
        assertFalse(((TextView) reader.findViewById(R.id.hadithReaderBook)).getText().toString().isEmpty());
        getInstrumentation().runOnMainSync(() -> reader.findViewById(R.id.dailyReaderCopy).performClick());
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
            getInstrumentation().getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
        String copied = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        assertTrue(copied.contains(arabic)); assertTrue(copied.contains(reference));
        String id = reader.getIntent().getStringExtra("entry");
        boolean before = prefs().getStringSet("hadith_bookmarks", new HashSet<>()).contains(id);
        getInstrumentation().runOnMainSync(() -> reader.findViewById(R.id.dailyReaderSave).performClick());
        assertEquals(!before, prefs().getStringSet("hadith_bookmarks", new HashSet<>()).contains(id));
        getInstrumentation().runOnMainSync(reader::finish);
        reader = null;
        waitForHadithUi(() -> ((TextView) page.findViewById(R.id.dailyHadithResume))
            .getText().toString().contains(selectedTitle));
      } finally {
        getInstrumentation().removeMonitor(monitor);
      }
    } finally {
      SharedPreferences.Editor restore = prefs().edit().putStringSet("hadith_bookmarks", original);
      if (originalLast == null) restore.remove("last_hadith_id"); else restore.putString("last_hadith_id", originalLast);
      if (hadSize) restore.putInt("hadith_reader_size", originalSize); else restore.remove("hadith_reader_size");
      restore.commit();
    }
  }

  public void testDuaFilterAndReadWithoutVoice() {
    launch(DuaActivity.class);
    capture("DailyDuaOverview");
    LinearLayout list = page.findViewById(R.id.duaContainer);
    View first = list.getChildAt(0);
    getInstrumentation()
        .runOnMainSync(
            () ->
                ((ScrollView) page.findViewById(R.id.duaScroll))
                    .scrollTo(0, ((View) page.findViewById(R.id.duaCount).getParent()).getTop()));
    capture("DailyDuaList");
    getInstrumentation().runOnMainSync(() -> first.findViewById(R.id.duaExpand).performClick());
    assertEquals(View.VISIBLE, first.findViewById(R.id.duaContent).getVisibility());
    assertTrue(
        ((TextView) first.findViewById(R.id.duaSource)).getText().toString().contains("20:114"));
    assertEquals(View.GONE, first.findViewById(R.id.duaSourceDetails).getVisibility());
    assertTrue(((TextView) first.findViewById(R.id.duaLatin)).getText().toString().contains("Rabbi"));
    getInstrumentation().runOnMainSync(() -> first.findViewById(R.id.duaSourceToggle).performClick());
    assertEquals(View.VISIBLE, first.findViewById(R.id.duaSourceDetails).getVisibility());
    assertTrue(((TextView) first.findViewById(R.id.duaSourceDetails)).getText().toString().contains("quran.com/20/114"));
    getInstrumentation().runOnMainSync(() -> first.findViewById(R.id.duaSourceToggle).performClick());
    assertEquals(View.GONE, first.findViewById(R.id.duaSourceDetails).getVisibility());
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
