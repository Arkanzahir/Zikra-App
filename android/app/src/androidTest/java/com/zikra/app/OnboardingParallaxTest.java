package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.Settings;
import android.test.InstrumentationTestCase;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.InputStream;

@SuppressWarnings("deprecation")
public final class OnboardingParallaxTest extends InstrumentationTestCase {
  private Activity page;
  private ImageView artwork;
  private String originalScale;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    originalScale =
        Settings.Global.getString(
            getInstrumentation().getTargetContext().getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE);
    setScale("1");
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), OnboardingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long end = SystemClock.uptimeMillis() + 30000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue("Onboarding receives focus", page.hasWindowFocus());
    artwork = page.findViewById(R.id.onboardIllustration);
  }

  public void testTouchDepthReturnsWithoutAdvancingOrExposingEdges() throws Exception {
    Matrix before = new Matrix(artwork.getImageMatrix());
    boolean record =
        "true"
            .equals(
                ((android.test.InstrumentationTestRunner) getInstrumentation())
                    .getArguments()
                    .getString("record_motion"));
    ParcelFileDescriptor recording =
        record
            ? getInstrumentation()
                .getUiAutomation()
                .executeShellCommand(
                    "screenrecord --bit-rate 1600000 --time-limit 10"
                        + " /sdcard/OnboardingParallax.mp4")
            : null;
    try {
      if (record) SystemClock.sleep(500);
      int[] location = new int[2];
      getInstrumentation().runOnMainSync(() -> artwork.getLocationOnScreen(location));
      float cx = location[0] + artwork.getWidth() * .5f;
      float cy = location[1] + artwork.getHeight() * .5f;
      float distance = 48 * page.getResources().getDisplayMetrics().density;
      long down = SystemClock.uptimeMillis();
      touch(MotionEvent.ACTION_DOWN, cx, cy, down);
      for (int frame = 1; frame <= 16; frame++) {
        touch(MotionEvent.ACTION_MOVE, cx + distance * frame / 16f, cy, down);
        if (record) SystemClock.sleep(35);
      }
      getInstrumentation()
          .runOnMainSync(
              () -> {
                assertFalse(
                    "Artwork moves during a short drag", before.equals(artwork.getImageMatrix()));
                View copy = page.findViewById(R.id.onboardCopy);
                assertTrue("Copy counter-moves to provide depth", copy.getTranslationX() < 0);
                RectF bounds =
                    new RectF(
                        0,
                        0,
                        artwork.getDrawable().getIntrinsicWidth(),
                        artwork.getDrawable().getIntrinsicHeight());
                artwork.getImageMatrix().mapRect(bounds);
                assertTrue(
                    "No horizontal uncovered edge",
                    bounds.left <= .1f && bounds.right >= artwork.getWidth() - .1f);
                assertTrue(
                    "No vertical uncovered edge",
                    bounds.top <= .1f && bounds.bottom >= artwork.getHeight() - .1f);
              });
      if (record) SystemClock.sleep(700);
      touch(MotionEvent.ACTION_UP, cx + distance, cy, down);
      SystemClock.sleep(600);
      getInstrumentation().waitForIdleSync();
      assertEquals(
          "A short depth gesture does not navigate",
          "01 / 03",
          ((TextView) page.findViewById(R.id.onboardStep)).getText().toString());
      assertEquals(0f, page.findViewById(R.id.onboardCopy).getTranslationX(), .01f);
      assertEquals("Artwork settles at its original matrix", before, artwork.getImageMatrix());
      if (record) {
        // Show both directions in the actual native screen recording.
        for (int repeat = 0; repeat < 2; repeat++) {
          down = SystemClock.uptimeMillis();
          touch(MotionEvent.ACTION_DOWN, cx, cy, down);
          for (int frame = 1; frame <= 20; frame++) {
            touch(MotionEvent.ACTION_MOVE, cx - distance * frame / 20f, cy, down);
            SystemClock.sleep(35);
          }
          touch(MotionEvent.ACTION_UP, cx - distance, cy, down);
          SystemClock.sleep(500);
        }
      }
    } finally {
      if (recording != null) {
        try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(recording)) {
          byte[] buffer = new byte[4096];
          while (stream.read(buffer) != -1) {}
        }
      }
    }
  }

  public void testDisablingAnimationsWhileOpenStopsDepth() throws Exception {
    setScale("0");
    SystemClock.sleep(300);
    getInstrumentation().waitForIdleSync();
    Matrix before = new Matrix(artwork.getImageMatrix());
    int[] location = new int[2];
    getInstrumentation().runOnMainSync(() -> artwork.getLocationOnScreen(location));
    float cx = location[0] + artwork.getWidth() * .5f;
    float cy = location[1] + artwork.getHeight() * .5f;
    long down = SystemClock.uptimeMillis();
    touch(MotionEvent.ACTION_DOWN, cx, cy, down);
    touch(MotionEvent.ACTION_MOVE, cx + artwork.getWidth() * .12f, cy, down);
    assertEquals("Reduced motion keeps the illustration still", before, artwork.getImageMatrix());
    assertEquals(0f, page.findViewById(R.id.onboardCopy).getTranslationX(), .01f);
    touch(MotionEvent.ACTION_UP, cx + artwork.getWidth() * .12f, cy, down);
    assertEquals("01 / 03", ((TextView) page.findViewById(R.id.onboardStep)).getText().toString());
  }

  private void touch(int action, float x, float y, long down) {
    getInstrumentation()
        .runOnMainSync(
            () -> {
              MotionEvent event =
                  MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0);
              page.dispatchTouchEvent(event);
              event.recycle();
            });
  }

  private void setScale(String value) throws Exception {
    String command =
        value == null
            ? "settings delete global animator_duration_scale"
            : "settings put global animator_duration_scale " + value;
    try (InputStream stream =
        new ParcelFileDescriptor.AutoCloseInputStream(
            getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      byte[] buffer = new byte[256];
      while (stream.read(buffer) != -1) {}
    }
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (page != null) getInstrumentation().runOnMainSync(page::finish);
      setScale(originalScale);
      getInstrumentation().waitForIdleSync();
    } finally {
      super.tearDown();
    }
  }
}
