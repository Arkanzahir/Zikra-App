package com.zikra.app.sky;

import com.zikra.app.PrayerSchedule;
import java.time.*;

/** Daily astronomical illustration, not a compass, lunar rise map or rukyat decision. */
public final class SkyDay {
  public final LocalDate date;
  public final LocalTime rise, noon, set;
  public final double illumination;
  public final boolean waxing, usno, solarAvailable;

  public SkyDay(
      LocalDate date,
      LocalTime rise,
      LocalTime noon,
      LocalTime set,
      double illumination,
      boolean waxing,
      boolean usno) {
    this(date, rise, noon, set, illumination, waxing, usno, true);
  }

  public SkyDay(
      LocalDate date,
      LocalTime rise,
      LocalTime noon,
      LocalTime set,
      double illumination,
      boolean waxing,
      boolean usno,
      boolean solarAvailable) {
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
    this.solarAvailable = solarAvailable;
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
    return estimate(date, com.zikra.app.location.LocationPoint.INITIAL);
  }

  public static SkyDay estimate(LocalDate date, com.zikra.app.location.LocationPoint point) {
    PrayerSchedule.Day prayer = PrayerSchedule.forDate(date, point);
    boolean available =
        prayer.at(1) != null
            && prayer.at(2) != null
            && prayer.at(4) != null
            && prayer.at(1).isBefore(prayer.at(2))
            && prayer.at(2).isBefore(prayer.at(4));
    // Mean synodic approximation; explicit fallback label, not an API observation.
    double days =
        Duration.between(
                    Instant.parse("2000-01-06T18:14:00Z"),
                    date.atTime(12, 0).atZone(point.zone).toInstant())
                .getSeconds()
            / 86400d;
    double age = ((days / 29.530588853) % 1 + 1) % 1;
    return new SkyDay(
        date,
        available ? prayer.at(1) : LocalTime.of(6, 0),
        available ? prayer.at(2).minusMinutes(2) : LocalTime.NOON,
        available ? prayer.at(4).minusMinutes(2) : LocalTime.of(18, 0),
        (1 - Math.cos(age * 2 * Math.PI)) / 2,
        age < .5,
        false,
        available);
  }
}
