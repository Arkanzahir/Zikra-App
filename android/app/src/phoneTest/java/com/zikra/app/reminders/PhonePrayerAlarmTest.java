package com.zikra.app.reminders;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.test.InstrumentationTestCase;
import com.zikra.app.PrayerActivity;
import com.zikra.app.location.LocationStore;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/** Real physical alarm delivery; temporarily changed settings are restored in finally. */
@SuppressWarnings("deprecation")
public final class PhonePrayerAlarmTest extends InstrumentationTestCase {
  private Context context;

  private String shell(String command) throws Exception {
    try (InputStream in =
        new ParcelFileDescriptor.AutoCloseInputStream(
            getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
    }
  }

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }

  private void write(String name, JSONObject result) throws Exception {
    Files.write(
        new File(context.getFilesDir(), name).toPath(),
        result.toString(2).getBytes(StandardCharsets.UTF_8));
  }

  private void restore(SharedPreferences prefs, Map<String, ?> original) {
    SharedPreferences.Editor edit = prefs.edit().clear();
    for (Map.Entry<String, ?> entry : original.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof String) edit.putString(key, (String) value);
      else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
      else if (value instanceof Integer) edit.putInt(key, (Integer) value);
      else if (value instanceof Long) edit.putLong(key, (Long) value);
      else if (value instanceof Float) edit.putFloat(key, (Float) value);
      else if (value instanceof Set) edit.putStringSet(key, new HashSet<>((Set<String>) value));
      else throw new IllegalStateException("Unsupported preference type");
    }
    assertTrue(edit.commit());
  }

  public void testRealAlarmWithScreenOffAndUserNotificationStop() throws Exception {
    context = getInstrumentation().getTargetContext();
    assertFalse("Physical phone required", "1".equals(shell("getprop ro.kernel.qemu")));
    assertTrue(new File(context.getFilesDir(), "phone-d02-backup-verified").isFile());
    assertTrue("Notification permission required", PrayerReminders.canNotify(context));
    assertTrue("Exact alarm capability required", PrayerReminders.exactAllowed(context));
    assertTrue(
        "Existing sound policy must allow audio", PrayerAudioPolicy.deviceAllowsSound(context));
    assertFalse("Do not interrupt existing Zikra audio", AdhanService.state().active);
    SharedPreferences prefs = PrayerSettings.prefs(context);
    SharedPreferences plan = context.getSharedPreferences(PrayerReminders.PLAN, 0);
    Map<String, ?> original = new HashMap<>(prefs.getAll());
    Map<String, ?> originalPlan = new HashMap<>(plan.getAll());
    Activity page = null;
    PendingIntent operation = null;
    JSONObject proof = new JSONObject();
    proof.put("physicalPhone", true);
    proof.put("usesRealAlarmManager", true);
    proof.put("mockTimeOrBroadcastUsed", false);
    proof.put("passed", false);
    try {
      page =
          getInstrumentation()
              .startActivitySync(
                  new Intent(context, PrayerActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      getInstrumentation().waitForIdleSync();
      main(
          () -> {
            prefs
                .edit()
                .putBoolean("reminder_enabled", true)
                .putBoolean(PrayerSettings.PAUSE_FOCUS, false)
                .putBoolean(PrayerSettings.QUIET_CLASSES, false)
                .commit();
            PrayerSettings.setMode(context, 2, PrayerMode.ADHAN);
            PrayerReminders.sync(context);
          });
      long epoch = System.currentTimeMillis() + 30000;
      Intent event = PrayerReminders.alarmIntent(context, 2, epoch);
      String slot = event.getStringExtra("slot");
      Set<String> ids = new HashSet<>(plan.getStringSet("ids", java.util.Collections.emptySet()));
      ids.add(slot);
      plan.edit()
          .putStringSet("ids", ids)
          .putLong("e:" + slot, epoch)
          .putString("l:" + slot, LocationStore.current(context).key())
          .putLong("r:" + slot, PrayerSettings.revision(context))
          .commit();
      operation =
          PendingIntent.getBroadcast(
              context, 0, event, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      context
          .getSystemService(AlarmManager.class)
          .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epoch, operation);
      proof.put("scheduledEpoch", epoch);
      Activity visible = page;
      main(() -> visible.moveTaskToBack(true));
      shell("input keyevent 223");
      SystemClock.sleep(1000);
      proof.put(
          "screenOffBeforeDelivery", !context.getSystemService(PowerManager.class).isInteractive());
      proof.put(
          "keyguardLockedBeforeDelivery",
          context.getSystemService(KeyguardManager.class).isKeyguardLocked());
      write("phone-d02-armed.json", proof);
      long deadline = SystemClock.elapsedRealtime() + 75000;
      while (!AdhanService.state().playing && SystemClock.elapsedRealtime() < deadline)
        SystemClock.sleep(100);
      assertTrue("Real scheduled alarm starts the player", AdhanService.state().playing);
      assertFalse("This must be automatic delivery, not preview", AdhanService.state().preview);
      assertEquals(2, AdhanService.state().index);
      assertEquals(epoch, prefs.getLong("prayer_delivered_2", -1));
      proof.put("scheduledAlarmDelivered", true);
      proof.put("automaticPlayerStarted", true);
      proof.put("deliveryLatenessMs", System.currentTimeMillis() - epoch);
      boolean stopAction = false;
      for (StatusBarNotification n :
          context.getSystemService(NotificationManager.class).getActiveNotifications()) {
        if (n.getId() == AdhanNotification.ID) {
          stopAction =
              n.getNotification().actions != null && n.getNotification().actions.length > 0;
        }
      }
      proof.put("notificationHasStopAction", stopAction);
      assertTrue("Automatic notification includes Stop", stopAction);
      write("phone-d02-playing.json", proof);
      deadline = SystemClock.elapsedRealtime() + 90000;
      while (AdhanService.state().active && SystemClock.elapsedRealtime() < deadline)
        SystemClock.sleep(150);
      boolean stopped = !AdhanService.state().active;
      proof.put("serviceStoppedBeforeTimeout", stopped);
      assertTrue("User Stop ends playback/service", stopped);
      proof.put("passed", true);
    } finally {
      main(() -> AdhanService.stop(context));
      long deadline = SystemClock.elapsedRealtime() + 8000;
      while (AdhanService.state().active && SystemClock.elapsedRealtime() < deadline)
        SystemClock.sleep(100);
      main(
          () -> {
            prefs.edit().putBoolean("reminder_enabled", false).commit();
            PrayerReminders.sync(context);
          });
      if (operation != null) {
        context.getSystemService(AlarmManager.class).cancel(operation);
        operation.cancel();
      }
      Activity opened = page;
      main(
          () -> {
            restore(prefs, original);
            restore(plan, originalPlan);
            PrayerReminders.sync(context);
            if (opened != null) opened.finish();
          });
      proof.put("originalPreferenceValuesRestored", prefs.getAll().equals(original));
      proof.put("playerStoppedAtCleanup", !AdhanService.state().active);
      write("phone-d02-result.json", proof);
      shell("input keyevent 224");
    }
  }
}
