package com.zikra.app.location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit city search with IANA zones; bundled city choices remain usable offline. */
public final class CitySearch implements AutoCloseable {
  private final OkHttpClient http =
      new OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build();

  public List<LocationPoint> search(String query) throws Exception {
    String name = query.trim();
    if (name.length() < 2 || name.length() > 100) throw new IllegalArgumentException("City query");
    HttpUrl url =
        HttpUrl.parse("https://geocoding-api.open-meteo.com/v1/search")
            .newBuilder()
            .addQueryParameter("name", name)
            .addQueryParameter("count", "12")
            .addQueryParameter("language", "id")
            .addQueryParameter("format", "json")
            .build();
    JSONObject root = new JSONObject(read(url));
    List<LocationPoint> results = new ArrayList<>();
    JSONArray rows = root.optJSONArray("results");
    if (rows == null) return results;
    for (int i = 0; i < Math.min(12, rows.length()); i++) {
      JSONObject row = rows.getJSONObject(i);
      try {
        String label = row.getString("name");
        String admin = row.optString("admin1", ""), country = row.optString("country", "");
        if (!admin.isEmpty() && !admin.equals(label)) label += ", " + admin;
        if (!country.isEmpty()) label += ", " + country;
        results.add(
            new LocationPoint(
                row.getDouble("latitude"),
                row.getDouble("longitude"),
                row.getString("timezone"),
                label,
                "manual",
                System.currentTimeMillis(),
                -1));
      } catch (Exception invalid) {
        /* Skip a malformed city rather than invent its zone. */
      }
    }
    return results;
  }

  public LocationPoint device(android.location.Location fix) throws Exception {
    HttpUrl url =
        HttpUrl.parse("https://api.open-meteo.com/v1/forecast")
            .newBuilder()
            .addQueryParameter("latitude", Double.toString(fix.getLatitude()))
            .addQueryParameter("longitude", Double.toString(fix.getLongitude()))
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("forecast_days", "1")
            .build();
    JSONObject root = new JSONObject(read(url));
    if (Math.abs(root.getDouble("latitude") - fix.getLatitude()) > .15
        || Math.abs(root.getDouble("longitude") - fix.getLongitude()) > .15)
      throw new IOException("Timezone location mismatch");
    return new LocationPoint(
        fix.getLatitude(),
        fix.getLongitude(),
        root.getString("timezone"),
        "Lokasi perangkat",
        "device",
        System.currentTimeMillis(),
        fix.hasAccuracy() ? fix.getAccuracy() : -1);
  }

  private String read(HttpUrl url) throws Exception {
    try (Response r =
        http.newCall(
                new Request.Builder().url(url).header("User-Agent", "Zikra-Android/1.0").build())
            .execute()) {
      if (!r.isSuccessful() || r.body() == null)
        throw new IOException("Location service unavailable");
      r.body().source().request(262145);
      if (r.body().source().buffer().size() > 262144)
        throw new IOException("Location response too large");
      return r.body().string();
    }
  }

  public void cancel() {
    http.dispatcher().cancelAll();
  }

  @Override
  public void close() {
    cancel();
  }

  public static List<LocationPoint> bundled(String query) {
    String[][] cities = {
      {"Jakarta", "-6.1754", "106.8272", "Asia/Jakarta"},
      {"Bandung", "-6.9175", "107.6191", "Asia/Jakarta"},
      {"Surabaya", "-7.2575", "112.7521", "Asia/Jakarta"},
      {"Yogyakarta", "-7.7956", "110.3695", "Asia/Jakarta"},
      {"Semarang", "-6.9667", "110.4167", "Asia/Jakarta"},
      {"Medan", "3.5952", "98.6722", "Asia/Jakarta"},
      {"Padang", "-0.9471", "100.4172", "Asia/Jakarta"},
      {"Banda Aceh", "5.5483", "95.3238", "Asia/Jakarta"},
      {"Pontianak", "-0.0263", "109.3425", "Asia/Pontianak"},
      {"Makassar", "-5.1477", "119.4327", "Asia/Makassar"},
      {"Denpasar", "-8.6500", "115.2167", "Asia/Makassar"},
      {"Mataram", "-8.5833", "116.1167", "Asia/Makassar"},
      {"Balikpapan", "-1.2379", "116.8529", "Asia/Makassar"},
      {"Jayapura", "-2.5337", "140.7181", "Asia/Jayapura"},
      {"Ambon", "-3.6954", "128.1814", "Asia/Jayapura"}
    };
    List<LocationPoint> result = new ArrayList<>();
    for (String[] c : cities)
      if (c[0].toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)))
        result.add(
            new LocationPoint(
                Double.parseDouble(c[1]),
                Double.parseDouble(c[2]),
                c[3],
                c[0] + ", Indonesia",
                "manual",
                System.currentTimeMillis(),
                -1));
    return result;
  }
}
