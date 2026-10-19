package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.location.CitySearch;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Actual service calls: unavailable services fail visibly rather than using test places. */
@SuppressWarnings("deprecation")
public final class LocationLiveTest extends InstrumentationTestCase {
  private Activity page;
  private Map<String, ?> original;
  private android.content.Context context;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    original = new HashMap<>(context.getSharedPreferences(LocationStore.PREFS, 0).getAll());
  }

  private void launch(Class<? extends Activity> type) {
    page =
        getInstrumentation()
            .startActivitySync(new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long end = SystemClock.uptimeMillis() + 30000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue("Screen receives focus", page.hasWindowFocus());
  }

  private String text(int id) {
    return ((TextView) page.findViewById(id)).getText().toString();
  }

  private void click(int id) {
    getInstrumentation().runOnMainSync(() -> page.findViewById(id).performClick());
    getInstrumentation().waitForIdleSync();
  }

  public void testActualCitySearchAndWeatherUseMakassarZone() throws Exception {
    List<LocationPoint> choices;
    try (CitySearch cities = new CitySearch()) {
      choices = cities.search("Makassar");
    }
    assertFalse("Live city search returns candidates", choices.isEmpty());
    LocationPoint city = null;
    for (LocationPoint p : choices)
      if (p.zone.getId().equals("Asia/Makassar")) {
        city = p;
        break;
      }
    assertNotNull(city);
    LocationStore.save(context, city);
    launch(WeatherActivity.class);
    long end = SystemClock.uptimeMillis() + 35000;
    while (!text(R.id.weatherStatus).contains("Diambil") && SystemClock.uptimeMillis() < end)
      SystemClock.sleep(150);
    assertTrue(text(R.id.weatherStatus), text(R.id.weatherStatus).contains("WITA"));
    assertTrue(text(R.id.weatherLocation).contains("Makassar"));
    assertTrue(text(R.id.weatherTemperature).matches("-?[0-9]+°C"));
    UiEvidence.capture(getInstrumentation(), "LocationWeatherMakassarLive");
  }

  public void testActualNearbyListNativeMapAndRouting() throws Exception {
    LocationPoint point =
        new LocationPoint(
            -7.28,
            112.79,
            "Asia/Jakarta",
            "Sukolilo, Surabaya",
            "manual",
            System.currentTimeMillis(),
            -1);
    LocationStore.save(context, point);
    launch(NearbyMosquesActivity.class);
    click(R.id.mosquesRefresh);
    LinearLayout list = page.findViewById(R.id.mosquesResults);
    long end = SystemClock.uptimeMillis() + 35000;
    while (list.getChildCount() == 0 && SystemClock.uptimeMillis() < end) SystemClock.sleep(150);
    assertTrue(text(R.id.mosquesStatus), list.getChildCount() > 0);
    UiEvidence.capture(getInstrumentation(), "LocationMosquesListLive");
    final Intent[] routed = {null};
    Instrumentation.ActivityMonitor monitor =
        new Instrumentation.ActivityMonitor() {
          @Override
          public Instrumentation.ActivityResult onStartActivity(Intent intent) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())
                && intent.getData() != null
                && "www.google.com".equals(intent.getData().getHost())) {
              routed[0] = intent;
              return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
            }
            return null;
          }
        };
    getInstrumentation().addMonitor(monitor);
    try {
      LinearLayout first = (LinearLayout) list.getChildAt(0);
      getInstrumentation().runOnMainSync(() -> first.getChildAt(2).performClick());
      getInstrumentation().waitForIdleSync();
      assertNotNull("Route intent is emitted", routed[0]);
      String[] coordinates = routed[0].getData().getQueryParameter("destination").split(",");
      assertTrue(
          "Route points inside search area",
          point.distanceTo(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]))
              <= 3005);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    click(R.id.mosquesMapToggle);
    View container = page.findViewById(R.id.mosquesMap);
    getInstrumentation()
        .runOnMainSync(
            () ->
                container.requestRectangleOnScreen(
                    new Rect(0, 0, container.getWidth(), container.getHeight()), true));
    getInstrumentation().waitForIdleSync();
    SystemClock.sleep(5000);
    assertEquals(View.VISIBLE, container.getVisibility());
    java.lang.reflect.Field field = NearbyMosquesActivity.class.getDeclaredField("mapView");
    field.setAccessible(true);
    org.osmdroid.views.MapView map = (org.osmdroid.views.MapView) field.get(page);
    assertNotNull(map);
    assertTrue(map.getOverlays().size() > 1);
    assertTrue(map.getTileProvider().useDataConnection());
    UiEvidence.capture(getInstrumentation(), "LocationMosquesMapLive");
    click(R.id.mosquesMapToggle);
    assertFalse(map.getTileProvider().useDataConnection());
  }

  public void testRepositoryActualNearbyAndCache() throws Exception {
    try (com.zikra.app.mosque.MosqueSearch search =
        new com.zikra.app.mosque.MosqueSearch(context)) {
      com.zikra.app.mosque.MosqueSearch.Result first = search.find(LocationPoint.INITIAL, false);
      assertFalse("Live/cache source contains actual nearby records", first.places.isEmpty());
      com.zikra.app.mosque.MosqueSearch.Result cached = search.find(LocationPoint.INITIAL, false);
      assertTrue(cached.cached);
      assertEquals(first.fetchedAt, cached.fetchedAt);
    }
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    SharedPreferences.Editor e =
        context.getSharedPreferences(LocationStore.PREFS, 0).edit().clear();
    for (Map.Entry<String, ?> item : original.entrySet()) {
      Object v = item.getValue();
      String k = item.getKey();
      if (v instanceof String) e.putString(k, (String) v);
      else if (v instanceof Long) e.putLong(k, (Long) v);
      else if (v instanceof Float) e.putFloat(k, (Float) v);
      else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
      else if (v instanceof Integer) e.putInt(k, (Integer) v);
      else if (v instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) v));
    }
    e.commit();
    com.zikra.app.reminders.PrayerReminders.sync(context);
    super.tearDown();
  }
}
