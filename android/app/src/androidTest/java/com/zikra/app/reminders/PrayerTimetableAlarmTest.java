package com.zikra.app.reminders;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.test.InstrumentationTestCase;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.prayer.PrayerTimetables;
import java.io.File;
import java.time.*;
import java.util.Collections;

/** Checks schedule replacement and duplicate suppression without playing audio or changing real time. */
@SuppressWarnings("deprecation")
public final class PrayerTimetableAlarmTest extends InstrumentationTestCase {
  public void testChangedTableRejectsOldIntentAndSameDayDoesNotReplay() throws Exception {
    Context c=getInstrumentation().getTargetContext();
    assertTrue(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"));
    assertTrue(new File(c.getFilesDir(),"prayer-source-authorized").isFile());
    LocationStore.save(c,LocationPoint.INITIAL);
    android.content.SharedPreferences prefs=PrayerSettings.prefs(c);
    prefs.edit().putBoolean("reminder_enabled",true).commit();
    PrayerSettings.setMode(c,4,PrayerMode.NOTIFICATION);
    PrayerReminders.createChannel(c);
    long epoch=System.currentTimeMillis()-1000;
    LocalDate date=Instant.ofEpochMilli(epoch).atZone(LocationPoint.INITIAL.zone).toLocalDate();
    String key=date+"_4";
    Intent old=PrayerReminders.alarmIntent(c,4,epoch);
    c.getSharedPreferences(PrayerReminders.PLAN,0).edit().putStringSet("ids",Collections.singleton(key))
        .putLong("e:"+key,epoch).putString("l:"+key,LocationPoint.INITIAL.key())
        .putLong("r:"+key,PrayerSettings.revision(c)).commit();
    assertTrue(PrayerReminders.canNotify(c));
    assertTrue(PrayerReminders.valid(c,old));
    PrayerSchedule.Day original=PrayerSchedule.forDate(date);
    LocalTime[] times=new LocalTime[6];for(int i=0;i<6;i++)times[i]=original.at(i);
    times[4]=times[4].plusMinutes(1);
    PrayerSchedule.Day changed=PrayerSchedule.fromTimetable(date,times,"changed-public-fixture");
    try {
      PrayerSchedule.setTimetable((d,p)->d.equals(date)?changed:null);
      assertFalse(PrayerReminders.valid(c,old));
      prefs.edit().putLong("prayer_delivered_4",epoch-60000).commit();
      assertTrue(PrayerReminders.deliveredOnDate(c,4,epoch));
      assertFalse(PrayerReminders.deliveredOnDate(c,4,epoch+24*60*60*1000L));
    } finally {
      PrayerSchedule.setTimetable(PrayerTimetables.get(c));
      prefs.edit().putBoolean("reminder_enabled",false).remove("prayer_delivered_4").commit();
      PrayerReminders.sync(c);
    }
  }
}
