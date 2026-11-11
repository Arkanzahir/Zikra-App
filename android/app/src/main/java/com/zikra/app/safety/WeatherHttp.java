package com.zikra.app.safety;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

/** Coordinate-bearing forecast requests stay on the explicitly requested endpoint. */
final class WeatherHttp {
  private WeatherHttp() {}

  static OkHttpClient create() {
    return new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build();
  }
}
