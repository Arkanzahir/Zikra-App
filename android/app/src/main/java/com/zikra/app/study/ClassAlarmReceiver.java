package com.zikra.app.study;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ClassAlarmReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    if (intent != null) ClassReminders.deliver(context, intent);
  }
}
