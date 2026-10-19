package com.zikra.app.audio;

import android.content.Context;
import java.io.File;

/** App-private, durable downloads; no shared-storage permission or hidden bulk download. */
public final class AudioFiles {
  private AudioFiles() {}

  public static File file(Context context, String reciter, int chapter, int verse) {
    Recitation.globalVerse(chapter, verse);
    Recitation.reciterIndex(reciter);
    return new File(
        context.getFilesDir(), "quran-audio/" + reciter + "/" + chapter + "/" + verse + ".mp3");
  }

  public static boolean available(Context c, String reciter, int chapter, int verse) {
    File file = file(c, reciter, chapter, verse);
    return file.isFile() && file.length() > 128;
  }

  public static int downloaded(Context c, String reciter, int chapter) {
    int count = 0;
    for (int v = 1; v <= Recitation.count(chapter); v++)
      if (available(c, reciter, chapter, v)) count++;
    return count;
  }

  public static long bytes(Context c, String reciter, int chapter) {
    long bytes = 0;
    for (int v = 1; v <= Recitation.count(chapter); v++)
      bytes += file(c, reciter, chapter, v).length();
    return bytes;
  }

  public static void delete(Context c, String reciter, int chapter) {
    for (int v = 1; v <= Recitation.count(chapter); v++) {
      File f = file(c, reciter, chapter, v);
      if (f.exists() && !f.delete()) throw new IllegalStateException("Audio belum dapat dihapus");
    }
  }
}
