package com.zikra.app.assistant;

import android.content.*;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import org.json.*;

/** Isolated preference namespaces and intercepted requests; no real gateway or user messages. */
@SuppressWarnings("deprecation")
public final class AssistantPermissionTest extends InstrumentationTestCase {
  private Context context;
  private SharedPreferences prefs;
  private final Set<String> namespaces = new HashSet<>();
  private android.app.Activity foreground;
  protected void setUp() throws Exception {
    super.setUp(); Context target = getInstrumentation().getTargetContext();
    assertTrue(android.os.Build.MODEL.contains("sdk"));
    assertTrue(new File(target.getFilesDir(), "assistant-privacy-emulator-authorized").isFile());
    foreground = getInstrumentation().startActivitySync(new Intent(target, com.zikra.app.CalendarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    String prefix = "privacy-test-" + UUID.randomUUID() + "-";
    context = new ContextWrapper(target) {
      @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        namespaces.add(prefix + name); return target.getSharedPreferences(prefix + name, mode);
      }
    };
    prefs = context.getSharedPreferences("zikra_preferences", 0);
    new AssistantConnection(context).save("https://example.test/v1/assistant", "synthetic-permission-code-32-characters");
  }
  protected void tearDown() throws Exception {
    try {
      for (String name : namespaces) getInstrumentation().getTargetContext().deleteSharedPreferences(name);
      if (foreground != null) getInstrumentation().runOnMainSync(foreground::finish);
    } finally { super.tearDown(); }
  }
  private static JSONObject body() throws Exception { return new JSONObject().put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", "Synthetic"))); }
  private static Response reply(Request request) {
    return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
        .body(ResponseBody.create(MediaType.parse("application/json"), "{\"provider\":\"cloudflare\",\"reply\":\"Synthetic reply\"}")).build();
  }
  private static final class Result implements OnlineAssistantClient.Result {
    final CountDownLatch done = new CountDownLatch(1); volatile String reply; volatile OnlineAssistantClient.Failure failure;
    public void complete(String value) { reply = value; done.countDown(); }
    public void failed() { done.countDown(); }
    public void failed(OnlineAssistantClient.Failure value) { failure = value; done.countDown(); }
    void await() throws Exception { assertTrue(done.await(5, TimeUnit.SECONDS)); }
  }
  public void testMissingGrantRefusesBeforeAnyTransport() throws Exception {
    AtomicInteger sent = new AtomicInteger();
    try (OnlineAssistantClient client = new OnlineAssistantClient(new OkHttpClient.Builder().addInterceptor(chain -> { sent.incrementAndGet(); return reply(chain.request()); }).build())) {
      try { client.send(new AssistantConnection(context), body(), new Result()); fail(); } catch (OnlineAssistantClient.PermissionChanged expected) { }
      assertEquals(0, sent.get());
    }
  }
  public void testValidGrantSendsExactlyOnce() throws Exception {
    AssistantConnection connection = new AssistantConnection(context); AssistantConsent.accept(prefs, connection.endpoint());
    AtomicInteger sent = new AtomicInteger(); Result result = new Result();
    try (OnlineAssistantClient client = new OnlineAssistantClient(new OkHttpClient.Builder().addInterceptor(chain -> { sent.incrementAndGet(); return reply(chain.request()); }).build())) {
      client.send(connection, body(), result); result.await(); assertEquals("Synthetic reply", result.reply); assertEquals(1, sent.get());
    }
  }
  private void queuedChange(boolean service) throws Exception {
    AssistantConnection connection = new AssistantConnection(context); AssistantConsent.accept(prefs, connection.endpoint());
    AtomicInteger sent = new AtomicInteger(); Result result = new Result(); CountDownLatch release = new CountDownLatch(1), occupied = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> { occupied.countDown(); try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException stop) { Thread.currentThread().interrupt(); } });
    assertTrue(occupied.await(2, TimeUnit.SECONDS));
    OkHttpClient http = new OkHttpClient.Builder().dispatcher(new Dispatcher(executor)).addInterceptor(chain -> { sent.incrementAndGet(); return reply(chain.request()); }).build();
    try (OnlineAssistantClient client = new OnlineAssistantClient(http)) {
      client.send(connection, body(), result);
      if (service) connection.save("https://other.test/v1/assistant", "replacement-permission-code-32-characters");
      else { AssistantConsent.revoke(prefs); AssistantConsent.accept(prefs, connection.endpoint()); }
      release.countDown(); result.await(); assertEquals(0, sent.get()); assertNull(result.reply);
      assertEquals(OnlineAssistantClient.Failure.CONSENT, result.failure);
    } finally { release.countDown(); executor.shutdownNow(); }
  }
  public void testRevokedAndReacceptedQueuedRequestDoesNotReachTransport() throws Exception { queuedChange(false); }
  public void testChangedServiceQueuedRequestDoesNotReachTransport() throws Exception { queuedChange(true); }
  public void testReplyAfterRevocationIsDiscardedWithoutClaimingRecall() throws Exception {
    AssistantConnection connection = new AssistantConnection(context); AssistantConsent.accept(prefs, connection.endpoint());
    CountDownLatch sent = new CountDownLatch(1), release = new CountDownLatch(1); Result result = new Result();
    OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
      sent.countDown(); try { if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Fixture timeout"); }
      catch (InterruptedException stop) { Thread.currentThread().interrupt(); throw new IOException("Fixture stopped"); }
      return reply(chain.request());
    }).build();
    try (OnlineAssistantClient client = new OnlineAssistantClient(http)) {
      client.send(connection, body(), result); assertTrue(sent.await(2, TimeUnit.SECONDS));
      AssistantConsent.revoke(prefs); release.countDown(); result.await();
      assertNull(result.reply); assertEquals(OnlineAssistantClient.Failure.CONSENT, result.failure);
    } finally { release.countDown(); }
  }
  public void testAutomaticRoutingStillWorksWithLegacyOnlineFlagOff() throws Exception {
    AssistantConnection connection = new AssistantConnection(context); connection.offline();
    prefs.edit().putString("assistant_mode_v2", "auto").commit(); AssistantConsent.accept(prefs, connection.endpoint());
    assertFalse(connection.online()); assertTrue(connection.permits(connection.permission()));
  }
  private static final class ImageResult implements ImageGenerationClient.Result {
    final CountDownLatch done = new CountDownLatch(1); volatile GeneratedImages.Data image; volatile String error;
    public void complete(GeneratedImages.Data value) { image=value;done.countDown(); }
    public void failed(String value) { error=value;done.countDown(); }
    void await() throws Exception { assertTrue(done.await(5, TimeUnit.SECONDS)); }
  }
  private static Response imageReply(Request request) throws Exception {
    android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output)); } finally { bitmap.recycle(); }
    String raw = new JSONObject().put("provider","cloudflare").put("model","flux-1-schnell").put("generated",true)
        .put("mime","image/png").put("width",64).put("height",64)
        .put("data",android.util.Base64.encodeToString(output.toByteArray(),android.util.Base64.NO_WRAP)).toString();
    return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
        .body(ResponseBody.create(MediaType.parse("application/json"),raw)).build();
  }
  public void testExplicitImageConfirmationDoesNotGrantBasicChatPermission() throws Exception {
    AssistantConnection connection = new AssistantConnection(context); AtomicInteger sent = new AtomicInteger(); ImageResult result = new ImageResult();
    assertFalse(AssistantConsent.granted(prefs,connection.endpoint()));
    OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
      sent.incrementAndGet(); try { return imageReply(chain.request()); } catch(Exception invalid) { throw new IOException("Invalid image fixture",invalid); }
    }).build();
    try (ImageGenerationClient client = new ImageGenerationClient(http)) {
      client.generate(connection,"Synthetic image",UUID.randomUUID().toString(),connection.servicePermission(),result);
      result.await(); assertNotNull(result.image); assertNull(result.error); assertEquals(1,sent.get());
      assertFalse(AssistantConsent.granted(prefs,connection.endpoint()));
    }
  }
  public void testImageConfirmedBeforeRevocationCannotStartLater() throws Exception {
    AssistantConnection connection = new AssistantConnection(context);
    AssistantConnection.ServicePermit reviewed=connection.servicePermission(); AssistantConsent.revoke(prefs);
    try (ImageGenerationClient client = new ImageGenerationClient(new OkHttpClient())) {
      try { client.generate(connection,"Synthetic image",UUID.randomUUID().toString(),reviewed,new ImageResult()); fail(); }
      catch (OnlineAssistantClient.PermissionChanged expected) { }
    }
  }
  public void testImageServiceChangeWhileQueuedCannotSendPrompt() throws Exception {
    AssistantConnection connection = new AssistantConnection(context); AtomicInteger sent = new AtomicInteger();
    ImageResult result = new ImageResult(); CountDownLatch occupied = new CountDownLatch(1),release = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor(); executor.execute(() -> {
      occupied.countDown();try { release.await(5,TimeUnit.SECONDS); } catch(InterruptedException stop) { Thread.currentThread().interrupt(); }
    }); assertTrue(occupied.await(2,TimeUnit.SECONDS));
    OkHttpClient http = new OkHttpClient.Builder().dispatcher(new Dispatcher(executor)).addInterceptor(chain -> { sent.incrementAndGet();return reply(chain.request()); }).build();
    try(ImageGenerationClient client = new ImageGenerationClient(http)) {
      client.generate(connection,"Synthetic image",UUID.randomUUID().toString(),connection.servicePermission(),result);
      connection.save("https://other.test/v1/assistant","replacement-image-code-32-characters");
      release.countDown();result.await();assertEquals(0,sent.get());assertNull(result.image);assertEquals("image_permission_changed",result.error);
    } finally { release.countDown();executor.shutdownNow(); }
  }

}
