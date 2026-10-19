package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public final class StaticOnboardingTest extends InstrumentationTestCase {
  private Activity page;

  private void launch() {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), OnboardingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long end = SystemClock.uptimeMillis() + 30000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue(page.hasWindowFocus());
  }

  public void testArtworkRemainsStaticAndStoriesAreDistinct() {
    launch();
    ImageView artwork = page.findViewById(R.id.onboardIllustration);
    Bitmap previous = null;
    for (int step = 0; step < 3; step++) {
      getInstrumentation().waitForIdleSync();
      assertNotNull(artwork.getDrawable());
      assertFalse("Illustration is not a replay/drag control", artwork.isClickable());
      Bitmap first = snapshot(artwork);
      getInstrumentation().runOnMainSync(artwork::performClick);
      SystemClock.sleep(400);
      Bitmap later = snapshot(artwork);
      assertTrue("Illustration must remain still without a sensor/timer", first.sameAs(later));
      if (previous != null) {
        assertFalse("Each story has a different illustration", previous.sameAs(first));
        previous.recycle();
      }
      later.recycle();
      previous = first;
      if (step < 2)
        getInstrumentation()
            .runOnMainSync(() -> page.findViewById(R.id.onboardContinue).performClick());
    }
    previous.recycle();
    getInstrumentation().runOnMainSync(() -> page.findViewById(R.id.onboardBack).performClick());
    assertEquals("02 / 03", ((TextView) page.findViewById(R.id.onboardStep)).getText().toString());
  }

  public void testSwipeOnWideContinueAdvancesOnlyOneStep() {
    launch();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              View button = page.findViewById(R.id.onboardContinue);
              int[] xy = new int[2];
              button.getLocationOnScreen(xy);
              float startX = xy[0] + button.getWidth() * .85f,
                  endX = xy[0] + button.getWidth() * .15f;
              float y = xy[1] + button.getHeight() / 2f;
              long time = SystemClock.uptimeMillis();
              MotionEvent down =
                  MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, startX, y, 0);
              MotionEvent move =
                  MotionEvent.obtain(time, time + 40, MotionEvent.ACTION_MOVE, endX, y, 0);
              MotionEvent up =
                  MotionEvent.obtain(time, time + 80, MotionEvent.ACTION_UP, endX, y, 0);
              page.dispatchTouchEvent(down);
              page.dispatchTouchEvent(move);
              page.dispatchTouchEvent(up);
              down.recycle();
              move.recycle();
              up.recycle();
            });
    assertEquals("02 / 03", ((TextView) page.findViewById(R.id.onboardStep)).getText().toString());
  }

  public void testCompactLayoutAtLargeFontKeepsNavigationReachable() {
    launch();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              Configuration config = new Configuration(page.getResources().getConfiguration());
              config.screenWidthDp = 320;
              config.screenHeightDp = 560;
              config.smallestScreenWidthDp = 320;
              config.fontScale = 2.0f;
              android.content.Context scoped = page.createConfigurationContext(config);
              ContextThemeWrapper themed = new ContextThemeWrapper(scoped, R.style.Theme_Zikra);
              float density = themed.getResources().getDisplayMetrics().density;
              View root = LayoutInflater.from(themed).inflate(R.layout.activity_onboarding, null);
              ((ImageView) root.findViewById(R.id.onboardIllustration))
                  .setImageResource(R.drawable.onboarding_study_static);
              int width = Math.round(320 * density), height = Math.round(560 * density);
              root.measure(
                  View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
              root.layout(0, 0, width, height);
              Rect button = bounds(root, root.findViewById(R.id.onboardContinue));
              Rect progress = bounds(root, root.findViewById(R.id.onboardStep));
              Rect content = bounds(root, root.findViewById(R.id.onboardScroll));
              assertTrue(
                  "CTA stays inside the compact viewport",
                  button.bottom <= height && button.top >= 0);
              assertTrue("CTA remains at least 48dp tall", button.height() >= 48 * density);
              assertTrue("Progress and CTA do not overlap", progress.bottom <= button.top);
              assertTrue(
                  "Scrollable story and navigation do not overlap", content.bottom <= progress.top);
              TextView title = root.findViewById(R.id.onboardTitle);
              assertTrue(
                  "The scoped layout uses enlarged text",
                  title.getTextSize() >= 28 * density * 1.98f);
              ImageView illustration = root.findViewById(R.id.onboardIllustration);
              assertEquals("Hero reaches both viewport edges", width, illustration.getWidth());
              assertTrue("Full artwork remains visible", illustration.getHeight() > 0);
              ScrollView scroll = root.findViewById(R.id.onboardScroll);
              scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
              Rect caption = bounds(root, root.findViewById(R.id.onboardDescription));
              assertTrue(
                  "The full caption can be reached without navigation covering it",
                  caption.bottom <= content.bottom);
            });
  }

  private Rect bounds(View root, View child) {
    Rect r = new Rect();
    child.getDrawingRect(r);
    ((android.view.ViewGroup) root).offsetDescendantRectToMyCoords(child, r);
    return r;
  }

  private Bitmap snapshot(View view) {
    Bitmap[] result = new Bitmap[1];
    getInstrumentation()
        .runOnMainSync(
            () -> {
              result[0] =
                  Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
              view.draw(new Canvas(result[0]));
            });
    return result[0];
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
