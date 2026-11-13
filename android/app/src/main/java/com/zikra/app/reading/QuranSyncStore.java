package com.zikra.app.reading;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

/** Complete chapter generations are published by a single atomic manifest replacement. */
public final class QuranSyncStore {
  private static final Object COMMIT = new Object();
  private static final Set<String> ACTIVE = new HashSet<>();
  private static final String OWNER = "zikra-quran-sync-v1";
  private final File root;
  private boolean recovered;
  public interface Source { String page(int page) throws Exception; }
  public interface Check { void run() throws Exception; }
  public QuranSyncStore(Context context) { root = new File(context.getFilesDir(), "quran-content-sync-r33"); }
  private AtomicFile pointer(int chapter) { return new AtomicFile(new File(root, "chapter-" + chapter + ".json")); }
  public static String readBounded(InputStream source, int limit) throws IOException {
    try (InputStream input = source; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] bytes = new byte[8192]; int size;
      while ((size = input.read(bytes)) != -1) {
        if (output.size() + size > limit) throw new IOException("Quran file budget exceeded");
        output.write(bytes, 0, size);
      }
      return output.toString("UTF-8");
    }
  }
  public String read(QuranChapters.Chapter chapter, int page) throws Exception {
    synchronized (COMMIT) {
      recoverOrphans();
      JSONObject manifest = new JSONObject(readBounded(pointer(chapter.id).openRead(), 4096));
      String generation = manifest.getString("generation");
      if (manifest.getInt("schema") != 1 || manifest.getInt("chapter") != chapter.id
          || !generation.matches("[a-f0-9-]{36}") || manifest.getInt("pages") != (chapter.count + 49) / 50)
        throw new IOException("Invalid Quran chapter manifest");
      String json = readBounded(new FileInputStream(new File(new File(root, generation), page + ".json")), 524288);
      if (QuranSyncPage.validate(json, chapter, page, System.currentTimeMillis()) != manifest.getLong("checked_at"))
        throw new IOException("Quran generation mismatch");
      return json;
    }
  }
  public void download(QuranChapters.Chapter chapter, Source source, Check check) throws Exception {
    check.run();
    File staging;
    synchronized (COMMIT) {
      if (!root.isDirectory() && !root.mkdirs()) throw new IOException("Quran storage unavailable");
      if (root.getUsableSpace() < 6L * 1024 * 1024) throw new IOException("Insufficient Quran storage");
      recoverOrphans();
      staging = new File(root, UUID.randomUUID().toString());
      if (!staging.mkdir()) throw new IOException("Quran staging unavailable");
      try (FileOutputStream owner = new FileOutputStream(new File(staging, ".owner"))) {
        owner.write(OWNER.getBytes(StandardCharsets.UTF_8)); owner.getFD().sync();
      } catch (IOException failedOwner) {
        new File(staging, ".owner").delete(); staging.delete();
        throw failedOwner;
      }
      ACTIVE.add(staging.getName());
    }
    boolean committed = false;
    try {
      int pages = (chapter.count + 49) / 50;
      long checked = -1;
      for (int page = 1; page <= pages; page++) {
        check.run();
        String json = source.page(page);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 524288) throw new IOException("Quran page budget exceeded");
        long time = QuranSyncPage.validate(json, chapter, page, System.currentTimeMillis());
        if (checked != -1 && checked != time) throw new IOException("Quran source changed during download; retry");
        checked = time;
        try (FileOutputStream output = new FileOutputStream(new File(staging, page + ".json"))) {
          output.write(bytes); output.getFD().sync();
        }
      }
      check.run();
      JSONObject manifest = new JSONObject().put("schema", 1).put("chapter", chapter.id)
          .put("pages", pages).put("generation", staging.getName()).put("checked_at", checked);
      synchronized (COMMIT) {
        AtomicFile pointer = pointer(chapter.id); FileOutputStream output = null;
        String obsolete = null;
        try {
          JSONObject old = new JSONObject(readBounded(pointer.openRead(), 4096));
          if (old.getLong("checked_at") > checked) throw new IOException("Newer Quran generation already installed");
          String previous = old.getString("generation");
          if (previous.matches("[a-f0-9-]{36}")) manifest.put("previous", previous);
          obsolete = old.optString("previous", "");
        } catch (FileNotFoundException firstDownload) { /* No prior chapter generation. */ }
        try {
          output = pointer.startWrite(); output.write(manifest.toString().getBytes(StandardCharsets.UTF_8));
          pointer.finishWrite(output); committed = true;
        } catch (Exception error) {
          if (output != null) pointer.failWrite(output);
          throw error;
        }
        removeOwnedGeneration(obsolete);
      }
    } finally {
      synchronized (COMMIT) {
        ACTIVE.remove(staging.getName());
        if (!committed) removeOwnedGeneration(staging.getName());
      }
    }
  }
  private void removeOwnedGeneration(String name) {
    if (name == null || !name.matches("[a-f0-9-]{36}")) return;
    File folder = new File(root, name);
    File[] files = folder.listFiles();
    if (files == null || files.length > 7) return;
    try {
      if (!OWNER.equals(readBounded(new FileInputStream(new File(folder, ".owner")), 128))) return;
    } catch (IOException unknownOwner) { return; }
    for (File file : files) if (!file.isFile() || !file.getName().matches("([1-6]\\.json|\\.owner)")) return;
    for (File file : files) file.delete();
    folder.delete();
  }
  private void recoverOrphans() {
    if (recovered) return;
    File[] files = root.listFiles();
    if (files == null) return;
    if (files.length > 1000) return;
    Set<String> keep = new HashSet<>(ACTIVE), pointers = new HashSet<>();
    for (File file : files) {
      String name = file.getName();
      if (name.matches("chapter-[1-9][0-9]{0,2}\\.json(\\.bak)?"))
        pointers.add(name.replaceFirst("\\.bak$", ""));
    }
    try {
      for (String name : pointers) {
        JSONObject manifest = new JSONObject(readBounded(new AtomicFile(new File(root, name)).openRead(), 4096));
        if (manifest.getInt("schema") != 1 || !manifest.getString("generation").matches("[a-f0-9-]{36}")) return;
        keep.add(manifest.getString("generation"));
        keep.add(manifest.optString("previous", ""));
      }
    } catch (Exception uncertainManifest) { return; }
    for (File file : files) if (file.isDirectory() && !keep.contains(file.getName())) removeOwnedGeneration(file.getName());
    recovered = true;
  }
}
