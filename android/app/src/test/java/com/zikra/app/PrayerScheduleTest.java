package com.zikra.app;

import static org.junit.Assert.*;

import java.time.*;
import org.junit.Test;

public class PrayerScheduleTest {
  @Test
  public void matchesIndependentAstralFixturesAcrossSeasons() {
    // Astral 3.2, sea-level observer (-7.280,112.790), WIB; twilight 20/18 degrees,
    // geometric Asr shadow 1; rounded upward plus 2 minutes except sunrise.
    String[][] fixtures = {
      {"2026-01-15", "04:01", "05:24", "11:41", "15:05", "17:55", "19:11"},
      {"2026-04-15", "04:15", "05:31", "11:31", "14:51", "17:30", "18:40"},
      {"2026-07-15", "04:23", "05:43", "11:37", "14:59", "17:29", "18:44"},
      {"2026-10-17", "03:50", "05:07", "11:17", "14:23", "17:25", "18:35"}
    };
    for (String[] row : fixtures) {
      PrayerSchedule.Day day = PrayerSchedule.forDate(LocalDate.parse(row[0]));
      for (int i = 0; i < 6; i++)
        assertTrue(
            row[0] + " " + PrayerSchedule.NAMES[i],
            Math.abs(Duration.between(LocalTime.parse(row[i + 1]), day.at(i)).toMinutes()) <= 1);
    }
  }

  @Test
  public void eventsStayOrderedAcrossLeapYearAndSeasons() {
    for (int day = 0; day < 366; day++) {
      PrayerSchedule.Day schedule = PrayerSchedule.forDate(LocalDate.of(2028, 1, 1).plusDays(day));
      for (int i = 1; i < 6; i++) assertTrue(schedule.at(i).isAfter(schedule.at(i - 1)));
    }
  }

  @Test
  public void datesHaveDifferentDailyTimes() {
    assertNotEquals(
        PrayerSchedule.forDate(LocalDate.of(2026, 1, 15)).time(0),
        PrayerSchedule.forDate(LocalDate.of(2026, 7, 15)).time(0));
  }

  @Test
  public void afterLastPrayerUsesTomorrowsCalculatedFajr() {
    ZonedDateTime now = LocalDate.of(2026, 10, 17).atTime(23, 59).atZone(PrayerSchedule.ZONE);
    PrayerSchedule.NextPrayer next = PrayerSchedule.nextAfter(now);
    assertEquals(0, next.index);
    assertEquals(LocalDate.of(2026, 10, 18), next.time.toLocalDate());
    assertEquals(PrayerSchedule.forDate(LocalDate.of(2026, 10, 18)).at(0), next.time.toLocalTime());
  }

  @Test
  public void sunriseIsNotAnObligatoryPrayer() {
    LocalDate date = LocalDate.of(2026, 10, 17);
    ZonedDateTime now =
        date.atTime(PrayerSchedule.forDate(date).at(0).plusMinutes(1)).atZone(PrayerSchedule.ZONE);
    assertEquals(2, PrayerSchedule.nextAfter(now).index);
  }

  @Test
  public void theSameInstantGivesSameEventInOtherTimeZones() {
    ZonedDateTime now = LocalDate.of(2026, 10, 17).atTime(16, 0).atZone(PrayerSchedule.ZONE);
    assertEquals(
        PrayerSchedule.nextAfter(now).time,
        PrayerSchedule.nextAfter(now.withZoneSameInstant(ZoneId.of("UTC"))).time);
  }

  @Test
  public void exactBoundaryMovesToNextOccurrenceAndRejectsSunriseAlarm() {
    LocalDate day = LocalDate.of(2026, 10, 17);
    ZonedDateTime atPrayer =
        day.atTime(PrayerSchedule.forDate(day).at(2)).atZone(PrayerSchedule.ZONE);
    assertEquals(3, PrayerSchedule.nextAfter(atPrayer).index);
    assertEquals(day.plusDays(1), PrayerSchedule.nextOccurrence(2, atPrayer).toLocalDate());
    try {
      PrayerSchedule.nextOccurrence(1, atPrayer);
      fail("Sunrise must not schedule a prayer alarm");
    } catch (IllegalArgumentException expected) {
    }
  }
}
