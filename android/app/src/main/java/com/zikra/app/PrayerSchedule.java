package com.zikra.app;

import com.zikra.app.location.LocationPoint;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared city-timetable selection with an explicit solar fallback for a coordinate and time zone.
 * Every UI and alarm consumer uses the same selected daily values.
 */
public final class PrayerSchedule {
  public interface Timetable {
    Day find(LocalDate date, LocationPoint point);
  }

  private static volatile Timetable timetable;

  public static void setTimetable(Timetable value) {
    timetable = value;
  }

  public static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
  public static final double LATITUDE = -7.280, LONGITUDE = 112.790;
  public static final String[] NAMES = {"Subuh", "Terbit", "Dzuhur", "Ashar", "Maghrib", "Isya"};
  public static final String[] KEYS = {"subuh", "terbit", "dzuhur", "ashar", "maghrib", "isya"};
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final Map<String, Day> CACHE =
      new LinkedHashMap<String, Day>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Day> entry) {
          return size() > 32;
        }
      };

  private PrayerSchedule() {}

  public static final class Day {
    public final LocalDate date;
    public final String source;
    private final LocalTime[] times;

    private Day(LocalDate date, LocalTime[] times) {
      this(date, times, "local");
    }

    private Day(LocalDate date, LocalTime[] times, String source) {
      this.date = date;
      this.times = times.clone();
      this.source = source;
    }

    public LocalTime at(int index) {
      return times[index];
    }

    public String time(int index) {
      return at(index) == null ? "—" : TIME.format(at(index));
    }

    public String signature() { return date + ":" + source + ":" + java.util.Arrays.toString(times); }
  }

  public static Day fromTimetable(LocalDate date, LocalTime[] times, String source) {
    if (date == null || times == null || times.length != 6 || source == null)
      throw new IllegalArgumentException("Timetable day");
    for (int i = 0; i < times.length; i++)
      if (times[i] == null || (i > 0 && !times[i].isAfter(times[i - 1])))
        throw new IllegalArgumentException("Timetable order");
    return new Day(date, times, source);
  }

  public static final class NextPrayer {
    public final int index;
    public final ZonedDateTime time;

    private NextPrayer(int index, ZonedDateTime time) {
      this.index = index;
      this.time = time;
    }

    public int minutesFrom(ZonedDateTime now) {
      return (int) Math.max(0, (Duration.between(now, time).getSeconds() + 59) / 60);
    }
  }

  public static boolean isPrayer(int index) {
    return index >= 0 && index < 6 && index != 1;
  }

  public static synchronized Day forDate(LocalDate date) {
    return forDate(date, LocationPoint.INITIAL);
  }

  public static synchronized Day forDate(LocalDate date, LocationPoint point) {
    Timetable external = timetable;
    Day selected = external == null ? null : external.find(date, point);
    return selected == null ? calculated(date, point) : selected;
  }

  /** Solar events for illustration and the explicitly labelled fallback, independent of city tables. */
  public static synchronized Day calculated(LocalDate date, LocationPoint point) {
    String key = date + ":" + point.key();
    Day cached = CACHE.get(key);
    if (cached != null) return cached;
    // Julian day at UTC midnight of the requested local calendar date. Morning events can
    // occur on the previous UTC day; iteration intentionally permits negative UTC minutes.
    double julian = date.toEpochDay() + 2440587.5;
    LocalTime[] values = new LocalTime[6];
    values[0] = localTime(solarEvent(julian, point, -20, false, false), 2, date, point);
    values[1] = localTime(solarEvent(julian, point, -0.833, false, false), 0, date, point);
    double noon = 720 - 4 * point.longitude;
    for (int i = 0; i < 3; i++) noon = 720 - 4 * point.longitude - solar(julian + noon / 1440)[1];
    values[2] = localTime(noon, 2, date, point);
    values[3] = localTime(solarEvent(julian, point, 0, true, true), 2, date, point);
    values[4] = localTime(solarEvent(julian, point, -0.833, true, false), 2, date, point);
    values[5] = localTime(solarEvent(julian, point, -18, true, false), 2, date, point);
    Day day = new Day(date, values);
    CACHE.put(key, day);
    return day;
  }

  public static NextPrayer nextAfter(ZonedDateTime now) {
    return nextAfter(now, LocationPoint.INITIAL);
  }

  public static NextPrayer nextAfter(ZonedDateTime now, LocationPoint point) {
    ZonedDateTime local = now.withZoneSameInstant(point.zone);
    Day today = forDate(local.toLocalDate(), point);
    for (int index = 0; index < 6; index++) {
      if (!isPrayer(index) || today.at(index) == null) continue;
      ZonedDateTime candidate = today.date.atTime(today.at(index)).atZone(point.zone);
      if (candidate.isAfter(local)) return new NextPrayer(index, candidate);
    }
    Day tomorrow = forDate(local.toLocalDate().plusDays(1), point);
    for (int index = 0; index < 6; index++) {
      if (isPrayer(index) && tomorrow.at(index) != null)
        return new NextPrayer(index, tomorrow.date.atTime(tomorrow.at(index)).atZone(point.zone));
    }
    return null;
  }

  public static ZonedDateTime nextOccurrence(int index, ZonedDateTime now) {
    return nextOccurrence(index, now, LocationPoint.INITIAL);
  }

  public static ZonedDateTime nextOccurrence(int index, ZonedDateTime now, LocationPoint point) {
    if (!isPrayer(index)) throw new IllegalArgumentException("Not a prayer index");
    ZonedDateTime local = now.withZoneSameInstant(point.zone);
    Day day = forDate(local.toLocalDate(), point);
    ZonedDateTime event =
        day.at(index) == null ? null : day.date.atTime(day.at(index)).atZone(point.zone);
    if (event == null || !event.isAfter(local)) {
      day = forDate(day.date.plusDays(1), point);
      event = day.at(index) == null ? null : day.date.atTime(day.at(index)).atZone(point.zone);
    }
    return event;
  }

  private static LocalTime localTime(
      double utcMinutes, int precautionMinutes, LocalDate date, LocationPoint point) {
    if (!Double.isFinite(utcMinutes)) return null;
    long seconds = ((long) Math.ceil(utcMinutes - 1e-8) + precautionMinutes) * 60;
    return date.atStartOfDay(java.time.ZoneOffset.UTC)
        .toInstant()
        .plusSeconds(seconds)
        .atZone(point.zone)
        .toLocalTime();
  }

  private static double solarEvent(
      double julian, LocationPoint point, double altitude, boolean evening, boolean asr) {
    double utc = 720 - 4 * point.longitude + (evening ? 360 : -360);
    for (int iteration = 0; iteration < 4; iteration++) {
      double[] position = solar(julian + utc / 1440);
      double declination = position[0];
      // A geometric shadow-ratio Asr needs the solar centre above the horizon.
      if(asr&&Math.abs(point.latitude-declination)>=90)return Double.NaN;
      double target =
          asr
              ? degrees(
                  Math.atan(1 / (1 + Math.tan(radians(Math.abs(point.latitude - declination))))))
              : altitude;
      double cosine =
          (Math.sin(radians(target))
                  - Math.sin(radians(point.latitude)) * Math.sin(radians(declination)))
              / (Math.cos(radians(point.latitude)) * Math.cos(radians(declination)));
      if (!Double.isFinite(cosine) || Math.abs(cosine) > 1) return Double.NaN;
      double angle = degrees(Math.acos(cosine));
      utc = 720 - 4 * point.longitude - position[1] + (evening ? 4 : -4) * angle;
    }
    return utc;
  }

  /** NOAA solar equations: apparent declination (degrees) and equation of time (minutes). */
  private static double[] solar(double julian) {
    double t = (julian - 2451545) / 36525;
    double longitude = ((280.46646 + t * (36000.76983 + t * 0.0003032)) % 360 + 360) % 360;
    double anomaly = 357.52911 + t * (35999.05029 - 0.0001537 * t);
    double eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
    double center =
        Math.sin(radians(anomaly)) * (1.914602 - t * (0.004817 + 0.000014 * t))
            + Math.sin(radians(2 * anomaly)) * (0.019993 - 0.000101 * t)
            + Math.sin(radians(3 * anomaly)) * 0.000289;
    double omega = 125.04 - 1934.136 * t;
    double apparent = longitude + center - 0.00569 - 0.00478 * Math.sin(radians(omega));
    double obliquity = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60;
    obliquity += 0.00256 * Math.cos(radians(omega));
    double declination =
        degrees(Math.asin(Math.sin(radians(obliquity)) * Math.sin(radians(apparent))));
    double y = Math.pow(Math.tan(radians(obliquity) / 2), 2);
    double equation =
        4
            * degrees(
                y * Math.sin(2 * radians(longitude))
                    - 2 * eccentricity * Math.sin(radians(anomaly))
                    + 4
                        * eccentricity
                        * y
                        * Math.sin(radians(anomaly))
                        * Math.cos(2 * radians(longitude))
                    - 0.5 * y * y * Math.sin(4 * radians(longitude))
                    - 1.25 * eccentricity * eccentricity * Math.sin(2 * radians(anomaly)));
    return new double[] {declination, equation};
  }

  private static double radians(double value) {
    return Math.toRadians(value);
  }

  private static double degrees(double value) {
    return Math.toDegrees(value);
  }
}
