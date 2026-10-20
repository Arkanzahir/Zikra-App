package com.zikra.app.study;

import android.content.*;

public final class StudyAlarmReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent != null) StudyReminders.deliver(context, intent);
  }
}
