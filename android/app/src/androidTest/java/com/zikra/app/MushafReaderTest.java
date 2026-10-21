package com.zikra.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.audio.QuranAudioService;
import com.zikra.app.reading.mushaf.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Live page/font rendering on a dedicated emulator. No stored API token or synthetic Quran text.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public final class MushafReaderTest extends InstrumentationTestCase {
  private Activity page, parent;
  private Context context;
  private final Map<String, Map<String, ?>> preferences = new HashMap<>();

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
  }

  private void until(String message, BooleanSupplier condition) {
    long end = SystemClock.uptimeMillis() + 80000;
    while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < end) SystemClock.sleep(150);
    assertTrue(message, condition.getAsBoolean());
  }

  private Object field(Object target, String name) {
    try {
      Field f = target.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.get(target);
    } catch (Exception error) {
      throw new AssertionError(error);
    }
  }

  private void invoke(Object target, String name, Class<?>[] types, Object... args) {
    main(
        () -> {
          try {
            Method m = target.getClass().getDeclaredMethod(name, types);
            m.setAccessible(true);
            m.invoke(target, args);
          } catch (Exception error) {
            throw new AssertionError(error);
          }
        });
  }

  private void loaded(int number) {
    until(
        "Mushaf page " + number + " and QCF font loaded",
        () ->
            Boolean.TRUE.equals(field(page, "ready")) && ((Integer) field(page, "page")) == number);
    assertTrue(page.findViewById(R.id.mushafPage).getWidth() > 100);
    assertTrue(page.findViewById(R.id.mushafPage).getHeight() > 100);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    for (String name : new String[] {"zikra_preferences", "quran_audio_options"})
      preferences.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .putString("reading_theme", "light")
        .commit();
  }

  public void testActualPageFontsNavigationNightAndZoom() throws Exception {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, MushafActivity.class)
                    .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 1)
                    .putExtra(QuranActivity.EXTRA_TARGET_VERSE, 1)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    loaded(1);
    assertFalse(page.findViewById(R.id.mushafPrevious).isEnabled());
    assertEquals(View.GONE, page.findViewById(R.id.mushafAudioProgress).getVisibility());
    main(() -> page.findViewById(R.id.mushafTranslation).performClick());
    AlertDialog menu = (AlertDialog) field(page, "dialog");
    main(() -> menu.getListView().performItemClick(null, 2, 2));
    assertEquals(View.VISIBLE, page.findViewById(R.id.mushafAudioProgress).getVisibility());
    main(() -> page.findViewById(R.id.mushafTranslation).performClick());
    AlertDialog collapse = (AlertDialog) field(page, "dialog");
    main(() -> collapse.getListView().performItemClick(null, 2, 2));
    main(() -> page.findViewById(R.id.mushafTranslation).performClick());
    AlertDialog sourceMenu = (AlertDialog) field(page, "dialog");
    main(() -> sourceMenu.getListView().performItemClick(null, 3, 3));
    AlertDialog source = (AlertDialog) field(page, "dialog");
    assertTrue(
        ((TextView) source.findViewById(android.R.id.message))
            .getText()
            .toString()
            .contains("Quran Foundation"));
    main(source::dismiss);
    UiEvidence.capture(getInstrumentation(), "Mushaf-AlFatihah");
    for (int number : new int[] {300, 562, 604}) {
      invoke(page, "load", new Class<?>[] {int.class}, number);
      loaded(number);
      MushafDocument data = (MushafDocument) field(page, "document");
      QcfCanvasFont nativeFont =
          (QcfCanvasFont) field(page.findViewById(R.id.mushafPage), "canvasFont");
      for (MushafPage.Line line : data.page.lines)
        for (MushafPage.Word word : line.words)
          for (int cp : word.glyph.codePoints().toArray())
            assertTrue("Native font covers page glyph", nativeFont.metrics.glyph(cp) > 0);
      UiEvidence.capture(getInstrumentation(), "Mushaf-Page-" + number);
    }
    assertFalse(page.findViewById(R.id.mushafNext).isEnabled());
    main(() -> page.findViewById(R.id.mushafPrevious).performClick());
    loaded(603);
    main(() -> page.findViewById(R.id.mushafNext).performClick());
    loaded(604);
    invoke(page, "load", new Class<?>[] {int.class}, 562);
    loaded(562);
    main(() -> page.findViewById(R.id.mushafZoom).performClick());
    AlertDialog options = (AlertDialog) field(page, "dialog");
    main(() -> options.getListView().performItemClick(null, 3, 3));
    until(
        "200% page zoom really enlarges rendered viewport",
        () ->
            ((Integer) field(page, "zoom")) == 200
                && page.findViewById(R.id.mushafPage).getWidth()
                    >= page.findViewById(R.id.mushafHorizontal).getWidth() * 1.9);
    main(() -> page.findViewById(R.id.mushafZoom).performClick());
    AlertDialog night = (AlertDialog) field(page, "dialog");
    main(() -> night.getListView().performItemClick(null, 4, 4));
    assertTrue((Boolean) field(page, "night"));
    UiEvidence.capture(getInstrumentation(), "Mushaf-Night-Zoom-200");
    main(() -> page.findViewById(R.id.mushafPage).performClick());
    assertEquals(View.GONE, page.findViewById(R.id.mushafHeader).getVisibility());
    assertEquals(View.VISIBLE, page.findViewById(R.id.quranAudioPanel).getVisibility());
    main(() -> page.findViewById(R.id.mushafPage).performClick());
  }

  public void testModeSwitchReturnsTheSameSelectedAyah() throws Exception {
    parent =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, QuranActivity.class)
                    .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 67)
                    .putExtra(QuranActivity.EXTRA_TARGET_VERSE, 5)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    until(
        "Translation reader is ready",
        () -> field(parent, "loadedPage") != null && parent.hasWindowFocus());
    invoke(parent, "showReadingOptions", new Class<?>[] {});
    AlertDialog settings = (AlertDialog) field(parent, "readingDialog");
    android.app.Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(MushafActivity.class.getName(), null, false);
    try {
      main(() -> settings.findViewById(R.id.quranOpenMushaf).performClick());
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 15000);
      assertNotNull(page);
      loaded(562);
      main(() -> ((MushafActivity) page).verse(67, 5));
      AlertDialog actions = (AlertDialog) field(page, "dialog");
      main(() -> actions.getListView().performItemClick(null, 1, 1));
      until(
          "Returns to same verse in translation reader",
          () ->
              parent.hasWindowFocus()
                  && ((Integer) field(parent, "selectedIndex")) == 66
                  && ((Integer) field(parent, "pendingVerse")) == 5
                  && field(parent, "loadedPage") != null);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testMiniPlayerWithActualAudioAndTimeline() throws Exception {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, MushafActivity.class)
                    .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 67)
                    .putExtra(QuranActivity.EXTRA_TARGET_VERSE, 5)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    loaded(562);
    Object audio = field(page, "audio");
    until("Audio service bound", () -> field(audio, "service") != null);
    try {
      invoke(audio, "playVerse", new Class<?>[] {int.class}, 5);
      until(
          "Actual recitation plays through native decoder",
          () -> {
            QuranAudioService.State state = (QuranAudioService.State) field(audio, "state");
            return state != null
                && state.chapter == 67
                && state.playing
                && state.position > 250
                && state.duration > 0;
          });
      assertEquals(View.GONE, page.findViewById(R.id.mushafAudioProgress).getVisibility());
      UiEvidence.capture(getInstrumentation(), "Mushaf-Mini-Audio-Active");
      main(() -> page.findViewById(R.id.mushafTranslation).performClick());
      AlertDialog menu = (AlertDialog) field(page, "dialog");
      main(() -> menu.getListView().performItemClick(null, 2, 2));
      assertEquals(View.VISIBLE, page.findViewById(R.id.mushafAudioProgress).getVisibility());
      assertTrue(page.findViewById(R.id.quranAudioSeek).isEnabled());
      UiEvidence.capture(getInstrumentation(), "Mushaf-Expanded-Audio");
      main(() -> page.findViewById(R.id.quranAudioPlay).performClick());
      until("Pause button works", () -> !((QuranAudioService.State) field(audio, "state")).playing);
    } finally {
      main(() -> QuranAudioService.send(context, QuranAudioService.STOP));
    }
  }

  private EditText editText(View root) {
    if (root instanceof EditText) return (EditText) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        EditText found = editText(group.getChildAt(i));
        if (found != null) return found;
      }
    }
    return null;
  }

  public void testPageJumpValidationAndLargeSystemText() throws Exception {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, MushafActivity.class)
                    .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 67)
                    .putExtra(QuranActivity.EXTRA_TARGET_VERSE, 5)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    loaded(562);
    UiEvidence.capture(getInstrumentation(), "Mushaf-System-Font-Large");
    main(() -> page.findViewById(R.id.mushafJump).performClick());
    AlertDialog choices = (AlertDialog) field(page, "dialog");
    main(() -> choices.getListView().performItemClick(null, 0, 0));
    AlertDialog jump = (AlertDialog) field(page, "dialog");
    EditText input = editText(jump.getWindow().getDecorView());
    assertNotNull(input);
    main(
        () -> {
          input.setText("605");
          jump.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });
    assertTrue(jump.isShowing());
    assertNotNull(input.getError());
    main(
        () -> {
          input.setText("604");
          jump.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });
    loaded(604);
    assertEquals(
        604, context.getSharedPreferences("zikra_preferences", 0).getInt("last_mushaf_page", 0));
    main(() -> page.findViewById(R.id.mushafJump).performClick());
    AlertDialog jumpChoices = (AlertDialog) field(page, "dialog");
    main(() -> jumpChoices.getListView().performItemClick(null, 2, 2));
    AlertDialog juz = (AlertDialog) field(page, "dialog");
    main(() -> juz.getListView().performItemClick(null, 28, 28));
    loaded(562);
    assertTrue(((MushafDocument) field(page, "document")).page.contains(67, 1));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (page != null) main(page::finish);
      if (parent != null) main(parent::finish);
      getInstrumentation().waitForIdleSync();
      for (Map.Entry<String, Map<String, ?>> group : preferences.entrySet()) {
        SharedPreferences.Editor edit =
            context.getSharedPreferences(group.getKey(), 0).edit().clear();
        for (Map.Entry<String, ?> item : group.getValue().entrySet()) {
          Object value = item.getValue();
          String key = item.getKey();
          if (value instanceof String) edit.putString(key, (String) value);
          else if (value instanceof Integer) edit.putInt(key, (Integer) value);
          else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
          else if (value instanceof Long) edit.putLong(key, (Long) value);
          else if (value instanceof Float) edit.putFloat(key, (Float) value);
          else if (value instanceof Set) edit.putStringSet(key, (Set<String>) value);
        }
        edit.commit();
      }
    } finally {
      super.tearDown();
    }
  }
}
