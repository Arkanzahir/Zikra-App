package com.zikra.app.reading;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.HomeActivity;
import com.zikra.app.audio.QuranAudioService;
import com.zikra.app.audio.Recitation;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONObject;

/** Physical-device receipt. Reads reader settings and checks live playback without seeding fixtures. */
@SuppressWarnings("deprecation")
public final class PhoneQuranReleaseTest extends InstrumentationTestCase {
  private Context phone() {
    assertFalse("USB test requires a physical device",
        "ranchu".equals(Build.HARDWARE) || "goldfish".equals(Build.HARDWARE));
    return getInstrumentation().getTargetContext();
  }

  public void testSnapshotReaderAndOfflineAudio() throws Exception {
    Context context = phone();
    SharedPreferences reader = context.getSharedPreferences("zikra_preferences", Context.MODE_PRIVATE);
    SharedPreferences audio = context.getSharedPreferences("quran_audio_options", Context.MODE_PRIVATE);
    JSONObject snapshot = new JSONObject()
        .put("chapter", reader.getInt("last_chapter", 0))
        .put("ayah", reader.getInt("last_ayah", 0))
        .put("verseKey", reader.getString("last_verse_key", ""))
        .put("wordFollow", reader.getBoolean("reader_word_sync", false))
        .put("reciter", audio.getString("reciter", ""));
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    Path root = context.getFilesDir().toPath().resolve("quran-audio");
    List<Path> offline = java.util.Collections.emptyList();
    if (Files.isDirectory(root)) {
      try (Stream<Path> entries = Files.walk(root)) {
        offline = entries.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".mp3"))
            .sorted().collect(Collectors.toList());
      }
    }
    for (Path file : offline) {
      digest.update((root.relativize(file) + ":" + Files.size(file) + ":" +
          Files.getLastModifiedTime(file).toMillis() + "\n").getBytes(StandardCharsets.UTF_8));
    }
    Bundle result = new Bundle();
    result.putString("reader", hex(MessageDigest.getInstance("SHA-256")
        .digest(snapshot.toString().getBytes(StandardCharsets.UTF_8))));
    result.putString("offline", hex(digest.digest()));
    result.putInt("offlineCount", offline.size());
    getInstrumentation().sendStatus(0, result);
  }

  public void testLiveMurottalContinuesBetweenAyat() throws Exception {
    Context context = phone();
    JSONObject selection;
    try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand("cat /data/local/tmp/zikra-usb-murottal.json"))) {
      byte[] bytes = input.readNBytes(2049);
      assertTrue("Missing or oversized test selection", bytes.length > 1 && bytes.length <= 2048);
      selection = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    }
    int chapter = selection.getInt("chapter");
    int first = selection.getInt("verse");
    assertTrue(first > 0 && first < Recitation.count(chapter));
    String chosen = selection.optString("reciter");
    String[] reciters = chosen.isEmpty() ? Recitation.IDS : new String[] {chosen};
    Activity page = getInstrumentation().startActivitySync(new Intent(context, HomeActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    CountDownLatch ready = new CountDownLatch(1);
    AtomicReference<QuranAudioService> player = new AtomicReference<>();
    ServiceConnection listener = new ServiceConnection() {
      @Override public void onServiceConnected(ComponentName name, IBinder binder) {
        player.set(((QuranAudioService.LocalBinder) binder).service());
        ready.countDown();
      }
      @Override public void onServiceDisconnected(ComponentName name) { player.set(null); }
    };
    boolean bound = context.bindService(new Intent(context, QuranAudioService.class),
        listener, Context.BIND_AUTO_CREATE);
    try {
      assertTrue(bound && ready.await(12, TimeUnit.SECONDS));
      for (String reciter : reciters) {
        Recitation.reciterIndex(reciter);
        getInstrumentation().runOnMainSync(() -> QuranAudioService.send(context,
            QuranAudioService.play(chapter, first, first + 1, first, reciter, 1, false)));
        awaitPlayback(player.get(), reciter, chapter, first);
        awaitPlayback(player.get(), reciter, chapter, first + 1);
      }
      Bundle result = new Bundle();
      result.putInt("recitersPassed", reciters.length);
      getInstrumentation().sendStatus(0, result);
    } finally {
      getInstrumentation().runOnMainSync(() -> {
        if (bound && player.get() != null) QuranAudioService.send(context, QuranAudioService.STOP);
        if (bound) context.unbindService(listener);
        page.finish();
      });
    }
  }

  private void awaitPlayback(QuranAudioService player, String reciter, int chapter, int verse) throws Exception {
    assertNotNull(player);
    long deadline = SystemClock.elapsedRealtime() + 40000;
    AtomicReference<QuranAudioService.State> latest = new AtomicReference<>();
    while (SystemClock.elapsedRealtime() < deadline) {
      getInstrumentation().runOnMainSync(() -> latest.set(player.state()));
      QuranAudioService.State state = latest.get();
      if (reciter.equals(state.reciter) && state.chapter == chapter && state.verse == verse
          && state.playing && state.position > 100) return;
      Thread.sleep(80);
    }
    fail("Playback did not advance for " + reciter + " " + chapter + ":" + verse
        + " (" + latest.get().status + ")");
  }

  private String hex(byte[] digest) {
    StringBuilder output = new StringBuilder(digest.length * 2);
    for (byte value : digest) output.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
    return output.toString();
  }
}
