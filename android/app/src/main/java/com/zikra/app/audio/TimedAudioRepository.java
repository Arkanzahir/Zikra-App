package com.zikra.app.audio;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

/** Three transient recordings at most; timing JSON remains in memory, no offline content corpus. */
public final class TimedAudioRepository implements AutoCloseable {
  public static final class Clip {
    public final TimedRecitation data;
    public final File file;
    public final String sha256;

    Clip(TimedRecitation data, File file, String sha256) {
      this.data = data;
      this.file = file;
      this.sha256 = sha256;
    }
  }

  private final File folder;
  private final OkHttpClient http =
      new OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).followRedirects(false).build();
  private final LinkedHashMap<String, Clip> clips = new LinkedHashMap<>(4, .75f, true);

  public TimedAudioRepository(Context context) {
    folder = new File(context.getCacheDir(), "qcf-timed-audio");
    File[] abandoned =
        folder.listFiles((dir, name) -> name.startsWith("ayah-") && name.endsWith(".mp3"));
    if (abandoned != null) for (File file : abandoned) file.delete();
  }

  private byte[] fetch(String url, int limit) throws IOException {
    try (Response response = http.newCall(new Request.Builder().url(url).build()).execute()) {
      if (!response.isSuccessful() || response.body() == null) throw new IOException("HTTP");
      response.body().source().request(limit + 1L);
      if (response.body().source().buffer().size() > limit) throw new IOException("Too large");
      return response.body().bytes();
    }
  }

  private String json(String path) throws IOException {
    return new String(
        fetch("https://api.quran.com/api/v4/" + path, 1024 * 1024), StandardCharsets.UTF_8);
  }

  public synchronized Clip load(String reciter, int chapter, int verse) throws Exception {
    String key = TimedRecitation.id(reciter) + ":" + chapter + ":" + verse;
    Clip previous = clips.get(key);
    if (previous != null && previous.file.isFile()) return previous;
    TimedRecitation data =
        new TimedRecitation(
            chapter,
            verse,
            json(
                "recitations/"
                    + TimedRecitation.id(reciter)
                    + "/by_ayah/"
                    + chapter
                    + ":"
                    + verse
                    + "?fields=segments,duration"),
            json("verses/by_key/" + chapter + ":" + verse + "?words=true&word_fields=text_uthmani"));
    byte[] bytes = fetch(data.url, 8 * 1024 * 1024);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    StringBuilder hash = new StringBuilder();
    for (byte b : digest.digest(bytes)) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
    if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Audio cache folder");
    File file = File.createTempFile("ayah-", ".mp3", folder);
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(bytes);
    } catch (IOException error) {
      file.delete();
      throw error;
    }
    Clip clip = new Clip(data, file, hash.toString());
    clips.put(key, clip);
    while (clips.size() > 3) {
      Iterator<Clip> oldest = clips.values().iterator();
      oldest.next().file.delete();
      oldest.remove();
    }
    return clip;
  }

  public void cancel() {
    http.dispatcher().cancelAll();
  }

  @Override
  public synchronized void close() {
    cancel();
    for (Clip clip : clips.values()) clip.file.delete();
    clips.clear();
  }
}
