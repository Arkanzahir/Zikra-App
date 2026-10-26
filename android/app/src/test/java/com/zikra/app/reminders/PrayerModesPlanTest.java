package com.zikra.app.reminders;

import static org.junit.Assert.*;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.location.LocationPoint;
import java.time.*;
import java.util.*;
import org.junit.Test;

public final class PrayerModesPlanTest {
  @Test public void oldPreferencesAndUnknownModesNeverEnableAdhan() {
    assertEquals(PrayerMode.NOTIFICATION,PrayerMode.stored(null,true));
    assertEquals(PrayerMode.OFF,PrayerMode.stored(null,false));
    assertEquals(PrayerMode.NOTIFICATION,PrayerMode.stored("unknown",true));
    assertEquals(PrayerMode.OFF,PrayerMode.stored("ADHAN",false));
    for(PrayerMode mode:PrayerMode.values())assertEquals(mode,PrayerMode.stored(mode.name(),true));
  }
  @Test public void sevenCityDatesHaveUniquePrayerSlotsWithoutSunrise() {
    LocationPoint point=LocationPoint.INITIAL;LocalDate day=LocalDate.of(2026,10,27);
    long now=day.atStartOfDay(point.zone).toInstant().toEpochMilli();List<PrayerAlarmPlan.Entry> plan=PrayerAlarmPlan.next(now,point);
    assertEquals(35,plan.size());Set<String> keys=new HashSet<>();long previous=now;
    for(PrayerAlarmPlan.Entry entry:plan){assertTrue(PrayerSchedule.isPrayer(entry.index));assertTrue(entry.epoch>previous);previous=entry.epoch;assertTrue(keys.add(entry.key));assertTrue(Instant.ofEpochMilli(entry.epoch).atZone(point.zone).toLocalDate().isBefore(day.plusDays(7)));}
    assertEquals(30,PrayerAlarmPlan.next(day.atTime(23,59).atZone(point.zone).toInstant().toEpochMilli(),point).size());
  }
  @Test public void selectedZoneAndMissingPolarEventsArePreserved() {
    LocationPoint london=new LocationPoint(51.5074,-.1278,"Europe/London","Fixture","manual",0,-1);
    long now=Instant.parse("2026-10-24T23:00:00Z").toEpochMilli();
    for(PrayerAlarmPlan.Entry entry:PrayerAlarmPlan.next(now,london)){
      ZonedDateTime at=Instant.ofEpochMilli(entry.epoch).atZone(london.zone);
      assertEquals(PrayerSchedule.forDate(at.toLocalDate(),london).at(entry.index),at.toLocalTime());
    }
    LocationPoint polar=new LocationPoint(69.6492,18.9553,"Europe/Oslo","Fixture","manual",0,-1);
    List<PrayerAlarmPlan.Entry> plan=PrayerAlarmPlan.next(LocalDate.of(2026,1,15).atStartOfDay(polar.zone).toInstant().toEpochMilli(),polar);
    assertFalse(plan.isEmpty());assertTrue(plan.size()<35);
  }
  @Test public void futureAndOverdueAudioAreRejectedWithoutChangingNoticeWindow() {
    long epoch=10000000;
    assertFalse(PrayerAlarmPlan.timely(epoch,epoch-1,PrayerAlarmPlan.MAX_AUDIO_AGE));
    assertTrue(PrayerAlarmPlan.timely(epoch,epoch+120000,PrayerAlarmPlan.MAX_AUDIO_AGE));
    assertFalse(PrayerAlarmPlan.timely(epoch,epoch+120001,PrayerAlarmPlan.MAX_AUDIO_AGE));
    assertTrue(PrayerAlarmPlan.timely(epoch,epoch+1800000,PrayerAlarmPlan.MAX_NOTIFICATION_AGE));
    assertFalse(PrayerAlarmPlan.timely(epoch,epoch+1800001,PrayerAlarmPlan.MAX_NOTIFICATION_AGE));
    assertFalse(PrayerAlarmPlan.timely(0,epoch,PrayerAlarmPlan.MAX_AUDIO_AGE));
  }
}
