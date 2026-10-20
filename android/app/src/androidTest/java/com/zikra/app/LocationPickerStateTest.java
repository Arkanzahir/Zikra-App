package com.zikra.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.location.CitySearch;
import com.zikra.app.location.LocationStore;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Local HTTP fixtures exercise UI states; no city-provider availability is claimed. */
@SuppressWarnings("deprecation")
public final class LocationPickerStateTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private final Map<String, Map<String, ?>> saved = new HashMap<>();
  private final CountDownLatch requested = new CountDownLatch(1);
  private final CountDownLatch reply = new CountDownLatch(1);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    for (String name : new String[] {LocationStore.PREFS, "zikra_preferences"})
      saved.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("reminder_enabled", false)
        .commit();
    LocationStore.save(context, CitySearch.bundled("Surabaya").get(0));
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, LocationActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long until = SystemClock.uptimeMillis() + 30000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    assertTrue(page.hasWindowFocus());
  }

  private void fixture(boolean failure) throws Exception {
    Field serviceField = LocationActivity.class.getDeclaredField("service");
    serviceField.setAccessible(true);
    CitySearch service = (CitySearch) serviceField.get(page);
    Field http = CitySearch.class.getDeclaredField("http");
    http.setAccessible(true);
    OkHttpClient original = (OkHttpClient) http.get(service);
    http.set(
        service,
        original
            .newBuilder()
            .addInterceptor(
                chain -> {
                  requested.countDown();
                  try {
                    if (!reply.await(15, TimeUnit.SECONDS))
                      throw new IOException("Fixture timeout");
                  } catch (InterruptedException e) {
                    throw new IOException(e);
                  }
                  if (failure) throw new IOException("Offline test fixture");
                  return new Response.Builder()
                      .request(chain.request())
                      .protocol(Protocol.HTTP_1_1)
                      .code(200)
                      .message("Local test fixture")
                      .body(
                          ResponseBody.create(
                              MediaType.parse("application/json"), "{\"results\":[]}"))
                      .build();
                })
            .build());
  }

  private String text(int id) {
    return ((TextView) page.findViewById(id)).getText().toString();
  }

  private void search(String query) throws Exception {
    getInstrumentation()
        .runOnMainSync(
            () -> {
              ((EditText) page.findViewById(R.id.locationQuery)).setText(query);
              page.findViewById(R.id.locationSearch).performClick();
            });
    assertTrue("Lookup starts", requested.await(10, TimeUnit.SECONDS));
    getInstrumentation().waitForIdleSync();
    assertEquals(View.VISIBLE, page.findViewById(R.id.locationProgress).getVisibility());
    assertEquals("Mencari kota…", text(R.id.locationStatus));
  }

  private void awaitText(String value) {
    long until = SystemClock.uptimeMillis() + 10000;
    while (!text(R.id.locationStatus).contains(value) && SystemClock.uptimeMillis() < until)
      SystemClock.sleep(100);
    getInstrumentation().waitForIdleSync();
    assertTrue(text(R.id.locationStatus), text(R.id.locationStatus).contains(value));
    assertEquals(View.GONE, page.findViewById(R.id.locationProgress).getVisibility());
  }

  public void testLoadingThenOfflineFallbackKeepsSavedLocation() throws Exception {
    fixture(true);
    search("Makassar");
    UiEvidence.capture(getInstrumentation(), "LocationPolishLoading");
    reply.countDown();
    awaitText("Pencarian online belum tersedia");
    LinearLayout rows = page.findViewById(R.id.locationResults);
    assertEquals(1, rows.getChildCount());
    assertTrue(((TextView) rows.getChildAt(0)).getText().toString().contains("Makassar"));
    assertTrue(text(R.id.locationCurrent).contains("Surabaya"));
    assertEquals("Asia/Jakarta", LocationStore.current(context).zone.getId());
    UiEvidence.capture(getInstrumentation(), "LocationPolishOffline");
  }

  public void testManualSelectionCancelsPendingLookup() throws Exception {
    fixture(false);
    search("TidakAda");
    getInstrumentation()
        .runOnMainSync(
            () -> {
              LinearLayout rows = page.findViewById(R.id.locationResults);
              for (int i = 0; i < rows.getChildCount(); i++) {
                if (((TextView) rows.getChildAt(i)).getText().toString().startsWith("Makassar,")) {
                  rows.getChildAt(i).performClick();
                  return;
                }
              }
              fail("Offline city remains available during lookup");
            });
    getInstrumentation().waitForIdleSync();
    Field dialogField = LocationActivity.class.getDeclaredField("confirmation");
    dialogField.setAccessible(true);
    AlertDialog dialog = (AlertDialog) dialogField.get(page);
    assertTrue(dialog.isShowing());
    assertEquals(View.GONE, page.findViewById(R.id.locationProgress).getVisibility());
    getInstrumentation()
        .runOnMainSync(() -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
    reply.countDown();
    Field workField = LocationActivity.class.getDeclaredField("work");
    workField.setAccessible(true);
    ((java.util.concurrent.ExecutorService) workField.get(page))
        .submit(() -> {})
        .get(10, TimeUnit.SECONDS);
    getInstrumentation().waitForIdleSync();
    assertTrue(text(R.id.locationCurrent).contains("Makassar"));
    assertEquals("Asia/Makassar", LocationStore.current(context).zone.getId());
    assertTrue(text(R.id.locationStatus).startsWith("Lokasi disimpan"));
    UiEvidence.capture(getInstrumentation(), "LocationPolishManual");
  }

  public void testEmptySearchDoesNotChangeSavedLocation() throws Exception {
    fixture(false);
    search("TidakAda");
    reply.countDown();
    awaitText("Kota belum ditemukan");
    assertEquals(0, ((LinearLayout) page.findViewById(R.id.locationResults)).getChildCount());
    assertTrue(text(R.id.locationCurrent).contains("Surabaya"));
    assertTrue(page.findViewById(R.id.locationSearch).isEnabled());
    UiEvidence.capture(getInstrumentation(), "LocationPolishEmpty");
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      reply.countDown();
      if (page != null) getInstrumentation().runOnMainSync(page::finish);
      getInstrumentation().waitForIdleSync();
      for (Map.Entry<String, Map<String, ?>> item : saved.entrySet()) {
        SharedPreferences.Editor editor =
            context.getSharedPreferences(item.getKey(), 0).edit().clear();
        for (Map.Entry<String, ?> value : item.getValue().entrySet()) {
          Object v = value.getValue();
          String key = value.getKey();
          if (v instanceof String) editor.putString(key, (String) v);
          else if (v instanceof Boolean) editor.putBoolean(key, (Boolean) v);
          else if (v instanceof Integer) editor.putInt(key, (Integer) v);
          else if (v instanceof Long) editor.putLong(key, (Long) v);
          else if (v instanceof Float) editor.putFloat(key, (Float) v);
          else if (v instanceof java.util.Set) editor.putStringSet(key, (java.util.Set<String>) v);
        }
        editor.commit();
      }
      com.zikra.app.reminders.PrayerReminders.sync(context);
    } finally {
      super.tearDown();
    }
  }
}
