package com.zikra.app.prayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.widget.TextView;
import com.zikra.app.HomeActivity;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.R;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.reminders.PrayerAlarmPlan;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Runs exclusively in the owned emulator with public city data and synthetic upgrade fixtures. */
@SuppressWarnings("deprecation")
public final class PrayerTimetableTest extends InstrumentationTestCase {
  private Context context;
  private PrayerTimetables store;
  private Activity page;

  @Override protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    assertTrue(new File(context.getFilesDir(), "prayer-source-authorized").isFile());
    context.getSharedPreferences("zikra_preferences",0).edit()
        .putBoolean("onboarding_complete",true).putBoolean("reminder_enabled",false)
        .putBoolean("shake_enabled",false).commit();
    store = PrayerTimetables.get(context);
    LocationStore.save(context, LocationPoint.INITIAL);
  }

  @Override protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    super.tearDown();
  }

  public void testLiveRefreshStoresCurrentAndNextMonthWithoutAdjustment() throws Exception {
    PrayerRegion region=store.region(LocationPoint.INITIAL);
    assertNotNull(region);
    assertEquals("KOTA SURABAYA",region.name);
    LocalDate today = LocalDate.now(LocationPoint.INITIAL.zone);
    try(PrayerApi api=new PrayerApi()) {
      PrayerMonth live=PrayerMonth.parse(api.month(region,YearMonth.from(today)),region,YearMonth.from(today));
      assertTrue(live.supports(LocationPoint.INITIAL));
    }
    CountDownLatch done = new CountDownLatch(1);
    store.refresh(true,done::countDown);
    // setUp may already have initiated the same refresh, so wait for actual data as well.
    done.await(55,TimeUnit.SECONDS);
    long deadline = SystemClock.uptimeMillis()+55000;
    while ((store.find(today,LocationPoint.INITIAL)==null
        ||store.find(YearMonth.from(today).plusMonths(1).atEndOfMonth(),LocationPoint.INITIAL)==null)
        &&SystemClock.uptimeMillis()<deadline) SystemClock.sleep(200);
    assertEquals(PrayerMonth.SOURCE,PrayerSchedule.forDate(today).source);
    assertEquals(PrayerMonth.SOURCE,PrayerSchedule.forDate(YearMonth.from(today).plusMonths(1).atEndOfMonth()).source);
    try(PrayerApi api=new PrayerApi()) {
      PrayerMonth live=PrayerMonth.parse(api.month(region,YearMonth.from(today)),region,YearMonth.from(today));
      for(int i=0;i<6;i++) assertEquals(live.day(today).at(i),PrayerSchedule.forDate(today).at(i));
    }
  }

  public void testOfflineRestartHomeCountdownAlarmAndMonthBoundaryAgree() throws Exception {
    android.net.ConnectivityManager network=context.getSystemService(android.net.ConnectivityManager.class);
    long disconnected=SystemClock.uptimeMillis()+15000;
    while(network.getActiveNetwork()!=null&&SystemClock.uptimeMillis()<disconnected) SystemClock.sleep(100);
    assertNull("Offline test requires no active network",network.getActiveNetwork());
    LocalDate day=LocalDate.of(2026,10,28);
    assertEquals(PrayerMonth.SOURCE,PrayerSchedule.forDate(day).source);
    assertEquals("17:26",PrayerSchedule.forDate(day).time(4));
    ZonedDateTime now=day.atTime(17,25).atZone(LocationPoint.INITIAL.zone);
    assertEquals(now.plusMinutes(1),PrayerSchedule.nextAfter(now).time);
    assertEquals(now.plusMinutes(1).toInstant().toEpochMilli(),
        PrayerAlarmPlan.next(now.toInstant().toEpochMilli(),LocationPoint.INITIAL).get(0).epoch);
    ZonedDateTime end=LocalDate.of(2026,10,31).atTime(23,59).atZone(LocationPoint.INITIAL.zone);
    assertEquals(PrayerSchedule.forDate(LocalDate.of(2026,11,1)).at(0),PrayerSchedule.nextAfter(end).time.toLocalTime());
    assertEquals(PrayerMonth.SOURCE,PrayerSchedule.forDate(LocalDate.of(2026,11,1)).source);
    page=getInstrumentation().startActivitySync(new Intent(context,HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    Field clock=HomeActivity.class.getDeclaredField("clock");clock.setAccessible(true);
    Method tick=HomeActivity.class.getDeclaredMethod("updateClock");tick.setAccessible(true);
    getInstrumentation().runOnMainSync(()->{
      try {clock.set(page,Clock.fixed(now.toInstant(),ZoneOffset.UTC));tick.invoke(page);}
      catch(Exception e){throw new RuntimeException(e);}
    });
    getInstrumentation().waitForIdleSync();
    assertTrue(((TextView)page.findViewById(R.id.homeNextTime)).getText().toString().contains("17.26"));
    assertEquals("00:01:00",((TextView)page.findViewById(R.id.homeClock)).getText().toString());
    assertTrue(((TextView)page.findViewById(R.id.homeScheduleSource)).getText().toString().contains("myQuran"));
    capture("home-offline.png");
    final android.app.AlertDialog[] dialog=new android.app.AlertDialog[1];
    getInstrumentation().runOnMainSync(()->dialog[0]=PrayerSourceDialog.show(page));
    getInstrumentation().waitForIdleSync();
    for(int id:new int[]{R.id.prayerSourceRefresh,R.id.prayerSourceRegions,R.id.prayerSourceClose}) {
      android.view.View button=dialog[0].findViewById(id);
      getInstrumentation().runOnMainSync(()->button.requestRectangleOnScreen(
          new android.graphics.Rect(0,0,button.getWidth(),button.getHeight()),true));
      getInstrumentation().waitForIdleSync();
      android.graphics.Rect visible=new android.graphics.Rect();
      assertTrue(button.getGlobalVisibleRect(visible));
      assertEquals("Entire action must be reachable",button.getHeight(),visible.height());
    }
    capture("source-offline.png");
    android.view.View close=dialog[0].findViewById(R.id.prayerSourceClose);
    int[] xy=new int[2];close.getLocationOnScreen(xy);
    long time=SystemClock.uptimeMillis();
    for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}) {
      android.view.MotionEvent touch=android.view.MotionEvent.obtain(time,time+20,action,
          xy[0]+close.getWidth()/2f,xy[1]+close.getHeight()/2f,0);
      assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(touch,true));touch.recycle();
    }
    getInstrumentation().waitForIdleSync();assertFalse(dialog[0].isShowing());
  }

  public void testUnknownOrInvalidCacheKeepsExplicitFallback() throws Exception {
    LocationPoint unknown=new LocationPoint(-7.28,112.79,"Asia/Jakarta","Lokasi perangkat","device",0,100);
    assertNull(store.region(unknown));
    assertEquals("local",PrayerSchedule.forDate(LocalDate.of(2026,10,28),unknown).source);
    assertTrue(store.caption(LocalDate.of(2026,10,28),unknown).contains("cadangan"));
    PrayerRegion region=store.region(LocationPoint.INITIAL);
    File invalid=new File(context.getFilesDir(),"prayer-timetables/"+region.id+"-2027-01.json");
    try(FileOutputStream out=new FileOutputStream(invalid)){out.write("{broken".getBytes("UTF-8"));}
    assertEquals("local",PrayerSchedule.forDate(LocalDate.of(2027,1,1)).source);
    assertEquals("17:26",PrayerSchedule.forDate(LocalDate.of(2026,10,28)).time(4));
    assertTrue(invalid.delete());
    assertNotNull(context.getSystemService(android.app.job.JobScheduler.class).getPendingJob(7411));
  }

  private void capture(String name) throws Exception {
    SystemClock.sleep(300);
    Bitmap screen=getInstrumentation().getUiAutomation().takeScreenshot();
    File folder=new File(context.getExternalFilesDir(null),"prayer-source");assertTrue(folder.isDirectory()||folder.mkdirs());
    try(FileOutputStream out=new FileOutputStream(new File(folder,name))){assertTrue(screen.compress(Bitmap.CompressFormat.PNG,100,out));}
    screen.recycle();
  }
}
