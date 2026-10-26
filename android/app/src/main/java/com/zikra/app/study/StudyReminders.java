package com.zikra.app.study;

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
import android.net.Uri;
import android.os.Build;
import com.zikra.app.R;
import com.zikra.app.StudyActivity;
import com.zikra.app.data.Task;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.location.LocationStore;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Legacy due-only reminders remain due-only until the user opts into advance notices. */
public final class StudyReminders {
  private static final String PREFS = "study_alarm_ids", CHANNEL = "study_deadlines";
  private static final int MAX_ALARMS = 128;
  private StudyReminders() {}

  public static boolean permitted(Context c) {
    return (Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED)
        && c.getSystemService(NotificationManager.class).areNotificationsEnabled();
  }
  private static PendingIntent alarm(Context c, long id, long due, int stage, ZoneId zone) {
    String path = "zikra://study/deadline/" + id + (stage == 0 ? "" : "/" + stage);
    Intent intent = new Intent(c, StudyAlarmReceiver.class).setAction("deadline")
        .setData(Uri.parse(path)).putExtra("task", id).putExtra("due", due)
        .putExtra("stage", stage).putExtra("zone", zone.getId());
    return PendingIntent.getBroadcast(c, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }
  public static void sync(Context c) { sync(c, null); }
  private static void sync(Context c, String justDelivered) {
    try { syncAvailable(c,justDelivered); }
    catch(android.database.sqlite.SQLiteException | SecurityException unavailable){
      // Never reset a database to recover an alarm. A later explicit edit/resume can retry.
    }
  }
  private static void syncAvailable(Context c, String justDelivered) {
    ZoneId zone = LocationStore.current(c).zone; long now = System.currentTimeMillis();
    SharedPreferences prefs = c.getSharedPreferences(PREFS, 0);
    Set<String> old = new HashSet<>();
    for (String key : prefs.getStringSet("ids", Collections.emptySet()))
      old.add(key.contains(":") ? key : key + ":0");
    Set<String> current = new HashSet<>(); List<Notice> plans = new ArrayList<>();
    List<Task> tasks;
    try (ZikraDbHelper db = new ZikraDbHelper(c)) {
      tasks = db.getTasks();
      if (permitted(c)) for (Task task : tasks) {
        if (!ZikraDbHelper.CATEGORY_STUDY.equals(task.category) || task.completed
            || !task.reminder || task.dueAt <= 0) continue;
        for (int stage = 0; stage < (task.advanceReminder ? 3 : 1); stage++) {
          long at = DeadlinePlan.at(task.dueAt, stage, zone);
          if (stage == 2 && at == task.dueAt) continue;
          String key=task.id+":"+stage;
          String delivered="delivered:"+(stage==0?Long.toString(task.id):key);
          if(prefs.getLong(delivered,0)==task.dueAt)continue;
          if(at<=now){
            if(!old.contains(key)||!DeadlinePlan.deliverable(task.dueAt,stage,zone,now))continue;
            at=now+1000;
          }
          plans.add(new Notice(task.id, task.dueAt, stage, at));
        }
      }
    }
    Set<String> visible = new HashSet<>(prefs.getStringSet("visible", Collections.emptySet()));
    for (String entry : new HashSet<>(visible)) {
      try {
        String[] parts = entry.split(":"); long id = Long.parseLong(parts[0]);
        int stage = Integer.parseInt(parts[1]); long due = Long.parseLong(parts[2]);
        Task selected = null;
        for (Task task : tasks) if (task.id == id) selected = task;
        if (selected != null && !selected.completed && selected.reminder && selected.dueAt == due
            && (stage == 0 || selected.advanceReminder) && now - due < 86400000L && permitted(c)) continue;
        c.getSystemService(NotificationManager.class).cancel(stage == 0 ? "study:" + id : "study:" + id + ":" + stage, 1);
      } catch (IllegalArgumentException | IndexOutOfBoundsException malformed) { }
      visible.remove(entry);
    }
    plans.sort(Comparator.comparingLong((Notice n) -> n.at).thenComparingLong(n -> n.id));
    for (int i = 0; i < Math.min(MAX_ALARMS, plans.size()); i++) {
      Notice notice = plans.get(i);
      StudyAlarmPolicy.schedule(c, notice.at, alarm(c, notice.id, notice.due, notice.stage, zone));
      current.add(notice.id + ":" + notice.stage);
    }
    old.removeAll(current);
    NotificationManager notifications = c.getSystemService(NotificationManager.class);
    for (String key : old) {
      try {
        String[] parts = key.split(":"); long id = Long.parseLong(parts[0]);
        int stage = Integer.parseInt(parts[1]);
        c.getSystemService(AlarmManager.class).cancel(alarm(c, id, 0, stage, zone));
        if (!key.equals(justDelivered)) {
          notifications.cancel("study:" + id + ":" + stage, 1);
          if (stage == 0) notifications.cancel("study:" + id, 1);
        }
      } catch (IllegalArgumentException | IndexOutOfBoundsException malformedOldId) {
        // A malformed legacy preference must not block valid reminders.
      }
    }
    prefs.edit().putStringSet("ids", current).putStringSet("visible", visible)
        .putBoolean("limited", plans.size() > MAX_ALARMS).apply();
    ClassReminders.sync(c);
  }
  public static void deliver(Context c, Intent intent) {
    long id = intent.getLongExtra("task", -1), due = intent.getLongExtra("due", 0);
    int stage = intent.getIntExtra("stage", 0); String key = id + ":" + stage;
    try {
      if (stage < 0 || stage > 2 || due <= 0) return;
      ZoneId zone = LocationStore.current(c).zone;
      String expectedZone = intent.getStringExtra("zone");
      if (stage > 0 && !zone.getId().equals(expectedZone)) return;
      Task selected = null;
      try (ZikraDbHelper db = new ZikraDbHelper(c)) {
        for (Task task : db.getTasks()) if (task.id == id) selected = task;
      }
      if (selected == null || selected.completed || !selected.reminder
          || !ZikraDbHelper.CATEGORY_STUDY.equals(selected.category) || selected.dueAt != due
          || (stage > 0 && !selected.advanceReminder) || !permitted(c)
          || !DeadlinePlan.deliverable(due, stage, zone, System.currentTimeMillis())) return;
      SharedPreferences prefs = c.getSharedPreferences(PREFS, 0);
      String delivered = "delivered:" + (stage == 0 ? Long.toString(id) : key);
      if (prefs.getLong(delivered, 0) == due) return;
      NotificationManager manager = c.getSystemService(NotificationManager.class);
      manager.createNotificationChannel(new NotificationChannel(CHANNEL,
          c.getString(R.string.weekly_task_notice_due), NotificationManager.IMPORTANCE_DEFAULT));
      int title = stage == 1 ? R.string.weekly_task_notice_day_before
          : stage == 2 ? R.string.weekly_task_notice_day_of : R.string.weekly_task_notice_due;
      String body = selected.title + (selected.subject.isEmpty() ? "" : " · " + selected.subject);
      manager.notify(stage == 0 ? "study:" + id : "study:" + key, 1, new Notification.Builder(c, CHANNEL)
          .setSmallIcon(R.drawable.ic_nearby_info).setContentTitle(c.getString(title))
          .setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body))
          .setVisibility(Notification.VISIBILITY_PRIVATE).setOnlyAlertOnce(true)
          .setContentIntent(PendingIntent.getActivity(c, 8101, new Intent(c, StudyActivity.class),
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
          .setAutoCancel(true).build());
      Set<String> visible = new HashSet<>(prefs.getStringSet("visible", Collections.emptySet()));
      visible.removeIf(value -> value.startsWith(key + ":")); visible.add(key + ":" + due);
      prefs.edit().putLong(delivered, due).putStringSet("visible", visible).commit();
    } catch (IllegalArgumentException | SecurityException | android.database.sqlite.SQLiteException invalidOrRevoked) {
      // Permission can change after the record is rechecked.
    } finally { sync(c, key); }
  }
  private static final class Notice {
    final long id, due, at; final int stage;
    Notice(long id, long due, int stage, long at) {
      this.id = id; this.due = due; this.stage = stage; this.at = at;
    }
  }
}
