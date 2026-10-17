package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.discovery.LearningOrbitView;

@SuppressWarnings("deprecation")
public final class OnboardingMotionTest extends InstrumentationTestCase {
  private Activity activity;

  public void testOrbitMovesAndStopsWhenLeavingWelcome() throws Exception {
    activity =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), OnboardingActivity.class)
                    .putExtra("edit", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    getInstrumentation()
        .runOnMainSync(() -> activity.findViewById(R.id.onboardBack).performClick());
    SystemClock.sleep(350);
    LearningOrbitView orbit = activity.findViewById(R.id.onboardOrbit);
    assertTrue("Motion-enabled welcome must animate", orbit.isMotionRunning());
    Bitmap before = snapshot(orbit);
    SystemClock.sleep(350);
    Bitmap after = snapshot(orbit);
    assertFalse("The orbit must change over time", before.sameAs(after));
    before.recycle();
    after.recycle();
    getInstrumentation()
        .runOnMainSync(() -> activity.findViewById(R.id.onboardContinue).performClick());
    assertFalse("Hidden artwork must stop frame and sensor work", orbit.isMotionRunning());
  }

  private Bitmap snapshot(LearningOrbitView view) {
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
    if (activity != null) getInstrumentation().runOnMainSync(activity::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
