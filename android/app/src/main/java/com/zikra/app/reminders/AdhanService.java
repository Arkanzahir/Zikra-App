package com.zikra.app.reminders;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.net.Uri;
import android.os.*;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.R;
import com.zikra.app.location.LocationStore;
import java.util.Objects;

/** One bounded local recording. Notification ducking continues; other focus loss stops playback. */
public final class AdhanService extends Service {
  private static final String PLAY="adhan.play", PREVIEW="adhan.preview", STOP="adhan.stop";
  private static final int NOTICE=AdhanNotification.ID;
  private static final long MAX_PLAY_MS=300000;
  private static volatile AdhanService active;
  public static final class State {
    public final boolean active, playing, preview;
    public final int index, position, duration;
    public final String status;
    State(boolean active,boolean playing,boolean preview,int index,int position,int duration,String status){
      this.active=active;this.playing=playing;this.preview=preview;this.index=index;
      this.position=position;this.duration=duration;this.status=status;
    }
  }
  private static volatile State state=new State(false,false,false,-1,0,0,"");
  public static State state(){return state;}
  public static boolean preview(Context c,int index){return start(c,new Intent(c,AdhanService.class).setAction(PREVIEW).putExtra(PrayerReminders.EXTRA_INDEX,index));}
  static boolean fromAlarm(Context c,Intent alarm){return start(c,new Intent(alarm).setClass(c,AdhanService.class).setAction(PLAY));}
  private static boolean start(Context c,Intent command){
    try{c.startForegroundService(command);return true;}
    catch(SecurityException|IllegalStateException blocked){return false;}
  }
  public static void stop(Context c){c.stopService(new Intent(c,AdhanService.class));}
  static Intent stopIntent(Context c,String token){
    return new Intent(c,AdhanService.class).setAction(STOP)
        .setData(Uri.parse("zikra://adhan/stop/"+Uri.encode(token))).putExtra("token",token);
  }
  public static void stopIfInvalid(){AdhanService current=active;if(current!=null)current.main.post(current::check);}

  private final Handler main=new Handler(Looper.getMainLooper());
  private MediaPlayer player;
  private VolumeShaper entrance;
  private AudioManager audio;
  private AudioFocusRequest focus;
  private boolean preview, prepared, foreground;
  private int index=-1, generation;
  private long epoch, revision, began;
  private String locationKey, token, status="";
  private final SharedPreferences.OnSharedPreferenceChangeListener settings=(p,key)->check();
  private final Runnable prepareTimeout=()->finish(getString(R.string.adhan_unavailable),true);
  private final Runnable tick=new Runnable(){@Override public void run(){
    check();if(player!=null){publish();main.postDelayed(this,1000);}
  }};
  private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){finish(getString(R.string.adhan_interrupted),false);}};

  @Override public void onCreate(){
    super.onCreate();active=this;audio=getSystemService(AudioManager.class);PrayerReminders.createChannel(this);
    PrayerSettings.prefs(this).registerOnSharedPreferenceChangeListener(settings);
    getSharedPreferences(LocationStore.PREFS,0).registerOnSharedPreferenceChangeListener(settings);
    IntentFilter filter=new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,filter);
  }
  @Override public IBinder onBind(Intent intent){return null;}
  @Override public int onStartCommand(Intent command,int flags,int id){
    if(command==null){stopSelf();return START_NOT_STICKY;}
    if(STOP.equals(command.getAction())){
      if(command.getStringExtra("token")==null||Objects.equals(token,command.getStringExtra("token")))finish(getString(R.string.adhan_stopped),false);
      else if(token==null)stopSelf();
      return START_NOT_STICKY;
    }
    if(!PLAY.equals(command.getAction())&&!PREVIEW.equals(command.getAction())){stopSelf();return START_NOT_STICKY;}
    boolean nextPreview=PREVIEW.equals(command.getAction());int nextIndex=command.getIntExtra(PrayerReminders.EXTRA_INDEX,-1);
    if(!PrayerSchedule.isPrayer(nextIndex)){stopSelf();return START_NOT_STICKY;}
    long nextEpoch=command.getLongExtra(PrayerReminders.EXTRA_EPOCH,0);
    String nextToken=nextPreview?"preview:"+SystemClock.elapsedRealtime():nextIndex+":"+nextEpoch;
    if(player!=null&&nextToken.equals(token))return START_NOT_STICKY;
    release();preview=nextPreview;index=nextIndex;epoch=nextEpoch;token=nextToken;
    locationKey=command.getStringExtra("location_key");revision=command.getLongExtra("revision",-1);began=SystemClock.elapsedRealtime();
    if(!validAlarm()||!AdhanNotification.allowed(this,preview)||!AdhanAsset.available(this,index)){
      finish(getString(R.string.adhan_unavailable),true);return START_NOT_STICKY;
    }
    if(!PrayerAudioPolicy.deviceAllowsSound(this)||PrayerAudioPolicy.quietClass(this)){
      finish(getString(R.string.adhan_silent_device),true);return START_NOT_STICKY;
    }
    if(!preview&&!PrayerAlarmPlan.timely(epoch,System.currentTimeMillis(),PrayerAlarmPlan.MAX_AUDIO_AGE)){
      finish(getString(R.string.adhan_late),true);return START_NOT_STICKY;
    }
    status=getString(R.string.adhan_preparing);
    try{
      Notification notice=notification();
      if(Build.VERSION.SDK_INT>=29)startForeground(NOTICE,notice,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);else startForeground(NOTICE,notice);
      foreground=true;prepare();
    }catch(SecurityException|IllegalStateException failure){finish(getString(R.string.adhan_unavailable),true);}
    return START_NOT_STICKY;
  }
  private boolean validAlarm(){
    return preview||(PrayerSchedule.isPrayer(index)&&PrayerReminders.enabled(this)
        &&PrayerSettings.mode(this,index)==PrayerMode.ADHAN&&LocationStore.current(this).key().equals(locationKey)
        &&PrayerSettings.revision(this)==revision&&PrayerSettings.prefs(this).getLong("prayer_delivered_"+index,-1)==epoch);
  }
  private void check(){
    if(player==null)return;
    if(!validAlarm()||!AdhanNotification.allowed(this,preview)
        ||!PrayerAudioPolicy.deviceAllowsSound(this)||PrayerAudioPolicy.quietClass(this)
        ||SystemClock.elapsedRealtime()-began>MAX_PLAY_MS)finish(getString(R.string.adhan_stopped),false);
  }
  private AudioAttributes attributes(){return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();}
  private void focusChanged(int run,int change){
    if(run!=generation||player==null)return;
    if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK||change==AudioManager.AUDIOFOCUS_GAIN){
      float volume=change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK?0.2f:1f;
      try{player.setVolume(volume,volume);}
      catch(IllegalStateException unavailable){finish(getString(R.string.adhan_unavailable),true);}
    }else if(change<0)finish(getString(R.string.adhan_interrupted),false);
  }
  private void prepare(){
    final int run=++generation;final MediaPlayer next=new MediaPlayer();player=next;
    next.setAudioAttributes(attributes());next.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
    next.setOnPreparedListener(ready->{
      if(run!=generation||player!=ready)return;
      prepared=true;check();if(player!=ready)return;
      try{
      if(!preview&&!PrayerAlarmPlan.timely(epoch,System.currentTimeMillis(),PrayerAlarmPlan.MAX_AUDIO_AGE)){finish(getString(R.string.adhan_late),true);return;}
      if(ready.getDuration()<=0||ready.getDuration()>MAX_PLAY_MS){finish(getString(R.string.adhan_unavailable),true);return;}
      focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes())
          .setOnAudioFocusChangeListener(change->focusChanged(run,change),main).build();
      if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){finish(getString(R.string.adhan_focus_denied),true);return;}
      if(run!=generation||player!=ready)return;
      main.removeCallbacks(prepareTimeout);
      try{
        if(index!=0){
          entrance=ready.createVolumeShaper(new VolumeShaper.Configuration.Builder()
              .setDuration(600).setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
              .setCurve(new float[]{0f,1f},new float[]{0f,1f}).build());
          entrance.apply(VolumeShaper.Operation.PLAY);
        }
        ready.start();
      }
      catch(IllegalStateException failed){finish(getString(R.string.adhan_unavailable),true);return;}
      if(!preview&&PrayerSettings.prefs(this).getBoolean(PrayerSettings.PAUSE_FOCUS,false))com.zikra.app.study.FocusTimer.pauseForAdhan(this);
      status=getString(R.string.adhan_notification_playing);
      publish();getSystemService(NotificationManager.class).notify(NOTICE,notification());
      }catch(SecurityException|IllegalStateException failure){finish(getString(R.string.adhan_unavailable),true);}
    });
    next.setOnCompletionListener(done->{if(run==generation)finish(getString(R.string.adhan_finished),false);});
    next.setOnErrorListener((failed,what,extra)->{if(run==generation)finish(getString(R.string.adhan_unavailable),true);return true;});
    try(AssetFileDescriptor source=AdhanAsset.open(this,index)){
      next.setDataSource(source.getFileDescriptor(),source.getStartOffset(),source.getLength());next.prepareAsync();
      main.postDelayed(prepareTimeout,10000);main.post(tick);publish();
    }catch(java.io.IOException|IllegalStateException|IllegalArgumentException failed){finish(getString(R.string.adhan_unavailable),true);}
  }
  private Notification notification(){
    return AdhanNotification.create(this,index,preview,token,status);
  }
  private void publish(){
    int position=0,duration=0;boolean playing=false;
    try{if(prepared&&player!=null){position=player.getCurrentPosition();duration=player.getDuration();playing=player.isPlaying();}}
    catch(IllegalStateException invalid){ }
    state=new State(player!=null,playing,preview,index,position,duration,status);
  }
  private void finish(String message,boolean fallback){
    status=message;
    if(fallback&&!preview&&PrayerSchedule.isPrayer(index)&&LocationStore.current(this).key().equals(locationKey))
      PrayerReminders.post(this,index,epoch,PrayerMode.NOTIFICATION,PrayerAudioPolicy.quietClass(this)||!PrayerAudioPolicy.deviceAllowsSound(this),message);
    release();publish();stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;stopSelf();
  }
  private void release(){
    generation++;main.removeCallbacks(tick);main.removeCallbacks(prepareTimeout);
    MediaPlayer old=player;player=null;prepared=false;
    VolumeShaper oldEntrance=entrance;entrance=null;
    if(oldEntrance!=null)try{oldEntrance.close();}catch(IllegalStateException released){ }
    if(old!=null){old.setOnPreparedListener(null);old.setOnCompletionListener(null);old.setOnErrorListener(null);old.release();}
    AudioFocusRequest oldFocus=focus;focus=null;if(oldFocus!=null)audio.abandonAudioFocusRequest(oldFocus);
  }
  @Override public void onDestroy(){
    if(player!=null)status=getString(R.string.adhan_stopped);
    PrayerSettings.prefs(this).unregisterOnSharedPreferenceChangeListener(settings);
    getSharedPreferences(LocationStore.PREFS,0).unregisterOnSharedPreferenceChangeListener(settings);
    unregisterReceiver(noisy);release();publish();main.removeCallbacksAndMessages(null);
    if(active==this)active=null;if(foreground)stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
  }
}
