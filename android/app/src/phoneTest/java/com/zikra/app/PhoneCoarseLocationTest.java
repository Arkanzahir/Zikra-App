package com.zikra.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CancellationSignal;
import android.os.Looper;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Physical coarse-only diagnostic. Never changes saved locations or publishes coordinates. */
@SuppressWarnings("deprecation")
public final class PhoneCoarseLocationTest extends InstrumentationTestCase {
  public void testActualCoarseProvidersWithoutChangingSavedLocation() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    assertTrue(android.os.Build.VERSION.SDK_INT >= 31);
    assertTrue(new File(context.getFilesDir(), "phone-d01-backup-verified").isFile());
    assertEquals(
        PackageManager.PERMISSION_GRANTED,
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
    assertEquals(
        PackageManager.PERMISSION_DENIED,
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));
    LocationManager manager = context.getSystemService(LocationManager.class);
    Map<String, ?> savedLocation =
        context.getSharedPreferences(com.zikra.app.location.LocationStore.PREFS, 0).getAll();
    android.app.Activity page =
        getInstrumentation()
            .startActivitySync(
                new android.content.Intent(context, LocationActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    Map<String, AtomicBoolean> fixes = new LinkedHashMap<>();
    Map<String, LocationListener> listeners = new LinkedHashMap<>();
    JSONObject result = new JSONObject();
    CancellationSignal signal = new CancellationSignal();
    try {
      for (String provider : new String[] {"network", "gps", "fused"}) {
        AtomicBoolean received = new AtomicBoolean();
        fixes.put(provider, received);
        JSONObject state = new JSONObject();
        result.put(provider, state);
        state.put("available", manager.getAllProviders().contains(provider));
        if (!manager.getAllProviders().contains(provider)) continue;
        state.put("enabled", manager.isProviderEnabled(provider));
        try {
          Location cached = manager.getLastKnownLocation(provider);
          state.put("cachedFixAvailable", cached != null);
          if (cached != null)
            state.put(
                "cachedFixRecent",
                SystemClock.elapsedRealtimeNanos() - cached.getElapsedRealtimeNanos()
                    < 180_000_000_000L);
          LocationListener listener =
              new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                  if (location != null && !location.isMock()) received.set(true);
                }
              };
          listeners.put(provider, listener);
          getInstrumentation()
              .runOnMainSync(
                  () -> manager.requestSingleUpdate(provider, listener, Looper.getMainLooper()));
          state.put("requestAccepted", true);
        } catch (SecurityException | IllegalArgumentException unavailable) {
          state.put("requestAccepted", false);
          state.put("reason", unavailable.getClass().getSimpleName());
        }
      }
      AtomicBoolean current = new AtomicBoolean();
      if (manager.getAllProviders().contains("fused")) {
        manager.getCurrentLocation(
            "fused",
            signal,
            context.getMainExecutor(),
            location -> {
              if (location != null && !location.isMock()) current.set(true);
            });
      }
      long deadline = SystemClock.elapsedRealtime() + 27000;
      while (SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(150);
      for (Map.Entry<String, AtomicBoolean> entry : fixes.entrySet())
        result.getJSONObject(entry.getKey()).put("freshNonMockFix", entry.getValue().get());
      result.put("fusedCurrentNonMockFix", current.get());
      boolean changed =
          !savedLocation.equals(
              context.getSharedPreferences(com.zikra.app.location.LocationStore.PREFS, 0).getAll());
      result.put("savedLocationChanged", changed);
      result.put("foregroundLocationActivity", true);
      result.put("coordinatesPublished", false);
      Files.write(
          new File(context.getFilesDir(), "phone-d01-coarse-result.json").toPath(),
          result.toString(2).getBytes(StandardCharsets.UTF_8));
      assertFalse("Diagnostic must preserve the saved location", changed);
    } finally {
      signal.cancel();
      for (LocationListener listener : listeners.values()) manager.removeUpdates(listener);
      getInstrumentation().runOnMainSync(page::finish);
    }
  }
}
