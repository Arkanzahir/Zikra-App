package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.net.ConnectivityManager;
import android.os.ParcelFileDescriptor;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.AiActivity;
import com.zikra.app.R;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Opt-in physical-device check. No profile/task uploads, database writes, or bundled credentials. */
@SuppressWarnings("deprecation")
public final class PhoneAssistantTest extends InstrumentationTestCase {
  private Context context;
  private AiActivity activity;
  private String endpoint, token, quotaToken, directory;
  private int wifiBefore, dataBefore;
  private boolean historyExisted, hadGateway, successful, prepared;
  private String originalHistory;
  private final Map<String, Object> gatewayBefore = new HashMap<>();
  private final JSONObject proof = new JSONObject();

  @Override protected void setUp() throws Exception {
    super.setUp(); context = getInstrumentation().getTargetContext();
    assertFalse("This suite requires an explicitly authorized physical phone", "1".equals(shell("getprop ro.kernel.qemu").trim()));
    assertFalse("Unlock the phone before this UI check", ((android.app.KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE)).isDeviceLocked());
    JSONObject config;
    try (InputStream input = new FileInputStream(new File(context.getFilesDir(), "assistant-phone-live.json"))) {
      config = new JSONObject(new String(input.readAllBytes(), StandardCharsets.UTF_8));
    }
    assertTrue("Explicit phone authorization required", config.getBoolean("physicalDeviceAuthorized"));
    assertEquals(26, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
    endpoint = config.getString("endpoint"); token = config.getString("token"); quotaToken = config.getString("quotaToken");
    directory = config.getString("proofDirectory"); wifiBefore = config.getInt("wifiBefore"); dataBefore = config.getInt("dataBefore");
    assertTrue("Reviewed endpoint required", "https://zikra-ai.vercel.app/v1/assistant".equals(endpoint));
    assertTrue("Private gateway credentials required", token.matches("[a-f0-9]{64}") && quotaToken.matches("[a-f0-9]{64}"));
    assertFalse("Quota fixture must use a separate token", token.equals(quotaToken));
    assertTrue("Private proof directory required", directory.matches("phone-proof-[a-f0-9]{16}"));
    assertTrue("Original network flags required", (wifiBefore == 0 || wifiBefore == 1) && (dataBefore == 0 || dataBefore == 1));
    SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
    historyExisted = prefs.contains("assistant_history_v1"); originalHistory = prefs.getString("assistant_history_v1", "[]");
    gatewayBefore.putAll(context.getSharedPreferences("assistant_gateway", 0).getAll());
    hadGateway = !new AssistantConnection(context).endpoint().isEmpty();
    prepared = true;
    new AssistantConnection(context).save(endpoint, token);
    proof.put("scope", "physical-phone-APK-Vercel-Featherless-real-no-interceptor")
        .put("physicalPhoneTested", true).put("actualMicrophoneTested", false)
        .put("laptopOffTested", false).put("personalContextUploaded", false)
        .put("quotaFixtureSeededToDailyLimit", true).put("gatewayConfiguredBefore", hadGateway);
  }

  @Override protected void tearDown() throws Exception {
    try {
      if (!prepared) return;
      restoreNetwork();
      if (activity != null) { getInstrumentation().runOnMainSync(() -> activity.finish()); getInstrumentation().waitForIdleSync(); }
      SharedPreferences.Editor history = context.getSharedPreferences("zikra_preferences", 0).edit();
      if (historyExisted) history.putString("assistant_history_v1", originalHistory); else history.remove("assistant_history_v1");
      history.commit();
      if (!successful || hadGateway) restoreGateway();
      else new AssistantConnection(context).save(endpoint, token);
    } finally { super.tearDown(); }
  }

  public void testPhysicalHostedConversationFailuresCancelAndRetry() throws Exception {
    open(); draft("Ini uji teknis. Jawab singkat: siap membantu belajar.", false);
    screenshot("consent.png"); AlertDialog dialog = selection();
    tap(dialog.getButton(AlertDialog.BUTTON_NEGATIVE)); assertEquals(1, messages().length());
    retry(); waitReply(); assertOnline(); screenshot("reply.png"); proof.put("hostedReplyDisplayed", true);

    new AssistantConnection(context).save(endpoint, "0".repeat(64)); open();
    draft("Uji akses. Jawab singkat: siap.", true); waitStatus(R.string.assistant_auth_failed);
    assertEquals(1, messages().length()); screenshot("wrong-token.png"); proof.put("wrongTokenNoFallback", true);
    new AssistantConnection(context).save(endpoint, token); retry(); waitReply(); assertOnline();
    proof.put("retryAfterCorrectToken", true);

    new AssistantConnection(context).save(endpoint, quotaToken); open();
    draft("Uji batas pemakaian. Jawab singkat: siap.", true); waitStatus(R.string.assistant_quota_failed);
    assertEquals(1, messages().length()); screenshot("quota.png"); proof.put("actualServerQuotaShown", true);

    new AssistantConnection(context).save(endpoint, token); open();
    draft("Berikan satu saran belajar singkat.", true); tap(activity.findViewById(R.id.aiSend));
    waitStatus(R.string.assistant_cancelled); Thread.sleep(1000); getInstrumentation().waitForIdleSync();
    assertEquals(1, messages().length()); screenshot("cancel.png"); proof.put("cancelNoLateReplyObserved", true);

    try {
      if (wifiBefore == 1) shell("svc wifi disable");
      if (dataBefore == 1) shell("svc data disable");
      waitNetwork(false); open(); draft("Uji jaringan. Jawab singkat: siap belajar.", true);
      waitStatus(R.string.assistant_network_failed); assertEquals(1, messages().length());
      screenshot("network-off.png"); proof.put("actualNetworkLossNoFallback", true);
    } finally { restoreNetwork(); }
    waitNetwork(true); retry(); waitReply(); assertOnline(); screenshot("network-restored.png");
    proof.put("retryAfterNetworkRestored", true).put("consentContextOffByDefault", true)
        .put("networkFlagsRestored", true).put("passed", true);
    writeProof(); successful = true;
  }

  private void restoreGateway() {
    SharedPreferences.Editor editor = context.getSharedPreferences("assistant_gateway", 0).edit();
    for (String key : new String[] {"endpoint", "iv", "access", "online"}) {
      Object value = gatewayBefore.get(key);
      if (value instanceof String) editor.putString(key, (String) value);
      else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
      else editor.remove(key);
    }
    editor.commit();
  }
  private void restoreNetwork() throws Exception {
    if (wifiBefore == 1) shell("svc wifi enable");
    if (dataBefore == 1) shell("svc data enable");
  }
  private void open() throws Exception {
    if (activity != null) { getInstrumentation().runOnMainSync(() -> activity.finish()); getInstrumentation().waitForIdleSync(); }
    context.getSharedPreferences("zikra_preferences", 0).edit().putString("assistant_history_v1", "[]").commit();
    activity = (AiActivity) getInstrumentation().startActivitySync(new Intent(context, AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().runOnMainSync(() -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    getInstrumentation().waitForIdleSync();
  }
  private void draft(String text, boolean send) throws Exception {
    getInstrumentation().runOnMainSync(() -> { ((EditText) activity.findViewById(R.id.aiInput)).setText(text); activity.findViewById(R.id.aiSend).performClick(); });
    getInstrumentation().waitForIdleSync(); ArrayList<CheckBox> checks = new ArrayList<>();
    findChecks(selection().getWindow().getDecorView(), checks); assertEquals(6, checks.size());
    for (CheckBox check : checks) assertFalse(check.isChecked());
    JSONObject selected = AssistantSelection.build(context, messages(), false, false, false, false, false, null);
    assertEquals(0, selected.getJSONObject("context").length()); assertEquals(1, selected.getJSONArray("messages").length());
    assertEquals(text, selected.getJSONArray("messages").getJSONObject(0).getString("content"));
    if (send) tap(selection().getButton(AlertDialog.BUTTON_POSITIVE));
  }
  private void retry() throws Exception { tap(activity.findViewById(R.id.aiRetry)); tap(selection().getButton(AlertDialog.BUTTON_POSITIVE)); }
  private AlertDialog selection() throws Exception { AlertDialog dialog = (AlertDialog) field("selectionDialog"); assertTrue(dialog.isShowing()); return dialog; }
  private JSONArray messages() throws Exception { return (JSONArray) field("messages"); }
  private Object field(String name) throws Exception { Field field = AiActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(activity); }
  private void tap(View view) throws Exception {
    assertNotNull("Required UI control exists", view);
    getInstrumentation().waitForIdleSync(); Thread.sleep(100);
    assertTrue("Required UI control is visible", view.isShown() && view.getGlobalVisibleRect(new Rect()));
    getInstrumentation().runOnMainSync(() -> view.performClick()); getInstrumentation().waitForIdleSync();
  }
  private void waitReply() throws Exception {
    long end = System.currentTimeMillis() + 55000;
    while (System.currentTimeMillis() < end) {
      getInstrumentation().waitForIdleSync(); if (messages().length() == 2) return;
      if (activity.findViewById(R.id.aiRetry).getVisibility() == View.VISIBLE) fail("Hosted request failed; sanitized status available on phone");
      Thread.sleep(100);
    }
    fail("Hosted phone response timeout");
  }
  private void assertOnline() throws Exception {
    JSONObject answer = messages().getJSONObject(1); assertEquals("online", answer.getString("mode"));
    assertTrue(answer.getString("text").trim().length() > 0);
  }
  private void waitStatus(int resource) throws Exception {
    long end = System.currentTimeMillis() + 55000;
    while (System.currentTimeMillis() < end) {
      getInstrumentation().waitForIdleSync(); TextView status = activity.findViewById(R.id.aiRequestStatus);
      if (status.getVisibility() == View.VISIBLE && context.getString(resource).contentEquals(status.getText())) return;
      Thread.sleep(100);
    }
    fail("Expected phone error/cancel status not displayed");
  }
  private void waitNetwork(boolean connected) throws Exception {
    long end = System.currentTimeMillis() + 30000;
    ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    while (System.currentTimeMillis() < end) {
      android.net.NetworkInfo network = manager.getActiveNetworkInfo();
      if ((network != null && network.isConnected()) == connected) { Thread.sleep(300); return; }
      Thread.sleep(200);
    }
    fail("Physical phone network did not reach the expected state");
  }
  private String shell(String command) throws Exception {
    try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
  private static void findChecks(View view, List<CheckBox> result) {
    if (view instanceof CheckBox) result.add((CheckBox) view);
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) findChecks(((ViewGroup) view).getChildAt(i), result);
  }
  private File proofDirectory() { File result = new File(context.getExternalFilesDir(null), directory); result.mkdirs(); return result; }
  private void screenshot(String name) throws Exception {
    getInstrumentation().waitForIdleSync(); Thread.sleep(350);
    final Bitmap[] bitmap = new Bitmap[1];
    getInstrumentation().runOnMainSync(() -> {
      View view = activity.getWindow().getDecorView();
      try {
        AlertDialog dialog = (AlertDialog) field("selectionDialog");
        if (dialog != null && dialog.isShowing()) view = dialog.getWindow().getDecorView();
      } catch (Exception ignored) { }
      bitmap[0] = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
      view.draw(new Canvas(bitmap[0]));
    });
    try (FileOutputStream output = new FileOutputStream(new File(proofDirectory(), name))) { assertTrue(bitmap[0].compress(Bitmap.CompressFormat.PNG, 100, output)); }
    finally { bitmap[0].recycle(); }
  }
  private void writeProof() throws Exception {
    try (FileOutputStream output = new FileOutputStream(new File(proofDirectory(), "phone-live.json"))) {
      output.write(proof.toString(2).getBytes(StandardCharsets.UTF_8));
    }
  }
}
