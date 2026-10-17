package com.zikra.app.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class PrayerRescheduleReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    PrayerReminders.sync(context);
  }
}
