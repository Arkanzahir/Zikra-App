package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public final class NativePolishTest extends InstrumentationTestCase {
  private Activity page;

  private <T extends Activity> T launch(Class<T> type) {
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .commit();
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    return type.cast(page);
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }

  public void testComposerStaysAboveDockAndKeyboardHidesNavigation() {
    AiActivity ai = launch(AiActivity.class);
    Rect composer = new Rect(), dock = new Rect();
    assertTrue(ai.findViewById(R.id.aiComposer).getGlobalVisibleRect(composer));
    assertTrue(ai.findViewById(R.id.navSurface).getGlobalVisibleRect(dock));
    assertTrue("Composer must remain above the dock", composer.bottom <= dock.top);
    UiEvidence.capture(getInstrumentation(), "AiActivity");
    EditText input = ai.findViewById(R.id.aiInput);
    InputMethodManager keyboard =
        (InputMethodManager) ai.getSystemService(Activity.INPUT_METHOD_SERVICE);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              input.setText("Rencana belajar Java");
              ai.findViewById(R.id.aiChatTile).performClick();
            });
    View navigation = ai.findViewById(R.id.aiRoot).findViewWithTag("zikraDock");
    long deadline = SystemClock.uptimeMillis() + 15000;
    while (navigation.getVisibility() != View.GONE && SystemClock.uptimeMillis() < deadline)
      SystemClock.sleep(200);
    UiEvidence.capture(getInstrumentation(), "AiKeyboard");
    assertEquals(
        "Keyboard must not be covered by floating navigation",
        View.GONE,
        navigation.getVisibility());
    UiEvidence.capture(getInstrumentation(), "AiKeyboard");
    getInstrumentation()
        .runOnMainSync(() -> keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0));
  }

  public void testPrayerSelectionAndWeekViewSurviveRecreation() {
    PrayerActivity prayer = launch(PrayerActivity.class);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              ((android.widget.LinearLayout) prayer.findViewById(R.id.prayerWeekDates))
                  .getChildAt(0)
                  .performClick();
              prayer.findViewById(R.id.prayerWeekTab).performClick();
            });
    String caption = ((TextView) prayer.findViewById(R.id.prayerDateCaption)).getText().toString();
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(PrayerActivity.class.getName(), null, false);
    try {
      getInstrumentation().runOnMainSync(prayer::recreate);
      Activity restored = getInstrumentation().waitForMonitorWithTimeout(monitor, 60000);
      assertNotNull("Recreated prayer Activity must reopen", restored);
      page = restored;
      getInstrumentation().waitForIdleSync();
      assertEquals(View.VISIBLE, restored.findViewById(R.id.prayerWeekSummary).getVisibility());
      assertEquals(View.GONE, restored.findViewById(R.id.prayerRows).getVisibility());
      assertEquals(
          caption, ((TextView) restored.findViewById(R.id.prayerDateCaption)).getText().toString());
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testProfileHeaderReflectsSelectedLearningRole() {
    SharedPreferences prefs =
        getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    String previous = prefs.getString("user_role", "Student");
    try {
      prefs.edit().putString("user_role", "Teacher").commit();
      ProfileActivity profile = launch(ProfileActivity.class);
      assertTrue(
          ((TextView) profile.findViewById(R.id.profileRoleCaption))
              .getText()
              .toString()
              .startsWith("Pengajar"));
      UiEvidence.capture(getInstrumentation(), "ProfileActivity");
    } finally {
      prefs.edit().putString("user_role", previous).commit();
    }
  }
}
