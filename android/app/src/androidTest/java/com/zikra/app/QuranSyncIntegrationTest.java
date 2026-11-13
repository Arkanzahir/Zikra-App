package com.zikra.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.test.InstrumentationTestCase;
import com.zikra.app.reading.*;
import com.zikra.app.reading.mushaf.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.UUID;
import org.json.*;

/** Disposable emulator only; source fixtures are pushed privately, never packaged. */
@SuppressWarnings("deprecation")
public final class QuranSyncIntegrationTest extends InstrumentationTestCase {
  private File fixtures, sandbox;
  private Context context;
  protected void setUp() throws Exception {
    super.setUp();
    assertTrue("Emulator only", android.os.Build.MODEL.contains("sdk")
        || android.os.Build.FINGERPRINT.contains("generic"));
    Context target = getInstrumentation().getTargetContext();
    fixtures = new File(target.getFilesDir(), "quran-sync-fixtures");
    assertTrue(new File(fixtures, "mushaf-pages/golden.json").isFile());
    sandbox = new File(target.getFilesDir(), "quran-sync-test-" + UUID.randomUUID());
    assertTrue(sandbox.mkdir());
    context = new ContextWrapper(target) { public File getFilesDir() { return sandbox; } };
  }
  protected void tearDown() throws Exception {
    if (sandbox != null) erase(sandbox);
    super.tearDown();
  }
  private static void erase(File file) throws IOException {
    if (file.isDirectory()) for (File child : file.listFiles()) erase(child);
    if (!file.delete()) throw new IOException("Test cleanup failed");
  }
  private static String read(File file) throws Exception {
    return QuranSyncStore.readBounded(new FileInputStream(file), 524288);
  }
  private String reader(int chapter, int page) throws Exception {
    return read(new File(fixtures, "reader-pages/" + chapter + "-" + page + ".json"));
  }
  private void rejected(QuranSyncStore store, QuranChapters.Chapter chapter, QuranSyncStore.Source source,
      QuranSyncStore.Check check) throws Exception {
    boolean failed = false;
    try { store.download(chapter, source, check); } catch (Exception expected) { failed = true; }
    assertTrue("Incomplete generation must not publish", failed);
  }
  public void testAtomicMigrationFailuresAndRecoveryPreserveExistingData() throws Exception {
    QuranChapters.Chapter chapter = QuranChapters.all(context).get(1);
    File legacy = new File(sandbox, "quran-offline-r33"); assertTrue(legacy.mkdir());
    File bookmark = new File(legacy, "user-bookmark.json");
    Files.write(bookmark.toPath(), "preserve-user-state".getBytes(StandardCharsets.UTF_8));
    QuranSyncStore store = new QuranSyncStore(context);
    store.download(chapter, page -> reader(2, page), () -> {});
    String old = store.read(chapter, 6);
    rejected(store, chapter, page -> { if (page == 4) throw new IOException("network interrupted"); return reader(2, page); }, () -> {});
    assertEquals(old, store.read(chapter, 6));
    rejected(store, chapter, page -> {
      JSONObject json = new JSONObject(reader(2, page));
      if (page == 2) json.getJSONObject("zikra_sync").put("checked_at",
          json.getJSONObject("zikra_sync").getLong("checked_at") - 1000);
      return json.toString();
    }, () -> {});
    final int[] checks = {0};
    rejected(store, chapter, page -> reader(2, page), () -> { if (++checks[0] == 8) throw new IOException("cancel before commit"); });
    assertEquals(old, new QuranSyncStore(context).read(chapter, 6));
    File root = new File(sandbox, "quran-content-sync-r33");
    File orphan = new File(root, UUID.randomUUID().toString()); assertTrue(orphan.mkdir());
    Files.write(new File(orphan, ".owner").toPath(), "zikra-quran-sync-v1".getBytes(StandardCharsets.UTF_8));
    Files.write(new File(orphan, "1.json").toPath(), "interrupted".getBytes(StandardCharsets.UTF_8));
    File unknown = new File(root, UUID.randomUUID().toString()); assertTrue(unknown.mkdir());
    Files.write(new File(unknown, "private.txt").toPath(), new byte[]{1});
    assertEquals(old, new QuranSyncStore(context).read(chapter, 6));
    assertFalse(orphan.exists()); assertTrue(unknown.exists());
    assertEquals("preserve-user-state", read(bookmark));
  }
  public void testAllSourcePagesPreserveNativeGlyphOrderAndReaderVerses() throws Exception {
    JSONArray golden = new JSONArray(read(new File(fixtures, "mushaf-pages/golden.json")));
    assertEquals(604, golden.length()); int words = 0, verses = 0;
    for (int page = 1; page <= 604; page++) {
      String json = read(new File(fixtures, "mushaf-pages/" + page + ".json"));
      QuranSyncPage.validateMetadata(json, System.currentTimeMillis());
      MushafDocument document = MushafDocument.parse(json, page);
      StringBuilder glyphs = new StringBuilder(); int count = 0;
      for (MushafPage.Line line : document.page.lines) for (MushafPage.Word word : line.words) {
        glyphs.append(word.glyph); count++;
      }
      assertEquals(golden.getJSONObject(page - 1).getInt("words"), count);
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(glyphs.toString().getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(); for (byte value : hash) hex.append(String.format("%02x", value & 255));
      assertEquals("Glyphs page " + page, golden.getJSONObject(page - 1).getString("glyphSha256"), hex.toString());
      words += count;
    }
    assertEquals(83665, words);
    for (QuranChapters.Chapter chapter : QuranChapters.all(context)) {
      for (int page = 1; page <= (chapter.count + 49) / 50; page++) {
        String json = reader(chapter.id, page);
        QuranSyncPage.validate(json, chapter, page, System.currentTimeMillis());
        verses += new JSONObject(json).getJSONArray("verses").length();
      }
    }
    assertEquals(6236, verses);
  }
  public void testStoragePublishFailureAndCorruptPageNeverReplaceValidData() throws Exception {
    QuranChapters.Chapter chapter = QuranChapters.all(context).get(1);
    QuranSyncStore store = new QuranSyncStore(context);
    store.download(chapter, page -> reader(2, page), () -> {});
    File root = new File(sandbox, "quran-content-sync-r33");
    File manifest = new File(root, "chapter-2.json");
    String before = read(manifest), pageBefore = store.read(chapter, 1);
    File blockedWrite = new File(root, "chapter-2.json.new"); assertTrue(blockedWrite.mkdir());
    Files.write(new File(blockedWrite, "blocked").toPath(), new byte[]{1});
    rejected(store, chapter, page -> reader(2, page), () -> {});
    assertEquals(before, read(manifest)); assertEquals(pageBefore, store.read(chapter, 1));
    erase(blockedWrite);
    store.download(chapter, page -> reader(2, page), () -> {});
    JSONObject current = new JSONObject(read(manifest));
    assertNotNull(current.getString("previous"));
    File corrupt = new File(new File(root, current.getString("generation")), "1.json");
    Files.write(corrupt.toPath(), "corrupt".getBytes(StandardCharsets.UTF_8));
    boolean refused = false;
    try { store.read(chapter, 1); } catch (Exception expected) { refused = true; }
    assertTrue(refused);
    assertTrue(new File(root, current.getString("previous") + "/1.json").isFile());
  }
  public void testZReaderBindsSynchronizedChapterWithoutNetwork() throws Exception {
    Context target = getInstrumentation().getTargetContext();
    assertFalse("Fresh emulator only", new File(target.getFilesDir(), "quran-content-sync-r33").exists());
    QuranChapters.Chapter chapter = QuranChapters.all(target).get(1);
    new QuranSyncStore(target).download(chapter, page -> reader(2, page), () -> {});
    android.app.Activity activity = getInstrumentation().startActivitySync(
        new android.content.Intent(target, QuranActivity.class)
            .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 2)
            .putExtra(QuranActivity.EXTRA_TARGET_VERSE, 1)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    android.os.SystemClock.sleep(1800);
    getInstrumentation().runOnMainSync(() -> {
      android.view.ViewGroup rows = activity.findViewById(R.id.ayahContainer);
      assertEquals(50, rows.getChildCount());
      android.widget.TextView arabic = rows.getChildAt(0).findViewById(R.id.ayahArabic);
      assertNotNull(arabic.getTypeface()); assertTrue(arabic.getText().length() > 0);
    });
  }
  public void testMidPageFilesystemWriteFailureKeepsCommittedGeneration() throws Exception {
    QuranChapters.Chapter chapter = QuranChapters.all(context).get(1);
    QuranSyncStore store = new QuranSyncStore(context);
    store.download(chapter, page -> reader(2, page), () -> {});
    File root = new File(sandbox, "quran-content-sync-r33");
    File manifest = new File(root, "chapter-2.json");
    String baseline = read(manifest), pageBefore = store.read(chapter, 2);
    String committed = new JSONObject(baseline).getString("generation");
    File userFile = new File(sandbox, "bookmark-kept.json");
    Files.write(userFile.toPath(), "2:12".getBytes(StandardCharsets.UTF_8));
    final boolean[] reachedPartialWrite = {false};
    rejected(store, chapter, page -> {
      if (page == 2) for (File staging : root.listFiles()) {
        if (staging.isDirectory() && !staging.getName().equals(committed)) {
          assertTrue(new File(staging, "1.json").isFile());
          assertTrue(new File(staging, "2.json").mkdir());
          reachedPartialWrite[0] = true;
        }
      }
      return reader(2, page);
    }, () -> {});
    assertTrue(reachedPartialWrite[0]);
    assertEquals(baseline, read(manifest));
    assertEquals(pageBefore, new QuranSyncStore(context).read(chapter, 2));
    assertEquals("2:12", read(userFile));
  }
  public void testLowSpaceGuardDoesNotFetchOrReplaceCommittedData() throws Exception {
    QuranChapters.Chapter chapter = QuranChapters.all(context).get(1);
    QuranSyncStore store = new QuranSyncStore(context);
    store.download(chapter, page -> reader(2, page), () -> {});
    File root = new File(sandbox, "quran-content-sync-r33");
    String manifest = read(new File(root, "chapter-2.json")), pageBefore = store.read(chapter, 1);
    int entries = root.listFiles().length;
    File lowSpace = new File(root.getAbsolutePath()) {
      @Override public long getUsableSpace() { return 5L * 1024 * 1024; }
    };
    java.lang.reflect.Field field = QuranSyncStore.class.getDeclaredField("root");
    field.setAccessible(true); field.set(store, lowSpace);
    final boolean[] fetched = {false};
    try {
      rejected(store, chapter, page -> { fetched[0] = true; return reader(2, page); }, () -> {});
    } finally { field.set(store, root); }
    assertFalse("Space guard must run before any source request", fetched[0]);
    assertEquals(entries, root.listFiles().length);
    assertEquals(manifest, read(new File(root, "chapter-2.json")));
    assertEquals(pageBefore, new QuranSyncStore(context).read(chapter, 1));
  }
}
