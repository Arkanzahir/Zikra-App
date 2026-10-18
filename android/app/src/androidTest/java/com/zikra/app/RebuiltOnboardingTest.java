package com.zikra.app;

import android.app.*;
import android.content.*;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.widget.TextView;
import java.io.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class RebuiltOnboardingTest extends InstrumentationTestCase {
  private Activity page, home;
  private SharedPreferences prefs;
  private Map<String, ?> original;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    prefs = getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    original = new HashMap<>(prefs.getAll());
  }

  private void launch() {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), OnboardingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    waitFocus(page);
  }

  private void waitFocus(Activity activity) {
    long end = SystemClock.uptimeMillis() + 30000;
    while (!activity.hasWindowFocus() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue("Activity gets focus", activity.hasWindowFocus());
  }

  private void capture(String name) {
    getInstrumentation().waitForIdleSync();
    SystemClock.sleep(3500);
    UiEvidence.capture(getInstrumentation(), name);
  }

  public void testThreeStoriesRecreateAndPreserveLegacyPreferences() {
    prefs
        .edit()
        .putString("education_level", "legacy-level")
        .putStringSet("learning_interests", new HashSet<>(Arrays.asList("legacy-interest")))
        .commit();
    launch();
    assertNotNull(page.getWindow().getDecorView().findViewWithTag("zikraBrand"));
    assertEquals(
        "Langkah 1 dari 3", ((TextView) page.findViewById(R.id.onboardStep)).getText().toString());
    capture("RebuiltOnboarding1");
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.onboardContinue).performClick());
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(OnboardingActivity.class.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
      waitFocus(page);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    assertEquals(
        "Langkah 2 dari 3", ((TextView) page.findViewById(R.id.onboardStep)).getText().toString());
    capture("RebuiltOnboarding2");
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.onboardContinue).performClick());
    capture("RebuiltOnboarding3");
    monitor = getInstrumentation().addMonitor(HomeActivity.class.getName(), null, false);
    try {
      getInstrumentation()
          .runOnMainSync(() -> page.findViewById(R.id.onboardContinue).performClick());
      home = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(home);
      waitFocus(home);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    assertTrue(prefs.getBoolean("onboarding_complete", false));
    assertEquals(3, prefs.getInt("onboarding_story_version", 0));
    assertEquals("legacy-level", prefs.getString("education_level", ""));
    assertTrue(
        prefs
            .getStringSet("learning_interests", Collections.emptySet())
            .contains("legacy-interest"));
  }

  public void testSkipHomeBrandAndActualAppIcon() throws Exception {
    launch();
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(HomeActivity.class.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(() -> page.findViewById(R.id.onboardSkip).performClick());
      home = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(home);
      waitFocus(home);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    assertNotNull(home.getWindow().getDecorView().findViewWithTag("zikraBrand"));
    capture("RebuiltHomeBrand");
    if ("true"
        .equals(
            ((android.test.InstrumentationTestRunner) getInstrumentation())
                .getArguments()
                .getString("record_motion"))) {
      try (android.os.ParcelFileDescriptor descriptor =
              getInstrumentation()
                  .getUiAutomation()
                  .executeShellCommand(
                      "screenrecord --bit-rate 1200000 --time-limit 8"
                          + " /sdcard/RebuiltHomeMotion.mp4");
          InputStream in = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
        byte[] buffer = new byte[4096];
        while (in.read(buffer) != -1) {}
      }
    }
    Bitmap icon = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              Drawable drawable = home.getApplicationInfo().loadIcon(home.getPackageManager());
              drawable.setBounds(0, 0, 256, 256);
              drawable.draw(new Canvas(icon));
            });
    File out = new File(home.getExternalFilesDir(null), "validation/RebuiltAppIcon.png");
    try (FileOutputStream stream = new FileOutputStream(out)) {
      assertTrue(icon.compress(Bitmap.CompressFormat.PNG, 100, stream));
    }
    icon.recycle();
  }

  @Override
  protected void tearDown() throws Exception {
    if (home != null) getInstrumentation().runOnMainSync(home::finish);
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    SharedPreferences.Editor e = prefs.edit().clear();
    for (Map.Entry<String, ?> entry : original.entrySet()) {
      Object v = entry.getValue();
      String k = entry.getKey();
      if (v instanceof String) e.putString(k, (String) v);
      else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
      else if (v instanceof Integer) e.putInt(k, (Integer) v);
      else if (v instanceof Long) e.putLong(k, (Long) v);
      else if (v instanceof Float) e.putFloat(k, (Float) v);
      else if (v instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) v));
    }
    e.commit();
    super.tearDown();
  }
}
