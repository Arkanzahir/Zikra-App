package com.zikra.app.safety;

import android.app.Activity;
import android.content.SharedPreferences;
import java.io.*;
import java.time.*;
import java.util.concurrent.*;
import okhttp3.*;

/** Validated hourly model forecast; ten-minute refresh and six-hour labelled failure fallback. */
public final class OpenMeteoClient implements AutoCloseable {
  public static okhttp3.HttpUrl url(com.zikra.app.location.LocationPoint point) {
    return okhttp3.HttpUrl.parse("https://api.open-meteo.com/v1/forecast")
        .newBuilder()
        .addQueryParameter("latitude", Double.toString(point.latitude))
        .addQueryParameter("longitude", Double.toString(point.longitude))
        .addQueryParameter(
            "hourly",
            "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m")
        .addQueryParameter("timezone", point.zone.getId())
        .addQueryParameter("timeformat", "unixtime")
        .addQueryParameter("forecast_days", "3")
        .addQueryParameter("wind_speed_unit", "kmh")
        .build();
  }

  public static final long FRESH_MS = 10 * 60 * 1000L, MAX_AGE_MS = 6 * 60 * 60 * 1000L;
  private static final ExecutorService WORK = Executors.newFixedThreadPool(2);
  private static final OkHttpClient HTTP =
      new OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build();
  private final Activity owner;
  private final SharedPreferences cache;
  private volatile int generation;
  private volatile boolean closed;
  private volatile Call active;

  public OpenMeteoClient(Activity activity) {
    owner = activity;
    cache = owner.getSharedPreferences("zikra_weather_openmeteo_v2", 0);
  }

  public void reset() {
    generation++;
    Call c = active;
    if (c != null) c.cancel();
  }

  public void load(BmkgClient.Result<BmkgParser.Forecast> result) {
    if (closed) return;
    reset();
    int token = generation;
    com.zikra.app.location.LocationPoint point =
        com.zikra.app.location.LocationStore.current(owner);
    String prefix = point.key() + ":";
    WORK.execute(
        () -> {
          String cached = cache.getString(prefix + "json", "");
          long at = cache.getLong(prefix + "fetched_at", 0);
          long now = System.currentTimeMillis();
          BmkgParser.Forecast previous = null;
          if (WeatherCode.usableCache(at, now, MAX_AGE_MS))
            try {
              previous =
                  OpenMeteoParser.parse(
                      cached,
                      Instant.ofEpochMilli(now),
                      Instant.ofEpochMilli(at),
                      true,
                      now - at > FRESH_MS,
                      point);
            } catch (Exception ignored) {
            }
          if (previous != null) {
            BmkgParser.Forecast value = previous;
            deliver(token, () -> result.success(value));
            if (WeatherCode.usableCache(at, now, FRESH_MS)) return;
          }
          BmkgParser.Forecast fallback = previous;
          Call call =
              HTTP.newCall(
                  new Request.Builder()
                      .url(url(point))
                      .header("Accept", "application/json")
                      .header("User-Agent", "Zikra-Android/1.0")
                      .build());
          active = call;
          if (closed || generation != token) {
            call.cancel();
            return;
          }
          try (Response response = call.execute()) {
            if (!response.isSuccessful() || response.body() == null)
              throw new IOException("Weather HTTP " + response.code());
            response.body().source().request(262145);
            if (response.body().source().buffer().size() > 262144)
              throw new IOException("Weather response too large");
            String json = response.body().string();
            Instant fetched = Instant.now();
            BmkgParser.Forecast value =
                OpenMeteoParser.parse(json, fetched, fetched, false, false, point);
            if (closed || generation != token) return;
            synchronized (OpenMeteoClient.class) {
              cache
                  .edit()
                  .putString(prefix + "json", json)
                  .putLong(prefix + "fetched_at", fetched.toEpochMilli())
                  .apply();
              java.util.List<String> keys = new java.util.ArrayList<>();
              for (String key : cache.getAll().keySet())
                if (key.endsWith(":fetched_at")) keys.add(key);
              keys.sort(java.util.Comparator.comparingLong(k -> cache.getLong(k, 0)));
              for (int i = 0; i < keys.size() - 4; i++) {
                String old = keys.get(i);
                cache
                    .edit()
                    .remove(old)
                    .remove(old.substring(0, old.length() - "fetched_at".length()) + "json")
                    .apply();
              }
            }
            deliver(token, () -> result.success(value));
          } catch (Exception error) {
            if (call.isCanceled()) return;
            if (fallback == null)
              deliver(
                  token,
                  () ->
                      result.failure(
                          "Cuaca belum tersedia. Periksa koneksi lalu muat ulang. Cache lebih dari"
                              + " enam jam tidak ditampilkan."));
            else
              deliver(
                  token,
                  () -> result.failure("Pembaruan cuaca gagal. Data tersimpan tetap tersedia."));
          } finally {
            if (active == call) active = null;
          }
        });
  }

  private void deliver(int token, Runnable action) {
    owner.runOnUiThread(
        () -> {
          if (!closed && generation == token && !owner.isFinishing() && !owner.isDestroyed())
            action.run();
        });
  }

  public static String status(BmkgParser.Forecast f) {
    String time =
        java.time.format.DateTimeFormatter.ofPattern(
                "d MMM HH:mm", new java.util.Locale("id", "ID"))
            .withZone(f.location.zone)
            .format(f.fetchedAt);
    return "Open-Meteo · "
        + (f.stored ? (f.stale ? "tersimpan · perlu diperbarui" : "tersimpan") : "prakiraan model")
        + "\nDiambil "
        + time
        + " "
        + f.location.zoneLabel(f.fetchedAt)
        + " · "
        + f.location.label;
  }

  @Override
  public void close() {
    closed = true;
    reset();
  }
}
