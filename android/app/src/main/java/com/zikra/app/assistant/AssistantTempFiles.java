package com.zikra.app.assistant;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Owns renderer copies only; never scans saved attachments or the document provider. */
public final class AssistantTempFiles {
  static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;
  private static final int SCAN_LIMIT = 256;
  private static final Set<Path> ACTIVE = new HashSet<>();

  private AssistantTempFiles() {}

  static synchronized Pdf createPdf(File cache) throws IOException {
    File file = File.createTempFile("assistant-pdf-", ".pdf", cache);
    Path path = file.toPath().toAbsolutePath().normalize();
    ACTIVE.add(path);
    return new Pdf(file, path);
  }

  static final class Pdf implements AutoCloseable {
    final File file;
    private final Path path;
    private Pdf(File file, Path path) { this.file = file; this.path = path; }
    @Override public void close() {
      synchronized (AssistantTempFiles.class) {
        ACTIVE.remove(path);
        try { Files.deleteIfExists(path); } catch (IOException ignored) { /* Retry on a later startup. */ }
      }
    }
  }

  /** Best effort after abnormal process exit; no guarantee of physical secure erasure. */
  public static synchronized void pruneStalePdfs(File cache, long now) {
    if (cache == null || now <= MAX_AGE_MS) return;
    Path root = cache.toPath().toAbsolutePath().normalize();
    try (DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
      int inspected = 0;
      for (Path entry : entries) {
        if (++inspected > SCAN_LIMIT) break;
        Path path = entry.toAbsolutePath().normalize();
        if (!root.equals(path.getParent()) || ACTIVE.contains(path)
            || !path.getFileName().toString().matches("assistant-pdf--?[0-9]+\\.pdf")
            || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
        long modified = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
        if (modified > 0 && modified < now - MAX_AGE_MS) Files.deleteIfExists(path);
      }
    } catch (IOException | java.nio.file.DirectoryIteratorException | SecurityException ignored) {
      // Cache cleanup must not prevent opening the app; retry at the next startup.
    }
  }
}
