package com.zikra.app.safety;

import android.app.Activity;
import android.content.SharedPreferences;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

/** Fixed BMKG origin, one bounded atomic snapshot, one-minute reuse and labelled offline fallback. */
public final class EarthquakeClient implements AutoCloseable {
  private static final ExecutorService WORK = Executors.newFixedThreadPool(2);
  private final Activity owner;
  private final SharedPreferences cache;
  private final OkHttpClient http;
  private volatile int generation;
  private volatile boolean closed;
  private volatile Call active;

  public EarthquakeClient(Activity owner) {
    this(owner, new OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build());
  }

  EarthquakeClient(Activity owner, OkHttpClient http) {
    this.owner = owner;
    this.http = http;
    cache = owner.getSharedPreferences("bmkg_earthquake_v1", 0);
  }

  public void reset() {
    generation++;
    Call call = active;
    if (call != null) call.cancel();
  }

  public void load(BmkgClient.Result<EarthquakeReport> result) {
    if (closed) return;
    reset();
    int token = generation;
    WORK.execute(() -> {
      EarthquakeReport previous = null;
      try { previous = EarthquakeReport.cached(cache.getString("snapshot", ""), Instant.now()); }
      catch (Exception invalid) { }
      if (previous != null) {
        EarthquakeReport value = previous;
        deliver(token, () -> result.success(value));
        if (previous.fresh(Instant.now())) return;
      }
      if (closed || token != generation) return;
      Call call = http.newCall(new Request.Builder().url(BmkgClient.QUAKE_URL)
          .header("Accept", "application/json").header("User-Agent", "Zikra-Android/1.9").build());
      active = call;
      if (closed || token != generation) { call.cancel(); return; }
      try (Response response = call.execute()) {
        if (!response.isSuccessful() || response.body() == null) throw new IOException("BMKG unavailable");
        EarthquakeReport report = EarthquakeReport.network(BmkgClient.readBounded(response.body()), Instant.now());
        if (closed || token != generation) return;
        EarthquakeReport chosen;
        synchronized (EarthquakeClient.class) {
          EarthquakeReport saved = null;
          try { saved = EarthquakeReport.cached(cache.getString("snapshot", ""), Instant.now()); }
          catch (Exception invalid) { }
          chosen = EarthquakeReport.newest(saved, report);
          if (chosen == report) cache.edit().putString("snapshot", report.snapshot()).apply();
        }
        EarthquakeReport value = chosen;
        deliver(token, () -> result.success(value));
      } catch (Exception unavailable) {
        if (!call.isCanceled()) deliver(token, () -> result.failure(
            "BMKG belum dapat diperbarui. Periksa internet, lalu muat ulang. Cache maksimal24jam."));
      } finally {
        if (active == call) active = null;
      }
    });
  }

  private void deliver(int token, Runnable action) {
    owner.runOnUiThread(() -> {
      if (!closed && token == generation && !owner.isDestroyed() && !owner.isFinishing()) action.run();
    });
  }

  @Override public void close() {
    closed = true; reset();
    WORK.execute(() -> http.connectionPool().evictAll());
  }
}
