package com.zikra.app.study;

import android.content.*;

public final class FocusAlarmReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (com.zikra.app.migration.LocalMigration.blocked()) return;
    if (intent != null) FocusTimer.deliver(context, intent);
  }
}
