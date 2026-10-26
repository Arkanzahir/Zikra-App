package com.zikra.app.data;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/** Weekly wall-clock times follow the selected city's zone, rather than the device zone. */
public final class WeeklyClass {
  public final long id, revision;
  public final String title, room, teacher;
  public final int weekday, startMinute, endMinute, reminderMinutes, credits;

  public WeeklyClass(long id, long revision, String title, String room, String teacher,
      int weekday, int startMinute, int endMinute, int reminderMinutes) {
    this(id,revision,title,room,teacher,weekday,startMinute,endMinute,reminderMinutes,0);
  }
  public WeeklyClass(long id, long revision, String title, String room, String teacher,
      int weekday, int startMinute, int endMinute, int reminderMinutes, int credits) {
    if (id < 0 || revision < 0 || weekday < 1 || weekday > 7 || startMinute < 0
        || startMinute >= endMinute || endMinute > 1439
        || credits < 0 || credits > 30
        || (reminderMinutes != 0 && reminderMinutes != 15 && reminderMinutes != 30))
      throw new IllegalArgumentException("Invalid weekly class");
    this.id = id; this.revision = revision;
    this.title = label(title, 100, true);
    this.room = label(room, 100, false);
    this.teacher = label(teacher, 100, false);
    this.weekday = weekday; this.startMinute = startMinute;
    this.endMinute = endMinute; this.reminderMinutes = reminderMinutes;
    this.credits = credits;
  }

  public static String label(String value, int max, boolean required) {
    if (value == null) throw new IllegalArgumentException("Missing label");
    String result = value.trim();
    if ((required && result.isEmpty()) || result.length() > max
        || result.codePoints().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException("Invalid label");
    return result;
  }

  public boolean overlaps(WeeklyClass other) {
    return weekday == other.weekday && startMinute < other.endMinute
        && endMinute > other.startMinute;
  }

  public long startOn(LocalDate date, ZoneId zone) {
    if (date.getDayOfWeek().getValue() != weekday)
      throw new IllegalArgumentException("Wrong class day");
    return date.atTime(LocalTime.of(startMinute / 60, startMinute % 60))
        .atZone(zone).toInstant().toEpochMilli();
  }

  public long nextReminder(long now, ZoneId zone) {
    if (reminderMinutes == 0) return 0;
    LocalDate date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(weekday)));
    long trigger = startOn(date, zone) - reminderMinutes * 60000L;
    return trigger > now ? trigger : startOn(date.plusWeeks(1), zone) - reminderMinutes * 60000L;
  }
}
