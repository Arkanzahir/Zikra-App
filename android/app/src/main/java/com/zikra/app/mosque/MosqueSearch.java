package com.zikra.app.mosque;

import android.content.Context;
import android.content.SharedPreferences;
import com.zikra.app.location.LocationPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Explicit, bounded OSM lookup with a short location-scoped cache and no credentials. */
public final class MosqueSearch implements AutoCloseable {
  public static final int RADIUS = 3000;
  private final OkHttpClient http =
      new OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build();
  private final Context context;
  private volatile int generation;
  private volatile boolean closed;

  public MosqueSearch(Context context) {
    this.context = context.getApplicationContext();
  }

  public static final class Result {
    public final List<MosquePlace> places;
    public final long fetchedAt;
    public final boolean cached;

    public Result(List<MosquePlace> places, long fetchedAt, boolean cached) {
      this.places = Collections.unmodifiableList(new ArrayList<>(places));
      this.fetchedAt = fetchedAt;
      this.cached = cached;
    }
  }

  public static final class RateLimited extends IOException {
    public final long retryAfterMs;

    private RateLimited(long retryAfterMs) {
      super("Places rate limited");
      this.retryAfterMs = retryAfterMs;
    }
  }

  public Result find(LocationPoint origin, boolean force) throws Exception {
    if (closed) throw new IOException("Places lookup closed");
    int token = generation;
    SharedPreferences cache = context.getSharedPreferences("zikra_mosques_osm", 0);
    String cacheKey = origin.key() + ":" + RADIUS;
    if (!force && cacheKey.equals(cache.getString("location", ""))) {
      long at = cache.getLong("fetched_at", 0), age = System.currentTimeMillis() - at;
      if (age >= 0 && age < 30 * 60_000L)
        try {
          return new Result(
              MosquePlace.parse(cache.getString("json", ""), "osm", origin, RADIUS), at, true);
        } catch (Exception invalid) {
          /* Request fresh data if the cache is malformed. */
        }
    }
    String around = "(around:" + RADIUS + "," + origin.latitude + "," + origin.longitude + ")";
    long blockedUntil = cache.getLong("retry_until", 0);
    if (blockedUntil > System.currentTimeMillis())
      throw new RateLimited(blockedUntil - System.currentTimeMillis());
    String query =
        "[out:json][timeout:15][maxsize:16777216];(nwr"
            + around
            + "[amenity=place_of_worship][religion=muslim];nwr"
            + around
            + "[building=mosque];);out center 100;";
    Exception last = null;
    for (String host : new String[] {"overpass.private.coffee", "overpass-api.de"}) {
      if (token != generation || Thread.currentThread().isInterrupted())
        throw new IOException("Places lookup cancelled");
      HttpUrl url =
          HttpUrl.parse("https://" + host + "/api/interpreter")
              .newBuilder()
              .addQueryParameter("data", query)
              .build();
      Request request =
          new Request.Builder()
              .url(url)
              .header("User-Agent", "Zikra-Android/1.0 (+https://github.com/Arkanzahir/Zikra-App)")
              .build();
      try (Response response = http.newCall(request).execute()) {
        if (response.code() == 429) {
          long seconds = 60;
          String retry = response.header("Retry-After", "60");
          try {
            seconds = Long.parseLong(retry);
          } catch (NumberFormatException ignored) {
            try {
              seconds =
                  java.time.Duration.between(
                          java.time.Instant.now(),
                          java.time.ZonedDateTime.parse(
                                  retry, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                              .toInstant())
                      .getSeconds();
            } catch (RuntimeException invalid) {
            }
          }
          long delay = Math.max(60, Math.min(31_536_000L, seconds)) * 1000;
          cache.edit().putLong("retry_until", System.currentTimeMillis() + delay).apply();
          throw new RateLimited(delay);
        }
        if (!response.isSuccessful() || response.body() == null) {
          throw new IOException("Places HTTP " + response.code());
        }
        response.body().source().request(1048577);
        if (response.body().source().buffer().size() > 1048576)
          throw new IOException("Places response too large");
        String json = response.body().string();
        List<MosquePlace> result = MosquePlace.parse(json, "osm", origin, RADIUS);
        long at = System.currentTimeMillis();
        cache
            .edit()
            .putString("json", json)
            .putString("location", cacheKey)
            .putLong("fetched_at", at)
            .apply();
        return new Result(result, at, false);
      } catch (RateLimited limited) {
        throw limited;
      } catch (Exception unavailable) {
        last = unavailable;
        if (token != generation || Thread.currentThread().isInterrupted()) throw unavailable;
      }
    }
    throw last == null ? new IOException("Places unavailable") : last;
  }

  public void cancel() {
    generation++;
    http.dispatcher().cancelAll();
  }

  @Override
  public void close() {
    closed = true;
    cancel();
  }
}
