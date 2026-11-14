package com.zikra.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.test.InstrumentationTestCase;
import com.zikra.app.reading.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import org.json.JSONObject;

/** Private host-pushed fixtures and a unique scratch context; never the user's Quran directory. */
@SuppressWarnings("deprecation")
public final class QuranManifestRecoveryTest extends InstrumentationTestCase {
  private File scratch, fixtures, root;
  private Context context;
  private QuranChapters.Chapter chapter;
  private android.app.Activity foreground;
  protected void setUp() throws Exception {
    super.setUp(); Context target = getInstrumentation().getTargetContext();
    fixtures = new File(target.getCacheDir(), "quran-manifest-fixtures");
    assertTrue(new File(fixtures, "host-authorized").isFile());
    foreground = getInstrumentation().startActivitySync(new android.content.Intent(target, CalendarActivity.class)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    scratch = new File(target.getCacheDir(), "quran-manifest-test-" + UUID.randomUUID());
    assertTrue(scratch.mkdir());
    context = new ContextWrapper(target) { public File getFilesDir() { return scratch; } };
    root = new File(scratch, "quran-content-sync-r33");
    chapter = QuranChapters.all(context).get(1);
  }
  protected void tearDown() throws Exception {
    try { if (scratch != null) remove(scratch); }
    finally {
      if (foreground != null) getInstrumentation().runOnMainSync(foreground::finish);
      super.tearDown();
    }
  }
  private void remove(File file) throws IOException {
    if (file.isDirectory()) for (File child : file.listFiles()) remove(child);
    if (!file.delete()) throw new IOException("Scratch cleanup failed");
  }
  private String read(File file) throws Exception {
    return QuranSyncStore.readBounded(new FileInputStream(file), 524288);
  }
  private String page(int id, int number) throws Exception { return read(new File(fixtures, id + "-" + number + ".json")); }
  private File pointer(int id) { return new File(root, "chapter-" + id + ".json"); }
  private void write(File file, String value) throws Exception { Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8)); }
  private void rejected(QuranSyncStore store, QuranSyncStore.Source source) throws Exception {
    try { store.download(chapter, source, () -> {}); fail("Invalid generation must not publish"); }
    catch (IOException | org.json.JSONException | IllegalArgumentException expected) { }
  }
  public void testCorruptManifestCanBeRepairedOnlyByACompleteValidDownload() throws Exception {
    QuranSyncStore store = new QuranSyncStore(context); store.download(chapter, n -> page(2, n), () -> {});
    JSONObject old = new JSONObject(read(pointer(2))); File retained = new File(root, old.getString("generation") + "/1.json");
    String original = read(retained); write(pointer(2), "{broken");
    rejected(store, n -> n == 3 ? "broken" : page(2, n));
    assertEquals("{broken", read(pointer(2))); assertEquals(original, read(retained));
    store.download(chapter, n -> page(2, n), () -> {});
    assertEquals(page(2, 1), store.read(chapter, 1));
    assertEquals(original, read(retained));
  }
  public void testInvalidSchemaAndOversizedManifestCannotPermanentlyBlockRepair() throws Exception {
    QuranSyncStore store = new QuranSyncStore(context); store.download(chapter, n -> page(2, n), () -> {});
    JSONObject old = new JSONObject(read(pointer(2)));
    write(pointer(2), old.put("schema", 2).put("checked_at", System.currentTimeMillis() + 86400000).toString());
    store.download(chapter, n -> page(2, n), () -> {});
    assertEquals(page(2, 6), store.read(chapter, 6));
    write(pointer(2), new String(new char[5000]).replace('\0', 'x'));
    store.download(chapter, n -> page(2, n), () -> {});
    assertEquals(page(2, 1), store.read(chapter, 1));
  }
  public void testValidNewerGenerationCannotBeReplacedByOlderSource() throws Exception {
    QuranSyncStore store = new QuranSyncStore(context); store.download(chapter, n -> page(2, n), () -> {});
    String before = read(pointer(2));
    rejected(store, n -> {
      JSONObject json = new JSONObject(page(2, n)); JSONObject metadata = json.getJSONObject("zikra_sync");
      metadata.put("checked_at", metadata.getLong("checked_at") - 1000); return json.toString();
    });
    assertEquals(before, read(pointer(2))); assertEquals(page(2, 6), store.read(chapter, 6));
  }
  public void testMismatchedManifestSourceTimeCannotPermanentlyBlockRepair() throws Exception {
    QuranSyncStore store = new QuranSyncStore(context); store.download(chapter, n -> page(2, n), () -> {});
    JSONObject old = new JSONObject(read(pointer(2)));
    old.put("checked_at", old.getLong("checked_at") + 1000); write(pointer(2), old.toString());
    store.download(chapter, n -> page(2, n), () -> {});
    assertEquals(page(2, 1), store.read(chapter, 1));
  }
  public void testInterruptedRefreshAndFinalCancellationKeepCommittedData() throws Exception {
    QuranSyncStore store = new QuranSyncStore(context); store.download(chapter, n -> page(2, n), () -> {});
    String before = read(pointer(2)), last = store.read(chapter, 6);
    File bookmark = new File(scratch, "user-bookmark.json"); write(bookmark, "2:12");
    rejected(store, n -> { if (n == 3) throw new IOException("Interrupted refresh"); return page(2, n); });
    final int[] checks = {0}; boolean cancelled = false;
    try { store.download(chapter, n -> page(2, n), () -> {
      if (++checks[0] == 9) throw new IOException("Cancelled at final commit check");
    }); } catch (IOException expected) { cancelled = true; }
    assertTrue("Cancellation is checked while holding the commit lock", cancelled);
    assertEquals(before, read(pointer(2))); assertEquals(last, store.read(chapter, 6));
    assertEquals("2:12", read(bookmark));
  }
  public void testAtomicPointerWriteFailureKeepsCommittedGeneration() throws Exception {
    QuranSyncStore store = new QuranSyncStore(context); store.download(chapter, n -> page(2, n), () -> {});
    String before = read(pointer(2)), first = store.read(chapter, 1);
    File blocker = new File(root, "chapter-2.json.new"); assertTrue(blocker.mkdir());
    write(new File(blocker, "occupied"), "test-only");
    rejected(store, n -> page(2, n));
    assertEquals(before, read(pointer(2))); assertEquals(first, store.read(chapter, 1));
  }
  public void testObsoletePointerCannotDeleteAnotherChaptersCommittedGeneration() throws Exception {
    QuranSyncStore store = new QuranSyncStore(context); store.download(chapter, n -> page(2, n), () -> {});
    String committed = read(pointer(2)), original = store.read(chapter, 1);
    QuranChapters.Chapter first = QuranChapters.all(context).get(0);
    store.download(first, n -> page(1, n), () -> {});
    JSONObject borrowed = new JSONObject(read(pointer(1)));
    borrowed.put("previous", new JSONObject(committed).getString("generation")); write(pointer(1), borrowed.toString());
    store.download(first, n -> page(1, n), () -> {});
    assertEquals(committed, read(pointer(2))); assertEquals(original, store.read(chapter, 1));
  }
}
