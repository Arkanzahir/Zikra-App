package com.zikra.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import java.util.HashMap;
import java.util.Map;

/** Exercises the foreground flow with an Android framework test GPS provider at Makassar. */
@SuppressWarnings("deprecation")
public final class DeviceLocationTest extends InstrumentationTestCase {
  private Activity page;
  private Map<String, ?> saved;
  private android.content.Context context;
  private android.location.LocationManager manager;
  private boolean mockAdded;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    saved = new HashMap<>(context.getSharedPreferences(LocationStore.PREFS, 0).getAll());
  }

  public void testForegroundGpsUsesReceivedFixAndResolvesItsZone() throws Exception {
    manager =
        (android.location.LocationManager)
            context.getSystemService(android.content.Context.LOCATION_SERVICE);
    manager.addTestProvider(
        android.location.LocationManager.GPS_PROVIDER,
        false,
        false,
        false,
        false,
        true,
        true,
        true,
        android.location.Criteria.POWER_LOW,
        android.location.Criteria.ACCURACY_FINE);
    mockAdded = true;
    manager.setTestProviderEnabled(android.location.LocationManager.GPS_PROVIDER, true);
    android.location.Location fix =
        new android.location.Location(android.location.LocationManager.GPS_PROVIDER);
    fix.setLatitude(-5.1477);
    fix.setLongitude(119.4327);
    fix.setAccuracy(10);
    fix.setTime(System.currentTimeMillis());
    fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    manager.setTestProviderLocation(android.location.LocationManager.GPS_PROVIDER, fix);
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, LocationActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.locationAutomatic).performClick());
    java.lang.reflect.Field field = LocationActivity.class.getDeclaredField("confirmation");
    field.setAccessible(true);
    long end = SystemClock.uptimeMillis() + 35000;
    AlertDialog dialog = null;
    while (SystemClock.uptimeMillis() < end) {
      dialog = (AlertDialog) field.get(page);
      if (dialog != null && dialog.isShowing()) break;
      SystemClock.sleep(150);
    }
    assertNotNull("GPS fix and timezone reach confirmation", dialog);
    AlertDialog chosen = dialog;
    assertNotNull(
        "Timezone service returned a valid zone", chosen.getButton(AlertDialog.BUTTON_POSITIVE));
    getInstrumentation()
        .runOnMainSync(() -> chosen.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
    getInstrumentation().waitForIdleSync();
    LocationPoint point = LocationStore.current(context);
    assertEquals("device", point.source);
    assertEquals(-5.1477, point.latitude, .002);
    assertEquals(119.4327, point.longitude, .002);
    assertEquals("Asia/Makassar", point.zone.getId());
    UiEvidence.capture(getInstrumentation(), "LocationDeviceGpsMakassar");
  }

  @Override
  protected void tearDown() throws Exception {
    if (mockAdded) manager.removeTestProvider(android.location.LocationManager.GPS_PROVIDER);
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    SharedPreferences.Editor e =
        context.getSharedPreferences(LocationStore.PREFS, 0).edit().clear();
    for (Map.Entry<String, ?> item : saved.entrySet()) {
      Object v = item.getValue();
      String k = item.getKey();
      if (v instanceof String) e.putString(k, (String) v);
      else if (v instanceof Long) e.putLong(k, (Long) v);
      else if (v instanceof Float) e.putFloat(k, (Float) v);
    }
    e.commit();
    com.zikra.app.reminders.PrayerReminders.sync(context);
    super.tearDown();
  }
}
