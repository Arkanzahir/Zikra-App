package com.zikra.app.reminders;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.VolumeShaper;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.HomeActivity;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.R;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.prayer.PrayerRegion;
import com.zikra.app.prayer.PrayerTimetables;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/** Physical checks require a private host backup marker. No database fixtures or uploads. */
@SuppressWarnings("deprecation")
public final class PhonePrayerModesTest extends InstrumentationTestCase {
  private Context context;
  @Override protected void setUp() throws Exception {
    super.setUp();context=getInstrumentation().getTargetContext();
    assertFalse(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"));
    assertTrue(new File(context.getFilesDir(),"phone-ad-backup-verified").isFile());
    java.lang.reflect.Field provider=PrayerSchedule.class.getDeclaredField("timetable");
    provider.setAccessible(true);
    boolean readyBefore=provider.get(null)!=null;
    main(()->{});
    save("phone-ad-startup-result.json",new JSONObject().put("providerReadyBeforeMainBarrier",readyBefore)
        .put("providerReadyAfterMainBarrier",provider.get(null)!=null));
    assertNotNull("Application startup must finish before checks",provider.get(null));
  }
  private void main(Runnable action){getInstrumentation().runOnMainSync(action);getInstrumentation().waitForIdleSync();}
  private void openHome(){main(()->context.startActivity(new Intent(context,HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));}
  private void save(String name,JSONObject result)throws Exception{
    Files.write(new File(context.getFilesDir(),name).toPath(),result.toString(2).getBytes(StandardCharsets.UTF_8));
  }
  private VolumeShaper entrance()throws Exception{
    java.lang.reflect.Field active=AdhanService.class.getDeclaredField("active");active.setAccessible(true);
    java.lang.reflect.Field shape=AdhanService.class.getDeclaredField("entrance");shape.setAccessible(true);
    return (VolumeShaper)shape.get(active.get(null));
  }
  private void restore(SharedPreferences prefs,Map<String,?> original){
    SharedPreferences.Editor e=prefs.edit().clear();
    for(Map.Entry<String,?> row:original.entrySet()){
      String key=row.getKey();Object value=row.getValue();
      if(value instanceof String)e.putString(key,(String)value);
      else if(value instanceof Boolean)e.putBoolean(key,(Boolean)value);
      else if(value instanceof Long)e.putLong(key,(Long)value);
      else if(value instanceof Integer)e.putInt(key,(Integer)value);
      else if(value instanceof Float)e.putFloat(key,(Float)value);
      else if(value instanceof Set)e.putStringSet(key,new HashSet<>((Set<String>)value));
      else throw new IllegalStateException("Unsupported preference type");
    }
    assertTrue(e.commit());
  }
  public void testConfirmedSurabayaRegionAndSource()throws Exception{
    assertTrue(new File(context.getFilesDir(),"phone-ad-surabaya-confirmed").isFile());
    LocationPoint old=LocationStore.current(context);
    assertEquals("Asia/Jakarta",old.zone.getId());
    LocationPoint named=new LocationPoint(old.latitude,old.longitude,old.zone.getId(),
        "Sukolilo, Surabaya",old.source,old.selectedAt,old.accuracy);
    LocationStore.save(context,named);
    PrayerTimetables tables=PrayerTimetables.get(context);
    PrayerRegion city=null;for(PrayerRegion r:tables.regions())if(r.name.equals("KOTA SURABAYA"))city=r;
    assertNotNull(city);tables.select(named,city);
    CountDownLatch done=new CountDownLatch(1);tables.refresh(true,done::countDown);
    assertTrue("Public timetable request completes",done.await(95,TimeUnit.SECONDS));
    LocalDate today=LocalDate.now(named.zone);
    assertEquals("kemenag_myquran",PrayerSchedule.forDate(today,named).source);
    assertEquals("kemenag_myquran",PrayerSchedule.forDate(YearMonth.from(today).plusMonths(1).atEndOfMonth(),named).source);
    assertEquals("Jadwal · Kemenag",tables.caption(today,named));
    assertEquals(old.key(),LocationStore.current(context).key());
    JSONObject result=new JSONObject().put("physicalPhone",true).put("regionConfirmedByUser",true)
        .put("region","KOTA SURABAYA").put("displayNameConfirmedByUser",true)
        .put("coordinatesAndZonePreserved",true).put("currentAndNextMonthAvailable",true)
        .put("sourceLabel","Jadwal · Kemenag").put("passed",true);
    save("phone-ad-location-result.json",result);
  }
  public void testTwentyModesMasterOffAndNotificationChannels()throws Exception{
    assertTrue(PrayerReminders.canNotify(context));
    SharedPreferences prefs=PrayerSettings.prefs(context);
    Map<String,?> original=new HashMap<>(prefs.getAll());
    int count=0;JSONArray results=new JSONArray();
    try{
      for(int index:new int[]{0,2,3,4,5})for(PrayerMode mode:PrayerMode.values()){
        prefs.edit().putBoolean("reminder_enabled",true).commit();PrayerSettings.setMode(context,index,mode);
        PrayerReminders.sync(context);assertEquals(mode,PrayerSettings.mode(context,index));
        SharedPreferences plan=context.getSharedPreferences(PrayerReminders.PLAN,0);
        Set<String> ids=plan.getStringSet("ids",java.util.Collections.emptySet());
        boolean found=false;for(String key:ids)if(key.endsWith("_"+index)){
          found=true;assertFalse(mode==PrayerMode.OFF);
          long epoch=plan.getLong("e:"+key,0);
          LocationPoint point=LocationStore.current(context);LocalDate date=Instant.ofEpochMilli(epoch).atZone(point.zone).toLocalDate();
          PrayerSchedule.Day selected=PrayerSchedule.forDate(date,point);
          assertEquals("index="+index+" date="+date+" source="+selected.source
              +" planned="+plan.getString("t:"+key,"")+" current="+selected.signature()
              +" application="+context.getApplicationContext().getClass().getName(),
              date.atTime(selected.at(index)).atZone(point.zone).toInstant().toEpochMilli(),epoch);
        }
        assertEquals(mode!=PrayerMode.OFF,found);count++;
        results.put(new JSONObject().put("prayerIndex",index).put("mode",mode.name()).put("passed",true));
      }
      assertEquals(20,count);
      NotificationManager manager=context.getSystemService(NotificationManager.class);
      PrayerSettings.setMode(context,2,PrayerMode.NOTIFICATION);
      assertTrue(PrayerReminders.post(context,2,System.currentTimeMillis(),PrayerMode.NOTIFICATION,false,"Uji notifikasi Zikra"));
      assertFalse(AdhanService.state().active);manager.cancel(PrayerReminders.NOTIFICATION_BASE+2);
      PrayerSettings.setMode(context,2,PrayerMode.VIBRATE);
      assertTrue(PrayerReminders.post(context,2,System.currentTimeMillis(),PrayerMode.VIBRATE,false,"Uji getar Zikra"));
      assertNull(manager.getNotificationChannel(PrayerReminders.VIBRATE).getSound());
      assertFalse(AdhanService.state().active);manager.cancel(PrayerReminders.NOTIFICATION_BASE+2);
      prefs.edit().putBoolean("reminder_enabled",false).commit();PrayerReminders.sync(context);
      assertTrue(context.getSharedPreferences(PrayerReminders.PLAN,0).getStringSet("ids",java.util.Collections.emptySet()).isEmpty());
      assertFalse(PrayerReminders.post(context,2,System.currentTimeMillis(),PrayerMode.VIBRATE,false,"Uji mati"));
      save("phone-ad-modes-result.json",new JSONObject().put("physicalPhone",true).put("modeCases",results)
          .put("masterOffHasNoAlarmsOrNotice",true).put("notificationDoesNotStartAdhan",true)
          .put("vibrateChannelHasNoSound",true).put("passed",true));
    }finally{restore(prefs,original);PrayerReminders.sync(context);}
    assertEquals(original,prefs.getAll());
  }
  public void testBothRecordingsFinishNaturally()throws Exception{
    assertFalse(AdhanService.state().active);
    assertTrue(PrayerAudioPolicy.deviceAllowsSound(context));
    SharedPreferences prefs=PrayerSettings.prefs(context);Map<String,?> original=new HashMap<>(prefs.getAll());
    JSONArray records=new JSONArray();Activity page=null;
    try{
      prefs.edit().putBoolean(PrayerSettings.QUIET_CLASSES,false).putBoolean(PrayerSettings.PAUSE_FOCUS,false).commit();
      openHome();
      for(int index:new int[]{2,0}){
        main(()->assertTrue(AdhanService.preview(context,index)));
        long until=SystemClock.elapsedRealtime()+12000;
        while(!AdhanService.state().playing&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(100);
        assertTrue(AdhanService.state().playing);int duration=AdhanService.state().duration;
        if(index==0)assertNull("Subuh keeps its original start",entrance());
        save("phone-ad-playback-current.json",new JSONObject().put("prayerIndex",index).put("durationMs",duration).put("started",true));
        assertTrue(duration>1000&&duration<=300000);long began=SystemClock.elapsedRealtime();
        while(AdhanService.state().active&&SystemClock.elapsedRealtime()-began<duration+12000)SystemClock.sleep(200);
        assertFalse(AdhanService.state().active);
        assertEquals(context.getString(R.string.adhan_finished),AdhanService.state().status);
        assertTrue(SystemClock.elapsedRealtime()-began>=duration-3000);
        records.put(new JSONObject().put("prayerIndex",index).put("durationMs",duration)
            .put("naturalCompletion",true).put("passed",true));
        save("phone-ad-playback-progress.json",new JSONObject().put("records",records));
      }
      save("phone-ad-playback-result.json",new JSONObject().put("physicalPhone",true).put("records",records).put("passed",true));
    }finally{
      Activity opened=page;main(()->{AdhanService.stop(context);restore(prefs,original);if(opened!=null)opened.finish();});
    }
  }
  public void testNotificationDuckingContinuesAndStrongerFocusStops()throws Exception{
    assertFalse(AdhanService.state().active);
    assertTrue(PrayerAudioPolicy.deviceAllowsSound(context));
    SharedPreferences prefs=PrayerSettings.prefs(context);Map<String,?> original=new HashMap<>(prefs.getAll());
    AudioManager manager=context.getSystemService(AudioManager.class);
    Handler handler=new Handler(Looper.getMainLooper());
    AudioFocusRequest notification=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        .setOnAudioFocusChangeListener(change->{},handler).build();
    AudioFocusRequest exclusive=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build())
        .setOnAudioFocusChangeListener(change->{},handler).build();
    Activity page=null;
    try{
      prefs.edit().putBoolean(PrayerSettings.QUIET_CLASSES,false).commit();
      openHome();
      main(()->assertTrue(AdhanService.preview(context,2)));
      long end=SystemClock.elapsedRealtime()+12000;
      while(!AdhanService.state().playing&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
      assertTrue(AdhanService.state().playing);
      VolumeShaper shape=entrance();assertNotNull("Regular audio has a soft start",shape);
      float early=shape.getVolume();assertTrue("Initial gain stays in range",early>=0f&&early<=1f);
      JSONArray envelope=new JSONArray();long shapeStarted=SystemClock.elapsedRealtime();
      do{
        envelope.put(new JSONObject().put("elapsedMs",SystemClock.elapsedRealtime()-shapeStarted).put("gain",shape.getVolume()));
        if(shape.getVolume()>=0.98f)break;
        SystemClock.sleep(100);
      }while(SystemClock.elapsedRealtime()-shapeStarted<4000);
      assertEquals("Full gain after soft start",1f,shape.getVolume(),0.02f);
      int before=AdhanService.state().position;
      main(()->assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,manager.requestAudioFocus(notification)));
      SystemClock.sleep(2500);
      assertTrue("Brief notification must not stop adzan",AdhanService.state().playing);
      assertTrue(AdhanService.state().position>before+1000);
      main(()->manager.abandonAudioFocusRequest(notification));SystemClock.sleep(1500);
      assertTrue(AdhanService.state().playing);
      main(()->assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,manager.requestAudioFocus(exclusive)));
      end=SystemClock.elapsedRealtime()+5000;
      while(AdhanService.state().active&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
      assertFalse("Exclusive transient focus must stop adzan",AdhanService.state().active);
      assertEquals(context.getString(R.string.adhan_interrupted),AdhanService.state().status);
      main(()->manager.abandonAudioFocusRequest(exclusive));SystemClock.sleep(1000);
      assertFalse("Focus gain must not restart a stopped adzan",AdhanService.state().active);
      save("phone-ad-focus-result.json",new JSONObject().put("physicalPhone",true)
          .put("regularSoftStartAvailable",true).put("earlySoftStartGain",early).put("fullGainAfterSoftStart",true)
          .put("softStartSamples",envelope)
          .put("realAndroidFocusRequests",true).put("notificationDuckingContinuesPlayback",true)
          .put("positionAdvancesDuringDucking",true).put("strongerTransientLossStops",true)
          .put("focusGainDoesNotRestartStoppedAudio",true).put("passed",true));
    }finally{
      Activity opened=page;main(()->{manager.abandonAudioFocusRequest(notification);manager.abandonAudioFocusRequest(exclusive);
        AdhanService.stop(context);restore(prefs,original);if(opened!=null)opened.finish();});
    }
  }
  public void testCompletionAfterSeekPreservesSubuhStart()throws Exception{
    assertFalse(AdhanService.state().active);
    assertTrue(PrayerAudioPolicy.deviceAllowsSound(context));
    SharedPreferences prefs=PrayerSettings.prefs(context);Map<String,?> original=new HashMap<>(prefs.getAll());
    JSONArray records=new JSONArray();Activity page=null;
    try{
      prefs.edit().putBoolean(PrayerSettings.QUIET_CLASSES,false).commit();
      openHome();
      for(int index:new int[]{2,0}){
        main(()->assertTrue(AdhanService.preview(context,index)));
        long end=SystemClock.elapsedRealtime()+12000;
        while(!AdhanService.state().playing&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
        assertTrue(AdhanService.state().playing);
        if(index==0)assertNull("Subuh has no new fade",entrance());else assertNotNull(entrance());
        SystemClock.sleep(1200);
        java.lang.reflect.Field active=AdhanService.class.getDeclaredField("active");active.setAccessible(true);
        java.lang.reflect.Field player=AdhanService.class.getDeclaredField("player");player.setAccessible(true);
        android.media.MediaPlayer current=(android.media.MediaPlayer)player.get(active.get(null));
        int duration=AdhanService.state().duration;assertTrue(duration>10000);
        main(()->current.seekTo(duration-1800));
        end=SystemClock.elapsedRealtime()+10000;
        while(AdhanService.state().active&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
        assertFalse(AdhanService.state().active);
        assertEquals(context.getString(R.string.adhan_finished),AdhanService.state().status);
        records.put(new JSONObject().put("prayerIndex",index).put("durationMs",duration)
            .put("seekedNearEnd",true).put("fullRecordingPlayed",false).put("completionAndCleanup",true));
      }
      save("phone-ad-completion-result.json",new JSONObject().put("physicalPhone",true).put("records",records).put("passed",true));
    }finally{
      Activity opened=page;main(()->{AdhanService.stop(context);restore(prefs,original);if(opened!=null)opened.finish();});
    }
  }
  public void testRegularPreviewWithoutSimulatedInterruptions()throws Exception{
    assertFalse(AdhanService.state().active);assertTrue(PrayerAudioPolicy.deviceAllowsSound(context));
    SharedPreferences prefs=PrayerSettings.prefs(context);Map<String,?> original=new HashMap<>(prefs.getAll());
    try{
      prefs.edit().putBoolean(PrayerSettings.QUIET_CLASSES,false).commit();openHome();
      main(()->assertTrue(AdhanService.preview(context,2)));
      long end=SystemClock.elapsedRealtime()+12000;
      while(!AdhanService.state().playing&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
      assertTrue(AdhanService.state().playing);SystemClock.sleep(30000);
      assertTrue("Ordinary preview stays playing",AdhanService.state().playing);
      save("phone-ad-listening-result.json",new JSONObject().put("physicalPhone",true)
          .put("previewSeconds",30).put("simulatedFocusRequests",false).put("seekUsed",false)
          .put("continuedPlaying",true).put("passed",true));
    }finally{main(()->{AdhanService.stop(context);restore(prefs,original);});}
  }
}
