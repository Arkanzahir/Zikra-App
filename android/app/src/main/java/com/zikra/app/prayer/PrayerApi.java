package com.zikra.app.prayer;

import java.io.IOException;
import java.time.YearMonth;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Public city IDs and months only; no coordinates, account data or provider credentials. */
public final class PrayerApi implements AutoCloseable {
  private final OkHttpClient http = new OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
      .callTimeout(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build();

  public String month(PrayerRegion region, YearMonth month) throws IOException {
    String url = "https://api.myquran.com/v3/sholat/jadwal/" + region.id + "/" + month;
    try (Response response = http.newCall(new Request.Builder().url(url)
        .header("Accept", "application/json").header("User-Agent", "Zikra-Android/1.0").build()).execute()) {
      if (!response.isSuccessful() || response.body() == null) throw new IOException("Timetable unavailable");
      response.body().source().request(131073);
      if (response.body().source().buffer().size() > 131072) throw new IOException("Timetable too large");
      return response.body().string();
    }
  }

  @Override public void close() { http.dispatcher().cancelAll(); }
}
