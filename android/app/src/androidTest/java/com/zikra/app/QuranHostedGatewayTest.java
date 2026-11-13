package com.zikra.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.test.InstrumentationTestCase;
import com.zikra.app.assistant.AssistantConnection;
import com.zikra.app.reading.*;
import com.zikra.app.reading.mushaf.*;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.*;
import org.json.JSONObject;

/** Actual HTTPS and Keystore on a disposable emulator; no provider key enters this test APK. */
@SuppressWarnings("deprecation")
public final class QuranHostedGatewayTest extends InstrumentationTestCase {
  public void testHostedReaderMushafLocateAndOfflineGeneration() throws Exception {
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    Context target = getInstrumentation().getTargetContext();
    assertTrue("Requires fresh disposable installation", target.getSharedPreferences("assistant_gateway", 0).getAll().isEmpty());
    File credential = new File(target.getFilesDir(), "hosted-test-private.json");
    JSONObject identity = new JSONObject(QuranSyncStore.readBounded(new FileInputStream(credential), 4096));
    String token = identity.getString("token");
    File sandbox = new File(target.getFilesDir(), "hosted-test-" + UUID.randomUUID());
    assertTrue(sandbox.mkdir());
    Context context = new ContextWrapper(target) { @Override public File getFilesDir() { return sandbox; } };
    AssistantConnection connection = new AssistantConnection(target);
    OkHttpClient http = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(55, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
    android.app.Activity foreground = getInstrumentation().startActivitySync(
        new android.content.Intent(target, CalendarActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long focusDeadline = android.os.SystemClock.uptimeMillis() + 5000;
    while (!foreground.hasWindowFocus() && android.os.SystemClock.uptimeMillis() < focusDeadline)
      android.os.SystemClock.sleep(50);
    try {
      assertTrue("Network test requires a foreground app, not background instrumentation", foreground.hasWindowFocus());
      connection.save("https://zikra-ai.vercel.app/v1/assistant", token);
      assertTrue(QuranSyncGateway.configured(target));
      QuranChapters.Chapter chapter = QuranChapters.all(target).get(0);
      QuranSyncStore store = new QuranSyncStore(context);
      store.download(chapter, page -> QuranSyncGateway.fetch(target, http, chapter, page, call -> {}), () -> {});
      String saved = store.read(chapter, 1);
      assertEquals(7, new JSONObject(saved).getJSONArray("verses").length());
      String mushaf = QuranSyncGateway.fetchMushaf(target, http, 1, call -> {});
      assertFalse(MushafDocument.parse(mushaf, 1).accessibleText.isEmpty());
      assertEquals(2, QuranSyncGateway.locate(target, http, 2, 1, 0, 0, 0, call -> {}));
      connection.save("https://zikra-ai.vercel.app/v1/assistant", "00000000000000000000000000000000");
      boolean denied = false;
      try { QuranSyncGateway.fetch(target, http, chapter, 1, call -> {}); }
      catch (IOException expected) { denied = true; }
      assertTrue("Invalid installation credential must fail", denied);
      connection.save("https://zikra-ai.vercel.app/v1/assistant", token);
      assertEquals(7, new JSONObject(QuranSyncGateway.fetch(target, http, chapter, 1, call -> {})).getJSONArray("verses").length());
      try { store.download(chapter, page -> { throw new IOException("offline"); }, () -> {}); fail("Offline refresh must fail"); }
      catch (IOException expected) { assertEquals(saved, store.read(chapter, 1)); }
      AtomicBoolean ready = new AtomicBoolean();
      CountDownLatch done = new CountDownLatch(1);
      try (MushafRepository repository = new MushafRepository(target)) {
        repository.load(1, new MushafRepository.Result() {
          @Override public void ready(MushafDocument document, android.graphics.Typeface font) {
            ready.set(font != null && !document.accessibleText.isEmpty()); done.countDown();
          }
          @Override public void failed() { done.countDown(); }
        });
        assertTrue("Mushaf callback must finish", done.await(150, TimeUnit.SECONDS));
        assertTrue("Hosted Mushaf and real CDN fonts must load", ready.get());
      }
    } finally {
      getInstrumentation().runOnMainSync(foreground::finish);
      http.dispatcher().cancelAll();
      target.getSharedPreferences("assistant_gateway", 0).edit().clear().commit();
      erase(sandbox);
      if (!credential.delete()) throw new IOException("Private test credential cleanup failed");
    }
  }
  private static void erase(File file) throws IOException {
    if (file.isDirectory()) for (File child : file.listFiles()) erase(child);
    if (!file.delete()) throw new IOException("Owned test cleanup failed");
  }
}
