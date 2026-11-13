package com.zikra.app.reading.mushaf;

import android.util.AtomicFile;
import java.io.*;

/** CDN fonts are separate from Content Sync text and transient audio metadata. */
final class MushafFontCache {
  interface Source { byte[] read() throws IOException; }
  interface Parser<T> { T read(File file) throws IOException; }
  static final class AccessDenied extends IOException {}
  private static final Object LOCK = new Object();
  private static final long REFRESH = 7L * 86400000;

  static <T> T load(File file, Source source, Parser<T> parser) throws IOException {
    synchronized (LOCK) {
      T previous = null;
      if (file.isFile()) {
        try { previous = parser.read(file); }
        catch (IOException | RuntimeException invalid) { /* Keep the file until replacement validates. */ }
      }
      long age = System.currentTimeMillis() - file.lastModified();
      if (previous != null && age >= 0 && age < REFRESH) return previous;
      byte[] bytes;
      try { bytes = source.read(); }
      catch (AccessDenied denied) { throw denied; }
      catch (IOException unavailable) {
        if (previous != null) return previous;
        throw unavailable;
      }
      File folder = file.getParentFile();
      if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Font storage unavailable");
      File staged = File.createTempFile("qcf-font-", ".pending", folder);
      try {
        try (FileOutputStream out = new FileOutputStream(staged)) {
          out.write(bytes); out.getFD().sync();
        }
        T candidate;
        try { candidate = parser.read(staged); }
        catch (RuntimeException invalid) { throw new IOException("Invalid Quran font", invalid); }
        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream output = null;
        try {
          output = atomic.startWrite(); output.write(bytes); output.getFD().sync();
          atomic.finishWrite(output);
        } catch (IOException failedWrite) {
          if (output != null) atomic.failWrite(output);
          if (previous != null) return previous;
          throw failedWrite;
        }
        return candidate;
      } finally { staged.delete(); }
    }
  }
  private MushafFontCache() {}
}
