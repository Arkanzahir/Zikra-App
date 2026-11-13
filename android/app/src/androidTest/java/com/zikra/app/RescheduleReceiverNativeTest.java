package com.zikra.app;

import android.content.*;
import android.test.InstrumentationTestCase;
import com.zikra.app.reminders.PrayerRescheduleReceiver;

@SuppressWarnings("deprecation")
public final class RescheduleReceiverNativeTest extends InstrumentationTestCase {
  public void testUnrelatedBroadcastCannotTouchSchedulingState() {
    Context rejected = new ContextWrapper(null) {
      @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        throw new AssertionError("Unexpected scheduling preference access");
      }
      @Override public Object getSystemService(String name) {
        throw new AssertionError("Unexpected notification/alarm service access");
      }
    };
    PrayerRescheduleReceiver receiver = new PrayerRescheduleReceiver();
    receiver.onReceive(rejected, null);
    receiver.onReceive(rejected, new Intent());
    receiver.onReceive(rejected, new Intent("zikra.TEST"));
    receiver.onReceive(rejected, new Intent("android.intent.action.PACKAGE_REPLACED"));
  }
}
