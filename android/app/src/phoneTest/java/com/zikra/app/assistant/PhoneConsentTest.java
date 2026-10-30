package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.AiActivity;
import com.zikra.app.R;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import org.json.*;

/** Isolated conversation, synthetic HTTP replies; never submits personal data. */
@SuppressWarnings("deprecation")
public final class PhoneConsentTest extends InstrumentationTestCase {
  private AiActivity activity;
  private Context context;
  private AtomicInteger requests=new AtomicInteger();
  private AtomicReference<JSONObject> captured=new AtomicReference<>();
  private void ui(Runnable action){getInstrumentation().runOnMainSync(action);getInstrumentation().waitForIdleSync();}
  private Object field(String name)throws Exception{Field f=AiActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
  private void waitFor(Callable<Boolean> condition)throws Exception{for(int i=0;i<150;i++){if(condition.call())return;SystemClock.sleep(100);}fail("Condition timed out");}
  private void launch()throws Exception{
    activity=(AiActivity)getInstrumentation().startActivitySync(new Intent(context,AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
    waitFor(()->new AssistantNetwork(context,()->{}).validated());
    ((OnlineAssistantClient)field("onlineClient")).close();
    Field client=AiActivity.class.getDeclaredField("onlineClient");client.setAccessible(true);
    client.set(activity,new OnlineAssistantClient(new OkHttpClient.Builder().addInterceptor(chain->{
      okio.Buffer body=new okio.Buffer();chain.request().body().writeTo(body);
      try{captured.set(new JSONObject(body.readUtf8()));}catch(JSONException e){throw new IOException(e);}
      requests.incrementAndGet();
      return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic")
          .body(ResponseBody.create(MediaType.parse("application/json"),"{\"reply\":\"Jawaban sintetis\",\"provider\":\"featherless\"}")).build();
    }).build()));
  }
  private void send(String text){ui(()->{((EditText)activity.findViewById(R.id.aiInput)).setText(text);activity.findViewById(R.id.aiSend).performClick();});}
  private AlertDialog selection()throws Exception{return (AlertDialog)field("selectionDialog");}
  private void checks(View view,List<CheckBox> result){if(view instanceof CheckBox)result.add((CheckBox)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)checks(((ViewGroup)view).getChildAt(i),result);}
  public void testOneTimePermissionDirectSendReopenExtraContextAndRevocation()throws Exception{
    context=getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(),"phone-c07-backup-verified").isFile());
    try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))){assertFalse("Physical only","1".equals(new String(in.readAllBytes(),StandardCharsets.UTF_8).trim()));}
    SharedPreferences prefs=context.getSharedPreferences("zikra_preferences",0);
    String[] keys={"assistant_active_chat","assistant_mode_v2","assistant_basic_consent_endpoint_v1"};
    Map<String,String> original=new HashMap<>();for(String key:keys)if(prefs.contains(key))original.put(key,prefs.getString(key,""));
    String endpoint=new AssistantConnection(context).endpoint();assertFalse(endpoint.isEmpty());
    String chat=null;
    try(ConversationStore store=new ConversationStore(context)){
      chat=store.create().id;store.rename(chat,"Uji izin sekali sementara");
      prefs.edit().putString(keys[0],chat).putString(keys[1],"auto").remove(keys[2]).commit();
      try{
        launch();assertEquals(chat,field("chatId"));
        send("Uji izin pertama");assertTrue(selection().isShowing());assertEquals(0,requests.get());
        android.graphics.Rect button=new android.graphics.Rect();
        assertTrue(selection().getButton(AlertDialog.BUTTON_POSITIVE).getGlobalVisibleRect(button));
        assertEquals(selection().getButton(AlertDialog.BUTTON_POSITIVE).getHeight(),button.height());
        com.zikra.app.UiEvidence.capture(getInstrumentation(),"Consent-once-first");
        AlertDialog first=selection();ui(()->first.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        assertFalse(AssistantConsent.granted(prefs,endpoint));assertEquals(0,requests.get());
        ui(()->activity.findViewById(R.id.aiRetry).performClick());
        AlertDialog agreed=selection();ui(()->agreed.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        waitFor(()->requests.get()==1&&!(Boolean)field("waiting"));
        assertTrue(AssistantConsent.granted(prefs,endpoint));assertEquals(0,captured.get().getJSONObject("context").length());
        send("Uji pesan kedua langsung");waitFor(()->requests.get()==2&&!(Boolean)field("waiting"));assertFalse(selection().isShowing());
        Field attachment=AiActivity.class.getDeclaredField("attachment");attachment.setAccessible(true);
        attachment.set(activity,new AssistantAttachments.Draft(java.util.UUID.randomUUID().toString(),"izin-uji.txt","text","text/plain","Data sintetis",1,13,new ArrayList<>()));
        Method render=AiActivity.class.getDeclaredMethod("renderAttachment");render.setAccessible(true);
        ui(()->{try{render.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});
        send("Uji lampiran langsung");waitFor(()->requests.get()==3&&!(Boolean)field("waiting"));
        assertFalse(selection().isShowing());assertEquals(1,captured.get().getJSONArray("attachments").length());
        assertEquals(0,captured.get().getJSONObject("context").length());assertNull(field("attachment"));
        com.zikra.app.UiEvidence.capture(getInstrumentation(),"Consent-once-direct");
        ui(activity::finish);activity=null;launch();
        send("Uji setelah buka ulang");waitFor(()->requests.get()==4&&!(Boolean)field("waiting"));
        assertTrue(selection()==null||!selection().isShowing());
        assertFalse(AssistantConsent.granted(prefs,"https://different.example/v1/assistant"));
        ui(()->{((EditText)activity.findViewById(R.id.aiInput)).setText("Uji konteks pilihan");activity.findViewById(R.id.aiTools).performClick();});
        AlertDialog tools=(AlertDialog)field("clearDialog");
        ui(()->tools.getListView().performItemClick(tools.getListView().getChildAt(1),1,1));
        assertTrue(selection().isShowing());ArrayList<CheckBox> choices=new ArrayList<>();checks(selection().getWindow().getDecorView(),choices);
        assertEquals(6,choices.size());for(CheckBox choice:choices)assertFalse(choice.isChecked());
        assertEquals(4,requests.get());AlertDialog extra=selection();ui(()->extra.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        Method settings=AiActivity.class.getDeclaredMethod("showConsentSettings");settings.setAccessible(true);
        ui(()->{try{settings.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});
        AlertDialog permission=(AlertDialog)field("modeDialog");ui(()->permission.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        assertFalse(AssistantConsent.granted(prefs,endpoint));
        ui(()->activity.findViewById(R.id.aiRetry).performClick());assertTrue(selection().isShowing());assertEquals(4,requests.get());
        AlertDialog revoked=selection();ui(()->revoked.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        Files.write(new File(context.getFilesDir(),"phone-consent-result.json").toPath(),new JSONObject()
            .put("physicalPhone",true).put("firstDeclineNoUpload",true).put("firstAcceptSendsOnce",true)
            .put("nextSendNoRepeatedReview",true).put("reopenRetainsPermission",true).put("extraContextOptInDefaultFalse",true)
            .put("differentServiceRequiresPermission",true).put("revocationBlocksUntilNewConsent",true)
            .put("chosenAttachmentDirectSendAfterConsent",true).put("syntheticRequests",4).put("liveProviderInference",false).put("passed",true).toString(2).getBytes(StandardCharsets.UTF_8));
      }finally{
        if(activity!=null)ui(activity::finish);
        SharedPreferences.Editor edit=prefs.edit();for(String key:keys){if(original.containsKey(key))edit.putString(key,original.get(key));else edit.remove(key);}
        if(chat!=null){edit.remove("assistant_image_draft:"+chat);store.delete(chat);}edit.commit();
      }
    }
  }
}
