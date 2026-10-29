package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.zikra.app.AiActivity;
import com.zikra.app.R;
import com.zikra.app.data.WeeklyClass;
import com.zikra.app.data.WeeklyClassStore;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.location.LocationStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;

/** Synthetic data and provider interceptor, gated to an owned emulator with a host backup. */
@SuppressWarnings("deprecation")
public final class AssistantClassesTest extends InstrumentationTestCase {
  private Context context;
  private AiActivity activity;
  private long classId, taskId;
  private String actionId;
  private final AtomicInteger calls=new AtomicInteger();
  private final AtomicReference<JSONObject> sent=new AtomicReference<>();

  @Override protected void setUp() throws Exception {
    super.setUp();context=getInstrumentation().getTargetContext();
    try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))){
      assertEquals("Emulator only","1",new String(input.readAllBytes(),StandardCharsets.UTF_8).trim());
    }
    assertTrue("Host backup and isolation required",new File(context.getFilesDir(),"assistant-c04-authorized").isFile());
    context.deleteDatabase("assistant_chats.db");context.deleteDatabase("study-schedule.db");
    context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true)
        .putBoolean("reminder_enabled",false).putBoolean("shake_enabled",false).putString("assistant_history_v1","[]")
        .putString("assistant_mode_v2","online").commit();
    context.getSharedPreferences(LocationStore.PREFS,0).edit().clear().putLong("latitude",Double.doubleToLongBits(-7.28))
        .putLong("longitude",Double.doubleToLongBits(112.79)).putString("zone","Asia/Makassar")
        .putString("label","[DATA UJI] zona pilihan").putString("source","manual").commit();
    try(WeeklyClassStore store=new WeeklyClassStore(context)){
      classId=store.save(new WeeklyClass(0,0,"[DATA UJI C04] Kalkulus","PRIVATE-ROOM","PRIVATE-TEACHER",
          LocalDate.now(java.time.ZoneId.of("Asia/Makassar")).plusDays(1).getDayOfWeek().getValue(),480,540,0,3));
    }
    new AssistantConnection(context).save("https://example.test/v1/assistant","private-test-code-32-characters-long");
    activity=(AiActivity)getInstrumentation().startActivitySync(new Intent(context,AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    main(()->{});
    ((OnlineAssistantClient)field(activity,"onlineClient")).close();
    OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{
      calls.incrementAndGet();okio.Buffer buffer=new okio.Buffer();chain.request().body().writeTo(buffer);
      try{
        JSONObject request=new JSONObject(buffer.readUtf8());sent.set(request);
        JSONObject response=new JSONObject().put("provider","featherless").put("reply","[DATA UJI] Usulan belum disimpan.");
        if(request.getJSONObject("features").getBoolean("taskProposals"))response.put("proposal",new JSONObject()
            .put("id",request.getString("requestId")).put("kind","add_task").put("title","[DATA UJI C04] Persiapan kalkulus").put("dueAt",0));
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic")
            .body(ResponseBody.create(MediaType.parse("application/json"),response.toString())).build();
      }catch(org.json.JSONException invalid){throw new java.io.IOException("Synthetic request invalid");}
    }).build();
    set(activity,"onlineClient",new OnlineAssistantClient(http));
  }

  public void testDefaultsCancelUncheckAndExactPreviewSnapshot() throws Exception {
    AlertDialog firstDialog=compose();ArrayList<CheckBox> choices=choices(firstDialog);
    assertEquals(6,choices.size());for(CheckBox choice:choices)assertFalse(choice.isChecked());
    assertFalse(text(firstDialog.getWindow().getDecorView()).contains("[DATA UJI C04] Kalkulus"));
    screenshot("consent-default");
    main(()->choices.get(5).setChecked(true));
    assertTrue(text(firstDialog.getWindow().getDecorView()).contains("[DATA UJI C04] Kalkulus"));
    assertTrue(text(firstDialog.getWindow().getDecorView()).contains("Asia/Makassar"));
    assertFalse(text(firstDialog.getWindow().getDecorView()).contains("PRIVATE-TEACHER"));
    main(()->choices.get(5).setChecked(false));
    assertFalse(text(firstDialog.getWindow().getDecorView()).contains("[DATA UJI C04] Kalkulus"));
    main(()->firstDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());assertEquals(0,calls.get());
    main(()->activity.findViewById(R.id.aiRetry).performClick());
    AlertDialog dialog=(AlertDialog)field(activity,"selectionDialog");ArrayList<CheckBox> retried=choices(dialog);
    for(CheckBox choice:retried)assertFalse("Per-send consent resets",choice.isChecked());
    CheckBox classes=retried.get(5);main(()->classes.setChecked(true));
    String preview=text(dialog.getWindow().getDecorView());assertTrue(preview.contains("[DATA UJI C04] Kalkulus"));
    screenshot("consent-classes");
    try(WeeklyClassStore store=new WeeklyClassStore(context)){
      WeeklyClass before=store.find(classId);store.save(new WeeklyClass(before.id,before.revision,"CHANGED-AFTER-PREVIEW",before.room,before.teacher,before.weekday,before.startMinute,before.endMinute,0,3));
    }
    AlertDialog finalDialog=dialog;main(()->finalDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());untilReply();
    JSONObject request=sent.get(), selected=request.getJSONObject("context");
    assertEquals(1,selected.length());assertTrue(selected.has("classes"));
    assertTrue(request.toString().contains("[DATA UJI C04] Kalkulus"));
    for(String hidden:new String[]{"CHANGED-AFTER-PREVIEW","PRIVATE-ROOM","PRIVATE-TEACHER","latitude","longitude","credits","revision"})assertFalse(request.toString().contains(hidden));
    assertEquals(1,calls.get());
  }

  public void testClassSnapshotProposalRequiresReviewAndSavesOnce() throws Exception {
    int before;try(ZikraDbHelper db=new ZikraDbHelper(context)){before=db.getTasks().size();}
    AlertDialog dialog=compose();ArrayList<CheckBox> choices=choices(dialog);
    main(()->{choices.get(4).setChecked(true);choices.get(5).setChecked(true);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});untilReply();
    JSONArray messages=(JSONArray)field(activity,"messages");JSONObject proposal=messages.getJSONObject(messages.length()-1).getJSONObject("proposal");actionId=proposal.getString("id");
    try(ZikraDbHelper db=new ZikraDbHelper(context)){assertEquals(before,db.getTasks().size());assertFalse(db.assistantProposalApplied(actionId));}
    View review=findText(activity.getWindow().getDecorView(),context.getString(R.string.assistant_review_task));assertNotNull(review);main(review::performClick);
    AlertDialog taskDialog=(AlertDialog)field(activity,"taskDialog");screenshot("proposal-review");
    main(()->{taskDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();taskDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
    try(ZikraDbHelper db=new ZikraDbHelper(context)){
      assertEquals(before+1,db.getTasks().size());assertTrue(db.assistantProposalApplied(actionId));
      for(com.zikra.app.data.Task task:db.getTasks())if(task.title.equals("[DATA UJI C04] Persiapan kalkulus"))taskId=task.id;
      assertTrue(taskId>0);for(com.zikra.app.data.Task task:db.getTasks())if(task.id==taskId)assertFalse(task.completed);
    }
    try(WeeklyClassStore store=new WeeklyClassStore(context)){assertEquals(1,store.all().size());assertEquals(1,store.find(classId).revision);}
    assertEquals(1,calls.get());
  }

  public void testConsentAndTaskActionsReachableByActualTouch() throws Exception {
    AlertDialog dialog=compose();ArrayList<CheckBox> choices=choices(dialog);
    revealAndTap(choices.get(4));assertTrue(choices.get(4).isChecked());
    revealAndTap(choices.get(5));assertTrue(choices.get(5).isChecked());
    com.zikra.app.ui.AssistantReviewScrollView body=findBody(dialog.getWindow().getDecorView());assertNotNull(body);
    main(()->body.fullScroll(View.FOCUS_DOWN));SystemClock.sleep(250);
    assertTrue(body.getScrollY()>0);assertFullyVisible(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
    assertFullyVisible(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));screenshot("consent-actions-visible");
    tap(dialog.getButton(AlertDialog.BUTTON_POSITIVE));untilReply();
    assertTrue(sent.get().getJSONObject("context").has("classes"));
    JSONArray messages=(JSONArray)field(activity,"messages");JSONObject proposal=messages.getJSONObject(messages.length()-1).getJSONObject("proposal");actionId=proposal.getString("id");
    View review=findText(activity.getWindow().getDecorView(),context.getString(R.string.assistant_review_task));assertNotNull(review);revealAndTap(review);
    AlertDialog task=(AlertDialog)field(activity,"taskDialog");assertFullyVisible(task.getButton(AlertDialog.BUTTON_POSITIVE));
    assertFullyVisible(task.getButton(AlertDialog.BUTTON_NEGATIVE));screenshot("task-actions-visible");tap(task.getButton(AlertDialog.BUTTON_POSITIVE));
    try(ZikraDbHelper db=new ZikraDbHelper(context)){
      assertTrue(db.assistantProposalApplied(actionId));
      try(android.database.Cursor row=db.getReadableDatabase().rawQuery("SELECT task_id FROM assistant_actions WHERE action_id=?",new String[]{actionId})){assertTrue(row.moveToFirst());taskId=row.getLong(0);}
    }
  }

  private void revealAndTap(View view){main(()->view.requestRectangleOnScreen(new android.graphics.Rect(0,0,view.getWidth(),view.getHeight()),true));SystemClock.sleep(250);assertFullyVisible(view);tap(view);}
  private void assertFullyVisible(View view){android.graphics.Rect bounds=new android.graphics.Rect();assertTrue(view.getGlobalVisibleRect(bounds));assertTrue("Action height not clipped",bounds.height()>=view.getHeight()-2);assertTrue("Action width not clipped",bounds.width()>=view.getWidth()-2);if(view instanceof android.widget.Button){android.widget.Button button=(android.widget.Button)view;assertNotNull(button.getLayout());for(int i=0;i<button.getLineCount();i++)assertEquals("Action text not ellipsized",0,button.getLayout().getEllipsisCount(i));}}
  private void tap(View view){int[] point=new int[2];view.getLocationOnScreen(point);float x=point[0]+view.getWidth()/2f,y=point[1]+view.getHeight()/2f;long time=SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,0,x,y,0),up=android.view.MotionEvent.obtain(time,time+50,1,x,y,0);try{assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(down,true));assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(up,true));}finally{down.recycle();up.recycle();}main(()->{});}
  private static com.zikra.app.ui.AssistantReviewScrollView findBody(View view){if(view instanceof com.zikra.app.ui.AssistantReviewScrollView)return (com.zikra.app.ui.AssistantReviewScrollView)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){com.zikra.app.ui.AssistantReviewScrollView body=findBody(((ViewGroup)view).getChildAt(i));if(body!=null)return body;}return null;}

  private AlertDialog compose()throws Exception {
    main(()->{((EditText)activity.findViewById(R.id.aiInput)).setText("[DATA UJI] Buat usulan persiapan kelas yang saya sertakan, tanpa tenggat.");activity.findViewById(R.id.aiSend).performClick();});
    AlertDialog dialog=(AlertDialog)field(activity,"selectionDialog");assertNotNull("Validated network required for consent",dialog);assertTrue(dialog.isShowing());return dialog;
  }
  private void untilReply()throws Exception{
    long end=SystemClock.elapsedRealtime()+5000;
    while(SystemClock.elapsedRealtime()<end){main(()->{});if(!(Boolean)field(activity,"waiting")&&sent.get()!=null)return;SystemClock.sleep(50);}fail("Native reply missing");
  }
  private void main(Runnable action){getInstrumentation().runOnMainSync(action);getInstrumentation().waitForIdleSync();}
  private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
  private static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
  private ArrayList<CheckBox> choices(AlertDialog dialog){ArrayList<CheckBox> result=new ArrayList<>();checks(dialog.getWindow().getDecorView(),result);return result;}
  private static void checks(View v,ArrayList<CheckBox> result){if(v instanceof CheckBox)result.add((CheckBox)v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)checks(((ViewGroup)v).getChildAt(i),result);}
  private static String text(View v){StringBuilder s=new StringBuilder();if(v instanceof TextView)s.append(((TextView)v).getText());if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)s.append('\n').append(text(((ViewGroup)v).getChildAt(i)));return s.toString();}
  private static View findText(View v,String value){if(v instanceof TextView&&value.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=findText(((ViewGroup)v).getChildAt(i),value);if(found!=null)return found;}return null;}
  private void screenshot(String name)throws Exception{SystemClock.sleep(200);File folder=new File(context.getExternalFilesDir(null),"assistant-c04");assertTrue(folder.isDirectory()||folder.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){assertTrue(getInstrumentation().getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}}
  @Override protected void tearDown()throws Exception{
    if(activity!=null)main(activity::finish);
    if(context!=null&&taskId>0)try(ZikraDbHelper db=new ZikraDbHelper(context)){db.deleteTask(taskId);if(actionId!=null)db.getWritableDatabase().delete("assistant_actions","action_id=?",new String[]{actionId});}
    super.tearDown();
  }
}
