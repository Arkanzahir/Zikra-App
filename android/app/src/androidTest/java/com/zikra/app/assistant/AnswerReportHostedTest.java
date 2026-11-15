package com.zikra.app.assistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import com.zikra.app.AiActivity;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import org.json.JSONObject;

/** Host provisions one synthetic, short-lived installation; never run on a real phone. */
@SuppressWarnings("deprecation")
public final class AnswerReportHostedTest extends InstrumentationTestCase {
  private Context context;
  private AiActivity page;
  private ConversationStore store;
  private String ownedChat;
  private Map<String,?> preferences, gateway;
  private JSONObject fixture;
  @Override protected void setUp() throws Exception {
    super.setUp();
    assertTrue(Build.MODEL.toLowerCase(java.util.Locale.ROOT).contains("sdk"));
    context = getInstrumentation().getTargetContext();
    File file = new File(context.getFilesDir(),"answer-report-hosted.json");
    assertTrue(file.isFile() && file.length() < 4096);
    fixture = new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
    assertEquals("https://zikra-ai.vercel.app/v1/assistant",fixture.getString("endpoint"));
    assertTrue(fixture.getString("token").matches("[a-f0-9]{64}"));
    assertTrue(fixture.getLong("expiresAt") > System.currentTimeMillis());
    assertTrue(fixture.getString("excerpt").startsWith("Synthetic Android report UI verification."));
    preferences = context.getSharedPreferences("zikra_preferences",0).getAll();
    gateway = context.getSharedPreferences("assistant_gateway",0).getAll();
    store = new ConversationStore(context); ownedChat = store.create().id;
    context.getSharedPreferences("zikra_preferences",0).edit()
        .putString("assistant_active_chat",ownedChat).putString("assistant_mode_v2","online")
        .putBoolean("onboarding_complete",true).commit();
    new AssistantConnection(context).save(fixture.getString("endpoint"),fixture.getString("token"));
    android.accessibilityservice.AccessibilityServiceInfo info=getInstrumentation().getUiAutomation().getServiceInfo();
    info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
    getInstrumentation().getUiAutomation().setServiceInfo(info);
    page = (AiActivity)getInstrumentation().startActivitySync(new Intent(context,AiActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }
  @Override protected void tearDown() throws Exception {
    try {
      if (page != null) getInstrumentation().runOnMainSync(page::finish);
      getInstrumentation().waitForIdleSync();
      if (store != null) { if(ownedChat != null) store.delete(ownedChat); store.close(); }
      if(preferences != null) restore("zikra_preferences",preferences);
      if(gateway != null) restore("assistant_gateway",gateway);
    } finally { super.tearDown(); }
  }
  private void restore(String name,Map<String,?> values) {
    SharedPreferences.Editor editor=context.getSharedPreferences(name,0).edit().clear();
    for(Map.Entry<String,?> entry:values.entrySet()) {
      String key=entry.getKey();Object value=entry.getValue();
      if(value instanceof String)editor.putString(key,(String)value);
      else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
      else if(value instanceof Integer)editor.putInt(key,(Integer)value);
      else if(value instanceof Long)editor.putLong(key,(Long)value);
      else if(value instanceof Float)editor.putFloat(key,(Float)value);
      else if(value instanceof java.util.Set)editor.putStringSet(key,(java.util.Set<String>)value);
      else throw new AssertionError("Unsupported preference type");
    }
    assertTrue(editor.commit());
  }
  private void clickText(String text) throws Exception {
    long deadline=SystemClock.elapsedRealtime()+10000;
    while(SystemClock.elapsedRealtime()<deadline) {
      getInstrumentation().waitForIdleSync();
      AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
      if(root!=null) for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text)) {
        for(int depth=0;node!=null&&depth<4;depth++,node=node.getParent())
          if(node.isClickable()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK))return;
      }
      SystemClock.sleep(100);
    }
    com.zikra.app.UiEvidence.capture(getInstrumentation(),"report-dialog-blocker");
    fail("Expected report dialog control not reachable: "+text);
  }
  public void testReviewedSyntheticExcerptReachesProduction() throws Exception {
    Method bubble=AiActivity.class.getDeclaredMethod("bubble",String.class,String.class,String.class);
    bubble.setAccessible(true);
    getInstrumentation().runOnMainSync(()->{
      try { bubble.invoke(page,"assistant",fixture.getString("excerpt"),"online"); }
      catch(Exception error){throw new AssertionError("Cannot render synthetic reply");}
    });
    ArrayList<View> actions=new ArrayList<>();
    page.getWindow().getDecorView().findViewsWithText(actions,"Laporkan jawaban AI ini",
        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
    assertEquals(1,actions.size());
    getInstrumentation().runOnMainSync(()->actions.get(0).performClick());
    clickText("Alasan lain"); clickText("Kirim laporan");
    getInstrumentation().waitForIdleSync();
    Field call=AiActivity.class.getDeclaredField("reportCall");call.setAccessible(true);
    long end=SystemClock.elapsedRealtime()+30000;
    while(SystemClock.elapsedRealtime()<end) {
      if(call.get(page)==null)return;
      SystemClock.sleep(100);
    }
    fail("Report request did not complete within budget");
  }
}
