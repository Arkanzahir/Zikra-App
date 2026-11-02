package com.zikra.app.safety;

import android.app.Activity;
import android.graphics.*;
import android.view.View;
import android.widget.*;
import com.zikra.app.R;
import java.io.*;
import java.util.concurrent.*;
import okhttp3.*;

/** Bounded, cancellable official BMKG image request; never draws a fabricated shaking overlay. */
public final class ShakemapLoader implements AutoCloseable {
  private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
  private static final OkHttpClient HTTP =
      new OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
  private final Activity owner;
  private final OkHttpClient http;
  private final ImageView image;
  private final TextView status;
  private volatile int generation;
  private volatile Call call;
  private boolean closed;
  private String current = "", cached = "";

  public ShakemapLoader(Activity owner, ImageView image, TextView status) {
    this(owner, image, status, HTTP);
  }

  ShakemapLoader(Activity owner, ImageView image, TextView status, OkHttpClient http) {
    this.owner = owner;
    this.http = http;
    this.image = image;
    this.status = status;
    status.setOnClickListener(v -> load(current));
  }

  public void load(String url) {
    if (closed) return;
    current = url;
    pause();
    if (!url.isEmpty() && url.equals(cached)) {
      status.setVisibility(View.GONE);
      return;
    }
    cached = "";
    image.setImageDrawable(null);
    if (url.isEmpty()) {
      image.setImageDrawable(null);
      status.setText(R.string.protector_shakemap_missing);
      status.setVisibility(View.VISIBLE);
      return;
    }
    // Only the parser-generated fixed HTTPS origin and allowlisted filename are accepted.
    String prefix = "https://static.bmkg.go.id/";
    if (!url.startsWith(prefix)
        || !url.equals(BmkgParser.shakemapUrl(url.substring(prefix.length())))) {
      status.setText(R.string.protector_shakemap_missing);
      status.setVisibility(View.VISIBLE);
      return;
    }
    int token = generation;
    image.setImageDrawable(null);
    status.setText(R.string.protector_shakemap_loading);
    status.setVisibility(View.VISIBLE);
    Call request =
        http.newCall(
            new Request.Builder().url(url).header("User-Agent", "Zikra-Android/1.5").build());
    call = request;
    WORK.execute(
        () -> {
          Bitmap bitmap = null;
          try {
            bitmap = fetchImage(request, url, token);
          } catch (Exception error) {
          }
          final Bitmap result = bitmap;
          owner.runOnUiThread(
              () -> {
                if (closed || generation != token || owner.isDestroyed() || owner.isFinishing()) {
                  if (result != null) result.recycle();
                  return;
                }
                call = null;
                if (result == null) {
                  status.setText(R.string.protector_shakemap_failed);
                  status.setVisibility(View.VISIBLE);
                } else {
                  cached = url;
                  image.setImageBitmap(result);
                  status.setVisibility(View.GONE);
                }
              });
        });
  }

  private Bitmap fetchImage(Call request, String url, int token) throws IOException {
    String target;
    try (Response first = request.execute()) {
      if (first.code() != 301 && first.code() != 302 && first.code() != 307 && first.code() != 308)
        return decode(first);
      target = BmkgParser.shakemapRedirect(url, first.header("Location"));
      if (target.isEmpty()) throw new IOException("Unapproved image redirect");
    }
    Call redirected = http.newCall(new Request.Builder().url(target)
        .header("User-Agent", "Zikra-Android/1.9").build());
    call = redirected;
    if (closed || generation != token) { redirected.cancel(); throw new IOException("Cancelled"); }
    try (Response response = redirected.execute()) { return decode(response); }
  }

  private static Bitmap decode(Response response) throws IOException {
    if (!response.isSuccessful() || response.body() == null) throw new IOException();
    if (response.body().contentLength() > 4194304) throw new IOException();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int count;
    try (InputStream in = response.body().byteStream()) {
      while ((count = in.read(chunk)) != -1) {
        if (buffer.size() + count > 4194304) throw new IOException();
        buffer.write(chunk, 0, count);
      }
    }
    byte[] bytes = buffer.toByteArray();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    if (options.outWidth <= 0
        || options.outHeight <= 0
        || options.outWidth > 8192
        || options.outHeight > 8192) throw new IOException();
    options.inJustDecodeBounds = false;
    options.inSampleSize = 1;
    while (options.outWidth / options.inSampleSize > 1200
        || options.outHeight / options.inSampleSize > 1200) options.inSampleSize *= 2;
    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    if (bitmap == null) throw new IOException();

    return bitmap;
  }

  public void pause() {
    generation++;
    Call active = call;
    if (active != null) active.cancel();
    call = null;
  }

  public void clear() {
    pause();
    cached = "";
    image.setImageDrawable(null);
  }

  @Override
  public void close() {
    closed = true;
    clear();
  }
}
