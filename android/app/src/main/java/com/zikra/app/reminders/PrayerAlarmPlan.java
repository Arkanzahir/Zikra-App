package com.zikra.app.reminders;

import com.zikra.app.PrayerSchedule;
import com.zikra.app.location.LocationPoint;
import java.time.*;
import java.util.*;

/** Next seven city calendar days; sunrise never creates a prayer alarm. */
public final class PrayerAlarmPlan {
  public static final long MAX_NOTIFICATION_AGE=30*60000L, MAX_AUDIO_AGE=2*60000L;
  public static final class Entry {
    public final int index;
    public final long epoch;
    public final String key;
    public Entry(int index,long epoch,String key){this.index=index;this.epoch=epoch;this.key=key;}
  }
  private PrayerAlarmPlan() {}
  public static List<Entry> next(long now,LocationPoint point) {
    LocalDate today=Instant.ofEpochMilli(now).atZone(point.zone).toLocalDate();List<Entry> result=new ArrayList<>();
    for(int offset=0;offset<7;offset++){
      LocalDate date=today.plusDays(offset);PrayerSchedule.Day day=PrayerSchedule.forDate(date,point);
      for(int index=0;index<6;index++)if(PrayerSchedule.isPrayer(index)&&day.at(index)!=null){
        long epoch=date.atTime(day.at(index)).atZone(point.zone).toInstant().toEpochMilli();
        if(epoch>now)result.add(new Entry(index,epoch,date+"_"+index));
      }
    }
    result.sort(Comparator.comparingLong(item->item.epoch));return result;
  }
  public static boolean timely(long epoch,long now,long maximumAge) {
    return epoch>0&&now>=epoch&&now-epoch<=maximumAge;
  }
}
