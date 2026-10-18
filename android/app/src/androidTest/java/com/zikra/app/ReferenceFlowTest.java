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

  public void testOnboardingStoryDoesNotRequestOrOverwriteInterests() {
    android.content.SharedPreferences prefs =
        getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    java.util.Map<String, ?> original = new java.util.HashMap<>(prefs.getAll());
    try {
      prefs
          .edit()
          .putString("education_level", "SMA / SMK")
          .putStringSet(
              "learning_interests", new java.util.HashSet<>(java.util.Arrays.asList("Sains")))
          .commit();
      OnboardingActivity onboarding = launch(OnboardingActivity.class);
      assertTrue(onboarding.findViewById(R.id.onboardContinue).isEnabled());
      assertEquals(
          "Langkah 1 dari 3",
          ((TextView) onboarding.findViewById(R.id.onboardStep)).getText().toString());
      getInstrumentation()
          .runOnMainSync(() -> onboarding.findViewById(R.id.onboardContinue).performClick());
      assertEquals(
          "Langkah 2 dari 3",
          ((TextView) onboarding.findViewById(R.id.onboardStep)).getText().toString());
      getInstrumentation()
          .runOnMainSync(() -> onboarding.findViewById(R.id.onboardSkip).performClick());
      getInstrumentation().waitForIdleSync();
      assertTrue(prefs.getBoolean("onboarding_complete", false));
      assertEquals("SMA / SMK", prefs.getString("education_level", ""));
      assertTrue(
          prefs
              .getStringSet("learning_interests", java.util.Collections.emptySet())
              .contains("Sains"));
    } finally {
      android.content.SharedPreferences.Editor e = prefs.edit().clear();
      for (java.util.Map.Entry<String, ?> item : original.entrySet()) {
        Object v = item.getValue();
        String k = item.getKey();
        if (v instanceof String) e.putString(k, (String) v);
        else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
        else if (v instanceof Integer) e.putInt(k, (Integer) v);
        else if (v instanceof Long) e.putLong(k, (Long) v);
        else if (v instanceof Float) e.putFloat(k, (Float) v);
        else if (v instanceof java.util.Set)
          e.putStringSet(k, new java.util.HashSet<>((java.util.Set<String>) v));
      }
      e.commit();
    }
  }
}
