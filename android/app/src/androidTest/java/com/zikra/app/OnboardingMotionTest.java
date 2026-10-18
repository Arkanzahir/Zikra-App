package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.discovery.GardenSceneView;

@SuppressWarnings("deprecation")
public final class OnboardingMotionTest extends InstrumentationTestCase {
  private Activity activity;

  public void testGardenMovesAndPausesWithoutChangingBrand() throws Exception {
    activity =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), OnboardingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long end = SystemClock.uptimeMillis() + 15000;
    while (!activity.hasWindowFocus() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    GardenSceneView scene = activity.findViewById(R.id.onboardGarden);
    assertTrue(
        "Foreground garden must animate when system motion is enabled", scene.isMotionRunning());
    Bitmap first = snapshot(scene);
    SystemClock.sleep(500);
    Bitmap next = snapshot(scene);
    assertFalse("Floating scenery changes pixels over time", first.sameAs(next));
    first.recycle();
    next.recycle();
    getInstrumentation().runOnMainSync(() -> scene.setRunning(false));
    assertFalse(scene.isMotionRunning());
    Bitmap stopped = snapshot(scene);
    SystemClock.sleep(300);
    Bitmap still = snapshot(scene);
    assertTrue("Paused garden does not advance decorative motion", stopped.sameAs(still));
    stopped.recycle();
    still.recycle();
  }

  private Bitmap snapshot(GardenSceneView scene) {
    Bitmap[] image = new Bitmap[1];
    getInstrumentation()
        .runOnMainSync(
            () -> {
              image[0] =
                  Bitmap.createBitmap(scene.getWidth(), scene.getHeight(), Bitmap.Config.ARGB_8888);
              scene.draw(new Canvas(image[0]));
            });
    return image[0];
  }

  @Override
  protected void tearDown() throws Exception {
    if (activity != null) getInstrumentation().runOnMainSync(activity::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
