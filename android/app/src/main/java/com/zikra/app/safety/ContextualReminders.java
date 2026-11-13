package com.zikra.app.safety;

import android.app.*;
import android.app.job.*;
import android.content.*;
import com.zikra.app.*;
import com.zikra.app.location.*;
import java.time.*;
import java.util.*;

/** Optional local reminders; forecast messages are not official severe-weather warnings. */
public final class ContextualReminders {
  public static final String PREFS = "zikra_contextual_reminders_v1";
  private static final String CHANNEL = "contextual_reminders_v1";
  private static final int JOB = 46103;
  private static final Object LOCK = new Object();
  public static final String[] OPTIONS = {"weather", "briefing", "morning", "evening"};
  private ContextualReminders() {}
  public static boolean enabled(Context c, String option) {
    return Arrays.asList(OPTIONS).contains(option) && c.getSharedPreferences(PREFS, 0).getBoolean(option, false);
  }
  public static void set(Context c, String option, boolean enabled) {
    if (!Arrays.asList(OPTIONS).contains(option)) throw new IllegalArgumentException("Reminder option");
    synchronized (LOCK) {
      android.content.SharedPreferences p = c.getSharedPreferences(PREFS, 0);
      p.edit().putBoolean(option, enabled).putLong("revision", p.getLong("revision", 0) + 1).commit();
      NotificationManager manager = c.getSystemService(NotificationManager.class);
      for (int i = 0; i < 3; i++) manager.cancel(JOB + i);
      schedule(c);
    }
  }
  public static int minute(Context c, String period) {
    int fallback = "evening".equals(period) ? 17 * 60 : 7 * 60;
    return Math.max(0, Math.min(1439, c.getSharedPreferences(PREFS, 0).getInt(period + "_minute", fallback)));
  }
  public static void time(Context c, String period, int minute) {
    if (!("morning".equals(period) || "evening".equals(period)) || minute < 0 || minute > 1439)
      throw new IllegalArgumentException("Reminder time");
    synchronized (LOCK) {
      android.content.SharedPreferences p = c.getSharedPreferences(PREFS, 0);
      p.edit().putInt(period + "_minute", minute).putLong("revision", p.getLong("revision", 0) + 1).commit();
      schedule(c);
    }
  }
  public static void schedule(Context c) {
    JobScheduler scheduler = c.getSystemService(JobScheduler.class);
    if (scheduler == null) return;
    boolean any = false;
    for (String key : OPTIONS) any |= enabled(c, key);
    if (!any) { scheduler.cancel(JOB); return; }
    if (scheduler.getPendingJob(JOB) != null) return;
    scheduler.schedule(new JobInfo.Builder(JOB, new ComponentName(c, ContextualReminderJob.class))
        .setPeriodic(15 * 60 * 1000L, 5 * 60 * 1000L).setPersisted(true).build());
  }
  public static boolean inWindow(ZonedDateTime now, int minute) {
    int current = now.getHour() * 60 + now.getMinute();
    return current >= minute && current < Math.min(1440, minute + 120);
  }
  static void deliver(Context c, LocationPoint location, long revision, String kind, int offset,
      String title, String message, String category) {
    synchronized (LOCK) {
      android.content.SharedPreferences p = c.getSharedPreferences(PREFS, 0);
      if (p.getLong("revision", 0) != revision || !LocationStore.current(c).key().equals(location.key())) return;
      boolean allowed = "morning".equals(kind) ? enabled(c, "morning") || enabled(c, "briefing") : enabled(c, kind);
      if (!allowed || !EarthquakeAlerts.permission(c)) return;
      NotificationManager manager = c.getSystemService(NotificationManager.class);
      if (manager == null || !manager.areNotificationsEnabled()) return;
      manager.createNotificationChannel(new NotificationChannel(CHANNEL, c.getString(R.string.contextual_title), NotificationManager.IMPORTANCE_DEFAULT));
      NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
      if (channel == null || channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return;
      Instant instant = Instant.now();
      String day = instant.atZone(location.zone).toLocalDate().toString();
      long at = instant.toEpochMilli(), previous = p.getLong("sent_" + kind + "_at", 0);
      if (day.equals(p.getString("sent_" + kind, "")) || at - previous < 6 * 3600000L) return;
      Intent open = "weather".equals(kind) ? new Intent(c, WeatherActivity.class)
          : category == null ? new Intent(c, ContextualActivity.class)
          : new Intent(c, DuaActivity.class).putExtra("dua_category_name", category);
      PendingIntent pending = PendingIntent.getActivity(c, JOB + offset, open,
          PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      Notification notification = new Notification.Builder(c, CHANNEL)
          .setSmallIcon(R.drawable.ic_notification_prayer).setContentTitle(title).setContentText(message)
          .setStyle(new Notification.BigTextStyle().bigText(message)).setContentIntent(pending)
          .setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
      if (!p.edit().putString("sent_" + kind, day).putLong("sent_" + kind + "_at", at).commit()) return;
      try { manager.notify(JOB + offset, notification); }
      catch (RuntimeException failed) {
        p.edit().remove("sent_" + kind).remove("sent_" + kind + "_at").commit();
      }
    }
  }
  public static String briefing(Context c) {
    LocationPoint point = LocationStore.current(c);
    PrayerSchedule.Day day = PrayerSchedule.forDate(LocalDate.now(point.zone), point);
    String agenda = c.getString(R.string.contextual_brief_open_study);
    try (com.zikra.app.data.ZikraDbHelper db = new com.zikra.app.data.ZikraDbHelper(c)) {
      int pending = 0, due = 0;
      long end = LocalDate.now(point.zone).plusDays(1).atStartOfDay(point.zone).toInstant().toEpochMilli();
      for (com.zikra.app.data.Task task : db.getTasks()) {
        if (task.completed || !com.zikra.app.data.ZikraDbHelper.CATEGORY_STUDY.equals(task.category)) continue;
        pending++;
        if (task.dueAt > 0 && task.dueAt < end) due++;
      }
      agenda = c.getString(R.string.contextual_brief_tasks, pending, due);
    } catch (RuntimeException unavailable) { /* Do not invent an empty agenda when storage is unavailable. */ }
    return point.label + " · " + point.zoneLabel(Instant.now()) + "\n"
        + "Zuhur " + day.time(2) + " · Maghrib " + day.time(4)
        + "\n" + (c.getString("local".equals(day.source) ? R.string.contextual_brief_calculated : R.string.contextual_brief_cached))
        + "\n" + agenda + "\n" + weatherSummary(c, point);
  }
  private static String weatherSummary(Context c, LocationPoint point) {
    android.content.SharedPreferences cache = c.getSharedPreferences("zikra_weather_openmeteo_v2", 0);
    String prefix = point.key() + ":";
    long fetched = cache.getLong(prefix + "fetched_at", 0), now = System.currentTimeMillis();
    if (WeatherCode.usableCache(fetched, now, OpenMeteoClient.MAX_AGE_MS)) {
      try {
        String json = cache.getString(prefix + "json", "");
        if (json.length() > 262144) return c.getString(R.string.contextual_brief_no_weather);
        BmkgParser.Forecast value = OpenMeteoParser.parse(json, Instant.ofEpochMilli(now), Instant.ofEpochMilli(fetched),
            true, now - fetched > OpenMeteoClient.FRESH_MS, point);
        if (WeatherOutlook.usable(value, Instant.ofEpochMilli(now), OpenMeteoClient.MAX_AGE_MS))
          return "Prakiraan tersimpan: " + value.current.description + ", " + value.current.temperature
              + "°C · Open-Meteo · diambil " + java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm", new Locale("id", "ID"))
                  .format(Instant.ofEpochMilli(fetched).atZone(point.zone));
      } catch (Exception invalid) { /* No invented weather in the morning summary. */ }
    }
    return c.getString(R.string.contextual_brief_no_weather);
  }
}
