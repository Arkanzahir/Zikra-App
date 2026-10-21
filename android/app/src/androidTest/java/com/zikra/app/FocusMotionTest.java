package com.zikra.app;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.*;
import android.provider.Settings;
import android.test.InstrumentationTestCase;
import android.widget.TextView;
import com.zikra.app.data.*;
import com.zikra.app.study.*;
import com.zikra.app.ui.FocusGardenView;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Real Android motion/lifecycle checks. Temporary task and timer state are restored afterwards. */
@SuppressWarnings("deprecation")
public final class FocusMotionTest extends InstrumentationTestCase {
  private Context context;
  private FocusTimerActivity page;
  private ZikraDbHelper db;
  private long task;
  private final Map<String, Map<String, ?>> saved = new HashMap<>();
  private String scale;
  private String fontScale;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    for (String name : new String[] {FocusTimer.PREFS, "zikra_preferences"})
      saved.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    fontScale = Settings.System.getString(context.getContentResolver(), Settings.System.FONT_SCALE);
    scale =
        Settings.Global.getString(
            context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE);
    shell("settings put global animator_duration_scale 1");
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .commit();
    db = new ZikraDbHelper(context);
    task = db.addTask("[DATA UJI] Baca satu bab", ZikraDbHelper.CATEGORY_STUDY);
    FocusClock clock = new FocusClock();
    clock.reset(25, 5, task);
    clock.remaining = 17 * 60000 + 40000;
    FocusTimer.save(context, clock);
  }

  private void shell(String command) throws Exception {
    try (InputStream in =
        new ParcelFileDescriptor.AutoCloseInputStream(
            getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      byte[] buffer = new byte[1024];
      while (in.read(buffer) != -1) {}
    }
  }

  private void main(Runnable action) {
    java.util.concurrent.atomic.AtomicReference<Throwable> error =
        new java.util.concurrent.atomic.AtomicReference<>();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                action.run();
              } catch (Throwable failure) {
                error.set(failure);
              }
            });
    getInstrumentation().waitForIdleSync();
    if (error.get() != null) throw new AssertionError(error.get());
  }

  private void launch() {
    page =
        (FocusTimerActivity)
            getInstrumentation()
                .startActivitySync(
                    new Intent(context, FocusTimerActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    await(() -> page.hasWindowFocus());
  }

  private void click(int id) {
    main(() -> page.findViewById(id).performClick());
  }

  private Object field(Object target, String name) {
    try {
      Field f = target.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.get(target);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private FocusGardenView garden() {
    return page.findViewById(R.id.focusGarden);
  }

  private boolean animating() {
    return (Boolean) field(garden(), "framePosted");
  }

  private void await(BooleanSupplier test) {
    long end = SystemClock.uptimeMillis() + 15000;
    while (!test.getAsBoolean() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue(test.getAsBoolean());
  }

  public void testMotionSettingAndPauseNeverChangeTimerAccuracy() throws Exception {
    launch();
    assertFalse(animating());
    click(R.id.focusPlay);
    await(this::animating);
    await(() -> (Float) field(garden(), "motionTime") > .2f);
    shell("settings put global animator_duration_scale 0");
    await(() -> !animating());
    assertTrue(FocusTimer.load(context).running);
    shell("settings put global animator_duration_scale 1");
    await(this::animating);
    click(R.id.focusPlay);
    await(() -> !animating());
    assertFalse(FocusTimer.load(context).running);
    UiEvidence.capture(getInstrumentation(), "FocusCompanionPaused");
  }

  public void testDialogBackgroundAndRecreationStopAndResumeDrawing() throws Exception {
    launch();
    click(R.id.focusPlay);
    await(this::animating);
    main(
        () ->
            ((com.zikra.app.ui.FeatureHeader) page.findViewById(R.id.focusHeader))
                .action()
                .performClick());
    await(() -> !animating());
    main(() -> ((AlertDialog) field(page, "confirmation")).dismiss());
    await(this::animating);
    Activity home =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    await(() -> !animating());
    assertTrue(FocusTimer.load(context).running);
    main(home::finish);
    await(this::animating);
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(FocusTimerActivity.class.getName(), null, false);
    try {
      main(page::recreate);
      page = (FocusTimerActivity) getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull(page);
      await(this::animating);
      assertTrue(FocusTimer.load(context).running);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testBreakPoseOnlyStartsAfterExplicitAction() throws Exception {
    FocusClock clock = FocusTimer.load(context);
    clock.remaining = 0;
    clock.finished = true;
    FocusTimer.save(context, clock);
    launch();
    assertFalse(FocusTimer.load(context).breakPhase);
    assertFalse(animating());
    click(R.id.focusPlay);
    await(() -> (Boolean) field(garden(), "resting"));
    assertTrue(FocusTimer.load(context).breakPhase);
    assertTrue(FocusTimer.load(context).running);
    assertEquals(
        "WAKTU ISTIRAHAT", ((TextView) page.findViewById(R.id.focusPhase)).getText().toString());
    UiEvidence.capture(getInstrumentation(), "FocusCompanionBreak");
    // Record only after the UI is confirmed to be in an active break. A recording
    // started before launch/focus/pause can end before the break ever appears.
    File dir = new File(context.getExternalFilesDir(null), "validation");
    dir.mkdirs();
    File video = new File(dir, "break-active-preview.mp4");
    if (video.exists()) video.delete();
    long before = ((FocusClock) field(page, "clock")).remaining;
    try (ParcelFileDescriptor recording =
        getInstrumentation()
            .getUiAutomation()
            .executeShellCommand("screenrecord --time-limit 8 " + video.getAbsolutePath())) {
      SystemClock.sleep(1100);
      Bitmap first = characterFrame();
      SystemClock.sleep(1000);
      Bitmap second = characterFrame();
      assertFalse(
          "The resting companion really moves during an active break", first.sameAs(second));
      first.recycle();
      second.recycle();
      SystemClock.sleep(7000);
    }
    assertTrue("Active-break video captured", video.isFile() && video.length() > 1000);
    assertTrue(FocusTimer.load(context).breakPhase);
    assertTrue(FocusTimer.load(context).running);
    assertTrue(((FocusClock) field(page, "clock")).remaining < before);
  }

  public void testCaptureNativeMotionPreview() throws Exception {
    launch();
    File dir = new File(context.getExternalFilesDir(null), "validation");
    dir.mkdirs();
    File video = new File(dir, "focus-companion-preview.mp4");
    if (video.exists()) video.delete();
    try (ParcelFileDescriptor recording =
        getInstrumentation()
            .getUiAutomation()
            .executeShellCommand("screenrecord --time-limit 24 " + video.getAbsolutePath())) {
      SystemClock.sleep(900);
      click(R.id.focusPlay);
      SystemClock.sleep(5000);
      UiEvidence.capture(getInstrumentation(), "FocusCompanionRunning");
      click(R.id.focusPlay);
      SystemClock.sleep(2000);
      click(R.id.focusPlay);
      SystemClock.sleep(2500);
      main(
          () -> {
            FocusClock clock = (FocusClock) field(page, "clock");
            clock.running = false;
            clock.remaining = 0;
            clock.finished = true;
            FocusTimer.schedule(context, clock);
          });
      await(
          () ->
              ((TextView) page.findViewById(R.id.focusPlay))
                  .getText()
                  .toString()
                  .equals("Mulai istirahat"));
      click(R.id.focusPlay);
      await(() -> (Boolean) field(garden(), "resting"));
      SystemClock.sleep(8500);
    }
    assertTrue("Native video captured", video.isFile() && video.length() > 1000);
    for (Task existing : db.getTasks()) if (existing.id == task) assertFalse(existing.completed);
  }

  public void testLargeTextKeepsClockSeparateFromCompanion() throws Exception {
    shell("settings put system font_scale 2.0");
    launch();
    assertTrue(page.getResources().getConfiguration().fontScale > 1.5f);
    UiEvidence.capture(getInstrumentation(), "FocusCompanionLargeText");
    final android.graphics.Rect labels = new android.graphics.Rect();
    main(
        () -> {
          android.view.ViewGroup stage = page.findViewById(R.id.focusStage);
          android.view.View total = page.findViewById(R.id.focusTotal);
          total.getDrawingRect(labels);
          stage.offsetDescendantRectToMyCoords(total, labels);
          float density = page.getResources().getDisplayMetrics().density;
          assertTrue(
              "Clock labels keep a dedicated gap above the companion view",
              labels.bottom + 11 * density <= garden().getTop());
          assertTrue("Character has its own space", garden().getHeight() >= 190 * density);
        });
    UiEvidence.capture(getInstrumentation(), "FocusCompanionLargeText");
    main(
        () ->
            page.findViewById(R.id.focusPlay)
                .requestRectangleOnScreen(
                    new android.graphics.Rect(
                        0,
                        0,
                        page.findViewById(R.id.focusPlay).getWidth(),
                        page.findViewById(R.id.focusPlay).getHeight()),
                    true));
    assertTrue(page.findViewById(R.id.focusPlay).getGlobalVisibleRect(new android.graphics.Rect()));
  }

  public void testPauseUsesRestPoseWithoutStartingBreakAndResumeReadsAgain() {
    FocusClock seed = FocusTimer.load(context);
    seed.reset(25, 5, task);
    FocusTimer.save(context, seed);
    launch();
    assertFalse((Boolean) field(garden(), "resting"));
    click(R.id.focusPlay);
    await(this::animating);
    await(() -> (Float) field(garden(), "motionTime") > .25f);
    click(R.id.focusPlay);
    await(() -> (Boolean) field(garden(), "resting"));
    FocusClock paused = FocusTimer.load(context);
    assertFalse(paused.running);
    assertFalse("Pausing the character never starts a break timer", paused.breakPhase);
    assertTrue(paused.remaining > 24 * 60000L);
    assertFalse(animating());
    assertEquals(
        "SEDANG DIJEDA", ((TextView) page.findViewById(R.id.focusPhase)).getText().toString());
    UiEvidence.capture(getInstrumentation(), "FocusPauseRestPose");
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(FocusTimerActivity.class.getName(), null, false);
    try {
      main(page::recreate);
      page = (FocusTimerActivity) getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull(page);
      await(() -> (Boolean) field(garden(), "resting"));
      assertFalse(animating());
      assertFalse(FocusTimer.load(context).breakPhase);
      assertEquals(paused.remaining, FocusTimer.load(context).remaining);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    click(R.id.focusPlay);
    await(() -> !(Boolean) field(garden(), "resting"));
    await(this::animating);
    assertTrue(FocusTimer.load(context).running);
    assertFalse(FocusTimer.load(context).breakPhase);
    assertTrue(FocusTimer.load(context).remaining <= paused.remaining);
  }

  private Bitmap characterFrame() {
    final Bitmap[] result = new Bitmap[1];
    main(
        () -> {
          result[0] =
              Bitmap.createBitmap(
                  garden().getWidth(), garden().getHeight(), Bitmap.Config.ARGB_8888);
          garden().draw(new Canvas(result[0]));
        });
    return result[0];
  }

  public void testCharacterPixelsMoveAndFreezeOnPause() throws Exception {
    launch();
    click(R.id.focusPlay);
    await(this::animating);
    SystemClock.sleep(600);
    Bitmap first = characterFrame();
    SystemClock.sleep(1000);
    Bitmap second = characterFrame();
    int changed = 0;
    for (int y = 0; y < first.getHeight(); y++)
      for (int x = 0; x < first.getWidth(); x++)
        if (first.getPixel(x, y) != second.getPixel(x, y)) changed++;
    assertTrue(
        "Visible character pixels change, not just a scheduled callback: " + changed,
        changed > first.getWidth() * first.getHeight() / 100);
    File dir = new File(context.getExternalFilesDir(null), "validation");
    dir.mkdirs();
    try (FileOutputStream out = new FileOutputStream(new File(dir, "character-motion-a.png"))) {
      first.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    try (FileOutputStream out = new FileOutputStream(new File(dir, "character-motion-b.png"))) {
      second.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    first.recycle();
    second.recycle();
    click(R.id.focusPlay);
    await(() -> !animating());
    long remaining = FocusTimer.load(context).remaining;
    await(() -> (Boolean) field(garden(), "resting"));
    SystemClock.sleep(400);
    Bitmap paused = characterFrame();
    SystemClock.sleep(900);
    Bitmap stillPaused = characterFrame();
    assertTrue("Character really stops drawing movement on pause", paused.sameAs(stillPaused));
    assertEquals(remaining, FocusTimer.load(context).remaining);
    paused.recycle();
    stillPaused.recycle();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (page != null) main(page::finish);
      db.deleteTask(task);
      db.close();
      for (Map.Entry<String, Map<String, ?>> item : saved.entrySet()) {
        SharedPreferences.Editor e = context.getSharedPreferences(item.getKey(), 0).edit().clear();
        for (Map.Entry<String, ?> value : item.getValue().entrySet()) {
          Object v = value.getValue();
          String k = value.getKey();
          if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
          else if (v instanceof String) e.putString(k, (String) v);
          else if (v instanceof Long) e.putLong(k, (Long) v);
          else if (v instanceof Integer) e.putInt(k, (Integer) v);
          else if (v instanceof Float) e.putFloat(k, (Float) v);
          else if (v instanceof Set) e.putStringSet(k, (Set<String>) v);
        }
        e.commit();
      }
      FocusTimer.restore(context);
      shell(
          fontScale == null
              ? "settings delete system font_scale"
              : "settings put system font_scale " + Float.parseFloat(fontScale));
      shell(
          scale == null
              ? "settings delete global animator_duration_scale"
              : "settings put global animator_duration_scale " + Float.parseFloat(scale));
    } finally {
      super.tearDown();
    }
  }
}
