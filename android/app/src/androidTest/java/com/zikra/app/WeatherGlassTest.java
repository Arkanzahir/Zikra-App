package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.*;
import com.zikra.app.safety.*;
import com.zikra.app.ui.AppAppearance;
import java.io.*;
import java.lang.reflect.*;
import java.time.Instant;

/** Deterministic layout/data regression; runs only on the backed-up project emulator. */
@SuppressWarnings("deprecation")
public final class WeatherGlassTest extends InstrumentationTestCase {
  private Activity page;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    assertTrue(
        "Owned emulator marker required",
        new File(getInstrumentation().getTargetContext().getFilesDir(), "ui11-emulator-authorized")
            .isFile());
    assertTrue(
        android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk"));
    // Backed up by the harness; prevent any saved personal location reaching a provider.
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_location", 0)
        .edit()
        .clear()
        .commit();
  }

  private BmkgParser.Forecast fixture() throws Exception {
    try (InputStream in =
            getInstrumentation().getContext().getAssets().open("openmeteo-sukolilo.json");
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int read;
      while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
      Instant now = Instant.parse("2026-10-18T05:00:00Z");
      return OpenMeteoParser.parse(out.toString("UTF-8"), now, now, true, false);
    }
  }

  private void launch(boolean dark, Class<? extends Activity> type) {
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences(AppAppearance.PREFS, 0)
        .edit()
        .putString(AppAppearance.KEY, dark ? "dark" : "light")
        .commit();
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }

  private void showFixture() throws Exception {
    BmkgParser.Forecast forecast = fixture();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                Field field =
                    page.getClass()
                        .getDeclaredField(page instanceof WeatherActivity ? "client" : "bmkg");
                field.setAccessible(true);
                ((BmkgClient) field.get(page)).reset();
                Method show =
                    page.getClass()
                        .getDeclaredMethod(
                            page instanceof WeatherActivity ? "show" : "showWeather",
                            BmkgParser.Forecast.class);
                show.setAccessible(true);
                show.invoke(page, forecast);
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
    getInstrumentation().waitForIdleSync();
  }

  private String text(int id) {
    return ((TextView) page.findViewById(id)).getText().toString();
  }

  public void testLightForecastHoursDailyGraphAndDock() throws Exception {
    checkDetail(false, "UI11-weather-light");
  }

  public void testDarkForecastHoursDailyGraphAndDock() throws Exception {
    checkDetail(true, "UI11-weather-dark");
  }

  private void checkDetail(boolean dark, String screenshot) throws Exception {
    launch(dark, WeatherActivity.class);
    showFixture();
    assertEquals(5, ((LinearLayout) page.findViewById(R.id.weatherForecastList)).getChildCount());
    assertEquals(3, ((LinearLayout) page.findViewById(R.id.weatherDailyList)).getChildCount());
    assertTrue(text(R.id.weatherStatus).contains("tersimpan"));
    int background =
        ((ColorDrawable) page.findViewById(R.id.protectorMainDock).getBackground()).getColor();
    assertEquals(page.getColor(R.color.app_surface), background);
    assertTrue(text(R.id.weatherForecastNote).contains("WIB"));
    getInstrumentation()
        .runOnMainSync(
            () ->
                ((LinearLayout) page.findViewById(R.id.weatherForecastList))
                    .getChildAt(2)
                    .performClick());
    assertTrue(text(R.id.weatherSelectedHour).contains("14:00"));
    assertEquals(View.GONE, page.findViewById(R.id.weatherChart).getVisibility());
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.weatherGraphToggle).performClick());
    assertEquals(View.VISIBLE, page.findViewById(R.id.weatherChart).getVisibility());
    assertTrue(
        page.findViewById(R.id.weatherChart)
            .getContentDescription()
            .toString()
            .contains("Suhu prakiraan"));
    SystemClock.sleep(250);
    UiEvidence.capture(getInstrumentation(), screenshot);
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.weatherGraphToggle).performClick());
    UiEvidence.capture(getInstrumentation(), screenshot + "-collapsed");
    getInstrumentation()
        .runOnMainSync(
            () -> ((ScrollView) page.findViewById(R.id.weatherScroll)).fullScroll(View.FOCUS_DOWN));
    getInstrumentation().waitForIdleSync();
    UiEvidence.capture(getInstrumentation(), screenshot + "-bottom");
    assertNotNull(page.findViewById(R.id.protectorNavQuran));
    assertTrue(page.findViewById(R.id.toolbarBack).isClickable());
  }

  public void testEmptyFailureDoesNotKeepDailyOrGraph() throws Exception {
    launch(false, WeatherActivity.class);
    showFixture();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                Field value = WeatherActivity.class.getDeclaredField("displayed");
                value.setAccessible(true);
                value.set(page, null);
                Method fail = WeatherActivity.class.getDeclaredMethod("showFailure", String.class);
                fail.setAccessible(true);
                fail.invoke(page, "Tidak ada koneksi");
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
    assertEquals(0, ((LinearLayout) page.findViewById(R.id.weatherForecastList)).getChildCount());
    assertEquals(View.GONE, page.findViewById(R.id.weatherDailyPanel).getVisibility());
    assertEquals(View.GONE, page.findViewById(R.id.weatherGraphPanel).getVisibility());
    assertEquals(View.VISIBLE, page.findViewById(R.id.weatherRetry).getVisibility());
    assertTrue(text(R.id.weatherTemperature).contains("—"));
    UiEvidence.capture(getInstrumentation(), "UI11-weather-empty");
  }

  public void testOverviewForecastKeepsEarthquakeMap() throws Exception {
    launch(false, ProtectorActivity.class);
    showFixture();
    assertEquals(
        4, ((LinearLayout) page.findViewById(R.id.protectorForecastSlots)).getChildCount());
    assertNotNull(page.findViewById(R.id.protectorQuakeMap));
    assertNotNull(page.findViewById(R.id.protectorMagnitude));
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.protectorScroll).scrollTo(0, 0));
    SystemClock.sleep(250);
    UiEvidence.capture(getInstrumentation(), "UI11-protector");
    android.app.Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(WeatherActivity.class.getName(), null, false);
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.protectorForecastOpen).performClick());
    Activity detail = getInstrumentation().waitForMonitorWithTimeout(monitor, 5000);
    getInstrumentation().removeMonitor(monitor);
    assertNotNull("Forecast detail opens", detail);
    getInstrumentation().runOnMainSync(detail::finish);
    getInstrumentation().waitForIdleSync();
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
