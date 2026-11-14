package com.zikra.app.migration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** App-start transaction: retain original files until every staged replacement has been installed. */
final class MigrationTransaction {
  interface Checkpoint { void moved(String key) throws IOException; }
  private final File root, journal, originals;
  private final Map<String, File> staged, targets;
  MigrationTransaction(File root, Map<String, File> staged, Map<String, File> targets) throws Exception {
    this.root = root.getCanonicalFile(); this.staged = new LinkedHashMap<>(staged); this.targets = new LinkedHashMap<>(targets);
    if (!this.staged.keySet().equals(this.targets.keySet()) || staged.size() > 64 || staged.isEmpty())
      throw new IOException("Invalid migration inventory");
    File owner = new File(root, ".owner");
    if (!owner.isFile() || owner.length() > 128 || !MigrationStageContext.OWNER.equals(new String(Files.readAllBytes(owner.toPath()), StandardCharsets.UTF_8)))
      throw new IOException("Unknown migration stage");
    Set<String> sourcePaths = new HashSet<>(), targetPaths = new HashSet<>();
    for (String key : staged.keySet()) {
      String source = staged.get(key).getCanonicalPath(), target = targets.get(key).getCanonicalPath();
      if (!key.matches("[a-zA-Z0-9._:-]{1,80}") || !source.startsWith(this.root.getPath() + File.separator)
          || target.equals(this.root.getPath()) || target.startsWith(this.root.getPath() + File.separator)
          || !sourcePaths.add(source) || !targetPaths.add(target))
        throw new IOException("Invalid migration destination");
    }
    journal = new File(root, "journal.json"); originals = new File(root, "originals");
  }
  boolean hasJournal() { return journal.isFile(); }
  String phase() throws Exception { return load().getString("phase"); }
  void prepare() throws Exception {
    if (journal.exists() || originals.exists()) throw new IOException("Migration transaction already exists");
    if (!originals.mkdir()) throw new IOException("Migration rollback storage unavailable");
    JSONArray entries = new JSONArray();
    for (String key : targets.keySet()) entries.put(new JSONObject().put("key", key)
        .put("old", nullable(hash(targets.get(key)))).put("next", nullable(hash(staged.get(key)))));
    save(new JSONObject().put("schema", 1).put("phase", "prepared").put("entries", entries));
  }
  private static Object nullable(String value) { return value == null ? JSONObject.NULL : value; }
  private void save(JSONObject value) throws Exception {
    File next = new File(root, "journal.next");
    try (FileOutputStream output = new FileOutputStream(next)) {
      output.write(value.toString().getBytes(StandardCharsets.UTF_8)); output.getFD().sync();
    }
    Files.move(next.toPath(), journal.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }
  private JSONObject load() throws Exception {
    if (!journal.isFile() || journal.length() > 65536) throw new IOException("Migration journal unavailable");
    JSONObject value = new JSONObject(new String(Files.readAllBytes(journal.toPath()), StandardCharsets.UTF_8));
    if (value.getInt("schema") != 1 || !Arrays.asList("prepared", "committed", "rolled_back").contains(value.getString("phase")))
      throw new IOException("Invalid migration journal");
    JSONArray entries = value.getJSONArray("entries"); Set<String> found = new HashSet<>();
    for (int i = 0; i < entries.length(); i++) {
      JSONObject entry = entries.getJSONObject(i); String key = entry.getString("key");
      if (!targets.containsKey(key) || !found.add(key)) throw new IOException("Migration inventory mismatch");
      for (String field : new String[]{"old", "next"})
        if (!entry.isNull(field) && !entry.getString(field).matches("[a-f0-9]{64}")) throw new IOException("Invalid migration digest");
    }
    if (!found.equals(targets.keySet())) throw new IOException("Incomplete migration inventory"); return value;
  }
  private static String digest(JSONObject entry, String field) throws Exception { return entry.isNull(field) ? null : entry.getString(field); }
  private static void require(File file, String expected) throws Exception {
    if (!Objects.equals(hash(file), expected)) throw new IOException("Migration file changed; refusing overwrite");
  }
  private static void move(File source, File destination) throws IOException {
    File parent = destination.getParentFile();
    if ((!parent.isDirectory() && !parent.mkdirs()) || destination.exists() || !source.renameTo(destination))
      throw new IOException("Migration move failed");
  }
  void apply() throws Exception { apply(key -> {}); }
  void apply(Checkpoint checkpoint) throws Exception {
    JSONObject value = load();
    if (value.getString("phase").equals("committed")) {
      JSONArray committed = value.getJSONArray("entries");
      for (int i = 0; i < committed.length(); i++) {
        JSONObject entry = committed.getJSONObject(i); require(targets.get(entry.getString("key")), digest(entry, "next"));
      }
      return;
    }
    if (!value.getString("phase").equals("prepared")) throw new IOException("Migration was rolled back");
    JSONArray entries = value.getJSONArray("entries");
    for (int i = 0; i < entries.length(); i++) {
      JSONObject entry = entries.getJSONObject(i); String key = entry.getString("key");
      File target = targets.get(key), next = staged.get(key), old = new File(originals, key);
      String previous = digest(entry, "old"), replacement = digest(entry, "next");
      if (previous != null) {
        if (!old.exists()) { require(target, previous); move(target, old); checkpoint.moved(key); }
        else require(old, previous);
      } else if (old.exists()) throw new IOException("Unexpected rollback file");
      if (replacement != null) {
        if (next.exists()) { require(next, replacement); move(next, target); checkpoint.moved(key); }
        require(target, replacement);
      } else require(target, null);
    }
    value.put("phase", "committed"); save(value);
  }
  void rollback() throws Exception {
    JSONObject value = load();
    if (value.getString("phase").equals("committed")) throw new IOException("Committed migration cannot be rolled back automatically");
    JSONArray entries = value.getJSONArray("entries");
    for (int i = entries.length() - 1; i >= 0; i--) {
      JSONObject entry = entries.getJSONObject(i); String key = entry.getString("key");
      File target = targets.get(key), old = new File(originals, key);
      String previous = digest(entry, "old"), replacement = digest(entry, "next");
      if (old.exists()) {
        require(old, previous);
        if (target.exists()) { require(target, replacement); BackupArchive.removeOwnedTree(target); }
        move(old, target);
      } else if (previous == null) {
        if (target.exists()) { require(target, replacement); BackupArchive.removeOwnedTree(target); }
      } else require(target, previous);
    }
    value.put("phase", "rolled_back"); save(value);
  }
  static String hash(File file) throws Exception {
    if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return null;
    MessageDigest digest = MessageDigest.getInstance("SHA-256"); long[] budget = {0, 0};
    hash(file, "", digest, budget, 0); StringBuilder hex = new StringBuilder();
    for (byte value : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", value & 255)); return hex.toString();
  }
  private static void hash(File file, String name, MessageDigest digest, long[] budget, int depth) throws Exception {
    if (++budget[0] > BackupArchive.MAX_FILES || depth > 12 || Files.isSymbolicLink(file.toPath())) throw new IOException("Invalid migration tree");
    digest.update((file.isDirectory() ? "D:" : "F:").getBytes(StandardCharsets.US_ASCII));
    digest.update(name.getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0);
    if (file.isDirectory()) {
      File[] children = file.listFiles(); if (children == null) throw new IOException("Migration directory unreadable");
      Arrays.sort(children, Comparator.comparing(File::getName));
      for (File child : children) hash(child, name + "/" + child.getName(), digest, budget, depth + 1);
    } else {
      digest.update(java.nio.ByteBuffer.allocate(8).putLong(file.length()).array());
      try (InputStream input = new FileInputStream(file)) {
        byte[] buffer = new byte[16384]; int count;
        while ((count = input.read(buffer)) != -1) {
          budget[1] += count; if (budget[1] > BackupCipher.MAX_PAYLOAD) throw new IOException("Migration tree exceeds budget");
          digest.update(buffer, 0, count);
        }
      }
    }
  }
}
