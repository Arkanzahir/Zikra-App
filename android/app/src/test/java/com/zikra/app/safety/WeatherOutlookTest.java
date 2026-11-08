package com.zikra.app.safety;

import static org.junit.Assert.*;

import com.zikra.app.location.LocationPoint;
import java.time.*;
import java.util.*;
import org.junit.Test;

public final class WeatherOutlookTest {
  private BmkgParser.Weather slot(String time, String temperature) {
    return new BmkgParser.Weather(
        "", "", "", temperature, "Berawan", "72", "12", Instant.parse(time), false);
  }

  private BmkgParser.Forecast forecast(BmkgParser.Weather current, BmkgParser.Weather... slots) {
    return new BmkgParser.Forecast(
        current,
        Arrays.asList(slots),
        "Open-Meteo",
        current.time,
        false,
        false,
        LocationPoint.INITIAL);
  }

  @Test
  public void nextHoursExcludeCurrentAndPreserveMidnight() {
    BmkgParser.Weather current = slot("2026-11-08T16:00:00Z", "29"),
        next = slot("2026-11-08T17:00:00Z", "27");
    assertEquals(
        Collections.singletonList(next),
        WeatherOutlook.upcoming(forecast(current, current, next), 4));
    assertTrue(WeatherOutlook.upcoming(forecast(current, current), 4).isEmpty());
    assertTrue(WeatherOutlook.upcoming(forecast(current, current, next), 0).isEmpty());
  }

  @Test
  public void daysFollowLocationMidnightAndKeepPartialRange() {
    BmkgParser.Weather first = slot("2026-11-08T16:00:00Z", "29"),
        midnight = slot("2026-11-08T17:00:00Z", "25");
    BmkgParser.Weather noon = slot("2026-11-09T05:00:00Z", "33");
    List<WeatherOutlook.Day> days = WeatherOutlook.days(forecast(first, first, midnight, noon));
    assertEquals(2, days.size());
    assertEquals(LocalDate.of(2026, 11, 8), days.get(0).date);
    assertEquals(LocalDate.of(2026, 11, 9), days.get(1).date);
    assertEquals(25, days.get(1).minimum, 0);
    assertEquals(33, days.get(1).maximum, 0);
    assertEquals(noon, days.get(1).representative);
    assertEquals(2, days.get(1).samples);
  }

  @Test
  public void missingInvalidAndOutOfRangeDaysAreNotInvented() {
    BmkgParser.Weather current = slot("2026-11-08T05:00:00Z", "30");
    List<WeatherOutlook.Day> days =
        WeatherOutlook.days(
            forecast(
                current,
                slot("2026-11-07T05:00:00Z", "26"),
                current,
                slot("2026-11-09T05:00:00Z", "NaN"),
                slot("2026-11-10T05:00:00Z", "100"),
                slot("2026-11-11T05:00:00Z", "32")));
    assertEquals(1, days.size());
    assertEquals(30, days.get(0).minimum, 0);
    assertTrue(WeatherOutlook.days(forecast(current)).isEmpty());
  }

  @Test
  public void skyUsesLocalHourInsteadOfAppTheme() {
    Instant time = Instant.parse("2026-11-08T11:00:00Z");
    assertTrue(WeatherOutlook.night(time, ZoneId.of("Asia/Jakarta")));
    assertFalse(WeatherOutlook.night(time, ZoneId.of("UTC")));
    assertFalse(
        WeatherOutlook.night(Instant.parse("2026-11-08T00:00:00Z"), ZoneId.of("Asia/Jakarta")));
  }
}
