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
    int base = blend(0xFF0A1931, 0xFF589DBD, daylight(day, time));
    return blend(base, 0xFF5B638B, twilight(day, time) * .55f);
  }

  public static int horizon(SkyDay day, LocalTime time) {
    int base = blend(0xFF1E314C, 0xFFBAD9E3, daylight(day, time));
    return blend(base, 0xFFF1AA74, twilight(day, time) * .85f);
  }

  public static float sunVisibility(SkyDay day, LocalTime time) {
    int second = time.toSecondOfDay();
    return clamp((second - day.rise.toSecondOfDay() + 420) / 600f)
        * clamp((day.set.toSecondOfDay() - second + 420) / 600f);
  }

  public static float nightVisibility(SkyDay day, LocalTime time) {
    int second = time.toSecondOfDay();
    return Math.max(clamp((day.rise.toSecondOfDay() - second - 720) / 3480f),
        clamp((second - day.set.toSecondOfDay() - 720) / 3480f));
  }

  /** Facade lights fade only outside the selected location's daylight hours. */
  public static float lampVisibility(SkyDay day, LocalTime time) {
    if (day.daytime(time)) return 0;
    int second=time.toSecondOfDay();
    int sinceSet=(second-day.set.toSecondOfDay()+86400)%86400;
    int untilRise=(day.rise.toSecondOfDay()-second+86400)%86400;
    return clamp(Math.min(sinceSet,untilRise)/1200f);
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
