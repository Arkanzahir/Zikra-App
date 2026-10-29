package com.zikra.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.zikra.app.ui.AppAppearance;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Owned-emulator tests: real Activities, framework dialogs, back stack and draft persistence. */
public final class GlobalAppearanceTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private final List<Activity> opened = new ArrayList<>();

  @Override protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue("Owned emulator fixtures required", new File(context.getFilesDir(),"global-theme-authorized").isFile());
    context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true)
        .putBoolean("shake_enabled",false).commit();
  }

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }

  private void mode(String value) {
    context.getSharedPreferences(AppAppearance.PREFS, 0).edit().putString(AppAppearance.KEY, value).commit();
  }

  private Activity launch(Class<? extends Activity> type) {
    Intent intent = new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    page = getInstrumentation().startActivitySync(intent);
    opened.add(page);
    getInstrumentation().waitForIdleSync();
    return page;
  }

  private void finish() {
    if (page != null && !page.isDestroyed()) main(page::finish);
  }

  @Override protected void tearDown() throws Exception {
    for (Activity activity : opened) if (!activity.isDestroyed()) main(activity::finish);
    super.tearDown();
  }

  private Activity awaitReplacement(ActivityMonitor monitor, Activity old) {
    long end=android.os.SystemClock.uptimeMillis()+10000;
    Activity result=null;
    while(android.os.SystemClock.uptimeMillis()<end) {
      result=monitor.waitForActivityWithTimeout(1000);
      if(result!=null && result!=old && !result.isDestroyed()) return result;
    }
    return null;
  }

  private Activity changed(Activity old, Runnable action) {
    ActivityMonitor monitor = getInstrumentation().addMonitor(old.getClass().getName(), null, false);
    try {
      main(action);
      Activity next = monitor.waitForActivityWithTimeout(10000);
      assertNotNull("Appearance must recreate " + old.getClass().getSimpleName(), next);
      page = next;
      opened.add(next);
      getInstrumentation().waitForIdleSync();
      return next;
    } finally { getInstrumentation().removeMonitor(monitor); }
  }

  private void assertPalette(Activity activity, boolean dark) {
    assertEquals(activity.getClass().getSimpleName(), dark, AppAppearance.dark(activity));
    assertTrue("Main text contrast", ratio(activity.getColor(R.color.text), activity.getColor(R.color.app_surface)) >= 4.5);
    assertTrue("Secondary text contrast", ratio(activity.getColor(R.color.muted), activity.getColor(R.color.background)) >= 4.5);
    assertTrue("AI text contrast", ratio(activity.getColor(R.color.ai_refresh_ink), activity.getColor(R.color.ai_refresh_surface)) >= 4.5);
    TextView title = activity.findViewById(R.id.featureTitle);
    if (title != null && title.getVisibility() == View.VISIBLE) {
      // Branded headers have a deliberate white foreground; only compare surface headers here.
      if (title.getCurrentTextColor() == activity.getColor(R.color.text))
        assertTrue("Rendered header text", ratio(title.getCurrentTextColor(), activity.getColor(R.color.app_surface)) >= 4.5);
    }
    View dock = activity.getWindow().getDecorView().findViewWithTag("zikraDock");
    if (dock != null) {
      View surface = dock.findViewById(R.id.navSurface);
      int fill = ((ColorDrawable) surface.getBackground()).getColor();
      assertEquals("Actual dock surface", dark, Color.valueOf(fill).luminance() < .15f);
      TextView label = dock.findViewById(R.id.navDestinationLabel);
      assertTrue("Actual dock text", ratio(label.getCurrentTextColor(), fill) >= 4.5);
    }
  }

  private double ratio(int a, int b) {
    double x=Color.valueOf(a).luminance(), y=Color.valueOf(b).luminance();
    return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);
  }

  private void buttonContrast(Activity activity) {
    if(activity instanceof OnboardingActivity) {
      android.widget.Button button=activity.findViewById(R.id.onboardContinue);
      assertNull("Custom button must retain its semantic background",button.getBackgroundTintList());
      assertTrue("Branded button text contrast",ratio(button.getCurrentTextColor(),activity.getColor(R.color.pine))>=4.5);
    }
  }

  private void image(String name) throws Exception {
    Thread.sleep(200);
    Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(bitmap);
    File directory=new File(context.getExternalFilesDir(null), "global-theme");
    assertTrue(directory.isDirectory() || directory.mkdirs());
    try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));
    } finally {bitmap.recycle();}
  }

  public void testEveryScreenUsesGlobalLightAndDark() throws Exception {
    Class<?>[] screens={HomeActivity.class, ProfileActivity.class, StudyActivity.class,
      CalendarActivity.class, FaithActivity.class, QuranActivity.class, MushafActivity.class,
      AiActivity.class, DuaActivity.class, DuaLibraryActivity.class, HadithActivity.class,
      HadithReaderActivity.class, PrayerActivity.class, LocationActivity.class, QiblaActivity.class,
      NearbyMosquesActivity.class, ProtectorActivity.class, WeatherActivity.class,
      EarthquakeActivity.class, FocusTimerActivity.class, WeeklyClassesActivity.class,
      OnboardingActivity.class, InfaqActivity.class};
    for(String choice:new String[]{"light","dark"}) {
      mode(choice);
      for(Class<?> type:screens) {
        launch(type.asSubclass(Activity.class));
        assertPalette(page,"dark".equals(choice));buttonContrast(page);
        image(choice+"-"+type.getSimpleName());
        finish();
      }
      Intent hifz = new Intent(context,StudyActivity.class).putExtra("category",com.zikra.app.data.ZikraDbHelper.CATEGORY_HIFZ)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      page=getInstrumentation().startActivitySync(hifz);opened.add(page);
      getInstrumentation().waitForIdleSync();assertPalette(page,"dark".equals(choice));
      image(choice+"-Hifz");finish();
    }
  }

  public void testHomeChoiceBackStackAndAiDraft() throws Exception {
    mode("light");
    Activity home=launch(HomeActivity.class);
    Activity ai=launch(AiActivity.class);
    main(()->((EditText)ai.findViewById(R.id.aiInput)).setText("Draf uji tema, tidak dikirim"));
    Activity profile=launch(ProfileActivity.class);
    Activity current=changed(profile,()->AppAppearance.select(profile,"dark"));
    assertPalette(current,true);
    ActivityMonitor monitor=getInstrumentation().addMonitor(AiActivity.class.getName(),null,false);
    main(current::finish);
    Activity restored=awaitReplacement(monitor,ai);
    getInstrumentation().removeMonitor(monitor);
    assertNotNull("AI back stack recreated",restored);opened.add(restored);page=restored;
    getInstrumentation().waitForIdleSync();assertPalette(restored,true);
    assertEquals("Draf uji tema, tidak dikirim",((EditText)restored.findViewById(R.id.aiInput)).getText().toString());
    ActivityMonitor homeMonitor=getInstrumentation().addMonitor(HomeActivity.class.getName(),null,false);
    main(restored::finish);Activity returned=awaitReplacement(homeMonitor,home);
    getInstrumentation().removeMonitor(homeMonitor);assertNotNull(returned);opened.add(returned);page=returned;
    getInstrumentation().waitForIdleSync();assertPalette(returned,true);
    main(()->returned.findViewById(R.id.homeAppearance).performClick());
    image("dark-appearance-dialog");
    ActivityMonitor chooser=getInstrumentation().addMonitor(HomeActivity.class.getName(),null,false);
    clickText("Terang");
    Activity next=awaitReplacement(chooser,returned);
    getInstrumentation().removeMonitor(chooser);
    assertNotNull("Home choice recreates the screen",next);opened.add(next);page=next;
    getInstrumentation().waitForIdleSync();assertPalette(next,false);
    assertEquals("light",AppAppearance.selected(context));
  }

  // Click the real framework dialog option through Accessibility, without a UI library.
  private void clickText(String text) {
    AccessibilityNodeInfo root=null;
    for(int attempt=0;attempt<20 && root==null;attempt++) {
      root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
      if(root==null) android.os.SystemClock.sleep(100);
    }
    assertNotNull("Active dialog accessibility tree",root);
    List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text);
    assertFalse("Dialog option "+text,nodes.isEmpty());
    Rect bounds=new Rect();nodes.get(0).getBoundsInScreen(bounds);
    assertFalse("Visible dialog option",bounds.isEmpty());
    long time=android.os.SystemClock.uptimeMillis();
    android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,
        android.view.MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
    android.view.MotionEvent up=android.view.MotionEvent.obtain(time,time+60,
        android.view.MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
    try {getInstrumentation().sendPointerSync(down);getInstrumentation().sendPointerSync(up);}
    finally {down.recycle();up.recycle();}
    getInstrumentation().waitForIdleSync();
  }

  public void testFollowDeviceAndLegacyHomePreference() throws Exception {
    mode("system");
    boolean expected=(context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
        == Configuration.UI_MODE_NIGHT_YES;
    for(Class<?> type:new Class<?>[]{HomeActivity.class,AiActivity.class,QuranActivity.class,ProfileActivity.class}) {
      launch(type.asSubclass(Activity.class));assertPalette(page,expected);finish();
    }
    // Existing Home dark preference wins over old reader flags without deleting them.
    context.getSharedPreferences("zikra_preferences",0).edit().putString("reading_theme","light")
        .putBoolean("mushaf_night",false).commit();
    mode("dark");launch(QuranActivity.class);assertPalette(page,true);finish();
    launch(MushafActivity.class);assertPalette(page,true);finish();
    assertEquals("light",context.getSharedPreferences("zikra_preferences",0).getString("reading_theme",""));
    assertFalse(context.getSharedPreferences("zikra_preferences",0).getBoolean("mushaf_night",true));
  }

  public void testDuaIllustrationCoversLargeTextInBothThemes() throws Exception {
    for(String choice:new String[]{"light","dark"}) {
      mode(choice);launch(DuaActivity.class);assertPalette(page,"dark".equals(choice));
      getInstrumentation().waitForIdleSync();
      android.view.ViewGroup hero=page.findViewById(R.id.duaHero);
      assertTrue("Illustration covers the text block at large font",
          page.findViewById(R.id.duaHeroImage).getHeight() >= hero.getChildAt(1).getHeight());
      assertEquals("Hero height follows content rather than bitmap intrinsic size",
          hero.getChildAt(1).getHeight(),hero.getHeight());
      image(choice+"-DuaActivity");finish();
    }
  }

  public void testRestartKeepsAppearanceAndDraft() throws Exception {
    assertEquals("dark",AppAppearance.selected(context));
    launch(AiActivity.class);assertPalette(page,true);
    assertEquals("Draf uji tema, tidak dikirim",((EditText)page.findViewById(R.id.aiInput)).getText().toString());
  }
}
