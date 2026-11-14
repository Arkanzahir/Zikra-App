package com.zikra.app.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class PrayerAlarmReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (com.zikra.app.migration.LocalMigration.blocked()) return;
    PrayerReminders.deliver(context, intent);
  }
}
