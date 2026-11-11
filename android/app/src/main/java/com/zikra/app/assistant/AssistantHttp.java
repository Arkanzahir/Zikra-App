package com.zikra.app.assistant;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

final class AssistantHttp {
  private AssistantHttp() {}

  static OkHttpClient create() {
    // The gateway can spend 35 seconds generating, plus bounded access-store operations.
    return new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build();
  }
}
