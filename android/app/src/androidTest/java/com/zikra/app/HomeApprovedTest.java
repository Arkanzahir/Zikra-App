package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.test.InstrumentationTestCase;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.location.*;
import com.zikra.app.ui.HomeAppearance;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.List;

/** Only the task-owned, disposable emulator may run this suite. */
@SuppressWarnings("deprecation")
public final class HomeApprovedTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(), "approved-home-authorized").isFile());
    assertTrue(
        android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.HARDWARE.contains("goldfish"));
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .putString("last_surah", "Al-Baqarah")
        .putInt("last_chapter", 2)
        .putInt("last_ayah", 1)
        .commit();
    context.getSharedPreferences(HomeAppearance.PREFS, 0).edit().clear().commit();
    LocationStore.save(
        context,
        new LocationPoint(
            LocationPoint.INITIAL.latitude,
            LocationPoint.INITIAL.longitude,
            "Asia/Jakarta",
            "Sukolilo, Surabaya, Jawa Timur",
            "device",
            Instant.now().toEpochMilli(),
            100));
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) main(page::finish);
    super.tearDown();
  }

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }

  private void launch(Class<? extends Activity> type) {
    if (page != null) main(page::finish);
    page =
        getInstrumentation()
            .startActivitySync(new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }

  private void image(String name) throws Exception {
    Thread.sleep(650);
    Bitmap bitmap = getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(bitmap);
    File folder = new File(context.getExternalFilesDir(null), "approved-home");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    bitmap.recycle();
  }

  private void contrast() {
    int foreground = ((TextView) page.findViewById(R.id.homeLastRead)).getCurrentTextColor();
    int fill = page.getColor(R.color.refined_surface);
    assertTrue("Reading text contrast", ratio(foreground, fill) >= 4.5);
    LinearLayout reading = page.findViewById(R.id.homeContinueReading);
    ImageView icon = (ImageView) reading.getChildAt(0);
    assertNotNull(icon.getImageTintList());
    assertTrue(
        "Reading icon contrast",
        ratio(icon.getImageTintList().getDefaultColor(), page.getColor(R.color.refined_soft))
            >= 4.5);
    assertTrue(
        "Secondary text contrast",
        ratio(page.getColor(R.color.refined_muted), page.getColor(R.color.refined_background))
            >= 4.5);
  }

  private double ratio(int a, int b) {
    double x = Color.valueOf(a).luminance(), y = Color.valueOf(b).luminance();
    return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
  }

  public void testHomeDefaultLightAndRealThemeChoicePersist() throws Exception {
    launch(HomeActivity.class);
    assertFalse(com.zikra.app.ui.RefinedUi.dark(page));
    assertTrue(
        ((TextView) page.findViewById(R.id.homeLocationLabel))
            .getText()
            .toString()
            .contains("Surabaya"));
    contrast();
    image("home-light");
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(HomeActivity.class.getName(), null, false);
    main(() -> page.findViewById(R.id.homeAppearance).performClick());
    Thread.sleep(250);
    List<AccessibilityNodeInfo> choices =
        getInstrumentation()
            .getUiAutomation()
            .getRootInActiveWindow()
            .findAccessibilityNodeInfosByText("Gelap");
    assertFalse(choices.isEmpty());
    AccessibilityNodeInfo choice = choices.get(0);
    while (choice != null && !choice.isClickable()) choice = choice.getParent();
    assertNotNull(choice);
    assertTrue(choice.performAction(AccessibilityNodeInfo.ACTION_CLICK));
    Activity recreated = getInstrumentation().waitForMonitorWithTimeout(monitor, 7000);
    getInstrumentation().removeMonitor(monitor);
    assertNotNull("Theme selection recreates Home", recreated);
    page = recreated;
    getInstrumentation().waitForIdleSync();
    assertEquals("dark", HomeAppearance.selected(context));
    assertTrue(com.zikra.app.ui.RefinedUi.dark(page));
    contrast();
    image("home-dark");
    launch(HomeActivity.class);
    assertTrue(com.zikra.app.ui.RefinedUi.dark(page));
    main(() -> page.findViewById(R.id.homeScheduleSource).performClick());
    Thread.sleep(200);
    assertFalse(
        getInstrumentation()
            .getUiAutomation()
            .getRootInActiveWindow()
            .findAccessibilityNodeInfosByText("Sumber waktu salat")
            .isEmpty());
    getInstrumentation()
        .getUiAutomation()
        .performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
  }

  public void testLocationShowsNameAndPreservesCoordinates() throws Exception {
    LocationPoint before = LocationStore.current(context);
    launch(LocationActivity.class);
    assertEquals(
        before.label, ((TextView) page.findViewById(R.id.locationCurrent)).getText().toString());
    assertTrue(
        ((TextView) page.findViewById(R.id.locationAutomatic))
            .getText()
            .toString()
            .contains("Perbarui"));
    image("location");
    LocationPoint after = LocationStore.current(context);
    assertEquals(before.key(), after.key());
    assertEquals(before.label, after.label);
  }

  public void testLauncherAndNotificationKeepCompleteBrand() throws Exception {
    Drawable icon = context.getApplicationInfo().loadIcon(context.getPackageManager());
    assertTrue(icon instanceof AdaptiveIconDrawable);
    Drawable foreground = ((AdaptiveIconDrawable) icon).getForeground();
    Bitmap b = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888);
    foreground.setBounds(0, 0, 108, 108);
    foreground.draw(new Canvas(b));
    int min = 108, max = -1;
    for (int y = 0; y < 108; y++)
      for (int x = 0; x < 108; x++)
        if (Color.alpha(b.getPixel(x, y)) > 128) {
          min = Math.min(min, y);
          max = Math.max(max, y);
          assertTrue("Logo stays in circle safe area", Math.hypot(x - 54, y - 54) < 33);
        }
    assertTrue(max > min);
    b.recycle();
    Drawable notification = context.getDrawable(R.drawable.ic_notification_prayer);
    assertNotNull(notification);
    Bitmap n = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
    notification.setBounds(0, 0, 48, 48);
    notification.draw(new Canvas(n));
    int lower = 0;
    for (int y = 33; y < 47; y++)
      for (int x = 5; x < 43; x++) if (Color.alpha(n.getPixel(x, y)) > 128) lower++;
    assertTrue("Book remains in notification logo", lower > 10);
    n.recycle();
  }

  public void testHomeLargeTextKeepsControlsReachable() throws Exception {
    launch(HomeActivity.class);
    assertTrue(page.getResources().getConfiguration().fontScale >= 1.9f);
    assertTrue(page.findViewById(R.id.homeAppearance).isShown());
    assertTrue(page.findViewById(R.id.homeLocation).isShown());
    TextView label = page.findViewById(R.id.homeLocationLabel);
    assertTrue(label.getText().toString().contains("Surabaya"));
    for (int i = 0; i < label.getLayout().getLineCount(); i++)
      assertEquals("Full location remains readable", 0, label.getLayout().getEllipsisCount(i));
    assertEquals(4, ((LinearLayout) page.findViewById(R.id.homeShortcuts)).getChildCount());
    image("home-large-text");
  }

  public void testSelectedAudioTracksHaveReadableAndroidMetadata() throws Exception {
    for (int index : new int[] {0, 2}) {
      try (android.content.res.AssetFileDescriptor file =
          com.zikra.app.reminders.AdhanAsset.open(context, index)) {
        android.media.MediaMetadataRetriever metadata = new android.media.MediaMetadataRetriever();
        try {
          metadata.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
          assertTrue(
              Long.parseLong(
                      metadata.extractMetadata(
                          android.media.MediaMetadataRetriever.METADATA_KEY_DURATION))
                  > 10000);
          assertTrue(
              Long.parseLong(
                      metadata.extractMetadata(
                          android.media.MediaMetadataRetriever.METADATA_KEY_DURATION))
                  < 600000);
        } finally {
          metadata.release();
        }
      }
    }
  }
}
