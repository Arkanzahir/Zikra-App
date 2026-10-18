package com.zikra.app.reading;

import android.content.Context;
import android.os.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;
import org.json.JSONException;

/** On-demand public v4 content; maximum eight cached 50-verse pages, not Content Sync. */
public final class QuranRepository implements AutoCloseable {
  private static final ExecutorService WORK = Executors.newFixedThreadPool(2);
  private static final Object CACHE_LOCK = new Object();
  private static final OkHttpClient HTTP =
      new OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final File directory;
  private volatile boolean closed;
  private volatile int generation;
  private volatile Call active;

  public interface Result {
    void ready(QuranPage page, String source);

    void failed(String message);
  }

  public QuranRepository(Context c) {
    directory = new File(c.getFilesDir(), "quran-pages-v4-r33");
  }

  public void cancel() {
    generation++;
    Call call = active;
    if (call != null) call.cancel();
  }

  public void load(QuranChapters.Chapter chapter, int page, Result result) {
    cancel();
    int token = generation;
    WORK.execute(
        () -> {
          QuranPage local = QuranPage.builtIn(chapter.id);
          if (local != null) {
            deliver(token, () -> result.ready(local, "Empat surah bawaan · offline"));
            return;
          }
          File file = new File(directory, chapter.id + "-" + page + ".json");
          try {
            String cached;
            synchronized (CACHE_LOCK) {
              cached = read(new AtomicFile(file).openRead());
            }
            QuranPage stored = QuranPage.parse(cached, chapter, page);
            synchronized (CACHE_LOCK) {
              file.setLastModified(System.currentTimeMillis());
            }
            deliver(token, () -> result.ready(stored, "Quran.com · halaman tersimpan"));
            return;
          } catch (IOException | JSONException | RuntimeException ignored) {
          }
          Call call =
              HTTP.newCall(
                  new Request.Builder()
                      .url(
                          "https://api.quran.com/api/v4/verses/by_chapter/"
                              + chapter.id
                              + "?language=id&translations=33&fields=text_uthmani&per_page=50&page="
                              + page)
                      .header("Accept", "application/json")
                      .header("User-Agent", "Zikra-Android/1.0")
                      .build());
          active = call;
          if (closed || token != generation) {
            call.cancel();
            return;
          }
          try (Response response = call.execute()) {
            if (!response.isSuccessful() || response.body() == null)
              throw new IOException("HTTP " + response.code());
            response.body().source().request(524289);
            if (response.body().source().buffer().size() > 524288)
              throw new IOException("Page too large");
            String json = response.body().string();
            QuranPage value = QuranPage.parse(json, chapter, page);
            if (closed || token != generation) return;
            save(file, json);
            deliver(token, () -> result.ready(value, "Quran.com · online"));
          } catch (IOException | JSONException | RuntimeException error) {
            if (!call.isCanceled())
              deliver(
                  token,
                  () ->
                      result.failed(
                          "Bacaan belum dapat dimuat. Periksa koneksi lalu coba lagi. Empat surah"
                              + " bawaan dan halaman yang tersimpan tetap dapat dibaca."));
          } finally {
            if (active == call) active = null;
          }
        });
  }

  private String read(InputStream input) throws IOException {
    try (InputStream in = input;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[4096];
      int n;
      while ((n = in.read(b)) != -1) {
        if (out.size() + n > 524288) throw new IOException("Cache too large");
        out.write(b, 0, n);
      }
      return out.toString("UTF-8");
    }
  }

  private void save(File file, String json) {
    synchronized (CACHE_LOCK) {
      if (!directory.exists() && !directory.mkdirs()) return;
      AtomicFile atomic = new AtomicFile(file);
      FileOutputStream stream = null;
      try {
        stream = atomic.startWrite();
        stream.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        atomic.finishWrite(stream);
      } catch (IOException e) {
        if (stream != null) atomic.failWrite(stream);
        return;
      }
      File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
      if (files == null) return;
      Arrays.sort(files, Comparator.comparingLong(File::lastModified));
      for (int i = 0; i < files.length - 8; i++) files[i].delete();
    }
  }

  private void deliver(int token, Runnable action) {
    main.post(
        () -> {
          if (!closed && generation == token) action.run();
        });
  }

  @Override
  public void close() {
    closed = true;
    cancel();
    main.removeCallbacksAndMessages(null);
  }
}
