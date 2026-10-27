package com.zikra.app.reminders;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.HomeActivity;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.study.StudyAlarmPolicy;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

/** Host performs real reboot/forced idle between methods; never invokes BOOT receiver itself. */
@SuppressWarnings("deprecation")
public final class SystemAlarmLifecycleTest extends InstrumentationTestCase {
  private Context context;
  private static final PrayerMode[] MODES = {
    PrayerMode.ADHAN,
    PrayerMode.OFF,
    PrayerMode.NOTIFICATION,
    PrayerMode.VIBRATE,
    PrayerMode.OFF,
    PrayerMode.NOTIFICATION
  };

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue(
        "Host must isolate original emulator data",
        new File(context.getFilesDir(), "d02-system-authorized").isFile());
    assertTrue(PrayerReminders.canNotify(context));
  }

  private void report(String name, JSONObject value) throws Exception {
    Files.write(
        new File(context.getFilesDir(), name).toPath(),
        value.toString(2).getBytes(StandardCharsets.UTF_8));
  }

  private Set<String> ids() {
    return new HashSet<>(
        context
            .getSharedPreferences(PrayerReminders.PLAN, 0)
            .getStringSet("ids", Collections.emptySet()));
  }

  private void assertModes() {
    assertTrue(PrayerReminders.enabled(context));
    for (int i = 0; i < MODES.length; i++)
      if (PrayerSchedule.isPrayer(i)) assertEquals(MODES[i], PrayerSettings.mode(context, i));
  }

  public void testArmBeforeRealReboot() throws Exception {
    PrayerSettings.prefs(context)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .putBoolean("reminder_enabled", true)
        .commit();
    LocationStore.save(context, LocationPoint.INITIAL);
    for (int i = 0; i < MODES.length; i++)
      if (PrayerSchedule.isPrayer(i)) PrayerSettings.setMode(context, i, MODES[i]);
    Activity home =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    getInstrumentation().runOnMainSync(home::finish);
    getInstrumentation().waitForIdleSync();
    PrayerReminders.sync(context);
    assertModes();
    assertTrue(ids().size() >= 24);
    report(
        "d02-before-reboot.json",
        new JSONObject().put("plannedSlots", ids().size()).put("allFourModesStored", true));
  }

  public void testReadAfterRealRebootWithoutResync() throws Exception {
    // No sync, Activity launch or receiver invocation: inspect only the system-restored plan.
    assertModes();
    SharedPreferences plan = context.getSharedPreferences(PrayerReminders.PLAN, 0);
    Set<String> slots = ids();
    assertTrue(slots.size() >= 24);
    for (String slot : slots) {
      int i = Integer.parseInt(slot.substring(slot.lastIndexOf('_') + 1));
      assertTrue(PrayerSchedule.isPrayer(i));
      assertTrue(PrayerSettings.mode(context, i) != PrayerMode.OFF);
      long epoch = plan.getLong("e:" + slot, 0);
      assertTrue(epoch > System.currentTimeMillis());
      assertEquals(LocationStore.current(context).key(), plan.getString("l:" + slot, ""));
      assertEquals(PrayerSettings.revision(context), plan.getLong("r:" + slot, -1));
      assertNotNull(
          PendingIntent.getBroadcast(
              context,
              0,
              PrayerReminders.alarmIntent(context, i, epoch),
              PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE));
    }
    assertFalse(AdhanService.state().active);
    report(
        "d02-after-reboot.json",
        new JSONObject()
            .put("plannedSlots", slots.size())
            .put("modesRetained", true)
            .put("pendingIdentitiesExist", true)
            .put("resyncInvokedByTest", false)
            .put("audioNotResumedAfterBoot", true));
  }

  public void testArmExactAlarmBeforeForcedIdle() throws Exception {
    assertTrue(StudyAlarmPolicy.exact(context));
    PrayerSettings.setMode(context, 2, PrayerMode.ADHAN);
    PrayerSettings.prefs(context).edit().remove("prayer_delivered_2").commit();
    PrayerReminders.sync(context);
    long epoch = System.currentTimeMillis() + 25000;
    Intent alarm = PrayerReminders.alarmIntent(context, 2, epoch);
    String slot = alarm.getStringExtra("slot");
    Set<String> next = ids();
    next.add(slot);
    context
        .getSharedPreferences(PrayerReminders.PLAN, 0)
        .edit()
        .putStringSet("ids", next)
        .putLong("e:" + slot, epoch)
        .putString("l:" + slot, LocationStore.current(context).key())
        .putLong("r:" + slot, PrayerSettings.revision(context))
        .commit();
    StudyAlarmPolicy.schedule(
        context,
        epoch,
        PendingIntent.getBroadcast(
            context, 0, alarm, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    report(
        "d02-idle-arm.json",
        new JSONObject()
            .put("scheduledEpoch", epoch)
            .put("armedAtElapsed", SystemClock.elapsedRealtime())
            .put("usesActualAlarmManager", true));
  }

  public void testCancelOwnedAlarmsBeforeRestore() {
    PrayerSettings.prefs(context).edit().putBoolean("reminder_enabled", false).commit();
    PrayerReminders.sync(context);
    getInstrumentation().runOnMainSync(() -> AdhanService.stop(context));
    getInstrumentation().waitForIdleSync();
    assertTrue(ids().isEmpty());
  }
}
