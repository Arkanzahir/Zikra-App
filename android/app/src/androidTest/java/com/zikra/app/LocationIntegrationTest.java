package com.zikra.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.location.CitySearch;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.mosque.MosquePlace;
import com.zikra.app.safety.OpenMeteoParser;
import com.zikra.app.sky.SkyRepository;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

@SuppressWarnings("deprecation")
public final class LocationIntegrationTest extends InstrumentationTestCase {
  private Context context;
  private final List<Activity> pages = new ArrayList<>();
  private final Map<String, Map<String, ?>> saved = new HashMap<>();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    for (String name : new String[] {LocationStore.PREFS, "zikra_preferences"})
      saved.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context.getSharedPreferences(LocationStore.PREFS, 0).edit().clear().commit();
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("reminder_enabled", false)
        .commit();
  }

  private Activity launch(Class<? extends Activity> type) {
    Activity page =
        getInstrumentation()
            .startActivitySync(new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    pages.add(page);
    getInstrumentation().waitForIdleSync();
    waitFocus(page);
    return page;
  }

  private void waitFocus(Activity page) {
    long until = SystemClock.uptimeMillis() + 30000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    assertTrue("Activity receives focus", page.hasWindowFocus());
  }

  private void finish(Activity page) {
    getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
  }

  private String text(Activity page, int id) {
    return ((TextView) page.findViewById(id)).getText().toString();
  }

  private String asset(String name) throws Exception {
    try (InputStream in = getInstrumentation().getContext().getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[4096];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
      return out.toString("UTF-8");
    }
  }

  public void testOfflineCitySelectionUpdatesPrayerQiblaAndHome() throws Exception {
    Activity picker = launch(LocationActivity.class);
    LinearLayout list = picker.findViewById(R.id.locationResults);
    android.view.View chosen = null;
    for (int i = 0; i < list.getChildCount(); i++)
      if (((TextView) list.getChildAt(i)).getText().toString().startsWith("Makassar,"))
        chosen = list.getChildAt(i);
    assertNotNull(chosen);
    android.view.View row = chosen;
    getInstrumentation().runOnMainSync(row::performClick);
    getInstrumentation().waitForIdleSync();
    java.lang.reflect.Field field = LocationActivity.class.getDeclaredField("confirmation");
    field.setAccessible(true);
    AlertDialog dialog = (AlertDialog) field.get(picker);
    assertNotNull(dialog);
    getInstrumentation()
        .runOnMainSync(() -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
    getInstrumentation().waitForIdleSync();
    LocationPoint selected = LocationStore.current(context);
    assertEquals("Asia/Makassar", selected.zone.getId());
    assertEquals("manual", selected.source);
    assertTrue(text(picker, R.id.locationDetails).contains("WITA"));
    UiEvidence.capture(getInstrumentation(), "LocationManualMakassar");
    finish(picker);
    Activity prayer = launch(PrayerActivity.class);
    assertTrue(text(prayer, R.id.featureSubtitle).contains("Makassar"));
    assertTrue(text(prayer, R.id.prayerMethod).contains("WITA"));
    UiEvidence.capture(getInstrumentation(), "LocationPrayerMakassar");
    finish(prayer);
    Activity qibla = launch(QiblaActivity.class);
    assertEquals(Math.round(selected.qiblaBearing()) + "°", text(qibla, R.id.qiblaBearing));
    assertTrue(text(qibla, R.id.featureSubtitle).contains("Makassar"));
    UiEvidence.capture(getInstrumentation(), "LocationQiblaMakassar");
    finish(qibla);
    Activity home = launch(HomeActivity.class);
    assertTrue(text(home, R.id.homeLocationLabel).contains("WITA"));
    assertTrue(
        home.findViewById(R.id.homeSearch).getGlobalVisibleRect(new android.graphics.Rect()));
    UiEvidence.capture(getInstrumentation(), "LocationHomeMakassar");
  }

  public void testSelectionSurvivesRecreationAndDenialKeepsManualChoices() throws Exception {
    LocationStore.save(context, CitySearch.bundled("Jayapura").get(0));
    Activity picker = launch(LocationActivity.class);
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(LocationActivity.class.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(picker::recreate);
      Activity recreated = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(recreated);
      pages.add(recreated);
      waitFocus(recreated);
      assertTrue(text(recreated, R.id.locationCurrent).contains("Jayapura"));
      getInstrumentation()
          .runOnMainSync(
              () ->
                  recreated.onRequestPermissionsResult(
                      804,
                      new String[] {
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                      },
                      new int[] {-1, -1}));
      assertTrue(recreated.findViewById(R.id.locationSearch).isEnabled());
      assertEquals(
          15, ((LinearLayout) recreated.findViewById(R.id.locationResults)).getChildCount());
      assertEquals("Asia/Jayapura", LocationStore.current(context).zone.getId());
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testForecastRejectsAnotherCityCacheAndKeepsZone() throws Exception {
    JSONObject root = new JSONObject(asset("openmeteo-sukolilo.json"));
    root.put("latitude", -5.1477)
        .put("longitude", 119.4327)
        .put("timezone", "Asia/Makassar")
        .put("utc_offset_seconds", 28800);
    LocationPoint point = CitySearch.bundled("Makassar").get(0);
    Instant now = Instant.parse("2026-10-18T11:00:00Z");
    com.zikra.app.safety.BmkgParser.Forecast forecast =
        OpenMeteoParser.parse(root.toString(), now, now, false, false, point);
    assertEquals(point.key(), forecast.location.key());
    assertEquals("Makassar, Indonesia", forecast.current.village);
    assertTrue(com.zikra.app.safety.OpenMeteoClient.status(forecast).contains("WITA"));
    try {
      OpenMeteoParser.parse(root.toString(), now, now, false, false, LocationPoint.INITIAL);
      fail("Reject other city");
    } catch (org.json.JSONException expected) {
    }
  }

  public void testSkyValidatesSelectedCoordinateAndTimeZone() throws Exception {
    JSONObject root = new JSONObject(asset("usno-sukolilo-2026-10-18.json"));
    LocationPoint point = CitySearch.bundled("Makassar").get(0);
    root.getJSONObject("geometry")
        .getJSONArray("coordinates")
        .put(0, point.longitude)
        .put(1, point.latitude);
    root.getJSONObject("properties").getJSONObject("data").put("tz", 8);
    assertTrue(SkyRepository.parse(root.toString(), LocalDate.of(2026, 10, 18), point).usno);
    try {
      SkyRepository.parse(root.toString(), LocalDate.of(2026, 10, 18), LocationPoint.INITIAL);
      fail("Reject old coordinate");
    } catch (org.json.JSONException expected) {
    }
  }

  public void testMosquesSortDeduplicateAndRejectIncompleteResponse() throws Exception {
    String json =
        "{\"elements\":[{\"type\":\"node\",\"id\":1,\"lat\":-7.29,\"lon\":112.79,\"tags\":{\"name\":\"Masjid"
            + " uji jauh\",\"religion\":\"muslim\"}},{\"type\":\"node\",\"id\":2,\"lat\":-7.281,\"lon\":112.79,\"tags\":{\"name\":\"Masjid"
            + " uji dekat\",\"religion\":\"muslim\"}},"
            + "{\"type\":\"node\",\"id\":2,\"lat\":-7.281,\"lon\":112.79,\"tags\":{\"religion\":\"muslim\"}},"
            + "{\"type\":\"node\",\"id\":3,\"lat\":-8,\"lon\":112.79,\"tags\":{\"religion\":\"muslim\"}}]}";
    List<MosquePlace> result = MosquePlace.parse(json, "osm", LocationPoint.INITIAL, 3000);
    assertEquals(2, result.size());
    assertEquals("Masjid uji dekat", result.get(0).name);
    assertTrue(result.get(0).distance < result.get(1).distance);
    try {
      MosquePlace.parse(
          "{\"remark\":\"runtime error\",\"elements\":[]}", "osm", LocationPoint.INITIAL, 3000);
      fail("Incomplete query");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCompactLocationAndMosqueLayoutsKeepActionsReachableAt200Percent() {
    List<String> errors = new ArrayList<>();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              android.content.res.Configuration config =
                  new android.content.res.Configuration(context.getResources().getConfiguration());
              config.fontScale = 2f;
              config.screenWidthDp = 320;
              config.screenHeightDp = 560;
              android.content.Context scoped = context.createConfigurationContext(config);
              android.view.ContextThemeWrapper themed =
                  new android.view.ContextThemeWrapper(scoped, R.style.Theme_Zikra);
              float density = themed.getResources().getDisplayMetrics().density;
              for (int layout :
                  new int[] {R.layout.activity_location, R.layout.activity_nearby_mosques}) {
                android.view.ViewGroup root =
                    (android.view.ViewGroup)
                        android.view.LayoutInflater.from(themed).inflate(layout, null);
                int width = Math.round(320 * density), height = Math.round(560 * density);
                root.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(
                        width, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(
                        height, android.view.View.MeasureSpec.EXACTLY));
                root.layout(0, 0, width, height);
                android.widget.ScrollView scroll =
                    (android.widget.ScrollView)
                        ((android.view.ViewGroup) root.getChildAt(0)).getChildAt(1);
                int[] ids =
                    layout == R.layout.activity_location
                        ? new int[] {
                          R.id.locationAutomatic, R.id.locationQuery, R.id.locationSearch
                        }
                        : new int[] {
                          R.id.mosquesLocation,
                          R.id.mosquesRefresh,
                          R.id.mosquesMapToggle,
                          R.id.mosquesExternal
                        };
                android.graphics.Rect viewport = new android.graphics.Rect();
                scroll.getDrawingRect(viewport);
                root.offsetDescendantRectToMyCoords(scroll, viewport);
                for (int id : ids) {
                  android.view.View action = root.findViewById(id);
                  android.graphics.Rect target = new android.graphics.Rect();
                  action.getDrawingRect(target);
                  ((android.view.ViewGroup) scroll.getChildAt(0))
                      .offsetDescendantRectToMyCoords(action, target);
                  scroll.scrollTo(0, target.top);
                  android.graphics.Rect visible = new android.graphics.Rect();
                  action.getDrawingRect(visible);
                  root.offsetDescendantRectToMyCoords(action, visible);
                  if (visible.top < viewport.top
                      || visible.bottom > viewport.bottom
                      || action.getHeight() < 48 * density)
                    errors.add("Action not reachable at enlarged text: " + id);
                }
              }
            });
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Override
  protected void tearDown() throws Exception {
    for (int i = pages.size() - 1; i >= 0; i--) {
      Activity page = pages.get(i);
      if (page != null) getInstrumentation().runOnMainSync(page::finish);
    }
    getInstrumentation().waitForIdleSync();
    for (Map.Entry<String, Map<String, ?>> entry : saved.entrySet()) {
      SharedPreferences.Editor e = context.getSharedPreferences(entry.getKey(), 0).edit().clear();
      for (Map.Entry<String, ?> item : entry.getValue().entrySet()) {
        Object v = item.getValue();
        String k = item.getKey();
        if (v instanceof String) e.putString(k, (String) v);
        else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
        else if (v instanceof Integer) e.putInt(k, (Integer) v);
        else if (v instanceof Long) e.putLong(k, (Long) v);
        else if (v instanceof Float) e.putFloat(k, (Float) v);
        else if (v instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) v));
      }
      e.commit();
    }
    com.zikra.app.reminders.PrayerReminders.sync(context);
    super.tearDown();
  }
}
