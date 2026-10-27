package com.zikra.app.reminders;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import com.zikra.app.PrayerActivity;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.R;
import com.zikra.app.location.*;
import java.time.*;
import java.util.*;

/** Durable seven-day one-shot plan. Delivery rechecks the current city, mode and permission. */
public final class PrayerReminders {
  public static final String CHANNEL="zikra_prayer_times", VIBRATE="zikra_prayer_vibrate", QUIET="zikra_prayer_quiet", PLAYBACK="zikra_adhan_player";
  public static final String PLAN="prayer_alarm_plan";
  static final int REQUEST_BASE=7200, NOTIFICATION_BASE=7300;
  public static final String EXTRA_INDEX="prayer_index", EXTRA_EPOCH="scheduled_epoch";
  private PrayerReminders() {}
  public static boolean enabled(Context c){return PrayerSettings.prefs(c).getBoolean("reminder_enabled",false);}
  public static void createChannel(Context c){
    NotificationManager manager=c.getSystemService(NotificationManager.class);
    NotificationChannel regular=new NotificationChannel(CHANNEL,c.getString(R.string.adhan_channel_normal),NotificationManager.IMPORTANCE_DEFAULT);
    regular.enableVibration(true);manager.createNotificationChannel(regular);
    NotificationChannel vibrate=new NotificationChannel(VIBRATE,c.getString(R.string.adhan_channel_vibrate),NotificationManager.IMPORTANCE_DEFAULT);
    vibrate.setSound(null,null);vibrate.enableVibration(true);manager.createNotificationChannel(vibrate);
    NotificationChannel quiet=new NotificationChannel(QUIET,c.getString(R.string.adhan_channel_quiet),NotificationManager.IMPORTANCE_LOW);
    quiet.setSound(null,null);quiet.enableVibration(false);manager.createNotificationChannel(quiet);
    NotificationChannel player=new NotificationChannel(PLAYBACK,c.getString(R.string.adhan_channel_playback),NotificationManager.IMPORTANCE_LOW);
    player.setSound(null,null);player.enableVibration(false);manager.createNotificationChannel(player);
    AdhanNotification.createChannel(c);
  }
  public static boolean canNotify(Context c){
    return (Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
        &&c.getSystemService(NotificationManager.class).areNotificationsEnabled();
  }
  public static boolean channelAllowed(Context c,String name){
    NotificationChannel channel=c.getSystemService(NotificationManager.class).getNotificationChannel(name);
    return canNotify(c)&&(channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE);
  }
  public static boolean exactAllowed(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
  private static PendingIntent pending(Context c,Intent intent,int flags){return PendingIntent.getBroadcast(c,0,intent,flags|PendingIntent.FLAG_IMMUTABLE);}
  static Intent alarmIntent(Context c,int index,long epoch){
    String key=Instant.ofEpochMilli(epoch).atZone(LocationStore.current(c).zone).toLocalDate()+"_"+index;
    return intent(c,index,epoch,key);
  }
  private static Intent intent(Context c,int index,long epoch,String key){
    return new Intent(c,PrayerAlarmReceiver.class).setAction("com.zikra.app.PRAYER_"+index)
        .setData(new Uri.Builder().scheme("zikra").authority("prayer").appendPath(key).build())
        .putExtra(EXTRA_INDEX,index).putExtra(EXTRA_EPOCH,epoch).putExtra("slot",key)
        .putExtra("location_key",LocationStore.current(c).key()).putExtra("revision",PrayerSettings.revision(c));
  }
  public static synchronized void sync(Context c){
    createChannel(c);AdhanService.stopIfInvalid();
    SharedPreferences plan=c.getSharedPreferences(PLAN,0);Set<String> old=new HashSet<>(plan.getStringSet("ids",Collections.emptySet()));
    Map<String,PrayerAlarmPlan.Entry> next=new LinkedHashMap<>();LocationPoint point=LocationStore.current(c);long now=System.currentTimeMillis();
    if(enabled(c)&&canNotify(c)){
      for(PrayerAlarmPlan.Entry entry:PrayerAlarmPlan.next(now,point))if(PrayerSettings.mode(c,entry.index)!=PrayerMode.OFF)next.put(entry.key,entry);
      for(String key:old)try{
        int index=Integer.parseInt(key.substring(key.lastIndexOf('_')+1));long epoch=plan.getLong("e:"+key,0);
        if(!next.containsKey(key)&&PrayerSchedule.isPrayer(index)&&PrayerSettings.mode(c,index)!=PrayerMode.OFF
            &&PrayerAlarmPlan.timely(epoch,now,PrayerAlarmPlan.MAX_NOTIFICATION_AGE)
            &&point.key().equals(plan.getString("l:"+key,""))&&plan.getLong("r:"+key,-1)==PrayerSettings.revision(c)
            &&PrayerSettings.prefs(c).getLong("prayer_delivered_"+index,-1)!=epoch)next.put(key,new PrayerAlarmPlan.Entry(index,epoch,key));
      }catch(IllegalArgumentException malformed){ }
    }
    AlarmManager alarms=c.getSystemService(AlarmManager.class);SharedPreferences.Editor update=plan.edit();
    for(String key:old)if(!next.containsKey(key)){
      int split=key.lastIndexOf('_');try{
        int index=Integer.parseInt(key.substring(split+1));PendingIntent operation=pending(c,intent(c,index,0,key),PendingIntent.FLAG_NO_CREATE);
        if(operation!=null){alarms.cancel(operation);operation.cancel();}
      }catch(IllegalArgumentException invalid){ }
      update.remove("e:"+key).remove("l:"+key).remove("r:"+key);
    }
    for(PrayerAlarmPlan.Entry entry:next.values()){
      update.putLong("e:"+entry.key,entry.epoch).putString("l:"+entry.key,point.key()).putLong("r:"+entry.key,PrayerSettings.revision(c));
      com.zikra.app.study.StudyAlarmPolicy.schedule(c,Math.max(now+1000,entry.epoch),pending(c,intent(c,entry.index,entry.epoch,entry.key),PendingIntent.FLAG_UPDATE_CURRENT));
    }
    update.putStringSet("ids",new HashSet<>(next.keySet())).commit();
    for(int index=0;index<6;index++)if(PrayerSchedule.isPrayer(index)){
      Intent legacy=new Intent(c,PrayerAlarmReceiver.class).setAction("com.zikra.app.PRAYER_"+index);
      PendingIntent oldIntent=PendingIntent.getBroadcast(c,REQUEST_BASE+index,legacy,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
      if(oldIntent!=null){alarms.cancel(oldIntent);oldIntent.cancel();}
      if(!enabled(c)||PrayerSettings.mode(c,index)==PrayerMode.OFF)c.getSystemService(NotificationManager.class).cancel(NOTIFICATION_BASE+index);
    }
  }
  static boolean valid(Context c,Intent intent){
    int index=intent.getIntExtra(EXTRA_INDEX,-1);long epoch=intent.getLongExtra(EXTRA_EPOCH,0);String key=intent.getStringExtra("slot");
    if(!PrayerSchedule.isPrayer(index)||key==null||!key.endsWith("_"+index)||!enabled(c)||!canNotify(c)
        ||PrayerSettings.mode(c,index)==PrayerMode.OFF||!LocationStore.current(c).key().equals(intent.getStringExtra("location_key"))
        ||intent.getLongExtra("revision",-1)!=PrayerSettings.revision(c)||!PrayerAlarmPlan.timely(epoch,System.currentTimeMillis(),PrayerAlarmPlan.MAX_NOTIFICATION_AGE))return false;
    SharedPreferences plan=c.getSharedPreferences(PLAN,0);
    return plan.getLong("e:"+key,-1)==epoch&&LocationStore.current(c).key().equals(plan.getString("l:"+key,""))
        &&plan.getLong("r:"+key,-1)==PrayerSettings.revision(c);
  }
  static synchronized void deliver(Context c,Intent intent){
    try{
      if(!valid(c,intent))return;
      int index=intent.getIntExtra(EXTRA_INDEX,-1);long epoch=intent.getLongExtra(EXTRA_EPOCH,0);SharedPreferences prefs=PrayerSettings.prefs(c);
      if(prefs.getLong("prayer_delivered_"+index,-1)==epoch)return;
      PrayerMode mode=PrayerSettings.mode(c,index);boolean quiet=PrayerAudioPolicy.quietClass(c);
      boolean audio=mode==PrayerMode.ADHAN&&!quiet&&PrayerAudioPolicy.deviceAllowsSound(c)&&AdhanAsset.available(c,index)
          &&AdhanNotification.allowed(c,false)&&PrayerAlarmPlan.timely(epoch,System.currentTimeMillis(),PrayerAlarmPlan.MAX_AUDIO_AGE);
      if(audio){
        prefs.edit().putLong("prayer_delivered_"+index,epoch).commit();
        if(!AdhanService.fromAlarm(c,intent))post(c,index,epoch,PrayerMode.NOTIFICATION,false,c.getString(R.string.adhan_unavailable));
      }else if(post(c,index,epoch,mode,quiet,null))prefs.edit().putLong("prayer_delivered_"+index,epoch).commit();
    }finally{sync(c);}
  }
  static boolean post(Context c,int index,long epoch,PrayerMode mode,boolean quiet,String reason){
    if(!enabled(c)||PrayerSettings.mode(c,index)==PrayerMode.OFF)return false;
    String channel=quiet?QUIET:mode==PrayerMode.VIBRATE?VIBRATE:mode==PrayerMode.ADHAN?QUIET:CHANNEL;
    if(!channelAllowed(c,channel))return false;
    LocationPoint point=LocationStore.current(c);String time=Instant.ofEpochMilli(epoch).atZone(point.zone).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    PendingIntent open=PendingIntent.getActivity(c,NOTIFICATION_BASE+index,new Intent(c,PrayerActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification notice=new Notification.Builder(c,channel).setSmallIcon(R.drawable.ic_notification_prayer)
        .setContentTitle("Waktu "+PrayerSchedule.NAMES[index]).setContentText(reason==null?time+" "+point.zoneLabel(Instant.ofEpochMilli(epoch))+" · "+point.label:reason)
        .setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_REMINDER).setWhen(epoch).setShowWhen(true)
        .setOnlyAlertOnce(true).setAutoCancel(true).setContentIntent(open).setColor(c.getColor(R.color.pine)).build();
    try{c.getSystemService(NotificationManager.class).notify(NOTIFICATION_BASE+index,notice);return true;}
    catch(SecurityException revoked){return false;}
  }
}
