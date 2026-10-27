package com.zikra.app;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import com.zikra.app.audio.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.json.*;

/**
 * Six actual CDN recordings, sampled at the first and last global ayah of each supported edition.
 */
@SuppressWarnings("deprecation")
public final class QuranCdnAuditTest extends InstrumentationTestCase {
  public void testActualCdnDownloadsDecodeAndReplayWithoutNetworkOrBookmarkLoss() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    Map<File, byte[]> originals = new HashMap<>();
    JSONArray evidence = new JSONArray();
    OkHttpClient online = new OkHttpClient.Builder().callTimeout(18, TimeUnit.SECONDS).build();
    OkHttpClient offline =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  throw new java.io.IOException("Offline replay must reuse completed local audio");
                })
            .build();
    android.content.SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
    String bookmark = prefs.getString("last_verse_key", "");
    try {
      for (String qari : Recitation.IDS)
        for (int[] ayah : new int[][] {{1, 1}, {114, 6}}) {
          File file = AudioFiles.file(context, qari, ayah[0], ayah[1]);
          originals.put(file, file.exists() ? Files.readAllBytes(file.toPath()) : null);
          if (file.exists()) assertTrue(file.delete());
          AudioTransfer.ensure(context, online, qari, ayah[0], ayah[1], () -> {});
          assertTrue(AudioTransfer.decodable(file));
          AudioTransfer.ensure(context, offline, qari, ayah[0], ayah[1], () -> {});
          MediaPlayer player = MediaPlayer.create(context, Uri.fromFile(file));
          assertNotNull("Native decoder accepts " + qari + " " + ayah[0] + ":" + ayah[1], player);
          try {
            player.start();
            long deadline = SystemClock.uptimeMillis() + 3000;
            while (player.getCurrentPosition() < 100 && SystemClock.uptimeMillis() < deadline)
              SystemClock.sleep(50);
            assertTrue(
                "Native playback position advances offline", player.getCurrentPosition() >= 100);
            StringBuilder hash = new StringBuilder();
            for (byte value :
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath())))
              hash.append(String.format(Locale.ROOT, "%02x", value & 255));
            evidence.put(
                new JSONObject()
                    .put("reciter", qari)
                    .put("verse", ayah[0] + ":" + ayah[1])
                    .put("globalVerse", Recitation.globalVerse(ayah[0], ayah[1]))
                    .put("url", Recitation.url(qari, ayah[0], ayah[1]))
                    .put("bytes", file.length())
                    .put("sha256", hash.toString())
                    .put("durationMs", player.getDuration())
                    .put("positionMs", player.getCurrentPosition()));
          } finally {
            player.release();
          }
        }
      assertEquals(bookmark, prefs.getString("last_verse_key", ""));
      File folder = new File(context.getExternalFilesDir(null), "validation");
      assertTrue(folder.isDirectory() || folder.mkdirs());
      Files.write(
          new File(folder, "D03-Cdn-Audio.json").toPath(),
          evidence.toString(2).getBytes(StandardCharsets.UTF_8));
    } finally {
      for (Map.Entry<File, byte[]> item : originals.entrySet()) {
        if (item.getValue() == null) item.getKey().delete();
        else Files.write(item.getKey().toPath(), item.getValue());
      }
      online.dispatcher().cancelAll();
      offline.dispatcher().cancelAll();
    }
  }
}
