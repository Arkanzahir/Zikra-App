package com.zikra.app.safety;

import android.app.Activity;
import android.content.SharedPreferences;
import com.zikra.app.PrayerSchedule;
import java.io.*;
import java.time.*;
import java.util.concurrent.*;
import okhttp3.*;

/** Validated hourly model forecast; ten-minute refresh and six-hour labelled failure fallback. */
public final class OpenMeteoClient implements AutoCloseable {
  public static final String URL =
      "https://api.open-meteo.com/v1/forecast?latitude="
          + PrayerSchedule.LATITUDE
          + "&longitude="
          + PrayerSchedule.LONGITUDE
          + "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m&timezone=Asia%2FJakarta&forecast_days=3&wind_speed_unit=kmh";
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
    cache = owner.getSharedPreferences("zikra_weather_openmeteo_v1", 0);
  }

  public void reset() {
    generation++;
    Call c = active;
    if (c != null) c.cancel();
  }

  public void load(BmkgClient.Result<BmkgParser.Forecast> result) {
    reset();
    int token = generation;
    WORK.execute(
        () -> {
          String cached = cache.getString("json", "");
          long at = cache.getLong("fetched_at", 0);
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
                      now - at > FRESH_MS);
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
                      .url(URL)
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
            BmkgParser.Forecast value = OpenMeteoParser.parse(json, fetched, fetched, false, false);
            if (closed || generation != token) return;
            cache
                .edit()
                .putString("json", json)
                .putLong("fetched_at", fetched.toEpochMilli())
                .apply();
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
            // A valid older forecast was already delivered with its saved timestamp and stale
            // label.
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
                "d MMM HH:mm 'WIB'", new java.util.Locale("id", "ID"))
            .withZone(PrayerSchedule.ZONE)
            .format(f.fetchedAt);
    return "Open-Meteo · "
        + (f.stored
            ? (f.stale ? "tersimpan; pembaruan belum tersedia" : "tersimpan")
            : "prakiraan model")
        + "\nDiambil "
        + time;
  }

  @Override
  public void close() {
    closed = true;
    reset();
  }
}
