package com.zikra.app;

import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.os.*;
import android.provider.Settings;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import com.zikra.app.audio.*;
import com.zikra.app.reading.QuranPage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Native catalog/reader/sheet checks. No network audio is seeded or claimed as Quran playback. */
@SuppressWarnings("deprecation")
public final class QuranDesignTest extends InstrumentationTestCase {
  private Context context;
  private QuranActivity page;
  private final Map<String, Map<String, ?>> saved = new HashMap<>();
  private String font;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    for (String name : new String[] {"zikra_preferences", "quran_audio_options"})
      saved.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    font = Settings.System.getString(context.getContentResolver(), Settings.System.FONT_SCALE);
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .putString("reading_theme", "light")
        .putInt("last_chapter", 1)
        .putString("last_surah", "Al-Fatihah")
        .putInt("last_ayah", 1)
        .commit();
  }

  private void main(Runnable action) {
    java.util.concurrent.atomic.AtomicReference<Throwable> failure =
        new java.util.concurrent.atomic.AtomicReference<>();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                action.run();
              } catch (Throwable error) {
                failure.set(error);
              }
            });
    getInstrumentation().waitForIdleSync();
    if (failure.get() != null) throw new AssertionError(failure.get());
  }

  private void until(BooleanSupplier value) {
    long end = SystemClock.uptimeMillis() + 20000;
    while (SystemClock.uptimeMillis() < end) {
      if (value.getAsBoolean()) return;
      SystemClock.sleep(100);
    }
    fail("Condition was not satisfied before timeout");
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

  private void launch(boolean reader) {
    Intent intent =
        new Intent(context, QuranActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (reader) intent.putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 1);
    page = (QuranActivity) getInstrumentation().startActivitySync(intent);
    until(() -> page.hasWindowFocus());
    if (reader) ready();
  }

  private void ready() {
    until(() -> ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount() == 7);
  }

  private void click(int id) {
    main(() -> page.findViewById(id).performClick());
  }

  private AlertDialog options() {
    return (AlertDialog) field(field(page, "audioUi"), "options");
  }

  private void shell(String command) throws Exception {
    try (InputStream in =
        new ParcelFileDescriptor.AutoCloseInputStream(
            getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      byte[] b = new byte[1024];
      while (in.read(b) != -1) {}
    }
  }

  public void testComfortableReaderLayoutAndVisibleActions() throws Exception {
    page =
        (QuranActivity)
            getInstrumentation()
                .startActivitySync(
                    new Intent(context, QuranActivity.class)
                        .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 112)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    until(
        () ->
            page.hasWindowFocus()
                && ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount() == 4);
    getInstrumentation().waitForIdleSync();
    assertEquals(View.GONE, page.findViewById(R.id.readerAudioBody).getVisibility());
    UiEvidence.capture(getInstrumentation(), "ReaderOrnamentFolded");
    click(R.id.quranAudioExpand);
    View row = ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildAt(0);
    Rect number = new Rect(), play = new Rect(), arabic = new Rect(), translation = new Rect();
    row.findViewById(R.id.ayahNumber).getGlobalVisibleRect(number);
    row.findViewById(R.id.ayahPlay).getGlobalVisibleRect(play);
    row.findViewById(R.id.ayahArabic).getGlobalVisibleRect(arabic);
    row.findViewById(R.id.ayahTranslation).getGlobalVisibleRect(translation);
    assertTrue(
        "Number then audio then Arabic from left",
        number.right <= play.left && play.right <= arabic.left);
    assertTrue("Translation below Arabic line", translation.top >= arabic.bottom);
    Rect player = new Rect(), dock = new Rect(), movement = new Rect();
    page.findViewById(R.id.quranAudioPanel).getGlobalVisibleRect(player);
    page.findViewById(R.id.dockQuran).getGlobalVisibleRect(dock);
    page.findViewById(R.id.quranReaderMovement).getGlobalVisibleRect(movement);
    assertTrue("Reader dock stays visible", page.findViewById(R.id.dockQuran).isShown());
    assertTrue("Movement strip above player", movement.bottom <= player.top);
    Rect controls = new Rect();
    page.findViewById(R.id.readerAudioControls).getGlobalVisibleRect(controls);
    assertTrue("Player controls clear navigation", controls.bottom <= dock.top);
    assertTrue(
        "Expanded player remains compact",
        player.height() <= 176 * page.getResources().getDisplayMetrics().density);
    assertEquals(View.GONE, page.findViewById(R.id.quranLoadStatus).getVisibility());
    UiEvidence.capture(getInstrumentation(), "ComfortableReaderIkhlas");
    click(R.id.quranReaderFont);
    assertTrue(((AlertDialog) field(page, "readingDialog")).isShowing());
    main(() -> ((AlertDialog) field(page, "readingDialog")).dismiss());
    main(() -> row.findViewById(R.id.ayahNumber).performClick());
    until(
        () ->
            getInstrumentation()
                    .getUiAutomation()
                    .getRootInActiveWindow()
                    .findAccessibilityNodeInfosByText("Tandai terakhir dibaca")
                    .size()
                > 0);
    UiEvidence.capture(getInstrumentation(), "ComfortableVerseActions");
    shell("input keyevent 4");
  }

  public void testComfortableLargeReaderKeepsPlayerAndNavigationReachable() throws Exception {
    shell("settings put system font_scale 2.0");
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putString("reading_theme", "dark")
        .commit();
    launch(true);
    click(R.id.quranAudioExpand);
    Rect play = new Rect(), dock = new Rect();
    assertTrue(page.findViewById(R.id.quranAudioPlay).getGlobalVisibleRect(play));
    assertTrue(page.findViewById(R.id.dockQuran).getGlobalVisibleRect(dock));
    assertTrue(
        "Full play target above dock",
        play.bottom <= dock.top
            && play.height() >= 48 * page.getResources().getDisplayMetrics().density - 1);
    assertTrue(
        "Reader retains scroll viewport",
        page.findViewById(R.id.quranScroll).getHeight()
            >= 100 * page.getResources().getDisplayMetrics().density);
    for (int id : new int[] {R.id.quranReaderFont, R.id.quranReaderMushaf, R.id.quranReaderAudio}) {
      assertEquals(
          "Night tool text remains readable",
          0xFFEEF6F2,
          ((TextView) page.findViewById(id)).getCurrentTextColor());
      android.graphics.drawable.GradientDrawable background =
          (android.graphics.drawable.GradientDrawable) page.findViewById(id).getBackground();
      assertEquals(
          "Night tool uses dark surface", 0xFF233D33, background.getColor().getDefaultColor());
    }
    UiEvidence.capture(getInstrumentation(), "ComfortableReaderLargeNight");
  }

  public void testCatalogReaderAndNativePanelPreview() throws Exception {
    launch(false);
    assertEquals(114, ((LinearLayout) page.findViewById(R.id.quranSurahList)).getChildCount());
    UiEvidence.capture(getInstrumentation(), "QuranDesignCatalog");
    main(
        () -> ((LinearLayout) page.findViewById(R.id.quranSurahList)).getChildAt(0).performClick());
    ready();
    LinearLayout verses = page.findViewById(R.id.ayahContainer);
    for (int i = 0; i < 7; i++)
      assertEquals(
          QuranPage.builtIn(1).verses.get(i).arabic,
          ((TextView) verses.getChildAt(i).findViewById(R.id.ayahArabic)).getText().toString());
    Rect scroll = new Rect(), player = new Rect();
    page.findViewById(R.id.quranScroll).getGlobalVisibleRect(scroll);
    page.findViewById(R.id.quranAudioPanel).getGlobalVisibleRect(player);
    assertTrue("Player owns space below the reading viewport", scroll.bottom <= player.top);
    UiEvidence.capture(getInstrumentation(), "QuranDesignReader");
    click(R.id.quranAudioOptions);
    AlertDialog dialog = options();
    assertEquals(Gravity.BOTTOM, dialog.getWindow().getAttributes().gravity);
    assertTrue(dialog.findViewById(R.id.quranAudioDownload).isShown());
    UiEvidence.capture(getInstrumentation(), "QuranDesignAudioSheet");
    File dir = new File(context.getExternalFilesDir(null), "validation");
    dir.mkdirs();
    File clip = new File(dir, "quran-panel-native-preview.mp4");
    if (clip.exists()) clip.delete();
    try (ParcelFileDescriptor recording =
        getInstrumentation()
            .getUiAutomation()
            .executeShellCommand("screenrecord --time-limit 10 " + clip.getAbsolutePath())) {
      SystemClock.sleep(2200);
      main(() -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
      SystemClock.sleep(1800);
      click(R.id.quranAudioOptions);
      SystemClock.sleep(7000);
    }
    assertTrue(clip.isFile() && clip.length() > 1000);
  }

  public void testPanelCancelAndDownloadUsesChosenReciter() {
    launch(true);
    String original =
        context
            .getSharedPreferences("quran_audio_options", 0)
            .getString("reciter", Recitation.IDS[0]);
    click(R.id.quranAudioOptions);
    main(
        () -> {
          ((Spinner) options().findViewById(R.id.quranAudioReciter)).setSelection(1);
          options().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        });
    assertEquals(
        original,
        context
            .getSharedPreferences("quran_audio_options", 0)
            .getString("reciter", Recitation.IDS[0]));
    click(R.id.quranAudioOptions);
    main(() -> ((Spinner) options().findViewById(R.id.quranAudioReciter)).setSelection(1));
    main(() -> options().findViewById(R.id.quranAudioDownload).performClick());
    AlertDialog manager = (AlertDialog) field(field(page, "audioUi"), "downloads");
    assertTrue(manager.isShowing());
    assertTrue(
        ((TextView) manager.findViewById(android.R.id.message))
            .getText()
            .toString()
            .contains(Recitation.NAMES[1]));
    assertEquals(
        Recitation.IDS[1],
        context.getSharedPreferences("quran_audio_options", 0).getString("reciter", ""));
    assertFalse(QuranDownloadService.running);
    UiEvidence.capture(getInstrumentation(), "QuranDesignOfflineManager");
    main(manager::dismiss);
  }

  public void testVerseMenuBookmarkSurvivesRecreation() {
    launch(true);
    main(
        () ->
            ((LinearLayout) page.findViewById(R.id.ayahContainer))
                .getChildAt(2)
                .findViewById(R.id.ayahMore)
                .performClick());
    until(
        () -> {
          AccessibilityNodeInfo root =
              getInstrumentation().getUiAutomation().getRootInActiveWindow();
          if (root == null) return false;
          List<AccessibilityNodeInfo> nodes =
              root.findAccessibilityNodeInfosByText("Tandai terakhir dibaca");
          if (nodes.isEmpty()) return false;
          AccessibilityNodeInfo action = nodes.get(0);
          while (action != null && !action.isClickable()) action = action.getParent();
          return action != null && action.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        });
    until(() -> context.getSharedPreferences("zikra_preferences", 0).getInt("last_ayah", 0) == 3);
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      main(page::recreate);
      page = (QuranActivity) getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull(page);
      ready();
      assertEquals(3, context.getSharedPreferences("zikra_preferences", 0).getInt("last_ayah", 0));
      UiEvidence.capture(getInstrumentation(), "QuranDesignBookmark");
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testNightAndLargeTextSheetRemainsScrollable() throws Exception {
    shell("settings put system font_scale 2.0");
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putString("reading_theme", "dark")
        .commit();
    launch(true);
    assertTrue(page.getResources().getConfiguration().fontScale > 1.5f);
    UiEvidence.capture(getInstrumentation(), "QuranDesignNightLargeReader");
    click(R.id.quranAudioOptions);
    UiEvidence.capture(getInstrumentation(), "QuranDesignNightLargeSheet");
    main(
        () -> {
          View download = options().findViewById(R.id.quranAudioDownload);
          download.requestRectangleOnScreen(
              new Rect(0, 0, download.getWidth(), download.getHeight()), true);
        });
    Rect action = new Rect();
    assertTrue(options().findViewById(R.id.quranAudioDownload).getGlobalVisibleRect(action));
    assertTrue(
        "Sheet keeps confirm action available",
        options().getButton(AlertDialog.BUTTON_POSITIVE).getGlobalVisibleRect(new Rect()));
    assertTrue(
        "Night text keeps contrast",
        ((TextView) options().findViewById(R.id.quranRangeSummary)).getCurrentTextColor()
            == 0xFFEEF6F2);
    main(() -> ((Switch) page.findViewById(R.id.readingNightSwitch)).setChecked(false));
    assertEquals(
        "Open sheet follows a live theme change",
        0xFF15332F,
        ((TextView) options().findViewById(R.id.quranRangeSummary)).getCurrentTextColor());
    assertEquals(
        0xFF15332F,
        ((SeekBar) options().findViewById(R.id.quranAutoSpeed))
            .getProgressTintList()
            .getDefaultColor());
    main(() -> options().getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
  }

  public void testJumpFromReadingSettingsValidatesBuiltInVerse() {
    launch(true);
    click(R.id.quranReadingOptions);
    AlertDialog reading = (AlertDialog) field(page, "readingDialog");
    main(() -> reading.findViewById(R.id.quranSettingsJump).performClick());
    AlertDialog jump = (AlertDialog) field(page, "jumpDialog");
    main(
        () -> {
          EditText input = jump.findViewById(R.id.quranJumpInput);
          input.setText("8");
          jump.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
          assertTrue(jump.isShowing());
          assertNotNull(input.getError());
          input.setText("4");
          jump.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });
    ready();
    until(() -> context.getSharedPreferences("zikra_preferences", 0).getInt("last_ayah", 0) == 4);
    assertFalse(jump.isShowing());
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (page != null) main(page::finish);
      for (Map.Entry<String, Map<String, ?>> item : saved.entrySet()) {
        SharedPreferences.Editor edit =
            context.getSharedPreferences(item.getKey(), 0).edit().clear();
        for (Map.Entry<String, ?> value : item.getValue().entrySet()) {
          String k = value.getKey();
          Object v = value.getValue();
          if (v instanceof Boolean) edit.putBoolean(k, (Boolean) v);
          else if (v instanceof String) edit.putString(k, (String) v);
          else if (v instanceof Integer) edit.putInt(k, (Integer) v);
          else if (v instanceof Long) edit.putLong(k, (Long) v);
          else if (v instanceof Float) edit.putFloat(k, (Float) v);
          else if (v instanceof Set) edit.putStringSet(k, (Set<String>) v);
        }
        edit.commit();
      }
      shell(
          font == null
              ? "settings delete system font_scale"
              : "settings put system font_scale " + Float.parseFloat(font));
    } finally {
      super.tearDown();
    }
  }
}
