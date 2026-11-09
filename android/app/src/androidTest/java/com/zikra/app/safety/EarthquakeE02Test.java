package com.zikra.app.safety;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.test.InstrumentationTestCase;
import android.widget.TextView;
import com.zikra.app.*;
import com.zikra.app.ui.AppAppearance;
import java.io.File;
import java.time.Instant;
import java.util.*;
import org.json.*;

/** Synthetic notification tests restricted to the backed-up project emulator. */
@SuppressWarnings("deprecation")
public final class EarthquakeE02Test extends InstrumentationTestCase {
  private Context context;
  private NotificationManager manager;
  private Activity page;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(), "e02-emulator-authorized").isFile());
    assertTrue(
        android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk"));
    manager = context.getSystemService(NotificationManager.class);
    manager.cancelAll();
  }

  private Map<String, String> message(Instant now, long age) throws Exception {
    JSONObject quake =
        new JSONObject()
            .put("DateTime", now.minusSeconds(age).toString())
            .put("Wilayah", "UJI E02 — bukan kejadian nyata")
            .put("Magnitude", "0.1")
            .put("Kedalaman", "10 km")
            .put("Coordinates", "0,110");
    EarthquakeReport report =
        EarthquakeReport.network(
            new JSONObject().put("Infogempa", new JSONObject().put("gempa", quake)).toString(),
            now);
    return new HashMap<>(
        Map.of(
            "schema",
            "1",
            "event_id",
            EarthquakeAlertPolicy.id(report),
            "quake_report",
            report.snapshot()));
  }

  private void enabled(boolean value) {
    context
        .getSharedPreferences(EarthquakeAlerts.PREFS, 0)
        .edit()
        .clear()
        .putBoolean("enabled", value)
        .commit();
  }

  private StatusBarNotification[] alerts() {
    return Arrays.stream(manager.getActiveNotifications())
        .filter(n -> EarthquakeAlerts.CHANNEL.equals(n.getNotification().getChannelId()))
        .toArray(StatusBarNotification[]::new);
  }

  private String text(int id) {
    final String[] result = new String[1];
    getInstrumentation()
        .runOnMainSync(() -> result[0] = ((TextView) page.findViewById(id)).getText().toString());
    return result[0];
  }

  public void testOffAndMalformedOrDelayedPushRemainSilent() throws Exception {
    Instant now = Instant.now();
    enabled(false);
    assertFalse(EarthquakeAlerts.receive(context, message(now, 5), now));
    enabled(true);
    assertFalse(EarthquakeAlerts.receive(context, message(now, 3601), now));
    Map<String, String> bad = message(now, 5);
    bad.put("event_id", "0".repeat(64));
    assertFalse(EarthquakeAlerts.receive(context, bad, now));
    assertEquals(0, alerts().length);
    assertEquals(
        "[]", context.getSharedPreferences(EarthquakeAlerts.PREFS, 0).getString("seen", "[]"));
  }

  public void testDeniedPermissionRemainsSilent() throws Exception {
    assertFalse("Harness must deny notification permission", EarthquakeAlerts.permission(context));
    enabled(true);
    assertFalse(EarthquakeAlerts.receive(context, message(Instant.now(), 5), Instant.now()));
    assertEquals(0, alerts().length);
  }

  public void testDuplicatePersistsAndTapOpensSelectedReport() throws Exception {
    assertTrue(EarthquakeAlerts.permission(context));
    enabled(true);
    Instant now = Instant.now();
    Map<String, String> data = message(now, 120);
    assertTrue(EarthquakeAlerts.receive(context, data, now));
    assertFalse(EarthquakeAlerts.receive(context, data, now));
    SystemClock.sleep(200);
    assertEquals(1, alerts().length);
    StatusBarNotification notification = alerts()[0];
    assertEquals(data.get("event_id"), notification.getTag());
    assertEquals(
        R.drawable.ic_notification_prayer,
        notification.getNotification().getSmallIcon().getResId());
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(EarthquakeActivity.class.getName(), null, false);
    notification.getNotification().contentIntent.send();
    page = monitor.waitForActivityWithTimeout(15000);
    assertNotNull(page);
    getInstrumentation().removeMonitor(monitor);
    assertEquals(data.get("quake_report"), page.getIntent().getStringExtra("quake_report"));
    assertEquals("0.1", text(R.id.quakeMagnitude));
    assertTrue(text(R.id.quakeArea).contains("UJI E02"));
    assertEquals(
        android.graphics.Color.WHITE,
        ((TextView) page.findViewById(R.id.quakeSource)).getCurrentTextColor());
    assertEquals(
        page.getColor(R.color.protector_reference_ink),
        ((TextView) page.findViewById(R.id.quakeTabShaking)).getCurrentTextColor());
    getInstrumentation().runOnMainSync(() -> page.recreate());
    SystemClock.sleep(500);
    assertFalse(EarthquakeAlerts.receive(context, data, now.plusSeconds(1)));
    try (java.io.FileOutputStream out =
        context.openFileOutput("e02-restart-message.json", Context.MODE_PRIVATE)) {
      out.write(new JSONObject(data).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  public void testDuplicateAfterActualProcessRestartRemainsSilent() throws Exception {
    assertTrue(EarthquakeAlerts.enabled(context));
    JSONObject saved;
    try (java.io.FileInputStream in = context.openFileInput("e02-restart-message.json")) {
      saved =
          new JSONObject(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
    Map<String, String> data = new HashMap<>();
    for (String key : new String[] {"schema", "event_id", "quake_report"})
      data.put(key, saved.getString(key));
    assertFalse(EarthquakeAlerts.receive(context, data, Instant.now()));
    assertEquals(0, alerts().length);
  }

  public void testRegisterOwnedEmulatorForRealFcmDelivery() throws Exception {
    assertTrue(EarthquakeAlerts.permission(context));
    EarthquakeAlerts.setEnabled(context, true);
    String token =
        com.google.android.gms.tasks.Tasks.await(
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken(),
            60,
            java.util.concurrent.TimeUnit.SECONDS);
    assertTrue(token.length() > 20);
    com.google.android.gms.tasks.Tasks.await(
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic("zikra-earthquake-beta-v1"),
        60,
        java.util.concurrent.TimeUnit.SECONDS);
    try (java.io.FileOutputStream out =
        context.openFileOutput("e02-token-private", Context.MODE_PRIVATE)) {
      out.write(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    assertTrue(EarthquakeAlerts.enabled(context));
  }

  public void testRemoveOwnedEmulatorCloudRegistration() throws Exception {
    EarthquakeAlerts.setEnabled(context, false);
    com.google.android.gms.tasks.Tasks.await(
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .unsubscribeFromTopic("zikra-earthquake-beta-v1"),
        60,
        java.util.concurrent.TimeUnit.SECONDS);
    com.google.android.gms.tasks.Tasks.await(
        com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken(),
        60,
        java.util.concurrent.TimeUnit.SECONDS);
    com.google.android.gms.tasks.Tasks.await(
        com.google.firebase.installations.FirebaseInstallations.getInstance().delete(),
        60,
        java.util.concurrent.TimeUnit.SECONDS);
    assertFalse(EarthquakeAlerts.enabled(context));
  }

  public void testOptOutCancelsOnlyEarthquakeNotifications() throws Exception {
    assertTrue(EarthquakeAlerts.permission(context));
    enabled(true);
    manager.createNotificationChannel(
        new NotificationChannel(
            "e02_other_fixture", "UJI kanal lain", NotificationManager.IMPORTANCE_LOW));
    manager.notify(
        "e02-other",
        2,
        new Notification.Builder(context, "e02_other_fixture")
            .setSmallIcon(R.drawable.ic_notification_prayer)
            .setContentTitle("UJI E02 kanal lain")
            .build());
    assertTrue(EarthquakeAlerts.receive(context, message(Instant.now(), 10), Instant.now()));
    EarthquakeAlerts.setEnabled(context, false);
    assertFalse(EarthquakeAlerts.enabled(context));
    long deadline = SystemClock.elapsedRealtime() + 3000;
    while (alerts().length != 0 && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50);
    assertEquals(0, alerts().length);
    assertTrue(
        Arrays.stream(manager.getActiveNotifications())
            .anyMatch(n -> "e02-other".equals(n.getTag())));
    manager.cancel("e02-other", 2);
    manager.deleteNotificationChannel("e02_other_fixture");
  }

  public void testExpiredNotificationCannotShowUnrelatedLatestReport() throws Exception {
    Map<String, String> stale = message(Instant.now().minusSeconds(90000), 5);
    Map<String, String> latest = message(Instant.now(), 5);
    context
        .getSharedPreferences("bmkg_earthquake_v1", 0)
        .edit()
        .putString("snapshot", latest.get("quake_report"))
        .commit();
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, EarthquakeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("quake_notification", true)
                    .putExtra("quake_report", stale.get("quake_report")));
    SystemClock.sleep(400);
    assertEquals(context.getString(R.string.quake_alert_expired_title), text(R.id.quakeArea));
    assertFalse(page.findViewById(R.id.quakeOpenMap).isEnabled());
    UiEvidence.capture(getInstrumentation(), "E02-expired-report");
  }

  public void testSettingsBothThemesRetainOffAndNavigation() throws Exception {
    enabled(false);
    assertTrue(EarthquakeAlerts.configured(context));
    for (String theme : new String[] {"light", "dark"}) {
      context
          .getSharedPreferences(AppAppearance.PREFS, 0)
          .edit()
          .putString(AppAppearance.KEY, theme)
          .commit();
      page =
          getInstrumentation()
              .startActivitySync(
                  new Intent(context, EarthquakeAlertsActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      getInstrumentation().waitForIdleSync();
      assertEquals(context.getString(R.string.quake_alert_off), text(R.id.quakeAlertStatus));
      assertNotNull(page.findViewById(R.id.quakeAlertAndroidSettings));
      assertFalse(((android.widget.Switch) page.findViewById(R.id.quakeAlertSwitch)).isChecked());
      UiEvidence.capture(getInstrumentation(), "E02-settings-" + theme);
      android.view.View button = page.findViewById(R.id.quakeAlertAndroidSettings);
      android.view.ViewParent parent = button.getParent();
      while (!(parent instanceof android.widget.ScrollView)) parent = parent.getParent();
      android.widget.ScrollView scroll = (android.widget.ScrollView) parent;
      getInstrumentation()
          .runOnMainSync(() -> scroll.scrollTo(0, scroll.getChildAt(0).getHeight()));
      getInstrumentation().waitForIdleSync();
      android.graphics.Rect bounds = new android.graphics.Rect();
      assertTrue(button.getGlobalVisibleRect(bounds));
      assertTrue(bounds.width() > 0 && bounds.height() >= button.getHeight() - 2);
      UiEvidence.capture(getInstrumentation(), "E02-settings-bottom-" + theme);
      Instrumentation.ActivityMonitor settings =
          getInstrumentation()
              .addMonitor(
                  new IntentFilter(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS),
                  null,
                  true);
      long at = SystemClock.uptimeMillis();
      for (int action :
          new int[] {android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP}) {
        android.view.MotionEvent touch =
            android.view.MotionEvent.obtain(
                at,
                SystemClock.uptimeMillis(),
                action,
                bounds.exactCenterX(),
                bounds.exactCenterY(),
                0);
        getInstrumentation().sendPointerSync(touch);
        touch.recycle();
      }
      long deadline = SystemClock.elapsedRealtime() + 5000;
      while (settings.getHits() == 0 && SystemClock.elapsedRealtime() < deadline)
        SystemClock.sleep(50);
      assertEquals(1, settings.getHits());
      getInstrumentation().removeMonitor(settings);
      getInstrumentation().runOnMainSync(() -> page.finish());
    }
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null && !page.isFinishing())
      getInstrumentation().runOnMainSync(() -> page.finish());
    manager.cancelAll();
    super.tearDown();
  }
}
