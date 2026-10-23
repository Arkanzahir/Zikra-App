package com.zikra.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.widget.*;
import com.zikra.app.safety.*;
import java.io.*;
import java.time.*;

@SuppressWarnings("deprecation")
public final class RebuiltWeatherTest extends InstrumentationTestCase {
  private Activity page;

  private void launch(Class<? extends Activity> type) {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }

  private void finishPage() {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    page = null;
  }

  private void awaitText(int id, String token) {
    long end = SystemClock.uptimeMillis() + 35000;
    while (SystemClock.uptimeMillis() < end) {
      boolean[] okay = {false};
      getInstrumentation()
          .runOnMainSync(
              () ->
                  okay[0] =
                      ((TextView) page.findViewById(id)).getText().toString().contains(token));
      if (okay[0]) return;
      SystemClock.sleep(150);
    }
    fail("Missing " + token + ": " + ((TextView) page.findViewById(id)).getText());
  }

  private String fixture() throws Exception {
    try (InputStream in =
            getInstrumentation().getContext().getAssets().open("openmeteo-sukolilo.json");
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[4096];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
      return out.toString("UTF-8");
    }
  }

  public void testWeatherParserUnitsDatesAndLocation() throws Exception {
    String json = fixture();
    Instant now = Instant.parse("2026-10-18T11:00:00Z");
    BmkgParser.Forecast f = OpenMeteoParser.parse(json, now, now, false, false);
    assertEquals(72, f.slots.size());
    assertEquals("Open-Meteo", f.source);
    assertFalse(f.current.time.isBefore(now));
    assertTrue(f.current.time.isBefore(now.plusSeconds(3601)));
    try {
      OpenMeteoParser.parse(json.replace("km/h", "m/s"), now, now, false, false);
      fail("Unit");
    } catch (org.json.JSONException expected) {
    }
    try {
      OpenMeteoParser.parse(json.replace("Asia/Jakarta", "UTC"), now, now, false, false);
      fail("Zone");
    } catch (org.json.JSONException expected) {
    }
    try {
      OpenMeteoParser.parse(json.replace("112.785774", "120.785774"), now, now, false, false);
      fail("Wrong location");
    } catch (org.json.JSONException expected) {
    }
  }

  public void testLiveWeatherCacheAndSharedHeader() {
    launch(WeatherActivity.class);
    awaitText(R.id.weatherStatus, "Diambil");
    assertTrue(
        ((TextView) page.findViewById(R.id.weatherStatus))
            .getText()
            .toString()
            .contains("Open-Meteo"));
    assertTrue(
        ((TextView) page.findViewById(R.id.weatherTemperature))
            .getText()
            .toString()
            .matches("-?[0-9]+°C"));
    assertTrue(((LinearLayout) page.findViewById(R.id.weatherForecastList)).getChildCount() > 0);
    TextView heading = page.findViewById(R.id.protectorToolbarTitle);
    assertEquals(Color.parseColor("#15332F"), heading.getCurrentTextColor());
    UiEvidence.capture(getInstrumentation(), "RebuiltWeatherLive");
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.protectorRefresh).performClick());
    awaitText(R.id.weatherStatus, "tersimpan");
    finishPage();
    launch(StudyActivity.class);
    assertNotNull(page.findViewById(R.id.sharedFeatureHeader));
    assertNotNull(page.findViewById(R.id.study_back));
    assertEquals(
        Color.parseColor("#15332F"),
        ((TextView) page.findViewById(R.id.study_screen_title)).getCurrentTextColor());
    assertEquals(
        "Study Hub", ((TextView) page.findViewById(R.id.study_screen_title)).getText().toString());
    SystemClock.sleep(700);
    assertTrue(
        page.findViewById(R.id.study_back).getWidth()
            >= page.getResources().getDisplayMetrics().density * 47);
    assertTrue(
        page.findViewById(R.id.study_back).getGlobalVisibleRect(new android.graphics.Rect()));
    UiEvidence.capture(getInstrumentation(), "RebuiltStudySharedHeader");
  }

  public void testOfficialEarthquakeAndProtectorLive() {
    launch(ProtectorActivity.class);
    awaitText(R.id.protectorWeatherTime, "Open-Meteo");
    awaitText(R.id.protectorQuakeStatus, "Laporan terakhir BMKG");
    UiEvidence.capture(getInstrumentation(), "RebuiltProtectorLive");
    finishPage();
    launch(EarthquakeActivity.class);
    awaitText(R.id.protectorToolbarTitle, "Gempa");
    SystemClock.sleep(3500);
    UiEvidence.capture(getInstrumentation(), "RebuiltEarthquakeLive");
  }

  public void testStudyHeaderBackPixels() {
    launch(StudyActivity.class);
    SystemClock.sleep(700);
    ImageButton back = page.findViewById(R.id.study_back);
    assertNotNull(back.getDrawable());
    android.graphics.Bitmap[] image = new android.graphics.Bitmap[1];
    getInstrumentation()
        .runOnMainSync(
            () -> {
              image[0] =
                  android.graphics.Bitmap.createBitmap(
                      back.getWidth(), back.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
              back.draw(new android.graphics.Canvas(image[0]));
            });
    int ink = 0;
    for (int y = 0; y < image[0].getHeight(); y++)
      for (int x = 0; x < image[0].getWidth(); x++) {
        int c = image[0].getPixel(x, y);
        if (android.graphics.Color.alpha(c) > 0 && android.graphics.Color.red(c) < 100) ink++;
      }
    image[0].recycle();
    assertTrue("Back icon renders Pine pixels", ink > 15);
    UiEvidence.capture(getInstrumentation(), "RebuiltStudySharedHeader");
  }

  @Override
  protected void tearDown() throws Exception {
    finishPage();
    super.tearDown();
  }
}
