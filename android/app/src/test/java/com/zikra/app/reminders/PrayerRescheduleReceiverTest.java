package com.zikra.app.reminders;

import org.junit.Test;
import static org.junit.Assert.*;

public final class PrayerRescheduleReceiverTest {
  @Test public void rejectsMissingAndUnrelatedActions() {
    for (String action : new String[]{null, "", "android.intent.action.PACKAGE_REPLACED", "zikra.TEST", "android.intent.action.BOOT_COMPLETED.extra"})
      assertFalse(PrayerRescheduleReceiver.supportedAction(action));
  }
  @Test public void acceptsDeclaredSystemEvents() {
    for (String action : new String[]{"android.intent.action.BOOT_COMPLETED", "android.intent.action.TIME_SET",
        "android.intent.action.TIMEZONE_CHANGED", "android.intent.action.MY_PACKAGE_REPLACED",
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"})
      assertTrue(PrayerRescheduleReceiver.supportedAction(action));
  }
}
