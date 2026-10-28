package com.zikra.app.prayer;

import com.zikra.app.PrayerSchedule;
import com.zikra.app.location.LocationPoint;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** A complete validated provider month; importing it never adds another ihtiyat adjustment. */
public final class PrayerMonth {
  public static final String SOURCE = "kemenag_myquran";
  public final PrayerRegion region;
  public final YearMonth month;
  public final String province;
  private final Map<LocalDate, PrayerSchedule.Day> days;

  private PrayerMonth(PrayerRegion region, YearMonth month, String province,
      Map<LocalDate, PrayerSchedule.Day> days) {
    this.region = region;
    this.month = month;
    this.province = province;
    this.days = Collections.unmodifiableMap(days);
  }

  public PrayerSchedule.Day day(LocalDate date) { return days.get(date); }

  public boolean supports(LocationPoint point) {
    if (!PrayerRegion.indonesianPoint(point)) return false;
    int hours;
    if (province.contains("PAPUA") || province.contains("MALUKU")) hours = 9;
    else if (province.contains("SULAWESI") || province.contains("NUSA TENGGARA")
        || province.equals("BALI") || province.equals("KALIMANTAN SELATAN")
        || province.equals("KALIMANTAN TIMUR") || province.equals("KALIMANTAN UTARA")) hours = 8;
    else if (province.contains("JAWA") || province.contains("SUMATERA")
        || province.contains("JAKARTA") || province.contains("YOGYAKARTA")
        || province.equals("ACEH") || province.equals("BANTEN")
        || province.contains("RIAU") || province.equals("LAMPUNG") || province.equals("JAMBI")
        || province.equals("BENGKULU") || province.contains("BANGKA")
        || province.equals("KALIMANTAN BARAT") || province.equals("KALIMANTAN TENGAH")) hours = 7;
    else return false;
    return point.zone.getRules().getOffset(month.atDay(1).atStartOfDay(point.zone).toInstant())
        .getTotalSeconds() == hours * 3600;
  }

  public static PrayerMonth parse(String text, PrayerRegion expected, YearMonth month) throws Exception {
    if (text.length() > 131072) throw new IllegalArgumentException("Month size");
    JSONObject root = new JSONObject(text);
    if (!root.getBoolean("status")) throw new IllegalArgumentException("Month status");
    JSONObject data = root.getJSONObject("data");
    if (!expected.id.equals(data.getString("id")) || !expected.name.equals(data.getString("kabko")))
      throw new IllegalArgumentException("Region mismatch");
    String province = data.getString("prov");
    if (!province.matches("[A-Z .'-]{3,80}")) throw new IllegalArgumentException("Province");
    JSONObject rows = data.getJSONObject("jadwal");
    if (rows.length() != month.lengthOfMonth()) throw new IllegalArgumentException("Incomplete month");
    Map<LocalDate, PrayerSchedule.Day> days = new LinkedHashMap<>();
    String[] fields = {"subuh", "terbit", "dzuhur", "ashar", "maghrib", "isya"};
    for (int n = 1; n <= month.lengthOfMonth(); n++) {
      LocalDate date = month.atDay(n);
      JSONObject row = rows.getJSONObject(date.toString());
      LocalTime[] times = new LocalTime[6];
      for (int i = 0; i < times.length; i++) times[i] = time(row.getString(fields[i]));
      if (!time(row.getString("imsak")).isBefore(times[0])
          || !time(row.getString("dhuha")).isAfter(times[1])
          || !time(row.getString("dhuha")).isBefore(times[2]))
        throw new IllegalArgumentException("Daily order");
      days.put(date, PrayerSchedule.fromTimetable(date, times, SOURCE));
    }
    return new PrayerMonth(expected, month, province, days);
  }

  private static LocalTime time(String text) {
    if (!text.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))
      throw new IllegalArgumentException("Timetable time");
    return LocalTime.parse(text);
  }
}
