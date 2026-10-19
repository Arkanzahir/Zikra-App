package com.zikra.app.audio;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.MediaMetadataRetriever;
import android.os.*;
import android.util.AtomicFile;
import com.zikra.app.QuranActivity;
import com.zikra.app.R;
import com.zikra.app.reading.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import okhttp3.*;

/** User-started, sequential and resumable surah download. Only verified files become playable. */
public final class QuranDownloadService extends Service {
  public static final String PREFS = "quran_audio_download";
  public static volatile boolean running;
  private static final String CHANNEL = "quran_downloads";
  private static final int ID = 7302;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final OkHttpClient http =
      new OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build();
  private volatile boolean cancelled, destroyed;
  private String qari;
  private int chapter;

  @Override
  public IBinder onBind(Intent i) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    getSystemService(NotificationManager.class)
        .createNotificationChannel(
            new NotificationChannel(CHANNEL, "Unduhan Quran", NotificationManager.IMPORTANCE_LOW));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Notification notification = notification("Menyiapkan unduhan…", 0, 0, true);
    if (Build.VERSION.SDK_INT >= 29)
      startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
    else startForeground(ID, notification);
    if (intent == null || "cancel".equals(intent.getAction())) {
      cancelled = true;
      http.dispatcher().cancelAll();
      if (!running) {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
      }
      return START_NOT_STICKY;
    }
    if (running) return START_NOT_STICKY;
    try {
      chapter = intent.getIntExtra("chapter", 0);
      Recitation.count(chapter);
      qari = intent.getStringExtra("reciter");
      Recitation.reciterIndex(qari);
    } catch (IllegalArgumentException invalid) {
      stopForeground(STOP_FOREGROUND_REMOVE);
      stopSelf();
      return START_NOT_STICKY;
    }
    cancelled = false;
    running = true;
    worker.execute(
        () -> {
          String result;
          try {
            update("Mengunduh teks surah…", 0);
            textPages();
            for (int verse = 1; verse <= Recitation.count(chapter); verse++) {
              checkCancelled();
              if (!AudioFiles.available(this, qari, chapter, verse)) download(verse);
              update(
                  "Mengunduh "
                      + QuranChapters.all(this).get(chapter - 1).name
                      + " · "
                      + Recitation.NAMES[Recitation.reciterIndex(qari)]
                      + " · "
                      + verse
                      + "/"
                      + Recitation.count(chapter)
                      + " ayat",
                  verse);
            }
            result = "Selesai · teks dan audio surah tersedia offline";
          } catch (Exception e) {
            result =
                cancelled
                    ? "Unduhan dijeda. Ketuk Unduh untuk melanjutkan."
                    : "Unduhan terhenti. Periksa koneksi atau ruang penyimpanan, lalu lanjutkan.";
          }
          final String message = result;
          getSharedPreferences(PREFS, 0)
              .edit()
              .putString("status", message)
              .putBoolean("busy", false)
              .commit();
          main.post(
              () -> {
                if (destroyed) return;
                running = false;
                getSystemService(NotificationManager.class)
                    .notify(ID, notification(message, 0, 0, false));
                stopForeground(STOP_FOREGROUND_DETACH);
                stopSelf();
              });
        });
    return START_NOT_STICKY;
  }

  private void update(String status, int count) {
    getSharedPreferences(PREFS, 0)
        .edit()
        .putInt("chapter", chapter)
        .putString("reciter", qari)
        .putString("status", status)
        .putBoolean("busy", true)
        .putInt("done", count)
        .commit();
    main.post(
        () -> {
          if (!destroyed)
            getSystemService(NotificationManager.class)
                .notify(ID, notification(status, count, Recitation.count(chapter), true));
        });
  }

  private void checkCancelled() throws IOException {
    if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
    if (getFilesDir().getUsableSpace() < 20L * 1024 * 1024) throw new IOException("Storage low");
  }

  private void textPages() throws Exception {
    if (QuranPage.builtIn(chapter) != null) return;
    QuranChapters.Chapter info = QuranChapters.all(this).get(chapter - 1);
    File folder = new File(getFilesDir(), "quran-offline-r33");
    folder.mkdirs();
    for (int page = 1; page <= (info.count + 49) / 50; page++) {
      checkCancelled();
      AtomicFile target = new AtomicFile(new File(folder, chapter + "-" + page + ".json"));
      try (InputStream in = target.openRead()) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
          if (buffer.size() + n > 524288) throw new IOException("Text too large");
          buffer.write(chunk, 0, n);
        }
        byte[] bytes = buffer.toByteArray();
        QuranPage.parse(new String(bytes, StandardCharsets.UTF_8), info, page);
        continue;
      } catch (Exception missing) {
        /* Fetch and validate a missing/incomplete page. */
      }
      Request request =
          new Request.Builder()
              .url(
                  "https://api.quran.com/api/v4/verses/by_chapter/"
                      + chapter
                      + "?language=id&translations=33&fields=text_uthmani&per_page=50&page="
                      + page)
              .build();
      try (Response response = http.newCall(request).execute()) {
        if (!response.isSuccessful() || response.body() == null) throw new IOException("Text HTTP");
        response.body().source().request(524289);
        if (response.body().source().buffer().size() > 524288)
          throw new IOException("Text too large");
        byte[] bytes = response.body().bytes();
        QuranPage.parse(new String(bytes, StandardCharsets.UTF_8), info, page);
        FileOutputStream out = null;
        try {
          out = target.startWrite();
          out.write(bytes);
          target.finishWrite(out);
        } catch (Exception failure) {
          if (out != null) target.failWrite(out);
          throw failure;
        }
      }
    }
  }

  private void download(int verse) throws Exception {
    File target = AudioFiles.file(this, qari, chapter, verse);
    target.getParentFile().mkdirs();
    File partial = new File(target.getPath() + ".part");
    try {
      Request request =
          new Request.Builder()
              .url(Recitation.url(qari, chapter, verse))
              .header("User-Agent", "Zikra-Android/1.0 (+https://github.com/Arkanzahir/Zikra-App)")
              .build();
      try (Response response = http.newCall(request).execute()) {
        if (!response.isSuccessful() || response.body() == null)
          throw new IOException("Audio HTTP");
        try (InputStream in = response.body().byteStream();
            FileOutputStream out = new FileOutputStream(partial)) {
          byte[] bytes = new byte[16384];
          int n;
          long size = 0;
          while ((n = in.read(bytes)) != -1) {
            checkCancelled();
            size += n;
            if (size > 20L * 1024 * 1024) throw new IOException("Audio too large");
            out.write(bytes, 0, n);
          }
          out.getFD().sync();
          if (size < 129
              || (response.body().contentLength() >= 0 && size != response.body().contentLength()))
            throw new IOException("Incomplete audio");
        }
      }
      MediaMetadataRetriever metadata = new MediaMetadataRetriever();
      try {
        metadata.setDataSource(partial.getAbsolutePath());
        String mime = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
        String duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (mime == null
            || !mime.startsWith("audio/")
            || duration == null
            || Long.parseLong(duration) < 1) throw new IOException("Invalid audio");
      } finally {
        metadata.release();
      }
      checkCancelled();
      if (!partial.renameTo(target)) throw new IOException("Cannot save audio");
    } finally {
      if (partial.exists()) partial.delete();
    }
  }

  private Notification notification(String text, int done, int total, boolean active) {
    Intent open = new Intent(this, QuranActivity.class);
    if (chapter > 0) open.putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, chapter);
    Notification.Builder builder =
        new Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.audio_download)
            .setContentTitle("Unduhan Quran")
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    ID,
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    if (active)
      builder
          .setProgress(total, done, total == 0)
          .addAction(
              R.drawable.audio_stop,
              "Jeda unduhan",
              PendingIntent.getForegroundService(
                  this,
                  ID,
                  new Intent(this, QuranDownloadService.class).setAction("cancel"),
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    return builder.build();
  }

  @Override
  public void onTimeout(int startId, int type) {
    cancelled = true;
    http.dispatcher().cancelAll();
    stopForeground(STOP_FOREGROUND_REMOVE);
    stopSelf();
  }

  @Override
  public void onDestroy() {
    destroyed = true;
    cancelled = true;
    running = false;
    main.removeCallbacksAndMessages(null);
    http.dispatcher().cancelAll();
    worker.shutdownNow();
    super.onDestroy();
  }
}
