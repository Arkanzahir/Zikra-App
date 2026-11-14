package com.zikra.app.migration;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Password-backed migration into a fresh installation. Activation runs before normal app initialization. */
public final class LocalMigration {
  private static volatile boolean blocked;
  private static final ThreadLocal<Boolean> RECOVERY = new ThreadLocal<>();
  private LocalMigration() {}
  private static File home(Context context) throws IOException {
    File home = new File(context.getNoBackupFilesDir(), "local-migration");
    if (!home.isDirectory() && !home.mkdirs()) throw new IOException("Private backup storage unavailable"); return home;
  }
  private static File pending(Context context) throws IOException { return new File(home(context), "pending.json"); }
  public static boolean awaitingRestart(Context context) {
    return new File(context.getNoBackupFilesDir(), "local-migration/pending.json").exists();
  }
  public static boolean blocked() { return blocked; }
  public static void initialize(Context context) { blocked = awaitingRestart(context); }
  public static Context storageContext(Context context) {
    if (blocked && !Boolean.TRUE.equals(RECOVERY.get())) throw new IllegalStateException("Local recovery is still running");
    return context;
  }
  public static synchronized void cleanAbandoned(Context context) {
    if (blocked || awaitingRestart(context)) return;
    long cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000;
    for (File parent : new File[]{new File(context.getNoBackupFilesDir(), "local-migration"), context.getCacheDir()}) {
      File[] children = parent.listFiles(); if (children == null) continue;
      for (File folder : children) {
        String name = folder.getName();
        if (!name.matches(parent.equals(context.getCacheDir()) ? "zikra-export-[a-f0-9-]{36}" : "[a-f0-9-]{36}")) continue;
        File owner = new File(folder, ".owner");
        try {
          if (folder.isDirectory() && !Files.isSymbolicLink(folder.toPath()) && owner.isFile() && owner.lastModified() > 0
              && owner.lastModified() < cutoff && owner.length() < 128
              && MigrationStageContext.OWNER.equals(new String(Files.readAllBytes(owner.toPath()), StandardCharsets.UTF_8)))
            BackupArchive.removeOwnedTree(folder);
        } catch (Exception retained) { /* Retain on uncertain ownership or failed cleanup. */ }
      }
    }
  }
  private static void capacity(Context context) throws IOException {
    if (context.getNoBackupFilesDir().getUsableSpace() < 4 * BackupCipher.MAX_PAYLOAD + 16L * 1024 * 1024)
      throw new IOException("At least 400 MB temporary space is required");
  }
  public static final class Preview implements AutoCloseable {
    private final Context context;
    final MigrationStageContext stage;
    final JSONObject data;
    final String seal;
    public final int records, mediaFiles;
    private Preview(Context context, MigrationStageContext stage, JSONObject data, int records, int mediaFiles, String seal) {
      this.context = context; this.stage = stage; this.data = data; this.records = records; this.mediaFiles = mediaFiles;
      this.seal = seal;
    }
    public boolean canRestore() { return android.os.Build.VERSION.SDK_INT >= 28 && !blocked && !awaitingRestart(context) && MigrationFreshness.empty(context); }
    @Override public void close() throws Exception {
      stage.close();
      if (!referenced(context, stage.root.getName())) BackupArchive.removeOwnedTree(stage.root);
    }
  }
  private static boolean referenced(Context context, String id) {
    try { return pending(context).exists() && readSmall(pending(context)).getString("stage").equals(id); }
    catch (Exception uncertain) { return true; }
  }
  public static Preview inspect(Context context, InputStream encrypted, char[] password) throws Exception {
    if (blocked || awaitingRestart(context)) throw new IOException("Finish pending recovery first");
    capacity(context); File root = new File(home(context), UUID.randomUUID().toString());
    MigrationStageContext stage = MigrationStageContext.create(context, root); boolean complete = false;
    try {
      File payload = new File(root, "payload.zip"), unpacked = new File(root, "unpacked");
      BackupCipher.decrypt(encrypted, payload, password); BackupArchive.unpack(payload, unpacked);
      JSONObject data = BackupSnapshot.read(new File(unpacked, "snapshot.json"));
      Object version = data.get("versionCode"), created = data.get("createdAt");
      int current = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
      if (!(version instanceof Number) || ((Number) version).doubleValue() != data.getInt("versionCode")
          || data.getInt("versionCode") < 1 || data.getInt("versionCode") > current
          || !(created instanceof Number) || data.getLong("createdAt") < 1)
        throw new IOException("Unsupported backup version");
      int count = BackupSnapshot.restoreDatabases(stage, data); BackupSnapshot.restorePreferences(stage, data);
      File media = new File(unpacked, "media");
      for (String group : new String[]{"assistant_attachments", "assistant_images"}) {
        File source = new File(media, group);
        if (source.exists() && !source.renameTo(new File(stage.getFilesDir(), group))) throw new IOException("Media staging failed");
      }
      Map<String, File> mediaEntries = BackupMedia.forSnapshot(BackupMedia.collect(stage.getFilesDir()), data, true);
      BackupMedia.validate(mediaEntries); int mediaFiles = mediaEntries.size();
      String seal = seal(context, root);
      complete = true; return new Preview(context, stage, data, count, mediaFiles, seal);
    } finally {
      if (!complete) { stage.close(); BackupArchive.removeOwnedTree(root); }
    }
  }
  /** Produces a private encrypted file, then validates that exact file without modifying live data. */
  public static File export(Context context, char[] password, File directory) throws Exception {
    if (blocked || awaitingRestart(context)) throw new IOException("Finish pending recovery first");
    capacity(context);
    if (!directory.getName().matches("zikra-export-[a-f0-9-]{36}")
        || !directory.getCanonicalFile().getParentFile().equals(context.getCacheDir().getCanonicalFile()) || !directory.mkdir())
      throw new IOException("Fresh private export directory required");
    Files.write(new File(directory, ".owner").toPath(), MigrationStageContext.OWNER.getBytes(StandardCharsets.UTF_8));
    File snapshot = new File(directory, "snapshot.json"), payload = new File(directory, "payload.zip"), encrypted = new File(directory, "zikra.zikrabackup");
    boolean complete = false;
    try {
      BackupSnapshot.export(context, snapshot);
      Map<String, File> entries = BackupMedia.forSnapshot(BackupMedia.collect(context.getFilesDir()), BackupSnapshot.read(snapshot), false);
      BackupMedia.validate(entries); entries.put("snapshot.json", snapshot);
      BackupArchive.pack(entries, payload);
      try (OutputStream output = Files.newOutputStream(encrypted.toPath(), StandardOpenOption.CREATE_NEW)) {
        BackupCipher.encrypt(payload, output, password);
      }
      try (FileOutputStream sync = new FileOutputStream(encrypted, true)) { sync.getFD().sync(); }
      try (InputStream input = new FileInputStream(encrypted); Preview verified = inspect(context, input, password)) { }
      Files.delete(snapshot.toPath()); Files.delete(payload.toPath()); complete = true; return encrypted;
    } finally { if (!complete) BackupArchive.removeOwnedTree(directory); }
  }
  public static void discardExport(Context context, File directory) throws Exception {
    if (!directory.getName().matches("zikra-export-[a-f0-9-]{36}")
        || !directory.getCanonicalFile().getParentFile().equals(context.getCacheDir().getCanonicalFile()))
      throw new IOException("Unknown export directory");
    BackupArchive.removeOwnedTree(directory);
  }
  private static String fingerprint(Context context, File root) throws Exception {
    File snapshot = new File(root, "fingerprint-" + UUID.randomUUID() + ".json");
    try {
      BackupSnapshot.export(context, snapshot); JSONObject data = BackupSnapshot.read(snapshot); data.remove("createdAt");
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(data.toString().getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder(); for (byte value : hash) result.append(String.format(Locale.ROOT, "%02x", value & 255)); return result.toString();
    } finally { Files.deleteIfExists(snapshot.toPath()); }
  }
  public static synchronized void requestRestore(Context context, Preview preview) throws Exception {
    if (!preview.canRestore() || !preview.stage.root.getCanonicalFile().getParentFile().equals(home(context).getCanonicalFile()))
      throw new IOException("Restore requires a fresh installation");
    if (!preview.seal.equals(seal(context, preview.stage.root))) throw new IOException("Validated backup stage changed");
    JSONObject request = new JSONObject().put("schema", 1).put("stage", preview.stage.root.getName())
        .put("fingerprint", fingerprint(context, preview.stage.root)).put("seal", preview.seal);
    saveSmall(pending(context), request);
  }
  private static Map<String, File> inventory(Context context, File stage, boolean live) {
    Map<String, File> result = new LinkedHashMap<>();
    for (String name : BackupPolicy.DATABASES.keySet()) for (String suffix : new String[]{"", "-wal", "-shm", "-journal"})
      result.put("db-" + name + suffix, live ? new File(context.getDatabasePath(name).getPath() + suffix)
          : new File(stage, "databases/" + name + suffix));
    for (String group : BackupPolicy.preferenceGroups()) for (String suffix : new String[]{"", ".bak", ".new"})
      result.put("pref-" + group + suffix, live ? new File(new File(context.getDataDir(), "shared_prefs"), group + ".xml" + suffix)
          : new File(stage, "preferences/" + group + ".xml" + suffix));
    for (String group : new String[]{"assistant_images", "assistant_attachments"})
      result.put(group, live ? new File(context.getFilesDir(), group) : new File(stage, "files/" + group));
    result.put("snapshot", live ? new File(context.getNoBackupFilesDir(), "local-migration/active-snapshot.json")
        : new File(stage, "unpacked/snapshot.json"));
    return result;
  }
  private static String seal(Context context, File root) throws Exception {
    JSONObject hashes = new JSONObject();
    for (Map.Entry<String, File> entry : inventory(context, root, false).entrySet()) {
      String hash = MigrationTransaction.hash(entry.getValue()); hashes.put(entry.getKey(), hash == null ? JSONObject.NULL : hash);
    }
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(hashes.toString().getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder(); for (byte value : digest) hex.append(String.format(Locale.ROOT, "%02x", value & 255)); return hex.toString();
  }
  private static JSONObject readSmall(File file) throws Exception {
    if (!file.isFile() || file.length() > 4096) throw new IOException("Invalid pending recovery");
    return new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
  }
  private static void saveSmall(File file, JSONObject value) throws Exception {
    File next = new File(file.getParentFile(), file.getName() + ".next");
    try (FileOutputStream output = new FileOutputStream(next)) { output.write(value.toString().getBytes(StandardCharsets.UTF_8)); output.getFD().sync(); }
    Files.move(next.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }
  /** Runs on the loading screen's worker before other app stores/jobs are initialized. */
  public static synchronized String recover(Context context) throws Exception {
    if (!pending(context).exists()) return blocked ? context.getSharedPreferences("local_migration_status", 0).getString("result", "restart") : "none";
    blocked = true; RECOVERY.set(true);
    try {
      JSONObject request = readSmall(pending(context)); String id = request.getString("stage");
      if (request.getInt("schema") != 1 || !id.matches("[a-f0-9-]{36}") || !request.getString("fingerprint").matches("[a-f0-9]{64}")
          || !request.getString("seal").matches("[a-f0-9]{64}"))
        throw new IOException("Invalid pending recovery");
      File root = new File(home(context), id);
      MigrationTransaction transaction = new MigrationTransaction(root, inventory(context, root, false), inventory(context, root, true));
      String result;
      if (!transaction.hasJournal()) {
        if (!MigrationFreshness.empty(context) || !request.getString("fingerprint").equals(fingerprint(context, root))
            || !request.getString("seal").equals(seal(context, root))) {
          result = "changed"; return finish(context, root, result);
        }
        transaction.prepare();
      }
      try {
        if (!transaction.phase().equals("rolled_back")) transaction.apply();
      } catch (Exception failed) {
        if (transaction.phase().equals("committed")) throw failed;
        transaction.rollback();
      }
      result = transaction.phase().equals("committed") ? "restored" : "rolled_back";
      if (result.equals("restored")) {
        JSONObject snapshot = BackupSnapshot.read(new File(context.getNoBackupFilesDir(), "local-migration/active-snapshot.json"));
        // A fresh process is required before normal initialization; no cached preference writes here.
        if (snapshot.getInt("schema") != 1) throw new IOException("Invalid activated snapshot");
      }
      return finish(context, root, result);
    } finally { RECOVERY.remove(); }
  }
  private static String finish(Context context, File root, String result) throws Exception {
    if (!context.getSharedPreferences("local_migration_status", 0).edit().putString("result", result).commit())
      throw new IOException("Recovery result could not be saved");
    Files.delete(pending(context).toPath()); // Stay gated until this process exits.
    try {
      Files.deleteIfExists(new File(context.getNoBackupFilesDir(), "local-migration/active-snapshot.json").toPath());
      BackupArchive.removeOwnedTree(root);
    } catch (IOException cleanupPending) {
      context.getSharedPreferences("local_migration_status", 0).edit().putBoolean("cleanup_pending", true).commit();
    }
    return result;
  }
}
