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

  private Intent target(int chapter, int verse) {
    return new Intent(getInstrumentation().getTargetContext(), QuranActivity.class)
        .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, chapter)
        .putExtra(QuranActivity.EXTRA_TARGET_VERSE, verse);
  }

  private void awaitVerse(String key) {
    long end = SystemClock.uptimeMillis() + 35000;
    while (SystemClock.uptimeMillis() < end) {
      boolean[] found = {false};
      getInstrumentation()
          .runOnMainSync(
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
    launch(new Intent(getInstrumentation().getTargetContext(), QuranActivity.class));
    assertEquals(114, ((LinearLayout) page.findViewById(R.id.quranSurahList)).getChildCount());
    UiEvidence.capture(getInstrumentation(), "RebuiltQuranCatalog");
    getInstrumentation()
        .runOnMainSync(() -> ((EditText) page.findViewById(R.id.quranSearch)).setText("baqarah"));
    assertEquals(1, ((LinearLayout) page.findViewById(R.id.quranSurahList)).getChildCount());
  }

  public void testNativeMulkBookmarkRecreateAndCachedResume() throws Exception {
    Intent intent =
        StudyActivity.readingIntent(
            getInstrumentation().getTargetContext(), new HifzTarget(67, "Al-Mulk", 30, 3, 7));
    assertEquals(QuranActivity.class.getName(), intent.getComponent().getClassName());
    launch(intent);
    awaitVerse("67:3");
    getInstrumentation()
        .runOnMainSync(
            () -> ((Switch) page.findViewById(R.id.readingNightSwitch)).setChecked(true));
    SystemClock.sleep(1200);
    UiEvidence.capture(getInstrumentation(), "RebuiltQuranMulk");
    getInstrumentation()
        .runOnMainSync(
            () ->
                ((LinearLayout) page.findViewById(R.id.ayahContainer))
                    .findViewWithTag("67:4")
                    .findViewById(R.id.ayahBookmark)
                    .performClick());
    assertEquals("67:4", prefs.getString("last_verse_key", ""));
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(page::recreate);
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
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.quranPreviousPage).performClick());
    awaitVerse("2:201");
    assertEquals(50, ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount());
    assertTrue(page.findViewById(R.id.quranNextPage).isEnabled());
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
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
