package com.zikra.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.widget.TextView;
import com.zikra.app.discovery.HadithCollection;
import com.zikra.app.discovery.HadithStore;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("deprecation")
public final class HadithContentTest extends InstrumentationTestCase {
  private Context context;
  private HadithStore store;
  private SharedPreferences prefs;
  private Set<String> originalSaved;
  private String lastId;
  private Activity page;
  private boolean originalBukhari, originalMuslim;

  @Override protected void setUp() throws Exception {
    super.setUp(); context = getInstrumentation().getTargetContext();
    assertTrue("Content fixtures run only on an emulator", android.os.Build.FINGERPRINT.contains("generic")
        || android.os.Build.MODEL.contains("sdk"));
    prefs = context.getSharedPreferences("zikra_preferences", 0);
    originalSaved = new HashSet<>(prefs.getStringSet("hadith_bookmarks", new HashSet<>()));
    lastId = prefs.getString("last_hadith_id", null);
    store = new HadithStore(context);
    HadithStore.Page initial = query("", "Semua", originalSaved, 0);
    originalBukhari = initial.bukhariInstalled; originalMuslim = initial.muslimInstalled;
  }

  private HadithStore.Page query(String text, String category, Set<String> saved, int offset) throws Exception {
    AtomicReference<HadithStore.Page> value = new AtomicReference<>();
    CountDownLatch done = new CountDownLatch(1);
    store.query(text, category, saved, offset, new HadithStore.Result<HadithStore.Page>() {
      public void show(HadithStore.Page page) { value.set(page); done.countDown(); }
      public void failed() { done.countDown(); }
    });
    assertTrue("Local page query finishes", done.await(15, TimeUnit.SECONDS));
    assertNotNull("Local query succeeds", value.get()); return value.get();
  }

  private void download(String book) throws Exception {
    CountDownLatch done = new CountDownLatch(1);
    AtomicReference<String> failure = new AtomicReference<>();
    store.download(book, new HadithStore.Download() {
      public void progress(int percent) { assertTrue(percent >= 0 && percent <= 100); }
      public void finished() { done.countDown(); }
      public void failed(String code) { failure.set(code); done.countDown(); }
    });
    assertTrue("Pinned real source download completes", done.await(200, TimeUnit.SECONDS));
    assertNull("Source import error code", failure.get());
  }

  private void remove(String book) throws Exception {
    CountDownLatch done = new CountDownLatch(1); store.remove(book, done::countDown);
    assertTrue(done.await(10, TimeUnit.SECONDS));
  }

  private void finish() {
    if (page != null) { getInstrumentation().runOnMainSync(page::finish); page = null; }
    getInstrumentation().waitForIdleSync();
  }

  private void reader(String id, String title) throws Exception {
    page = getInstrumentation().startActivitySync(new Intent(context, HadithReaderActivity.class)
        .putExtra("entry", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    long until = SystemClock.uptimeMillis() + 10000;
    while (SystemClock.uptimeMillis() < until) {
      TextView view = page.findViewById(R.id.dailyReaderTitle);
      if (view != null && title.contentEquals(view.getText())) return;
      SystemClock.sleep(50);
    }
    fail("Reader resolves the requested stable ID");
  }

  public void testRealBooksPagingSearchRollbackAndStableBookmarkAfterReinstall() throws Exception {
    download("bukhari"); download("muslim");
    HadithStore.Page first = query("", "Bukhari", originalSaved, 0);
    assertEquals(6638, first.total); assertEquals(20, first.entries.size());
    HadithStore.Page next = query("", "Bukhari", originalSaved, 20);
    assertEquals(20, next.offset); assertEquals(20, next.entries.size());
    assertFalse(first.entries.get(0).id.equals(next.entries.get(0).id));
    assertEquals(4930, query("", "Muslim", originalSaved, 0).total);
    HadithStore.Page found = query("٣٠٠١", "Bukhari", originalSaved, 0);
    assertEquals(1, found.total);
    assertEquals("tafsirq-v1:bukhari:3001", found.entries.get(0).id);
    assertEquals(0, query("2978", "Bukhari", originalSaved, 0).total);
    assertEquals(1, query("7008", "Bukhari", originalSaved, 0).total);
    assertEquals(0, query("%' OR 1=1 --", "Semua", originalSaved, 0).total);
    assertTrue(query("niat", "Bukhari", originalSaved, 0).total > 0);

    // Test parser rollback independently of the preceding real checksum verification.
    File broken = new File(context.getCacheDir(), "hadith-test-broken.json");
    Files.write(broken.toPath(), "[{\"number\":1,\"arab\":\"x\",\"id\":\"y\"}]".getBytes(StandardCharsets.UTF_8));
    Method importer = HadithStore.class.getDeclaredMethod("importValidated", File.class, HadithStore.Book.class);
    importer.setAccessible(true);
    try { importer.invoke(store, broken, HadithStore.BUKHARI); fail("Incomplete package rejected"); }
    catch (InvocationTargetException expected) { assertTrue(expected.getCause() instanceof IllegalStateException); }
    finally { Files.delete(broken.toPath()); }
    assertEquals(6638, query("", "Bukhari", originalSaved, 0).total);

    CountDownLatch cancelled = new CountDownLatch(1);
    AtomicReference<String> cancellation = new AtomicReference<>();
    store.download("bukhari", new HadithStore.Download() {
      public void progress(int percent) { }
      public void finished() { cancellation.set("unexpected_success"); cancelled.countDown(); }
      public void failed(String code) { cancellation.set(code); cancelled.countDown(); }
    });
    store.cancel();
    assertTrue(cancelled.await(10, TimeUnit.SECONDS));
    assertEquals("cancelled", cancellation.get());
    assertEquals(6638, query("", "Bukhari", originalSaved, 0).total);

    String id = "tafsirq-v1:bukhari:3001";
    Set<String> saved = new HashSet<>(originalSaved); saved.add(id);
    prefs.edit().putStringSet("hadith_bookmarks", saved).commit();
    reader(id, "Hadis 3001");
    assertTrue(((TextView) page.findViewById(R.id.dailyReaderSource)).getText().toString().contains("3001"));
    getInstrumentation().runOnMainSync(() -> page.findViewById(R.id.dailyReaderSave).performClick());
    assertFalse(prefs.getStringSet("hadith_bookmarks", new HashSet<>()).contains(id));
    getInstrumentation().runOnMainSync(() -> page.findViewById(R.id.dailyReaderSave).performClick());
    assertTrue(prefs.getStringSet("hadith_bookmarks", new HashSet<>()).contains(id));
    UiEvidence.capture(getInstrumentation(), "Hadith-Real-3001");
    finish();
    remove("bukhari");
    HadithStore.Page absent = query("", "Tersimpan", saved, 0);
    assertTrue(absent.unavailableSaved >= 1);
    assertTrue(prefs.getStringSet("hadith_bookmarks", new HashSet<>()).contains(id));
    assertEquals(id, prefs.getString("last_hadith_id", ""));
    reader(id, context.getString(R.string.hadith_unavailable)); finish();
    download("bukhari");
    assertEquals(id, query("3001", "Bukhari", saved, 0).entries.get(0).id);
    reader(id, "Hadis 3001"); finish();
    page = getInstrumentation().startActivitySync(new Intent(context, HadithActivity.class)
        .putExtra("book", "bukhari").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    long until = SystemClock.uptimeMillis() + 10000;
    while (SystemClock.uptimeMillis() < until
        && !((TextView) page.findViewById(R.id.hadithCount)).getText().toString().contains("6638"))
      SystemClock.sleep(50);
    assertTrue(((TextView) page.findViewById(R.id.hadithCount)).getText().toString().contains("6638"));
    assertEquals(20, ((android.widget.LinearLayout) page.findViewById(R.id.hadithList)).getChildCount());
    UiEvidence.capture(getInstrumentation(), "Hadith-Real-Collection");
  }

  @Override protected void tearDown() throws Exception {
    try {
      finish();
      if (store != null) {
        if (!originalBukhari) remove("bukhari");
        if (!originalMuslim) remove("muslim");
        store.close();
      }
      if (prefs != null) {
        SharedPreferences.Editor edit = prefs.edit().putStringSet("hadith_bookmarks", originalSaved);
        if (lastId == null) edit.remove("last_hadith_id"); else edit.putString("last_hadith_id", lastId);
        edit.commit();
      }
    } finally { super.tearDown(); }
  }
}
