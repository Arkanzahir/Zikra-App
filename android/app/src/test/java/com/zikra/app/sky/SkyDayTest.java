package com.zikra.app.sky;

import static org.junit.Assert.*;

import java.time.*;
import org.junit.Test;

public final class SkyDayTest {
  private SkyDay day() {
    return new SkyDay(
        LocalDate.of(2026, 10, 18),
        LocalTime.of(5, 6),
        LocalTime.of(11, 14),
        LocalTime.of(17, 22),
        .46,
        true,
        true);
  }

  @Test
  public void realSolarNoonIsMidArcAndEndpointsAreClamped() {
    SkyDay d = day();
    assertEquals(.5, d.sunProgress(d.noon), .0001);
    assertEquals(0, d.sunProgress(LocalTime.of(3, 0)), .0001);
    assertEquals(1, d.sunProgress(LocalTime.of(20, 0)), .0001);
    assertTrue(d.sunProgress(LocalTime.of(13, 0)) > .5);
    assertFalse(d.daytime(d.set));
    assertTrue(d.daytime(d.rise));
  }

  @Test
  public void fallbackMoonChangesByAstronomicalDate() {
    SkyDay full = SkyDay.estimate(LocalDate.of(2024, 1, 25));
    SkyDay next = SkyDay.estimate(LocalDate.of(2024, 2, 9));
    assertTrue(full.illumination > .98);
    assertTrue(next.illumination < .02);
    assertFalse(full.usno);
    assertTrue(full.rise.isBefore(full.noon));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsInvertedSolarEvents() {
    new SkyDay(
        LocalDate.now(), LocalTime.NOON, LocalTime.of(5, 0), LocalTime.of(18, 0), .5, true, true);
  }
}
