package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.content.*;
import android.net.ConnectivityManager;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.AiActivity;
import com.zikra.app.R;
import com.zikra.app.data.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Explicit live emulator checks. Credentials are supplied privately after installation, never in an APK. */
@SuppressWarnings("deprecation")
public final class HostedAssistantTest extends InstrumentationTestCase {
  private Context context;
  private AiActivity activity;
  private String endpoint, token;
  private final Map<String, Map<String, ?>> originals = new HashMap<>();
  private final JSONObject proof = new JSONObject();

  @Override protected void setUp() throws Exception {
    super.setUp(); context = getInstrumentation().getTargetContext();
    assertTrue("Live fixture requires an emulator", "1".equals(shell("getprop ro.kernel.qemu").trim()));
    File privateInput = new File(context.getFilesDir(), "assistant-live.json");
    try (InputStream input = new FileInputStream(privateInput)) {
      JSONObject config = new JSONObject(new String(input.readAllBytes(), StandardCharsets.UTF_8));
      endpoint = config.getString("endpoint"); token = config.getString("token");
    }
    assertTrue("Use the reviewed hosted endpoint", "https://zikra-ai.vercel.app/v1/assistant".equals(endpoint));
    assertTrue("Private installation token required", token.matches("[a-f0-9]{64}"));
    for (String name : new String[] {"zikra_preferences", "assistant_gateway"})
      originals.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context.getSharedPreferences("zikra_preferences", 0).edit().putBoolean("onboarding_complete", true)
        .putString("assistant_history_v1", "[]").putString("profile_display_name", "[DATA UJI EMULATOR]").commit();
    new AssistantConnection(context).save(endpoint, token);
    proof.put("scope", "emulator-APK-HTTPS-Vercel-Featherless-real-no-network-interceptor")
        .put("physicalPhoneTested", false).put("actualMicrophoneTested", false);
  }

  @Override protected void tearDown() throws Exception {
    try {
      if (activity != null) getInstrumentation().runOnMainSync(() -> activity.finish());
      getInstrumentation().waitForIdleSync();
      for (Map.Entry<String, Map<String, ?>> item : originals.entrySet()) {
        SharedPreferences.Editor editor = context.getSharedPreferences(item.getKey(), 0).edit().clear();
        for (Map.Entry<String, ?> entry : item.getValue().entrySet()) {
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
    } finally { super.tearDown(); }
  }

  public void testHostedConversationConsentWrongTokenCancelAndRetry() throws Exception {
    open(null); draft("Jawab singkat dalam bahasa Indonesia: siap membantu belajar.", false, false);
    AlertDialog consent = (AlertDialog) field(activity, "selectionDialog");
    screenshot("consent-live.png");
    getInstrumentation().runOnMainSync(() -> consent.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
    getInstrumentation().waitForIdleSync(); assertEquals(1, messages().length());
    assertEquals(View.VISIBLE, activity.findViewById(R.id.aiRetry).getVisibility());
    retry(); waitReply(); assertOnline(); screenshot("reply-live.png"); proof.put("hostedReply", true);

    open(null); new AssistantConnection(context).save(endpoint, "0".repeat(64));
    draft("Jawab singkat: siap.", false, true);
    waitStatus(R.string.assistant_auth_failed); assertEquals(1, messages().length());
    screenshot("unauthorized-live.png"); proof.put("wrongTokenNoFallback", true);
    new AssistantConnection(context).save(endpoint, token);
    retry(); waitReply(); assertOnline(); proof.put("retryAfterCorrectingToken", true);

    open(null); draft("Berikan satu saran belajar singkat.", false, true);
    getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.aiSend).performClick());
    waitStatus(R.string.assistant_cancelled); Thread.sleep(600); getInstrumentation().waitForIdleSync();
    assertEquals(1, messages().length()); screenshot("cancel-live.png"); proof.put("cancelNoLateReplyObserved", true);
    writeProof("conversation.json");
  }

  public void testHostedNetworkLossAndRetry() throws Exception {
    int wifi = Settings.Global.getInt(context.getContentResolver(), "wifi_on", 1);
    int data = Settings.Global.getInt(context.getContentResolver(), "mobile_data", 1);
    try {
      shell("svc wifi disable"); shell("svc data disable"); waitNetwork(false);
      open(null); draft("Jawab singkat: siap belajar.", false, true);
      waitStatus(R.string.assistant_network_failed); assertEquals(1, messages().length());
      screenshot("network-off.png"); proof.put("networkLossNoFallback", true);
    } finally {
      shell("svc wifi " + (wifi != 0 ? "enable" : "disable"));
      shell("svc data " + (data != 0 ? "enable" : "disable"));
    }
    waitNetwork(true); retry(); waitReply(); assertOnline(); screenshot("network-retry-live.png");
    proof.put("retryAfterNetworkRestored", true); writeProof("network.json");
  }

  public void testHostedProposalReviewAndTadabburSource() throws Exception {
    int before; long created = 0; String actionId = null;
    try (ZikraDbHelper db = new ZikraDbHelper(context)) { before = db.getTasks().size(); }
    try {
      open(null); draft("Buat satu tugas belajar berjudul Latihan Zikra tanpa tenggat. Jangan simpan otomatis.", true, true);
      waitReply(); assertOnline(); JSONObject proposal = messages().getJSONObject(1).optJSONObject("proposal");
      assertNotNull("Real provider must return a reviewable proposal", proposal); actionId = proposal.getString("id");
      try (ZikraDbHelper db = new ZikraDbHelper(context)) { assertEquals(before, db.getTasks().size()); }
      View review = findText(activity.getWindow().getDecorView(), context.getString(R.string.assistant_review_task));
      assertNotNull(review); getInstrumentation().runOnMainSync(() -> review.performClick()); getInstrumentation().waitForIdleSync();
      AlertDialog dialog = (AlertDialog) field(activity, "taskDialog");
      ArrayList<EditText> edits = new ArrayList<>(); findEdits(dialog.getWindow().getDecorView(), edits);
      assertFalse(edits.isEmpty());
      getInstrumentation().runOnMainSync(() -> edits.get(0).setText("[DATA UJI HOSTED] Latihan Zikra"));
      screenshot("proposal-review-live.png");
      getInstrumentation().runOnMainSync(() -> {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
      });
      getInstrumentation().waitForIdleSync();
      try (ZikraDbHelper db = new ZikraDbHelper(context)) {
        for (Task task : db.getTasks()) if ("[DATA UJI HOSTED] Latihan Zikra".equals(task.title)) {
          created = task.id; assertFalse(task.completed); assertFalse(task.reminder);
        }
        assertTrue(created > 0); assertEquals(before + 1, db.getTasks().size()); assertTrue(db.assistantProposalApplied(actionId));
      }
      proof.put("realProposalRequiresReview", true).put("doubleSaveCreatesOneTask", true);
    } finally {
      try (ZikraDbHelper db = new ZikraDbHelper(context)) {
        if (created != 0) db.deleteTask(created);
        if (actionId != null) db.getWritableDatabase().delete("assistant_actions", "action_id=?", new String[] {actionId});
        assertEquals(before, db.getTasks().size());
      }
    }
    Intent verse = new Intent(context, AiActivity.class).putExtra("tadabbur_chapter", 1).putExtra("tadabbur_verse", 2);
    open(verse); draft("Jelaskan makna ayat ini secara singkat berdasarkan petikan tafsir sumber.", false, true);
    waitReply(); assertOnline(); JSONObject source = messages().getJSONObject(1).getJSONObject("source");
    assertEquals("https://equran.id/tafsir/1", source.getString("url"));
    assertEquals(1, source.getInt("chapter")); assertEquals(2, source.getInt("number"));
    assertTrue(source.getString("text").trim().length() >= 10); screenshot("tadabbur-live.png");
    proof.put("sourcedTadabbur", true).put("testTaskRemoved", true); writeProof("actions-source.json");
  }

  private void open(Intent intent) throws Exception {
    if (activity != null) { getInstrumentation().runOnMainSync(() -> activity.finish()); getInstrumentation().waitForIdleSync(); }
    context.getSharedPreferences("zikra_preferences", 0).edit().putString("assistant_history_v1", "[]").commit();
    activity = (AiActivity) getInstrumentation().startActivitySync((intent == null ? new Intent(context, AiActivity.class) : intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }
  public void testHostedClassesFromNativeConsentAndReviewedSourceSamples() throws Exception {
    assertTrue("Host backup/isolation required",new File(context.getFilesDir(),"assistant-c04-authorized").isFile());
    context.deleteDatabase("assistant_chats.db");context.deleteDatabase("study-schedule.db");
    int before;try(ZikraDbHelper db=new ZikraDbHelper(context)){before=db.getTasks().size();}
    long classId;
    try(WeeklyClassStore store=new WeeklyClassStore(context)){
      classId=store.save(new WeeklyClass(0,0,"[DATA UJI C04] Matematika","PRIVATE-ROOM","PRIVATE-TEACHER",
          com.zikra.app.location.LocationStore.today(context).plusDays(1).getDayOfWeek().getValue(),480,540,0,3));
    }
    try {
      open(null);draft("Dari jadwal yang saya sertakan, buat satu usulan tugas persiapan Matematika tanpa tenggat. Jangan simpan otomatis.",true,false);
      AlertDialog dialog=(AlertDialog)field(activity,"selectionDialog");ArrayList<CheckBox> choices=new ArrayList<>();checks(dialog.getWindow().getDecorView(),choices);
      getInstrumentation().runOnMainSync(()->choices.get(5).setChecked(true));getInstrumentation().waitForIdleSync();
      screenshot("c04-consent-live.png");
      getInstrumentation().runOnMainSync(()->dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitReply();assertOnline();
      JSONObject proposal=messages().getJSONObject(1).optJSONObject("proposal");assertNotNull("Real class-context proposal",proposal);
      try(ZikraDbHelper db=new ZikraDbHelper(context)){assertEquals(before,db.getTasks().size());assertFalse(db.assistantProposalApplied(proposal.getString("id")));}
      try(WeeklyClassStore store=new WeeklyClassStore(context)){assertEquals(1,store.find(classId).revision);}
      proof.put("classesConsentHostedProposal",true).put("noTaskOrClassWriteWithoutReview",true);
      screenshot("c04-class-reply-live.png");
      JSONArray samples=new JSONArray();
      for(int[] verse:new int[][]{{1,2},{113,1}}){
        context.deleteDatabase("assistant_chats.db");
        open(new Intent(context,AiActivity.class).putExtra("tadabbur_chapter",verse[0]).putExtra("tadabbur_verse",verse[1]));
        draft("Ringkas hanya makna ayat "+verse[0]+":"+verse[1]+" dalam satu kalimat berdasarkan petikan; jangan bahas ayat lain atau menambah kutipan.",false,true);
        waitReply();assertOnline();JSONObject answer=messages().getJSONObject(1),source=answer.getJSONObject("source");
        assertEquals(verse[0],source.getInt("chapter"));assertEquals(verse[1],source.getInt("number"));
        assertNotNull(OnlineAssistantClient.verifiedSource(source,new JSONObject().put("chapter",verse[0]).put("number",verse[1])));
        samples.put(new JSONObject().put("chapter",verse[0]).put("verse",verse[1]).put("reply",answer.getString("text")).put("source",source));
        screenshot("c04-tafsir-"+verse[0]+"-"+verse[1]+".png");
      }
      proof.put("sourceSamples",samples);writeProof("c04-hosted.json");
    } finally {try(WeeklyClassStore store=new WeeklyClassStore(context)){WeeklyClass row=store.find(classId);if(row!=null)store.delete(row.id,row.revision);}}
  }
  private void draft(String text, boolean proposals, boolean send) throws Exception {
    getInstrumentation().runOnMainSync(() -> { ((EditText) activity.findViewById(R.id.aiInput)).setText(text); activity.findViewById(R.id.aiSend).performClick(); });
    getInstrumentation().waitForIdleSync(); AlertDialog dialog = (AlertDialog) field(activity, "selectionDialog");
    assertTrue(dialog.isShowing()); ArrayList<CheckBox> checks = new ArrayList<>(); checks(dialog.getWindow().getDecorView(), checks);
    assertEquals(6, checks.size()); for (CheckBox check : checks) assertFalse(check.isChecked());
    getInstrumentation().runOnMainSync(() -> { if (proposals) checks.get(4).setChecked(true); if (send) dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); });
    getInstrumentation().waitForIdleSync();
  }
  private void retry() throws Exception {
    getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.aiRetry).performClick()); getInstrumentation().waitForIdleSync();
    AlertDialog dialog = (AlertDialog) field(activity, "selectionDialog"); assertTrue(dialog.isShowing());
    getInstrumentation().runOnMainSync(() -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
  }
  private JSONArray messages() throws Exception { return (JSONArray) field(activity, "messages"); }
  private void waitReply() throws Exception {
    long end = System.currentTimeMillis() + 55000;
    while (System.currentTimeMillis() < end) {
      getInstrumentation().waitForIdleSync(); if (messages().length() == 2) return;
      if (activity.findViewById(R.id.aiRetry).getVisibility() == View.VISIBLE) fail("Hosted request failed; inspect the sanitized UI status");
      Thread.sleep(100);
    }
    fail("Hosted reply timeout");
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
    fail("Expected sanitized UI status not displayed");
  }
  private void waitNetwork(boolean connected) throws Exception {
    long end = System.currentTimeMillis() + 30000;
    ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    while (System.currentTimeMillis() < end) {
      android.net.NetworkInfo network = manager.getActiveNetworkInfo();
      if ((network != null && network.isConnected()) == connected) { Thread.sleep(300); return; }
      Thread.sleep(200);
    }
    fail("Emulator network state did not settle");
  }
  private String shell(String command) throws Exception {
    try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
  private static Object field(Object object, String name) throws Exception {
    Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
  }
  private static void checks(View view, List<CheckBox> out) {
    if (view instanceof CheckBox) out.add((CheckBox) view);
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) checks(((ViewGroup) view).getChildAt(i), out);
  }
  private static void findEdits(View view, List<EditText> out) {
    if (view instanceof EditText) out.add((EditText) view);
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) findEdits(((ViewGroup) view).getChildAt(i), out);
  }
  private static View findText(View view, String text) {
    if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
      View found = findText(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
    }
    return null;
  }
  private File proofDirectory() { File dir = new File(context.getExternalFilesDir(null), "hosted-ai-proof"); dir.mkdirs(); return dir; }
  private void screenshot(String name) throws Exception {
    try (FileOutputStream output = new FileOutputStream(new File(proofDirectory(), name))) {
      assertTrue(getInstrumentation().getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
    }
  }
  private void writeProof(String name) throws Exception {
    try (FileOutputStream output = new FileOutputStream(new File(proofDirectory(), name))) {
      output.write(proof.put("passed", true).toString(2).getBytes(StandardCharsets.UTF_8));
    }
  }
}
