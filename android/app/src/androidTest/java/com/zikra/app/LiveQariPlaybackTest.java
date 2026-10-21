package com.zikra.app;

import android.app.*;
import android.content.*;
import android.net.ConnectivityManager;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.widget.LinearLayout;
import com.zikra.app.audio.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Explicit live-network suite. Run only on a disposable emulator, never a user's phone. */
@SuppressWarnings({"deprecation", "unchecked"})
public final class LiveQariPlaybackTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private QuranAudioService service;
  private boolean bound;
  private final Map<File, byte[]> files = new HashMap<>();
  private final Map<String, Map<String, ?>> preferences = new HashMap<>();
  private final ServiceConnection connection =
      new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
          service = ((QuranAudioService.LocalBinder) binder).service();
        }

        public void onServiceDisconnected(ComponentName name) {
          service = null;
        }
      };

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
  }

  private void until(String message, BooleanSupplier condition) {
    long end = SystemClock.uptimeMillis() + 75000;
    while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
    assertTrue(message, condition.getAsBoolean());
  }

  private QuranAudioService.State state() {
    QuranAudioService.State[] result = {null};
    main(() -> result[0] = service.state());
    return result[0];
  }

  private void command(String action) {
    main(() -> QuranAudioService.send(context, action));
  }

  private void seek(int position) {
    main(
        () ->
            QuranAudioService.send(
                context,
                new Intent().setAction(QuranAudioService.SEEK).putExtra("position", position)));
  }

  private void play(int chapter, int first, int last, String qari, int repeats, boolean loop) {
    main(
        () ->
            QuranAudioService.send(
                context, QuranAudioService.play(chapter, first, last, first, qari, repeats, loop)));
    until(
        "Original recording starts: " + qari,
        () -> state().playing && state().chapter == chapter && qari.equals(state().reciter));
    until("Native decoder position advances: " + qari, () -> state().position > 200);
    assertTrue("Native decoder reports duration", state().duration > 1000);
    Bundle evidence = new Bundle();
    evidence.putString("live_qari", qari);
    evidence.putInt("chapter", chapter);
    evidence.putInt("duration_ms", state().duration);
    evidence.putInt("position_ms", state().position);
    getInstrumentation().sendStatus(0, evidence);
  }

  private String shell(String command) throws Exception {
    try (InputStream in =
            new ParcelFileDescriptor.AutoCloseInputStream(
                getInstrumentation().getUiAutomation().executeShellCommand(command));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int n;
      while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
      return bytes.toString("UTF-8").trim();
    }
  }

  private void preserve(File file) throws Exception {
    if (!files.containsKey(file))
      files.put(file, file.exists() ? Files.readAllBytes(file.toPath()) : null);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    assertTrue(
        "Live suite requires a disposable Android emulator",
        Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    context = getInstrumentation().getTargetContext();
    for (String name :
        new String[] {"zikra_preferences", "quran_audio_options", QuranDownloadService.PREFS})
      preferences.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .commit();
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, QuranActivity.class)
                    .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 1)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    until(
        "Reader ready",
        () ->
            page.hasWindowFocus()
                && ((LinearLayout) page.findViewById(R.id.ayahContainer)).getChildCount() == 7);
    main(
        () ->
            bound =
                context.bindService(
                    new Intent(context, QuranAudioService.class),
                    connection,
                    Context.BIND_AUTO_CREATE));
    until("Audio service bound", () -> service != null);
  }

  public void testLiveStreamingThreeQarisControlsAndScreenOff() throws Exception {
    for (String qari : Recitation.IDS) {
      for (int verse = 1; verse <= 2; verse++) {
        File file = AudioFiles.file(context, qari, 1, verse);
        preserve(file);
        if (file.exists()) assertTrue(file.delete());
      }
      play(1, 1, 2, qari, 1, true);
      command(QuranAudioService.PAUSE);
      until("Paused", () -> !state().playing && !state().requested);
      int paused = state().position;
      SystemClock.sleep(300);
      assertTrue("Position stable while paused", Math.abs(state().position - paused) < 150);
      seek(1000);
      until("Seek while paused", () -> Math.abs(state().position - 1000) < 400);
      command(QuranAudioService.RESUME);
      until("Resume", () -> state().playing);
      command(QuranAudioService.NEXT);
      until("Next ayah", () -> state().verse == 2 && state().playing);
      command(QuranAudioService.PREVIOUS);
      until("Previous ayah", () -> state().verse == 1 && state().playing);
      UiEvidence.capture(getInstrumentation(), "LiveQari-" + qari);
      command(QuranAudioService.STOP);
    }
    play(1, 1, 1, Recitation.IDS[0], 1, true);
    main(() -> page.moveTaskToBack(true));
    shell("input keyevent 223");
    try {
      int before = state().position;
      SystemClock.sleep(1000);
      assertTrue("Original recording continues with screen off", state().playing);
      assertTrue("Decoder advances with screen off", state().position > before + 400);
    } finally {
      shell("input keyevent 224");
      shell("wm dismiss-keyguard");
    }
    command(QuranAudioService.STOP);
    for (int verse = 1; verse <= 4; verse++) {
      File file = AudioFiles.file(context, Recitation.IDS[0], 112, verse);
      preserve(file);
      if (file.exists()) assertTrue(file.delete());
    }
    Activity previous = page;
    android.app.Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      if (Build.VERSION.SDK_INT >= 29)
        getInstrumentation()
            .getUiAutomation()
            .adoptShellPermissionIdentity("android.permission.START_ACTIVITIES_FROM_BACKGROUND");
      main(
          () ->
              previous.startActivity(
                  new Intent(context, QuranActivity.class)
                      .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 112)));
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 15000);
      assertNotNull("Surah browser returns from background within deadline", page);
    } finally {
      getInstrumentation().removeMonitor(monitor);
      if (Build.VERSION.SDK_INT >= 29)
        getInstrumentation().getUiAutomation().dropShellPermissionIdentity();
    }
    main(previous::finish);
    play(112, 1, 4, Recitation.IDS[0], 1, false);
    assertEquals(112, state().chapter);
  }

  public void testLiveSurahDownloadRepairAndOfflineRangeRepeat() throws Exception {
    for (int verse = 1; verse <= 7; verse++) {
      File file = AudioFiles.file(context, Recitation.IDS[0], 1, verse);
      preserve(file);
      preserve(new File(file + ".part"));
      if (file.exists()) assertTrue(file.delete());
    }
    File first = AudioFiles.file(context, Recitation.IDS[0], 1, 1);
    assertTrue(first.getParentFile().isDirectory() || first.getParentFile().mkdirs());
    Files.write(first.toPath(), new byte[1024]);
    File partial = new File(first + ".part");
    Files.write(partial.toPath(), new byte[512]);
    context.getSharedPreferences(QuranDownloadService.PREFS, 0).edit().clear().commit();
    main(
        () ->
            context.startForegroundService(
                new Intent(context, QuranDownloadService.class)
                    .setAction("download")
                    .putExtra("chapter", 1)
                    .putExtra("reciter", Recitation.IDS[0])));
    until(
        "Full original Al-Fatihah download completes",
        () ->
            context
                .getSharedPreferences(QuranDownloadService.PREFS, 0)
                .getString("status", "")
                .startsWith("Selesai"));
    until("Download worker stopped", () -> !QuranDownloadService.running);
    assertEquals(7, AudioFiles.downloaded(context, Recitation.IDS[0], 1));
    for (int verse = 1; verse <= 7; verse++)
      assertTrue(
          "Original MP3 decodes: " + verse,
          AudioTransfer.decodable(AudioFiles.file(context, Recitation.IDS[0], 1, verse)));
    assertFalse("Stale partial removed", partial.exists());
    String wifi = shell("settings get global wifi_on");
    String data = shell("settings get global mobile_data");
    try {
      shell("svc wifi disable");
      shell("svc data disable");
      until(
          "Emulator has no active network",
          () -> context.getSystemService(ConnectivityManager.class).getActiveNetwork() == null);
      play(1, 1, 2, Recitation.IDS[0], 2, false);
      UiEvidence.capture(getInstrumentation(), "LiveQari-offline-downloaded");
      seek(state().duration - 400);
      until(
          "Repeat uses the real downloaded MP3",
          () -> state().verse == 1 && state().repeat == 2 && state().playing);
      seek(state().duration - 400);
      until("Range advances offline", () -> state().verse == 2 && state().playing);
      seek(state().duration - 400);
      until(
          "Second ayah repeats offline",
          () -> state().verse == 2 && state().repeat == 2 && state().playing);
      seek(state().duration - 400);
      until("Range ends at selected ayah", () -> state().status.startsWith("Rentang selesai"));
      assertFalse(state().playing);
      File missing = AudioFiles.file(context, Recitation.IDS[1], 112, 1);
      preserve(missing);
      if (missing.exists()) assertTrue(missing.delete());
      main(
          () ->
              QuranAudioService.send(
                  context, QuranAudioService.play(112, 1, 1, 1, Recitation.IDS[1], 1, false)));
      until(
          "Missing audio reports offline failure",
          () ->
              state().chapter == 112
                  && Recitation.IDS[1].equals(state().reciter)
                  && !state().loading
                  && !state().requested);
      assertFalse(state().playing);
    } finally {
      if ("1".equals(wifi)) shell("svc wifi enable");
      if ("1".equals(data)) shell("svc data enable");
    }
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (context != null) {
        context.stopService(new Intent(context, QuranAudioService.class));
        context.stopService(new Intent(context, QuranDownloadService.class));
        if (bound) main(() -> context.unbindService(connection));
        if (page != null) main(page::finish);
        // Finishing the tracked Activity is synchronous above. A continuously updating
        // media UI must not turn cleanup into an unbounded waitForIdleSync on API 37.
        for (Map.Entry<File, byte[]> item : files.entrySet()) {
          if (item.getValue() == null) item.getKey().delete();
          else Files.write(item.getKey().toPath(), item.getValue());
        }
        for (Map.Entry<String, Map<String, ?>> group : preferences.entrySet()) {
          SharedPreferences.Editor edit =
              context.getSharedPreferences(group.getKey(), 0).edit().clear();
          for (Map.Entry<String, ?> item : group.getValue().entrySet()) {
            Object value = item.getValue();
            String key = item.getKey();
            if (value instanceof String) edit.putString(key, (String) value);
            else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) edit.putInt(key, (Integer) value);
            else if (value instanceof Long) edit.putLong(key, (Long) value);
            else if (value instanceof Float) edit.putFloat(key, (Float) value);
            else if (value instanceof Set) edit.putStringSet(key, (Set<String>) value);
          }
          edit.commit();
        }
      }
    } finally {
      super.tearDown();
    }
  }
}
