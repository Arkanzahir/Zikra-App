package com.zikra.app.reminders;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import com.zikra.app.data.WeeklyClass;
import com.zikra.app.data.WeeklyClassStore;
import com.zikra.app.location.LocationStore;
import java.time.ZonedDateTime;

public final class PrayerAudioPolicy {
  private PrayerAudioPolicy() {}
  public static boolean deviceAllowsSound(Context c) {
    return deviceAllowsSound(c, headphonesConnected(c.getSystemService(AudioManager.class)));
  }
  static boolean deviceAllowsSound(Context c, boolean media) {
    AudioManager audio=c.getSystemService(AudioManager.class);
    NotificationManager notifications=c.getSystemService(NotificationManager.class);
    return audio.getRingerMode()==AudioManager.RINGER_MODE_NORMAL
        &&audio.getStreamVolume(media ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_ALARM)>0
        &&notifications.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_ALL;
  }
  static boolean headphonesConnected(AudioManager audio) {
    for (AudioDeviceInfo output : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
      if (isHeadphone(output)) return true;
    }
    return false;
  }
  static boolean isHeadphone(AudioDeviceInfo output) {
    return switch (output.getType()) {
      case AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
          AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
          AudioDeviceInfo.TYPE_BLE_HEADSET,
          AudioDeviceInfo.TYPE_HEARING_AID -> true;
      default -> false;
    };
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
