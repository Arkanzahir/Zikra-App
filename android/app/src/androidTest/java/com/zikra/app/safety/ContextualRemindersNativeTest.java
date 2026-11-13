package com.zikra.app.safety;

import android.app.NotificationManager;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;
import com.zikra.app.location.LocationStore;
import com.zikra.app.location.LocationPoint;
import java.time.Instant;

@SuppressWarnings("deprecation")
public final class ContextualRemindersNativeTest extends InstrumentationTestCase {
  public void testSchedulingDeduplicationAndStaleWork() {
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    Context c = getInstrumentation().getTargetContext();
    SharedPreferences prefs = c.getSharedPreferences(ContextualReminders.PREFS, 0);
    assertTrue("Requires disposable, fresh reminder preferences", prefs.getAll().isEmpty());
    NotificationManager notifications = c.getSystemService(NotificationManager.class);
    JobScheduler scheduler = c.getSystemService(JobScheduler.class);
    assertTrue(EarthquakeAlerts.permission(c));
    try {
      ContextualReminders.set(c, "morning", true);
      assertNotNull(scheduler.getPendingJob(46103));
      ContextualReminders.time(c, "morning", 65);
      assertEquals(65, ContextualReminders.minute(c, "morning"));
      LocationPoint point = LocationStore.current(c);
      long revision = prefs.getLong("revision", 0);
      ContextualReminders.deliver(c, point, revision - 1, "morning", 0, "[Uji]", "Data sintetis", null);
      assertFalse(prefs.contains("sent_morning"));
      ContextualReminders.deliver(c, point, revision, "morning", 0, "[Uji]", "Data sintetis", null);
      long sent = prefs.getLong("sent_morning_at", 0);
      assertTrue(sent > 0);
      assertEquals(Instant.now().atZone(point.zone).toLocalDate().toString(), prefs.getString("sent_morning", ""));
      ContextualReminders.deliver(c, point, revision, "morning", 0, "[Uji] duplikat", "Data sintetis", null);
      assertEquals(sent, prefs.getLong("sent_morning_at", 0));
      long recent = System.currentTimeMillis() - 3600000;
      prefs.edit().putString("sent_morning", "2000-01-01").putLong("sent_morning_at", recent).commit();
      ContextualReminders.deliver(c, point, revision, "morning", 0, "[Uji] cooldown", "Data sintetis", null);
      assertEquals(recent, prefs.getLong("sent_morning_at", 0));
      ContextualReminders.set(c, "morning", false);
      assertNull(scheduler.getPendingJob(46103));
      prefs.edit().remove("sent_morning").remove("sent_morning_at").commit();
      ContextualReminders.deliver(c, point, prefs.getLong("revision", 0), "morning", 0, "[Uji] opt-out", "Data sintetis", null);
      assertFalse(prefs.contains("sent_morning"));
      assertEquals(0, notifications.getActiveNotifications().length);
    } finally {
      scheduler.cancel(46103);
      for (int i = 0; i < 3; i++) notifications.cancel(46103 + i);
      prefs.edit().clear().commit();
    }
  }
}
