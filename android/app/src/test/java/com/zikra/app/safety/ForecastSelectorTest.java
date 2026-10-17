package com.zikra.app.safety;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class ForecastSelectorTest {
  @Test
  public void selectsNearestUpcomingForecastFromUnsortedResponse() {
    Instant now = Instant.parse("2026-10-15T04:00:00Z");
    assertEquals(
        2,
        ForecastSelector.select(
            Arrays.asList(
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T00:00:00Z"),
                Instant.parse("2026-10-15T06:00:00Z")),
            now));
  }

  @Test
  public void expiredResponseUsesLatestAvailableTime() {
    Instant now = Instant.parse("2026-10-15T18:00:00Z");
    assertEquals(
        1,
        ForecastSelector.select(
            Arrays.asList(
                Instant.parse("2026-10-15T06:00:00Z"), Instant.parse("2026-10-15T12:00:00Z"), null),
            now));
  }

  @Test
  public void emptyOrInvalidResponseHasNoForecast() {
    assertEquals(-1, ForecastSelector.select(Collections.emptyList(), Instant.now()));
    assertEquals(-1, ForecastSelector.select(Arrays.asList(null, null), Instant.now()));
  }

  @Test
  public void parsesBmkgUtcAndWibWithoutApplyingDeviceTimezone() {
    Instant expected = Instant.parse("2026-10-15T06:00:00Z");
    assertEquals(expected, ForecastSelector.parseTimestamp("2026-10-15 06:00:00", false));
    assertEquals(expected, ForecastSelector.parseTimestamp("2026-10-15 13:00:00", true));
    assertEquals(expected, ForecastSelector.parseTimestamp("2026-10-15T06:00:00Z", false));
    assertEquals(expected, ForecastSelector.parseTimestamp("2026-10-15T06:00:00+00:00", false));
    assertEquals(expected, ForecastSelector.parseTimestamp("2026-10-15T13:00:00+07:00", true));
    assertNull(ForecastSelector.parseTimestamp("not-a-date", false));
    assertNull(ForecastSelector.parseTimestamp("", false));
  }
}
