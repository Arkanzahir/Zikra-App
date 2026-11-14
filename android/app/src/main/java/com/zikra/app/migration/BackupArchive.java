package com.zikra.app.migration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.*;

/** Only the personal snapshot and bounded app-owned chat media can enter an archive. */
public final class BackupArchive {
  public static final int MAX_FILES = 12000, MAX_SINGLE = 32 * 1024 * 1024;
  private static final String ID = "(?:legacy-local|[a-f0-9-]{36})";
  private BackupArchive() {}

  public static boolean allowed(String name) {
    return "snapshot.json".equals(name)
        || name != null && (name.matches("media/assistant_images/" + ID + "/[a-f0-9-]{36}\\.(?:jpg|png)")
        || name.matches("media/assistant_attachments/" + ID + "/(?:draft\\.json|[a-f0-9-]{36}/(?:meta\\.json|text\\.txt|image-[0-2]\\.jpg))"));
  }

  public static void pack(Map<String, File> entries, File output) throws IOException {
    if (!entries.containsKey("snapshot.json") || entries.size() > MAX_FILES) throw new IOException("Invalid backup inventory");
    for (String name : entries.keySet()) if (!allowed(name)) throw new IOException("Unsupported backup entry");
    boolean created = false, complete = false;
    try (OutputStream stream = Files.newOutputStream(output.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      created = true;
      try (ZipOutputStream zip = new ZipOutputStream(stream)) {
        long total = 0; byte[] buffer = new byte[16384];
        for (String name : new TreeSet<>(entries.keySet())) {
          File input = entries.get(name); if (!input.isFile()) throw new IOException("Backup source unavailable");
          ZipEntry entry = new ZipEntry(name); entry.setTime(0); zip.putNextEntry(entry);
          long size = 0;
          try (InputStream source = new FileInputStream(input)) {
            int count;
            while ((count = source.read(buffer)) != -1) {
              size += count; total += count;
              if (size > MAX_SINGLE || total > BackupCipher.MAX_PAYLOAD) throw new IOException("Backup content exceeds budget");
              zip.write(buffer, 0, count);
            }
          }
          zip.closeEntry();
        }
      }
      if (output.length() > BackupCipher.MAX_PAYLOAD) throw new IOException("Backup archive exceeds budget");
      complete = true;
    } finally { if (created && !complete) Files.deleteIfExists(output.toPath()); }
  }

  /** Creates a fresh private directory and discards it entirely on any validation failure. */
  public static void unpack(File payload, File directory) throws IOException {
    if (!directory.mkdir()) throw new IOException("Fresh backup staging directory required");
    boolean complete = false;
    try (ZipInputStream zip = new ZipInputStream(new FileInputStream(payload))) {
      Set<String> names = new HashSet<>(); byte[] buffer = new byte[16384]; long total = 0;
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (entry.isDirectory() || !allowed(name) || !names.add(name) || names.size() > MAX_FILES)
          throw new IOException("Invalid backup entry");
        File output = new File(directory, name);
        if (!output.getCanonicalPath().startsWith(directory.getCanonicalPath() + File.separator))
          throw new IOException("Invalid backup path");
        File parent = output.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Backup staging unavailable");
        try (OutputStream destination = Files.newOutputStream(output.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
          int count; long size = 0;
          while ((count = zip.read(buffer)) != -1) {
            size += count; total += count;
            if (size > MAX_SINGLE || total > BackupCipher.MAX_PAYLOAD) throw new IOException("Expanded backup exceeds budget");
            destination.write(buffer, 0, count);
          }
        }
        zip.closeEntry();
      }
      if (!names.contains("snapshot.json")) throw new IOException("Backup snapshot missing");
      complete = true;
    } finally { if (!complete) removeOwnedTree(directory); }
  }

  static void removeOwnedTree(File file) throws IOException {
    if (Files.isSymbolicLink(file.toPath())) { Files.delete(file.toPath()); return; }
    File[] children = file.listFiles();
    if (children != null) {
      Arrays.sort(children, Comparator.comparing(child -> child.getName().equals(".owner")));
      for (File child : children) removeOwnedTree(child);
    }
    Files.deleteIfExists(file.toPath());
  }
}
