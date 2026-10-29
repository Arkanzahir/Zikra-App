package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.zikra.app.assistant.ConversationStore;
import com.zikra.app.sky.HomeSkyView;
import com.zikra.app.sky.SkyDay;
import com.zikra.app.ui.AppAppearance;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.time.*;
import java.util.function.BooleanSupplier;
import org.json.JSONArray;
import org.json.JSONObject;

/** Synthetic fixtures on the owned emulator only; never sends a message or opens user files. */
@android.annotation.TargetApi(30)
public final class UiFollowupTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;

  @Override protected void setUp() throws Exception {
    super.setUp();context=getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(),"ui-followup-authorized").isFile());
    context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true)
        .putBoolean("shake_enabled",false).putString("assistant_mode_v2","offline").commit();
  }
  private void main(Runnable action){
    java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
    getInstrumentation().runOnMainSync(()->{try{action.run();}catch(Throwable e){error.set(e);}});
    getInstrumentation().waitForIdleSync();if(error.get()!=null)throw new AssertionError(error.get());
  }
  private void until(String label,BooleanSupplier done){
    long end=SystemClock.uptimeMillis()+8000;
    while(SystemClock.uptimeMillis()<end){getInstrumentation().waitForIdleSync();if(done.getAsBoolean())return;SystemClock.sleep(50);}
    fail(label);
  }
  private static Object field(Object value,String name){
    try{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    catch(Exception e){throw new AssertionError(e);}
  }
  private static void set(Object value,String name,Object data){
    try{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);f.set(value,data);}
    catch(Exception e){throw new AssertionError(e);}
  }
  private void mode(String choice){context.getSharedPreferences(AppAppearance.PREFS,0).edit().putString(AppAppearance.KEY,choice).commit();}
  private Activity launch(Class<? extends Activity> type){
    page=getInstrumentation().startActivitySync(new Intent(context,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();return page;
  }
  private Activity destination(Class<? extends Activity> type,Runnable action){
    ActivityMonitor monitor=getInstrumentation().addMonitor(type.getName(),null,false);
    try{main(action);Activity next=monitor.waitForActivityWithTimeout(8000);assertNotNull(next);page=next;getInstrumentation().waitForIdleSync();return next;}
    finally{getInstrumentation().removeMonitor(monitor);}
  }
  private void image(String name) throws Exception {
    SystemClock.sleep(120);Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
    File dir=new File(context.getExternalFilesDir(null),"ui-followup");assertTrue(dir.isDirectory()||dir.mkdirs());
    try(FileOutputStream stream=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}
    finally{bitmap.recycle();}
  }
  @Override protected void tearDown() throws Exception {if(page!=null&&!page.isDestroyed())main(page::finish);super.tearDown();}

  public void testSeedPreviousVersionFixture() throws Exception {
    assertEquals("1.9.23",context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName);
    try(ConversationStore store=new ConversationStore(context)){
      ConversationStore.Chat chat=store.create();JSONArray messages=new JSONArray()
          .put(new JSONObject().put("role","user").put("text","Data uji sebelum pembaruan"))
          .put(new JSONObject().put("role","assistant").put("text","Balasan uji tersimpan"));
      store.save(chat.id,messages,"Draf UI06 sebelum pembaruan");
      context.getSharedPreferences("zikra_preferences",0).edit().putString("assistant_active_chat",chat.id)
          .putString("profile_display_name","Data Uji UI06").putInt("ui06_fixture_count",messages.length()).commit();
      mode("dark");
    }
  }
  public void testPreviousDataSurvivesUpdate() throws Exception {
    assertEquals("1.9.24",context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName);
    assertEquals("dark",AppAppearance.selected(context));
    assertEquals("Data Uji UI06",context.getSharedPreferences("zikra_preferences",0).getString("profile_display_name",""));
    try(ConversationStore store=new ConversationStore(context)){
      ConversationStore.Chat chat=store.get(context.getSharedPreferences("zikra_preferences",0).getString("assistant_active_chat",""));
      assertNotNull(chat);assertEquals(2,chat.messages.length());assertEquals("Draf UI06 sebelum pembaruan",chat.draft);
      assertEquals("Data uji sebelum pembaruan",chat.messages.getJSONObject(0).getString("text"));
    }
  }

  public void testAiDockDraftNavigationAndRealKeyboard() throws Exception {
    for(String choice:new String[]{"light","dark"}){
      mode(choice);launch(AiActivity.class);Activity ai=page;
      View dock=ai.getWindow().getDecorView().findViewWithTag("zikraDock");assertNotNull(dock);
      assertEquals(View.VISIBLE,dock.getVisibility());assertTrue(ai.findViewById(R.id.dockAi).isSelected());
      for(int id:new int[]{R.id.dockHome,R.id.dockQuran,R.id.dockAi,R.id.dockStudy,R.id.dockProfile}){
        Rect bounds=new Rect();assertTrue(ai.findViewById(id).getGlobalVisibleRect(bounds));
      }
      Rect composer=new Rect(),bar=new Rect();ai.findViewById(R.id.aiComposer).getGlobalVisibleRect(composer);dock.getGlobalVisibleRect(bar);
      assertTrue("Composer above navigation",composer.bottom<=bar.top);
      JSONArray original=new JSONArray(((JSONArray)field(ai,"messages")).toString());
      main(()->((EditText)ai.findViewById(R.id.aiInput)).setText("Draf UI06 belum dikirim"));
      image(choice+"-ai-dock");
      Activity home=destination(HomeActivity.class,()->ai.findViewById(R.id.dockHome).performClick());
      Activity returned=destination(AiActivity.class,()->home.findViewById(R.id.dockAi).performClick());
      assertEquals("Draf UI06 belum dikirim",((EditText)returned.findViewById(R.id.aiInput)).getText().toString());
      assertEquals(original.toString(),((JSONArray)field(returned,"messages")).toString());
      until("AI window has focus before requesting IME",returned::hasWindowFocus);
      main(()->{EditText input=returned.findViewById(R.id.aiInput);input.requestFocus();
        ((InputMethodManager)returned.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);});
      until("Real IME opened",()->returned.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()));
      until("Dock hidden for keyboard",()->returned.getWindow().getDecorView().findViewWithTag("zikraDock").getVisibility()==View.GONE);
      Rect send=new Rect();assertTrue(returned.findViewById(R.id.aiSend).getGlobalVisibleRect(send));
      image(choice+"-keyboard-bounds");
      JSONArray geometry=new JSONArray();View target=returned.findViewById(R.id.aiSend);
      for(View v=target;v!=null;v=v.getParent() instanceof View?(View)v.getParent():null){
        Rect area=new Rect();v.getGlobalVisibleRect(area);
        geometry.put(new JSONObject().put("class",v.getClass().getSimpleName()).put("height",v.getHeight())
            .put("y",v.getY()).put("scaleY",v.getScaleY()).put("visible",area.toString()));
      }
      try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),"ui-followup/control-geometry.json"))){
        out.write(geometry.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      until("Entire send touch target becomes visible",()->{
        Rect bounds=new Rect();return returned.findViewById(R.id.aiSend).getGlobalVisibleRect(bounds)
            && returned.findViewById(R.id.aiSend).getHeight()==bounds.height();
      });
      returned.findViewById(R.id.aiSend).getGlobalVisibleRect(send);
      int ime=returned.getWindow().getDecorView().getRootWindowInsets().getInsets(WindowInsets.Type.ime()).bottom;
      assertTrue("Send remains above keyboard",send.bottom<=returned.getWindow().getDecorView().getHeight()-ime+3);
      image(choice+"-ai-keyboard");
      main(()->((InputMethodManager)returned.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(returned.findViewById(R.id.aiInput).getWindowToken(),0));
      until("Dock restored",()->returned.getWindow().getDecorView().findViewWithTag("zikraDock").getVisibility()==View.VISIBLE);
      assertEquals("Draf UI06 belum dikirim",((EditText)returned.findViewById(R.id.aiInput)).getText().toString());
      main(returned::finish);page=home;main(home::finish);page=null;
    }
  }

  private void freeze(HomeSkyView sky,LocalTime time,SkyDay day){
    main(()->{
      set(page,"clock",Clock.fixed(LocalDate.of(2026,10,29).atTime(time).atZone(ZoneId.of("Asia/Jakarta")).toInstant(),ZoneOffset.UTC));
      ((android.os.Handler)field(page,"timer")).removeCallbacksAndMessages(null);
      try{java.lang.reflect.Method update=HomeActivity.class.getDeclaredMethod("updateClock");update.setAccessible(true);update.invoke(page);}
      catch(Exception e){throw new AssertionError(e);}
      sky.setRunning(false);sky.setSky(day,time);
    });
  }
  public void testLampsNightDayAndThemeIndependent() throws Exception {
    mode("light");launch(HomeActivity.class);final HomeSkyView sky=page.findViewById(R.id.homeSky);
    SkyDay day=new SkyDay(LocalDate.of(2026,10,29),LocalTime.of(6,0),LocalTime.NOON,LocalTime.of(18,0),.7,true,false);
    for(LocalTime time:new LocalTime[]{LocalTime.NOON,LocalTime.of(18,10),LocalTime.of(21,0),LocalTime.of(5,50)}){
      freeze(sky,time,day);
      Bitmap lit=Bitmap.createBitmap(sky.getWidth(),sky.getHeight(),Bitmap.Config.ARGB_8888);
      Bitmap unlit=Bitmap.createBitmap(sky.getWidth(),sky.getHeight(),Bitmap.Config.ARGB_8888);
      main(()->sky.draw(new Canvas(lit)));float strength=(Float)field(sky,"lampStrength");
      assertEquals(time.equals(LocalTime.NOON)?0:time.equals(LocalTime.of(21,0))?1:.5,strength,.001);
      main(()->{set(sky,"lampStrength",0f);sky.draw(new Canvas(unlit));set(sky,"lampStrength",strength);sky.invalidate();});
      int changed=0;for(int y=0;y<lit.getHeight();y+=2)for(int x=0;x<lit.getWidth();x+=2){
        int a=lit.getPixel(x,y),b=unlit.getPixel(x,y);if(Color.red(a)-Color.red(b)>10 && Color.red(a)>Color.blue(a)*1.1f)changed++;
      }
      if(strength==0)assertEquals("No added lights during day",0,changed);else assertTrue("Visible warm facade and reflected light",changed>150);
      lit.recycle();unlit.recycle();image("home-"+time.toString().replace(':','-'));
    }
    main(page::finish);mode("dark");launch(HomeActivity.class);final HomeSkyView darkSky=page.findViewById(R.id.homeSky);freeze(darkSky,LocalTime.NOON,day);
    Bitmap image=Bitmap.createBitmap(darkSky.getWidth(),darkSky.getHeight(),Bitmap.Config.ARGB_8888);
    main(()->darkSky.draw(new Canvas(image)));assertEquals(0,(Float)field(darkSky,"lampStrength"),.001);image.recycle();image("dark-theme-day-unlit");
  }

  private Activity coldLaunch(boolean clearIntro) throws Exception {
    if(clearIntro){Field shown=SplashActivity.class.getDeclaredField("introShown");shown.setAccessible(true);shown.setBoolean(null,false);}
    ActivityMonitor home=getInstrumentation().addMonitor(HomeActivity.class.getName(),null,false);
    try{
      main(()->context.startActivity(new Intent(context,SplashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)));
      Activity result=home.waitForActivityWithTimeout(8000);assertNotNull("Home entered",result);page=result;
      getInstrumentation().waitForIdleSync();SystemClock.sleep(150);assertEquals("Exactly one Home resume",1,home.getHits());return result;
    }finally{getInstrumentation().removeMonitor(home);}
  }
  public void testAnimatedColdAndWarmLaunchNavigateOnce() throws Exception {
    coldLaunch(true);image("launch-complete");coldLaunch(false);
  }
  public void testReducedMotionSkipsIntro() throws Exception {
    ActivityMonitor splash=getInstrumentation().addMonitor(SplashActivity.class.getName(),null,false);
    try{coldLaunch(true);Activity intro=splash.getLastActivity();assertNotNull(intro);assertFalse((Boolean)field(intro,"started"));}
    finally{getInstrumentation().removeMonitor(splash);}
  }
  public void testBackgroundDoesNotNavigateUntilResumed() throws Exception {
    Field shown=SplashActivity.class.getDeclaredField("introShown");shown.setAccessible(true);shown.setBoolean(null,false);
    ActivityMonitor splash=getInstrumentation().addMonitor(SplashActivity.class.getName(),null,false);
    ActivityMonitor home=getInstrumentation().addMonitor(HomeActivity.class.getName(),null,false);
    try{
      main(()->context.startActivity(new Intent(context,SplashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)));
      Activity intro=splash.waitForActivityWithTimeout(3000);assertNotNull(intro);
      getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);SystemClock.sleep(600);
      assertFalse((Boolean)field(intro,"resumed"));assertEquals("No background navigation",0,home.getHits());
      main(()->context.startActivity(new Intent(context,SplashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
      Activity entered=home.waitForActivityWithTimeout(8000);assertNotNull(entered);page=entered;
      assertEquals(1,home.getHits());
    }finally{getInstrumentation().removeMonitor(splash);getInstrumentation().removeMonitor(home);}
  }
}
