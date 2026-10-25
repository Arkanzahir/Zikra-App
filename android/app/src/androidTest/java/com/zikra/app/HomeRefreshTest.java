package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.Context;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.zikra.app.assistant.AssistantConnection;
import com.zikra.app.data.ZikraDbHelper;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Synthetic UI fixtures run only after a host-side emulator backup and explicit marker. */
@SuppressWarnings("deprecation")
public final class HomeRefreshTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;

  @Override protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))) {
      assertEquals("Emulator only", "1", new String(input.readAllBytes(), StandardCharsets.UTF_8).trim());
    }
    assertTrue("Host must back up and isolate emulator app data first",
        new File(context.getFilesDir(), "home-ai-ui-test-authorized").isFile());
    context.getSharedPreferences("zikra_preferences", 0).edit()
        .putBoolean("onboarding_complete", true).remove("assistant_history_v1")
        .putString("last_surah", "Al-Kahf").putInt("last_ayah", 12).commit();
    com.zikra.app.location.LocationStore.save(context, new com.zikra.app.location.LocationPoint(
        -7.2575, 112.7521, "Asia/Jakarta", "Surabaya", "manual", 0, -1));
  }

  @Override protected void tearDown() throws Exception {
    close();
    super.tearDown();
  }

  private <T extends Activity> T launch(Class<T> type) {
    page = getInstrumentation().startActivitySync(new Intent(context, type)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    getInstrumentation().waitForIdleSync();
    SystemClock.sleep(700);
    return type.cast(page);
  }

  private void close() {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    page = null;
    getInstrumentation().waitForIdleSync();
  }

  private void capture(String name) {
    String scale = context.getResources().getConfiguration().fontScale >= 1.9f ? "200" : "100";
    UiEvidence.capture(getInstrumentation(), "Refresh-" + name + "-" + scale);
  }

  public void testHomeEmptyAndRealAgendaScrollNavigation() throws Exception {
    HomeActivity home = launch(HomeActivity.class);
    ScrollView scroll = home.findViewById(R.id.homeScroll);
    TextView clockView = home.findViewById(R.id.homeClock);
    assertEquals("Countdown is not clipped", clockView.length(),
        clockView.getLayout().getLineEnd(clockView.getLineCount() - 1));
    String countdown = clockView.getText().toString();
    assertTrue("Live countdown includes hours, minutes and seconds", countdown.matches("[0-9]{2}:[0-9]{2}:[0-9]{2}"));
    com.zikra.app.location.LocationPoint point = com.zikra.app.location.LocationStore.current(context);
    PrayerSchedule.NextPrayer event = PrayerSchedule.nextAfter(java.time.ZonedDateTime.now(point.zone), point);
    assertNotNull(event);
    assertEquals(context.getString(R.string.refresh_home_towards, new String[] {"Subuh","Terbit","Zuhur","Asar","Maghrib","Isya"}[event.index]),
        ((TextView) home.findViewById(R.id.homeNextPrayer)).getText().toString());
    assertTrue(((TextView) home.findViewById(R.id.homeNextTime)).getText().length() > 5);
    try (ZikraDbHelper db = new ZikraDbHelper(context)) {
      assertTrue("Isolated database must start empty", db.getTasks().isEmpty());
    }
    assertEquals(context.getString(R.string.refresh_home_empty_title),
        ((TextView) home.findViewById(R.id.homeAgendaCount)).getText().toString());
    assertEquals("Al-Kahf · Ayat 12", ((TextView) home.findViewById(R.id.homeLastRead)).getText().toString());
    assertTrue("Home extends below viewport", scroll.canScrollVertically(1));
    android.view.View brand = home.findViewById(R.id.homeRoot).findViewWithTag("zikraBrand");
    android.graphics.Rect brandBounds = new android.graphics.Rect();
    assertTrue(brand.getGlobalVisibleRect(brandBounds));
    assertEquals("Brand not vertically clipped", brand.getHeight(), brandBounds.height());
    android.view.WindowInsets insets = home.getWindow().getDecorView().getRootWindowInsets();
    int statusBottom = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
    assertTrue("Brand below actual status bar", brandBounds.top >= statusBottom);
    android.graphics.Rect heroBounds = new android.graphics.Rect(), stripBounds = new android.graphics.Rect();
    assertTrue(home.findViewById(R.id.homeHero).getGlobalVisibleRect(heroBounds));
    assertTrue(home.findViewById(R.id.homePrayerStrip).getGlobalVisibleRect(stripBounds));
    assertTrue("Prayer strip remains inside interactive hero bounds", heroBounds.contains(stripBounds));
    for (int id : new int[] {R.id.dockHome, R.id.dockQuran, R.id.dockAi, R.id.dockStudy, R.id.dockProfile}) {
      TextView label = home.findViewById(id).findViewById(R.id.navDestinationLabel);
      android.graphics.Rect bounds = new android.graphics.Rect();
      assertTrue(label.getGlobalVisibleRect(bounds));
      assertEquals("Navigation text not vertically clipped", label.getHeight(), bounds.height());
    }
    capture("home-top");
    getInstrumentation().runOnMainSync(() -> scroll.scrollTo(0, scroll.getChildAt(0).getHeight()));
    getInstrumentation().waitForIdleSync();
    Rect last = new Rect(), dock = new Rect();
    assertTrue(home.findViewById(R.id.homeFocusCard).getGlobalVisibleRect(last));
    assertTrue(home.getWindow().getDecorView().findViewWithTag("zikraDock").getGlobalVisibleRect(dock));
    assertTrue("Focus action above dock", last.bottom <= dock.top);
    capture("home-empty-bottom");
    close();
    long[] ids = new long[5];
    try (ZikraDbHelper db = new ZikraDbHelper(context)) {
      ids[0] = db.addAcademicTask("[Uji] Tanpa tenggat", 0, false);
      ids[1] = db.addAcademicTask("[Uji] Selesaikan rangkuman", 1800000000000L, false);
      ids[2] = db.addAcademicTask("[Uji] Baca catatan", 1790000000000L, false);
      ids[3] = db.addAcademicTask("[Uji] Latihan soal", 1810000000000L, false);
      ids[4] = db.addAcademicTask("[Uji] Sudah selesai", 1780000000000L, false);
      db.setCompleted(ids[4], true);
    }
    try {
      home = launch(HomeActivity.class);
      LinearLayout items = home.findViewById(R.id.homeAgendaItems);
      assertEquals(context.getString(R.string.refresh_home_agenda_count, 4),
          ((TextView) home.findViewById(R.id.homeAgendaCount)).getText().toString());
      assertEquals("Three rows with dividers", 5, items.getChildCount());
      assertEquals("[Uji] Baca catatan", ((TextView) ((LinearLayout) items.getChildAt(0)).getChildAt(0)).getText().toString());
      assertEquals("[Uji] Latihan soal", ((TextView) ((LinearLayout) items.getChildAt(4)).getChildAt(0)).getText().toString());
      ScrollView filled = home.findViewById(R.id.homeScroll);
      getInstrumentation().runOnMainSync(() -> filled.scrollTo(0, items.getTop() + ((View) items.getParent()).getTop()));
      getInstrumentation().waitForIdleSync();
      capture("home-agenda");
      Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(StudyActivity.class.getName(), null, false);
      try {
        getInstrumentation().runOnMainSync(() -> items.getChildAt(0).performClick());
        Activity destination = getInstrumentation().waitForMonitorWithTimeout(monitor, 5000);
        assertNotNull("Agenda opens actual Study", destination);
        getInstrumentation().runOnMainSync(destination::finish);
      } finally { getInstrumentation().removeMonitor(monitor); }
      Instrumentation.ActivityMonitor focus = getInstrumentation().addMonitor(FocusTimerActivity.class.getName(), null, false);
      try {
        HomeActivity current = home;
        getInstrumentation().runOnMainSync(() -> current.findViewById(R.id.homeFocusCard).performClick());
        Activity destination = getInstrumentation().waitForMonitorWithTimeout(focus, 5000);
        assertNotNull("Last action opens actual focus timer", destination);
        getInstrumentation().runOnMainSync(destination::finish);
      } finally { getInstrumentation().removeMonitor(focus); }
    } finally {
      close();
      try (ZikraDbHelper db = new ZikraDbHelper(context)) {
        for (long id : ids) db.deleteTask(id);
      }
    }
  }

  public void testSkyDayNightPhaseAndOffscreenMotion() throws Exception {
    HomeActivity home = launch(HomeActivity.class);
    com.zikra.app.sky.HomeSkyView sky = home.findViewById(R.id.homeSky);
    java.lang.reflect.Field field = HomeActivity.class.getDeclaredField("timer");
    field.setAccessible(true);
    android.os.Handler timer = (android.os.Handler) field.get(home);
    java.lang.reflect.Field repository = HomeActivity.class.getDeclaredField("skyRepository");
    repository.setAccessible(true);
    ((com.zikra.app.sky.SkyRepository) repository.get(home)).close();
    com.zikra.app.sky.SkyDay day = new com.zikra.app.sky.SkyDay(java.time.LocalDate.of(2026,10,25),
        java.time.LocalTime.of(6,0), java.time.LocalTime.NOON, java.time.LocalTime.of(18,0), .18, true, false);
    getInstrumentation().runOnMainSync(() -> {
      timer.removeCallbacksAndMessages(null);
      setHomeTime(home, day, java.time.LocalTime.of(12,30));
      sky.setSky(day, java.time.LocalTime.of(12,30));
    });
    getInstrumentation().waitForIdleSync();
    assertTrue(sky.isDaytime());
    int[] motionBefore = cloudPixels(home, sky);
    android.os.SystemClock.sleep(2400);
    int[] motionMiddle = cloudPixels(home, sky);
    android.os.SystemClock.sleep(2400);
    int[] motionAfter = cloudPixels(home, sky);
    int changed = Math.max(changedPixels(motionBefore, motionMiddle), changedPixels(motionBefore, motionAfter));
    assertTrue("Cloud region actually changes on the display", changed > 10);
    awaitSkyFrame(sky, true);
    int dayWarmLamps = illuminatedWindowPixels(sky);
    capture("sky-day");
    getInstrumentation().runOnMainSync(() -> {
      setHomeTime(home, day, java.time.LocalTime.of(20,0));
      sky.setSky(day, java.time.LocalTime.of(20,0));
    });
    getInstrumentation().waitForIdleSync();
    assertFalse(sky.isDaytime());
    com.zikra.app.location.LocationPoint point = com.zikra.app.location.LocationStore.current(context);
    PrayerSchedule.NextPrayer next = PrayerSchedule.nextAfter(day.date.atTime(20,0).atZone(point.zone), point);
    assertTrue("Night advances to tomorrow", next.time.toLocalDate().isAfter(day.date));
    LinearLayout firstPrayer = home.findViewById(R.id.prayerSlot0);
    assertEquals("Highlighted Subuh uses next day's time",
        next.time.format(java.time.format.DateTimeFormatter.ofPattern("HH.mm", java.util.Locale.ROOT)),
        ((TextView) firstPrayer.getChildAt(2)).getText().toString());
    awaitSkyFrame(sky, false);
    int nightWarmLamps = illuminatedWindowPixels(sky);
    assertTrue("Artificial window glow appears at night and is absent in daylight",
        nightWarmLamps > Math.max(10, dayWarmLamps * 2));
    capture("sky-crescent");
    com.zikra.app.sky.SkyDay full = new com.zikra.app.sky.SkyDay(day.date, day.rise, day.noon, day.set, 1, true, false);
    getInstrumentation().runOnMainSync(() -> sky.setSky(full, java.time.LocalTime.of(20,0)));
    getInstrumentation().waitForIdleSync();
    android.os.SystemClock.sleep(400);
    awaitSkyFrame(sky, false);
    capture("sky-full-moon");
    ScrollView scroll = home.findViewById(R.id.homeScroll);
    getInstrumentation().runOnMainSync(() -> scroll.scrollTo(0, scroll.getChildAt(0).getHeight()));
    getInstrumentation().waitForIdleSync();
    assertFalse("Offscreen sky stops animation", sky.isMotionRunning());
    getInstrumentation().runOnMainSync(() -> scroll.scrollTo(0, 0));
    getInstrumentation().waitForIdleSync();
    android.os.SystemClock.sleep(250);
    assertTrue("Visible foreground sky resumes", sky.isMotionRunning());
    String old = shell("settings get global animator_duration_scale").trim();
    try {
      shell("settings put global animator_duration_scale 0");
      android.os.SystemClock.sleep(400);
      assertFalse("System disables ambient motion", sky.isMotionRunning());
      getInstrumentation().runOnMainSync(() -> sky.setSky(day, java.time.LocalTime.of(12,30)));
      android.os.SystemClock.sleep(400);
      int[] stillBefore = cloudPixels(home, sky);
      android.os.SystemClock.sleep(600);
      assertEquals("Reduced-motion display stays still", 0, changedPixels(stillBefore, cloudPixels(home, sky)));
      String scale = context.getResources().getConfiguration().fontScale >= 1.9f ? "200" : "100";
      File proof = new File(context.getExternalFilesDir(null), "validation/Refresh-motion-" + scale + ".json");
      try (java.io.FileOutputStream out = new java.io.FileOutputStream(proof)) {
        out.write(new org.json.JSONObject().put("cloudRegionPixelsChanged", changed)
            .put("dayWarmLampPixels", dayWarmLamps).put("nightWarmLampPixels", nightWarmLamps)
            .put("nightHasMoreLampEmission", true)
            .put("reducedMotionRegionIdentical", true).put("actualDisplayScreenshots", true)
            .toString().getBytes(StandardCharsets.UTF_8));
      }
    } finally {
      shell("settings put global animator_duration_scale " + (old.matches("[0-9.]+") ? old : "1"));
    }
  }

  private void awaitSkyFrame(android.view.View sky, boolean daytime) {
    int[] position = new int[2];
    getInstrumentation().runOnMainSync(() -> sky.getLocationOnScreen(position));
    long deadline = android.os.SystemClock.uptimeMillis() + 5000;
    do {
      android.os.SystemClock.sleep(150);
      android.graphics.Bitmap screen = getInstrumentation().getUiAutomation().takeScreenshot();
      assertNotNull("Rendered sky screenshot", screen);
      int pixel = screen.getPixel(position[0] + sky.getWidth()/2,
          position[1] + Math.round(sky.getHeight() * .05f));
      screen.recycle();
      int brightness = (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel)
          + android.graphics.Color.blue(pixel)) / 3;
      if (daytime ? brightness > 70 : brightness < 60) return;
    } while (android.os.SystemClock.uptimeMillis() < deadline);
    fail("Display must render the requested " + (daytime ? "day" : "night") + " frame before capture");
  }

  private int illuminatedWindowPixels(android.view.View sky) {
    int[] position = new int[2];
    getInstrumentation().runOnMainSync(() -> sky.getLocationOnScreen(position));
    android.graphics.Bitmap screen = getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull("Actual display for lamp comparison", screen);
    float w = sky.getWidth(), u = w / 400f, sceneHeight = w * 2f / 3f + 4 * u;
    int left = Math.max(0, Math.round(position[0] - 2 * u + .43f * (w + 44 * u)));
    int right = Math.min(screen.getWidth(), Math.round(position[0] - 2 * u + .96f * (w + 44 * u)));
    float sceneTop = position[1] + sky.getHeight() - w * 2f / 3f - 24 * u;
    int top = Math.max(0, Math.round(sceneTop + .72f * sceneHeight));
    int bottom = Math.min(screen.getHeight(), Math.round(sceneTop + .84f * sceneHeight));
    int count = 0;
    for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
      int pixel = screen.getPixel(x, y);
      int r = android.graphics.Color.red(pixel), g = android.graphics.Color.green(pixel), b = android.graphics.Color.blue(pixel);
      if (r > 110 && r > g * 1.45f && g > b * 1.5f) count++;
    }
    screen.recycle(); return count;
  }

  private int[] cloudPixels(HomeActivity home, android.view.View sky) {
    int[] position = new int[2];
    getInstrumentation().runOnMainSync(() -> sky.getLocationOnScreen(position));
    android.graphics.Bitmap screenshot = getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull("Display screenshot", screenshot);
    int left = position[0] + Math.round(sky.getWidth() * .25f);
    int top = position[1] + Math.round(sky.getHeight() * .12f);
    int width = Math.round(sky.getWidth() * .65f), height = Math.round(sky.getHeight() * .23f);
    int[] pixels = new int[width * height];
    screenshot.getPixels(pixels, 0, width, left, top, width, height);
    screenshot.recycle(); return pixels;
  }

  private int changedPixels(int[] before, int[] after) {
    assertEquals(before.length, after.length);
    int count = 0; for (int i = 0; i < before.length; i++) if (before[i] != after[i]) count++;
    return count;
  }

  private void setHomeTime(HomeActivity home, com.zikra.app.sky.SkyDay day, java.time.LocalTime time) {
    try {
      java.time.ZoneId zone = com.zikra.app.location.LocationStore.current(context).zone;
      java.lang.reflect.Field clock = HomeActivity.class.getDeclaredField("clock"); clock.setAccessible(true);
      clock.set(home, java.time.Clock.fixed(day.date.atTime(time).atZone(zone).toInstant(), zone));
      java.lang.reflect.Method refresh = HomeActivity.class.getDeclaredMethod("updateClock"); refresh.setAccessible(true);
      refresh.invoke(home);
    } catch (ReflectiveOperationException failed) { throw new AssertionError(failed); }
  }

  private String shell(String command) throws Exception {
    try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
