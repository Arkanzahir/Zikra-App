package com.zikra.app.reminders;

public enum PrayerMode {
  ADHAN, NOTIFICATION, VIBRATE, OFF;

  public static PrayerMode stored(String value, boolean legacyEnabled) {
    if (!legacyEnabled) return OFF;
    if (value != null) {
      try { return valueOf(value); }
      catch (IllegalArgumentException unknown) { /* Never infer audible playback from invalid data. */ }
    }
    return NOTIFICATION;
  }
}
