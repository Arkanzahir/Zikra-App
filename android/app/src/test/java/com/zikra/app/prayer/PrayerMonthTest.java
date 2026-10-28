package com.zikra.app.prayer;

import static org.junit.Assert.*;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.reminders.PrayerAlarmPlan;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

public class PrayerMonthTest {
  private final PrayerRegion region = new PrayerRegion("4734ba6f3de83d861c3176a6273cac6d", "KOTA SURABAYA");
  private final YearMonth october = YearMonth.of(2026, 10);

  private String fixture(String city, String period) throws Exception {
    try (java.io.InputStream in = getClass().getResourceAsStream("/prayer/" + city + "-" + period + ".json")) {
      assertNotNull(in);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private PrayerMonth month() throws Exception { return PrayerMonth.parse(fixture("surabaya", "2026-10"), region, october); }
  @After public void clearProvider() { PrayerSchedule.setTimetable(null); }

  @Test public void bundledCatalogIncludesAllReturnedRegionsIncludingIslandExceptions() throws Exception {
    List<PrayerRegion> catalog = PrayerRegion.parse(fixture("regions", "v3"));
    assertEquals(517, catalog.size());
    assertEquals("KOTA SURABAYA", PrayerRegion.automatic(catalog, LocationPoint.INITIAL).name);
    assertTrue(catalog.stream().anyMatch(r -> r.name.equals("PULAU TAMBELAN KAB. BINTAN")));
  }

  @Test public void importedValuesStayExactAndChangeWithEachDate() throws Exception {
    PrayerMonth value = month();
    assertEquals("17:26", value.day(LocalDate.of(2026, 10, 28)).time(4));
    assertEquals("17:27", value.day(LocalDate.of(2026, 10, 29)).time(4));
    assertEquals("11:16", value.day(LocalDate.of(2026, 10, 28)).time(2));
    assertTrue(value.supports(LocationPoint.INITIAL));
    assertNull(value.day(LocalDate.of(2026, 11, 1)));
    assertFalse(value.supports(new LocationPoint(-2.53,140.71,"Asia/Jayapura","Jayapura","manual",0,-1)));
  }

  @Test public void alarmCountdownAndMonthBoundaryUseTheSameProvider() throws Exception {
    PrayerMonth value = month();
    PrayerMonth november = PrayerMonth.parse(fixture("surabaya", "2026-11"), region, october.plusMonths(1));
    PrayerSchedule.setTimetable((day,point) -> point.key().equals(LocationPoint.INITIAL.key())
        ? (YearMonth.from(day).equals(october) ? value.day(day) : november.day(day)) : null);
    ZonedDateTime now = LocalDate.of(2026,10,28).atTime(17,25).atZone(PrayerSchedule.ZONE);
    assertEquals(LocalTime.of(17,26), PrayerSchedule.nextAfter(now).time.toLocalTime());
    assertEquals(60, Duration.between(now, PrayerSchedule.nextAfter(now).time).getSeconds());
    assertEquals(now.plusMinutes(1).toInstant().toEpochMilli(),
        PrayerAlarmPlan.next(now.toInstant().toEpochMilli(), LocationPoint.INITIAL).get(0).epoch);
    ZonedDateTime last = LocalDate.of(2026,10,31).atTime(23,59).atZone(PrayerSchedule.ZONE);
    assertEquals(november.day(LocalDate.of(2026,11,1)).at(0), PrayerSchedule.nextAfter(last).time.toLocalTime());
    assertEquals("kemenag_myquran", PrayerSchedule.forDate(LocalDate.of(2026,11,1)).source);
  }

  @Test public void invalidMonthIdentityMissingDateAndMalformedTimesAreRejected() throws Exception {
    JSONObject raw = new JSONObject(fixture("surabaya", "2026-10"));
    JSONObject data = raw.getJSONObject("data");
    data.put("id", "00000000000000000000000000000000"); reject(raw);
    data.put("id", region.id);
    data.getJSONObject("jadwal").remove("2026-10-28"); reject(raw);
    raw = new JSONObject(fixture("surabaya", "2026-10"));
    raw.getJSONObject("data").getJSONObject("jadwal").getJSONObject("2026-10-28").put("maghrib", "24:00"); reject(raw);
    raw = new JSONObject(fixture("surabaya", "2026-10"));
    raw.getJSONObject("data").getJSONObject("jadwal").getJSONObject("2026-10-28").put("maghrib", "10:00"); reject(raw);
    try { PrayerMonth.parse(fixture("surabaya", "2026-10"), region, october.plusMonths(1)); fail(); }
    catch (Exception expected) { }
  }

  private void reject(JSONObject raw) {
    try { PrayerMonth.parse(raw.toString(), region, october); fail("Invalid timetable accepted"); }
    catch (Exception expected) { }
  }

  @Test public void everyLiveFixtureDayParsesInThreeZones() throws Exception {
    String[][] cities = {{"surabaya", "KOTA SURABAYA", "4734ba6f3de83d861c3176a6273cac6d"},
        {"makassar", "KOTA MAKASSAR", ""}, {"jayapura", "KOTA JAYAPURA", ""}};
    for (String[] city : cities) for (String period : new String[]{"2026-10","2026-11"}) {
      String text = fixture(city[0], period);
      PrayerRegion expected = new PrayerRegion(new JSONObject(text).getJSONObject("data").getString("id"), city[1]);
      PrayerMonth table = PrayerMonth.parse(text, expected, YearMonth.parse(period));
      assertNotNull(table.day(YearMonth.parse(period).atEndOfMonth()));
    }
  }

  @Test public void ambiguousRegionAndUnknownGpsNeverGuessNearestCity() {
    List<PrayerRegion> regions = Arrays.asList(region,
        new PrayerRegion("11111111111111111111111111111111", "KOTA BOGOR"),
        new PrayerRegion("22222222222222222222222222222222", "KAB. BOGOR"));
    assertSame(region, PrayerRegion.automatic(regions, LocationPoint.INITIAL));
    assertNull(PrayerRegion.automatic(regions, new LocationPoint(-6.6,106.8,"Asia/Jakarta","Bogor","manual",0,-1)));
    assertNull(PrayerRegion.automatic(regions, new LocationPoint(-7.28,112.79,"Asia/Jakarta","Lokasi perangkat","device",0,100)));
  }
}
