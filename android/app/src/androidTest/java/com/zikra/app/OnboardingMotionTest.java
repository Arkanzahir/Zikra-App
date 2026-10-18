package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.discovery.GardenSceneView;
import java.util.function.BooleanSupplier;

@SuppressWarnings("deprecation")
public final class OnboardingMotionTest extends InstrumentationTestCase {
  private Activity activity;

  public void testCharacterPosesChangeThenRestAndPause() throws Exception {
    activity =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), OnboardingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    await(activity::hasWindowFocus);
    GardenSceneView scene = activity.findViewById(R.id.onboardGarden);
    // Let the entry complete, then explicitly replay to observe its full lifetime.
    SystemClock.sleep(3500);
    getInstrumentation().runOnMainSync(scene::performClick);
    await(scene::isMotionRunning);
    Bitmap first = snapshot(scene);
    SystemClock.sleep(400);
    Bitmap next = snapshot(scene);
    assertFalse("Articulated poses change pixels over time", first.sameAs(next));
    first.recycle();
    next.recycle();
    await(() -> !scene.isMotionRunning());
    Bitmap rest = snapshot(scene);
    SystemClock.sleep(300);
    Bitmap still = snapshot(scene);
    assertTrue("No perpetual animation after the two-second entry", rest.sameAs(still));
    rest.recycle();
    still.recycle();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              scene.performClick();
              scene.setRunning(false);
            });
    assertFalse(scene.isMotionRunning());
    Bitmap stopped = snapshot(scene);
    SystemClock.sleep(300);
    Bitmap paused = snapshot(scene);
    assertTrue("Background/paused view stays still", stopped.sameAs(paused));
    stopped.recycle();
    paused.recycle();
  }

  public void testReducedMotionUsesStillPoster() throws Exception {
    String old =
        android.provider.Settings.Global.getString(
            getInstrumentation().getTargetContext().getContentResolver(),
            "animator_duration_scale");
    try {
      shell("settings put global animator_duration_scale 0");
      activity =
          getInstrumentation()
              .startActivitySync(
                  new Intent(getInstrumentation().getTargetContext(), OnboardingActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      await(activity::hasWindowFocus);
      GardenSceneView scene = activity.findViewById(R.id.onboardGarden);
      SystemClock.sleep(1500);
      getInstrumentation().runOnMainSync(scene::performClick);
      assertFalse(scene.isMotionRunning());
      Bitmap first = snapshot(scene);
      SystemClock.sleep(350);
      Bitmap next = snapshot(scene);
      assertTrue("Reduced motion keeps the final illustration still", first.sameAs(next));
      first.recycle();
      next.recycle();
    } finally {
      shell(
          old == null
              ? "settings delete global animator_duration_scale"
              : "settings put global animator_duration_scale " + old);
    }
  }

  private void shell(String command) throws Exception {
    try (android.os.ParcelFileDescriptor fd =
            getInstrumentation().getUiAutomation().executeShellCommand(command);
        java.io.InputStream in = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
      byte[] b = new byte[1024];
      while (in.read(b) != -1) {}
    }
  }

  private void await(BooleanSupplier ready) {
    long end = SystemClock.uptimeMillis() + 15000;
    while (!ready.getAsBoolean() && SystemClock.uptimeMillis() < end) SystemClock.sleep(50);
    assertTrue("Expected animation lifecycle state", ready.getAsBoolean());
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
