package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.zikra.app.AiActivity;
import com.zikra.app.R;
import com.zikra.app.UiEvidence;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import okio.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** C03 fixtures are emulator-only and require verified host backup. No real user content is sent. */
@SuppressWarnings("deprecation")
public final class AssistantConnectivityTest extends InstrumentationTestCase {
  private static final String CODE = "synthetic-connectivity-code-32-characters";
  private Context context;
  private AiActivity activity;

  @Override protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertEquals("Emulator only", "1", shell("getprop ro.kernel.qemu").trim());
    assertTrue("Verified host backup required", new File(context.getFilesDir(), "assistant-c03-authorized").isFile());
    context.deleteDatabase("assistant_chats.db");
    context.getSharedPreferences("zikra_preferences", 0).edit()
        .putBoolean("onboarding_complete", true).putString("assistant_mode_v2", "auto")
        .remove("assistant_active_chat").remove("assistant_history_v1").commit();
    new AssistantConnection(context).save("https://example.test/v1/assistant", CODE);
  }

  @Override protected void tearDown() throws Exception {
    close();
    context.deleteDatabase("assistant_chats.db");
    super.tearDown();
  }

  public void testAbsentAndEmptyCapabilitiesDoNotCountAsInternet() {
    assertEquals(AssistantNetwork.State.OFFLINE, AssistantNetwork.classify(null));
    assertEquals(AssistantNetwork.State.UNVALIDATED, AssistantNetwork.classify(new NetworkCapabilities()));
  }

  public void testBodyReadFailuresStayDistinctFromMalformedReply() throws Exception {
    IOException[] errors = {new java.net.SocketTimeoutException("Synthetic read deadline"), new IOException("Synthetic truncated stream")};
    OnlineAssistantClient.Failure[] expected = {OnlineAssistantClient.Failure.TIMEOUT, OnlineAssistantClient.Failure.NETWORK};
    for (int i = 0; i < errors.length; i++) {
      final IOException error = errors[i];
      OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
        ResponseBody body = new ResponseBody() {
          private final BufferedSource source = Okio.buffer(new Source() {
            public long read(Buffer sink, long count) throws IOException { throw error; }
            public Timeout timeout() { return Timeout.NONE; }
            public void close() {}
          });
          public MediaType contentType() { return MediaType.parse("application/json"); }
          public long contentLength() { return -1; }
          public BufferedSource source() { return source; }
        };
        return response(chain.request(), 200, body);
      }).build();
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<OnlineAssistantClient.Failure> failure = new AtomicReference<>();
      try (OnlineAssistantClient client = new OnlineAssistantClient(http)) {
        client.send(new AssistantConnection(context), new JSONObject().put("messages", new JSONArray()), new OnlineAssistantClient.Result() {
          public void complete(String value) { done.countDown(); }
          public void failed() { done.countDown(); }
          public void failed(OnlineAssistantClient.Failure value) { failure.set(value); done.countDown(); }
        });
        assertTrue(done.await(4, TimeUnit.SECONDS));
        assertEquals(expected[i], failure.get());
      }
    }
  }

  public void testCallDeadlineShowsTimeoutAndRetryNeedsFreshConsent() throws Exception {
    launch(true);
    AtomicInteger calls = new AtomicInteger();
    install(new OkHttpClient.Builder().callTimeout(150, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false)
        .addInterceptor(chain -> {
          if (calls.incrementAndGet() == 1) SystemClock.sleep(400);
          return reply(chain.request(), 200, "{\"reply\":\"Jawaban setelah coba lagi\",\"provider\":\"featherless\"}");
        }).build());
    compose("Pesan sintetis untuk timeout"); approve();
    waitFor(() -> !waiting());
    assertEquals(context.getString(R.string.assistant_timeout_failed), text(R.id.aiRequestStatus));
    assertEquals(1, rows().length()); assertEquals(1, calls.get());
    assertTrue(text(R.id.aiMode).contains("Layanan bermasalah"));
    shot("timeout");
    ui(() -> activity.findViewById(R.id.aiRetry).performClick());
    assertTrue(dialog("selectionDialog").isShowing()); assertEquals(1, calls.get());
    approve(); waitFor(() -> rows().length() == 2);
    assertEquals(2, calls.get()); assertEquals("online", rows().getJSONObject(1).getString("mode"));
    assertEquals("Jawaban setelah coba lagi", rows().getJSONObject(1).getString("text"));
    shot("retry");
  }

  public void testManualModesPersistAndDoNotUploadDraft() throws Exception {
    launch(true);
    AtomicInteger calls = new AtomicInteger();
    install(counter(calls));
    ui(() -> ((EditText) activity.findViewById(R.id.aiInput)).setText("Draf lokal yang tetap disimpan"));
    mode(2);
    assertEquals("offline", prefMode()); assertTrue(text(R.id.aiMode).contains("Offline"));
    close(); launch(true); install(counter(calls));
    assertEquals("Draf lokal yang tetap disimpan", text(R.id.aiInput));
    assertTrue(text(R.id.aiMode).contains("Offline"));
    mode(1); assertEquals("online", prefMode()); assertTrue(text(R.id.aiMode).contains("Online"));
    mode(0); assertEquals("auto", prefMode()); assertTrue(text(R.id.aiMode).startsWith("Otomatis"));
    assertEquals(0, calls.get()); assertEquals(0, rows().length());
    mode(2); ui(() -> activity.findViewById(R.id.aiSend).performClick());
    assertEquals(2, rows().length()); assertEquals("offline", rows().getJSONObject(1).getString("mode"));
    assertEquals(0, calls.get()); shot("manual-offline");
  }

  public void testGatewayValidationAndSaveSuppressOldReply() throws Exception {
    launch(true);
    AtomicInteger calls = new AtomicInteger();
    CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
    install(new OkHttpClient.Builder().addInterceptor(chain -> {
      calls.incrementAndGet(); started.countDown();
      try { release.await(8, TimeUnit.SECONDS); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      return reply(chain.request(), 200, "{\"reply\":\"Balasan lama tidak boleh masuk\",\"provider\":\"featherless\"}");
    }).build());
    try {
      compose("Pesan konfigurasi sintetis"); approve(); assertTrue(started.await(3, TimeUnit.SECONDS));
      mode(3);
      AlertDialog form = dialog("modeDialog"); List<EditText> fields = new ArrayList<>();
      edits(form.getWindow().getDecorView(), fields); assertEquals(2, fields.size());
      String[][] invalid = {{"http://example.test/v1/assistant", CODE},
          {"https://api.featherless.ai/v1/assistant", CODE},
          {"https://example.test/v1/assistant?token=x", CODE},
          {"https://example.test/v1/assistant", "sk-provider-key-must-not-be-saved-0123456789"}};
      for (String[] pair : invalid) {
        ui(() -> { fields.get(0).setText(pair[0]); fields.get(1).setText(pair[1]); form.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); });
        assertTrue(form.isShowing()); assertNotNull(fields.get(0).getError());
        assertEquals("https://example.test/v1/assistant", new AssistantConnection(context).endpoint());
      }
      String replacement = CODE + "-replacement";
      ui(() -> { fields.get(0).setText("https://replacement.test/v1/assistant"); fields.get(1).setText(replacement); form.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); });
      assertFalse(form.isShowing()); assertFalse(waiting());
      assertEquals(context.getString(R.string.assistant_cancelled), text(R.id.aiRequestStatus));
      release.countDown(); SystemClock.sleep(450); getInstrumentation().waitForIdleSync();
      assertEquals(1, rows().length()); assertEquals(1, calls.get());
      assertEquals(replacement, new AssistantConnection(context).accessCode());
      assertFalse(context.getSharedPreferences("assistant_gateway", 0).getAll().toString().contains(replacement));
      close(); launch(true);
      assertEquals("https://replacement.test/v1/assistant", new AssistantConnection(context).endpoint());
      assertEquals(1, rows().length()); shot("configured");
    } finally { release.countDown(); }
  }

  public void testModeChangeCancelsPendingReplyAndKeepsRetry() throws Exception {
    launch(true);
    AtomicInteger calls=new AtomicInteger();
    CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
    install(new OkHttpClient.Builder().addInterceptor(chain->{
      if(calls.incrementAndGet()==1){
        started.countDown();
        try{release.await(8,TimeUnit.SECONDS);}catch(InterruptedException error){Thread.currentThread().interrupt();}
      }
      return reply(chain.request(),200,"{\"reply\":\"Balasan setelah ganti mode\",\"provider\":\"featherless\"}");
    }).build());
    try{
      compose("Uji pergantian mode saat mengirim");approve();assertTrue(started.await(3,TimeUnit.SECONDS));
      mode(2);assertFalse(waiting());assertEquals(context.getString(R.string.assistant_cancelled),text(R.id.aiRequestStatus));
      assertEquals(View.VISIBLE,activity.findViewById(R.id.aiRetry).getVisibility());
      release.countDown();SystemClock.sleep(400);getInstrumentation().waitForIdleSync();
      assertEquals(1,rows().length());assertEquals(1,calls.get());shot("mode-cancel");
      mode(1);ui(()->activity.findViewById(R.id.aiRetry).performClick());
      assertTrue(dialog("selectionDialog").isShowing());assertEquals(1,calls.get());
      approve();waitFor(()->rows().length()==2);assertEquals(2,calls.get());
    }finally{release.countDown();}
  }

  public void testBackgroundCancelsWithoutAppendingLateReply() throws Exception {
    launch(true);
    CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    install(new OkHttpClient.Builder().addInterceptor(chain -> {
      calls.incrementAndGet(); started.countDown();
      try { release.await(8, TimeUnit.SECONDS); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      return reply(chain.request(), 200, "{\"reply\":\"Late\",\"provider\":\"featherless\"}");
    }).build());
    try {
      compose("Pesan sebelum background"); approve(); assertTrue(started.await(3, TimeUnit.SECONDS));
      shell("input keyevent 3"); waitFor(() -> !waiting());
      release.countDown(); SystemClock.sleep(450); assertEquals(1, rows().length());
      ui(() -> activity.startActivity(new Intent(context, AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
      waitFor(() -> activity.hasWindowFocus());
      assertEquals(1, calls.get()); assertEquals(1, rows().length()); shot("background-cancel");
    } finally { release.countDown(); }
  }

  /** Host installs real Android validation probes, then restores them after the ready marker. */
  public void testUnvalidatedWifiAndRecoveryNeverUploadDraft() throws Exception {
    String expected = new String(Files.readAllBytes(new File(context.getFilesDir(), "assistant-c03-network.txt").toPath()), StandardCharsets.UTF_8).trim();
    assertTrue(expected.equals("CAPTIVE_PORTAL") || expected.equals("UNVALIDATED"));
    waitFor(() -> new AssistantNetwork(context, () -> {}).state().name().equals(expected));
    ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
    NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
    assertNotNull(caps); assertTrue(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
    assertFalse(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
    launch(false); AtomicInteger calls = new AtomicInteger(); install(counter(calls));
    ui(() -> ((EditText) activity.findViewById(R.id.aiInput)).setText("Draf harus tetap lokal selama jaringan pulih"));
    assertTrue(text(R.id.aiMode).contains("Offline"));
    assertEquals(context.getString(expected.equals("CAPTIVE_PORTAL") ? R.string.assistant_network_login : R.string.assistant_network_unvalidated), text(R.id.aiNetworkNote));
    mode(1); ui(() -> activity.findViewById(R.id.aiSend).performClick());
    assertEquals(0, calls.get()); assertEquals(0, rows().length()); assertFalse(text(R.id.aiInput).isEmpty());
    mode(0); shot(expected.toLowerCase(java.util.Locale.ROOT));
    Files.write(new File(context.getFilesDir(), "assistant-c03-recover-ready").toPath(), expected.getBytes(StandardCharsets.UTF_8));
    waitFor(() -> ((AssistantNetwork) get("network")).validated(), 600);
    assertTrue(text(R.id.aiMode).contains("Online"));
    assertEquals("Draf harus tetap lokal selama jaringan pulih", text(R.id.aiInput));
    assertEquals(0, calls.get()); assertEquals(0, rows().length()); shot("recovered-" + expected);
  }

  private Response response(Request request, int status, ResponseBody body) {
    return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(status).message("Synthetic C03").body(body).build();
  }
  private Response reply(Request request, int status, String json) { return response(request, status, ResponseBody.create(MediaType.parse("application/json"), json)); }
  private OkHttpClient counter(AtomicInteger calls) { return new OkHttpClient.Builder().addInterceptor(chain -> { calls.incrementAndGet(); throw new IOException("Unexpected request"); }).build(); }
  private void install(OkHttpClient http) throws Exception {
    ((OnlineAssistantClient) get("onlineClient")).close();
    Field field = AiActivity.class.getDeclaredField("onlineClient"); field.setAccessible(true); field.set(activity, new OnlineAssistantClient(http));
  }
  private void launch(boolean online) throws Exception {
    if (online) waitFor(() -> new AssistantNetwork(context, () -> {}).validated());
    activity = (AiActivity) getInstrumentation().startActivitySync(new Intent(context, AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    waitFor(() -> activity.hasWindowFocus()); getInstrumentation().waitForIdleSync(); SystemClock.sleep(400);
  }
  private void close() { if (activity != null) { ui(activity::finish); activity = null; } }
  private void compose(String text) { ui(() -> { ((EditText) activity.findViewById(R.id.aiInput)).setText(text); activity.findViewById(R.id.aiSend).performClick(); }); }
  private void approve() throws Exception { AlertDialog dialog = dialog("selectionDialog"); assertTrue(dialog.isShowing()); ui(() -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()); }
  private void mode(int position) throws Exception {
    ui(() -> activity.findViewById(R.id.aiMode).performClick()); AlertDialog dialog = dialog("modeDialog");
    ui(() -> dialog.getListView().performItemClick(null, position, position));
  }
  private Object get(String name) throws Exception { Field field = AiActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(activity); }
  private boolean waiting() throws Exception { return (Boolean) get("waiting"); }
  private JSONArray rows() throws Exception { return (JSONArray) get("messages"); }
  private AlertDialog dialog(String name) throws Exception { return (AlertDialog) get(name); }
  private String prefMode() { return context.getSharedPreferences("zikra_preferences", 0).getString("assistant_mode_v2", ""); }
  private String text(int id) { return ((TextView) activity.findViewById(id)).getText().toString(); }
  private void edits(View view, List<EditText> fields) { if (view instanceof EditText) fields.add((EditText) view); if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) edits(((ViewGroup) view).getChildAt(i), fields); }
  private void ui(Runnable action) { getInstrumentation().runOnMainSync(action); getInstrumentation().waitForIdleSync(); }
  private void waitFor(Callable<Boolean> condition) throws Exception { waitFor(condition, 150); }
  private void waitFor(Callable<Boolean> condition, int polls) throws Exception { for (int i = 0; i < polls; i++) { if (condition.call()) return; SystemClock.sleep(100); } fail("Expected C03 state not reached"); }
  private void shot(String name) throws Exception {
    View mode = activity.findViewById(R.id.aiMode);
    android.graphics.Rect bounds = new android.graphics.Rect();
    assertTrue("Mode remains visible", mode.getGlobalVisibleRect(bounds));
    assertEquals("Entire mode target visible", mode.getHeight(), bounds.height());
    float density = context.getResources().getDisplayMetrics().density;
    assertTrue("Mode keeps an accessible touch target", bounds.width() >= 48 * density);
    assertTrue("Status row stays compact", ((View) mode.getParent()).getHeight() <= 96 * density);
    View status=activity.findViewById(R.id.aiRequestStatus),retry=activity.findViewById(R.id.aiRetry);
    if(status.getVisibility()==View.VISIBLE&&retry.getVisibility()==View.VISIBLE&&!waiting()){
      try{
        waitFor(()->{
          android.graphics.Rect visible=new android.graphics.Rect();
          return status.getGlobalVisibleRect(visible)&&visible.height()==status.getHeight()
              &&retry.getGlobalVisibleRect(visible)&&visible.height()==retry.getHeight();
        });
      }catch(junit.framework.AssertionFailedError failure){UiEvidence.capture(getInstrumentation(),"AssistantC03-"+name+"-failure");throw failure;}
    }
    UiEvidence.capture(getInstrumentation(), "AssistantC03-" + name + "-" + (context.getResources().getConfiguration().fontScale > 1.9 ? "200" : "100"));
  }
  private String shell(String command) throws Exception { try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); } }
}
