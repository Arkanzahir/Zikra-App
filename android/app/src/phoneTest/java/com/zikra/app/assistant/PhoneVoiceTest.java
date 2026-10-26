package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.test.InstrumentationTestCase;
import android.widget.EditText;
import android.widget.TextView;
import com.zikra.app.AiActivity;
import com.zikra.app.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Callable;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit physical-device opt-in; uses the real external recognizer, never fabricated callbacks. */
@SuppressWarnings("deprecation")
public final class PhoneVoiceTest extends InstrumentationTestCase {
  private Context context;
  private AiActivity activity;

  @Override protected void setUp() throws Exception {
    super.setUp();
    context=getInstrumentation().getTargetContext();
    assertFalse("Physical phone required", "1".equals(shell("getprop ro.kernel.qemu").trim()));
    assertFalse("Phone must be unlocked", context.getSystemService(KeyguardManager.class).isDeviceLocked());
    JSONObject permission=read("assistant-phone-c06-authorized.json");
    assertTrue("Physical testing must be explicitly authorized",permission.getBoolean("physicalDeviceAuthorized"));
    assertTrue("Host must verify a fresh backup before isolation",permission.getBoolean("backupHashesVerified"));
    assertTrue("Host must preserve original data separately",permission.getBoolean("originalDataMovedAside"));
    assertEquals(34,context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionCode);
    assertNotNull("Android recognizer must resolve",new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(context.getPackageManager()));
    context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true)
        .putString("assistant_mode_v2","offline").remove("assistant_history_v1").commit();
    new AssistantConnection(context).offline();
    try(ConversationStore store=new ConversationStore(context)){
      context.getSharedPreferences("zikra_preferences",0).edit().putString("assistant_active_chat",store.create().id).commit();
    }
  }

  @Override protected void tearDown() throws Exception {
    try{close();}finally{super.tearDown();}
  }

  public void testPhysicalRecognizerCancellationKeepsDraft() throws Exception {
    launch();
    ui(()->input().setText("Draf sintetis sebelum pembatalan suara"));
    ui(()->activity.findViewById(R.id.aiVoice).performClick());
    AlertDialog consent=voiceDialog();assertTrue(consent.isShowing());
    ui(()->consent.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
    assertTrue("Declining consent keeps draft",input().getText().toString().equals("Draf sintetis sebelum pembatalan suara"));
    beginRecognizer();
    waitFor(()->!activity.hasWindowFocus(),100);
    SystemClock.sleep(400);shell("input keyevent 4");
    waitFor(()->activity.hasWindowFocus(),150);
    assertTrue("External cancel must preserve draft",input().getText().toString().equals("Draf sintetis sebelum pembatalan suara"));
    assertEquals(0,rows().length());
    write("phone-c06-cancel.json",new JSONObject().put("physicalPhone",true).put("realRecognizerStarted",true)
        .put("consentDeclineKeptDraft",true).put("externalCancelKeptDraft",true).put("aiMessagesSent",0).put("passed",true));
  }

  public void testPhysicalMicrophoneProducesReviewedLocalDraft() throws Exception {
    launch();
    ui(()->input().setText("Draf sebelum uji mikrofon nyata"));
    write("phone-c06-ready.json",new JSONObject().put("readyForHumanSpeech",true));
    waitFor(()->new File(context.getFilesDir(),"phone-c06-speak-now").isFile(),1800);
    beginRecognizer();
    waitFor(()->!activity.hasWindowFocus(),100);
    waitFor(()->activity.hasWindowFocus()&&((TextView)activity.findViewById(R.id.aiVoiceStatus)).getText().toString().equals("Pesan siap. Periksa lalu tekan kirim."),900);
    String recognized=input().getText().toString();
    assertTrue("Recognized test phrase must contain belajar and matematika",recognized.toLowerCase(Locale.ROOT).contains("belajar")&&recognized.toLowerCase(Locale.ROOT).contains("matematika"));
    assertTrue("Draft remains bounded",recognized.length()>0&&recognized.length()<=OfflineAssistant.MAX_INPUT_LENGTH);
    assertEquals("Speech never automatically sends a chat message",0,rows().length());
    close();launch();
    assertTrue("Recognized draft survives reopening",recognized.equals(input().getText().toString()));
    assertEquals(0,rows().length());
    write("phone-c06-microphone.json",new JSONObject().put("physicalPhone",true).put("androidApi",android.os.Build.VERSION.SDK_INT)
        .put("realExternalRecognizer",true).put("fabricatedActivityResult",false).put("spokenTestPhraseMatched",true)
        .put("recognizedCharacters",recognized.length()).put("boundedDraft",true).put("draftSurvivesReopen",true)
        .put("aiMessagesSent",0).put("rawTranscriptIncluded",false).put("passed",true));
  }

  private void beginRecognizer() throws Exception {
    ui(()->activity.findViewById(R.id.aiVoice).performClick());
    AlertDialog consent=voiceDialog();assertTrue("Privacy consent precedes recognition",consent.isShowing());
    ui(()->consent.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
  }
  private void launch() throws Exception {
    activity=(AiActivity)getInstrumentation().startActivitySync(new Intent(context,AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
    waitFor(()->activity.hasWindowFocus(),150);getInstrumentation().waitForIdleSync();
  }
  private void close() throws Exception {
    if(activity!=null){AiActivity previous=activity;ui(previous::finish);waitFor(previous::isDestroyed,150);activity=null;}
  }
  private EditText input(){return activity.findViewById(R.id.aiInput);}
  private JSONArray rows() throws Exception{return (JSONArray)field("messages");}
  private AlertDialog voiceDialog() throws Exception{return (AlertDialog)field("voiceDialog");}
  private Object field(String name) throws Exception{Field field=AiActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}
  private void ui(Runnable action){getInstrumentation().runOnMainSync(action);getInstrumentation().waitForIdleSync();}
  private void waitFor(Callable<Boolean> condition,int polls) throws Exception{for(int i=0;i<polls;i++){if(condition.call())return;SystemClock.sleep(100);}fail("Expected physical voice state not reached; no transcript is logged");}
  private JSONObject read(String name) throws Exception{try(InputStream in=new FileInputStream(new File(context.getFilesDir(),name))){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}}
  private void write(String name,JSONObject value) throws Exception{try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))){out.write(value.toString().getBytes(StandardCharsets.UTF_8));}}
  private String shell(String command) throws Exception{try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
}
