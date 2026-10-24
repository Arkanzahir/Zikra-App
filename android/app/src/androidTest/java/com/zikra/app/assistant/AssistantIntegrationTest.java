package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.content.*;
import android.os.ParcelFileDescriptor;
import android.speech.RecognizerIntent;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.AiActivity;
import com.zikra.app.R;
import com.zikra.app.data.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import org.json.*;

/** Controlled native checks, not a claim of physical microphone or hosted provider testing. */
@SuppressWarnings("deprecation")
public final class AssistantIntegrationTest extends InstrumentationTestCase {
  private Context context;
  private final Map<String, Map<String, ?>> originals = new HashMap<>();
  private AiActivity activity;
  private static final String ID = "8da51639-4899-42f0-9dc4-66723b358667";
  private static final String CODE = "private-test-code-32-characters-long";

  @Override protected void setUp() throws Exception {
    super.setUp(); context = getInstrumentation().getTargetContext();
    try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))) {
      assertEquals("Emulator-only fixtures", "1", new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim());
    }
    for (String name : new String[] {"zikra_preferences", "assistant_gateway"})
      originals.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context.getSharedPreferences("zikra_preferences", 0).edit().putBoolean("onboarding_complete", true)
        .putString("assistant_history_v1", "[]").putString("profile_display_name", "PRIVATE-FIXTURE-NAME").commit();
    new AssistantConnection(context).save("https://example.test/v1/assistant", CODE);
  }

  @Override protected void tearDown() throws Exception {
    if (activity != null) getInstrumentation().runOnMainSync(() -> activity.finish());
    for (Map.Entry<String, Map<String, ?>> prefs : originals.entrySet()) {
      SharedPreferences.Editor editor = context.getSharedPreferences(prefs.getKey(), 0).edit().clear();
      for (Map.Entry<String, ?> entry : prefs.getValue().entrySet()) {
        Object value = entry.getValue(); String key = entry.getKey();
        if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Set) editor.putStringSet(key, new HashSet<>((Set<String>) value));
      }
      editor.commit();
    }
    super.tearDown();
  }

  public void testNativeClientMapsSuccessAuthQuotaTimeoutServerAndMalformedWithoutFallback() throws Exception {
    int[] codes = {200, 401, 429, 504, 502, 503, 200, 200};
    OnlineAssistantClient.Failure[] failures = {null, OnlineAssistantClient.Failure.UNAUTHORIZED,
      OnlineAssistantClient.Failure.QUOTA, OnlineAssistantClient.Failure.TIMEOUT,
      OnlineAssistantClient.Failure.SERVER, OnlineAssistantClient.Failure.SOURCE,
      OnlineAssistantClient.Failure.INVALID_RESPONSE, OnlineAssistantClient.Failure.INVALID_RESPONSE};
    for (int i = 0; i < codes.length; i++) {
      final int code = codes[i];
      final String json = i == 5 ? "{\"error\":\"source_unavailable\"}" : i == 6 ? "{"
          : i == 7 ? "{\"reply\":42,\"provider\":\"featherless\"}"
          : "{\"reply\":\"Synthetic reply\",\"provider\":\"featherless\"}";
      OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
        assertEquals("https", chain.request().url().scheme());
        assertEquals("Bearer " + CODE, chain.request().header("Authorization"));
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(code).message("Synthetic").body(ResponseBody.create(MediaType.parse("application/json"), json)).build();
      }).build();
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<String> answer = new AtomicReference<>();
      AtomicReference<OnlineAssistantClient.Failure> failure = new AtomicReference<>();
      try (OnlineAssistantClient client = new OnlineAssistantClient(http)) {
        client.send(new AssistantConnection(context), new JSONObject().put("messages", new JSONArray()
            .put(new JSONObject().put("role", "user").put("content", "Synthetic message"))), new OnlineAssistantClient.Result() {
          @Override public void complete(String value) { answer.set(value); done.countDown(); }
          @Override public void failed() { done.countDown(); }
          @Override public void failed(OnlineAssistantClient.Failure value) { failure.set(value); done.countDown(); }
        });
        assertTrue("Native callback", done.await(5, TimeUnit.SECONDS));
        assertEquals(failures[i], failure.get());
        assertEquals(i == 0 ? "Synthetic reply" : null, answer.get());
      }
    }
  }

  public void testCancellationSuppressesLateNativeCallback() throws Exception {
    CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
    AtomicInteger callbacks = new AtomicInteger();
    OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
      started.countDown();
      try { release.await(3, TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
      return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
          .message("Synthetic").body(ResponseBody.create(MediaType.parse("application/json"),
            "{\"reply\":\"Late reply\",\"provider\":\"featherless\"}")).build();
    }).build();
    try (OnlineAssistantClient client = new OnlineAssistantClient(http)) {
      client.send(new AssistantConnection(context), new JSONArray().put(new JSONObject().put("role", "user").put("text", "Synthetic")),
          new OnlineAssistantClient.Result() {
            @Override public void complete(String value) { callbacks.incrementAndGet(); }
            @Override public void failed() { callbacks.incrementAndGet(); }
          });
      assertTrue(started.await(3, TimeUnit.SECONDS)); client.cancel(); release.countDown();
      Thread.sleep(250); getInstrumentation().waitForIdleSync();
      assertEquals(0, callbacks.get());
    } finally { release.countDown(); }
  }

  public void testVersionThreeUpgradeAndReviewedActionsAreAtomicOnceAndRejectStaleOrCompletedTasks() throws Exception {
    String name = "assistant-actions-native-test.db";
    context.deleteDatabase(name);
    try {
      long original, completed;
      try (ZikraDbHelper db = new ZikraDbHelper(context, name, Clock.systemUTC())) {
        original = db.addAcademicTask("Original task", 0, false);
        completed = db.addTask("Completed task", ZikraDbHelper.CATEGORY_STUDY); db.setCompleted(completed, true);
        db.addTask("Retained worship", ZikraDbHelper.CATEGORY_IBADAH);
        db.getWritableDatabase().execSQL("DROP TABLE assistant_actions");
        db.getWritableDatabase().setVersion(3);
      }
      TaskProposal addition = new TaskProposal(ID, "add_task", "Reviewed addition", 0, 0, "", 0);
      long created;
      try (ZikraDbHelper db = new ZikraDbHelper(context, name, Clock.systemUTC())) {
        assertEquals(4, db.getReadableDatabase().getVersion()); assertEquals(3, db.getTasks().size());
        created = db.applyReviewedProposal(addition);
        assertEquals(created, db.applyReviewedProposal(addition)); assertEquals(4, db.getTasks().size());
      }
      try (ZikraDbHelper db = new ZikraDbHelper(context, name, Clock.systemUTC())) {
        assertEquals(created, db.applyReviewedProposal(addition)); assertEquals(4, db.getTasks().size());
        TaskProposal edit = new TaskProposal(UUID.randomUUID().toString(), "edit_task", "Reviewed edit", 0, original, "Original task", 0);
        assertEquals(original, db.applyReviewedProposal(edit));
        TaskProposal stale = new TaskProposal(UUID.randomUUID().toString(), "edit_task", "Must reject", 0, original, "Original task", 0);
        try { db.applyReviewedProposal(stale); fail("Stale edit applied"); } catch (IllegalStateException expected) { }
        assertFalse(db.assistantProposalApplied(stale.id));
        TaskProposal done = new TaskProposal(UUID.randomUUID().toString(), "edit_task", "Must reject", 0, completed, "Completed task", 0);
        try { db.applyReviewedProposal(done); fail("Completed task edited"); } catch (IllegalStateException expected) { }
        assertEquals(1, db.getProgress(ZikraDbHelper.CATEGORY_STUDY).completed);
        assertEquals(4, db.getTasks().size());
      }
    } finally { context.deleteDatabase(name); }
  }

  public void testConsentDefaultsCancelAndVoiceResultsRemainDrafts() throws Exception {
    activity = (AiActivity) getInstrumentation().startActivitySync(new Intent(context, AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    getInstrumentation().runOnMainSync(() -> {
      ((EditText) activity.findViewById(R.id.aiInput)).setText("Synthetic private test message");
      activity.findViewById(R.id.aiSend).performClick();
    });
    getInstrumentation().waitForIdleSync();
    AlertDialog dialog = (AlertDialog) field(activity, "selectionDialog");
    assertTrue(dialog.isShowing());
    ArrayList<CheckBox> choices = new ArrayList<>(); findChecks(dialog.getWindow().getDecorView(), choices);
    assertEquals(5, choices.size()); for (CheckBox choice : choices) assertFalse(choice.isChecked());
    assertFalse(allText(dialog.getWindow().getDecorView()).contains("PRIVATE-FIXTURE-NAME"));
    JSONObject request = AssistantSelection.build(context, (JSONArray) field(activity, "messages"), false, false, false, false, false, null);
    assertEquals(0, request.getJSONObject("context").length()); assertEquals(1, request.getJSONArray("messages").length());
    saveScreenshot("consent.png");
    getInstrumentation().runOnMainSync(() -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
    getInstrumentation().waitForIdleSync();
    assertEquals(View.VISIBLE, activity.findViewById(R.id.aiRetry).getVisibility());
    assertEquals(1, ((JSONArray) field(activity, "messages")).length());
    getInstrumentation().runOnMainSync(() -> invokeVoiceResult(activity.RESULT_OK,
        new Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, new ArrayList<>(Arrays.asList("Voice draft only")))));
    assertEquals("Voice draft only", ((EditText) activity.findViewById(R.id.aiInput)).getText().toString());
    getInstrumentation().runOnMainSync(() -> invokeVoiceResult(activity.RESULT_CANCELED, null));
    assertEquals("Voice draft only", ((EditText) activity.findViewById(R.id.aiInput)).getText().toString());
    assertEquals(1, ((JSONArray) field(activity, "messages")).length());
    getInstrumentation().runOnMainSync(() -> invokeVoiceResult(activity.RESULT_OK,
        new Intent().putIntegerArrayListExtra(RecognizerIntent.EXTRA_RESULTS, new ArrayList<>(Arrays.asList(123)))));
    assertEquals("Voice draft only", ((EditText) activity.findViewById(R.id.aiInput)).getText().toString());
    saveScreenshot("voice-draft.png");
    boolean available = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(context.getPackageManager()) != null;
    File proof = new File(context.getExternalFilesDir(null), "c-ai-proof/recognizer.json");
    try (FileOutputStream output = new FileOutputStream(proof)) {
      output.write(new JSONObject().put("recognizerAvailable", available).put("actualMicrophoneTested", false).toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  public void testActualScreenLoadingCancelRetryAndOnlineLabel() throws Exception {
    activity = (AiActivity) getInstrumentation().startActivitySync(new Intent(context, AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    AtomicInteger calls = new AtomicInteger(); CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
    installClient(new OkHttpClient.Builder().addInterceptor(chain -> {
      if (calls.incrementAndGet() == 1) {
        started.countDown();
        try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
      }
      return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic")
          .body(ResponseBody.create(MediaType.parse("application/json"), "{\"reply\":\"Controlled online reply\",\"provider\":\"featherless\"}")).build();
    }).build());
    try {
      sendDraft("Synthetic retry test", false);
      assertTrue(started.await(3, TimeUnit.SECONDS));
      assertTrue((Boolean) field(activity, "waiting"));
      assertEquals(context.getString(R.string.assistant_cancel_send), activity.findViewById(R.id.aiSend).getContentDescription());
      saveScreenshot("loading.png");
      getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.aiSend).performClick());
      release.countDown(); Thread.sleep(250); getInstrumentation().waitForIdleSync();
      assertFalse((Boolean) field(activity, "waiting")); assertEquals(1, ((JSONArray) field(activity, "messages")).length());
      getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.aiRetry).performClick());
      getInstrumentation().waitForIdleSync();
      AlertDialog review = (AlertDialog) field(activity, "selectionDialog");
      getInstrumentation().runOnMainSync(() -> review.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
      waitForReply(2);
      JSONObject answer = ((JSONArray) field(activity, "messages")).getJSONObject(1);
      assertEquals("online", answer.getString("mode")); assertEquals("Controlled online reply", answer.getString("text"));
      assertEquals(2, calls.get()); saveScreenshot("online-reply.png");
    } finally { release.countDown(); }
  }

  public void testActualReviewDoesNotSaveUntilConfirmedAndDoubleTapDoesNotDuplicate() throws Exception {
    activity = (AiActivity) getInstrumentation().startActivitySync(new Intent(context, AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    installClient(new OkHttpClient.Builder().addInterceptor(chain -> {
      okio.Buffer buffer = new okio.Buffer(); chain.request().body().writeTo(buffer);
      try {
        JSONObject request = new JSONObject(buffer.readUtf8());
        assertTrue(request.getJSONObject("features").getBoolean("taskProposals"));
        assertEquals(0, request.getJSONObject("context").length());
        JSONObject response = new JSONObject().put("reply", "Usulan belum disimpan.").put("provider", "featherless")
            .put("proposal", new JSONObject().put("id", request.getString("requestId")).put("kind", "add_task")
                .put("title", "[DATA UJI C04] Proposed task").put("dueAt", 0));
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic")
            .body(ResponseBody.create(MediaType.parse("application/json"), response.toString())).build();
      } catch (JSONException invalid) { throw new IOException(invalid); }
    }).build());
    int before;
    try (ZikraDbHelper db = new ZikraDbHelper(context)) { before = db.getTasks().size(); }
    long created = 0; String actionId = null;
    try {
      sendDraft("Synthetic propose task", true); waitForReply(2);
      JSONObject proposal = ((JSONArray) field(activity, "messages")).getJSONObject(1).getJSONObject("proposal");
      actionId = proposal.getString("id");
      try (ZikraDbHelper db = new ZikraDbHelper(context)) { assertEquals(before, db.getTasks().size()); }
      View action = findText(activity.getWindow().getDecorView(), context.getString(R.string.assistant_review_task));
      assertNotNull(action); getInstrumentation().runOnMainSync(() -> action.performClick()); getInstrumentation().waitForIdleSync();
      AlertDialog dialog = (AlertDialog) field(activity, "taskDialog"); assertTrue(dialog.isShowing());
      saveScreenshot("task-review.png");
      getInstrumentation().runOnMainSync(() -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
      getInstrumentation().waitForIdleSync();
      try (ZikraDbHelper db = new ZikraDbHelper(context)) { assertEquals(before, db.getTasks().size()); }
      getInstrumentation().runOnMainSync(() -> action.performClick()); getInstrumentation().waitForIdleSync();
      AlertDialog confirm = (AlertDialog) field(activity, "taskDialog");
      getInstrumentation().runOnMainSync(() -> {
        confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
      });
      getInstrumentation().waitForIdleSync();
      try (ZikraDbHelper db = new ZikraDbHelper(context)) {
        for (Task task : db.getTasks()) if ("[DATA UJI C04] Proposed task".equals(task.title)) {
          created = task.id; assertFalse(task.completed); assertFalse(task.reminder);
        }
        assertTrue(created > 0); assertEquals(before + 1, db.getTasks().size()); assertTrue(db.assistantProposalApplied(actionId));
      }
    } finally {
      try (ZikraDbHelper db = new ZikraDbHelper(context)) {
        if (created != 0) db.deleteTask(created);
        if (actionId != null) db.getWritableDatabase().delete("assistant_actions", "action_id=?", new String[] {actionId});
      }
    }
  }

  private void installClient(OkHttpClient http) throws Exception {
    ((OnlineAssistantClient) field(activity, "onlineClient")).close();
    Field field = AiActivity.class.getDeclaredField("onlineClient"); field.setAccessible(true);
    field.set(activity, new OnlineAssistantClient(http)); getInstrumentation().waitForIdleSync();
  }
  private void sendDraft(String value, boolean proposals) throws Exception {
    getInstrumentation().runOnMainSync(() -> {
      ((EditText) activity.findViewById(R.id.aiInput)).setText(value); activity.findViewById(R.id.aiSend).performClick();
    });
    getInstrumentation().waitForIdleSync();
    AlertDialog dialog = (AlertDialog) field(activity, "selectionDialog");
    if (proposals) {
      ArrayList<CheckBox> checks = new ArrayList<>(); findChecks(dialog.getWindow().getDecorView(), checks);
      getInstrumentation().runOnMainSync(() -> checks.get(4).setChecked(true));
    }
    getInstrumentation().runOnMainSync(() -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
  }
  private void waitForReply(int count) throws Exception {
    long deadline = android.os.SystemClock.elapsedRealtime() + 5000;
    while (android.os.SystemClock.elapsedRealtime() < deadline) {
      getInstrumentation().waitForIdleSync();
      if (((JSONArray) field(activity, "messages")).length() == count) return;
      Thread.sleep(20);
    }
    fail("Expected reply not displayed");
  }
  private static View findText(View view, String text) {
    if (view instanceof TextView && text.equals(((TextView) view).getText().toString())) return view;
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
      View found = findText(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
    }
    return null;
  }

  private void invokeVoiceResult(int code, Intent data) {
    try {
      Method method = AiActivity.class.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
      method.setAccessible(true); method.invoke(activity, 4001, code, data);
    } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
  }
  private static Object field(Object object, String name) throws Exception {
    Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
  }
  private static void findChecks(View view, List<CheckBox> result) {
    if (view instanceof CheckBox) result.add((CheckBox) view);
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) findChecks(((ViewGroup) view).getChildAt(i), result);
  }
  private static String allText(View view) {
    StringBuilder text = new StringBuilder();
    if (view instanceof TextView) text.append(((TextView) view).getText());
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) text.append(allText(((ViewGroup) view).getChildAt(i)));
    return text.toString();
  }
  private void saveScreenshot(String name) throws Exception {
    File directory = new File(context.getExternalFilesDir(null), "c-ai-proof"); directory.mkdirs();
    try (FileOutputStream output = new FileOutputStream(new File(directory, name))) {
      assertTrue(getInstrumentation().getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
    }
  }
}
