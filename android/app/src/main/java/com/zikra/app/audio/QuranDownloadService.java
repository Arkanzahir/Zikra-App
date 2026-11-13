package com.zikra.app.audio;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
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
  static final String CHANNEL = "quran_downloads";
  static final int ID = 7500;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final OkHttpClient http =
      new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(55, TimeUnit.SECONDS)
          .callTimeout(60, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
          .retryOnConnectionFailure(false).build();
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
    clearLegacyDownload(getSystemService(NotificationManager.class));
    getSystemService(NotificationManager.class)
        .createNotificationChannel(
            new NotificationChannel(CHANNEL, "Unduhan Quran", NotificationManager.IMPORTANCE_LOW));
  }

  static void clearLegacyDownload(NotificationManager manager) {
    for (android.service.notification.StatusBarNotification active :
        manager.getActiveNotifications())
      if (active.getId() == 7302
          && active.getTag() == null
          && CHANNEL.equals(active.getNotification().getChannelId())) manager.cancel(7302);
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
              AudioTransfer.ensure(this, http, qari, chapter, verse, this::checkCancelled);
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
    if (!QuranSyncGateway.configured(this) && QuranPage.builtIn(chapter) != null) return;
    QuranChapters.Chapter info = QuranChapters.all(this).get(chapter - 1);
    QuranSyncStore store = new QuranSyncStore(this);
    boolean current = !QuranSyncGateway.sourceRemoved(this);
    for (int page = 1; page <= (info.count + 49) / 50; page++) {
      try {
        String json = store.read(info, page);
        long checked = QuranSyncPage.validate(json, info, page, System.currentTimeMillis());
        if (!QuranSyncPage.fresh(checked, System.currentTimeMillis())) current = false;
      } catch (Exception missing) { current = false; }
    }
    if (current) return;
    store.download(info, page -> {
      checkCancelled();
      return QuranSyncGateway.fetch(this, http, info, page, call -> {
        if (cancelled || destroyed) call.cancel();
      });
    }, this::checkCancelled);
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
