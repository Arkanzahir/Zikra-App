package com.zikra.app.safety;

import static org.junit.Assert.*;

import org.junit.Test;

public final class WeatherCodeTest {
  @Test
  public void cacheAgeBoundariesAndClockRollback() {
    long now = 100000000;
    assertTrue(WeatherCode.usableCache(now - 600000, now, 600000));
    assertFalse(WeatherCode.usableCache(now - 600001, now, 600000));
    assertTrue(WeatherCode.usableCache(now - 21600000, now, 21600000));
    assertFalse(WeatherCode.usableCache(now - 21600001, now, 21600000));
    assertFalse(WeatherCode.usableCache(now + 1, now, 21600000));
    assertFalse(WeatherCode.usableCache(0, now, 21600000));
  }

  @Test
  public void codesAndWindDirectionsAreExplicit() {
    assertEquals("Cerah", WeatherCode.description(0));
    assertEquals("Hujan petir", WeatherCode.description(95));
    assertEquals("Hujan lebat", WeatherCode.description(65));
    assertEquals("Utara", WeatherCode.direction(360));
    assertEquals("Barat", WeatherCode.direction(270));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsUnknownWeatherCode() {
    WeatherCode.description(1000);
  }
}
