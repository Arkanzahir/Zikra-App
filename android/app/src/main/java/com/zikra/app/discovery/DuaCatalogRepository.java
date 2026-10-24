package com.zikra.app.discovery;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class DuaCatalogRepository implements AutoCloseable {
  private static final int LIMIT = 1024 * 1024;
  private final AtomicFile cache;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final OkHttpClient client = new OkHttpClient.Builder().followRedirects(false)
      .followSslRedirects(false).callTimeout(35, TimeUnit.SECONDS).build();
  private volatile boolean closed;
  private volatile Call call;
  public interface Result { void show(DuaCatalog catalog); void failed(); }
  public DuaCatalogRepository(Context context) {
    cache = new AtomicFile(new File(context.getFilesDir(), "equran-doa-catalog.json"));
  }
  private byte[] bounded(InputStream input) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int n;
    while ((n = input.read(buffer)) != -1) {
      if (closed || output.size() + n > LIMIT) throw new IllegalStateException("Invalid payload");
      output.write(buffer, 0, n);
    }
    return output.toByteArray();
  }
  private void post(Runnable action) { main.post(() -> { if (!closed) action.run(); }); }
  public void load(boolean refresh, Result result) {
    worker.execute(() -> {
      try {
        byte[] bytes;
        if (!refresh) {
          try (FileInputStream input = cache.openRead()) { bytes = bounded(input); }
        } else {
          Call request = client.newCall(new Request.Builder().url("https://equran.id/api/doa").build());
          call = request;
          if (closed) { request.cancel(); return; }
          try (Response response = request.execute()) {
            if (!response.isSuccessful() || response.body() == null
                || response.body().contentLength() > LIMIT) throw new IllegalStateException("Unavailable");
            try (InputStream input = response.body().byteStream()) { bytes = bounded(input); }
          }
        }
        DuaCatalog parsed = DuaCatalog.parse(new String(bytes, StandardCharsets.UTF_8));
        if (closed) return;
        if (refresh) {
          FileOutputStream output = null;
          try {
            output = cache.startWrite(); output.write(bytes);
            if (closed) throw new IllegalStateException("Closed");
            cache.finishWrite(output);
          } catch (Exception failure) { if (output != null) cache.failWrite(output); throw failure; }
        }
        post(() -> result.show(parsed));
      } catch (Exception failure) { post(result::failed); }
      finally { call = null; }
    });
  }
  public void remove(Runnable done) { worker.execute(() -> { cache.delete(); post(done); }); }
  @Override public void close() {
    closed = true; Call active = call; if (active != null) active.cancel(); worker.shutdown();
  }
}
