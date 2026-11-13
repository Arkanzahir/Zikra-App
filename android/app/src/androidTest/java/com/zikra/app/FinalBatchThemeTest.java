package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import com.zikra.app.ui.AppAppearance;
import com.zikra.app.safety.ContextualReminders;

/** Only on a disposable emulator: capture actual screens, without activating reminders. */
@SuppressWarnings("deprecation")
public final class FinalBatchThemeTest extends InstrumentationTestCase {
  public void testFeatureThemesAndOptInDefaults() throws Exception {
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    android.content.Context context = getInstrumentation().getTargetContext();
    android.content.SharedPreferences appearance = context.getSharedPreferences(AppAppearance.PREFS, 0);
    String previous = appearance.getString(AppAppearance.KEY, "light");
    android.content.SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
    boolean onboarded = prefs.getBoolean("onboarding_complete", false);
    prefs.edit().putBoolean("onboarding_complete", true).commit();
    for (String option : ContextualReminders.OPTIONS) assertFalse(ContextualReminders.enabled(context, option));
    Class<?>[] screens = {HomeActivity.class, AiActivity.class, StudyActivity.class,
        ProfileActivity.class, CalendarActivity.class, ContextualActivity.class};
    try {
      for (String mode : new String[]{"light", "dark"}) {
        appearance.edit().putString(AppAppearance.KEY, mode).commit();
        for (Class<?> screen : screens) {
          Intent intent = new Intent(context, screen).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          if (screen == StudyActivity.class) intent.putExtra("category", com.zikra.app.data.ZikraDbHelper.CATEGORY_HIFZ);
          Activity page = getInstrumentation().startActivitySync(intent);
          try {
            getInstrumentation().waitForIdleSync();
            assertEquals(mode.equals("dark"), AppAppearance.dark(page));
            UiEvidence.capture(getInstrumentation(), "final-" + screen.getSimpleName() + "-" + mode);
          } finally { getInstrumentation().runOnMainSync(page::finish); }
        }
      }
      for (String option : ContextualReminders.OPTIONS) assertFalse(ContextualReminders.enabled(context, option));
    } finally {
      appearance.edit().putString(AppAppearance.KEY, previous).commit();
      prefs.edit().putBoolean("onboarding_complete", onboarded).commit();
    }
  }
}
