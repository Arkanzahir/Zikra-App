package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zikra.app.data.ZikraDbHelper;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/** Behavior and layout regressions for the shared navigation and daily worship summary. */
@SuppressWarnings("deprecation")
public final class FoundationExperienceTest extends InstrumentationTestCase {
  private Activity page, destination, scoped;
  private SharedPreferences preferences;
  private boolean onboarded;
  private String oldFontScale, oldSize;
  private ZikraDbHelper db;
  private long study = -1, ibadah = -1;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Context context = getInstrumentation().getTargetContext();
    preferences = context.getSharedPreferences("zikra_preferences", 0);
    onboarded = preferences.getBoolean("onboarding_complete", false);
    preferences.edit().putBoolean("onboarding_complete", true).commit();
  }

  private HomeActivity launchHome() {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    getInstrumentation().waitForIdleSync();
    long deadline = SystemClock.uptimeMillis() + 45000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
    assertTrue("Home receives focus", page.hasWindowFocus());
    return (HomeActivity) page;
  }

  public void testHomeSummaryCountsWorshipWithoutAcademicTasks() {
    db = new ZikraDbHelper(getInstrumentation().getTargetContext());
    study = db.addTask("Foundation academic fixture", ZikraDbHelper.CATEGORY_STUDY);
    ibadah = db.addTask("Foundation worship fixture", ZikraDbHelper.CATEGORY_IBADAH);
    db.setCompleted(study, true);
    ZikraDbHelper.Progress expected = db.getProgress(ZikraDbHelper.CATEGORY_IBADAH);
    launchHome();
    assertEquals(
        expected.percent + "%",
        ((TextView) page.findViewById(R.id.homeProgressPercent)).getText().toString());
    assertEquals(
        expected.completed + " dari " + expected.total + " ibadah selesai",
        ((TextView) page.findViewById(R.id.homeProgressSubtitle)).getText().toString());
    assertEquals(
        "Buka checklist ibadah",
        ((TextView) page.findViewById(R.id.homeChecklist)).getText().toString());
  }

  public void testHomeChecklistOpensWorshipInsteadOfAcademics() {
    launchHome();
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(StudyActivity.class.getName(), null, false);
    try {
      getInstrumentation()
          .runOnMainSync(() -> page.findViewById(R.id.homeChecklist).performClick());
      destination = getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull("Checklist opens", destination);
      getInstrumentation().waitForIdleSync();
      assertEquals(
          ZikraDbHelper.CATEGORY_IBADAH, destination.getIntent().getStringExtra("category"));
      assertEquals(
          "Checklist ibadah",
          ((TextView) destination.findViewById(R.id.study_screen_title)).getText().toString());
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testPrimaryStudyDoesNotReopenAScopedHifzScreen() {
    scoped =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), StudyActivity.class)
                    .putExtra("category", ZikraDbHelper.CATEGORY_HIFZ)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), QuranActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(StudyActivity.class.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(() -> page.findViewById(R.id.dockStudy).performClick());
      destination = getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull("Primary Study opens", destination);
      assertNotSame("Existing Hifz page stays separate", scoped, destination);
      assertNull("Primary Study is academic", destination.getIntent().getStringExtra("category"));
      assertEquals(
          "Study Hub",
          ((TextView) destination.findViewById(R.id.study_screen_title)).getText().toString());
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testSupportingTextAndAmberActionMeetNormalTextContrast() {
    launchHome();
    assertTrue(
        "Supporting text on white reaches 4.5:1",
        contrast(page.getColor(R.color.muted), Color.WHITE) >= 4.5);
    assertTrue(
        "Supporting text on page reaches 4.5:1",
        contrast(page.getColor(R.color.muted), page.getColor(R.color.background)) >= 4.5);
    assertTrue(
        "Amber action label reaches 4.5:1",
        contrast(
                ((TextView) page.findViewById(R.id.homeChecklist)).getCurrentTextColor(),
                page.getColor(R.color.amber))
            >= 4.5);
  }

  public void testStudyDockFromQuranAlwaysOpensAcademicsWithScopedStudyInStack() {
    launchHome();
    for (String category :
        new String[] {ZikraDbHelper.CATEGORY_HIFZ, ZikraDbHelper.CATEGORY_IBADAH}) {
      Activity scoped = null, quran = null;
      try {
        scoped =
            getInstrumentation()
                .startActivitySync(
                    new Intent(getInstrumentation().getTargetContext(), StudyActivity.class)
                        .putExtra("category", category)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getInstrumentation().waitForIdleSync();
        quran = navigate(scoped, R.id.dockQuran, QuranActivity.class);
        destination = navigate(quran, R.id.dockStudy, StudyActivity.class);
        assertNotSame("Do not reuse the existing scoped Study page", scoped, destination);
        assertFalse(
            "Academic destination has no scoped category",
            destination.getIntent().hasExtra("category"));
        assertEquals(
            "Study Hub",
            ((TextView) destination.findViewById(R.id.study_screen_title)).getText().toString());
      } finally {
        if (destination != null) getInstrumentation().runOnMainSync(destination::finish);
        destination = null;
        if (quran != null) getInstrumentation().runOnMainSync(quran::finish);
        if (scoped != null) getInstrumentation().runOnMainSync(scoped::finish);
        getInstrumentation().waitForIdleSync();
      }
    }
  }

  private Activity navigate(Activity from, int id, Class<? extends Activity> target) {
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(target.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(() -> from.findViewById(id).performClick());
      Activity opened = getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull("Destination opened: " + target.getSimpleName(), opened);
      getInstrumentation().waitForIdleSync();
      return opened;
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testNarrowHomeAtTwoHundredPercentKeepsActionsAboveDock() throws Exception {
    oldFontScale = shell("settings get system font_scale").trim();
    oldSize = shell("wm size");
    float density =
        getInstrumentation().getTargetContext().getResources().getDisplayMetrics().density;
    shell("wm size " + Math.round(320 * density) + "x" + Math.round(720 * density));
    shell("settings put system font_scale 2.0");
    SystemClock.sleep(1000);
    launchHome();
    assertTrue(
        "Large font setting reached Activity",
        page.getResources().getConfiguration().fontScale >= 1.9f);
    assertTrue(
        "Narrow viewport reached Activity",
        page.getResources().getConfiguration().screenWidthDp <= 325);
    View dock = page.findViewById(R.id.homeRoot).findViewWithTag("zikraDock");
    float dp = page.getResources().getDisplayMetrics().density;
    assertTrue(dock.getHeight() >= 96 * dp - 1);
    assertTrue(page.findViewById(R.id.homeSkySource).getHeight() >= 48 * dp - 1);
    assertTrue(page.findViewById(R.id.homeHijri).getHeight() >= 48 * dp - 1);
    LinearLayout shortcuts = page.findViewById(R.id.homeShortcuts);
    assertEquals("Large text uses four rows of two shortcuts", 4, shortcuts.getChildCount());
    LinearLayout items = page.findViewById(R.id.navItems);
    assertEquals(5, items.getChildCount());
    for (int i = 0; i < items.getChildCount(); i++) {
      View item = items.getChildAt(i);
      assertTrue(item.getWidth() >= 48 * dp - 1);
      assertTrue(item.getHeight() >= 48 * dp - 1);
      TextView label = item.findViewById(R.id.navDestinationLabel);
      assertNotNull(label.getLayout());
      if (i != 2) assertEquals("Keep each destination word intact", 1, label.getLineCount());
      assertEquals(
          "Every destination label remains readable",
          label.length(),
          label.getLayout().getLineEnd(label.getLineCount() - 1));
    }
    assertTrue(page.findViewById(R.id.dockHome).isSelected());
    assertEquals(
        View.VISIBLE,
        page.findViewById(R.id.dockHome)
            .findViewById(R.id.navDestinationIndicator)
            .getVisibility());
    getInstrumentation()
        .runOnMainSync(
            () -> {
              ScrollView scroll = page.findViewById(R.id.homeScroll);
              scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
            });
    getInstrumentation().waitForIdleSync();
    Rect action = new Rect(), dockBounds = new Rect();
    assertTrue(page.findViewById(R.id.homeContinueReading).getGlobalVisibleRect(action));
    assertTrue(dock.getGlobalVisibleRect(dockBounds));
    assertTrue("Last action is not covered by navigation", action.bottom <= dockBounds.top);
    assertTrue(page.findViewById(R.id.homeContinueReading).getHeight() >= 48 * dp - 1);
    UiEvidence.capture(getInstrumentation(), "FoundationHomeLargeText");
  }

  private String shell(String command) throws Exception {
    ParcelFileDescriptor descriptor =
        getInstrumentation().getUiAutomation().executeShellCommand(command);
    try (FileInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int count;
      while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
      return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  private static double contrast(int first, int second) {
    double a = luminance(first), b = luminance(second);
    return (Math.max(a, b) + .05) / (Math.min(a, b) + .05);
  }

  private static double luminance(int color) {
    double result = 0;
    int[] values = {Color.red(color), Color.green(color), Color.blue(color)};
    double[] weights = {.2126, .7152, .0722};
    for (int i = 0; i < values.length; i++) {
      double v = values[i] / 255.0;
      result += (v <= .04045 ? v / 12.92 : Math.pow((v + .055) / 1.055, 2.4)) * weights[i];
    }
    return result;
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (destination != null) getInstrumentation().runOnMainSync(destination::finish);
      if (page != null) getInstrumentation().runOnMainSync(page::finish);
      if (scoped != null) getInstrumentation().runOnMainSync(scoped::finish);
      getInstrumentation().waitForIdleSync();
      if (db != null) {
        if (study >= 0) db.deleteTask(study);
        if (ibadah >= 0) db.deleteTask(ibadah);
        db.close();
      }
      preferences.edit().putBoolean("onboarding_complete", onboarded).commit();
      if (oldFontScale != null) {
        shell(
            "settings put system font_scale "
                + (oldFontScale.equals("null") ? "1.0" : oldFontScale));
        java.util.regex.Matcher override =
            java.util.regex.Pattern.compile("Override size: (\\d+x\\d+)").matcher(oldSize);
        shell("wm size " + (override.find() ? override.group(1) : "reset"));
        SystemClock.sleep(700);
      }
    } finally {
      super.tearDown();
    }
  }
}
