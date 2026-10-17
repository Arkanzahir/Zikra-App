package com.zikra.app.reminders;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import com.zikra.app.PrayerActivity;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.R;
import java.time.ZonedDateTime;

/** Five native one-shot alarms, recalculated after delivery, boot, clock or preference changes. */
public final class PrayerReminders {
  public static final String CHANNEL = "zikra_prayer_times";
  static final int REQUEST_BASE = 7200;
  static final int NOTIFICATION_BASE = 7300;
  public static final String EXTRA_INDEX = "prayer_index", EXTRA_EPOCH = "scheduled_epoch";

  private PrayerReminders() {}

  private static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences("zikra_preferences", Context.MODE_PRIVATE);
  }

  public static boolean enabled(Context context) {
    return prefs(context).getBoolean("reminder_enabled", false);
  }

  public static void createChannel(Context context) {
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL, "Pengingat waktu sholat", NotificationManager.IMPORTANCE_DEFAULT);
    channel.setDescription("Waktu sholat harian untuk Sukolilo, Surabaya");
    channel.enableVibration(true);
    context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
  }

  public static boolean canNotify(Context context) {
    if (Build.VERSION.SDK_INT >= 33
        && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return false;
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
    return manager.areNotificationsEnabled()
        && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
  }

  public static boolean exactAllowed(Context context) {
    return Build.VERSION.SDK_INT < 31
        || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
  }

  public static void sync(Context context) {
    createChannel(context);
    AlarmManager alarms = context.getSystemService(AlarmManager.class);
    NotificationManager notifications = context.getSystemService(NotificationManager.class);
    for (int index = 0; index < 6; index++) {
      if (!PrayerSchedule.isPrayer(index)) continue;
      PendingIntent old =
          PendingIntent.getBroadcast(
              context,
              REQUEST_BASE + index,
              alarmIntent(context, index, 0),
              PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
      if (old != null) {
        alarms.cancel(old);
        old.cancel();
      }
      boolean active =
          enabled(context)
              && prefs(context).getBoolean("reminder_" + PrayerSchedule.KEYS[index], true);
      if (!active) notifications.cancel(NOTIFICATION_BASE + index);
      if (!active || !canNotify(context)) continue;
      long when =
          PrayerSchedule.nextOccurrence(index, ZonedDateTime.now(PrayerSchedule.ZONE))
              .toInstant()
              .toEpochMilli();
      PendingIntent operation =
          PendingIntent.getBroadcast(
              context,
              REQUEST_BASE + index,
              alarmIntent(context, index, when),
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      try {
        if (exactAllowed(context))
          alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, operation);
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, operation);
      } catch (SecurityException revokedWhileScheduling) {
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, operation);
      }
    }
  }

  static Intent alarmIntent(Context context, int index, long epoch) {
    return new Intent(context, PrayerAlarmReceiver.class)
        .setAction("com.zikra.app.PRAYER_" + index)
        .putExtra(EXTRA_INDEX, index)
        .putExtra(EXTRA_EPOCH, epoch);
  }

  static void deliver(Context context, Intent intent) {
    int index = intent.getIntExtra(EXTRA_INDEX, -1);
    long epoch = intent.getLongExtra(EXTRA_EPOCH, 0);
    long age = System.currentTimeMillis() - epoch;
    if (!PrayerSchedule.isPrayer(index) || epoch <= 0) return;
    SharedPreferences preferences = prefs(context);
    String receipt = "prayer_delivered_" + index;
    if (enabled(context)
        && preferences.getBoolean("reminder_" + PrayerSchedule.KEYS[index], true)
        && age >= 0
        && age <= 30 * 60_000L
        && canNotify(context)
        && preferences.getLong(receipt, -1) != epoch) {
      createChannel(context);
      PendingIntent open =
          PendingIntent.getActivity(
              context,
              NOTIFICATION_BASE + index,
              new Intent(context, PrayerActivity.class)
                  .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      String time =
          java.time.Instant.ofEpochMilli(epoch)
              .atZone(PrayerSchedule.ZONE)
              .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
      Notification notification =
          new Notification.Builder(context, CHANNEL)
              .setSmallIcon(R.drawable.ic_notification_prayer)
              .setContentTitle("Waktu " + PrayerSchedule.NAMES[index])
              .setContentText(time + " WIB · Sukolilo, Surabaya")
              .setCategory(Notification.CATEGORY_REMINDER)
              .setWhen(epoch)
              .setShowWhen(true)
              .setAutoCancel(true)
              .setContentIntent(open)
              .setColor(context.getColor(R.color.pine))
              .build();
      try {
        context
            .getSystemService(NotificationManager.class)
            .notify(NOTIFICATION_BASE + index, notification);
        preferences.edit().putLong(receipt, epoch).apply();
      } catch (SecurityException permissionRevoked) {
        // The user can revoke permission between the check and the actual post.
      }
    }
    sync(context);
  }
}
