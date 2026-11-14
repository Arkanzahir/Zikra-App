package com.zikra.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import com.zikra.app.ui.AppAppearance;
import java.io.File;

/** Explicit host marker required. No seeded tasks, outgoing messages or personal screenshots. */
@SuppressWarnings("deprecation")
public final class PhysicalBetaSmokeTest extends InstrumentationTestCase {
  public void testThemesAndAiDockOnPreservedInstallation() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(), "physical-beta-smoke-authorized").isFile());
    assertEquals("1.9.50", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
    SharedPreferences appearance = context.getSharedPreferences(AppAppearance.PREFS, 0);
    boolean hadTheme = appearance.contains(AppAppearance.KEY);
    String previousTheme = appearance.getString(AppAppearance.KEY, "light");
    SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
    boolean hadShake = prefs.contains("shake_enabled"), previousShake = prefs.getBoolean("shake_enabled", false);
    Activity page = null;
    int screens = 0;
    try {
      prefs.edit().putBoolean("shake_enabled", false).commit();
      for (String mode : new String[]{"light", "dark"}) {
        appearance.edit().putString(AppAppearance.KEY, mode).commit();
        for (Class<?> screen : new Class<?>[]{HomeActivity.class, AiActivity.class, QuranActivity.class,
            StudyActivity.class, ProfileActivity.class, CalendarActivity.class, ProtectorActivity.class}) {
          page = getInstrumentation().startActivitySync(new Intent(context, screen).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          getInstrumentation().waitForIdleSync();
          long end = SystemClock.uptimeMillis() + 10000;
          while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
          assertTrue("Screen must receive focus: " + screen.getSimpleName(), page.hasWindowFocus());
          assertEquals(mode.equals("dark"), AppAppearance.dark(page));
          if (screen == AiActivity.class) {
            for (int id : new int[]{R.id.dockHome, R.id.dockQuran, R.id.dockAi, R.id.dockStudy, R.id.dockProfile}) {
              View button = page.findViewById(id);
              assertNotNull(button); assertTrue(button.isShown());
              Rect bounds = new Rect(); assertTrue(button.getGlobalVisibleRect(bounds));
              assertTrue(bounds.width() > 0 && bounds.height() > 0);
            }
            View dock = page.getWindow().getDecorView().findViewWithTag("zikraDock");
            Rect composer = new Rect(), bar = new Rect();
            assertNotNull(dock); assertTrue(dock.getGlobalVisibleRect(bar));
            assertTrue(page.findViewById(R.id.aiComposer).getGlobalVisibleRect(composer));
            assertTrue("Composer remains above navigation", composer.bottom <= bar.top);
          }
          screens++;
          Activity finished = page; getInstrumentation().runOnMainSync(finished::finish);
          getInstrumentation().waitForIdleSync(); page = null;
        }
      }
      assertEquals(14, screens);
      Bundle result = new Bundle(); result.putInt("physical_screen_theme_checks", screens);
      result.putBoolean("personal_screenshots_taken", false); getInstrumentation().sendStatus(0, result);
    } finally {
      if (page != null) { Activity finished = page; getInstrumentation().runOnMainSync(finished::finish); }
      SharedPreferences.Editor theme = appearance.edit();
      if (hadTheme) theme.putString(AppAppearance.KEY, previousTheme); else theme.remove(AppAppearance.KEY);
      theme.commit();
      SharedPreferences.Editor shake = prefs.edit();
      if (hadShake) shake.putBoolean("shake_enabled", previousShake); else shake.remove("shake_enabled");
      shake.commit();
    }
  }
}
