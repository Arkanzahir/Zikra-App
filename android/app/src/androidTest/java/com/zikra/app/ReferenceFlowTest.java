package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public final class ReferenceFlowTest extends InstrumentationTestCase {
  private Activity page;

  private <T extends Activity> T launch(Class<T> type) {
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .commit();
    Intent intent =
        new Intent(getInstrumentation().getTargetContext(), type)
            .putExtra("edit", true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    page = getInstrumentation().startActivitySync(intent);
    getInstrumentation().waitForIdleSync();
    UiEvidence.capture(getInstrumentation(), type.getSimpleName());
    return type.cast(page);
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }

  public void testCalendarMonthMovesAndTodayRestoresDate() {
    CalendarActivity calendar = launch(CalendarActivity.class);
    String first = ((TextView) calendar.findViewById(R.id.calendarMonth)).getText().toString();
    getInstrumentation()
        .runOnMainSync(() -> calendar.findViewById(R.id.calendarNext).performClick());
    assertFalse(
        first.equals(((TextView) calendar.findViewById(R.id.calendarMonth)).getText().toString()));
    getInstrumentation()
        .runOnMainSync(() -> calendar.findViewById(R.id.calendarToday).performClick());
    assertEquals(
        first, ((TextView) calendar.findViewById(R.id.calendarMonth)).getText().toString());
  }

  public void testHadithSearchShowsEmptyStateAndCanRecover() {
    HadithActivity hadith = launch(HadithActivity.class);
    getInstrumentation()
        .runOnMainSync(
            () -> ((EditText) hadith.findViewById(R.id.hadithSearch)).setText("nomatch-xyz-123"));
    assertEquals(View.VISIBLE, hadith.findViewById(R.id.hadithEmpty).getVisibility());
    getInstrumentation()
        .runOnMainSync(() -> ((EditText) hadith.findViewById(R.id.hadithSearch)).setText(""));
    assertTrue(((LinearLayout) hadith.findViewById(R.id.hadithList)).getChildCount() > 0);
  }

  public void testOnboardingSavesRoleAndReturnsFromEditMode() {
    OnboardingActivity onboarding = launch(OnboardingActivity.class);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              onboarding.findViewById(R.id.onboardRoleTeacher).performClick();
              onboarding.findViewById(R.id.onboardContinue).performClick();
              onboarding.findViewById(R.id.onboardContinue).performClick();
            });
    getInstrumentation().waitForIdleSync();
    assertEquals(
        "Teacher",
        getInstrumentation()
            .getTargetContext()
            .getSharedPreferences("zikra_preferences", 0)
            .getString("user_role", ""));
    assertTrue(onboarding.isFinishing());
  }
}
