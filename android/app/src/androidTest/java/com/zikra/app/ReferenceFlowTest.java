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

  public void testOnboardingSavesLevelAndLimitsInterestsInEditMode() {
    android.content.SharedPreferences prefs =
        getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    String oldLevel = prefs.getString("education_level", null);
    String oldRole = prefs.getString("user_role", null);
    java.util.Set<String> oldInterests = prefs.getStringSet("learning_interests", null);
    try {
      prefs.edit().remove("education_level").remove("learning_interests").commit();
      OnboardingActivity onboarding = launch(OnboardingActivity.class);
      assertFalse(onboarding.findViewById(R.id.onboardContinue).isEnabled());
      getInstrumentation()
          .runOnMainSync(() -> onboarding.findViewById(R.id.onboardBack).performClick());
      UiEvidence.capture(getInstrumentation(), "OnboardingWelcome");
      getInstrumentation()
          .runOnMainSync(
              () -> {
                onboarding.findViewById(R.id.onboardContinue).performClick();
                onboarding.findViewById(R.id.onboardLevelSma).performClick();
              });
      UiEvidence.capture(getInstrumentation(), "OnboardingEducation");
      assertTrue(onboarding.findViewById(R.id.onboardLevelSma).isSelected());
      getInstrumentation()
          .runOnMainSync(() -> onboarding.findViewById(R.id.onboardContinue).performClick());
      android.widget.GridLayout grid = onboarding.findViewById(R.id.onboardInterests);
      getInstrumentation()
          .runOnMainSync(
              () -> {
                for (int i = 0; i < 4; i++) grid.getChildAt(i).performClick();
              });
      assertFalse("A fourth interest must not be selected", grid.getChildAt(3).isSelected());
      assertEquals(
          "3 dari 3 minat dipilih",
          ((TextView) onboarding.findViewById(R.id.onboardInterestCount)).getText().toString());
      getInstrumentation()
          .runOnMainSync(
              () -> {
                grid.getChildAt(0).performClick();
                grid.getChildAt(3).performClick();
                onboarding.findViewById(R.id.onboardBack).performClick();
              });
      assertTrue(
          "Education survives moving back",
          onboarding.findViewById(R.id.onboardLevelSma).isSelected());
      getInstrumentation()
          .runOnMainSync(() -> onboarding.findViewById(R.id.onboardContinue).performClick());
      UiEvidence.capture(getInstrumentation(), "OnboardingInterests");
      getInstrumentation()
          .runOnMainSync(() -> onboarding.findViewById(R.id.onboardContinue).performClick());
      getInstrumentation().waitForIdleSync();
      assertEquals("SMA / SMK", prefs.getString("education_level", ""));
      assertEquals("Student", prefs.getString("user_role", ""));
      java.util.Set<String> saved =
          prefs.getStringSet("learning_interests", java.util.Collections.emptySet());
      assertEquals(3, saved.size());
      assertFalse(saved.contains(OnboardingActivity.INTERESTS[0]));
      assertTrue(saved.contains(OnboardingActivity.INTERESTS[3]));
      assertTrue(onboarding.isFinishing());
    } finally {
      prefs
          .edit()
          .putString("education_level", oldLevel)
          .putString("user_role", oldRole)
          .putStringSet("learning_interests", oldInterests)
          .commit();
    }
  }
}
