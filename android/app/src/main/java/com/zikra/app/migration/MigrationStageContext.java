package com.zikra.app.migration;

import android.content.*;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/** Schema creation is confined to a fresh app-private stage, never a live database path. */
final class MigrationStageContext extends ContextWrapper implements AutoCloseable {
  static final String OWNER = "zikra-personal-migration-v1";
  final File root;
  private final String emptyPreferences = "migration-empty-" + UUID.randomUUID();
  private MigrationStageContext(Context context, File root) { super(context); this.root = root; }
  static MigrationStageContext create(Context context, File root) throws IOException {
    File parent = new File(context.getNoBackupFilesDir(), "local-migration");
    if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Private migration storage unavailable");
    if (!root.getCanonicalFile().getParentFile().equals(parent.getCanonicalFile()) || !root.mkdir())
      throw new IOException("Fresh private migration stage required");
    Files.write(new File(root, ".owner").toPath(), OWNER.getBytes(StandardCharsets.UTF_8));
    return new MigrationStageContext(context, root);
  }
  @Override public Context getApplicationContext() { return this; }
  private File folder(String name) {
    File result = new File(root, name);
    if (!result.isDirectory() && !result.mkdirs()) throw new IllegalStateException("Migration storage unavailable");
    return result;
  }
  @Override public File getFilesDir() { return folder("files"); }
  @Override public File getCacheDir() { return folder("cache"); }
  @Override public File getDatabasePath(String name) {
    if (!BackupPolicy.DATABASES.containsKey(name)) throw new IllegalArgumentException("Unknown migration database");
    return new File(folder("databases"), name);
  }
  @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
    return openOrCreateDatabase(name, mode, factory, null);
  }
  @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
      DatabaseErrorHandler errorHandler) {
    File path = getDatabasePath(name);
    return errorHandler == null ? SQLiteDatabase.openOrCreateDatabase(path, factory)
        : SQLiteDatabase.openDatabase(path.getPath(), factory, SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler);
  }
  @Override public boolean deleteDatabase(String name) { return SQLiteDatabase.deleteDatabase(getDatabasePath(name)); }
  @Override public SharedPreferences getSharedPreferences(String name, int mode) {
    return getBaseContext().getSharedPreferences(emptyPreferences, Context.MODE_PRIVATE);
  }
  @Override public void close() { getBaseContext().deleteSharedPreferences(emptyPreferences); }
}
