package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.zikra.app.location.CitySearch;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import java.util.HashMap;
import java.util.Map;
import org.osmdroid.views.MapView;

/** Deterministic interaction checks. Mosque records are explicitly labeled test data. */
@SuppressWarnings("deprecation")
public final class NearbyDesignTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private final Map<String, Map<String, ?>> saved = new HashMap<>();
  private LocationPoint location;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    for (String name : new String[] {LocationStore.PREFS, "zikra_preferences", "zikra_mosques_osm"})
      saved.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("reminder_enabled", false)
        .putBoolean("shake_enabled", false)
        .commit();
    location = CitySearch.bundled("Surabaya").get(0);
    LocationStore.save(context, location);
    String fixture =
        "{\"elements\":["
            + "{\"type\":\"node\",\"id\":1,\"lat\":-7.2580,\"lon\":112.7525,"
            + "\"tags\":{\"name\":\"DATA UJI · Masjid A\",\"religion\":\"muslim\"}},"
            + "{\"type\":\"node\",\"id\":2,\"lat\":-7.2600,\"lon\":112.7550,"
            + "\"tags\":{\"name\":\"DATA UJI · Masjid B\",\"religion\":\"muslim\"}}]}";
    context
        .getSharedPreferences("zikra_mosques_osm", 0)
        .edit()
        .clear()
        .putString("location", location.key() + ":3000")
        .putString("json", fixture)
        .putLong("fetched_at", System.currentTimeMillis())
        .commit();
  }

  private void launch() {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, NearbyMosquesActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long until = SystemClock.uptimeMillis() + 30000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    assertTrue("Page receives focus", page.hasWindowFocus());
  }

  private void click(int id) {
    getInstrumentation().runOnMainSync(() -> page.findViewById(id).performClick());
    getInstrumentation().waitForIdleSync();
  }

  private String text(int id) {
    return ((TextView) page.findViewById(id)).getText().toString();
  }

  private LinearLayout results() {
    return page.findViewById(R.id.mosquesResults);
  }

  private void loadCached() {
    click(R.id.mosquesRefresh);
    long until = SystemClock.uptimeMillis() + 15000;
    while (results().getChildCount() == 0 && SystemClock.uptimeMillis() < until)
      SystemClock.sleep(100);
    getInstrumentation().waitForIdleSync();
    assertEquals(text(R.id.mosquesStatus), 2, results().getChildCount());
    assertTrue(text(R.id.mosquesUpdated).contains("Data tersimpan"));
    assertEquals(View.GONE, page.findViewById(R.id.mosquesProgress).getVisibility());
  }

  private MapView map() throws Exception {
    java.lang.reflect.Field field = NearbyMosquesActivity.class.getDeclaredField("mapView");
    field.setAccessible(true);
    return (MapView) field.get(page);
  }

  public void testListMapSelectionAndDirectionsStayInSync() throws Exception {
    launch();
    assertTrue(((RadioButton) page.findViewById(R.id.mosquesListToggle)).isChecked());
    assertEquals(View.GONE, page.findViewById(R.id.mosquesMap).getVisibility());
    ViewGroup header = page.findViewById(R.id.mosquesHeader);
    assertEquals(
        context.getColor(R.color.nearby_surface),
        ((ColorDrawable) header.getChildAt(0).getBackground()).getColor());
    loadCached();
    assertEquals(
        "DATA UJI · Masjid A",
        ((TextView) results().getChildAt(0).findViewById(R.id.mosqueName)).getText().toString());
    UiEvidence.capture(getInstrumentation(), "NearbyDesignListTestData");
    click(R.id.mosquesMapToggle);
    assertEquals(View.GONE, results().getVisibility());
    assertEquals(View.VISIBLE, page.findViewById(R.id.mosquesMap).getVisibility());
    assertEquals(View.VISIBLE, page.findViewById(R.id.mosquesSelection).getVisibility());
    MapView map = map();
    // Do not download tiles in a deterministic UI test.
    getInstrumentation().runOnMainSync(() -> map.setUseDataConnection(false));
    View container = page.findViewById(R.id.mosquesMap);
    getInstrumentation()
        .runOnMainSync(
            () ->
                container.requestRectangleOnScreen(
                    new Rect(0, 0, container.getWidth(), container.getHeight()), true));
    getInstrumentation().waitForIdleSync();
    UiEvidence.capture(getInstrumentation(), "NearbyDesignMapTestData");
    final Intent[] route = {null};
    Instrumentation.ActivityMonitor monitor =
        new Instrumentation.ActivityMonitor() {
          @Override
          public Instrumentation.ActivityResult onStartActivity(Intent intent) {
            if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
              route[0] = intent;
              return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
            }
            return null;
          }
        };
    getInstrumentation().addMonitor(monitor);
    try {
      getInstrumentation()
          .runOnMainSync(
              () ->
                  page.findViewById(R.id.mosquesSelection)
                      .findViewById(R.id.mosqueDirections)
                      .performClick());
      getInstrumentation().waitForIdleSync();
      assertNotNull(route[0]);
      assertEquals("-7.258,112.7525", route[0].getData().getQueryParameter("destination"));
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    click(R.id.mosquesListToggle);
    assertEquals(View.VISIBLE, results().getVisibility());
    assertEquals(View.GONE, page.findViewById(R.id.mosquesSelection).getVisibility());
    assertEquals(View.GONE, page.findViewById(R.id.mosquesMapStatus).getVisibility());
    assertFalse(map.getTileProvider().useDataConnection());
  }

  public void testFailedRefreshKeepsResultsAndFetchTime() throws Exception {
    launch();
    loadCached();
    String fetched = text(R.id.mosquesUpdated);
    context
        .getSharedPreferences("zikra_mosques_osm", 0)
        .edit()
        .putLong("retry_until", System.currentTimeMillis() + 60000)
        .commit();
    java.lang.reflect.Field cooldown =
        NearbyMosquesActivity.class.getDeclaredField("nextAllowedSearch");
    cooldown.setAccessible(true);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                cooldown.setLong(page, 0);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    click(R.id.mosquesRefresh);
    long until = SystemClock.uptimeMillis() + 10000;
    while (!text(R.id.mosquesStatus).contains("Menampilkan hasil tersimpan")
        && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    assertTrue(text(R.id.mosquesStatus), text(R.id.mosquesStatus).contains("Menampilkan hasil tersimpan"));
    assertEquals(2, results().getChildCount());
    assertEquals(fetched, text(R.id.mosquesUpdated));
    assertEquals(View.GONE, page.findViewById(R.id.mosquesEmpty).getVisibility());
    assertTrue(page.findViewById(R.id.mosquesRefresh).isEnabled());
  }

  public void testChangingCityClearsOldResultsAndMapSelection() throws Exception {
    launch();
    loadCached();
    click(R.id.mosquesMapToggle);
    getInstrumentation().runOnMainSync(() -> mapUnchecked().setUseDataConnection(false));
    Activity picker =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, LocationActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    try {
      getInstrumentation()
          .runOnMainSync(() -> LocationStore.save(context, CitySearch.bundled("Makassar").get(0)));
    } finally {
      getInstrumentation().runOnMainSync(picker::finish);
    }
    getInstrumentation().waitForIdleSync();
    assertTrue(text(R.id.mosquesLocation).contains("Makassar"));
    assertTrue(text(R.id.mosquesLocation).contains("WITA"));
    assertEquals(0, results().getChildCount());
    assertEquals(View.GONE, page.findViewById(R.id.mosquesSelection).getVisibility());
    assertEquals(View.GONE, page.findViewById(R.id.mosquesUpdated).getVisibility());
    assertEquals(View.VISIBLE, page.findViewById(R.id.mosquesEmpty).getVisibility());
  }

  public void testMissingMapTilesShowStatusAndRetry() throws Exception {
    launch();
    loadCached();
    click(R.id.mosquesMapToggle);
    getInstrumentation().runOnMainSync(() -> mapUnchecked().setUseDataConnection(false));
    java.lang.reflect.Field field = NearbyMosquesActivity.class.getDeclaredField("tileEvents");
    field.setAccessible(true);
    android.os.Handler events = (android.os.Handler) field.get(page);
    events.sendEmptyMessage(org.osmdroid.tileprovider.MapTileProviderBase.MAPTILE_FAIL_ID);
    getInstrumentation().waitForIdleSync();
    assertEquals(View.VISIBLE, page.findViewById(R.id.mosquesMapRetry).getVisibility());
    assertTrue(text(R.id.mosquesMapNotice).contains("belum termuat"));
    UiEvidence.capture(getInstrumentation(), "MapMissingTilesExplicit");
    click(R.id.mosquesMapRetry);
    assertTrue(text(R.id.mosquesMapNotice).contains("Memuat"));
    events.sendEmptyMessage(org.osmdroid.tileprovider.MapTileProviderBase.MAPTILE_SUCCESS_ID);
    getInstrumentation().waitForIdleSync();
    assertEquals(View.GONE, page.findViewById(R.id.mosquesMapNotice).getVisibility());
    click(R.id.mosquesListToggle);
    assertEquals(2, results().getChildCount());
  }

  private MapView mapUnchecked() {
    try {
      return map();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void testEmptySearchSurvivesRecreation() throws Exception {
    context
        .getSharedPreferences("zikra_mosques_osm", 0)
        .edit()
        .putString("json", "{\"elements\":[]}")
        .commit();
    launch();
    click(R.id.mosquesRefresh);
    waitForText(R.id.mosquesCount, "0 tempat");
    assertEquals(View.VISIBLE, page.findViewById(R.id.mosquesEmpty).getVisibility());
    assertTrue(text(R.id.mosquesEmptyTitle).contains("Belum ada masjid"));
    String fetched = text(R.id.mosquesUpdated);
    UiEvidence.capture(getInstrumentation(), "NearbyPolishEmpty");
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(NearbyMosquesActivity.class.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
      waitForText(R.id.mosquesCount, "0 tempat");
      assertEquals(fetched, text(R.id.mosquesUpdated));
      assertEquals(View.VISIBLE, page.findViewById(R.id.mosquesEmpty).getVisibility());
      assertEquals(0, results().getChildCount());
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testRateLimitWithoutResultsShowsRetryAndKeepsLocation() throws Exception {
    context
        .getSharedPreferences("zikra_mosques_osm", 0)
        .edit()
        .clear()
        .putLong("retry_until", System.currentTimeMillis() + 60000)
        .commit();
    launch();
    click(R.id.mosquesRefresh);
    waitForText(R.id.mosquesStatus, "membatasi pencarian");
    assertEquals(View.GONE, page.findViewById(R.id.mosquesProgress).getVisibility());
    assertEquals(View.VISIBLE, page.findViewById(R.id.mosquesEmpty).getVisibility());
    assertEquals("Coba lagi", text(R.id.mosquesRefresh));
    assertEquals("Pencarian belum selesai", text(R.id.mosquesCount));
    assertEquals(0, results().getChildCount());
    assertEquals(location.key(), LocationStore.current(context).key());
    UiEvidence.capture(getInstrumentation(), "NearbyPolishRateLimit");
  }

  private void waitForText(int id, String value) {
    long until = SystemClock.uptimeMillis() + 15000;
    while (!text(id).contains(value) && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    getInstrumentation().waitForIdleSync();
    assertTrue(text(id), text(id).contains(value));
  }

  public void testRecreationRestoresMapAndCachedResults() throws Exception {
    launch();
    loadCached();
    click(R.id.mosquesMapToggle);
    getInstrumentation().runOnMainSync(() -> mapUnchecked().setUseDataConnection(false));
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(NearbyMosquesActivity.class.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
      getInstrumentation().waitForIdleSync();
      long until = SystemClock.uptimeMillis() + 15000;
      while (results().getChildCount() == 0 && SystemClock.uptimeMillis() < until)
        SystemClock.sleep(100);
      assertEquals(2, results().getChildCount());
      assertTrue(((RadioButton) page.findViewById(R.id.mosquesMapToggle)).isChecked());
      assertEquals(View.GONE, results().getVisibility());
      assertEquals(View.VISIBLE, page.findViewById(R.id.mosquesSelection).getVisibility());
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (page != null) getInstrumentation().runOnMainSync(page::finish);
      getInstrumentation().waitForIdleSync();
      for (Map.Entry<String, Map<String, ?>> item : saved.entrySet()) {
        SharedPreferences.Editor editor =
            context.getSharedPreferences(item.getKey(), 0).edit().clear();
        for (Map.Entry<String, ?> value : item.getValue().entrySet()) {
          Object v = value.getValue();
          if (v instanceof String) editor.putString(value.getKey(), (String) v);
          else if (v instanceof Long) editor.putLong(value.getKey(), (Long) v);
          else if (v instanceof Integer) editor.putInt(value.getKey(), (Integer) v);
          else if (v instanceof Boolean) editor.putBoolean(value.getKey(), (Boolean) v);
          else if (v instanceof Float) editor.putFloat(value.getKey(), (Float) v);
          else if (v instanceof java.util.Set)
            editor.putStringSet(value.getKey(), (java.util.Set<String>) v);
        }
        editor.commit();
      }
    } finally {
      super.tearDown();
    }
  }
}
