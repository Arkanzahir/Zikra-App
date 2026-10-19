package com.zikra.app;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.*;
import com.zikra.app.data.HifzTarget;
import com.zikra.app.reading.*;
import java.io.*;
import java.time.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class RebuiltQuranTest extends InstrumentationTestCase {
  private Activity page;
  private SharedPreferences prefs;
  private Map<String, ?> original;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    prefs = getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    original = new HashMap<>(prefs.getAll());
  }

  private void launch(Intent intent) {
    page = getInstrumentation().startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }

  private void onMain(Runnable action) {
    java.util.concurrent.atomic.AtomicReference<Throwable> failure =
        new java.util.concurrent.atomic.AtomicReference<>();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                action.run();
              } catch (Throwable error) {
                failure.set(error);
              }
            });
    if (failure.get() != null) throw new AssertionError(failure.get());
  }

  private Intent target(int chapter, int verse) {
    return new Intent(getInstrumentation().getTargetContext(), QuranActivity.class)
        .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, chapter)
        .putExtra(QuranActivity.EXTRA_TARGET_VERSE, verse);
  }

  private void awaitVerse(String key) {
    long end = SystemClock.uptimeMillis() + 35000;
    while (SystemClock.uptimeMillis() < end) {
      boolean[] found = {false};
      onMain(
          () ->
              found[0] =
                  ((LinearLayout) page.findViewById(R.id.ayahContainer)).findViewWithTag(key)
                      != null);
      if (found[0]) return;
      SystemClock.sleep(100);
    }
    fail("Missing " + key + ": " + ((TextView) page.findViewById(R.id.quranLoadStatus)).getText());
  }

  private String asset(String name) throws Exception {
    try (InputStream in = getInstrumentation().getContext().getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[4096];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
      return out.toString("UTF-8");
    }
  }

  public void testCatalogAndStrictPageValidation() throws Exception {
    List<QuranChapters.Chapter> all = QuranChapters.all(getInstrumentation().getTargetContext());
    assertEquals(114, all.size());
    int total = 0;
    for (QuranChapters.Chapter c : all) total += c.count;
    assertEquals(6236, total);
    String json = asset("quran-baqarah-page6.json");
    QuranPage p = QuranPage.parse(json, all.get(1), 6);
    assertEquals(36, p.verses.size());
    assertEquals("2:286", p.verses.get(35).key);
    try {
      QuranPage.parse(json.replace("2:251", "67:251"), all.get(1), 6);
      fail("Wrong verse key");
    } catch (org.json.JSONException expected) {
    }
    try {
      QuranPage.parse(
          json.replace("\"total_records\":286", "\"total_records\":285"), all.get(1), 6);
      fail("Wrong count");
    } catch (org.json.JSONException expected) {
    }
    prefs
        .edit()
        .putInt("last_chapter", 67)
        .putString("last_surah", "Al-Mulk")
        .putInt("last_ayah", 4)
        .commit();
    launch(new Intent(getInstrumentation().getTargetContext(), QuranActivity.class));
    String resume = ((TextView) page.findViewById(R.id.lastReadLabel)).getText().toString();
    assertTrue("Saved chapter appears on first catalog open", resume.contains("Al-Mulk"));
    assertTrue("Saved verse appears on first catalog open", resume.contains("Ayat 4"));
    assertEquals(114, ((LinearLayout) page.findViewById(R.id.quranSurahList)).getChildCount());
    UiEvidence.capture(getInstrumentation(), "RebuiltQuranCatalog");
    assertEquals(View.GONE, page.findViewById(R.id.quranSearchCard).getVisibility());
    onMain(() -> page.findViewById(R.id.quranHeaderSearch).performClick());
    assertEquals(View.VISIBLE, page.findViewById(R.id.quranSearchCard).getVisibility());
    onMain(() -> ((EditText) page.findViewById(R.id.quranSearch)).setText("al baqarah"));
    assertEquals(1, ((LinearLayout) page.findViewById(R.id.quranSurahList)).getChildCount());
  }

  public void testNativeMulkBookmarkRecreateAndCachedResume() throws Exception {
    Intent intent =
        StudyActivity.readingIntent(
            getInstrumentation().getTargetContext(), new HifzTarget(67, "Al-Mulk", 30, 3, 7));
    assertEquals(QuranActivity.class.getName(), intent.getComponent().getClassName());
    launch(intent);
    awaitVerse("67:3");
    onMain(() -> ((Switch) page.findViewById(R.id.readingNightSwitch)).setChecked(true));
    SystemClock.sleep(1200);
    UiEvidence.capture(getInstrumentation(), "RebuiltQuranMulk");
    onMain(
        () ->
            ((LinearLayout) page.findViewById(R.id.ayahContainer))
                .findViewWithTag("67:4")
                .findViewById(R.id.ayahBookmark)
                .performClick());
    assertEquals("67:4", prefs.getString("last_verse_key", ""));
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      onMain(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    awaitVerse("67:4");
    assertEquals("67:4", prefs.getString("last_verse_key", ""));
    assertTrue(
        ((TextView) page.findViewById(R.id.quranLoadStatus))
            .getText()
            .toString()
            .contains("tersimpan"));
    assertEquals(
        View.GONE, page.getWindow().getDecorView().findViewWithTag("zikraDock").getVisibility());
  }

  public void testBaqarahLastPageAndPreviousPageLive() {
    launch(target(2, 280));
    awaitVerse("2:280");
    assertEquals(36, ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount());
    assertFalse(page.findViewById(R.id.quranNextPage).isEnabled());
    UiEvidence.capture(getInstrumentation(), "RebuiltQuranBaqarah280");
    onMain(() -> page.findViewById(R.id.quranPreviousPage).performClick());
    awaitVerse("2:201");
    assertEquals(50, ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount());
    assertTrue(page.findViewById(R.id.quranNextPage).isEnabled());
  }

  public void testDefaultLightHeaderAndPersistedNightOption() throws Exception {
    prefs.edit().remove("reading_theme").commit();
    launch(target(1, 1));
    awaitVerse("1:1");
    assertFalse(((Switch) page.findViewById(R.id.readingNightSwitch)).isChecked());
    assertEquals(android.graphics.Color.WHITE, page.getWindow().getStatusBarColor());
    android.graphics.Rect bounds = new android.graphics.Rect();
    assertTrue(
        "Surah introduction remains visible on first verse",
        page.findViewById(R.id.surahMetadata).getGlobalVisibleRect(bounds));
    assertTrue(
        ((TextView) page.findViewById(R.id.surahMetadata))
            .getText()
            .toString()
            .contains("Al-Fatihah"));
    UiEvidence.capture(getInstrumentation(), "RebuiltQuranReaderLight");
    onMain(() -> page.findViewById(R.id.quranReadingOptions).performClick());
    getInstrumentation().waitForIdleSync();
    boolean clicked = false;
    for (android.view.accessibility.AccessibilityNodeInfo node :
        getInstrumentation()
            .getUiAutomation()
            .getRootInActiveWindow()
            .findAccessibilityNodeInfosByText("Malam")) {
      for (int i = 0; node != null && i < 4; i++, node = node.getParent()) {
        if (node.isClickable()) {
          clicked =
              node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
          break;
        }
      }
      if (clicked) break;
    }
    assertTrue("Night option accepts user selection", clicked);
    getInstrumentation().waitForIdleSync();
    assertEquals("dark", prefs.getString("reading_theme", ""));
    assertTrue(((Switch) page.findViewById(R.id.readingNightSwitch)).isChecked());
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      onMain(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    awaitVerse("1:1");
    assertTrue(((Switch) page.findViewById(R.id.readingNightSwitch)).isChecked());
  }

  public void testArabicSearchAndReadableSizesSurviveRecreation() throws Exception {
    launch(new Intent(getInstrumentation().getTargetContext(), QuranActivity.class));
    onMain(
        () -> {
          page.findViewById(R.id.quranHeaderSearch).performClick();
          ((EditText) page.findViewById(R.id.quranSearch)).setText("البقرة");
          LinearLayout results = page.findViewById(R.id.quranSurahList);
          assertEquals(1, results.getChildCount());
          assertEquals(
              "Al-Baqarah",
              ((TextView) results.getChildAt(0).findViewById(R.id.surahListName))
                  .getText()
                  .toString());
          ((EditText) page.findViewById(R.id.quranSearch)).setText("Al Fatihah");
          assertEquals(1, results.getChildCount());
          results.getChildAt(0).performClick();
        });
    awaitVerse("1:1");
    onMain(
        () -> {
          page.findViewById(R.id.quranReadingOptions).performClick();
          AlertDialog settings = dialog("readingDialog");
          ((SeekBar) settings.findViewById(R.id.quranArabicSize)).setProgress(12);
          ((SeekBar) settings.findViewById(R.id.quranTranslationSize)).setProgress(6);
          assertEquals(36, prefs.getInt("reading_arabic_size", 0));
          assertEquals(20, prefs.getInt("reading_translation_size", 0));
          settings.dismiss();
        });
    recreateReader();
    awaitVerse("1:1");
    onMain(
        () -> {
          View row = ((LinearLayout) page.findViewById(R.id.ayahContainer)).findViewWithTag("1:1");
          float density = page.getResources().getDisplayMetrics().scaledDensity;
          assertEquals(
              36f, ((TextView) row.findViewById(R.id.ayahArabic)).getTextSize() / density, .1f);
          assertEquals(
              20f,
              ((TextView) row.findViewById(R.id.ayahTranslation)).getTextSize() / density,
              .1f);
        });
    UiEvidence.capture(getInstrumentation(), "ImprovedQuranReadableSizes");
  }

  public void testVerseJumpValidatesBoundsAndOpensCorrectPart() throws Exception {
    launch(target(2, 1));
    awaitVerse("2:1");
    onMain(() -> page.findViewById(R.id.quranJumpToVerse).performClick());
    getInstrumentation().waitForIdleSync();
    onMain(
        () -> {
          AlertDialog jump = dialog("jumpDialog");
          EditText input = jump.findViewById(R.id.quranJumpInput);
          input.setText("0");
          jump.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
          assertTrue(jump.isShowing());
          assertNotNull(input.getError());
          input.setText("287");
          jump.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
          assertTrue(jump.isShowing());
          assertNotNull(input.getError());
          input.setText("280");
          jump.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
          assertFalse(jump.isShowing());
        });
    awaitVerse("2:280");
    assertEquals(280, prefs.getInt("last_ayah", 0));
    assertEquals("2:280", prefs.getString("last_verse_key", ""));
    assertTrue(
        ((TextView) page.findViewById(R.id.quranPageLabel))
            .getText()
            .toString()
            .contains("251–286"));
    assertEquals(36, ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount());
    recreateReader();
    awaitVerse("2:280");
    assertEquals(280, prefs.getInt("last_ayah", 0));
    UiEvidence.capture(getInstrumentation(), "ImprovedQuranVerseJump");
  }

  private AlertDialog dialog(String name) {
    try {
      java.lang.reflect.Field field = QuranActivity.class.getDeclaredField(name);
      field.setAccessible(true);
      return (AlertDialog) field.get(page);
    } catch (ReflectiveOperationException error) {
      throw new AssertionError(error);
    }
  }

  private void recreateReader() {
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      onMain(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) onMain(page::finish);
    getInstrumentation().waitForIdleSync();
    SharedPreferences.Editor e = prefs.edit().clear();
    for (Map.Entry<String, ?> x : original.entrySet()) {
      String k = x.getKey();
      Object v = x.getValue();
      if (v instanceof String) e.putString(k, (String) v);
      else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
      else if (v instanceof Integer) e.putInt(k, (Integer) v);
      else if (v instanceof Long) e.putLong(k, (Long) v);
      else if (v instanceof Float) e.putFloat(k, (Float) v);
      else if (v instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) v));
    }
    e.commit();
    super.tearDown();
  }
}
