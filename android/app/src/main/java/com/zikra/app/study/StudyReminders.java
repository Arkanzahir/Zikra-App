package com.zikra.app.study;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import com.zikra.app.R;
import com.zikra.app.StudyActivity;
import com.zikra.app.data.*;
import java.util.*;

/**
 * Optional, inexact deadline reminders. Stale alarms cannot announce edited/deleted/completed
 * tasks.
 */
public final class StudyReminders {
  private static final String PREFS = "study_alarm_ids", CHANNEL = "study_deadlines";

  private StudyReminders() {}

  public static boolean permitted(Context c) {
    return (Build.VERSION.SDK_INT < 33
            || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED)
        && c.getSystemService(NotificationManager.class).areNotificationsEnabled();
  }

  private static PendingIntent alarm(Context c, long id, long due) {
    Intent intent =
        new Intent(c, StudyAlarmReceiver.class)
            .setAction("deadline")
            .setData(Uri.parse("zikra://study/deadline/" + id))
            .putExtra("task", id)
            .putExtra("due", due);
    return PendingIntent.getBroadcast(
        c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  public static void sync(Context c) {
    AlarmManager manager = c.getSystemService(AlarmManager.class);
    SharedPreferences prefs = c.getSharedPreferences(PREFS, 0);
    Set<String> old = new HashSet<>(prefs.getStringSet("ids", Collections.emptySet()));
    Set<String> current = new HashSet<>();
    try (ZikraDbHelper db = new ZikraDbHelper(c)) {
      for (Task task : db.getTasks()) {
        if (!ZikraDbHelper.CATEGORY_STUDY.equals(task.category)
            || task.completed
            || !task.reminder
            || task.dueAt <= System.currentTimeMillis()
            || !permitted(c)) continue;
        String id = Long.toString(task.id);
        current.add(id);
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, task.dueAt, alarm(c, task.id, task.dueAt));
      }
    }
    old.removeAll(current);
    NotificationManager notifications = c.getSystemService(NotificationManager.class);
    for (String id : old) {
      manager.cancel(alarm(c, Long.parseLong(id), 0));
      notifications.cancel("study:" + id, 1);
    }
    prefs.edit().putStringSet("ids", current).apply();
  }

  public static void deliver(Context c, Intent intent) {
    long id = intent.getLongExtra("task", -1), due = intent.getLongExtra("due", 0);
    Task selected = null;
    try (ZikraDbHelper db = new ZikraDbHelper(c)) {
      for (Task task : db.getTasks()) if (task.id == id) selected = task;
    }
    if (selected == null
        || selected.completed
        || !selected.reminder
        || !ZikraDbHelper.CATEGORY_STUDY.equals(selected.category)
        || selected.dueAt != due
        || due <= 0
        || due > System.currentTimeMillis()
        || !permitted(c)) return;
    SharedPreferences delivered = c.getSharedPreferences(PREFS, 0);
    String key = "delivered:" + id;
    if (delivered.getLong(key, 0) == due) return;
    NotificationManager manager = c.getSystemService(NotificationManager.class);
    manager.createNotificationChannel(
        new NotificationChannel(CHANNEL, "Tenggat tugas", NotificationManager.IMPORTANCE_DEFAULT));
    Intent open = new Intent(c, StudyActivity.class);
    manager.notify(
        "study:" + id,
        1,
        new Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_nearby_info)
            .setContentTitle("Tenggat tugas")
            .setContentText(selected.title)
            .setStyle(new Notification.BigTextStyle().bigText(selected.title))
            .setContentIntent(
                PendingIntent.getActivity(
                    c,
                    8101,
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build());
    delivered.edit().putLong(key, due).commit();
  }
}
