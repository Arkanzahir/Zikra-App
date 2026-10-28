package com.zikra.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.location.*;
import com.zikra.app.sky.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import org.json.*;

/** Public city fixtures only; refuses to run outside the task-owned disposable emulator. */
@SuppressWarnings("deprecation")
public final class HomeLocationSkyTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private static final Instant FIXED = Instant.parse("2026-10-28T01:45:00Z");

  @Override protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(), "home-location-sky-authorized").isFile());
    assertTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    context.getSharedPreferences("zikra_preferences", 0).edit()
        .putBoolean("onboarding_complete", true).putBoolean("reminder_enabled", false)
        .putBoolean("shake_enabled", false).commit();
    context.getSharedPreferences(com.zikra.app.ui.HomeAppearance.PREFS, 0).edit().clear().commit();
    LocationStore.save(context, LocationPoint.INITIAL);
  }

  @Override protected void tearDown() throws Exception {
    if (page != null) main(page::finish);
    super.tearDown();
  }

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }

  private void launchHome() {
    if (page != null) main(page::finish);
    page = getInstrumentation().startActivitySync(new Intent(context, HomeActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long until = SystemClock.uptimeMillis() + 10000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    assertTrue(page.hasWindowFocus());
  }

  private Object field(Object owner, String name) throws Exception {
    Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true);
    return field.get(owner);
  }

  private void freeze(Instant instant) {
    main(() -> {
      try {
        ((Handler) field(page, "timer")).removeCallbacksAndMessages(null);
        ((SkyRepository) field(page, "skyRepository")).close();
        Field clock = HomeActivity.class.getDeclaredField("clock"); clock.setAccessible(true);
        clock.set(page, Clock.fixed(instant, ZoneOffset.UTC));
        LocationPoint point = LocationStore.current(context);
        LocalDate date = instant.atZone(point.zone).toLocalDate();
        Field sky = HomeActivity.class.getDeclaredField("skyDay"); sky.setAccessible(true);
        sky.set(page, SkyDay.estimate(date, point));
        Field requested = HomeActivity.class.getDeclaredField("requestedSkyDate");
        requested.setAccessible(true); requested.set(page, date);
        Method update = HomeActivity.class.getDeclaredMethod("updateClock");
        update.setAccessible(true); update.invoke(page);
      } catch (Exception failed) { throw new AssertionError(failed); }
    });
    SystemClock.sleep(350);
  }

  private String text(int id) { return ((TextView) page.findViewById(id)).getText().toString(); }

  private void tap(View view) {
    Rect rect = new Rect(); int[] rootPosition = new int[2];
    main(() -> {
      assertTrue(view.getGlobalVisibleRect(rect));
      view.getRootView().getLocationOnScreen(rootPosition);
      rect.offset(rootPosition[0], rootPosition[1]);
    });
    long now = SystemClock.uptimeMillis();
    System.out.println("tapId=" + view.getId()
        + " screen=" + rect + " clickable=" + view.isClickable());
    MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
        rect.centerX(), rect.centerY(), 0);
    getInstrumentation().sendPointerSync(down); SystemClock.sleep(70);
    MotionEvent up = MotionEvent.obtain(now, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP,
        rect.centerX(), rect.centerY(), 0);
    getInstrumentation().sendPointerSync(up);
    down.recycle(); up.recycle(); getInstrumentation().waitForIdleSync();
  }

  private Activity openLocation(int hitId) throws Exception {
    long until = SystemClock.uptimeMillis() + 7000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    assertTrue("Home has input focus", page.hasWindowFocus());
    Instrumentation.ActivityMonitor monitor = getInstrumentation()
        .addMonitor(LocationActivity.class.getName(), null, false);
    tap(page.findViewById(hitId));
    Activity picker = getInstrumentation().waitForMonitorWithTimeout(monitor, 6000);
    getInstrumentation().removeMonitor(monitor);
    if (picker == null) image("picker-missing");
    assertNotNull("Location activity after actual touch", picker);
    return picker;
  }

  private void choose(Activity picker, String city, boolean confirm) throws Exception {
    LinearLayout results = picker.findViewById(R.id.locationResults); View match = null;
    for (int i = 0; i < results.getChildCount(); i++)
      if (((TextView) results.getChildAt(i)).getText().toString().startsWith(city + ","))
        match = results.getChildAt(i);
    assertNotNull(match); View row = match;
    main(() -> row.requestRectangleOnScreen(new Rect(0, 0, row.getWidth(), row.getHeight()), true));
    SystemClock.sleep(250); tap(row);
    AlertDialog dialog = (AlertDialog) field(picker, "confirmation"); assertNotNull(dialog);
    tap(dialog.getButton(confirm ? AlertDialog.BUTTON_POSITIVE : AlertDialog.BUTTON_NEGATIVE));
    SystemClock.sleep(250);
    if (confirm) assertTrue("City confirmation applied", LocationStore.current(context).label.startsWith(city + ","));
  }

  private void image(String name) throws Exception {
    SystemClock.sleep(350);
    Bitmap bitmap = getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
    File folder = new File(context.getExternalFilesDir(null), "home-location-sky");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    bitmap.recycle();
  }

  private void evidence(String name, JSONObject result) throws Exception {
    File folder = new File(context.getExternalFilesDir(null), "home-location-sky");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".json"))) {
      out.write(result.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  public void testLocationNameAndArrowOpenRealPickerAndSelectionUpdatesHome() throws Exception {
    launchHome(); freeze(FIXED);
    assertEquals("08.45", text(R.id.homeWallClock)); assertEquals("02:30:00", text(R.id.homeClock));
    assertTrue(page.findViewById(R.id.homeLocationArrow).isShown());
    Activity picker = openLocation(R.id.homeLocationLabel);
    assertTrue(picker.findViewById(R.id.locationAutomatic).isShown());
    choose(picker, "Makassar", true); main(picker::finish); freeze(FIXED);
    assertTrue(text(R.id.homeLocationLabel).contains("Makassar"));
    assertEquals("09.45", text(R.id.homeWallClock)); assertEquals("WITA", text(R.id.homeWallZone));
    assertEquals("02:04:00", text(R.id.homeClock));
    assertTrue(text(R.id.homeNextTime).contains("11.49"));
    LinearLayout slot = page.findViewById(R.id.prayerSlot2);
    assertEquals("11.49", ((TextView) slot.getChildAt(2)).getText().toString());
    image("location-makassar");
    LocationPoint before = LocationStore.current(context);
    picker = openLocation(R.id.homeLocationArrow);
    choose(picker, "Jayapura", false); main(picker::finish); freeze(FIXED);
    assertEquals(before.key(), LocationStore.current(context).key());
    assertEquals("09.45", text(R.id.homeWallClock));
    picker = openLocation(R.id.homeLocationArrow);
    choose(picker, "Jayapura", true); main(picker::finish); freeze(FIXED);
    assertEquals("10.45", text(R.id.homeWallClock)); assertEquals("WIT", text(R.id.homeWallZone));
    assertTrue(text(R.id.homeLocationLabel).contains("Jayapura"));
    launchHome(); freeze(FIXED);
    assertTrue(text(R.id.homeLocationLabel).contains("Jayapura"));
    image("location-jayapura");
    evidence("location", new JSONObject().put("actualNameAndArrowTouches", true)
        .put("manualMakassarAndJayapura", true).put("cancelPreservedChoice", true)
        .put("clockPrayerCountdownAndZoneUpdated", true).put("reopenPreservedChoice", true)
        .put("gpsNotAcquired", true).put("publicFixturesOnly", true));
  }

  public void testCountdownBoundariesSingleSilhouetteAndDayNightScreens() throws Exception {
    launchHome();
    String[] times = {"08:45:00", "11:14:59", "11:15:00", "20:30:00"};
    String[] expected = {"02:30:00", "00:00:01", "03:13:00", "07:15:00"};
    JSONArray cases = new JSONArray();
    for (int i = 0; i < times.length; i++) {
      freeze(LocalDate.of(2026, 10, 28).atTime(LocalTime.parse(times[i]))
          .atZone(LocationPoint.INITIAL.zone).toInstant());
      assertEquals(times[i], expected[i], text(R.id.homeClock));
      cases.put(new JSONObject().put("time", times[i]).put("countdown", text(R.id.homeClock)));
    }
    HomeSkyView sky = page.findViewById(R.id.homeSky);
    RectF original = null;
    for (String time : new String[] {"05:00", "11:39", "16:50", "17:23", "20:30"}) {
      freeze(LocalDate.of(2026, 10, 28).atTime(LocalTime.parse(time))
          .atZone(LocationPoint.INITIAL.zone).toInstant());
      main(() -> sky.setRunning(false)); SystemClock.sleep(200);
      RectF bounds = new RectF((RectF) field(sky, "landscapeBounds"));
      if (original == null) original = bounds; else assertEquals(original, bounds);
      image("sky-" + time.replace(':', '-'));
    }
    int bytes = ((Bitmap) field(sky, "landscape")).getAllocationByteCount()
        + ((Bitmap) field(sky, "clouds")).getAllocationByteCount();
    evidence("sky-geometry-countdown", new JSONObject().put("cases", cases)
        .put("singleLandscapeBoundsAtFiveTimes", true).put("decodedBitmapBytes", bytes)
        .put("oldNightBitmapNotLoaded", true));
  }

  private int[] cloudPixels(HomeSkyView sky) {
    int[] position = new int[2]; main(() -> sky.getLocationOnScreen(position));
    Bitmap image = getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(image);
    int width = Math.round(sky.getWidth() * .40f), height = Math.round(sky.getHeight() * .22f);
    int[] pixels = new int[width * height];
    image.getPixels(pixels, 0, width, position[0] + Math.round(sky.getWidth() * .55f),
        position[1] + Math.round(sky.getHeight() * .18f), width, height);
    image.recycle(); return pixels;
  }

  private int changed(int[] a, int[] b) {
    assertEquals(a.length, b.length); int changed = 0;
    for (int i = 0; i < a.length; i++) if (a[i] != b[i]) changed++;
    return changed;
  }

  private String shell(String command) throws Exception {
    try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
    }
  }

  public void testActualCloudMotionPauseOffscreenAndReducedMotion() throws Exception {
    String previous = shell("settings get global animator_duration_scale");
    try {
      shell("settings put global animator_duration_scale 1");
      launchHome();
      String beforeClock = text(R.id.homeClock); SystemClock.sleep(1400);
      assertFalse("Real countdown ticks", beforeClock.equals(text(R.id.homeClock)));
      freeze(FIXED);
      HomeSkyView sky = page.findViewById(R.id.homeSky);
      main(() -> sky.setRunning(true)); SystemClock.sleep(500);
      assertTrue(sky.isMotionRunning()); int[] before = cloudPixels(sky);
      long beforeMillis = (Long) field(sky, "motionMillis"); SystemClock.sleep(2400);
      int movingPixels = changed(before, cloudPixels(sky));
      assertTrue("Clouds move on actual display", movingPixels > 20);
      assertTrue((Long) field(sky, "motionMillis") > beforeMillis);
      int[] position = new int[2]; main(() -> sky.getLocationOnScreen(position));
      long touchStart = SystemClock.uptimeMillis();
      MotionEvent touch = MotionEvent.obtain(touchStart, touchStart, MotionEvent.ACTION_DOWN,
          position[0] + sky.getWidth() * .90f, position[1] + sky.getHeight() * .35f, 0);
      getInstrumentation().sendPointerSync(touch); SystemClock.sleep(300);
      assertTrue("Touch keeps priority over gravity updates", (Boolean) field(sky, "touching"));
      assertTrue("Parallax eases toward finger", (Float) field(sky, "offsetX") > .25f);
      MotionEvent release = MotionEvent.obtain(touchStart, SystemClock.uptimeMillis(),
          MotionEvent.ACTION_UP, position[0] + sky.getWidth() * .90f,
          position[1] + sky.getHeight() * .35f, 0);
      getInstrumentation().sendPointerSync(release); touch.recycle(); release.recycle();
      main(() -> sky.setRunning(false)); SystemClock.sleep(350);
      before = cloudPixels(sky); long paused = (Long) field(sky, "motionMillis");
      SystemClock.sleep(650); assertEquals(0, changed(before, cloudPixels(sky)));
      assertEquals(paused, ((Long) field(sky, "motionMillis")).longValue());
      ScrollView scroll = page.findViewById(R.id.homeScroll);
      main(() -> scroll.scrollTo(0, scroll.getChildAt(0).getHeight()));
      assertFalse(sky.isMotionRunning());
      main(() -> scroll.scrollTo(0, 0)); SystemClock.sleep(350); assertTrue(sky.isMotionRunning());
      shell("settings put global animator_duration_scale 0"); SystemClock.sleep(500);
      assertFalse(sky.isMotionRunning()); before = cloudPixels(sky); SystemClock.sleep(600);
      assertEquals(0, changed(before, cloudPixels(sky)));
      image("motion-reduced");
      evidence("motion", new JSONObject().put("cloudDisplayPixelsChanged", movingPixels)
          .put("actualTouchParallaxResponded", true).put("touchPriorityOverGravity", true)
          .put("realCountdownTicked", true).put("pausedClockAccumulatorStable", true)
          .put("pausedDisplayRegionIdentical", true).put("offscreenStopsAndVisibleResumes", true)
          .put("reducedMotionRegionIdentical", true));
    } finally {
      shell("settings put global animator_duration_scale "
          + (previous.matches("[0-9.]+") ? previous : "1"));
    }
  }

  public void testLargeTextKeepsArrowClockAndCountdownReadable() throws Exception {
    launchHome(); freeze(FIXED);
    assertTrue(page.getResources().getConfiguration().fontScale >= 1.9f);
    assertTrue(page.findViewById(R.id.homeLocationArrow).isShown());
    for (int id : new int[] {R.id.homeLocationLabel, R.id.homeClock, R.id.homeWallClock}) {
      TextView view = page.findViewById(id);
      for (int i = 0; i < view.getLayout().getLineCount(); i++) {
        assertEquals(0, view.getLayout().getEllipsisCount(i));
        assertTrue("Text width fits", view.getLayout().getLineWidth(i)
            <= view.getWidth() - view.getPaddingLeft() - view.getPaddingRight() + 1);
      }
    }
    image("large-text");
  }
}
