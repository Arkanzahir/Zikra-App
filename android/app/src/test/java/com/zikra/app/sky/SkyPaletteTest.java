package com.zikra.app.sky;

import static org.junit.Assert.*;
import java.time.*;
import org.junit.Test;

public final class SkyPaletteTest {
  private SkyDay day(int rise, int set) {
    return new SkyDay(LocalDate.of(2026,10,25), LocalTime.of(rise,0),
        LocalTime.of((rise + set) / 2,0), LocalTime.of(set,0), .5, true, false);
  }

  @Test public void daylightUsesLocalSolarEventsAndTwilightIsBounded() {
    SkyDay day = day(6,18);
    assertEquals(0, SkyPalette.daylight(day, LocalTime.of(4,0)), .001);
    assertEquals(.5, SkyPalette.daylight(day, LocalTime.of(6,0)), .001);
    assertEquals(1, SkyPalette.daylight(day, LocalTime.NOON), .001);
    assertEquals(.5, SkyPalette.daylight(day, LocalTime.of(18,0)), .001);
    assertEquals(0, SkyPalette.daylight(day, LocalTime.of(20,0)), .001);
    assertEquals(1, SkyPalette.twilight(day, LocalTime.of(6,0)), .001);
    assertEquals(0, SkyPalette.twilight(day, LocalTime.NOON), .001);
    assertTrue(SkyPalette.daylight(day(8,20), LocalTime.of(7,0))
        < SkyPalette.daylight(day, LocalTime.of(7,0)));
  }

  @Test public void nightIsDarkerAndSunriseHasNoAbruptColorStep() {
    SkyDay day = day(6,18);
    int noon = SkyPalette.top(day, LocalTime.NOON), night = SkyPalette.top(day, LocalTime.MIDNIGHT);
    assertTrue((noon & 255) > (night & 255));
    int before = SkyPalette.horizon(day, LocalTime.of(5,59,59));
    int after = SkyPalette.horizon(day, LocalTime.of(6,0,1));
    for (int shift : new int[] {0,8,16})
      assertTrue(Math.abs((before >> shift & 255) - (after >> shift & 255)) <= 1);
    for (int hour = 0; hour < 24; hour++) {
      float value = SkyPalette.daylight(day, LocalTime.of(hour,0));
      assertTrue(value >= 0 && value <= 1);
    }
  }

  @Test public void celestialVisibilityFollowsSelectedLocalEvents() {
    SkyDay early = day(6,18), late = day(8,20);
    assertEquals(0, SkyPalette.sunVisibility(early, LocalTime.MIDNIGHT), .001);
    assertEquals(1, SkyPalette.sunVisibility(early, LocalTime.NOON), .001);
    assertEquals(0, SkyPalette.sunVisibility(early, LocalTime.of(19,0)), .001);
    assertEquals(1, SkyPalette.nightVisibility(early, LocalTime.of(23,0)), .001);
    assertEquals(0, SkyPalette.nightVisibility(early, LocalTime.NOON), .001);
    assertTrue(SkyPalette.sunVisibility(early, LocalTime.of(7,0))
        > SkyPalette.sunVisibility(late, LocalTime.of(7,0)));
    assertTrue(SkyPalette.nightVisibility(early, LocalTime.of(19,0))
        > SkyPalette.nightVisibility(late, LocalTime.of(19,0)));
  }
}
