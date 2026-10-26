package com.zikra.app;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.location.*;
import com.zikra.app.reading.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.concurrent.Callable;

/** Emulator fixtures do not establish physical GPS, compass calibration or heading accuracy. */
@SuppressWarnings("deprecation")
public final class D01VerificationTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  @Override protected void setUp()throws Exception {
    super.setUp();context=getInstrumentation().getTargetContext();
    try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))){assertEquals("1",new String(in.readAllBytes()).trim());}
    assertTrue("Host must isolate and restore emulator data",new File(context.getFilesDir(),"d01-authorized").isFile());
    context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true).putBoolean("shake_enabled",false).putBoolean("reminder_enabled",false).commit();
  }
  @Override protected void tearDown()throws Exception {
    try{if(page!=null)ui(page::finish);}finally{super.tearDown();}
  }
  private void ui(Runnable action){
    Throwable[] failure={null};getInstrumentation().runOnMainSync(()->{try{action.run();}catch(Throwable e){failure[0]=e;}});
    getInstrumentation().waitForIdleSync();if(failure[0] instanceof Error)throw(Error)failure[0];if(failure[0]!=null)throw new RuntimeException(failure[0]);
  }
  private void launch(Class<? extends Activity> type){if(page!=null)ui(page::finish);page=getInstrumentation().startActivitySync(new Intent(context,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));getInstrumentation().waitForIdleSync();}
  private Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
  private void set(Object owner,String name,Object value)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
  private void invoke(String method,Class<?>[] types,Object... arguments){try{Method m=page.getClass().getDeclaredMethod(method,types);m.setAccessible(true);m.invoke(page,arguments);}catch(Exception e){throw new RuntimeException(e);}}
  private void render(){invoke("renderReading",new Class<?>[]{long.class},SystemClock.elapsedRealtime());}
  private String text(int id){return ((TextView)page.findViewById(id)).getText().toString();}
  private void waitFor(Callable<Boolean> condition)throws Exception{long until=SystemClock.elapsedRealtime()+8000;while(SystemClock.elapsedRealtime()<until){if(condition.call())return;SystemClock.sleep(100);}fail("Expected native UI state did not arrive");}
  private void recreate()throws Exception{Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(page.getClass().getName(),null,false);try{ui(page::recreate);page=getInstrumentation().waitForMonitorWithTimeout(monitor,15000);assertNotNull(page);getInstrumentation().waitForIdleSync();}finally{getInstrumentation().removeMonitor(monitor);}}
  private void shot(String name){UiEvidence.capture(getInstrumentation(),"D01-"+name+"-"+(context.getResources().getConfiguration().fontScale>1.9?"200":"100"));}
  private CompassReading isolateSensor()throws Exception{
    ui(()->((SensorManager)page.getSystemService(Context.SENSOR_SERVICE)).unregisterListener((QiblaActivity)page));
    CompassReading state=(CompassReading)field(page,"reading");ui(state::reset);return state;
  }

  public void testDeniedLocationStillAllowsOfflineCityChangesAcrossScreens()throws Exception{
    assertEquals(PackageManager.PERMISSION_DENIED,context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION));
    assertEquals(PackageManager.PERMISSION_DENIED,context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION));
    assertFalse(new com.zikra.app.assistant.AssistantNetwork(context,()->{}).validated());
    for(String city:new String[]{"Makassar","Jayapura","Jakarta"}){
      launch(LocationActivity.class);LinearLayout list=page.findViewById(R.id.locationResults);View chosen=null;
      for(int i=0;i<list.getChildCount();i++)if(((TextView)list.getChildAt(i)).getText().toString().startsWith(city+","))chosen=list.getChildAt(i);
      assertNotNull(city,chosen);View row=chosen;ui(row::performClick);AlertDialog confirm=(AlertDialog)field(page,"confirmation");assertNotNull(confirm);
      ui(()->confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick());LocationPoint point=LocationStore.current(context);assertEquals("manual",point.source);assertTrue(point.label.startsWith(city));
      recreate();assertTrue(text(R.id.locationCurrent).contains(city));
      launch(PrayerActivity.class);assertTrue(text(R.id.featureSubtitle).contains(city));assertTrue(text(R.id.prayerMethod).contains("Bukan jadwal resmi Kemenag"));
      LinearLayout rows=page.findViewById(R.id.prayerRows);PrayerSchedule.Day day=PrayerSchedule.forDate(LocationStore.today(context),point);
      int[] indices={0,2,3,4,5};for(int i=0;i<5;i++)assertEquals(day.time(indices[i]),((TextView)rows.getChildAt(i).findViewById(R.id.prayerRowTime)).getText().toString());
      if(city.equals("Jayapura"))shot("prayer-wit");
      launch(QiblaActivity.class);assertEquals(Math.round(point.qiblaBearing())+"°",text(R.id.qiblaBearing));assertTrue(text(R.id.featureSubtitle).contains(city));
      launch(HomeActivity.class);assertTrue(text(R.id.homeLocationLabel).contains(city));
    }
  }

  public void testCalendarAndWeekSelectionSurviveRecreation()throws Exception{
    launch(PrayerActivity.class);LinearLayout dates=page.findViewById(R.id.prayerWeekDates);ui(()->dates.getChildAt(2).performClick());
    LocalDate chosen=(LocalDate)field(page,"selectedDate");ui(()->page.findViewById(R.id.prayerWeekTab).performClick());recreate();
    assertEquals(chosen,field(page,"selectedDate"));assertEquals(true,field(page,"weekMode"));shot("week-restored");
  }

  public void testAccuracyDropAndTiltImmediatelyClearAlignment()throws Exception{
    launch(QiblaActivity.class);CompassReading state=isolateSensor();float target=(Float)field(page,"qiblaBearing");
    ui(()->{long now=SystemClock.elapsedRealtime();state.accept(target,target,true,true,now,now);render();});
    assertTrue(text(R.id.qiblaSensorStatus).startsWith("Sejajar"));assertEquals(true,field(page,"aligned"));
    Sensor sensor=(Sensor)field(page,"rotationSensor");assertNotNull("AVD rotation sensor fixture",sensor);
    ui(()->((QiblaActivity)page).onAccuracyChanged(sensor,SensorManager.SENSOR_STATUS_ACCURACY_LOW));
    assertEquals(false,field(page,"aligned"));assertTrue(text(R.id.qiblaSensorStatus).contains("Akurasi sensor rendah"));shot("accuracy-low");
    ui(()->{long now=SystemClock.elapsedRealtime();state.accept(target,target,true,false,now,now);render();});
    assertEquals(false,field(page,"aligned"));assertTrue(text(R.id.qiblaSensorStatus).contains("menghadap atas"));
    ui(()->{state.invalidate();render();});assertTrue(text(R.id.qiblaSensorStatus).contains("belum valid"));assertEquals(false,field(page,"aligned"));
  }

  public void testStoppedSensorExpiresAndResumeRequiresFreshReading()throws Exception{
    launch(QiblaActivity.class);CompassReading state=isolateSensor();float target=(Float)field(page,"qiblaBearing");
    ui(()->{long now=SystemClock.elapsedRealtime();state.accept(target,target,true,true,now,now);render();});assertEquals(true,field(page,"aligned"));
    waitFor(()->text(R.id.qiblaSensorStatus).contains("terhenti"));assertEquals(false,field(page,"aligned"));
    assertEquals(0f,(Float)field(page.findViewById(R.id.qiblaCompass),"heading"),0f);shot("stale-static");
    assertTrue(page.findViewById(R.id.qiblaCompass).getContentDescription().toString().contains("bukan pembacaan"));
    recreate();isolateSensor();ui(this::render);assertFalse(text(R.id.qiblaSensorStatus).startsWith("Sejajar"));
  }

  public void testMissingSensorAndCalibrationRemainReachableAtLargeText()throws Exception{
    launch(QiblaActivity.class);isolateSensor();set(page,"rotationSensor",null);set(page,"listening",false);
    ui(()->invoke("showMissingSensor",new Class<?>[]{}));assertTrue(text(R.id.qiblaSensorStatus).contains("tidak tersedia"));assertEquals(false,field(page,"aligned"));
    ScrollView scroll=page.findViewById(R.id.qiblaScroll);View action=page.findViewById(R.id.qiblaCalibrate);
    ui(()->action.requestRectangleOnScreen(new Rect(0,0,action.getWidth(),action.getHeight()),true));
    Rect visible=new Rect();assertTrue(action.getGlobalVisibleRect(visible));assertEquals(action.getHeight(),visible.height());
    ui(action::performClick);AlertDialog dialog=(AlertDialog)field(page,"calibrationDialog");assertNotNull(dialog);assertTrue(dialog.isShowing());
    assertTrue(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getGlobalVisibleRect(new Rect()));shot("calibration");
    ui(()->dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());shot("missing-static");recreate();
  }
}
