package com.zikra.app.discovery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** A single explicit, bounded HTTPS download; mutable redirects are never trusted. */
final class HadeethEncTransfer {
  private static final OkHttpClient HTTP = new OkHttpClient.Builder()
      .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
      .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
      .callTimeout(120, TimeUnit.SECONDS).build();

  private HadeethEncTransfer() {}

  static void fetch(File target, BooleanSupplier stopped, Consumer<Call> capture,
      IntConsumer progress) throws Exception {
    Call call = HTTP.newCall(new Request.Builder().url(HadeethEncCsv.URL)
        .header("Accept-Encoding", "identity")
        .header("User-Agent", "Zikra-Android/1.0 (+https://github.com/Arkanzahir/Zikra-App)").build());
    capture.accept(call);
    if (stopped.getAsBoolean()) { call.cancel(); throw new IOException("cancelled"); }
    try (Response response = call.execute()) {
      if (!response.isSuccessful() || response.body() == null
          || response.body().contentLength() > HadeethEncCsv.BYTES) throw new IOException("network");
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      long bytes = 0; int previous = -1;
      try (InputStream input = response.body().byteStream(); FileOutputStream output = new FileOutputStream(target)) {
        byte[] buffer = new byte[8192]; int size;
        while ((size = input.read(buffer)) != -1) {
          if (stopped.getAsBoolean()) throw new IOException("cancelled");
          bytes += size;
          if (bytes > HadeethEncCsv.BYTES) throw new IOException("checksum");
          output.write(buffer, 0, size); digest.update(buffer, 0, size);
          int percent = (int) (bytes * 85 / HadeethEncCsv.BYTES);
          if (percent != previous) { progress.accept(percent); previous = percent; }
        }
        output.getFD().sync();
      }
      StringBuilder hash = new StringBuilder();
      for (byte value : digest.digest()) hash.append(String.format(Locale.ROOT, "%02x", value & 255));
      if (bytes != HadeethEncCsv.BYTES || !HadeethEncCsv.SHA256.equals(hash.toString()))
        throw new IOException("checksum");
    }
  }
}
