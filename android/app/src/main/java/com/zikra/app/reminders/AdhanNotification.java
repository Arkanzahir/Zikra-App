package com.zikra.app.reminders;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;
import com.zikra.app.PrayerActivity;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.R;

/** System-rendered banner and playback controls; no overlay or full-screen permission. */
public final class AdhanNotification {
  public static final String CHANNEL = "zikra_adhan_alerts";
  static final int ID = 7400;

  private AdhanNotification() {}

  static void createChannel(Context context) {
    NotificationChannel channel = new NotificationChannel(
        CHANNEL, context.getString(R.string.adhan_channel_banner), NotificationManager.IMPORTANCE_HIGH);
    channel.setDescription(context.getString(R.string.adhan_channel_banner_description));
    channel.setSound(null, null);
    channel.enableVibration(false);
    context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
  }

  static boolean allowed(Context context, boolean preview) {
    // Preserve the existing playback channel block when introducing the new banner category.
    return PrayerReminders.channelAllowed(context, PrayerReminders.PLAYBACK)
        && (preview || PrayerReminders.channelAllowed(context, CHANNEL));
  }

  static Notification create(Context context, int index, boolean preview, String token, String status) {
    PendingIntent stop = PendingIntent.getService(context, 7401, AdhanService.stopIntent(context, token),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    PendingIntent open = PendingIntent.getActivity(context, ID,
        new Intent(context, PrayerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    String title = preview ? context.getString(R.string.adhan_preview_title)
        : context.getString(R.string.adhan_notification_title, PrayerSchedule.NAMES[index]);
    Notification.Action action = new Notification.Action.Builder(
        android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_adhan_stop),
        context.getString(R.string.adhan_stop), stop).build();
    RemoteViews compact = content(context, R.layout.notification_adhan_compact, title, status);
    if (context.getResources().getConfiguration().fontScale >= 1.5f) {
      compact.setTextViewText(R.id.adhanNotificationStop, context.getString(R.string.adhan_stop_short));
      compact.setViewVisibility(R.id.adhanNotificationBody, android.view.View.GONE);
    }
    compact.setContentDescription(R.id.adhanNotificationStop, context.getString(R.string.adhan_stop));
    compact.setOnClickPendingIntent(R.id.adhanNotificationStop, stop);
    Notification.Builder builder = new Notification.Builder(context, preview ? PrayerReminders.PLAYBACK : CHANNEL)
        .setSmallIcon(R.drawable.ic_notification_prayer)
        .setColor(context.getColor(R.color.pine))
        .setContentTitle(title).setContentText(status)
        .setStyle(new Notification.DecoratedCustomViewStyle())
        .setCustomContentView(compact)
        .setCustomHeadsUpContentView(content(context, R.layout.notification_adhan_expanded, title, status))
        .setCustomBigContentView(content(context, R.layout.notification_adhan_expanded, title, status))
        .setCategory(preview ? Notification.CATEGORY_TRANSPORT : Notification.CATEGORY_ALARM)
        // Only the prayer name/playback state is shown; no profile, city or personal schedule.
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setOnlyAlertOnce(true).setOngoing(true).setAutoCancel(false)
        .setContentIntent(open).addAction(action);
    if (Build.VERSION.SDK_INT >= 31)
      builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
    return builder.build();
  }

  private static RemoteViews content(Context context, int layout, String title, String body) {
    RemoteViews views = new RemoteViews(context.getPackageName(), layout);
    views.setTextViewText(R.id.adhanNotificationTitle, title);
    views.setTextViewText(R.id.adhanNotificationBody, body);
    return views;
  }
}
