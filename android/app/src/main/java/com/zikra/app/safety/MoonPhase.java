package com.zikra.app.safety;

import java.time.Instant;

/** Decorative mean lunar age; not a hilal visibility or religious calendar calculation. */
public final class MoonPhase {
  private static final Instant NEW_MOON = Instant.parse("2000-01-06T18:14:00Z");
  private static final double PERIOD = 29.530588853 * 86400;

  private MoonPhase() {}

  public static double cycle(Instant now) {
    double age = (now.getEpochSecond() - NEW_MOON.getEpochSecond()) / PERIOD;
    return age - Math.floor(age);
  }

  public static double illumination(Instant now) {
    return (1 - Math.cos(2 * Math.PI * cycle(now))) / 2;
  }
}
