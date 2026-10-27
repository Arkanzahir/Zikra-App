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
        .putBoolean("reader_word_sync", false)
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
    main(() -> QuranAudioService.send(context, QuranAudioService.STOP));
    until("Each test starts without a stale audio queue", () -> state().chapter == 0);
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

  public void testRapidControlsPublishLatestStateAndStopCannotRepostNotification()
      throws Exception {
    seed(1, 20000);
    launch(false);
    main(
        () ->
            QuranAudioService.send(
                context, QuranAudioService.play(1, 1, 1, 1, Recitation.IDS[0], 1, true)));
    until("Playback starts", () -> state().playing);
    main(
        () -> {
          for (int i = 0; i < 4; i++) {
            QuranAudioService.send(context, QuranAudioService.PAUSE);
            QuranAudioService.send(context, QuranAudioService.RESUME);
          }
          QuranAudioService.send(context, QuranAudioService.PAUSE);
        });
    until("Latest pause wins", () -> !state().playing && state().status.equals("Dijeda"));
    until("Latest notification offers Play", () -> mediaNotification("Putar") != null);
    main(() -> QuranAudioService.send(context, QuranAudioService.STOP));
    until("Stop clears queue", () -> state().chapter == 0);
    SystemClock.sleep(1500);
    for (android.service.notification.StatusBarNotification item :
        context.getSystemService(NotificationManager.class).getActiveNotifications())
      assertTrue("Delayed notification cannot return after stop", item.getId() != 7301);
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
              new int[] {R.id.quranAudioPlay, R.id.quranAudioNext, R.id.quranAudioPrevious}) {
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
    seed(1, 10000);
    byte[] valid =
        java.nio.file.Files.readAllBytes(
            AudioFiles.file(context, Recitation.IDS[0], 1, 1).toPath());
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
    assertTrue("Repair instruction shown", state().status.contains("Unduh / lanjutkan"));
    SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
    prefs
        .edit()
        .putInt("last_chapter", 1)
        .putInt("last_ayah", 4)
        .putString("last_verse_key", "1:4")
        .commit();
    okhttp3.OkHttpClient http =
        new okhttp3.OkHttpClient.Builder()
            .addInterceptor(
                chain ->
                    new okhttp3.Response.Builder()
                        .request(chain.request())
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("Synthetic repair fixture")
                        .body(
                            okhttp3.ResponseBody.create(
                                okhttp3.MediaType.parse("audio/wav"), valid))
                        .build())
            .build();
    AudioTransfer.ensure(context, http, Recitation.IDS[0], 1, 1, () -> {});
    assertTrue(AudioTransfer.decodable(AudioFiles.file(context, Recitation.IDS[0], 1, 1)));
    main(() -> QuranAudioService.send(context, QuranAudioService.RESUME));
    until("Repaired local recording plays", () -> state().playing);
    assertEquals("1:4", prefs.getString("last_verse_key", ""));
    main(() -> QuranAudioService.send(context, QuranAudioService.STOP));
    AudioFiles.delete(context, Recitation.IDS[0], 1);
    assertEquals("1:4", prefs.getString("last_verse_key", ""));
  }

  private void syntheticLabel() {
    main(
        () -> {
          TextView label = new TextView(page);
          label.setText("DATA UJI · AUDIO SINTETIS, BUKAN QURAN");
          label.setTextSize(11);
          label.setTextColor(0xFF15332F);
          label.setBackgroundColor(0xFFFBF1DB);
          ((LinearLayout) page.findViewById(R.id.quranRoot)).addView(label, 0);
        });
  }

  public void testCatalogMiniPlayerControlsAudibleQueueAndReturnsToItsVerse() throws Exception {
    seed(1, 20000);
    seed(2, 20000);
    seed(3, 20000);
    launch(false);
    click(R.id.toolbarBack);
    assertEquals(View.GONE, page.findViewById(R.id.quranCatalogAudio).getVisibility());
    main(
        () ->
            QuranAudioService.send(
                context, QuranAudioService.play(1, 1, 3, 2, Recitation.IDS[0], 1, false)));
    until(
        "Catalog observes active queue",
        () ->
            state().playing
                && page.findViewById(R.id.quranCatalogAudio).getVisibility() == View.VISIBLE);
    until(
        "Catalog names audible verse",
        () ->
            ((TextView) page.findViewById(R.id.catalogAudioTitle))
                .getText()
                .toString()
                .contains("Ayat 2"));
    syntheticLabel();
    UiEvidence.capture(getInstrumentation(), "QuranCatalogMiniSynthetic");
    android.graphics.Rect player = new android.graphics.Rect(), dock = new android.graphics.Rect();
    page.findViewById(R.id.catalogAudioCard).getGlobalVisibleRect(player);
    page.findViewById(R.id.dockQuran).getGlobalVisibleRect(dock);
    assertTrue("Mini player stays above navigation", player.bottom <= dock.top);
    click(R.id.catalogAudioPlay);
    until("Catalog pauses decoder", () -> !state().playing && !state().requested);
    click(R.id.catalogAudioPlay);
    until("Catalog resumes same range", () -> state().playing);
    assertEquals(1, state().chapter);
    assertEquals(3, state().last);
    click(R.id.catalogAudioNext);
    until("Next preserves chosen range", () -> state().verse == 3 && state().playing);
    assertFalse(page.findViewById(R.id.catalogAudioNext).isEnabled());
    click(R.id.catalogAudioPrevious);
    until("Previous returns to verse two", () -> state().verse == 2 && state().playing);
    click(R.id.catalogAudioOpen);
    until(
        "Opens audible reader",
        () ->
            page.findViewById(R.id.quranReader).getVisibility() == View.VISIBLE
                && ((Spinner) page.findViewById(R.id.surahSelector)).getSelectedItemPosition()
                    == 0);
    assertEquals(View.GONE, page.findViewById(R.id.quranCatalogAudio).getVisibility());
    main(() -> ((Spinner) page.findViewById(R.id.surahSelector)).setSelection(111));
    until(
        "Different surah finishes opening before Back",
        () ->
            ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount() == 4
                && page.findViewById(R.id.quranReader).getVisibility() == View.VISIBLE);
    click(R.id.toolbarBack);
    until(
        "Browsing other surah does not replace audio",
        () ->
            page.findViewById(R.id.quranCatalogAudio).getVisibility() == View.VISIBLE
                && ((TextView) page.findViewById(R.id.catalogAudioTitle))
                    .getText()
                    .toString()
                    .startsWith("Al-Fatihah"));
    assertEquals(1, state().chapter);
    click(R.id.catalogAudioOpen);
    until(
        "Open uses queue instead of selected surah",
        () -> ((Spinner) page.findViewById(R.id.surahSelector)).getSelectedItemPosition() == 0);
    click(R.id.toolbarBack);
    main(() -> QuranAudioService.send(context, QuranAudioService.STOP));
    until(
        "Stop removes catalog mini player",
        () -> page.findViewById(R.id.quranCatalogAudio).getVisibility() == View.GONE);
  }

  public void testFollowAyahResumesAfterTouchAndColorsActiveVerse() throws Exception {
    seed(1, 20000);
    seed(2, 20000);
    launch(false);
    main(
        () ->
            QuranAudioService.send(
                context, QuranAudioService.play(1, 1, 2, 1, Recitation.IDS[0], 1, false)));
    until("Audio starts", () -> state().playing);
    final ReaderAudioUi audio = (ReaderAudioUi) ui();
    main(audio::userTouch);
    until(
        "Resume-follow action visible",
        () -> page.findViewById(R.id.quranAudioFollowResume).getVisibility() == View.VISIBLE);
    main(() -> QuranAudioService.send(context, QuranAudioService.NEXT));
    until("Next ayah loaded", () -> state().verse == 2 && state().playing);
    TextView arabic =
        ((LinearLayout) page.findViewById(R.id.ayahContainer))
            .getChildAt(1)
            .findViewById(R.id.ayahArabic);
    until(
        "Active ayah changes color",
        () -> arabic.getCurrentTextColor() == page.getColor(R.color.concept_gold_ink));
    main(() -> ((ScrollView) page.findViewById(R.id.quranScroll)).scrollTo(0, 0));
    click(R.id.quranAudioFollowResume);
    until(
        "Follow brings audible ayah into view",
        () -> ((ScrollView) page.findViewById(R.id.quranScroll)).getScrollY() > 20);
    assertEquals(View.GONE, page.findViewById(R.id.quranAudioFollowResume).getVisibility());
    syntheticLabel();
    UiEvidence.capture(getInstrumentation(), "QuranFollowResumedSynthetic");
  }

  public void testCatalogMiniPlayerAtLargeTextAndNight() throws Exception {
    String font =
        android.provider.Settings.System.getString(context.getContentResolver(), "font_scale");
    try {
      shell("settings put system font_scale 2.0");
      context
          .getSharedPreferences("zikra_preferences", 0)
          .edit()
          .putString("reading_theme", "dark")
          .commit();
      seed(1, 20000);
      launch(false);
      click(R.id.toolbarBack);
      main(
          () ->
              QuranAudioService.send(
                  context, QuranAudioService.play(1, 1, 1, 1, Recitation.IDS[0], 1, false)));
      until(
          "Large mini visible",
          () ->
              state().playing
                  && page.findViewById(R.id.quranCatalogAudio).getVisibility() == View.VISIBLE);
      assertTrue(page.getResources().getConfiguration().fontScale > 1.5f);
      android.graphics.Rect card = new android.graphics.Rect(),
          dock = new android.graphics.Rect(),
          play = new android.graphics.Rect();
      page.findViewById(R.id.catalogAudioCard).getGlobalVisibleRect(card);
      page.findViewById(R.id.dockQuran).getGlobalVisibleRect(dock);
      assertTrue(page.findViewById(R.id.catalogAudioPlay).getGlobalVisibleRect(play));
      assertTrue("Large text player clears dock", card.bottom <= dock.top);
      assertTrue(
          "Full play target visible",
          play.height() >= 48 * page.getResources().getDisplayMetrics().density - 1);
      assertEquals(
          0xFFEEF6F2, ((TextView) page.findViewById(R.id.catalogAudioTitle)).getCurrentTextColor());
      syntheticLabel();
      UiEvidence.capture(getInstrumentation(), "QuranCatalogMiniLargeNightSynthetic");
    } finally {
      if (page != null) main(page::finish);
      shell("settings put system font_scale " + (font == null ? "1.0" : font));
    }
  }

  public void testLiveReaderWordColorUsesDecodedAudioAndFollowsNextAyah() throws Exception {
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("reader_word_sync", true)
        .commit();
    launch(false);
    main(
        () ->
            QuranAudioService.send(
                context,
                QuranAudioService.play(1, 2, 3, 2, Recitation.IDS[0], 1, false)
                    .putExtra("word_sync", true)));
    long end = SystemClock.uptimeMillis() + 90000;
    while (!state().playing && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue("Actual timed recording starts: " + state().status, state().playing);
    assertTrue("Timing belongs to active recording", state().wordTimingAvailable);
    TextView arabic =
        ((LinearLayout) page.findViewById(R.id.ayahContainer))
            .getChildAt(1)
            .findViewById(R.id.ayahArabic);
    java.util.Set<Integer> starts = new java.util.HashSet<>();
    long watch = SystemClock.uptimeMillis() + 12000;
    while (starts.size() < 2 && SystemClock.uptimeMillis() < watch && state().verse == 2) {
      main(
          () -> {
            if (arabic.getText() instanceof android.text.Spanned) {
              android.text.Spanned text = (android.text.Spanned) arabic.getText();
              for (android.text.style.ForegroundColorSpan span :
                  text.getSpans(0, text.length(), android.text.style.ForegroundColorSpan.class))
                starts.add(text.getSpanStart(span));
            }
          });
      SystemClock.sleep(50);
    }
    if (starts.size() < 2) {
      Bundle details = new Bundle();
      details.putString("display_text", arabic.getText().toString());
      details.putString("source_words", state().words.toString());
      details.putString("status", state().status);
      details.putInt("verse", state().verse);
      getInstrumentation().sendStatus(0, details);
      UiEvidence.capture(getInstrumentation(), "QuranWordFailure");
    }
    assertTrue("Real decoder advances color across two different words", starts.size() >= 2);
    UiEvidence.capture(getInstrumentation(), "QuranReaderLiveWordColor");
    until("Actual next ayah plays", () -> state().verse == 3 && state().playing);
    until(
        "Audio follows next ayah in reader",
        () -> ((ScrollView) page.findViewById(R.id.quranScroll)).getScrollY() > 30);
    TextView next =
        ((LinearLayout) page.findViewById(R.id.ayahContainer))
            .getChildAt(2)
            .findViewById(R.id.ayahArabic);
    until(
        "Next verse receives word span",
        () ->
            next.getText() instanceof android.text.Spanned
                && ((android.text.Spanned) next.getText())
                        .getSpans(0, next.length(), android.text.style.ForegroundColorSpan.class)
                        .length
                    == 1);
    UiEvidence.capture(getInstrumentation(), "QuranReaderLiveNextAyah");
    main(() -> QuranAudioService.send(context, QuranAudioService.PAUSE));
    until("Actual recitation pauses", () -> !state().playing && !state().requested);
    int paused = state().position;
    SystemClock.sleep(350);
    assertTrue("Decoder position freezes", Math.abs(state().position - paused) < 120);
    main(
        () ->
            QuranAudioService.send(
                context,
                new Intent().setAction(QuranAudioService.SEEK).putExtra("position", 1000)));
    until("Seek updates actual position", () -> Math.abs(state().position - 1000) < 350);
    Bundle evidence = new Bundle();
    evidence.putString("recording", "Live Alafasy Quran.com timed recording");
    evidence.putInt("different_word_offsets", starts.size());
    evidence.putInt("next_verse", state().verse);
    evidence.putInt("duration_ms", state().duration);
    getInstrumentation().sendStatus(0, evidence);
  }

  public void testFoldedPanelFreesReadingSpacePersistsAndKeepsPlayback() throws Exception {
    seed(1, 30000);
    launch(false);
    assertEquals(View.GONE, page.findViewById(R.id.readerAudioBody).getVisibility());
    ScrollView scroll = page.findViewById(R.id.quranScroll);
    int foldedViewport = scroll.getHeight();
    click(R.id.quranAudioFoldedPlay);
    until("Compact play starts audio", () -> state().playing);
    click(R.id.quranAudioExpand);
    until("Panel opens", () -> page.findViewById(R.id.readerAudioBody).isShown());
    assertTrue(
        "Folding saves at least 80dp of reading space",
        foldedViewport - scroll.getHeight()
            >= 80 * page.getResources().getDisplayMetrics().density);
    ReaderAudioUi audio = (ReaderAudioUi) ui();
    main(audio::toggleAutoScroll);
    until("Independent scroll starts", () -> scroll.getScrollY() > 10);
    click(R.id.quranAudioFoldClose);
    assertEquals(View.GONE, page.findViewById(R.id.readerAudioBody).getVisibility());
    java.lang.reflect.Field auto = ReaderAudioUi.class.getDeclaredField("auto");
    auto.setAccessible(true);
    assertTrue("Fold leaves scroll running", auto.getBoolean(audio));
    int position = state().position;
    SystemClock.sleep(500);
    assertTrue("Fold leaves decoder playing", state().playing && state().position > position);
    click(R.id.quranAudioFoldedPlay);
    until("Compact pause works", () -> !state().playing && !state().requested);
    click(R.id.quranAudioFoldedPlay);
    until("Compact resume works", () -> state().playing);
    syntheticLabel();
    UiEvidence.capture(getInstrumentation(), "ReaderFoldedPlayingSynthetic");
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      main(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull(page);
      until(
          "Recreated reader ready",
          () ->
              page.hasWindowFocus()
                  && ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount() == 7);
      assertEquals(
          "Collapsed choice survives recreation",
          View.GONE,
          page.findViewById(R.id.readerAudioBody).getVisibility());
      assertTrue("Recreation does not stop playback", state().playing);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    click(R.id.quranReaderAudio);
    assertTrue(
        "Header audio action reopens controls", page.findViewById(R.id.readerAudioBody).isShown());
    click(R.id.quranAudioOptions);
    assertTrue(options().isShowing());
    AlertDialog panelOptions = options();
    main(panelOptions::dismiss);
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
      context.getSystemService(NotificationManager.class).cancel(7500);
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
