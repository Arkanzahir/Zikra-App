package com.zikra.app.reminders;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.test.mock.MockContentResolver;
import com.zikra.app.BuildConfig;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Disposable emulator, private host fixture only. No audio in either APK. */
@SuppressWarnings("deprecation")
public final class LocalAdhanAudioTest extends InstrumentationTestCase {
  private File root, input;
  private Context scope;
  private Uri uri=Uri.parse("content://adhan-fixture/audio");
  private String pref;
  private boolean pipe;
  private long declared=AssetFileDescriptor.UNKNOWN_LENGTH;
  @Override protected void setUp() throws Exception {
    super.setUp();
    assertEquals("ranchu",Build.HARDWARE);assertTrue(BuildConfig.PUBLIC_BETA_SCOPE);
    Context app=getInstrumentation().getTargetContext();
    assertTrue(new File(app.getFilesDir(),"local-adhan-emulator-authorized").isFile());
    input=new File("/data/local/tmp/ZikraLocalAdhanQA.mp3");assertTrue(input.isFile());
    root=new File(app.getCacheDir(),"local-adhan-qa-"+UUID.randomUUID());assertTrue(root.mkdir());
    pref="local-adhan-qa-"+UUID.randomUUID();
    MockContentResolver resolver=new MockContentResolver();
    ContentProvider provider=new ContentProvider(){
      public boolean onCreate(){return true;}
      public String getType(Uri value){return "audio/mpeg";}
      public AssetFileDescriptor openAssetFile(Uri value,String mode) throws FileNotFoundException {
        if(pipe) {
          try{ParcelFileDescriptor[] ends=ParcelFileDescriptor.createPipe();ends[1].close();
            return new AssetFileDescriptor(ends[0],0,AssetFileDescriptor.UNKNOWN_LENGTH);
          }catch(IOException failure){throw new FileNotFoundException("Pipe fixture unavailable");}
        }
        return new AssetFileDescriptor(ParcelFileDescriptor.open(input,ParcelFileDescriptor.MODE_READ_ONLY),0,declared);
      }
      public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
      public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
      public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
      public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    };
    android.content.pm.ProviderInfo info=new android.content.pm.ProviderInfo();info.authority="adhan-fixture";
    provider.attachInfo(app,info);resolver.addProvider("adhan-fixture",provider);
    scope=new ContextWrapper(app){
      public File getFilesDir(){return root;}
      public ContentResolver getContentResolver(){return resolver;}
      public SharedPreferences getSharedPreferences(String name,int mode){return app.getSharedPreferences(pref,mode);}
    };
  }
  private void install(int index)throws Exception{LocalAdhanAudio.importAudio(scope,index,uri,new CancellationSignal());}
  private void noStaging(){
    File[] files=new File(root,"local-adhan").listFiles();
    if(files!=null)for(File file:files)assertFalse(file.getName().endsWith(".tmp"));
  }
  public void testNormalAudioRestoresOnlySavedAudibleModes()throws Exception {
    SharedPreferences settings=PrayerSettings.prefs(scope);
    settings.edit().putString("prayer_mode_dzuhur","ADHAN").putBoolean("reminder_dzuhur",true)
        .putString("prayer_mode_subuh","ADHAN").putBoolean("reminder_subuh",true)
        .putString("prayer_mode_maghrib","OFF").putBoolean("reminder_maghrib",false).commit();
    assertEquals(PrayerMode.NOTIFICATION,PrayerSettings.mode(scope,2));
    install(2);
    assertEquals(PrayerMode.ADHAN,PrayerSettings.mode(scope,2));
    assertEquals(PrayerMode.NOTIFICATION,PrayerSettings.mode(scope,0));
    assertEquals(PrayerMode.OFF,PrayerSettings.mode(scope,4));
    assertFalse(PrayerReminders.enabled(scope));
    assertEquals("ADHAN",settings.getString("prayer_mode_dzuhur",null));
    try(AssetFileDescriptor audio=AdhanAsset.open(scope,2)){
      android.media.MediaPlayer player=new android.media.MediaPlayer();
      try{player.setDataSource(audio.getFileDescriptor(),audio.getStartOffset(),audio.getLength());player.prepare();
        assertTrue(player.getDuration()>=10_000&&player.getDuration()<=300_000);
      }finally{player.release();}
    }
    noStaging();
  }
  public void testFajrIsIndependentAndCopiedNotLinked()throws Exception {
    install(0);
    assertTrue(AdhanAsset.available(scope,0));assertFalse(AdhanAsset.available(scope,2));
    assertTrue(Arrays.equals(Files.readAllBytes(input.toPath()),Files.readAllBytes(LocalAdhanAudio.file(scope,0).toPath())));
    noStaging();
  }
  public void testInvalidReplacementRetainsPreviousRecording()throws Exception {
    install(0);byte[] previous=Files.readAllBytes(LocalAdhanAudio.file(scope,0).toPath());
    File invalid=new File(root,"invalid.txt");Files.write(invalid.toPath(),"not MP3".getBytes());input=invalid;
    try{install(0);fail("Invalid media accepted");}catch(IOException expected){}
    assertTrue(Arrays.equals(previous,Files.readAllBytes(LocalAdhanAudio.file(scope,0).toPath())));noStaging();
  }
  public void testKnownOversizeRejected()throws Exception {
    declared=LocalAdhanAudio.MAX_BYTES+1;
    try{install(2);fail("Oversize accepted");}catch(IOException expected){}
    assertFalse(AdhanAsset.available(scope,2));noStaging();
  }
  public void testUnknownLengthCannotBypassByteBudget()throws Exception {
    input=new File(root,"large.mp3");
    try(RandomAccessFile file=new RandomAccessFile(input,"rw")){file.setLength(LocalAdhanAudio.MAX_BYTES+1);}
    try{install(2);fail("Stream budget bypassed");}catch(IOException expected){}
    assertFalse(AdhanAsset.available(scope,2));noStaging();
  }
  public void testCancelledImportAndNonContentUriAreRejected()throws Exception {
    CancellationSignal cancel=new CancellationSignal();cancel.cancel();
    try{LocalAdhanAudio.importAudio(scope,2,uri,cancel);fail("Cancelled accepted");}catch(OperationCanceledException expected){}
    try{LocalAdhanAudio.importAudio(scope,2,Uri.fromFile(input),new CancellationSignal());fail("File URI accepted");}catch(IOException expected){}
    assertFalse(AdhanAsset.available(scope,2));noStaging();
  }
  public void testNonSeekableProviderRetainsExistingAudio()throws Exception {
    install(2);byte[] previous=Files.readAllBytes(LocalAdhanAudio.file(scope,2).toPath());pipe=true;
    try{install(2);fail("Pipe must be downloaded first");}catch(IOException expected){}
    assertTrue(Arrays.equals(previous,Files.readAllBytes(LocalAdhanAudio.file(scope,2).toPath())));noStaging();
  }
  public void testRemovalFallsBackWithoutChangingSavedMode()throws Exception {
    PrayerSettings.prefs(scope).edit().putString("prayer_mode_subuh","ADHAN").putBoolean("reminder_subuh",true).commit();
    install(0);install(2);LocalAdhanAudio.remove(scope,0);
    assertFalse(AdhanAsset.available(scope,0));assertTrue(AdhanAsset.available(scope,2));
    assertEquals(PrayerMode.NOTIFICATION,PrayerSettings.mode(scope,0));
    assertEquals("ADHAN",PrayerSettings.prefs(scope).getString("prayer_mode_subuh",null));noStaging();
  }
  public void testLocalRecordingPlaysWithNotificationStopAction()throws Exception {
    Context app=getInstrumentation().getTargetContext();
    File actual=LocalAdhanAudio.file(app,2);
    assertFalse("Owned test requires no existing local normal recording",actual.exists());
    File folder=actual.getParentFile();assertTrue(folder.isDirectory()||folder.mkdirs());
    android.app.Activity page=null;
    try {
      Files.copy(input.toPath(),actual.toPath());
      page=getInstrumentation().startActivitySync(new Intent(app,com.zikra.app.PrayerActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
      assertTrue(PrayerReminders.canNotify(app));
      assertTrue(PrayerAudioPolicy.deviceAllowsSound(app));
      getInstrumentation().runOnMainSync(()->assertTrue(AdhanService.preview(app,2)));
      long end=SystemClock.elapsedRealtime()+15_000;
      while(!AdhanService.state().playing&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
      assertTrue(AdhanService.state().status,AdhanService.state().playing);
      android.service.notification.StatusBarNotification notice=null;
      for(android.service.notification.StatusBarNotification item:app.getSystemService(android.app.NotificationManager.class).getActiveNotifications())
        if(item.getId()==AdhanNotification.ID)notice=item;
      assertNotNull(notice);
      assertEquals(PrayerReminders.PLAYBACK,notice.getNotification().getChannelId());
      assertEquals(app.getString(com.zikra.app.R.string.adhan_stop),notice.getNotification().actions[0].title);
      notice.getNotification().actions[0].actionIntent.send();
      end=SystemClock.elapsedRealtime()+5_000;
      while(AdhanService.state().active&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
      assertFalse(AdhanService.state().active);
    } finally {
      AdhanService.stop(app);
      if(page!=null){android.app.Activity opened=page;getInstrumentation().runOnMainSync(opened::finish);}
      Files.deleteIfExists(actual.toPath());
    }
  }
  public void testScheduledAlarmUsesLocalRecordingAndNotificationStop()throws Exception {
    Context app=getInstrumentation().getTargetContext();
    assertTrue(PrayerReminders.exactAllowed(app));
    SharedPreferences settings=PrayerSettings.prefs(app),plan=app.getSharedPreferences(PrayerReminders.PLAN,0);
    Map<String,?> oldSettings=new HashMap<>(settings.getAll()),oldPlan=new HashMap<>(plan.getAll());
    File actual=LocalAdhanAudio.file(app,2);assertFalse(actual.exists());
    assertTrue(actual.getParentFile().isDirectory()||actual.getParentFile().mkdirs());
    android.app.Activity page=null;
    try {
      Files.copy(input.toPath(),actual.toPath());
      page=getInstrumentation().startActivitySync(new Intent(app,com.zikra.app.PrayerActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
      settings.edit().putBoolean("reminder_enabled",true).putBoolean(PrayerSettings.QUIET_CLASSES,false).commit();
      PrayerSettings.setMode(app,2,PrayerMode.ADHAN);PrayerReminders.sync(app);
      long epoch=System.currentTimeMillis()+2_000;
      Intent alarm=PrayerReminders.alarmIntent(app,2,epoch);String slot=alarm.getStringExtra("slot");
      Set<String> ids=new HashSet<>(plan.getStringSet("ids",Collections.emptySet()));ids.add(slot);
      plan.edit().putStringSet("ids",ids).putLong("e:"+slot,epoch)
          .putString("l:"+slot,com.zikra.app.location.LocationStore.current(app).key())
          .putLong("r:"+slot,PrayerSettings.revision(app)).commit();
      android.app.PendingIntent operation=android.app.PendingIntent.getBroadcast(app,0,alarm,
          android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE);
      app.getSystemService(android.app.AlarmManager.class).setExactAndAllowWhileIdle(
          android.app.AlarmManager.RTC_WAKEUP,epoch,operation);
      long end=SystemClock.elapsedRealtime()+15_000;
      while(!AdhanService.state().playing&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
      assertTrue(AdhanService.state().status,AdhanService.state().playing);
      assertFalse(AdhanService.state().preview);
      android.service.notification.StatusBarNotification notice=null;
      for(android.service.notification.StatusBarNotification item:app.getSystemService(android.app.NotificationManager.class).getActiveNotifications())
        if(item.getId()==AdhanNotification.ID)notice=item;
      assertNotNull(notice);assertEquals(AdhanNotification.CHANNEL,notice.getNotification().getChannelId());
      notice.getNotification().actions[0].actionIntent.send();
      end=SystemClock.elapsedRealtime()+5_000;
      while(AdhanService.state().active&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
      assertFalse(AdhanService.state().active);
    } finally {
      AdhanService.stop(app);
      settings.edit().putBoolean("reminder_enabled",false).commit();PrayerReminders.sync(app);
      restore(settings,oldSettings);restore(plan,oldPlan);PrayerReminders.sync(app);
      if(page!=null){android.app.Activity opened=page;getInstrumentation().runOnMainSync(opened::finish);}
      Files.deleteIfExists(actual.toPath());
    }
  }
  private void restore(SharedPreferences prefs,Map<String,?> original) {
    SharedPreferences.Editor edit=prefs.edit().clear();
    for(Map.Entry<String,?> entry:original.entrySet()) {
      String key=entry.getKey();Object value=entry.getValue();
      if(value instanceof String)edit.putString(key,(String)value);
      else if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);
      else if(value instanceof Integer)edit.putInt(key,(Integer)value);
      else if(value instanceof Long)edit.putLong(key,(Long)value);
      else if(value instanceof Float)edit.putFloat(key,(Float)value);
      else if(value instanceof Set)edit.putStringSet(key,new HashSet<>((Set<String>)value));
    }
    edit.commit();
  }
  @Override protected void tearDown()throws Exception {
    if(scope!=null)getInstrumentation().getTargetContext().deleteSharedPreferences(pref);
    if(root!=null)remove(root);
    super.tearDown();
  }
  private void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);assertTrue(file.delete());}
}
