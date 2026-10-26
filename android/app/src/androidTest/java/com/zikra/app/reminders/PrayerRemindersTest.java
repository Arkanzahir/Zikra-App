package com.zikra.app.reminders;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.test.InstrumentationTestCase;
import com.zikra.app.PrayerSchedule;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/** Exercises real AlarmManager identities and NotificationManager delivery on Android. */
@SuppressWarnings("deprecation")
public final class PrayerRemindersTest extends InstrumentationTestCase {
  private Context context;
  private SharedPreferences prefs;
  private android.app.Activity page;
  private final Map<String, Object> old = new HashMap<>();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    prefs = context.getSharedPreferences("zikra_preferences", 0);
    for (String key :
        new String[] {
          "reminder_enabled",
          "reminder_subuh",
          "reminder_dzuhur",
          "reminder_ashar",
          "reminder_maghrib",
          "reminder_isya",
          "prayer_delivered_2"
        }) old.put(key, prefs.getAll().get(key));
    if (Build.VERSION.SDK_INT >= 33
        && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
      getInstrumentation()
          .getUiAutomation()
          .grantRuntimePermission(context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
    SharedPreferences.Editor edit = prefs.edit().putBoolean("reminder_enabled", true);
    for (int i = 0; i < 6; i++)
      if (PrayerSchedule.isPrayer(i)) edit.putBoolean("reminder_" + PrayerSchedule.KEYS[i], true);
    edit.remove("prayer_delivered_2").commit();
    PrayerReminders.createChannel(context);
    assertTrue(
        "Notifications must be enabled for this device test", PrayerReminders.canNotify(context));
  }

  private PendingIntent pending(int index) {
    SharedPreferences plan=context.getSharedPreferences(PrayerReminders.PLAN,0);
    for(String slot:plan.getStringSet("ids",Collections.emptySet()))if(slot.endsWith("_"+index))return PendingIntent.getBroadcast(
        context,0,PrayerReminders.alarmIntent(context,index,plan.getLong("e:"+slot,0)),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
    return null;

  }

  public void testFiveAlarmsRespectPerPrayerAndMasterSwitches() {
    PrayerReminders.sync(context);
    for (int i = 0; i < 6; i++) if (PrayerSchedule.isPrayer(i)) assertNotNull(pending(i));
    assertNull(pending(1));
    prefs.edit().putBoolean("reminder_subuh", false).commit();
    PrayerReminders.sync(context);
    assertNull(pending(0));
    assertNotNull(pending(2));
    prefs.edit().putBoolean("reminder_enabled", false).commit();
    PrayerReminders.sync(context);
    for (int i = 0; i < 6; i++) if (PrayerSchedule.isPrayer(i)) assertNull(pending(i));
  }

  public void testReceiverPostsRealNotificationAndIgnoresDuplicateAndFutureEvents() {
    page=getInstrumentation().startActivitySync(new android.content.Intent(context,com.zikra.app.PrayerActivity.class)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));
    getInstrumentation().waitForIdleSync();
    long epoch = System.currentTimeMillis() - 1000;
    android.content.Intent planned=PrayerReminders.alarmIntent(context,2,epoch);String slot=planned.getStringExtra("slot");
    context.getSharedPreferences(PrayerReminders.PLAN,0).edit().putLong("e:"+slot,epoch)
        .putString("l:"+slot,com.zikra.app.location.LocationStore.current(context).key())
        .putLong("r:"+slot,PrayerSettings.revision(context)).putStringSet("ids",Collections.singleton(slot)).commit();
    context.sendBroadcast(PrayerReminders.alarmIntent(context, 2, epoch));
    StatusBarNotification posted = awaitNotification(PrayerReminders.NOTIFICATION_BASE + 2);
    assertEquals("Waktu Dzuhur", posted.getNotification().extras.getString("android.title"));
    assertEquals(epoch, prefs.getLong("prayer_delivered_2", 0));
    long firstPost = posted.getPostTime();
    SystemClock.sleep(300);
    context.sendBroadcast(PrayerReminders.alarmIntent(context, 2, epoch));
    context.sendBroadcast(
        PrayerReminders.alarmIntent(context, 3, System.currentTimeMillis() + 60000));
    SystemClock.sleep(1000);
    assertEquals(firstPost, awaitNotification(PrayerReminders.NOTIFICATION_BASE + 2).getPostTime());
    for (StatusBarNotification notification :
        context.getSystemService(NotificationManager.class).getActiveNotifications())
      assertFalse(
          "A future event must not post early",
          notification.getId() == PrayerReminders.NOTIFICATION_BASE + 3);
  }

  private StatusBarNotification awaitNotification(int id) {
    long end = SystemClock.uptimeMillis() + 10000;
    while (SystemClock.uptimeMillis() < end) {
      for (StatusBarNotification notification :
          context.getSystemService(NotificationManager.class).getActiveNotifications())
        if (notification.getId() == id) return notification;
      SystemClock.sleep(100);
    }
    fail("Notification was not posted");
    return null;
  }

  @Override
  protected void tearDown() throws Exception {
    if(page!=null)getInstrumentation().runOnMainSync(page::finish);
    prefs.edit().putBoolean("reminder_enabled", false).commit();
    PrayerReminders.sync(context);
    SharedPreferences.Editor edit = prefs.edit();
    for (Map.Entry<String, Object> entry : old.entrySet()) {
      if (entry.getValue() == null) edit.remove(entry.getKey());
      else if (entry.getValue() instanceof Boolean)
        edit.putBoolean(entry.getKey(), (Boolean) entry.getValue());
      else if (entry.getValue() instanceof Long)
        edit.putLong(entry.getKey(), (Long) entry.getValue());
    }
    edit.commit();
    PrayerReminders.sync(context);
    super.tearDown();
  }
}
