package com.zikra.app.reading;

/** Circular calculations independent of Android, allowing ordinary JVM unit tests. */
public final class CompassMath {
  private CompassMath() {}

  public static float normalize(float degrees) {
    float normalized = degrees % 360f;
    return normalized < 0 ? normalized + 360f : normalized;
  }

  /** Signed smallest turn from the current heading to the target, in [-180, 180). */
  public static float delta(float target, float current) {
    return normalize(target - current + 180f) - 180f;
  }

  public static float smooth(float previous, float measured, float factor) {
    return normalize(previous + delta(measured, previous) * factor);
  }

  public static boolean aligned(float heading, float target, float tolerance) {
    return Math.abs(delta(target, heading)) <= tolerance;
  }
}
