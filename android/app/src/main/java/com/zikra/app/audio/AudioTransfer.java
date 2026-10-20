package com.zikra.app.audio;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** The downloader and its network/error tests share the same atomic, decoder-checked transfer. */
public final class AudioTransfer {
  private AudioTransfer() {}

  public interface Check {
    void run() throws IOException;
  }

  public static boolean decodable(File file) {
    if (!file.isFile() || file.length() < 129) return false;
    MediaMetadataRetriever metadata = new MediaMetadataRetriever();
    try {
      metadata.setDataSource(file.getAbsolutePath());
      String mime = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
      String duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
      return mime != null
          && mime.startsWith("audio/")
          && duration != null
          && Long.parseLong(duration) > 0;
    } catch (Exception invalid) {
      return false;
    } finally {
      try {
        metadata.release();
      } catch (Exception ignored) {
      }
    }
  }

  public static void ensure(
      Context context, OkHttpClient http, String qari, int chapter, int verse, Check check)
      throws IOException {
    check.run();
    File target = AudioFiles.file(context, qari, chapter, verse);
    if (decodable(target)) return;
    // A completed-looking but corrupt file must not be skipped on every retry.
    if (target.exists() && !target.delete()) throw new IOException("Cannot replace corrupt audio");
    if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs())
      throw new IOException("Cannot create audio folder");
    File partial = new File(target.getPath() + ".part");
    try {
      Request request =
          new Request.Builder()
              .url(Recitation.url(qari, chapter, verse))
              .header("User-Agent", "Zikra-Android/1.0 (+https://github.com/Arkanzahir/Zikra-App)")
              .build();
      try (Response response = http.newCall(request).execute()) {
        if (!response.isSuccessful() || response.body() == null)
          throw new IOException("Audio HTTP");
        try (InputStream in = response.body().byteStream();
            FileOutputStream out = new FileOutputStream(partial)) {
          byte[] bytes = new byte[16384];
          int n;
          long size = 0;
          while ((n = in.read(bytes)) != -1) {
            check.run();
            size += n;
            if (size > 20L * 1024 * 1024) throw new IOException("Audio too large");
            out.write(bytes, 0, n);
          }
          out.getFD().sync();
          if (size < 129
              || (response.body().contentLength() >= 0 && size != response.body().contentLength()))
            throw new IOException("Incomplete audio");
        }
      }
      if (!decodable(partial)) throw new IOException("Invalid audio");
      check.run();
      if (!partial.renameTo(target)) throw new IOException("Cannot save audio");
    } finally {
      if (partial.exists()) partial.delete();
    }
  }
}
