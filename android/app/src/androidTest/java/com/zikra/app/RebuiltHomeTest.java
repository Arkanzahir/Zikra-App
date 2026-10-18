package com.zikra.app;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.test.InstrumentationTestCase;
import android.widget.TextView;
import com.zikra.app.sky.*;
import java.time.*;

@SuppressWarnings("deprecation")
public final class RebuiltHomeTest extends InstrumentationTestCase {
  private Activity page;
  private boolean onboarded;
  private float animator;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Context c = getInstrumentation().getTargetContext();
    onboarded =
        c.getSharedPreferences("zikra_preferences", 0).getBoolean("onboarding_complete", false);
    c.getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .commit();
    animator =
        Settings.Global.getFloat(
            c.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
  }

  private void launch() {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long end = SystemClock.uptimeMillis() + 30000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue(page.hasWindowFocus());
    SystemClock.sleep(900);
  }

  public void testUsnoValidationRejectsWrongDateAndLocation() throws Exception {
    String json =
        readText(
            getInstrumentation().getContext().getAssets().open("usno-sukolilo-2026-10-18.json"));
    SkyDay d = SkyRepository.parse(json, LocalDate.of(2026, 10, 18));
    assertTrue(d.usno);
    assertEquals(.46, d.illumination, .0001);
    assertEquals(LocalTime.of(11, 14), d.noon);
    try {
      SkyRepository.parse(json, LocalDate.of(2026, 10, 19));
      fail("Reject stale date");
    } catch (org.json.JSONException expected) {
    }
    try {
      SkyRepository.parse(json.replace("112.79", "113.79"), d.date);
      fail("Reject other location");
    } catch (org.json.JSONException expected) {
    }
  }

  public void testDayNightMotionPauseAndReducedMotion() throws Exception {
    launch();
    HomeSkyView sky = page.findViewById(R.id.homeSky);
    long deadline = SystemClock.uptimeMillis() + 21000;
    while (!((TextView) page.findViewById(R.id.homeSkySource)).getText().toString().contains("USNO")
        && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(200);
    UiEvidence.capture(getInstrumentation(), "RebuiltHomeLive");
    SkyDay d =
        new SkyDay(
            LocalDate.now(PrayerSchedule.ZONE),
            LocalTime.of(5, 6),
            LocalTime.of(11, 14),
            LocalTime.of(17, 22),
            .46,
            true,
            true);
    getInstrumentation().runOnMainSync(() -> sky.setSky(d, LocalTime.NOON));
    assertTrue(sky.isDaytime());
    assertTrue(sky.isMotionRunning());
    Bitmap a = snapshot(sky);
    SystemClock.sleep(500);
    Bitmap b = snapshot(sky);
    assertFalse("Cloud motion changes actual pixels", a.sameAs(b));
    a.recycle();
    b.recycle();
    setIllustrationTime(d, LocalTime.of(11, 14), "11:14");
    UiEvidence.capture(getInstrumentation(), "RebuiltHomeDayIllustration");
    setIllustrationTime(d, LocalTime.of(19, 0), "19:00");
    assertFalse(sky.isDaytime());
    UiEvidence.capture(getInstrumentation(), "RebuiltHomeNightIllustration");
    getInstrumentation().runOnMainSync(() -> sky.setRunning(false));
    a = snapshot(sky);
    SystemClock.sleep(300);
    b = snapshot(sky);
    assertTrue(a.sameAs(b));
    a.recycle();
    b.recycle();
    getInstrumentation().runOnMainSync(() -> sky.setRunning(true));
    shell("settings put global animator_duration_scale 0");
    SystemClock.sleep(700);
    assertFalse("System reduced motion stops frames", sky.isMotionRunning());
    a = snapshot(sky);
    SystemClock.sleep(300);
    b = snapshot(sky);
    assertTrue(a.sameAs(b));
    a.recycle();
    b.recycle();
    assertEquals(1f, page.findViewById(R.id.homeClock).getAlpha(), .001f);
  }

  private void setIllustrationTime(SkyDay d, LocalTime time, String clock) {
    getInstrumentation()
        .runOnMainSync(
            () -> {
              ((HomeSkyView) page.findViewById(R.id.homeSky)).setSky(d, time);
              ((TextView) page.findViewById(R.id.homeClock)).setText(clock);
              ((TextView) page.findViewById(R.id.homeSkySource))
                  .setText("DATA UJI · ilustrasi " + clock);
            });
  }

  private Bitmap snapshot(HomeSkyView view) {
    Bitmap[] b = new Bitmap[1];
    getInstrumentation()
        .runOnMainSync(
            () -> {
              b[0] =
                  Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
              view.draw(new Canvas(b[0]));
            });
    return b[0];
  }

  private String readText(java.io.InputStream input) throws Exception {
    try (java.io.InputStream in = input;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int count;
      while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
      return out.toString("UTF-8");
    }
  }

  private void shell(String command) throws Exception {
    try (ParcelFileDescriptor fd =
        getInstrumentation().getUiAutomation().executeShellCommand(command)) {
      readText(new java.io.FileInputStream(fd.getFileDescriptor()));
    }
  }

  @Override
  protected void tearDown() throws Exception {
    shell("settings put global animator_duration_scale " + animator);
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", onboarded)
        .commit();
    super.tearDown();
  }
}
