package com.zikra.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.widget.TextView;
import com.zikra.app.discovery.HadeethEncCsv;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Requires a new, disposable emulator and the official privately pushed CSV fixture. */
@SuppressWarnings("deprecation")
public final class HadeethEncIntegrationTest extends InstrumentationTestCase {
  private Context context;
  private HadithStore store;
  private Activity activity;
  private File fixture;
  private boolean ownsDefaultDatabase;
  private java.util.Map<String, ?> previousPreferences, previousAppearance;
  private static final String TEST_DB = "zikra_hadeethenc_integration.db";
  private static final String OLD_ID = "tafsirq-v1:bukhari:3001";
  private static final String NEW_ID = "hadeethenc:id:4560";

  @Override protected void setUp() throws Exception {
    super.setUp(); context = getInstrumentation().getTargetContext();
    assertTrue("Never run on a user's phone", android.os.Build.MODEL.contains("sdk")
        || android.os.Build.FINGERPRINT.contains("generic"));
    fixture = new File(context.getFilesDir(), "hadeethenc-fixture.csv");
    assertEquals(HadeethEncCsv.BYTES, fixture.length());
    previousPreferences = context.getSharedPreferences("zikra_preferences", 0).getAll();
    previousAppearance = context.getSharedPreferences("zikra_home_appearance", 0).getAll();
  }

  private HadithStore.Page query(String query, String category, Set<String> saved, int offset) throws Exception {
    CountDownLatch done = new CountDownLatch(1); AtomicReference<HadithStore.Page> value = new AtomicReference<>();
    store.query(query, category, saved, offset, new HadithStore.Result<HadithStore.Page>() {
      public void show(HadithStore.Page page) { value.set(page); done.countDown(); }
      public void failed() { done.countDown(); }
    });
    assertTrue(done.await(30, TimeUnit.SECONDS)); assertNotNull(value.get()); return value.get();
  }
  private HadithCollection.Entry get(String id) throws Exception {
    CountDownLatch done = new CountDownLatch(1); AtomicReference<HadithCollection.Entry> value = new AtomicReference<>();
    store.get(id, new HadithStore.Result<HadithCollection.Entry>() {
      public void show(HadithCollection.Entry entry) { value.set(entry); done.countDown(); }
      public void failed() { done.countDown(); }
    });
    assertTrue(done.await(30, TimeUnit.SECONDS)); return value.get();
  }
  private void closeStore() throws Exception {
    if (store == null) return;
    store.close(); java.lang.reflect.Field field = HadithStore.class.getDeclaredField("worker");
    field.setAccessible(true); assertTrue(((ExecutorService) field.get(store)).awaitTermination(10, TimeUnit.SECONDS));
    store = null;
  }
  private void importFixture() throws Exception {
    Method method = HadithStore.class.getDeclaredMethod("importEncyclopedia", File.class);
    method.setAccessible(true); method.invoke(store, fixture);
  }
  private void finishActivity() {
    if (activity != null) { getInstrumentation().runOnMainSync(activity::finish); activity = null; }
    getInstrumentation().waitForIdleSync();
  }
  private void openReader(String id) throws Exception {
    activity = getInstrumentation().startActivitySync(new Intent(context, HadithReaderActivity.class)
        .putExtra("entry", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    long end = SystemClock.uptimeMillis() + 15000;
    while (SystemClock.uptimeMillis() < end) {
      TextView source = activity.findViewById(R.id.dailyReaderSource);
      if (source != null && source.getText().toString().contains("1.22.0")) return;
      SystemClock.sleep(50);
    }
    fail("Reader did not bind source version");
  }

  public void testVersionOneUpgradeRealDownloadRollbackBookmarksAndReinstall() throws Exception {
    assertFalse("Isolated fixture database must be new", context.getDatabasePath(TEST_DB).exists());
    try (SQLiteDatabase db = context.openOrCreateDatabase(TEST_DB, 0, null)) {
      db.execSQL("CREATE TABLE entries(id TEXT PRIMARY KEY NOT NULL,book TEXT NOT NULL,number INTEGER,title TEXT NOT NULL,arab TEXT NOT NULL,meaning TEXT NOT NULL,source TEXT NOT NULL,search TEXT NOT NULL,excerpt INTEGER NOT NULL,UNIQUE(book,number))");
      db.execSQL("CREATE TABLE packages(book TEXT PRIMARY KEY NOT NULL,revision TEXT NOT NULL,count INTEGER NOT NULL)");
      db.execSQL("CREATE TABLE bookmarks(id TEXT PRIMARY KEY NOT NULL)");
      db.execSQL("INSERT INTO entries VALUES(?, 'bukhari',3001,'Legacy fixture','نص تجريبي','Not a real hadith fixture','fixture','fixture',0)", new Object[]{OLD_ID});
      db.execSQL("INSERT INTO packages VALUES('bukhari',?,1)", new Object[]{HadithStore.REVISION});
      db.execSQL("INSERT INTO bookmarks VALUES(?)", new Object[]{OLD_ID}); db.setVersion(1);
    }
    store = new HadithStore(context, TEST_DB);
    Set<String> saved = new HashSet<>(); saved.add(OLD_ID); saved.add(NEW_ID);
    assertEquals(1, query("", "Bukhari", saved, 0).total);
    CountDownLatch done = new CountDownLatch(1); AtomicReference<String> failure = new AtomicReference<>();
    store.download("hadeethenc", new HadithStore.Download() {
      public void progress(int percent) { assertTrue(percent >= 0 && percent <= 100); }
      public void finished() { done.countDown(); }
      public void failed(String code) { failure.set(code); done.countDown(); }
    });
    assertTrue("Real pinned download finishes", done.await(180, TimeUnit.SECONDS)); assertNull(failure.get());
    assertEquals(2260, query("", "Pilihan", saved, 0).total);
    assertEquals(2261, query("", "Semua", saved, 0).total);
    assertEquals(20, query("", "Pilihan", saved, 20).offset);
    assertEquals(1, query("٤٥٦٠", "Pilihan", saved, 0).total);
    assertEquals(0, query("%' OR 1=1 --", "Pilihan", saved, 0).total);
    HadithCollection.Entry selected = get(NEW_ID); assertNotNull(selected);
    assertEquals("HadeethEnc", selected.collection); assertTrue(selected.encyclopedia.metadata.contains("v1.22.0"));
    assertTrue(query("", "Semua", saved, 0).bukhariInstalled);
    assertEquals("نص تجريبي", get(OLD_ID).arabic);
    File broken = new File(context.getCacheDir(), "enc-broken-fixture.csv");
    Files.write(broken.toPath(), "invalid".getBytes(StandardCharsets.UTF_8));
    Method importer = HadithStore.class.getDeclaredMethod("importEncyclopedia", File.class); importer.setAccessible(true);
    try { importer.invoke(store, broken); fail("Broken import accepted"); }
    catch (InvocationTargetException expected) { assertTrue(expected.getCause() instanceof java.io.IOException); }
    finally { broken.delete(); }
    assertEquals(2260, query("", "Pilihan", saved, 0).total);
    closeStore(); store = new HadithStore(context, TEST_DB);
    assertEquals(2, query("", "Tersimpan", saved, 0).total);
    CountDownLatch canceled = new CountDownLatch(1); AtomicReference<String> cancelCode = new AtomicReference<>();
    store.download("hadeethenc", new HadithStore.Download() {
      public void progress(int percent) { }
      public void finished() { cancelCode.set("unexpected_success"); canceled.countDown(); }
      public void failed(String code) { cancelCode.set(code); canceled.countDown(); }
    });
    store.cancel(); assertTrue(canceled.await(15, TimeUnit.SECONDS)); assertEquals("cancelled", cancelCode.get());
    assertEquals(2260, query("", "Pilihan", saved, 0).total);
    closeStore(); store = new HadithStore(context, TEST_DB);
    CountDownLatch removed = new CountDownLatch(1); store.remove("hadeethenc", removed::countDown);
    assertTrue(removed.await(20, TimeUnit.SECONDS));
    assertEquals(1, query("", "Tersimpan", saved, 0).unavailableSaved);
    assertEquals(1, query("", "Bukhari", saved, 0).total);
    importFixture(); assertEquals(NEW_ID, get(NEW_ID).id);
    assertEquals(2, query("", "Tersimpan", saved, 0).total);
  }

  public void testReaderSourceCopyBookmarkAndCollectionScreens() throws Exception {
    assertFalse("UI proof requires a fresh default database", context.getDatabasePath("zikra_hadith.db").exists());
    ownsDefaultDatabase = true;
    store = new HadithStore(context); importFixture();
    for (String theme : new String[]{"light", "dark"}) {
      context.getSharedPreferences("zikra_home_appearance", 0).edit().putString("theme", theme).commit();
      openReader(NEW_ID);
      assertTrue(((TextView) activity.findViewById(R.id.hadithReaderKind)).getText().toString().contains("ATSAR"));
      assertEquals(get(NEW_ID).encyclopedia.cell(11), ((TextView) activity.findViewById(R.id.hadeethencGrade)).getText().toString());
      getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.dailyReaderCopy).performClick());
      android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      String copied = clipboard.getPrimaryClip().getItemAt(0).coerceToText(context).toString();
      assertTrue(copied.contains("v1.22.0")); assertTrue(copied.contains(HadeethEncCsv.sourceUrl(4560)));
      // Let Android's clipboard overlay disappear; it is not part of Zikra's layout.
      SystemClock.sleep(6500);
      getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.dailyReaderSave).performClick());
      assertTrue(context.getSharedPreferences("zikra_preferences", 0).getStringSet("hadith_bookmarks", Set.of()).contains(NEW_ID));
      getInstrumentation().waitForIdleSync();
      for (int action : new int[]{R.id.dailyReaderSave, R.id.dailyReaderCopy, R.id.dailyReaderShare}) {
        TextView control = activity.findViewById(action);
        assertEquals("Reader action keeps its complete word on one line", 1, control.getLayout().getLineCount());
        android.graphics.Rect visible = new android.graphics.Rect();
        assertTrue(control.getGlobalVisibleRect(visible));
        assertEquals(control.getHeight(), visible.height());
      }
      UiEvidence.capture(getInstrumentation(), "HadeethEnc-Reader-" + theme);
      getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.hadeethencFullSource).performClick());
      java.lang.reflect.Field dialogField = HadithReaderActivity.class.getDeclaredField("sourceDialog");
      dialogField.setAccessible(true); android.app.AlertDialog dialog = (android.app.AlertDialog) dialogField.get(activity);
      getInstrumentation().waitForIdleSync();
      for (android.widget.Button button : new android.widget.Button[]{
          dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE), dialog.findViewById(R.id.hadeethencVersionCheck)}) {
        android.graphics.Rect visible = new android.graphics.Rect();
        assertTrue(button.getGlobalVisibleRect(visible));
        assertEquals("Source action must be fully visible", button.getHeight(), visible.height());
        android.text.Layout layout = button.getLayout(); assertNotNull(layout);
        for (int line = 0; line < layout.getLineCount(); line++) assertEquals(0, layout.getEllipsisCount(line));
      }
      UiEvidence.capture(getInstrumentation(), "HadeethEnc-Source-" + theme);
      finishActivity();
      context.getSharedPreferences("zikra_preferences", 0).edit().putStringSet("hadith_bookmarks", Set.of()).commit();
    }
    activity = getInstrumentation().startActivitySync(new Intent(context, HadithActivity.class)
        .putExtra("book", "hadeethenc").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    long end = SystemClock.uptimeMillis() + 15000;
    while (SystemClock.uptimeMillis() < end && !((TextView) activity.findViewById(R.id.hadithCount)).getText().toString().contains("2260"))
      SystemClock.sleep(50);
    assertTrue(((TextView) activity.findViewById(R.id.hadithCount)).getText().toString().contains("2260"));
    getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.hadithTabs).requestRectangleOnScreen(
        new android.graphics.Rect(0, 0, activity.findViewById(R.id.hadithTabs).getWidth(),
            activity.findViewById(R.id.hadithTabs).getHeight()), true));
    getInstrumentation().waitForIdleSync();
    android.widget.LinearLayout tabs = activity.findViewById(R.id.hadithTabs);
    for (int i = 0; i < tabs.getChildCount(); i++) {
      TextView tab = (TextView) tabs.getChildAt(i);
      assertEquals("Every category label fits without breaking a word", 1, tab.getLayout().getLineCount());
    }
    for (int destination : new int[]{R.id.dockHome, R.id.dockQuran, R.id.dockAi, R.id.dockStudy, R.id.dockProfile}) {
      android.view.View cell = activity.findViewById(destination);
      TextView label = cell.findViewById(R.id.navDestinationLabel);
      assertTrue("Dock labels stay inside their destination", label.getWidth() <= cell.getWidth());
      android.text.Layout layout = label.getLayout();
      assertEquals("Complete destination name remains readable", label.getText().length(), layout.getLineEnd(layout.getLineCount() - 1));
    }
    UiEvidence.capture(getInstrumentation(), "HadeethEnc-Collection");
  }

  @Override protected void tearDown() throws Exception {
    try {
      finishActivity(); closeStore(); context.deleteDatabase(TEST_DB);
      if (ownsDefaultDatabase) context.deleteDatabase("zikra_hadith.db");
      restore("zikra_preferences", previousPreferences, new String[]{"hadith_bookmarks", "last_hadith_id"});
      restore("zikra_home_appearance", previousAppearance, new String[]{"theme"});
    }
    finally { super.tearDown(); }
  }

  private void restore(String name, java.util.Map<String, ?> original, String[] keys) {
    if (original == null) return;
    android.content.SharedPreferences.Editor edit = context.getSharedPreferences(name, 0).edit();
    for (String key : keys) {
      Object value = original.get(key);
      if (value instanceof String) edit.putString(key, (String) value);
      else if (value instanceof Set) edit.putStringSet(key, new HashSet<>((Set<String>) value));
      else edit.remove(key);
    }
    edit.commit();
  }
}
