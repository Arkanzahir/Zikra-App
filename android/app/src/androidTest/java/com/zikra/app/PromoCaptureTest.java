package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.ui.AppAppearance;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Capture only on a fresh dedicated promo emulator; never a physical phone. */
@SuppressWarnings("deprecation")
public final class PromoCaptureTest extends InstrumentationTestCase {
  private Activity page;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    assertTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    assertTrue(
        new File(
                getInstrumentation().getTargetContext().getFilesDir(),
                "promo-dedicated-emulator-authorized")
            .isFile());
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences(AppAppearance.PREFS, 0)
        .edit()
        .putString(AppAppearance.KEY, "light")
        .commit();
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putInt("onboarding_story_version", 6)
        .commit();
  }

  private void capture(Class<? extends Activity> type, String name) throws Exception {
    Intent intent =
        new Intent(getInstrumentation().getTargetContext(), type)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (type == QuranActivity.class) intent.putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 1);
    page = getInstrumentation().startActivitySync(intent);
    if (type == FocusTimerActivity.class)
      getInstrumentation().runOnMainSync(() -> page.findViewById(R.id.focusPlay).performClick());
    SystemClock.sleep(type == ProtectorActivity.class ? 12000 : 4000);
    assertFalse("Capture activity must remain visible", page.isFinishing());
    Files.write(
        new File(page.getFilesDir(), "promo-ready.txt").toPath(),
        name.getBytes(StandardCharsets.UTF_8));
    SystemClock.sleep(14000);
  }

  public void testHome() throws Exception {
    capture(HomeActivity.class, "home");
  }

  public void testQuran() throws Exception {
    capture(QuranActivity.class, "quran");
  }

  public void testFocus() throws Exception {
    capture(FocusTimerActivity.class, "focus");
  }

  public void testProtector() throws Exception {
    capture(ProtectorActivity.class, "protector");
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(() -> page.finish());
    super.tearDown();
  }
}
