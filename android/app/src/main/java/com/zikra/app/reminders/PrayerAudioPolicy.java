package com.zikra.app.reminders;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import com.zikra.app.data.WeeklyClass;
import com.zikra.app.data.WeeklyClassStore;
import com.zikra.app.location.LocationStore;
import java.time.ZonedDateTime;

public final class PrayerAudioPolicy {
  private PrayerAudioPolicy() {}
  public static boolean deviceAllowsSound(Context c) {
    AudioManager audio=c.getSystemService(AudioManager.class);
    NotificationManager notifications=c.getSystemService(NotificationManager.class);
    return audio.getRingerMode()==AudioManager.RINGER_MODE_NORMAL
        &&audio.getStreamVolume(AudioManager.STREAM_ALARM)>0
        &&notifications.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_ALL;
  }
  public static boolean quietClass(Context c) {
    if(!PrayerSettings.prefs(c).getBoolean(PrayerSettings.QUIET_CLASSES,false))return false;
    ZonedDateTime now=ZonedDateTime.now(LocationStore.current(c).zone);
    int minute=now.getHour()*60+now.getMinute();
    try(WeeklyClassStore store=new WeeklyClassStore(c)){
      for(WeeklyClass lesson:store.all())if(lesson.weekday==now.getDayOfWeek().getValue()
          &&minute>=lesson.startMinute&&minute<lesson.endMinute)return true;
    }catch(android.database.sqlite.SQLiteException unavailable){return true;}
    return false;
  }
}
