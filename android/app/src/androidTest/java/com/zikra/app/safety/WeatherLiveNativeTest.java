package com.zikra.app.safety;

import android.app.Activity;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import com.zikra.app.CalendarActivity;
import com.zikra.app.location.LocationStore;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Real forecast repository on owned emulator; only the default public city is queried. */
@SuppressWarnings("deprecation")
public final class WeatherLiveNativeTest extends InstrumentationTestCase {
  public void testRealForecastAndFreshCache() throws Exception {
    android.content.Context context = getInstrumentation().getTargetContext();
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    assertTrue(new File(context.getFilesDir(), "ui11-emulator-authorized").isFile());
    assertTrue("No personal saved location", context.getSharedPreferences("zikra_location", 0).getAll().isEmpty());
    context.getSharedPreferences("zikra_weather_openmeteo_v2", 0).edit().clear().commit();
    Activity page = getInstrumentation().startActivitySync(new Intent(context, CalendarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long deadline = android.os.SystemClock.uptimeMillis() + 5000;
    while (!page.hasWindowFocus() && android.os.SystemClock.uptimeMillis() < deadline) android.os.SystemClock.sleep(50);
    assertTrue(page.hasWindowFocus());
    try (OpenMeteoClient client = new OpenMeteoClient(page)) {
      for (int attempt = 0; attempt < 2; attempt++) {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<BmkgParser.Forecast> result = new AtomicReference<>();
        client.load(new BmkgClient.Result<BmkgParser.Forecast>() {
          @Override public void success(BmkgParser.Forecast forecast) { result.set(forecast); done.countDown(); }
          @Override public void failure(String message) { done.countDown(); }
        });
        assertTrue("Weather callback completed", done.await(45, TimeUnit.SECONDS));
        assertNotNull("Real forecast, not a fixture or silent failure", result.get());
        assertTrue(result.get().slots.size() >= 24);
        assertEquals("Open-Meteo", result.get().source);
        if (attempt == 0) assertFalse(result.get().stored);
        else assertTrue(result.get().stored);
        String prefix = LocationStore.current(context).key() + ":";
        assertTrue(context.getSharedPreferences("zikra_weather_openmeteo_v2", 0).getLong(prefix + "fetched_at", 0) > 0);
      }
    } finally { getInstrumentation().runOnMainSync(page::finish); }
  }
}
