package com.zikra.app.safety;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.EarthquakeActivity;
import com.zikra.app.ProtectorActivity;
import com.zikra.app.R;
import com.zikra.app.UiEvidence;
import com.zikra.app.WeatherActivity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;

/** Exercises production data binding with labelled test fixtures, without live API access. */
@SuppressWarnings("deprecation")
public final class BmkgDesignTest extends InstrumentationTestCase {
  private Activity page;
  private static final String QUAKE =
      """
      {"Infogempa":{"gempa":{
        "DateTime":"2026-10-17T03:23:10+00:00","Magnitude":"5.1","Kedalaman":"10 km",
        "Wilayah":"Laut selatan Jawa (data uji)","Coordinates":"-8.55,112.30",
        "Potensi":"Contoh informasi potensi dari fixture pengujian",
        "Dirasakan":"Contoh wilayah dirasakan (data uji)"
      }}}
      """;
  private static final String FORECAST =
      """
{"data":[{"lokasi":{"adm4":"35.78.09.1001","desa":"Keputih",
"kecamatan":"Sukolilo","kotkab":"Kota Surabaya"},"cuaca":[[
  {"utc_datetime":"2026-10-17 03:00:00","t":27,"weather_desc":"Cerah Berawan","hu":72,"ws":12,"wd":"SE"},
  {"utc_datetime":"2026-10-17 06:00:00","t":29,"weather_desc":"Cerah","hu":70,"ws":10,"wd":"SE"},
  {"utc_datetime":"2026-10-17 09:00:00","t":31,"weather_desc":"Berawan","hu":69,"ws":9,"wd":"S"},
  {"utc_datetime":"2026-10-17 12:00:00","t":30,"weather_desc":"Hujan Ringan","hu":80,"ws":11,"wd":"SE"},
  {"utc_datetime":"2026-10-17 15:00:00","t":28,"weather_desc":"Berawan","hu":78,"ws":8,"wd":"S"},
  {"utc_datetime":"2026-10-17 18:00:00","t":26,"weather_desc":"Berawan","hu":77,"ws":8,"wd":"S"}
]]}]}
""";

  private void launch(Class<? extends Activity> type) throws Exception {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    Field client = type.getDeclaredField("bmkg");
    client.setAccessible(true);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                ((BmkgClient) client.get(page)).close();
              } catch (IllegalAccessException e) {
                throw new AssertionError(e);
              }
              TextView label = page.findViewById(R.id.protectorTitle);
              if (label != null) label.setText("Protector · DATA UJI");
            });
  }

  private void bind(String name, Class<?> type, Object value) throws Exception {
    Method method = page.getClass().getDeclaredMethod(name, type);
    method.setAccessible(true);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                method.invoke(page, value);
              } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
              }
            });
  }

  private void rendered(String screenshot) {
    long deadline = SystemClock.uptimeMillis() + 30000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
    assertTrue("Activity must acquire window focus", page.hasWindowFocus());
    getInstrumentation().waitForIdleSync();
    UiEvidence.capture(getInstrumentation(), screenshot);
  }

  public void testDashboardBindsWeatherAndQuakeIntoCompactCards() throws Exception {
    launch(ProtectorActivity.class);
    BmkgParser.Forecast forecast =
        BmkgParser.forecast(FORECAST, Instant.parse("2026-10-17T02:30:00Z"));
    bind("showWeather", BmkgParser.Forecast.class, forecast);
    bind("showQuake", BmkgParser.Earthquake.class, BmkgParser.earthquake(QUAKE));
    rendered("ProtectorDesignFixture");
    assertEquals("27°", ((TextView) page.findViewById(R.id.protectorWeatherTemperature)).getText());
    assertEquals("5.1", ((TextView) page.findViewById(R.id.protectorMagnitude)).getText());
    assertTrue(((EarthquakeMapView) page.findViewById(R.id.protectorQuakeMap)).hasEpicenter());
  }

  public void testForecastBindsEveryDatedSlotIntoXmlCards() throws Exception {
    launch(WeatherActivity.class);
    bind(
        "show",
        BmkgParser.Forecast.class,
        BmkgParser.forecast(FORECAST, Instant.parse("2026-10-17T02:30:00Z")));
    rendered("WeatherDesignFixture");
    LinearLayout list = page.findViewById(R.id.weatherForecastList);
    assertEquals(6, list.getChildCount());
    assertEquals(
        "27°C", ((TextView) list.getChildAt(0).findViewById(R.id.forecastTemperature)).getText());
    assertEquals("72%", ((TextView) page.findViewById(R.id.weatherHumidity)).getText());
  }

  public void testQuakeBindsTimestampCoordinatesAndMapAction() throws Exception {
    launch(EarthquakeActivity.class);
    bind("show", BmkgParser.Earthquake.class, BmkgParser.earthquake(QUAKE));
    rendered("EarthquakeDesignFixture");
    assertEquals("10:23", ((TextView) page.findViewById(R.id.quakeClock)).getText());
    String coords = ((TextView) page.findViewById(R.id.quakeCoordinates)).getText().toString();
    assertTrue(coords.contains("LS"));
    assertTrue(coords.contains("BT"));
    assertTrue(page.findViewById(R.id.quakeOpenMap).isEnabled());
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
