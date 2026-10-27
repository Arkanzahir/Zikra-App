package com.zikra.app.audio;

import android.app.*;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.test.InstrumentationTestCase;

@SuppressWarnings("deprecation")
public final class QuranNotificationTest extends InstrumentationTestCase {
  public void testLegacyDownloadMigrationPreservesPrayerAndSeparateDownload() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    String prayer = "d03-fixture-prayer";
    manager.createNotificationChannel(new NotificationChannel(prayer, "Fixture", 2));
    manager.createNotificationChannel(
        new NotificationChannel(QuranDownloadService.CHANNEL, "Unduhan Quran", 2));
    try {
      manager.notify(7302, notice(context, prayer));
      await(() -> active(manager, 7302, prayer));
      QuranDownloadService.clearLegacyDownload(manager);
      assertTrue("Dzuhur retained", active(manager, 7302, prayer));
      manager.notify(QuranDownloadService.ID, notice(context, QuranDownloadService.CHANNEL));
      await(() -> active(manager, QuranDownloadService.ID, QuranDownloadService.CHANNEL));
      assertTrue("Both notices coexist", active(manager, 7302, prayer));
      assertTrue(active(manager, QuranDownloadService.ID, QuranDownloadService.CHANNEL));
      manager.cancel(QuranDownloadService.ID);
      await(() -> !active(manager, QuranDownloadService.ID, QuranDownloadService.CHANNEL));
      assertTrue("Finishing download cannot remove prayer", active(manager, 7302, prayer));
      manager.notify(7302, notice(context, QuranDownloadService.CHANNEL));
      await(() -> active(manager, 7302, QuranDownloadService.CHANNEL));
      QuranDownloadService.clearLegacyDownload(manager);
      await(() -> !active(manager, 7302, QuranDownloadService.CHANNEL));
    } finally {
      manager.cancel(7302);
      manager.cancel(QuranDownloadService.ID);
      manager.deleteNotificationChannel(prayer);
    }
  }

  private void await(java.util.function.BooleanSupplier condition) {
    long deadline = android.os.SystemClock.uptimeMillis() + 3000;
    while (!condition.getAsBoolean() && android.os.SystemClock.uptimeMillis() < deadline)
      android.os.SystemClock.sleep(30);
    assertTrue("Android notification transaction completes", condition.getAsBoolean());
  }

  private Notification notice(Context context, String channel) {
    return new Notification.Builder(context, channel)
        .setSmallIcon(com.zikra.app.R.drawable.ic_notification_prayer)
        .setContentTitle("DATA UJI D03")
        .build();
  }

  private boolean active(NotificationManager manager, int id, String channel) {
    for (StatusBarNotification item : manager.getActiveNotifications())
      if (item.getId() == id && item.getNotification().getChannelId().equals(channel)) return true;
    return false;
  }
}
