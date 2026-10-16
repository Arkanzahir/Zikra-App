package com.zikra.app.safety;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import com.zikra.app.EarthquakeActivity;
import com.zikra.app.ProtectorActivity;
import com.zikra.app.R;
import com.zikra.app.WeatherActivity;
import java.util.ArrayList;
import java.util.List;

/** Real native navigation and empty-data safety checks; independent of BMKG availability. */
@SuppressWarnings("deprecation")
public final class BmkgNativeTest extends InstrumentationTestCase {
  private final List<Activity> pages = new ArrayList<>();

  private ProtectorActivity launch() {
    Intent intent =
        new Intent(getInstrumentation().getTargetContext(), ProtectorActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    ProtectorActivity page = (ProtectorActivity) getInstrumentation().startActivitySync(intent);
    pages.add(page);
    getInstrumentation().waitForIdleSync();
    return page;
  }

  @Override
  protected void tearDown() throws Exception {
    for (int i = pages.size() - 1; i >= 0; i--) {
      Activity page = pages.get(i);
      getInstrumentation().runOnMainSync(page::finish);
    }
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }

  public void testDashboardWeatherOpensNativeForecastActivity() {
    ProtectorActivity dashboard = launch();
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(WeatherActivity.class.getName(), null, false);
    try {
      getInstrumentation()
          .runOnMainSync(() -> dashboard.findViewById(R.id.protectorWeatherCard).performClick());
      Activity detail = getInstrumentation().waitForMonitorWithTimeout(monitor, 60000);
      assertNotNull("Weather card must open a native detail Activity", detail);
      pages.add(detail);
      getInstrumentation().waitForIdleSync();
      com.zikra.app.UiEvidence.capture(getInstrumentation(), "WeatherActivity");
      assertNotNull(detail.findViewById(R.id.weatherChart));
      assertNotNull(detail.findViewById(R.id.weatherForecastList));
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testDashboardEarthquakeOpensNativeMapAndDetails() {
    ProtectorActivity dashboard = launch();
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(EarthquakeActivity.class.getName(), null, false);
    try {
      getInstrumentation()
          .runOnMainSync(() -> dashboard.findViewById(R.id.protectorQuakeCard).performClick());
      Activity detail = getInstrumentation().waitForMonitorWithTimeout(monitor, 60000);
      assertNotNull("Earthquake card must open a native detail Activity", detail);
      pages.add(detail);
      getInstrumentation().waitForIdleSync();
      com.zikra.app.UiEvidence.capture(getInstrumentation(), "EarthquakeActivity");
      assertTrue(detail.findViewById(R.id.quakeMap) instanceof IndonesiaMapView);
      assertNotNull(detail.findViewById(R.id.quakeOpenMap));
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testMapNeverInventsAnEarthquakeMarker() {
    getInstrumentation()
        .runOnMainSync(
            () -> {
              IndonesiaMapView map = new IndonesiaMapView(getInstrumentation().getTargetContext());
              assertFalse(map.hasEpicenter());
              map.setEpicenter(Double.NaN, 112.75);
              assertFalse(map.hasEpicenter());
              map.setEpicenter(-7.25, 181.0);
              assertFalse(map.hasEpicenter());
              map.setEpicenter(-7.25, 112.75);
              assertTrue(map.hasEpicenter());
              map.setEpicenter(null, null);
              assertFalse(map.hasEpicenter());
            });
  }
}
