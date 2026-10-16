package com.zikra.app;

import java.time.LocalTime;

/** Offline reference timetable from the supplied prototype, not a live prayer-time feed. */
public final class PrayerSchedule {
  public static final String[] NAMES = {"Subuh", "Terbit", "Dzuhur", "Ashar", "Maghrib", "Isya"};
  public static final String[] TIMES = {"04:12", "05:28", "11:35", "14:52", "17:41", "18:50"};

  private PrayerSchedule() {}

  public static int nextPrayerIndex(LocalTime now) {
    for (int i = 0; i < TIMES.length; i++)
      if (i != 1 && LocalTime.parse(TIMES[i]).isAfter(now)) return i;
    return 0;
  }

  public static int minutesUntil(LocalTime now, int index) {
    int minute = now.getHour() * 60 + now.getMinute();
    LocalTime target = LocalTime.parse(TIMES[index]);
    int diff = target.getHour() * 60 + target.getMinute() - minute;
    return diff <= 0 ? diff + 1440 : diff;
  }
}
