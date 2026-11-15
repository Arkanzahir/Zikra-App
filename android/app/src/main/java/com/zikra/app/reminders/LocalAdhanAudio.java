package com.zikra.app.reminders;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import com.zikra.app.PrayerSchedule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Explicitly selected local audio; never bundled, uploaded or resolved as a filesystem URI. */
public final class LocalAdhanAudio {
  public static final long MAX_BYTES = 10_000_000;
  private LocalAdhanAudio() {}

  static File file(Context context, int index) {
    if (!PrayerSchedule.isPrayer(index)) throw new IllegalArgumentException("Invalid prayer");
    return new File(context.getFilesDir(), "local-adhan/" + (index == 0 ? "fajr.mp3" : "normal.mp3"));
  }

  public static boolean available(Context context, int index) {
    if (!PrayerSchedule.isPrayer(index)) return false;
    File audio = file(context, index);
    return audio.isFile() && audio.length() > 0 && audio.length() <= MAX_BYTES;
  }

  public static void remove(Context context, int index) throws IOException {
    Files.deleteIfExists(file(context, index).toPath());
  }

  /** Call on a worker. Invalid/cancelled input leaves the previous recording untouched. */
  public static void importAudio(Context context, int index, Uri uri, CancellationSignal cancel)
      throws IOException {
    File target = file(context, index);
    if (uri == null || !"content".equals(uri.getScheme())) throw new IOException("Content URI required");
    cancel.throwIfCanceled();
    File folder = target.getParentFile();
    if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Private storage unavailable");
    File staged = File.createTempFile("import-", ".tmp", folder);
    try {
      try (AssetFileDescriptor source = context.getContentResolver().openAssetFileDescriptor(uri, "r", cancel)) {
        if (source == null) throw new IOException("Audio unavailable");
        long actualSize = source.getParcelFileDescriptor().getStatSize();
        if (actualSize < 0) throw new IOException("Download audio to the device first");
        if (source.getLength() > MAX_BYTES || actualSize > MAX_BYTES) throw new IOException("Audio too large");
        cancel.setOnCancelListener(() -> { try { source.close(); } catch (IOException ignored) {} });
        try (InputStream input = source.createInputStream(); FileOutputStream output = new FileOutputStream(staged)) {
          byte[] buffer = new byte[16_384];
          long count = 0;
          for (int read; (read = input.read(buffer)) != -1;) {
            cancel.throwIfCanceled();
            count += read;
            if (count > MAX_BYTES) throw new IOException("Audio too large");
            output.write(buffer, 0, read);
          }
          output.getFD().sync();
        } finally { cancel.setOnCancelListener(null); }
      }
      cancel.throwIfCanceled();
      validate(staged);
      synchronized (cancel) {
        cancel.throwIfCanceled();
        Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (OperationCanceledException cancelled) {
      throw cancelled;
    } finally {
      Files.deleteIfExists(staged.toPath());
    }
  }

  private static void validate(File file) throws IOException {
    if (file.length() == 0 || file.length() > MAX_BYTES) throw new IOException("Invalid audio size");
    MediaExtractor media = new MediaExtractor();
    try {
      media.setDataSource(file.getAbsolutePath());
      if (media.getTrackCount() != 1) throw new IOException("One MP3 audio track required");
      MediaFormat format = media.getTrackFormat(0);
      if (!"audio/mpeg".equals(format.getString(MediaFormat.KEY_MIME))
          || !format.containsKey(MediaFormat.KEY_DURATION)) throw new IOException("MP3 required");
      long duration = format.getLong(MediaFormat.KEY_DURATION);
      if (duration < 10_000_000L || duration > 300_000_000L) throw new IOException("Audio duration out of range");
      media.selectTrack(0);
      if (media.getSampleTime() < 0) throw new IOException("No audio samples");
    } catch (IllegalArgumentException | IllegalStateException malformed) {
      throw new IOException("Invalid MP3", malformed);
    } finally { media.release(); }
  }
}
