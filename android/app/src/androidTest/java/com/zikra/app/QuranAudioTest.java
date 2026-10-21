package com.zikra.app;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.audio.*;
import com.zikra.app.data.HifzTarget;
import com.zikra.app.reading.QuranChapters;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Real platform playback of synthetic WAV fixtures. Never presents these fixtures as Quran audio.
 */
@SuppressWarnings("deprecation")
public final class QuranAudioTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private QuranAudioService service;
  private boolean bound;
  private final Map<String, Map<String, ?>> preferences = new HashMap<>();
  private final Map<File, byte[]> files = new HashMap<>();
  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName n, IBinder b) {
          service = ((QuranAudioService.LocalBinder) b).service();
        }

        @Override
        public void onServiceDisconnected(ComponentName n) {
          service = null;
        }
      };

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    for (String name :
        new String[] {"zikra_preferences", "quran_audio_options", QuranDownloadService.PREFS})
      preferences.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .putString("reading_theme", "light")
        .commit();
    context
        .getSharedPreferences("quran_audio_options", 0)
        .edit()
        .clear()
        .putString("reciter", Recitation.IDS[0])
        .putBoolean("follow", true)
        .putInt("repeats", 2)
        .commit();
    context.getSharedPreferences(QuranDownloadService.PREFS, 0).edit().clear().commit();
  }

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
  }

  private void until(String message, BooleanSupplier condition) {
    long end = SystemClock.uptimeMillis() + 20000;
    while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue(message, condition.getAsBoolean());
  }

  private QuranAudioService.State state() {
    final QuranAudioService.State[] result = {null};
    main(
        () -> {
          if (service != null) result[0] = service.state();
        });
    return result[0];
  }

  private void launch(boolean hifz) {
    Intent intent =
        hifz
            ? StudyActivity.readingIntent(context, new HifzTarget(1, "Al-Fatihah", 7, 2, 3))
            : new Intent(context, QuranActivity.class)
                .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 1);
    page = getInstrumentation().startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    until("Reader receives focus", () -> page.hasWindowFocus());
    until(
        "Native verses loaded",
        () -> ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount() == 7);
    main(
        () ->
            bound =
                context.bindService(
                    new Intent(context, QuranAudioService.class),
                    connection,
                    Context.BIND_AUTO_CREATE));
    until("Audio service bound", () -> service != null);
  }

  private void seed(int verse, int durationMs) throws Exception {
    File file = AudioFiles.file(context, Recitation.IDS[0], 1, verse);
    files.putIfAbsent(file, file.exists() ? java.nio.file.Files.readAllBytes(file.toPath()) : null);
    file.getParentFile().mkdirs();
    int samples = 22050 * durationMs / 1000, size = samples * 2;
    ByteBuffer wav = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN);
    wav.put("RIFF".getBytes("US-ASCII"))
        .putInt(36 + size)
        .put("WAVEfmt ".getBytes("US-ASCII"))
        .putInt(16)
        .putShort((short) 1)
        .putShort((short) 1)
        .putInt(22050)
        .putInt(44100)
        .putShort((short) 2)
        .putShort((short) 16)
        .put("data".getBytes("US-ASCII"))
        .putInt(size);
    for (int i = 0; i < samples; i++)
      wav.putShort((short) (500 * Math.sin(2 * Math.PI * 440 * i / 22050)));
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(wav.array());
    }
  }

  private void click(int id) {
    main(() -> page.findViewById(id).performClick());
    getInstrumentation().waitForIdleSync();
  }

  private Object ui() throws Exception {
    java.lang.reflect.Field field = QuranActivity.class.getDeclaredField("audioUi");
    field.setAccessible(true);
    return field.get(page);
  }

  private AlertDialog options() throws Exception {
    Object ui = ui();
    java.lang.reflect.Field f = ReaderAudioUi.class.getDeclaredField("options");
    f.setAccessible(true);
    return (AlertDialog) f.get(ui);
  }

  private void shell(String command) throws Exception {
    try (InputStream in =
        new ParcelFileDescriptor.AutoCloseInputStream(
            getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      byte[] buffer = new byte[1024];
      while (in.read(buffer) != -1) {}
    }
  }

  public void testHifzRangeRepeatsRealPlaybackAndStopsAtChosenLastAyah() throws Exception {
    seed(2, 900);
    seed(3, 900);
    launch(true);
    List<String> heard = Collections.synchronizedList(new ArrayList<>());
    main(
        () ->
            service.addListener(
                s -> {
                  if (s.playing) {
                    String key = s.verse + ":" + s.repeat;
                    if (heard.isEmpty() || !heard.get(heard.size() - 1).equals(key)) heard.add(key);
                  }
                }));
    click(R.id.quranAudioPlay);
    until("Hifz audio starts", () -> state().playing);
    assertEquals(2, state().first);
    assertEquals(3, state().last);
    main(
        () -> {
          TextView label = new TextView(page);
          label.setText("UJI PEMUTAR · AUDIO SINTETIS, BUKAN REKAMAN QURAN");
          label.setTextSize(10);
          label.setTextColor(0xFF15332F);
          label.setBackgroundColor(0xFFFBF1DB);
          ((LinearLayout) page.findViewById(R.id.quranRoot)).addView(label, 0);
        });
    UiEvidence.capture(getInstrumentation(), "QuranPlayerSyntheticAudioTest");
    until("Chosen range finishes", () -> state().status.startsWith("Rentang selesai"));
    assertEquals(Arrays.asList("2:1", "2:2", "3:1", "3:2"), heard);
    assertEquals(3, state().verse);
    assertFalse(state().playing);
    assertEquals(
        "Ayat aktif di pemutar",
        ((TextView)
                ((LinearLayout) page.findViewById(R.id.ayahContainer))
                    .getChildAt(2)
                    .findViewById(R.id.ayahAudioStatus))
            .getText()
            .toString());
    assertEquals(114, QuranChapters.all(context).size());
    for (QuranChapters.Chapter c : QuranChapters.all(context))
      assertEquals(c.count, Recitation.count(c.id));
  }

  public void testNotificationPauseFocusAndScreenOffBackgroundPlayback() throws Exception {
    seed(1, 10000);
    launch(false);
    main(
        () ->
            QuranAudioService.send(
                context, QuranAudioService.play(1, 1, 1, 1, Recitation.IDS[0], 1, true)));
    until("Real local playback starts", () -> state().playing);
    until("Playing notification offers Pause", () -> mediaNotification("Jeda") != null);
    android.service.notification.StatusBarNotification item = mediaNotification("Jeda");
    assertNotNull("Media notification exists", item);
    assertNotNull(
        "MediaSession token exists",
        item.getNotification().extras.getParcelable(Notification.EXTRA_MEDIA_SESSION));
    item.getNotification().actions[1].actionIntent.send();
    until("Notification pauses audio", () -> !state().playing && state().status.equals("Dijeda"));
    until("Paused notification offers Play", () -> mediaNotification("Putar") != null);
    item = mediaNotification("Putar");
    item.getNotification().actions[1].actionIntent.send();
    until("Notification resumes audio", () -> state().playing);
    main(
        () ->
            QuranAudioService.send(
                context,
                new Intent().setAction(QuranAudioService.SEEK).putExtra("position", 5000)));
    until("Seek changes the real decoder position", () -> state().position >= 4500);
    AudioManager audio = context.getSystemService(AudioManager.class);
    AudioFocusRequest other =
        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setOnAudioFocusChangeListener(change -> {}, new Handler(Looper.getMainLooper()))
            .build();
    final int[] granted = {0};
    main(() -> granted[0] = audio.requestAudioFocus(other));
    assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, granted[0]);
    until("Transient focus loss pauses recitation", () -> !state().playing);
    main(() -> audio.abandonAudioFocusRequest(other));
    until("Focus return resumes recitation", () -> state().playing);
    main(() -> page.moveTaskToBack(true));
    shell("input keyevent 223");
    try {
      SystemClock.sleep(1300);
      assertTrue("Playback continues with screen off", state().playing);
    } finally {
      shell("input keyevent 224");
      shell("wm dismiss-keyguard");
    }
  }

  private android.service.notification.StatusBarNotification mediaNotification(String action) {
    for (android.service.notification.StatusBarNotification item :
        context.getSystemService(NotificationManager.class).getActiveNotifications()) {
      Notification.Action[] buttons = item.getNotification().actions;
      if (item.getId() == 7301
          && buttons != null
          && buttons.length > 1
          && action.contentEquals(buttons[1].title)) return item;
    }
    return null;
  }

  public void testAutoScrollStopsOnManualTouchAndRangeValidationKeepsDialogOpen() throws Exception {
    launch(false);
    click(R.id.quranAudioOptions);
    AlertDialog dialog = options();
    main(
        () -> {
          ((EditText) dialog.findViewById(R.id.quranAudioLast)).setText("8");
          dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });
    assertTrue(dialog.isShowing());
    assertNotNull(((EditText) dialog.findViewById(R.id.quranAudioLast)).getError());
    UiEvidence.capture(getInstrumentation(), "QuranAudioOptionsTest");
    main(
        () -> {
          ((EditText) dialog.findViewById(R.id.quranAudioLast)).setText("7");
          dialog.findViewById(R.id.quranAutoScroll).performClick();
          assertFalse(((Switch) dialog.findViewById(R.id.quranAudioFollow)).isChecked());
          dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });
    ScrollView scroll = page.findViewById(R.id.quranScroll);
    until("Reading scroll moves without audio", () -> scroll.getScrollY() > 15);
    main(
        () -> {
          long now = SystemClock.uptimeMillis();
          MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 2, 50, 0);
          MotionEvent up = MotionEvent.obtain(now, now + 1, MotionEvent.ACTION_UP, 2, 50, 0);
          scroll.dispatchTouchEvent(down);
          scroll.dispatchTouchEvent(up);
          down.recycle();
          up.recycle();
        });
    Object ui = ui();
    java.lang.reflect.Field automatic = ReaderAudioUi.class.getDeclaredField("auto");
    automatic.setAccessible(true);
    assertFalse("Manual touch stops automatic movement", automatic.getBoolean(ui));
    int stopped = scroll.getScrollY();
    SystemClock.sleep(400);
    assertEquals(stopped, scroll.getScrollY());
    assertEquals(0, state().chapter);
  }

  public void testOfflineDownloadResumesUsingCompletedFilesAndIgnoresPartialFiles()
      throws Exception {
    for (int verse = 1; verse <= 7; verse++) seed(verse, 200);
    launch(false);
    File partial = new File(AudioFiles.file(context, Recitation.IDS[0], 2, 1).getPath() + ".part");
    partial.getParentFile().mkdirs();
    try (FileOutputStream out = new FileOutputStream(partial)) {
      out.write(new byte[1024]);
    }
    try {
      assertFalse(
          "Partial audio is never playable",
          AudioFiles.available(context, Recitation.IDS[0], 2, 1));
    } finally {
      partial.delete();
    }
    main(
        () ->
            context.startForegroundService(
                new Intent(context, QuranDownloadService.class)
                    .setAction("download")
                    .putExtra("chapter", 1)
                    .putExtra("reciter", Recitation.IDS[0])));
    until(
        "Verified existing files complete offline without HTTP",
        () ->
            context
                .getSharedPreferences(QuranDownloadService.PREFS, 0)
                .getString("status", "")
                .startsWith("Selesai"));
    assertEquals(7, AudioFiles.downloaded(context, Recitation.IDS[0], 1));
    AudioFiles.delete(context, Recitation.IDS[0], 1);
    assertEquals(0, AudioFiles.downloaded(context, Recitation.IDS[0], 1));
    assertEquals(
        "", context.getSharedPreferences(QuranDownloadService.PREFS, 0).getString("status", ""));
  }

  public void testPlayerControlsRemainReachableAt200PercentText() {
    main(
        () -> {
          android.content.res.Configuration config =
              new android.content.res.Configuration(context.getResources().getConfiguration());
          config.fontScale = 2f;
          config.screenWidthDp = 320;
          config.screenHeightDp = 560;
          Context scoped =
              new android.view.ContextThemeWrapper(
                  context.createConfigurationContext(config), R.style.Theme_Zikra);
          float density = scoped.getResources().getDisplayMetrics().density;
          ViewGroup panel =
              (ViewGroup) LayoutInflater.from(scoped).inflate(R.layout.view_quran_audio, null);
          panel.setVisibility(View.VISIBLE);
          ((TextView) panel.findViewById(R.id.quranAudioTitle)).setText("Al-Fatihah · Ayat 1–7");
          ((TextView) panel.findViewById(R.id.quranAudioStatus))
              .setText("Mahmoud Khalil Al-Husary · pengulangan 3 kali per ayat");
          int width = Math.round(320 * density);
          panel.measure(
              View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
          panel.layout(0, 0, width, panel.getMeasuredHeight());
          assertTrue("Reader retains a usable viewport", panel.getHeight() < 350 * density);
          for (int id :
              new int[] {
                R.id.quranAudioPlay,
                R.id.quranAudioNext,
                R.id.quranAudioPrevious,
                R.id.quranAudioOptions
              }) {
            View action = panel.findViewById(id);
            android.graphics.Rect box = new android.graphics.Rect();
            action.getDrawingRect(box);
            panel.offsetDescendantRectToMyCoords(action, box);
            assertTrue("Control remains inside phone", box.left >= 0 && box.right <= width);
            assertTrue(
                "Touch target remains usable",
                action.getWidth() >= 48 * density && action.getHeight() >= 48 * density);
          }
        });
  }

  public void testBrowsingAnotherSurahKeepsBackgroundAudioVisibleAndCanReturnToIt()
      throws Exception {
    seed(1, 10000);
    launch(false);
    main(
        () ->
            QuranAudioService.send(
                context, QuranAudioService.play(1, 1, 1, 1, Recitation.IDS[0], 1, true)));
    until("Playback starts", () -> state().playing);
    main(() -> ((Spinner) page.findViewById(R.id.surahSelector)).setSelection(111));
    until(
        "Another built-in surah opens",
        () -> ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount() == 4);
    until(
        "UI explains the real background source",
        () ->
            ((TextView) page.findViewById(R.id.quranAudioStatus))
                .getText()
                .toString()
                .startsWith("Audio Al-Fatihah"));
    assertEquals(1, state().chapter);
    assertTrue(state().playing);
    click(R.id.quranAudioTitle);
    until(
        "Title returns to the audible surah",
        () -> ((Spinner) page.findViewById(R.id.surahSelector)).getSelectedItemPosition() == 0);
    assertEquals(1, state().chapter);
  }

  public void testCorruptOfflineAudioShowsErrorWithoutCrashingReader() throws Exception {
    seed(1, 200);
    try (FileOutputStream out =
        new FileOutputStream(AudioFiles.file(context, Recitation.IDS[0], 1, 1))) {
      out.write(new byte[1024]);
    }
    launch(false);
    click(R.id.quranAudioPlay);
    until(
        "Decoder failure is reported",
        () ->
            state().status.contains("tidak dapat diputar")
                || state().status.contains("belum tersedia")
                || state().status.contains("terhenti"));
    assertFalse(state().playing);
    assertFalse(state().loading);
    assertTrue("Reader remains open after decoder error", page.hasWindowFocus());
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (page != null) main(page::finish);
      if (bound) main(() -> context.unbindService(connection));
      context.stopService(new Intent(context, QuranAudioService.class));
      context.stopService(new Intent(context, QuranDownloadService.class));
      getInstrumentation().waitForIdleSync();
      context.getSystemService(NotificationManager.class).cancel(7301);
      context.getSystemService(NotificationManager.class).cancel(7302);
      for (Map.Entry<File, byte[]> item : files.entrySet()) {
        if (item.getValue() == null) item.getKey().delete();
        else
          try (FileOutputStream out = new FileOutputStream(item.getKey())) {
            out.write(item.getValue());
          }
      }
      for (Map.Entry<String, Map<String, ?>> group : preferences.entrySet()) {
        SharedPreferences.Editor editor =
            context.getSharedPreferences(group.getKey(), 0).edit().clear();
        for (Map.Entry<String, ?> item : group.getValue().entrySet()) {
          Object v = item.getValue();
          String key = item.getKey();
          if (v instanceof String) editor.putString(key, (String) v);
          else if (v instanceof Boolean) editor.putBoolean(key, (Boolean) v);
          else if (v instanceof Integer) editor.putInt(key, (Integer) v);
          else if (v instanceof Long) editor.putLong(key, (Long) v);
          else if (v instanceof Float) editor.putFloat(key, (Float) v);
          else if (v instanceof Set) editor.putStringSet(key, (Set<String>) v);
        }
        editor.commit();
      }
    } finally {
      super.tearDown();
    }
  }
}
