package com.zikra.app.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.test.InstrumentationTestCase;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.zikra.app.account.AccountSession;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in check against the protected Vercel preview and an operator-owned synthetic account. */
@SuppressWarnings("deprecation")
public final class SignedSelfServiceStagingTest extends InstrumentationTestCase {
  private static final String PRODUCTION_HOST = "zikra-ai.vercel.app";
  private static final String PREVIEW_HEADER = "Cookie";
  private static final String FIXTURE = "/data/local/tmp/zikra-signed-self-service-staging.json";
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  public void testSignedChatWithoutInstallationAndWithdrawal() throws Exception {
    runAccountScenario(false);
  }

  public void testSignedChatAndSyntheticAccountDeletion() throws Exception {
    runAccountScenario(true);
  }

  private void runAccountScenario(boolean deleteAfterWithdrawal) throws Exception {
    assertTrue("Only an isolated Android emulator can run this fixture", "ranchu".equals(Build.HARDWARE));
    Context target = getInstrumentation().getTargetContext();
    assertEquals("1.9.66", target.getPackageManager().getPackageInfo(target.getPackageName(), 0).versionName);
    assertEquals(0, target.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE);
    JSONObject fixture;
    try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand("cat " + FIXTURE))) {
      byte[] bytes = stream.readNBytes(8193);
      assertTrue("Missing or oversized private fixture", bytes.length > 1 && bytes.length <= 8192);
      fixture = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    }
    HttpUrl preview = HttpUrl.parse(fixture.getString("preview"));
    assertTrue("Use an exact HTTPS Zikra preview deployment", preview != null && preview.isHttps()
        && preview.host().matches("zikra(?:-ai)?-[a-z0-9]+-arkanzahir86-3257s-projects\\.vercel\\.app")
        && preview.port() == 443
        && preview.username().isEmpty() && preview.password().isEmpty()
        && "/".equals(preview.encodedPath()) && preview.query() == null && preview.fragment() == null);
    String previewSecret = fixture.optString("previewAccess");
    assertTrue("Expected a deployment-scoped Vercel session",
        previewSecret.matches("_vercel_jwt=[A-Za-z0-9._~-]{32,4096}"));
    assertFalse("Fixture must contain no installation token", fixture.has("installation"));
    String email = fixture.getString("email");
    String password = fixture.getString("password");
    if (deleteAfterWithdrawal) {
      assertTrue("Deletion requires explicit opt-in for a disposable synthetic address",
          Boolean.TRUE.equals(fixture.opt("allowSyntheticDeletion"))
              && email.matches("zikra-staging-[a-z0-9-]+@example\\.test"));
    }

    SharedPreferences settings = target.getSharedPreferences("zikra_preferences", 0);
    SharedPreferences installation = target.getSharedPreferences("assistant_gateway", 0);
    assertTrue("Start on a fresh emulator without app preferences", settings.getAll().isEmpty());
    assertTrue("Start without an installation credential", installation.getAll().isEmpty());
    OkHttpClient plain = plainWithoutAccountInterceptor();
    HttpUrl health = preview.newBuilder().encodedPath("/health").build();
    try (Response anonymous = plain.newCall(new Request.Builder().url(health).build()).execute()) {
      assertTrue("Preview must require authentication", anonymous.code() == 401
          || anonymous.code() == 403 || (anonymous.code() >= 300 && anonymous.code() < 400));
    }
    try (Response accessible = plain.newCall(new Request.Builder().url(health)
        .header(PREVIEW_HEADER, previewSecret).build()).execute()) {
      assertEquals("Protected preview health", 200, accessible.code());
    }
    FirebaseAuth auth = initializePreviewAuth(target, fixture.getJSONObject("firebase"));

    String token = null;
    boolean declared = false;
    boolean withdrawn = false;
    try {
      FirebaseUser user = Tasks.await(auth.signInWithEmailAndPassword(email, password), 25, TimeUnit.SECONDS).getUser();
      assertNotNull("Synthetic Firebase sign-in failed", user);
      assertTrue("Wrong synthetic UID", fixture.getString("uid").equals(user.getUid()));
      assertTrue("Synthetic email must be verified", AccountSession.hasVerifiedUser());
      token = Tasks.await(user.getIdToken(true), 15, TimeUnit.SECONDS).getToken();
      assertNotNull(token);
      JSONObject status = account(plain, preview, previewSecret, token, "", null);
      assertEquals("Fixture must be unused on this preview", "not_registered", status.optString("status"));
      JSONObject declaration = new JSONObject().put("birthDate", "1990-01-01").put("accepted", true)
          .put("policyVersion", status.getString("policyVersion"));
      JSONObject declaredState = account(plain, preview, previewSecret, token, "/declare", declaration);
      declared = true;
      assertEquals("adult_confirmed", declaredState.optString("status"));

      AssistantConnection connection = new AssistantConnection(target);
      assertTrue(connection.usesAccountGateway());
      assertFalse(connection.hasInstallation());
      assertTrue(connection.configured());
      assertFalse(AccountSession.held());
      AssistantConsent.accept(settings, connection.endpoint());
      AtomicInteger chatRequests = new AtomicInteger();
      OkHttpClient staged = AssistantHttp.create().newBuilder().addInterceptor(chain -> {
        Request request = chain.request();
        if (!"POST".equals(request.method()) || !PRODUCTION_HOST.equals(request.url().host())
            || !"/v1/assistant".equals(request.url().encodedPath()) || request.header("Authorization") != null
            || request.header("X-Zikra-Account-Token") == null) {
          throw new IOException("Unexpected signed chat request");
        }
        chatRequests.incrementAndGet();
        return chain.proceed(request.newBuilder().url(preview.newBuilder().encodedPath("/v1/assistant").build())
            .header(PREVIEW_HEADER, previewSecret).build());
      }).build();
      JSONObject prompt = new JSONObject().put("messages", new JSONArray().put(new JSONObject()
          .put("role", "user").put("content", "Berapa 37 ditambah 26? Jawab singkat dengan angkanya.")));
      try (OnlineAssistantClient client = new OnlineAssistantClient(staged)) {
        ChatResult successful = send(client, connection, prompt);
        assertNull("Staging chat failure", successful.failure.get());
        assertTrue("Expected synthetic answer", successful.reply.get() != null
            && successful.reply.get().matches("(?s).*\\b63\\b.*"));
        assertEquals(1, chatRequests.get());
        assertTrue("Local hold was not persisted", AccountSession.hold());
        assertTrue("Local hold was not applied", AccountSession.held());
        ChatResult denied = send(client, connection, prompt);
        assertEquals(OnlineAssistantClient.Failure.UNAUTHORIZED, denied.failure.get());
        assertEquals("Local hold must stop the request before the wire", 1, chatRequests.get());
      }
      JSONObject state = account(plain, preview, previewSecret, token, "/withdraw", new JSONObject());
      assertEquals("withdrawn", state.optString("status"));
      withdrawn = true;
      Request serverCheck = new Request.Builder().url(preview.newBuilder().encodedPath("/v1/assistant").build())
          .header(PREVIEW_HEADER, previewSecret).header("X-Zikra-Account-Token", token)
          .header("X-Zikra-Provider", "cloudflare").header("X-Zikra-Attachments", "0")
          .post(RequestBody.create(JSON, prompt.toString())).build();
      // A fresh HTTP client measures server denial independently of the APK's local hold.
      try (Response denied = plain.newCall(serverCheck).execute()) {
        assertEquals("Preview must deny the same token after withdrawal", 403, denied.code());
        assertNotNull(denied.body());
        assertEquals("adult_confirmation_required", new JSONObject(denied.body().string()).optString("error"));
      }
      if (deleteAfterWithdrawal) {
        JSONObject deleted = account(plain, preview, previewSecret, token, "/delete",
            new JSONObject().put("confirm", true));
        assertEquals("deleted", deleted.optString("status"));
        try (Response denied = plain.newCall(serverCheck).execute()) {
          assertEquals("Preview must reject the deleted account's token", 401, denied.code());
        }
        auth.signOut();
        try {
          Tasks.await(auth.signInWithEmailAndPassword(email, password), 25, TimeUnit.SECONDS);
          fail("The deleted synthetic account must not sign in");
        } catch (ExecutionException denied) {
          assertTrue(denied.getCause() instanceof FirebaseAuthException);
          String code = ((FirebaseAuthException) denied.getCause()).getErrorCode();
          assertTrue("Unexpected Firebase login result: " + code,
              "ERROR_USER_NOT_FOUND".equals(code) || "ERROR_INVALID_CREDENTIAL".equals(code));
        }
      }
      assertTrue(installation.getAll().isEmpty());
    } finally {
      try {
        if (declared && !withdrawn) account(plain, preview, previewSecret, token, "/withdraw", new JSONObject());
      } finally {
        auth.signOut();
        settings.edit().clear().commit();
        target.getSharedPreferences("zikra_adult_ai_privacy", 0).edit().clear().commit();
      }
    }
  }

  private FirebaseAuth initializePreviewAuth(Context target, JSONObject config) throws Exception {
    String projectId = config.getString("projectId");
    String appId = config.getString("applicationId");
    String apiKey = config.getString("apiKey");
    assertTrue("Expected a separate Firebase project",
        projectId.matches("[a-z][a-z0-9-]{4,28}[a-z0-9]") && !"zikra-79b75".equals(projectId));
    assertTrue("Invalid staging Firebase app ID", appId.matches("1:[0-9]+:android:[a-f0-9]{16,64}"));
    assertTrue("Invalid staging Firebase API key", apiKey.matches("AIza[A-Za-z0-9_-]{30,50}"));
    FirebaseApp production = FirebaseApp.getInstance();
    assertEquals("Expected the unchanged signed candidate", "zikra-79b75", production.getOptions().getProjectId());
    assertNull("Start without a signed-in user", FirebaseAuth.getInstance(production).getCurrentUser());
    production.delete();
    FirebaseOptions options = new FirebaseOptions.Builder()
        .setProjectId(projectId).setApplicationId(appId).setApiKey(apiKey).build();
    return FirebaseAuth.getInstance(FirebaseApp.initializeApp(target.getApplicationContext(), options));
  }

  private OkHttpClient plainWithoutAccountInterceptor() {
    OkHttpClient.Builder client = AssistantHttp.create().newBuilder();
    client.interceptors().clear();
    return client.build();
  }

  private JSONObject account(OkHttpClient http, HttpUrl preview, String previewSecret, String token,
      String action, JSONObject data) throws Exception {
    Request.Builder request = new Request.Builder().url(preview.newBuilder().encodedPath("/v1/adult-account" + action).build())
        .header(PREVIEW_HEADER, previewSecret).header("Authorization", "Bearer " + token);
    if (data != null) request.post(RequestBody.create(JSON, data.toString()));
    try (Response response = http.newCall(request.build()).execute()) {
      assertEquals("Staging account action " + action, 200, response.code());
      assertNotNull(response.body());
      return new JSONObject(response.body().string());
    }
  }

  private static final class ChatResult implements OnlineAssistantClient.Result {
    final CountDownLatch done = new CountDownLatch(1);
    final AtomicReference<String> reply = new AtomicReference<>();
    final AtomicReference<OnlineAssistantClient.Failure> failure = new AtomicReference<>();
    public void complete(String value) { reply.set(value); done.countDown(); }
    public void failed(OnlineAssistantClient.Failure reason) { failure.set(reason); done.countDown(); }
    public void failed() { failed(OnlineAssistantClient.Failure.SERVER); }
  }

  private ChatResult send(OnlineAssistantClient client, AssistantConnection connection, JSONObject prompt) throws Exception {
    ChatResult result = new ChatResult();
    AtomicReference<Exception> error = new AtomicReference<>();
    getInstrumentation().runOnMainSync(() -> {
      try { client.send(connection, prompt, result); }
      catch (Exception failure) { error.set(failure); result.done.countDown(); }
    });
    assertTrue("Signed chat callback timed out", result.done.await(65, TimeUnit.SECONDS));
    if (error.get() != null) throw error.get();
    return result;
  }
}
