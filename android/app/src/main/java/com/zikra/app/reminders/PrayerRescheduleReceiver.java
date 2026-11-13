package com.zikra.app.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class PrayerRescheduleReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent == null || !supportedAction(intent.getAction())) return;
    PrayerReminders.sync(context);
    com.zikra.app.study.StudyReminders.sync(context);
    com.zikra.app.study.FocusTimer.restore(context);
  }

  static boolean supportedAction(String action) {
    return Intent.ACTION_BOOT_COMPLETED.equals(action)
        || Intent.ACTION_TIME_CHANGED.equals(action)
        || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
        || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
        || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action);
  }
}
