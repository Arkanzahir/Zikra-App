package com.zikra.app.sky;

import com.zikra.app.PrayerSchedule;
import java.time.*;

/** Daily astronomical illustration, not a compass, lunar rise map or rukyat decision. */
public final class SkyDay {
  public final LocalDate date;
  public final LocalTime rise, noon, set;
  public final double illumination;
  public final boolean waxing, usno;

  public SkyDay(
      LocalDate date,
      LocalTime rise,
      LocalTime noon,
      LocalTime set,
      double illumination,
      boolean waxing,
      boolean usno) {
    if (!(rise.isBefore(noon) && noon.isBefore(set))
        || !Double.isFinite(illumination)
        || illumination < 0
        || illumination > 1) throw new IllegalArgumentException("Invalid sky data");
    this.date = date;
    this.rise = rise;
    this.noon = noon;
    this.set = set;
    this.illumination = illumination;
    this.waxing = waxing;
    this.usno = usno;
  }

  public boolean daytime(LocalTime time) {
    return !time.isBefore(rise) && time.isBefore(set);
  }

  /** Noon is the centre of the arc, rather than an assumed 12:00 or 13:00. */
  public float sunProgress(LocalTime time) {
    double s = time.toSecondOfDay(),
        a = rise.toSecondOfDay(),
        b = noon.toSecondOfDay(),
        c = set.toSecondOfDay();
    return (float)
        Math.max(0, Math.min(1, s <= b ? .5 * (s - a) / (b - a) : .5 + .5 * (s - b) / (c - b)));
  }

  public static SkyDay estimate(LocalDate date) {
    PrayerSchedule.Day prayer = PrayerSchedule.forDate(date);
    // Mean synodic approximation; explicit fallback label, not an API observation.
    double days =
        Duration.between(
                    Instant.parse("2000-01-06T18:14:00Z"),
                    date.atTime(12, 0).atZone(PrayerSchedule.ZONE).toInstant())
                .getSeconds()
            / 86400d;
    double age = ((days / 29.530588853) % 1 + 1) % 1;
    return new SkyDay(
        date,
        prayer.at(1),
        prayer.at(2).minusMinutes(2),
        prayer.at(4).minusMinutes(2),
        (1 - Math.cos(age * 2 * Math.PI)) / 2,
        age < .5,
        false);
  }
}
