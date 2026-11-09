package com.zikra.app.safety;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.zikra.app.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.JSONArray;

/** Opt-in national report alerts. No GPS, chat content or server credentials are registered. */
public final class EarthquakeAlerts {
  public static final String PREFS = "zikra_earthquake_alerts", CHANNEL = "earthquake_reports_v1";
  private static final String TOPIC = "zikra-earthquake-beta-v1";
  private static final Object LOCK = new Object();

  private EarthquakeAlerts() {}

  public static boolean configured(Context context) {
    return context.getResources().getBoolean(R.bool.quake_push_configured);
  }

  public static boolean enabled(Context context) {
    return context.getSharedPreferences(PREFS, 0).getBoolean("enabled", false);
  }

  public static void sync(Context context) {
    if (!configured(context)) return;
    try {
      if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context);
      FirebaseMessaging messaging = FirebaseMessaging.getInstance();
      if (!enabled(context)) {
        messaging.setAutoInitEnabled(false);
        if (context.getSharedPreferences(PREFS, 0).getBoolean("ever_enabled", false))
          messaging.unsubscribeFromTopic(TOPIC);
        return;
      }
      messaging.setAutoInitEnabled(true);
      messaging
          .subscribeToTopic(TOPIC)
          .addOnCompleteListener(
              task ->
                  context
                      .getSharedPreferences(PREFS, 0)
                      .edit()
                      .putString("subscription", task.isSuccessful() ? "ready" : "pending")
                      .apply());
    } catch (RuntimeException unavailable) {
      context.getSharedPreferences(PREFS, 0).edit().putString("subscription", "pending").apply();
    }
  }

  public static void setEnabled(Context context, boolean value) {
    synchronized (LOCK) {
      context
          .getSharedPreferences(PREFS, 0)
          .edit()
          .putBoolean("enabled", value)
          .putBoolean(
              "ever_enabled",
              value || context.getSharedPreferences(PREFS, 0).getBoolean("ever_enabled", false))
          .putString("subscription", value ? "pending" : "off")
          .apply();
      if (!value) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        // A just-enqueued notification may not appear in getActiveNotifications yet.
        try {
          JSONArray seen =
              new JSONArray(context.getSharedPreferences(PREFS, 0).getString("seen", "[]"));
          for (int i = Math.max(0, seen.length() - 128); i < seen.length(); i++) {
            String id = seen.optString(i);
            if (id.matches("[a-f0-9]{64}")) manager.cancel(id, 0);
          }
        } catch (org.json.JSONException invalid) {
        }
        for (android.service.notification.StatusBarNotification notice :
            manager.getActiveNotifications())
          if (CHANNEL.equals(notice.getNotification().getChannelId()))
            manager.cancel(notice.getTag(), notice.getId());
      }
    }
    sync(context.getApplicationContext());
  }

  public static boolean permission(Context context) {
    return Build.VERSION.SDK_INT < 33
        || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
  }

  public static boolean allowed(Context context) {
    if (!permission(context)) return false;
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    NotificationChannel existing = manager.getNotificationChannel(CHANNEL);
    return manager.areNotificationsEnabled()
        && (existing == null || existing.getImportance() != NotificationManager.IMPORTANCE_NONE);
  }

  public static void channel(Context context) {
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL,
            context.getString(R.string.quake_alert_channel),
            NotificationManager.IMPORTANCE_DEFAULT);
    channel.setDescription(context.getString(R.string.quake_alert_scope));
    channel.enableVibration(true);
    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
    context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
  }

  public static boolean receive(Context context, Map<String, String> data, Instant now) {
    try {
      if (!enabled(context) || !allowed(context)) return false;
      NotificationManager manager = context.getSystemService(NotificationManager.class);
      channel(context);
      if (!manager.areNotificationsEnabled()
          || manager.getNotificationChannel(CHANNEL).getImportance()
              == NotificationManager.IMPORTANCE_NONE) return false;
      EarthquakeReport report = EarthquakeAlertPolicy.validate(data, now);
      String id = EarthquakeAlertPolicy.id(report);
      synchronized (LOCK) {
        if (!enabled(context) || !allowed(context)) return false;
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
        JSONArray previous = new JSONArray(prefs.getString("seen", "[]"));
        for (int i = 0; i < previous.length(); i++)
          if (id.equals(previous.optString(i))) return false;
        JSONArray updated = new JSONArray();
        for (int i = Math.max(0, previous.length() - 127); i < previous.length(); i++)
          updated.put(previous.optString(i));
        updated.put(id);
        if (!prefs.edit().putString("seen", updated.toString()).commit()) return false;
        Intent detail =
            new Intent(context, EarthquakeActivity.class)
                .setData(Uri.parse("zikra-internal://earthquake/" + id))
                .putExtra("quake_report", data.get("quake_report"))
                .putExtra("quake_notification", true);
        PendingIntent open =
            TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, ProtectorActivity.class))
                .addNextIntent(detail)
                .getPendingIntent(
                    0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String time =
            DateTimeFormatter.ofPattern("d MMM HH:mm 'WIB'", new Locale("id", "ID"))
                .withZone(ZoneId.of("Asia/Jakarta"))
                .format(report.quake.time);
        String content =
            context.getString(
                R.string.quake_alert_body, report.quake.area, report.quake.depth, time);
        Notification notice =
            new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_prayer)
                .setContentTitle(
                    context.getString(R.string.quake_alert_title, report.quake.magnitude))
                .setContentText(content)
                .setStyle(
                    new Notification.BigTextStyle()
                        .bigText(content + "\n" + context.getString(R.string.quake_alert_scope)))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setWhen(report.quake.time.toEpochMilli())
                .setShowWhen(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_EVENT)
                .build();
        manager.notify(id, 0, notice);
        return true;
      }
    } catch (Exception invalid) {
      return false;
    }
  }
}
