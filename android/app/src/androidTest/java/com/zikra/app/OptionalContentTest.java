package com.zikra.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.discovery.DuaCatalog;
import com.zikra.app.discovery.DuaCatalogRepository;
import com.zikra.app.discovery.HadithStore;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("deprecation")
public final class OptionalContentTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private static Object field(Object target, String name) throws Exception {
    Field value = target.getClass().getDeclaredField(name); value.setAccessible(true); return value.get(target);
  }
  @Override protected void setUp() throws Exception {
    super.setUp(); context = getInstrumentation().getTargetContext();
    assertTrue("Fixtures require an emulator", android.os.Build.FINGERPRINT.contains("generic")
        || android.os.Build.MODEL.contains("sdk"));
  }
  private DuaCatalog load(DuaCatalogRepository repo, boolean refresh, boolean success) throws Exception {
    CountDownLatch done = new CountDownLatch(1); AtomicReference<DuaCatalog> result = new AtomicReference<>();
    repo.load(refresh, new DuaCatalogRepository.Result() {
      public void show(DuaCatalog value) { result.set(value); done.countDown(); }
      public void failed() { done.countDown(); }
    });
    assertTrue("Catalogue request finishes", done.await(45, TimeUnit.SECONDS));
    if (success) assertNotNull("Catalogue load succeeds", result.get());
    else assertNull("Invalid update rejected", result.get());
    return result.get();
  }
  private void close(DuaCatalogRepository repo) throws Exception {
    repo.close(); assertTrue(((ExecutorService) field(repo, "worker")).awaitTermination(5, TimeUnit.SECONDS));
  }
  private void waitCatalog(Activity value) throws Exception {
    long deadline = SystemClock.uptimeMillis() + 10000;
    while (SystemClock.uptimeMillis() < deadline) {
      if (field(value, "catalog") != null) { getInstrumentation().waitForIdleSync(); return; }
      SystemClock.sleep(50);
    }
    fail("Offline screen did not resolve cached catalogue");
  }
  public void testRealDuaDownloadOfflinePagingSearchAndInvalidUpdatePreservesCache() throws Exception {
    File file = new File(context.getFilesDir(), "equran-doa-catalog.json");
    byte[] original = file.exists() ? Files.readAllBytes(file.toPath()) : null;
    DuaCatalogRepository repo = new DuaCatalogRepository(context);
    try {
      DuaCatalog online = load(repo, true, true);
      assertEquals(227, online.sourceCount); assertEquals(1, online.excludedWithoutReference);
      assertEquals(1, online.excludedWithoutText);
      assertEquals(225, online.entries.size());
      byte[] good = Files.readAllBytes(file.toPath());
      close(repo); repo = new DuaCatalogRepository(context);
      assertEquals(225, load(repo, false, true).entries.size());
      Field client = DuaCatalogRepository.class.getDeclaredField("client"); client.setAccessible(true);
      client.set(repo, new OkHttpClient.Builder().addInterceptor(chain -> new Response.Builder()
          .request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Fixture")
          .body(ResponseBody.create(MediaType.get("application/json"), "[{\"id\":1}]")).build()).build());
      load(repo, true, false);
      assertTrue("Bad remote response cannot replace offline data",
          java.util.Arrays.equals(good, Files.readAllBytes(file.toPath())));
      page = getInstrumentation().startActivitySync(new Intent(context, DuaLibraryActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); waitCatalog(page);
      LinearLayout list = page.findViewById(R.id.duaLibraryContent);
      assertEquals(20, list.getChildCount());
      String first = ((TextView) list.getChildAt(0).findViewById(R.id.duaTitle)).getText().toString();
      getInstrumentation().runOnMainSync(() -> page.findViewById(R.id.duaLibraryNext).performClick());
      assertEquals(20, list.getChildCount());
      assertFalse(first.contentEquals(((TextView) list.getChildAt(0).findViewById(R.id.duaTitle)).getText()));
      EditText search = page.findViewById(R.id.duaLibrarySearch);
      getInstrumentation().runOnMainSync(() -> search.setText("hujan"));
      assertTrue(list.getChildCount() > 0 && list.getChildCount() < 20);
      getInstrumentation().runOnMainSync(() -> list.getChildAt(0).findViewById(R.id.duaExpand).performClick());
      assertTrue(list.getChildAt(0).findViewById(R.id.duaListen).isEnabled());
      assertEquals(android.view.View.GONE, list.getChildAt(0).findViewById(R.id.duaSourceDetails).getVisibility());
      getInstrumentation().runOnMainSync(() -> list.getChildAt(0).findViewById(R.id.duaSourceToggle).performClick());
      assertTrue(((TextView) list.getChildAt(0).findViewById(R.id.duaSourceDetails)).getText().toString().contains("equran.id"));
      UiEvidence.capture(getInstrumentation(), "Dua-Library-Rain-Source");
      getInstrumentation().runOnMainSync(() -> search.setText("zzzz-no-such-prayer"));
      assertEquals(0, list.getChildCount());
      getInstrumentation().runOnMainSync(() -> search.setText(""));
      Field group = DuaLibraryActivity.class.getDeclaredField("group"); group.setAccessible(true);
      group.set(page, online.entries.get(0).group);
      getInstrumentation().runOnMainSync(() -> search.setText(" "));
      assertTrue(list.getChildCount() > 0);
      String targetGroup = online.entries.get(0).group;
      for (int i = 0; i < list.getChildCount(); i++)
        assertEquals(targetGroup, ((TextView) list.getChildAt(i).findViewById(R.id.duaRowCategory)).getText().toString());
      getInstrumentation().runOnMainSync(page::finish); page = null;
      page = getInstrumentation().startActivitySync(new Intent(context, DuaLibraryActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); waitCatalog(page);
      assertEquals(20, ((LinearLayout) page.findViewById(R.id.duaLibraryContent)).getChildCount());
      UiEvidence.capture(getInstrumentation(), "Dua-Library-Offline");
    } finally {
      if (page != null) { getInstrumentation().runOnMainSync(page::finish); page = null; }
      close(repo);
      if (original == null) Files.deleteIfExists(file.toPath()); else Files.write(file.toPath(), original);
    }
  }
  public void testDeletingPackageActuallyReturnsStorageWithoutDeletingOtherEntries() throws Exception {
    String name = "zikra-content-storage-fixture.db"; context.deleteDatabase(name);
    HadithStore store = new HadithStore(context, name);
    try {
      SQLiteOpenHelper helper = (SQLiteOpenHelper) field(store, "helper");
      SQLiteDatabase db = helper.getWritableDatabase();
      String text = new String(new char[12000]).replace('\0', 'x');
      db.beginTransaction();
      try {
        for (int i = 1; i <= 150; i++) db.execSQL(
            "INSERT INTO entries VALUES(?,?,?, ?,?,?,?, ?,?)",
            new Object[]{"fixture:" + i, "bukhari", i, "Fixture", text, text, "Fixture", text, 0});
        db.setTransactionSuccessful();
      } finally { db.endTransaction(); }
      long before = context.getDatabasePath(name).length(); assertTrue(before > 1000000);
      CountDownLatch done = new CountDownLatch(1); AtomicReference<Boolean> result = new AtomicReference<>();
      store.remove("bukhari", new HadithStore.Result<Boolean>() {
        public void show(Boolean removed) { result.set(removed); done.countDown(); }
        public void failed() { done.countDown(); }
      });
      assertTrue(done.await(15, TimeUnit.SECONDS)); assertEquals(Boolean.TRUE, result.get());
      long after = context.getDatabasePath(name).length();
      assertTrue("Deleted book releases actual database file pages", after < before / 2);
      try (android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM entries", null)) {
        cursor.moveToFirst(); assertEquals(7, cursor.getInt(0));
      }
    } finally {
      store.close(); ((ExecutorService) field(store, "worker")).awaitTermination(5, TimeUnit.SECONDS);
      context.deleteDatabase(name);
    }
  }

  public void testReviewedDuaDisplaysCorrectionsAndOriginalsFromOfflinePackage() throws Exception {
    File file = new File(context.getFilesDir(), "equran-doa-catalog.json");
    byte[] original = file.exists() ? Files.readAllBytes(file.toPath()) : null;
    DuaCatalogRepository repo = new DuaCatalogRepository(context);
    try {
      DuaCatalog downloaded = load(repo, true, true);
      for (DuaCatalog.Entry entry : downloaded.entries)
        assertTrue("Source snapshot changed: review it before updating evidence", entry.review != com.zikra.app.discovery.DuaReview.SOURCE_CHANGED);
      close(repo); repo = new DuaCatalogRepository(context);
      assertEquals(225, load(repo, false, true).entries.size());
      page = getInstrumentation().startActivitySync(new Intent(context, DuaLibraryActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); waitCatalog(page);
      EditText search = page.findViewById(R.id.duaLibrarySearch);
      LinearLayout list = page.findViewById(R.id.duaLibraryContent);
      for (int number : new int[]{3, 26, 55, 70, 76, 77, 94, 98, 111, 121, 122, 123, 124, 145, 154, 159, 168, 176, 186, 188, 211}) {
        DuaCatalog.Entry expected = null;
        for (DuaCatalog.Entry entry : downloaded.entries) if (entry.number == number) expected = entry;
        assertNotNull(expected);
        final DuaCatalog.Entry target = expected;
        getInstrumentation().runOnMainSync(() -> search.setText(number == 186 ? "لبيك"
            : number == 123 ? "kokohkanlah aku" : target.title));
        android.view.View card = null;
        for (int i = 0; i < list.getChildCount(); i++) {
          android.view.View child = list.getChildAt(i);
          if (("EQuran · " + number).contentEquals(((TextView) child.findViewById(R.id.duaSubtitle)).getText())) card = child;
        }
        assertNotNull("Search finds source ID " + number, card);
        final android.view.View result = card;
        getInstrumentation().runOnMainSync(() -> result.findViewById(R.id.duaExpand).performClick());
        assertEquals(target.arabic, ((TextView) card.findViewById(R.id.duaArabic)).getText().toString());
        assertEquals(target.latin, ((TextView) card.findViewById(R.id.duaLatin)).getText().toString());
        assertEquals(target.meaning, ((TextView) card.findViewById(R.id.duaTranslation)).getText().toString());
        assertEquals(target.review.displayTitle(target.title), ((TextView) card.findViewById(R.id.duaTitle)).getText().toString());
        assertTrue(((TextView) card.findViewById(R.id.duaSource)).getText().toString().contains("Catatan bacaan"));
        getInstrumentation().runOnMainSync(() -> result.findViewById(R.id.duaSourceToggle).performClick());
        String details = ((TextView) card.findViewById(R.id.duaSourceDetails)).getText().toString();
        assertTrue(details.contains(target.reference));
        if (!target.arabic.equals(target.sourceArabic)) assertTrue(details.contains(target.sourceArabic));
        if (!target.latin.equals(target.sourceLatin)) assertTrue(details.contains(target.sourceLatin));
        if (!target.meaning.equals(target.sourceMeaning)) assertTrue(details.contains(target.sourceMeaning));
        if (!target.title.equals(target.review.displayTitle(target.title))) assertTrue(details.contains(target.title));
        assertTrue(card.findViewById(R.id.duaListen).isEnabled());
        if (number == 186 || number == 77) UiEvidence.capture(getInstrumentation(), "Dua-Reviewed-" + number);
      }
      getInstrumentation().runOnMainSync(page::finish); page = null;
      org.json.JSONObject changed = new org.json.JSONObject(new String(Files.readAllBytes(file.toPath()),
          java.nio.charset.StandardCharsets.UTF_8));
      org.json.JSONArray data = changed.getJSONArray("data");
      for (int i = 0; i < data.length(); i++) if (data.getJSONObject(i).getInt("id") == 186)
        data.getJSONObject(i).put("nama", "Sumber berubah uji");
      Files.write(file.toPath(), changed.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      page = getInstrumentation().startActivitySync(new Intent(context, DuaLibraryActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); waitCatalog(page);
      EditText changedSearch = page.findViewById(R.id.duaLibrarySearch);
      getInstrumentation().runOnMainSync(() -> changedSearch.setText("Sumber berubah uji"));
      LinearLayout changedList = page.findViewById(R.id.duaLibraryContent);
      assertEquals(1, changedList.getChildCount());
      android.view.View changedCard = changedList.getChildAt(0);
      getInstrumentation().runOnMainSync(() -> changedCard.findViewById(R.id.duaExpand).performClick());
      assertTrue(((TextView) changedCard.findViewById(R.id.duaSource)).getText().toString().contains("Belum ditinjau"));
      assertTrue(((TextView) changedCard.findViewById(R.id.duaArabic)).getText().toString().startsWith("بَّيْكَ"));
      getInstrumentation().runOnMainSync(() -> changedCard.findViewById(R.id.duaSourceToggle).performClick());
      assertTrue(((TextView) changedCard.findViewById(R.id.duaSourceDetails)).getText().toString()
          .contains("Koreksi versi lama tidak diterapkan"));
    } finally {
      if (page != null) { getInstrumentation().runOnMainSync(page::finish); page = null; }
      close(repo);
      if (original == null) Files.deleteIfExists(file.toPath()); else Files.write(file.toPath(), original);
    }
  }
}
