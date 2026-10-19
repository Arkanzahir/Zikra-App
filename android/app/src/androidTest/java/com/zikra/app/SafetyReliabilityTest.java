package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.safety.BmkgClient;
import com.zikra.app.safety.BmkgParser;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;

/** Deterministic native state regression checks; fixture data never enters production storage. */
@SuppressWarnings("deprecation")
public final class SafetyReliabilityTest extends InstrumentationTestCase {
  private Activity page;

  private void launch(Class<? extends Activity> type) throws Exception {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    if (type == WeatherActivity.class || type == EarthquakeActivity.class) {
      Field field = type.getDeclaredField("bmkg");
      field.setAccessible(true);
      getInstrumentation()
          .runOnMainSync(
              () -> {
                try {
                  ((BmkgClient) field.get(page)).close();
                } catch (Exception failure) {
                  throw new AssertionError(failure);
                }
              });
    }
  }

  private void invoke(String name, Class<?> parameter, Object value) throws Exception {
    Method method =
        parameter == null
            ? page.getClass().getDeclaredMethod(name)
            : page.getClass().getDeclaredMethod(name, parameter);
    method.setAccessible(true);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                if (parameter == null) method.invoke(page);
                else method.invoke(page, value);
              } catch (Exception failure) {
                throw new AssertionError(failure);
              }
            });
  }

  private String text(int id) {
    String[] value = {""};
    getInstrumentation()
        .runOnMainSync(() -> value[0] = ((TextView) page.findViewById(id)).getText().toString());
    return value[0];
  }

  private BmkgParser.Forecast forecast(Instant fetched) {
    BmkgParser.Weather slot =
        new BmkgParser.Weather(
            "Sukolilo",
            "Surabaya",
            "Surabaya",
            "29",
            "Cerah berawan",
            "75",
            "8",
            Instant.now().plusSeconds(900),
            false,
            "Timur",
            "");
    return new BmkgParser.Forecast(
        slot, Collections.singletonList(slot), "Open-Meteo", fetched, true, false);
  }

  public void testWeatherPreservesDatedValuesAfterFailureButRejectsExpiredData() throws Exception {
    launch(WeatherActivity.class);
    invoke("show", BmkgParser.Forecast.class, forecast(Instant.now()));
    String date = text(R.id.weatherDate);
    invoke("refresh", null, null);
    assertEquals("29°C", text(R.id.weatherTemperature));
    assertEquals(date, text(R.id.weatherDate));
    assertTrue(text(R.id.weatherStatus).contains("Memperbarui"));
    invoke("showFailure", String.class, "Offline");
    assertEquals("29°C", text(R.id.weatherTemperature));
    assertEquals(date, text(R.id.weatherDate));
    assertTrue(text(R.id.weatherStatus).contains("Pembaruan gagal"));
    assertTrue(text(R.id.weatherStatus).contains("Diambil"));
    assertEquals(1, ((LinearLayout) page.findViewById(R.id.weatherForecastList)).getChildCount());

    invoke("show", BmkgParser.Forecast.class, forecast(Instant.now().minusSeconds(7 * 3600)));
    invoke("refresh", null, null);
    invoke("showFailure", String.class, "Offline");
    assertEquals("—°C", text(R.id.weatherTemperature));
    assertEquals(0, ((LinearLayout) page.findViewById(R.id.weatherForecastList)).getChildCount());
    assertTrue(text(R.id.weatherDescription).contains("belum tersedia"));
  }

  public void testEarthquakeRefreshAndFailureKeepReportAndValidCoordinates() throws Exception {
    launch(EarthquakeActivity.class);
    BmkgParser.Earthquake report =
        new BmkgParser.Earthquake(
            "5.1",
            "Laut Jawa",
            "10 km",
            "Tidak berpotensi tsunami",
            "-",
            "",
            Instant.parse("2026-10-18T12:00:00Z"),
            -6.1,
            112.3,
            "",
            "");
    invoke("show", BmkgParser.Earthquake.class, report);
    String eventTime = text(R.id.quakeTime);
    String coordinates = text(R.id.quakeCoordinates);
    invoke("refresh", null, null);
    assertEquals("5.1", text(R.id.quakeMagnitude));
    assertEquals(eventTime, text(R.id.quakeTime));
    assertEquals(coordinates, text(R.id.quakeCoordinates));
    assertTrue(page.findViewById(R.id.quakeOpenMap).isEnabled());
    invoke("showFailure", String.class, "Offline");
    assertEquals("Laut Jawa", text(R.id.quakeArea));
    assertTrue(text(R.id.quakeStatus).contains("Diambil"));
    assertTrue(text(R.id.quakeStatus).contains("Pembaruan gagal"));
    assertTrue(text(R.id.quakeStatus).contains("Bukan peringatan"));

    invoke(
        "show",
        BmkgParser.Earthquake.class,
        new BmkgParser.Earthquake("4.2", "Laut", "", "", "", "", Instant.now()));
    assertFalse(page.findViewById(R.id.quakeOpenMap).isEnabled());
    assertTrue(text(R.id.quakeCoordinates).contains("belum tersedia"));
  }

  public void testPrayerUsesReferenceHeaderAndAccessibleReminderTargets() throws Exception {
    launch(PrayerActivity.class);
    assertTrue(text(R.id.featureSubtitle).contains("Acuan Sukolilo"));
    float density = page.getResources().getDisplayMetrics().density;
    assertTrue(page.findViewById(R.id.prayerTodayTab).getHeight() >= 48 * density - 1);
    LinearLayout rows = page.findViewById(R.id.prayerRows);
    assertEquals(5, rows.getChildCount());
    assertTrue(rows.getChildAt(0).findViewById(R.id.prayerRowBell).getWidth() >= 48 * density - 1);
    assertTrue(
        page.findViewById(R.id.prayerOpenQibla)
            .getContentDescription()
            .toString()
            .contains("kiblat"));
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
