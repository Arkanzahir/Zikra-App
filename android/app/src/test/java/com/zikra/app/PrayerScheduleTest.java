package com.zikra.app;

import static org.junit.Assert.*;

import java.time.LocalTime;
import org.junit.Test;

public class PrayerScheduleTest {
  @Test
  public void sunsetCountdownMatchesReference() {
    assertEquals(4, PrayerSchedule.nextPrayerIndex(LocalTime.of(17, 14)));
    assertEquals(27, PrayerSchedule.minutesUntil(LocalTime.of(17, 14), 4));
  }

  @Test
  public void afterLastPrayerWrapsToNextDay() {
    assertEquals(0, PrayerSchedule.nextPrayerIndex(LocalTime.of(23, 59)));
    assertEquals(253, PrayerSchedule.minutesUntil(LocalTime.of(23, 59), 0));
  }

  @Test
  public void sunriseIsNotCountedAsPrayer() {
    assertEquals(2, PrayerSchedule.nextPrayerIndex(LocalTime.of(4, 13)));
  }
}
