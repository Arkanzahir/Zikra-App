package com.zikra.app.reading;

import android.content.*;
import android.test.InstrumentationTestCase;
import com.zikra.app.assistant.AssistantConnection;
import com.zikra.app.reading.mushaf.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import org.json.JSONObject;

/** Gateway responses are controlled locally; no synthetic credential leaves the emulator. */
@SuppressWarnings("deprecation")
public final class QuranWithdrawalTest extends InstrumentationTestCase {
  private Context target, isolated;
  private File root, fixtures;
  private final Set<String> preferences = ConcurrentHashMap.newKeySet();
  private final String token = "synthetic-withdrawal-test-token-00000000";

  @Override protected void setUp() throws Exception {
    super.setUp();
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    target = getInstrumentation().getTargetContext();
    fixtures = new File(target.getFilesDir(), "quran-failure-fixtures");
    assertTrue(new File(fixtures, "reader-pages/2-1.json").isFile());
    root = new File(target.getFilesDir(), "withdrawal-test-" + UUID.randomUUID());
    assertTrue(root.mkdir());
    isolated = new ContextWrapper(target) {
      @Override public File getFilesDir() { return root; }
      @Override public Context getApplicationContext() { return this; }
      @Override public File getCacheDir() {
        File cache = new File(root, "cache"); cache.mkdirs(); return cache;
      }
      @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        String scoped = root.getName() + "-" + name; preferences.add(scoped);
        return target.getSharedPreferences(scoped, mode);
      }
    };
  }
  private String fixture(String path) throws IOException {
    return QuranSyncStore.readBounded(new FileInputStream(new File(fixtures, path)), 524288);
  }
  private OkHttpClient reply(int status, String json) {
    return new OkHttpClient.Builder().addInterceptor(chain -> {
      assertEquals("https", chain.request().url().scheme());
      assertEquals("zikra-ai.vercel.app", chain.request().url().host());
      assertEquals("/v1/quran", chain.request().url().encodedPath());
      assertEquals("Bearer " + token, chain.request().header("Authorization"));
      return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
          .code(status).message("Controlled test response")
          .body(ResponseBody.create(MediaType.parse("application/json"), json)).build();
    }).build();
  }
  public void testWithdrawalBlocksCachedReaderAndMushafWithoutDeletingData() throws Exception {
    QuranChapters.Chapter chapter = QuranChapters.all(isolated).get(1);
    QuranSyncStore store = new QuranSyncStore(isolated);
    store.download(chapter, page -> fixture("reader-pages/2-" + page + ".json"), () -> {});
    String saved = store.read(chapter, 1);
    File bookmark = new File(root, "user-bookmark.txt");
    Files.write(bookmark.toPath(), "2:12".getBytes(StandardCharsets.UTF_8));
    File pages = new File(root, "quran-mushaf-sync-pages"); assertTrue(pages.mkdir());
    String mushaf = fixture("mushaf-pages/1.json");
    Files.write(new File(pages, "1.json").toPath(), mushaf.getBytes(StandardCharsets.UTF_8));
    AssistantConnection connection = new AssistantConnection(isolated);
    connection.save("https://zikra-ai.vercel.app/v1/assistant", token);
    for (String code : new String[]{"quran_resource_unavailable", "quran_source_access_unavailable"}) {
      isolated.getSharedPreferences("quran_content_sync", 0).edit().putBoolean("source_removed", false).commit();
      try {
        QuranSyncGateway.fetch(isolated, reply(503, "{\"error\":\"" + code + "\"}"), chapter, 1, call -> {});
        fail("Withdrawn resource must not be accepted");
      } catch (QuranSyncGateway.SourceRemoved expected) { }
      assertTrue(QuranSyncGateway.sourceRemoved(isolated));
      assertEquals(saved, store.read(chapter, 1));
    }
    isolated.getSharedPreferences("assistant_gateway", 0).edit().clear().commit();
    CountDownLatch readerDone = new CountDownLatch(1), mushafDone = new CountDownLatch(1);
    AtomicBoolean readerReady = new AtomicBoolean(), readerFailed = new AtomicBoolean(), mushafReady = new AtomicBoolean();
    try (QuranRepository reader = new QuranRepository(isolated); MushafRepository repository = new MushafRepository(isolated)) {
      reader.load(chapter, 1, new QuranRepository.Result() {
        @Override public void ready(QuranPage page, String source) { readerReady.set(true); readerDone.countDown(); }
        @Override public void failed(String message) { readerFailed.set(!message.isEmpty()); readerDone.countDown(); }
      });
      repository.load(1, new MushafRepository.Result() {
        @Override public void ready(MushafDocument document, android.graphics.Typeface font) { mushafReady.set(true); mushafDone.countDown(); }
        @Override public void failed() { mushafDone.countDown(); }
      });
      assertTrue(readerDone.await(5, TimeUnit.SECONDS)); assertTrue(mushafDone.await(5, TimeUnit.SECONDS));
      getInstrumentation().waitForIdleSync();
      assertFalse(readerReady.get()); assertTrue(readerFailed.get()); assertFalse(mushafReady.get());
    }
    assertEquals(saved, store.read(chapter, 1));
    assertEquals(mushaf, QuranSyncStore.readBounded(new FileInputStream(new File(pages, "1.json")), 524288));
    assertEquals("2:12", new String(Files.readAllBytes(bookmark.toPath()), StandardCharsets.UTF_8));
    connection.save("https://zikra-ai.vercel.app/v1/assistant", token);
    try {
      QuranSyncGateway.fetch(isolated, reply(200, "{}"), chapter, 1, call -> {});
      fail("Malformed success must not restore withdrawn content");
    } catch (org.json.JSONException expected) { }
    assertTrue(QuranSyncGateway.sourceRemoved(isolated));
    assertEquals(saved, QuranSyncGateway.fetch(isolated, reply(200, saved), chapter, 1, call -> {}));
    assertFalse(QuranSyncGateway.sourceRemoved(isolated));
    isolated.getSharedPreferences("quran_content_sync", 0).edit().putBoolean("source_removed", true).commit();
    assertEquals(mushaf, QuranSyncGateway.fetchMushaf(isolated, reply(200, mushaf), 1, call -> {}));
    assertFalse(QuranSyncGateway.sourceRemoved(isolated));
    assertTrue(QuranSyncPage.fresh(QuranSyncPage.validate(saved, chapter, 1, System.currentTimeMillis()), System.currentTimeMillis()));
    CountDownLatch restored = new CountDownLatch(1); AtomicBoolean recovered = new AtomicBoolean();
    try (QuranRepository reader = new QuranRepository(isolated)) {
      reader.load(chapter, 1, new QuranRepository.Result() {
        @Override public void ready(QuranPage page, String source) { recovered.set(true); restored.countDown(); }
        @Override public void failed(String message) { restored.countDown(); }
      });
      assertTrue(restored.await(5, TimeUnit.SECONDS)); assertTrue(recovered.get());
    }
  }
  public void testOrdinaryOutageDoesNotMarkResourceWithdrawn() throws Exception {
    AssistantConnection connection = new AssistantConnection(isolated);
    connection.save("https://zikra-ai.vercel.app/v1/assistant", token);
    for (int status : new int[]{401, 429, 500, 503}) {
      try {
        QuranSyncGateway.fetch(isolated, reply(status, "{\"error\":\"temporarily_unavailable\"}"), QuranChapters.all(isolated).get(1), 1, call -> {});
        fail("Unsuccessful response must fail");
      } catch (IOException expected) { assertFalse(QuranSyncGateway.sourceRemoved(isolated)); }
    }
  }
  @Override protected void tearDown() throws Exception {
    for (String name : preferences) target.getSharedPreferences(name, 0).edit().clear().commit();
    if (root != null) erase(root);
    super.tearDown();
  }
  private static void erase(File file) throws IOException {
    if (file.isDirectory()) for (File child : file.listFiles()) erase(child);
    if (!file.delete()) throw new IOException("Owned test cleanup failed");
  }
}
