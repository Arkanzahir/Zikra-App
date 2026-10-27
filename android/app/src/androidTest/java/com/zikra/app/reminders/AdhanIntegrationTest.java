package com.zikra.app.reminders;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.*;
import com.zikra.app.*;
import com.zikra.app.audio.*;
import com.zikra.app.data.*;
import com.zikra.app.location.*;
import com.zikra.app.study.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;

/** Isolated emulator data only. Real decoder/focus; synthetic alarm times and Quran WAV fixture. */
@SuppressWarnings("deprecation")
public final class AdhanIntegrationTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private QuranAudioService quran;
  private boolean bound;
  private final ServiceConnection connection=new ServiceConnection(){
    public void onServiceConnected(ComponentName name,IBinder binder){quran=((QuranAudioService.LocalBinder)binder).service();}
    public void onServiceDisconnected(ComponentName name){quran=null;}
  };
  @Override protected void setUp()throws Exception{
    super.setUp();context=getInstrumentation().getTargetContext();
    assertEquals("1",shell("getprop ro.kernel.qemu").trim());
    assertTrue(new File(context.getFilesDir(),"d02-authorized").isFile());
    ui(()->AdhanService.stop(context));waitFor(()->!AdhanService.state().active);
    PrayerSettings.prefs(context).edit().clear().putBoolean("onboarding_complete",true).putBoolean("shake_enabled",false).commit();
    context.getSharedPreferences(FocusTimer.PREFS,0).edit().clear().commit();
    try(WeeklyClassStore store=new WeeklyClassStore(context)){for(WeeklyClass item:store.all())store.delete(item.id,item.revision);}
    LocationStore.save(context,LocationPoint.INITIAL);PrayerReminders.sync(context);
  }
  @Override protected void tearDown()throws Exception{
    try{
      ui(()->{AdhanService.stop(context);if(quran!=null)QuranAudioService.send(context,QuranAudioService.STOP);if(page!=null)page.finish();if(bound){context.unbindService(connection);bound=false;}});
      waitFor(()->!AdhanService.state().active);context.stopService(new Intent(context,QuranAudioService.class));
      PrayerSettings.prefs(context).edit().putBoolean("reminder_enabled",false).commit();PrayerReminders.sync(context);
      for(int id=7300;id<=7305;id++)context.getSystemService(NotificationManager.class).cancel(id);
    }finally{super.tearDown();}
  }
  private String shell(String command)throws Exception{try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))){return new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
  private void ui(Runnable action){Throwable[] failure={null};getInstrumentation().runOnMainSync(()->{try{action.run();}catch(Throwable e){failure[0]=e;}});getInstrumentation().waitForIdleSync();if(failure[0] instanceof Error)throw(Error)failure[0];if(failure[0]!=null)throw new RuntimeException(failure[0]);}
  private void waitFor(Callable<Boolean> condition)throws Exception{long end=SystemClock.elapsedRealtime()+15000;while(SystemClock.elapsedRealtime()<end){if(condition.call())return;SystemClock.sleep(100);}fail("Expected state not reached; audio="+AdhanService.state().status);}
  private void launch(Class<? extends Activity> type){if(page!=null)ui(page::finish);page=getInstrumentation().startActivitySync(new Intent(context,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));getInstrumentation().waitForIdleSync();}
  private Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
  private void recreate()throws Exception{Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(page.getClass().getName(),null,false);try{ui(page::recreate);page=getInstrumentation().waitForMonitorWithTimeout(monitor,15000);assertNotNull(page);getInstrumentation().waitForIdleSync();}finally{getInstrumentation().removeMonitor(monitor);}}
  private void shot(String name){UiEvidence.capture(getInstrumentation(),"D02-"+name+"-"+(context.getResources().getConfiguration().fontScale>1.9?"200":"100"));}
  private void enable(int index,PrayerMode mode){PrayerSettings.prefs(context).edit().putBoolean("reminder_enabled",true).commit();PrayerSettings.setMode(context,index,mode);}
  private Intent planned(int index,long epoch){
    Intent alarm=PrayerReminders.alarmIntent(context,index,epoch);String key=alarm.getStringExtra("slot");SharedPreferences p=context.getSharedPreferences(PrayerReminders.PLAN,0);
    Set<String> ids=new HashSet<>(p.getStringSet("ids",Collections.emptySet()));ids.add(key);
    p.edit().putLong("e:"+key,epoch).putString("l:"+key,LocationStore.current(context).key()).putLong("r:"+key,PrayerSettings.revision(context)).putStringSet("ids",ids).commit();return alarm;
  }
  private StatusBarNotification notice(int id){for(StatusBarNotification n:context.getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==id)return n;return null;}
  private Set<String> ids(){return context.getSharedPreferences(PrayerReminders.PLAN,0).getStringSet("ids",Collections.emptySet());}
  private void playAlarm(int index)throws Exception{enable(index,PrayerMode.ADHAN);Intent intent=planned(index,System.currentTimeMillis()-1000);context.sendBroadcast(intent);waitFor(()->AdhanService.state().playing);}
  private String digest(int index)throws Exception{try(InputStream in=context.getAssets().open(index==0?"adhan/fajr.mp3":"adhan/selected.mp3")){return hex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));}}
  private String hex(byte[] data){StringBuilder text=new StringBuilder();for(byte value:data)text.append(String.format(java.util.Locale.ROOT,"%02x",value&255));return text.toString();}

  public void testLegacyModesAndSevenDayPlanSurviveRescheduling(){
    assertEquals(PrayerMode.NOTIFICATION,PrayerSettings.mode(context,0));PrayerSettings.prefs(context).edit().putBoolean("reminder_subuh",false).commit();assertEquals(PrayerMode.OFF,PrayerSettings.mode(context,0));
    enable(0,PrayerMode.VIBRATE);PrayerReminders.sync(context);assertTrue(ids().size()>=30);assertTrue(ids().size()<=35);
    Set<String> before=new HashSet<>(ids());new PrayerRescheduleReceiver().onReceive(context,new Intent(Intent.ACTION_BOOT_COMPLETED));assertEquals(before,ids());
    PrayerSettings.setMode(context,0,PrayerMode.OFF);PrayerReminders.sync(context);for(String key:ids())assertFalse(key.endsWith("_0"));
    PrayerSettings.prefs(context).edit().putBoolean("reminder_enabled",false).commit();PrayerReminders.sync(context);assertTrue(ids().isEmpty());
  }
  public void testModeEditorRetainsDraftAndCancelDoesNotSave()throws Exception{
    launch(PrayerActivity.class);LinearLayout rows=page.findViewById(R.id.prayerRows);ui(()->rows.getChildAt(1).findViewById(R.id.prayerRowBell).performClick());
    AlertDialog dialog=(AlertDialog)field(page,"modeDialog");ui(()->dialog.getListView().performItemClick(dialog.getListView().getChildAt(2),2,2));recreate();
    AlertDialog restored=(AlertDialog)field(page,"modeDialog");assertEquals(2,restored.getListView().getCheckedItemPosition());
    for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE}){
      View button=restored.getButton(which);android.graphics.Rect box=new android.graphics.Rect();assertTrue(button.getGlobalVisibleRect(box));assertEquals(button.getHeight(),box.height());assertEquals(button.getWidth(),box.width());
    }
    shot("mode-draft");
    ui(()->restored.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());assertEquals(PrayerMode.NOTIFICATION,PrayerSettings.mode(context,2));
    LinearLayout fresh=page.findViewById(R.id.prayerRows);ui(()->fresh.getChildAt(1).findViewById(R.id.prayerRowBell).performClick());AlertDialog save=(AlertDialog)field(page,"modeDialog");
    ui(()->{save.getListView().performItemClick(save.getListView().getChildAt(2),2,2);save.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});assertEquals(PrayerMode.VIBRATE,PrayerSettings.mode(context,2));
    View preview=page.findViewById(R.id.prayerAudioPreview);ViewParent ancestor=preview.getParent();
    while(!(ancestor instanceof ScrollView))ancestor=ancestor.getParent();ScrollView scroll=(ScrollView)ancestor;
    ui(()->scroll.scrollTo(0,Math.max(0,preview.getTop()-12)));assertTrue(preview.getGlobalVisibleRect(new android.graphics.Rect()));shot("settings");
  }
  public void testNotificationAndVibrateChannelsNeverStartAudio()throws Exception{
    // Explicit app broadcasts can be deferred for cached UIDs; real background alarms have a separate test.
    launch(PrayerActivity.class);
    for(PrayerMode mode:new PrayerMode[]{PrayerMode.NOTIFICATION,PrayerMode.VIBRATE}){
      enable(2,mode);context.getSystemService(NotificationManager.class).cancel(7302);Intent event=planned(2,System.currentTimeMillis()-1000);
      assertTrue("Invalid fixture before broadcast: "+mode,PrayerReminders.valid(context,event));
      context.sendBroadcast(event);waitFor(()->notice(7302)!=null);
      // The visible notice precedes deliver()'s finally/sync; finish that broadcast before the next fixture.
      getInstrumentation().waitForIdleSync();
      assertEquals(mode==PrayerMode.VIBRATE?PrayerReminders.VIBRATE:PrayerReminders.CHANNEL,notice(7302).getNotification().getChannelId());assertFalse(AdhanService.state().active);
      if(mode==PrayerMode.VIBRATE){NotificationChannel channel=context.getSystemService(NotificationManager.class).getNotificationChannel(PrayerReminders.VIBRATE);assertNull(channel.getSound());assertTrue(channel.shouldVibrate());}
    }
  }
  public void testOffFutureStaleCityAndDuplicatesAreRejected()throws Exception{
    enable(2,PrayerMode.OFF);PrayerReminders.deliver(context,planned(2,System.currentTimeMillis()-1000));assertNull(notice(7302));
    enable(2,PrayerMode.NOTIFICATION);PrayerReminders.deliver(context,planned(2,System.currentTimeMillis()+60000));assertNull(notice(7302));
    Intent old=planned(2,System.currentTimeMillis()-1000);LocationStore.save(context,new LocationPoint(-5.1477,119.4327,"Asia/Makassar","[UJI] Makassar","manual",0,-1));PrayerReminders.deliver(context,old);assertNull(notice(7302));
    long epoch=System.currentTimeMillis()-1000;PrayerReminders.deliver(context,planned(2,epoch));waitFor(()->notice(7302)!=null);long first=notice(7302).getPostTime();PrayerReminders.deliver(context,planned(2,epoch));assertEquals(first,notice(7302).getPostTime());
    PrayerSettings.prefs(context).edit().putBoolean("reminder_enabled",false).commit();PrayerReminders.deliver(context,planned(3,System.currentTimeMillis()-1000));assertNull(notice(7303));assertFalse(AdhanService.state().active);
  }
  public void testTwoSelectedMp3TracksDecodeAndStopActionsAreBoundToSession()throws Exception{
    launch(PrayerActivity.class);assertEquals("43ade766272ff9c89c86908959df6ddd7f3a34961897caf09f965a3841e1bcd2",digest(2));assertEquals("106a7b2ca4e8f85fb44f7b150d42e7ca11237c961642ad877dd02e45f8add530",digest(0));
    ui(()->assertTrue(AdhanService.preview(context,2)));waitFor(()->AdhanService.state().playing);assertTrue(AdhanService.state().duration>195000&&AdhanService.state().duration<210000);
    int initial=AdhanService.state().position;waitFor(()->AdhanService.state().position>initial+500);PendingIntent oldStop=notice(7400).getNotification().actions[0].actionIntent;shot("playing-makkah");oldStop.send();waitFor(()->!AdhanService.state().active);
    ui(()->assertTrue(AdhanService.preview(context,0)));waitFor(()->AdhanService.state().playing);assertTrue(AdhanService.state().duration>180000&&AdhanService.state().duration<195000);
    oldStop.send();SystemClock.sleep(350);assertTrue(AdhanService.state().playing);PendingIntent stop=notice(7400).getNotification().actions[0].actionIntent;stop.send();waitFor(()->!AdhanService.state().active);assertNull(notice(7400));
  }
  public void testMasterModeAndLocationChangesStopAutomaticPlayback()throws Exception{
    launch(PrayerActivity.class);playAlarm(2);PrayerSettings.prefs(context).edit().putBoolean("reminder_enabled",false).commit();waitFor(()->!AdhanService.state().active);
    playAlarm(2);PrayerSettings.setMode(context,2,PrayerMode.OFF);waitFor(()->!AdhanService.state().active);
    playAlarm(2);LocationStore.save(context,new LocationPoint(-5,119,"Asia/Makassar","[UJI] Kota","manual",0,-1));waitFor(()->!AdhanService.state().active);
  }
  public void testAdhanPausesVisibleFocusTimerWithoutCompletingTask()throws Exception{
    long task;try(ZikraDbHelper db=new ZikraDbHelper(context)){task=db.addTask("[UJI] Fokus saat adzan",ZikraDbHelper.CATEGORY_STUDY);}
    FocusClock timer=new FocusClock();timer.reset(25,5,task);timer.start(SystemClock.elapsedRealtime(),FocusTimer.boot(context));FocusTimer.schedule(context,timer);
    launch(FocusTimerActivity.class);PrayerSettings.prefs(context).edit().putBoolean(PrayerSettings.PAUSE_FOCUS,true).commit();playAlarm(2);
    waitFor(()->!FocusTimer.load(context).running);SystemClock.sleep(700);assertFalse(((FocusClock)field(page,"clock")).running);
    long remaining=FocusTimer.load(context).remaining;AdhanService.stop(context);waitFor(()->!AdhanService.state().active);SystemClock.sleep(300);assertEquals(remaining,FocusTimer.load(context).remaining);
    try(ZikraDbHelper db=new ZikraDbHelper(context)){for(Task item:db.getTasks())if(item.id==task)assertFalse(item.completed);db.deleteTask(task);}shot("focus-paused");
  }
  public void testQuietClassSuppressesAdhanAndKeepsSilentNotice()throws Exception{
    ZonedDateTime now=ZonedDateTime.now(LocationStore.current(context).zone);int minute=now.getHour()*60+now.getMinute();assertTrue("Avoid final minute of local day in fixture",minute<1439);
    try(WeeklyClassStore store=new WeeklyClassStore(context)){store.save(new WeeklyClass(0,0,"[UJI] Kelas aktif","","",now.getDayOfWeek().getValue(),Math.max(0,minute-1),Math.min(1439,minute+2),0));}
    enable(2,PrayerMode.ADHAN);PrayerSettings.prefs(context).edit().putBoolean(PrayerSettings.QUIET_CLASSES,true).commit();PrayerReminders.deliver(context,planned(2,System.currentTimeMillis()-1000));waitFor(()->notice(7302)!=null);
    assertEquals(PrayerReminders.QUIET,notice(7302).getNotification().getChannelId());assertFalse(AdhanService.state().active);
  }
  public void testRealAlarmStartsOfflinePlayerWithScreenOff()throws Exception{
    assertTrue(PrayerReminders.exactAllowed(context));assertFalse(new com.zikra.app.assistant.AssistantNetwork(context,()->{}).validated());
    launch(PrayerActivity.class);enable(2,PrayerMode.ADHAN);Intent event=planned(2,System.currentTimeMillis()+4000);
    PendingIntent operation=PendingIntent.getBroadcast(context,0,event,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    context.getSystemService(AlarmManager.class).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,event.getLongExtra(PrayerReminders.EXTRA_EPOCH,0),operation);
    ui(()->page.moveTaskToBack(true));shell("input keyevent 223");
    try{waitFor(()->AdhanService.state().playing);assertFalse(context.getSystemService(PowerManager.class).isInteractive());int before=AdhanService.state().position;waitFor(()->AdhanService.state().position>before+500);}
    finally{shell("input keyevent 224");shell("wm dismiss-keyguard");}
  }
  public void testQuranFocusPausesAndResumesAroundAdhan()throws Exception{
    launch(PrayerActivity.class);File file=AudioFiles.file(context,Recitation.IDS[0],1,1);file.getParentFile().mkdirs();int samples=22050*30,size=samples*2;
    ByteBuffer wav=ByteBuffer.allocate(44+size).order(ByteOrder.LITTLE_ENDIAN);wav.put("RIFF".getBytes()).putInt(36+size).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(size);
    for(int i=0;i<samples;i++)wav.putShort((short)(500*Math.sin(2*Math.PI*440*i/22050)));try(FileOutputStream out=new FileOutputStream(file)){out.write(wav.array());}
    ui(()->{bound=context.bindService(new Intent(context,QuranAudioService.class),connection,Context.BIND_AUTO_CREATE);QuranAudioService.send(context,QuranAudioService.play(1,1,1,1,Recitation.IDS[0],1,true));});waitFor(()->quran!=null&&quranState().playing);
    ui(()->AdhanService.preview(context,2));waitFor(()->AdhanService.state().playing);waitFor(()->!quranState().playing);AdhanService.stop(context);waitFor(()->quranState().playing);
    AudioManager manager=context.getSystemService(AudioManager.class);AudioFocusRequest other=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener(change->{},new Handler(Looper.getMainLooper())).build();
    ui(()->AdhanService.preview(context,2));waitFor(()->AdhanService.state().playing);ui(()->manager.requestAudioFocus(other));waitFor(()->!AdhanService.state().active);ui(()->manager.abandonAudioFocusRequest(other));SystemClock.sleep(300);assertFalse(AdhanService.state().active);
  }
  private QuranAudioService.State quranState(){QuranAudioService.State[] result={null};ui(()->result[0]=quran.state());return result[0];}
  public void testDeniedNotificationPreservesChoicesAndDoesNotStartAudio()throws Exception{
    assertFalse(PrayerReminders.canNotify(context));enable(2,PrayerMode.ADHAN);PrayerReminders.sync(context);assertTrue(ids().isEmpty());
    PrayerReminders.deliver(context,planned(2,System.currentTimeMillis()-1000));SystemClock.sleep(500);assertFalse(AdhanService.state().active);assertEquals(PrayerMode.ADHAN,PrayerSettings.mode(context,2));assertNull(notice(7302));
  }
  public void testDndSuppressesAdhanWithoutChangingDevicePolicy()throws Exception{
    assertTrue(NotificationManager.INTERRUPTION_FILTER_ALL!=context.getSystemService(NotificationManager.class).getCurrentInterruptionFilter());
    enable(2,PrayerMode.ADHAN);PrayerReminders.deliver(context,planned(2,System.currentTimeMillis()-1000));waitFor(()->notice(7302)!=null);assertEquals(PrayerReminders.QUIET,notice(7302).getNotification().getChannelId());assertFalse(AdhanService.state().active);
  }

  private AccessibilityNodeInfo systemAction(String text){
    for(AccessibilityWindowInfo window:getInstrumentation().getUiAutomation().getWindows()){
      AccessibilityNodeInfo root=window.getRoot();
      if(root==null||!"com.android.systemui".contentEquals(root.getPackageName()))continue;
      for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))
        if(node.getText()!=null&&text.equalsIgnoreCase(node.getText().toString())&&node.isVisibleToUser())return node;
    }
    return null;
  }
  private void clickSystemAction(String text)throws Exception{
    waitFor(()->systemAction(text)!=null);AccessibilityNodeInfo node=systemAction(text);
    while(node!=null&&!node.isClickable())node=node.getParent();
    assertNotNull("System action must be clickable",node);assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));
  }
  private int inspectSystemWindows(){
    android.app.UiAutomation automation=getInstrumentation().getUiAutomation();AccessibilityServiceInfo info=automation.getServiceInfo();
    int before=info.flags;info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
    automation.setServiceInfo(info);return before;
  }
  private void restoreSystemWindows(int flags)throws Exception{
    shell("cmd statusbar collapse");AccessibilityServiceInfo info=getInstrumentation().getUiAutomation().getServiceInfo();
    info.flags=flags;getInstrumentation().getUiAutomation().setServiceInfo(info);
  }
  private void systemShot(String name)throws Exception{
    File dir=new File(context.getExternalFilesDir(null),"validation");assertTrue(dir.isDirectory()||dir.mkdirs());
    String suffix=context.getResources().getConfiguration().fontScale>1.9?"200":"100";
    try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand("screencap -p"));
        OutputStream out=new FileOutputStream(new File(dir,"D02-"+name+"-"+suffix+".png"))){in.transferTo(out);}
  }
  public void testBannerStopIsVisibleOverLauncherAndStopsWithoutOpeningApp()throws Exception{
    launch(PrayerActivity.class);int flags=inspectSystemWindows();
    try{
      NotificationManager manager=context.getSystemService(NotificationManager.class);
      assertEquals(NotificationManager.IMPORTANCE_LOW,manager.getNotificationChannel(PrayerReminders.PLAYBACK).getImportance());
      NotificationChannel channel=manager.getNotificationChannel(AdhanNotification.CHANNEL);
      assertEquals(NotificationManager.IMPORTANCE_HIGH,channel.getImportance());assertNull(channel.getSound());assertFalse(channel.shouldVibrate());
      enable(4,PrayerMode.ADHAN);Intent event=planned(4,System.currentTimeMillis()+3000);
      PendingIntent operation=PendingIntent.getBroadcast(context,0,event,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
      context.getSystemService(AlarmManager.class).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,event.getLongExtra(PrayerReminders.EXTRA_EPOCH,0),operation);
      shell("input keyevent 3");waitFor(()->AdhanService.state().playing);
      SystemClock.sleep(300);systemShot("banner-before-check");
      waitFor(()->systemAction(context.getString(R.string.adhan_stop))!=null);systemShot("banner-heads-up");
      Notification notification=notice(7400).getNotification();assertEquals(AdhanNotification.CHANNEL,notification.getChannelId());
      assertEquals("Waktu Maghrib",notification.extras.getString(Notification.EXTRA_TITLE));
      assertNull(notification.fullScreenIntent);assertEquals(Notification.VISIBILITY_PUBLIC,notification.visibility);
      clickSystemAction(context.getString(R.string.adhan_stop));waitFor(()->!AdhanService.state().active);assertNull(notice(7400));
      assertFalse("Stop must not bring PrayerActivity to foreground",shell("dumpsys activity activities").lines()
          .anyMatch(line->line.contains("topResumedActivity")&&line.contains("com.zikra.app")));
    }finally{restoreSystemWindows(flags);}
  }
  public void testShadeKeepsStopControlForQuietPreview()throws Exception{
    launch(PrayerActivity.class);int flags=inspectSystemWindows();
    try{
      ui(()->AdhanService.preview(context,0));waitFor(()->AdhanService.state().playing);
      Notification notification=notice(7400).getNotification();assertEquals(PrayerReminders.PLAYBACK,notification.getChannelId());
      assertTrue((notification.flags&Notification.FLAG_ONLY_ALERT_ONCE)!=0);assertNull(notification.deleteIntent);
      shell("input keyevent 3");SystemClock.sleep(6500);assertNotNull(notice(7400));assertTrue(AdhanService.state().playing);
      assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS));
      SystemClock.sleep(500);systemShot("shade-before-check");
      waitFor(()->systemAction(context.getString(R.string.adhan_stop))!=null);systemShot("notification-shade");
      clickSystemAction(context.getString(R.string.adhan_stop));waitFor(()->!AdhanService.state().active);assertNull(notice(7400));
    }finally{restoreSystemWindows(flags);}
  }
}
