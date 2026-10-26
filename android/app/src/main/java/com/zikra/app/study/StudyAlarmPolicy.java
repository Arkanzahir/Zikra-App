package com.zikra.app.study;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

/** Exact only when Android permits it; otherwise use a documented inexact reminder. */
public final class StudyAlarmPolicy {
  private StudyAlarmPolicy() {}
  public static boolean exact(Context context) {
    return Build.VERSION.SDK_INT < 31
        || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
  }
  public static void schedule(Context context, long at, PendingIntent intent) {
    AlarmManager manager = context.getSystemService(AlarmManager.class);
    if (exact(context)) {
      try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent); return; }
      catch (SecurityException revoked) { /* Permission can change between checking and scheduling. */ }
    }
    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent);
  }
}
