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
  private final OkHttpClient http;
  private final Object lock = new Object();
  private final Set<Call> active = new HashSet<>();
  private final LinkedHashMap<String, Clip> clips = new LinkedHashMap<>(4, .75f, true);
  private long generation;
  private boolean closed;

  public TimedAudioRepository(Context context) {
    this(
        new File(context.getCacheDir(), "qcf-timed-audio"),
        new OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .build());
  }

  TimedAudioRepository(File folder, OkHttpClient http) {
    this.folder = folder;
    this.http = http;
    File[] abandoned =
        folder.listFiles((dir, name) -> name.startsWith("ayah-") && name.endsWith(".mp3"));
    if (abandoned != null) for (File file : abandoned) file.delete();
  }

  private void check(long token) throws IOException {
    synchronized (lock) {
      if (closed || token != generation) throw new IOException("Audio request cancelled");
    }
  }

  private byte[] fetch(String url, int limit, long token, long deadline) throws IOException {
    Call call = http.newCall(new Request.Builder().url(url).build());
    call.timeout().deadlineNanoTime(deadline);
    synchronized (lock) {
      check(token);
      active.add(call);
    }
    try (Response response = call.execute()) {
      check(token);
      if (!response.isSuccessful() || response.body() == null) throw new IOException("HTTP");
      response.body().source().request(limit + 1L);
      check(token);
      if (response.body().source().buffer().size() > limit) throw new IOException("Too large");
      return response.body().bytes();
    } finally {
      synchronized (lock) {
        active.remove(call);
      }
    }
  }

  private String json(String path, long token, long deadline) throws IOException {
    return new String(
        fetch("https://api.quran.com/api/v4/" + path, 1024 * 1024, token, deadline),
        StandardCharsets.UTF_8);
  }

  public Clip load(String reciter, int chapter, int verse) throws Exception {
    return load(reciter, chapter, verse, generation());
  }

  long generation() {
    synchronized (lock) {
      return generation;
    }
  }

  Clip load(String reciter, int chapter, int verse, long token) throws Exception {
    Recitation.globalVerse(chapter, verse);
    String key = TimedRecitation.id(reciter) + ":" + chapter + ":" + verse;
    synchronized (lock) {
      check(token);
      Clip previous = clips.get(key);
      if (previous != null && previous.file.isFile()) return previous;
    }
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
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
                    + "?fields=segments,duration",
                token,
                deadline),
            json(
                "verses/by_key/" + chapter + ":" + verse + "?words=true&word_fields=text_uthmani",
                token,
                deadline));
    byte[] bytes = fetch(data.url, 8 * 1024 * 1024, token, deadline);
    check(token);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    StringBuilder hash = new StringBuilder();
    for (byte b : digest.digest(bytes)) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
    if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Audio cache folder");
    File file = File.createTempFile("ayah-", ".mp3", folder);
    boolean published = false;
    try {
      try (FileOutputStream out = new FileOutputStream(file)) {
        out.write(bytes);
      }
      synchronized (lock) {
        check(token);
        Clip clip = new Clip(data, file, hash.toString());
        Clip replaced = clips.put(key, clip);
        if (replaced != null) replaced.file.delete();
        while (clips.size() > 3) {
          Iterator<Clip> oldest = clips.values().iterator();
          oldest.next().file.delete();
          oldest.remove();
        }
        published = true;
        return clip;
      }
    } finally {
      if (!published) file.delete();
    }
  }

  public void cancel() {
    List<Call> pending;
    synchronized (lock) {
      generation++;
      pending = new ArrayList<>(active);
    }
    for (Call call : pending) call.cancel();
  }

  @Override
  public void close() {
    List<Call> pending;
    synchronized (lock) {
      closed = true;
      generation++;
      pending = new ArrayList<>(active);
      for (Clip clip : clips.values()) clip.file.delete();
      clips.clear();
    }
    for (Call call : pending) call.cancel();
  }
}
