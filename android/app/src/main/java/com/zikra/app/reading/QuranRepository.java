package com.zikra.app.reading;

import android.content.Context;
import android.os.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;
import org.json.JSONException;

/** Synced pages plus preserved legacy data; ordinary API reads are no longer cached. */
public final class QuranRepository implements AutoCloseable {
  private static final ExecutorService WORK = Executors.newFixedThreadPool(2);
  private static final Object CACHE_LOCK = new Object();
  private static final OkHttpClient HTTP =
      new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(55, TimeUnit.SECONDS)
          .callTimeout(60, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
          .retryOnConnectionFailure(false).build();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Context context;
  private final File directory;
  private final File syncedCache;
  private final QuranSyncStore syncedStore;
  private final File offlineDirectory;
  private volatile boolean closed;
  private volatile int generation;
  private volatile Call active;

  public interface Result {
    void ready(QuranPage page, String source);

    void failed(String message);
  }

  public QuranRepository(Context c) {
    context = c.getApplicationContext();
    syncedStore = new QuranSyncStore(context);
    syncedCache = new File(c.getFilesDir(), "quran-sync-page-cache-r33");
    directory = new File(c.getFilesDir(), "quran-pages-v4-r33");
    offlineDirectory = new File(c.getFilesDir(), "quran-offline-r33");
  }

  public synchronized void cancel() {
    generation++;
    Call call = active;
    if (call != null) call.cancel();
  }
  private synchronized void track(int token, Call call) {
    if (closed || token != generation) call.cancel(); else active = call;
  }
  private synchronized void untrack(Call call) { if (active == call) active = null; }

  public void load(QuranChapters.Chapter chapter, int page, Result result) {
    cancel();
    int token = generation;
    WORK.execute(() -> {
      String stored = null;
      String source = "Quran Foundation · Content Sync · tersimpan";
      long checked = 0;
      try { stored = syncedStore.read(chapter, page); }
      catch (Exception missing) {
        try {
          synchronized (CACHE_LOCK) {
            stored = read(new AtomicFile(new File(syncedCache, chapter.id + "-" + page + ".json")).openRead());
          }
        }
        catch (Exception absent) { /* No synchronized page yet. */ }
      }
      if (stored != null) {
        try { checked = QuranSyncPage.validate(stored, chapter, page, System.currentTimeMillis()); }
        catch (Exception invalid) { stored = null; }
      }
      QuranPage fallback = null;
      try { if (stored != null) fallback = QuranPage.parse(stored, chapter, page); }
      catch (Exception invalid) { /* Preserve a previous valid source below. */ }
      if (fallback == null) {
        fallback = QuranPage.builtIn(chapter.id);
        source = "Empat surah bawaan · offline";
      }
      if (fallback == null) {
        for (File folder : new File[]{offlineDirectory, directory}) {
          try {
            fallback = QuranPage.parse(read(new AtomicFile(new File(folder, chapter.id + "-" + page + ".json")).openRead()), chapter, page);
            source = "Quran.com · bacaan lama tersimpan";
            break;
          } catch (Exception missing) { /* Keep looking without deleting legacy data. */ }
        }
      }
      final boolean displayed = fallback != null;
      final boolean removed = QuranSyncGateway.sourceRemoved(context);
      if (displayed && !removed) {
        final QuranPage value = fallback; final String label = source;
        deliver(token, () -> result.ready(value, label));
        if (QuranSyncPage.fresh(checked, System.currentTimeMillis())) return;
      }
      if (closed || token != generation) return;
      if (QuranSyncGateway.configured(context)) {
        final Call[] syncing = new Call[1];
        try {
          String json = QuranSyncGateway.fetch(context, HTTP, chapter, page, call -> {
            syncing[0] = call; track(token, call);
          });
          if (closed || token != generation) return;
          save(new File(syncedCache, chapter.id + "-" + page + ".json"), json);
          // Do not bind a second time: the reader appends rows and may be playing audio.
          if (!displayed || removed) {
            QuranPage value = QuranPage.parse(json, chapter, page);
            deliver(token, () -> result.ready(value, "Quran Foundation · Content Sync"));
          }
          return;
        } catch (QuranSyncGateway.SourceRemoved unavailable) {
          deliver(token, () -> result.failed("Sumber bacaan sedang tidak tersedia. Data lama dipertahankan; coba lagi nanti."));
          return;
        } catch (Exception unavailable) {
          if (closed || token != generation) return;
        } finally { untrack(syncing[0]); }
      }
      if (removed) {
        deliver(token, () -> result.failed("Sumber bacaan sedang tidak tersedia. Data lama dipertahankan; coba lagi nanti."));
        return;
      }
      if (displayed) return;
      // Online-only compatibility path. Never create new offline copies from this API.
      Call call = HTTP.newCall(new Request.Builder().url("https://api.quran.com/api/v4/verses/by_chapter/"
          + chapter.id + "?language=id&translations=33&fields=text_uthmani&per_page=50&page=" + page)
          .header("Accept", "application/json").header("User-Agent", "Zikra-Android/1.0").build());
      track(token, call);
      if (closed || token != generation) { call.cancel(); return; }
      try (Response response = call.execute()) {
        if (!response.isSuccessful() || response.body() == null) throw new IOException("Quran source unavailable");
        String json = read(response.body().byteStream());
        QuranPage value = QuranPage.parse(json, chapter, page);
        deliver(token, () -> result.ready(value, "Quran.com · online, belum disimpan"));
      } catch (Exception unavailable) {
        if (!call.isCanceled()) deliver(token, () -> result.failed("Bacaan belum dapat dimuat. Periksa koneksi lalu coba lagi. Bacaan lama tetap dipertahankan."));
      } finally { untrack(call); }
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
      if (!syncedCache.exists() && !syncedCache.mkdirs()) return;
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
      File[] files = syncedCache.listFiles((dir, name) -> name.endsWith(".json"));
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
