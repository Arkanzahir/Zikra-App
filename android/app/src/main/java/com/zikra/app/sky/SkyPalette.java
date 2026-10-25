package com.zikra.app.sky;

import java.time.LocalTime;

/** Clock/location-driven illustration colors; not an observed weather report. */
public final class SkyPalette {
  private SkyPalette() {}

  public static float daylight(SkyDay day, LocalTime time) {
    int second = time.toSecondOfDay();
    float sinceRise = (second - day.rise.toSecondOfDay()) / 1800f;
    float untilSet = (day.set.toSecondOfDay() - second) / 1800f;
    return clamp(Math.min(sinceRise, untilSet) + .5f);
  }

  public static float twilight(SkyDay day, LocalTime time) {
    int second = time.toSecondOfDay();
    int distance = Math.min(Math.abs(second - day.rise.toSecondOfDay()),
        Math.abs(second - day.set.toSecondOfDay()));
    return clamp(1f - distance / 3600f);
  }

  public static int top(SkyDay day, LocalTime time) {
    return blend(0xFF071F2D, 0xFF409AC0, daylight(day, time));
  }

  public static int horizon(SkyDay day, LocalTime time) {
    int base = blend(0xFF153A46, 0xFFB0D4EB, daylight(day, time));
    return blend(base, 0xFFD5B075, twilight(day, time) * .85f);
  }

  public static int cloud(SkyDay day, LocalTime time) {
    int base = blend(0xFF57737F, 0xFFF0F5ED, daylight(day, time));
    return blend(base, 0xFFE8C189, twilight(day, time) * .75f);
  }

  private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }

  private static int blend(int a, int b, float t) {
    int red = Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
    int green = Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
    int blue = Math.round((a & 255) * (1 - t) + (b & 255) * t);
    return 0xFF000000 | red << 16 | green << 8 | blue;
  }
}
