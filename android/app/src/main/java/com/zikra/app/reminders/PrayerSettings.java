package com.zikra.app.reminders;

import android.content.Context;
import android.content.SharedPreferences;
import com.zikra.app.PrayerSchedule;

public final class PrayerSettings {
  public static final String PAUSE_FOCUS = "prayer_pause_focus", QUIET_CLASSES = "prayer_quiet_classes";
  private PrayerSettings() {}
  public static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences("zikra_preferences", Context.MODE_PRIVATE);
  }
  public static PrayerMode mode(Context context, int index) {
    if (!PrayerSchedule.isPrayer(index)) return PrayerMode.OFF;
    SharedPreferences p=prefs(context);String key=PrayerSchedule.KEYS[index];
    return PrayerMode.stored(p.getString("prayer_mode_"+key,null),p.getBoolean("reminder_"+key,true));
  }
  public static void setMode(Context context, int index, PrayerMode mode) {
    if (!PrayerSchedule.isPrayer(index) || mode == null) throw new IllegalArgumentException("Invalid prayer mode");
    SharedPreferences p=prefs(context);String key=PrayerSchedule.KEYS[index];
    p.edit().putString("prayer_mode_"+key,mode.name()).putBoolean("reminder_"+key,mode!=PrayerMode.OFF)
        .putLong("prayer_revision",revision(context)+1).commit();
  }
  public static long revision(Context context) { return prefs(context).getLong("prayer_revision",0); }
}
