package com.zikra.app.study;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import com.zikra.app.R;
import com.zikra.app.WeeklyClassesActivity;
import com.zikra.app.data.WeeklyClass;
import com.zikra.app.data.WeeklyClassStore;
import com.zikra.app.location.LocationStore;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** One next occurrence per class; delivery checks the current record and selected zone again. */
public final class ClassReminders {
  private static final String PREFS = "study_class_alarms", CHANNEL = "study_classes";
  private ClassReminders() {}
  private static String signature(WeeklyClass item, ZoneId zone) {
    return item.id + "|" + item.revision + "|" + zone.getId();
  }
  private static PendingIntent alarm(Context c, String signature, long start, long trigger) {
    Intent intent = new Intent(c, ClassAlarmReceiver.class).setAction("class")
        .setData(new Uri.Builder().scheme("zikra").authority("study")
            .appendPath("class").appendPath(signature).build())
        .putExtra("signature", signature).putExtra("start", start).putExtra("trigger", trigger);
    return PendingIntent.getBroadcast(c, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }
  public static void sync(Context c) {
    try { syncAvailable(c); }
    catch (android.database.sqlite.SQLiteException | SecurityException unavailable) {
      // Retain records and prior schedule metadata; delivery also rechecks the store.
    }
  }
  private static void syncAvailable(Context c) {
    long now = System.currentTimeMillis(); ZoneId zone = LocationStore.current(c).zone;
    SharedPreferences prefs = c.getSharedPreferences(PREFS, 0);
    Set<String> old = new HashSet<>(prefs.getStringSet("ids", Collections.emptySet()));
    Set<String> current = new HashSet<>();
    SharedPreferences.Editor update=prefs.edit();
    try (WeeklyClassStore store = new WeeklyClassStore(c)) {
      if (StudyReminders.permitted(c)) for (WeeklyClass item : store.all()) {
        long trigger = item.nextReminder(now, zone);
        if (trigger == 0) continue;
        String key = signature(item, zone);
        long start=trigger+item.reminderMinutes*60000L;
        long pending=prefs.getLong("scheduled:"+key,0);
        java.time.LocalDate date=Instant.ofEpochMilli(pending).atZone(zone).toLocalDate();
        if(old.contains(key)&&pending>now&&pending-now<7*86400000L
            &&date.getDayOfWeek().getValue()==item.weekday&&item.startOn(date,zone)==pending
            &&pending-item.reminderMinutes*60000L<=now&&prefs.getLong("delivered:"+key,0)!=pending){
          start=pending;trigger=pending-item.reminderMinutes*60000L;
        }
        update.putLong("scheduled:"+key,start);
        StudyAlarmPolicy.schedule(c, Math.max(trigger,now+1000),alarm(c,key,start,trigger));
        current.add(key);
      }
    }
    old.removeAll(current);
    for (String key : old) {
      c.getSystemService(AlarmManager.class).cancel(alarm(c, key, 0, 0));
      c.getSystemService(NotificationManager.class).cancel("class:" + key, 1);
    }
    update.putStringSet("ids", current);
    Set<String> valid=new HashSet<>();
    try(WeeklyClassStore store=new WeeklyClassStore(c)){for(WeeklyClass item:store.all())valid.add(signature(item,zone));}
    for(String name:prefs.getAll().keySet())if(name.startsWith("delivered:")&&!valid.contains(name.substring(10)))update.remove(name);
    for(String name:prefs.getAll().keySet())if(name.startsWith("scheduled:")&&!current.contains(name.substring(10)))update.remove(name);
    update.apply();
  }
  public static void deliver(Context c, Intent intent) {
    try {
      String key = intent.getStringExtra("signature");
      if (key == null) return;
      String[] parts = key.split("\\|", -1);
      if (parts.length != 3) return;
      ZoneId zone = LocationStore.current(c).zone;
      if (!parts[2].equals(zone.getId())) return;
      long id = Long.parseLong(parts[0]), revision = Long.parseLong(parts[1]);
      long start = intent.getLongExtra("start", 0), trigger = intent.getLongExtra("trigger", 0);
      WeeklyClass item;
      try (WeeklyClassStore store = new WeeklyClassStore(c)) { item = store.find(id); }
      if (item == null || item.revision != revision || item.reminderMinutes == 0 || start <= 0
          || start != item.startOn(Instant.ofEpochMilli(start).atZone(zone).toLocalDate(), zone)
          || trigger != start - item.reminderMinutes * 60000L
          || System.currentTimeMillis() < trigger || System.currentTimeMillis() >= start
          || !StudyReminders.permitted(c)) return;
      SharedPreferences prefs = c.getSharedPreferences(PREFS, 0);
      String delivered = "delivered:" + key;
      if (prefs.getLong(delivered, 0) == start) return;
      NotificationManager manager = c.getSystemService(NotificationManager.class);
      manager.createNotificationChannel(new NotificationChannel(CHANNEL,
          c.getString(R.string.weekly_channel), NotificationManager.IMPORTANCE_DEFAULT));
      PendingIntent open = PendingIntent.getActivity(c, 8102,
          new Intent(c, WeeklyClassesActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      manager.notify("class:" + key, 1, new Notification.Builder(c, CHANNEL)
          .setSmallIcon(R.drawable.ic_nearby_info).setAutoCancel(true).setOnlyAlertOnce(true)
          .setVisibility(Notification.VISIBILITY_PRIVATE)
          .setContentTitle(c.getString(R.string.weekly_notice_title,
              Math.max(1, (int) ((start - System.currentTimeMillis() + 59999) / 60000))))
          .setContentText(c.getString(R.string.weekly_notice_body, item.title, WeeklyClassesActivity.clock(item.startMinute)))
          .setContentIntent(open).build());
      prefs.edit().putLong(delivered, start).commit();
    } catch (IllegalArgumentException | SecurityException | android.database.sqlite.SQLiteException invalidOrRevoked) {
      // A stale/corrupt alarm or a permission revoked during delivery must not crash the app.
    } finally { sync(c); }
  }
}
