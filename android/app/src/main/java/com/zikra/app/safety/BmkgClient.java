package com.zikra.app.safety;

import android.app.Activity;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;

/** Each screen owns and cancels its calls. Network and JSON parsing never run on the UI thread. */
public final class BmkgClient implements AutoCloseable {
  public static final String QUAKE_URL = "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json";
  public static final String WEATHER_URL =
      "https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4=" + BmkgParser.KEPUTIH_CODE;
  private final Activity activity;
  private final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(12, TimeUnit.SECONDS)
          .readTimeout(18, TimeUnit.SECONDS)
          .callTimeout(25, TimeUnit.SECONDS)
          .build();
  private volatile boolean closed;
  private int generation;

  public interface Result<T> {
    void success(T value);

    void failure(String message);
  }

  private interface Parser<T> {
    T parse(String json) throws JSONException;
  }

  public BmkgClient(Activity activity) {
    this.activity = activity;
  }

  public void reset() {
    generation++;
    client.dispatcher().cancelAll();
  }

  public void weather(Result<BmkgParser.Forecast> result) {
    fetch(WEATHER_URL, json -> BmkgParser.forecast(json, Instant.now()), result);
  }

  public void earthquake(Result<BmkgParser.Earthquake> result) {
    fetch(QUAKE_URL, BmkgParser::earthquake, result);
  }

  private <T> void fetch(String url, Parser<T> parser, Result<T> result) {
    final int requestGeneration = generation;
    Request request =
        new Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Zikra-Android/1.0")
            .build();
    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException error) {
                if (!call.isCanceled())
                  post(
                      requestGeneration,
                      () ->
                          result.failure(
                              "Koneksi ke BMKG belum tersedia. Periksa internet, lalu muat"
                                  + " ulang."));
              }

              @Override
              public void onResponse(Call call, Response response) {
                try (Response source = response) {
                  ResponseBody body = source.body();
                  if (!source.isSuccessful() || body == null) {
                    int code = source.code();
                    post(
                        requestGeneration,
                        () ->
                            result.failure(
                                "BMKG belum dapat diakses (HTTP " + code + "). Coba lagi nanti."));
                    return;
                  }
                  T value = parser.parse(body.string());
                  post(requestGeneration, () -> result.success(value));
                } catch (IOException | JSONException error) {
                  post(
                      requestGeneration,
                      () -> result.failure("Data BMKG belum dapat dibaca. Silakan muat ulang."));
                }
              }
            });
  }

  private void post(int requestGeneration, Runnable action) {
    if (closed) return;
    activity.runOnUiThread(
        () -> {
          if (!closed
              && !activity.isFinishing()
              && !activity.isDestroyed()
              && requestGeneration == generation) action.run();
        });
  }

  @Override
  public void close() {
    closed = true;
    generation++;
    client.dispatcher().cancelAll();
    client.connectionPool().evictAll();
  }
}
