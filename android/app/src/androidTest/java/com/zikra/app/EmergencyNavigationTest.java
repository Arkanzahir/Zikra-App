package com.zikra.app;

import android.app.*;
import android.content.*;
import android.test.InstrumentationTestCase;
import java.lang.reflect.*;
import java.util.*;

/** Outbound intents are intercepted: no real calls, messages or navigation. */
@SuppressWarnings("deprecation")
public final class EmergencyNavigationTest extends InstrumentationTestCase {
  private BaseActivity page;
  private final List<Intent> outbound = new ArrayList<>();
  private boolean rejectSms, rejectNavigation;
  private Instrumentation.ActivityMonitor monitor;

  @Override protected void setUp() throws Exception {
    super.setUp();
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    page = (BaseActivity)getInstrumentation().startActivitySync(
        new Intent(getInstrumentation().getTargetContext(), CalendarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    monitor = new Instrumentation.ActivityMonitor() {
      @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
        outbound.add(new Intent(intent));
        if (rejectSms && Intent.ACTION_SENDTO.equals(intent.getAction())) throw new SecurityException("Synthetic unavailable SMS app");
        if (rejectNavigation && Intent.ACTION_VIEW.equals(intent.getAction())) throw new ActivityNotFoundException("Synthetic unavailable map app");
        return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
      }
    };
    getInstrumentation().addMonitor(monitor);
  }
  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }
  private AlertDialog dialog(Class<?> owner, String name) throws Exception {
    Field field = owner.getDeclaredField(name); field.setAccessible(true);
    return (AlertDialog)field.get(page);
  }
  private void chooseEmergency(int index) throws Exception {
    main(page::showEmergencyActions);
    AlertDialog dialog = dialog(BaseActivity.class, "emergency");
    main(() -> dialog.getListView().performItemClick(null, index, index));
  }
  public void testDialerDraftsAndSourcesNeverSubmit() throws Exception {
    String[] numbers = {"112", "119", "110"};
    for (int i = 0; i < numbers.length; i++) {
      chooseEmergency(i);
      Intent intent = outbound.get(outbound.size()-1);
      assertEquals(Intent.ACTION_DIAL, intent.getAction());
      assertEquals("tel:" + numbers[i], intent.getDataString());
      assertNull(intent.getExtras());
    }
    chooseEmergency(3);
    AlertDialog draft = dialog(BaseActivity.class, "emergency");
    main(() -> draft.getListView().performItemClick(null, 0, 0));
    Intent sms = outbound.get(outbound.size()-1);
    assertEquals(Intent.ACTION_SENDTO, sms.getAction());
    assertEquals("smsto:", sms.getDataString());
    assertEquals(1, sms.getExtras().size());
    assertNotNull(sms.getStringExtra("sms_body"));
    rejectSms = true;
    chooseEmergency(3);
    AlertDialog fallback = dialog(BaseActivity.class, "emergency");
    main(() -> fallback.getListView().performItemClick(null, 0, 0));
    Intent chooser = outbound.get(outbound.size()-1);
    assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
    Intent share = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
    assertEquals(Intent.ACTION_SEND, share.getAction());
    assertEquals("text/plain", share.getType());
    assertEquals(sms.getStringExtra("sms_body"), share.getStringExtra(Intent.EXTRA_TEXT));
    chooseEmergency(6);
    AlertDialog scope = dialog(BaseActivity.class, "emergency");
    main(() -> scope.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
    AlertDialog source = dialog(BaseActivity.class, "emergency");
    main(() -> source.getListView().performItemClick(null, 2, 2));
    assertEquals("https://polri.go.id/kontak/call-center-110", outbound.get(outbound.size()-1).getDataString());
    for (Intent intent : outbound) assertFalse(Intent.ACTION_CALL.equals(intent.getAction()));
  }
  public void testMapFallbackStopsAtManualCopyOffer() throws Exception {
    getInstrumentation().removeMonitor(monitor);
    main(page::finish);
    page = (BaseActivity)getInstrumentation().startActivitySync(
        new Intent(getInstrumentation().getTargetContext(), NearbyMosquesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().addMonitor(monitor);
    com.zikra.app.mosque.MosquePlace place = new com.zikra.app.mosque.MosquePlace(
        "fixture", "Masjid uji", "Alamat uji", "osm", -7.28, 112.8,
        com.zikra.app.location.LocationPoint.INITIAL);
    Method directions = NearbyMosquesActivity.class.getDeclaredMethod("directions", com.zikra.app.mosque.MosquePlace.class);
    directions.setAccessible(true);
    main(() -> {
      try { directions.invoke(page, place); }
      catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    });
    assertEquals(1, outbound.size());
    assertEquals("/maps/dir/", outbound.get(0).getData().getPath());
    assertEquals("-7.28,112.8", outbound.get(0).getData().getQueryParameter("destination"));
    outbound.clear();
    rejectNavigation = true;
    Method navigate = NearbyMosquesActivity.class.getDeclaredMethod("openNavigation", String.class, double.class, double.class, String.class, boolean.class);
    navigate.setAccessible(true);
    main(() -> {
      try { navigate.invoke(page, "https://www.google.com/maps/search/?api=1&query=masjid", -7.28, 112.8, "Lokasi uji", true); }
      catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    });
    assertEquals(2, outbound.size());
    assertEquals("https", outbound.get(0).getData().getScheme());
    assertEquals("geo", outbound.get(1).getData().getScheme());
    AlertDialog copy = dialog(NearbyMosquesActivity.class, "sourceDialog");
    assertTrue(copy.isShowing());
    assertNotNull(copy.getButton(AlertDialog.BUTTON_POSITIVE));
    main(() -> copy.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
    assertEquals(2, outbound.size());
  }
  @Override protected void tearDown() throws Exception {
    if (monitor != null) getInstrumentation().removeMonitor(monitor);
    if (page != null) main(page::finish);
    super.tearDown();
  }
}
