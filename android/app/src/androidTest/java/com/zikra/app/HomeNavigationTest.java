package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import java.util.concurrent.atomic.AtomicBoolean;

/** Verifies that the replacement shortcut and the Hijri date open working native pages. */
@SuppressWarnings("deprecation")
public final class HomeNavigationTest extends InstrumentationTestCase {
  private HomeActivity home;
  private Activity destination;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .commit();
    home =
        (HomeActivity)
            getInstrumentation()
                .startActivitySync(
                    new Intent(getInstrumentation().getTargetContext(), HomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    getInstrumentation().waitForIdleSync();
    awaitWindowFocus(home);
  }

  public void testHadithShortcutOpensSearchableHadithPage() {
    openFromHome(R.id.shortcutHadith, HadithActivity.class);
    assertNotNull(destination.findViewById(R.id.hadithSearch));
    assertNotNull(destination.findViewById(R.id.hadithList));
  }

  public void testHijriDateOpensInteractiveCalendar() {
    openFromHome(R.id.homeHijri, CalendarActivity.class);
    assertNotNull(destination.findViewById(R.id.calendarMonth));
    assertTrue(destination.findViewById(R.id.calendarNext).isEnabled());
  }

  private void openFromHome(int id, Class<? extends Activity> target) {
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(target.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(() -> home.findViewById(id).performClick());
      destination = getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull("Destination did not open: " + target.getSimpleName(), destination);
      getInstrumentation().waitForIdleSync();
      awaitWindowFocus(destination);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  private void awaitWindowFocus(Activity activity) {
    long deadline = SystemClock.uptimeMillis() + 20000;
    AtomicBoolean focused = new AtomicBoolean();
    while (SystemClock.uptimeMillis() < deadline) {
      getInstrumentation().runOnMainSync(() -> focused.set(activity.hasWindowFocus()));
      if (focused.get()) return;
      SystemClock.sleep(100);
    }
    fail("Window did not receive focus: " + activity.getClass().getSimpleName());
  }

  @Override
  protected void tearDown() throws Exception {
    if (destination != null) {
      getInstrumentation().runOnMainSync(destination::finish);
      getInstrumentation().waitForIdleSync();
      awaitWindowFocus(home);
    }
    if (home != null) getInstrumentation().runOnMainSync(home::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
