package com.zikra.app.safety;

import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.test.InstrumentationTestCase;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.zikra.app.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Explicitly authorized phone and token-only FCM delivery; no server key in this package. */
@SuppressWarnings("deprecation")
public final class PhoneEarthquakeDeliveryTest extends InstrumentationTestCase {
  private Context context;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertFalse(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk"));
    assertTrue(new File(context.getFilesDir(), "phone-closeout-20261110-backup-verified").isFile());
  }

  private void write(String name, JSONObject value) throws Exception {
    Files.write(
        new File(context.getFilesDir(), name).toPath(),
        value.toString(2).getBytes(StandardCharsets.UTF_8));
  }

  public void testRegisterNewPhoneOptIn() throws Exception {
    SharedPreferences prefs = context.getSharedPreferences(EarthquakeAlerts.PREFS, 0);
    assertTrue(
        "Session requires a new opt-in, preserving an existing subscriber",
        prefs.getAll().isEmpty());
    assertTrue(EarthquakeAlerts.configured(context));
    assertTrue(
        "Notification permission must already be granted by the user",
        EarthquakeAlerts.allowed(context));
    EarthquakeAlerts.setEnabled(context, true);
    String token = Tasks.await(FirebaseMessaging.getInstance().getToken(), 75, TimeUnit.SECONDS);
    Tasks.await(
        FirebaseMessaging.getInstance().subscribeToTopic("zikra-earthquake-beta-v1"),
        75,
        TimeUnit.SECONDS);
    assertTrue(token.length() > 20);
    Files.write(
        new File(context.getFilesDir(), "phone-e02-token-private").toPath(),
        token.getBytes(StandardCharsets.UTF_8));
    write(
        "phone-e02-register.json",
        new JSONObject()
            .put("physicalPhone", true)
            .put("optIn", true)
            .put("realFirebaseRegistration", true)
            .put("topicSubscribed", true)
            .put("passed", true));
  }

  public void testInspectRealCloudNotification() throws Exception {
    String id =
        new String(
                Files.readAllBytes(new File(context.getFilesDir(), "phone-e02-event-id").toPath()),
                StandardCharsets.UTF_8)
            .trim();
    assertTrue(id.matches("[a-f0-9]{64}"));
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    StatusBarNotification selected = null;
    for (StatusBarNotification candidate : manager.getActiveNotifications())
      if (id.equals(candidate.getTag())) selected = candidate;
    assertNotNull("Token-targeted cloud message must reach the phone", selected);
    Notification notice = selected.getNotification();
    assertTrue(notice.extras.getString(Notification.EXTRA_TEXT).contains("UJI NOTIFIKASI ZIKRA"));
    assertEquals(EarthquakeAlerts.CHANNEL, notice.getChannelId());
    write(
        "phone-e02-notification.json",
        new JSONObject()
            .put("physicalPhone", true)
            .put("eventId", id)
            .put("postTime", selected.getPostTime())
            .put("when", notice.when)
            .put("channel", notice.getChannelId())
            .put("testLabelVisible", true)
            .put("actualFcmReceived", true)
            .put("passed", true));
  }

  public void testSelectedReportOpenedFromNotification() throws Exception {
    String id =
        new String(
                Files.readAllBytes(new File(context.getFilesDir(), "phone-e02-event-id").toPath()),
                StandardCharsets.UTF_8)
            .trim();
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    StatusBarNotification selected = null;
    for (StatusBarNotification candidate : manager.getActiveNotifications())
      if (id.equals(candidate.getTag())) selected = candidate;
    assertNotNull(selected);
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(EarthquakeActivity.class.getName(), null, false);
    Activity page = null;
    try {
      selected.getNotification().contentIntent.send();
      page = monitor.waitForActivityWithTimeout(15000);
      assertNotNull(page);
      assertEquals("zikra-internal://earthquake/" + id, page.getIntent().getDataString());
      EarthquakeReport report =
          EarthquakeReport.cached(
              page.getIntent().getStringExtra("quake_report"), java.time.Instant.now());
      assertEquals(id, EarthquakeAlertPolicy.id(report));
      assertTrue(report.quake.area.contains("UJI NOTIFIKASI ZIKRA"));
      write(
          "phone-e02-open.json",
          new JSONObject()
              .put("physicalPhone", true)
              .put("sameEventId", true)
              .put("realNotificationContentIntent", true)
              .put("passed", true));
    } finally {
      getInstrumentation().removeMonitor(monitor);
      if (page != null) {
        Activity finish = page;
        getInstrumentation().runOnMainSync(finish::finish);
      }
    }
  }

  public void testRemoveOnlyNewTestRegistration() throws Exception {
    SharedPreferences prefs = context.getSharedPreferences(EarthquakeAlerts.PREFS, 0);
    assertTrue(new File(context.getFilesDir(), "phone-e02-token-private").isFile());
    EarthquakeAlerts.setEnabled(context, false);
    Tasks.await(
        FirebaseMessaging.getInstance().unsubscribeFromTopic("zikra-earthquake-beta-v1"),
        60,
        TimeUnit.SECONDS);
    Tasks.await(FirebaseMessaging.getInstance().deleteToken(), 60, TimeUnit.SECONDS);
    Tasks.await(FirebaseInstallations.getInstance().delete(), 60, TimeUnit.SECONDS);
    prefs.edit().clear().commit();
    assertFalse(EarthquakeAlerts.enabled(context));
    write(
        "phone-e02-cleanup.json",
        new JSONObject()
            .put("ownNewRegistrationRemoved", true)
            .put("originalOptInOffRestored", true)
            .put("otherNotificationsCanceled", false)
            .put("passed", true));
  }
}
