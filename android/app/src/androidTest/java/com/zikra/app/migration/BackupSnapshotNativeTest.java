package com.zikra.app.migration;

import android.content.*;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.*;
import android.test.InstrumentationTestCase;
import com.zikra.app.assistant.ConversationStore;
import com.zikra.app.data.ZikraDbHelper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;

/** Scratch-only database round trip, never the physical installation or live credentials. */
@SuppressWarnings("deprecation")
public final class BackupSnapshotNativeTest extends InstrumentationTestCase {
  private File root;
  private Context source;
  private final Set<String> preferenceNames = new HashSet<>();
  private android.app.Activity foreground;
  protected void setUp() throws Exception {
    super.setUp(); Context target = getInstrumentation().getTargetContext();
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    assertTrue(new File(target.getFilesDir(), "local-migration-emulator-authorized").isFile());
    foreground = getInstrumentation().startActivitySync(new Intent(target, com.zikra.app.CalendarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    root = new File(target.getCacheDir(), "backup-native-" + UUID.randomUUID()); assertTrue(root.mkdir());
    source = scope(root);
  }
  private Context scope(File directoryRoot) {
    Context target = getInstrumentation().getTargetContext();
    String prefix = directoryRoot.getName() + "-";
    return new ContextWrapper(target) {
      @Override public Context getApplicationContext() { return this; }
      @Override public File getDataDir() { return directoryRoot; }
      @Override public File getFilesDir() { return directory("files"); }
      @Override public File getCacheDir() { return directory("cache"); }
      @Override public File getNoBackupFilesDir() { return directory("no-backup"); }
      private File directory(String name) { File path = new File(directoryRoot, name); path.mkdirs(); return path; }
      @Override public File getDatabasePath(String name) { return new File(directory("databases"), name); }
      @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
      }
      @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler handler) {
        return openOrCreateDatabase(name, mode, factory);
      }
      @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        preferenceNames.add(prefix + name); return target.getSharedPreferences(prefix + name, mode);
      }
      @Override public boolean deleteSharedPreferences(String name) { return target.deleteSharedPreferences(prefix + name); }
    };
  }
  protected void tearDown() throws Exception {
    try {
      LocalMigration.initialize(getInstrumentation().getTargetContext());
      if (foreground != null) getInstrumentation().runOnMainSync(foreground::finish);
      for (String name : preferenceNames) getInstrumentation().getTargetContext().deleteSharedPreferences(name);
      if (root != null) BackupArchive.removeOwnedTree(root);
    } finally { super.tearDown(); }
  }
  private JSONObject snapshot() throws Exception {
    File file = new File(root, "snapshot-" + UUID.randomUUID() + ".json"); BackupSnapshot.export(source, file); return BackupSnapshot.read(file);
  }
  private MigrationStageContext stage() throws Exception {
    return MigrationStageContext.create(source, new File(new File(source.getNoBackupFilesDir(), "local-migration"), UUID.randomUUID().toString()));
  }
  public void testPersonalRowsDraftsPreferencesAndDeletedIdSequenceSurviveWithoutCredentials() throws Exception {
    long removed;
    try (ZikraDbHelper db = new ZikraDbHelper(source)) {
      db.addTask("Synthetic retained task", ZikraDbHelper.CATEGORY_STUDY);
      removed = db.addTask("Synthetic deleted task", ZikraDbHelper.CATEGORY_STUDY);
      db.getWritableDatabase().delete("tasks", "id=?", new String[]{Long.toString(removed)});
    }
    String chat;
    try (ConversationStore chats = new ConversationStore(source)) {
      chat = chats.create().id;
      chats.save(chat, new JSONArray().put(new JSONObject().put("role", "user").put("text", "Synthetic conversation")), "Synthetic unsent draft");
    }
    source.getSharedPreferences("zikra_preferences", 0).edit().putString("profile_display_name", "Synthetic user")
        .putString("assistant_basic_consent_cloudflare_endpoint_v2", "SYNTHETIC-CONSENT-DO-NOT-EXPORT")
        .putBoolean("reminder_enabled", true).commit();
    source.getSharedPreferences("assistant_gateway", 0).edit().putString("token", "SYNTHETIC-TOKEN-DO-NOT-EXPORT").commit();
    JSONObject saved = snapshot(); assertFalse(saved.toString().contains("DO-NOT-EXPORT"));
    try (MigrationStageContext stage = stage()) {
      assertTrue(BackupSnapshot.restoreDatabases(stage, saved) >= 3);
      BackupSnapshot.restorePreferences(stage, saved);
      try (ZikraDbHelper restored = new ZikraDbHelper(stage); ConversationStore chats = new ConversationStore(stage)) {
        assertTrue(restored.addTask("Synthetic next task", ZikraDbHelper.CATEGORY_STUDY) > removed);
        assertEquals("Synthetic unsent draft", chats.get(chat).draft);
        assertEquals(1, chats.get(chat).messages.length());
      }
      String preferences = new String(Files.readAllBytes(new File(stage.root, "preferences/zikra_preferences.xml").toPath()), StandardCharsets.UTF_8);
      assertTrue(preferences.contains("Synthetic user")); assertFalse(preferences.contains("DO-NOT-EXPORT"));
      assertTrue(preferences.contains("name=\"reminder_enabled\" value=\"false\""));
      assertFalse(new File(stage.root, "preferences/assistant_gateway.xml").exists());
    }
    try (ZikraDbHelper original = new ZikraDbHelper(source)) { assertEquals(1, original.getTasks().size()); }
  }
  public void testUnsupportedSchemaColumnsAndPreferenceTypesAreRejectedInsideStage() throws Exception {
    try (ZikraDbHelper db = new ZikraDbHelper(source)) { db.addTask("Synthetic original", ZikraDbHelper.CATEGORY_STUDY); }
    JSONObject saved = snapshot();
    saved.getJSONObject("databases").getJSONObject("zikra.db").getJSONObject("tables").getJSONObject("tasks")
        .getJSONArray("columns").put(0, "id); DROP TABLE tasks; --");
    try (MigrationStageContext stage = stage()) {
      try { BackupSnapshot.restoreDatabases(stage, saved); fail(); } catch (IOException expected) { }
    }
    saved = snapshot(); saved.getJSONObject("preferences").getJSONObject("zikra_preferences")
        .put("last_ayah", new JSONObject().put("type", "string").put("value", "wrong type"));
    try (MigrationStageContext stage = stage()) {
      try { BackupSnapshot.restorePreferences(stage, saved); fail(); } catch (IOException expected) { }
    }
    try (ZikraDbHelper original = new ZikraDbHelper(source)) { assertEquals(1, original.getTasks().size()); }
  }
  public void testDuplicateAndDeepJsonNeverReachDatabaseRestoration() throws Exception {
    File file = new File(root, "malformed.json");
    for (String raw : new String[]{"{\"schema\":1,\"schema\":2}", "[[[[[[[[[[[[[[[[0]]]]]]]]]]]]]]]]"}) {
      Files.write(file.toPath(), raw.getBytes(StandardCharsets.UTF_8));
      try { BackupSnapshot.read(file); fail(); } catch (IOException expected) { }
    }
  }

  public void testEncryptedExportInspectAndStartupActivationPreserveAllFiveStores() throws Exception {
    try (ZikraDbHelper db = new ZikraDbHelper(source)) { db.addTask("Retained task", ZikraDbHelper.CATEGORY_STUDY); }
    try (com.zikra.app.data.WeeklyClassStore db = new com.zikra.app.data.WeeklyClassStore(source)) {
      db.getWritableDatabase().execSQL("INSERT INTO weekly_classes(revision,title,room,teacher,weekday,start_minute,end_minute,reminder_minutes,credits) VALUES(1,'Retained class','A','B',1,600,660,0,3)");
    }
    try (com.zikra.app.faith.FaithStore db = new com.zikra.app.faith.FaithStore(source)) {
      db.getWritableDatabase().execSQL("INSERT INTO day_notes(day,note) VALUES('2026-11-14','Retained journal')");
    }
    try (SQLiteOpenHelper db = com.zikra.app.discovery.HadithStore.migrationSchema(source)) {
      db.getWritableDatabase().execSQL("INSERT INTO bookmarks(id) VALUES('synthetic-bookmark')");
    }
    try (ConversationStore chats = new ConversationStore(source)) {
      chats.save(chats.create().id, new JSONArray().put(new JSONObject().put("role", "user").put("text", "Retained message")), "Retained draft");
    }
    source.getSharedPreferences("zikra_preferences", 0).edit().putString("profile_display_name", "Retained name").putBoolean("reminder_enabled", true).commit();
    File directory = new File(source.getCacheDir(), "zikra-export-" + UUID.randomUUID()); char[] password = "Synthetic-password-only-1234".toCharArray();
    File targetRoot = new File(root, "fresh-target"); assertTrue(targetRoot.mkdir()); Context target = scope(targetRoot);
    try {
      File encrypted = LocalMigration.export(source, password, directory);
      assertTrue(encrypted.length() > 48);
      try (InputStream input = new FileInputStream(encrypted); LocalMigration.Preview preview = LocalMigration.inspect(target, input, password)) {
        assertTrue(preview.canRestore()); assertTrue(preview.records >= 6);
        LocalMigration.requestRestore(target, preview);
      }
      assertTrue(LocalMigration.awaitingRestart(target)); LocalMigration.initialize(target);
      try { new ZikraDbHelper(target); fail("Normal stores must wait for recovery"); } catch (IllegalStateException expected) { }
      assertEquals("restored", LocalMigration.recover(target)); assertFalse(LocalMigration.awaitingRestart(target));
      assertTrue(LocalMigration.blocked()); // process exit remains mandatory
      LocalMigration.initialize(target); // simulate the next fresh process in the scoped fixture
      try (ZikraDbHelper db = new ZikraDbHelper(target)) { assertEquals(1, db.getTasks().size()); }
      for (String[] entry : new String[][]{{"study-schedule.db", "weekly_classes"}, {"faith.db", "day_notes"}, {"zikra_hadith.db", "bookmarks"}, {"assistant_chats.db", "conversations"}}) {
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(target.getDatabasePath(entry[0]).getPath(), null, SQLiteDatabase.OPEN_READONLY);
             android.database.Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + entry[1], null)) { assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0)); }
      }
      String xml = new String(Files.readAllBytes(new File(targetRoot, "shared_prefs/zikra_preferences.xml").toPath()), StandardCharsets.UTF_8);
      assertTrue(xml.contains("Retained name")); assertTrue(xml.contains("name=\"reminder_enabled\" value=\"false\""));
      try (ZikraDbHelper original = new ZikraDbHelper(source)) { assertEquals(1, original.getTasks().size()); }
    } finally { Arrays.fill(password, '\0'); LocalMigration.initialize(getInstrumentation().getTargetContext()); if (directory.exists()) LocalMigration.discardExport(source, directory); }
  }
  public void testWrongPasswordAndChangedTargetNeverReplacePersonalData() throws Exception {
    try (ZikraDbHelper db = new ZikraDbHelper(source)) { db.addTask("Original", ZikraDbHelper.CATEGORY_STUDY); }
    File directory = new File(source.getCacheDir(), "zikra-export-" + UUID.randomUUID()); char[] password = "Synthetic-password-only-1234".toCharArray();
    File targetRoot = new File(root, "changed-target"); assertTrue(targetRoot.mkdir()); Context target = scope(targetRoot);
    try {
      File encrypted = LocalMigration.export(source, password, directory);
      try (InputStream input = new FileInputStream(encrypted)) {
        try { LocalMigration.inspect(target, input, "Wrong-password-only-1234".toCharArray()); fail(); } catch (IOException expected) { }
      }
      assertFalse(target.getDatabasePath("zikra.db").exists());
      try (InputStream input = new FileInputStream(encrypted); LocalMigration.Preview preview = LocalMigration.inspect(target, input, password)) {
        LocalMigration.requestRestore(target, preview);
      }
      try (ZikraDbHelper db = new ZikraDbHelper(target)) { db.addTask("New personal work", ZikraDbHelper.CATEGORY_STUDY); }
      LocalMigration.initialize(target); assertEquals("changed", LocalMigration.recover(target)); LocalMigration.initialize(target);
      try (ZikraDbHelper db = new ZikraDbHelper(target)) { assertEquals("New personal work", db.getTasks().get(0).title); }
      try (InputStream input = new FileInputStream(encrypted); LocalMigration.Preview preview = LocalMigration.inspect(source, input, password)) { assertFalse(preview.canRestore()); }
    } finally { Arrays.fill(password, '\0'); LocalMigration.initialize(getInstrumentation().getTargetContext()); if (directory.exists()) LocalMigration.discardExport(source, directory); }
  }
  public void testFloatingPointIntegerPreferencesAndUnreferencedMediaAreRejected() throws Exception {
    JSONObject saved = snapshot(); saved.getJSONObject("preferences").getJSONObject("zikra_preferences")
        .put("last_ayah", new JSONObject().put("type", "int").put("value", 1.0));
    try (MigrationStageContext stage = stage()) {
      try { BackupSnapshot.restorePreferences(stage, saved); fail(); } catch (IOException expected) { }
    }
    Map<String, File> media = new HashMap<>(); media.put("media/assistant_images/00000000-0000-0000-0000-000000000000/00000000-0000-0000-0000-000000000001.png", new File(root, "orphan.png"));
    assertTrue(BackupMedia.forSnapshot(media, snapshot(), false).isEmpty());
    try { BackupMedia.forSnapshot(media, snapshot(), true); fail(); } catch (IOException expected) { }
  }
  public void testAbandonedCleanupRequiresOwnerAgeAndNoPendingRecovery() throws Exception {
    File parent = new File(source.getNoBackupFilesDir(), "local-migration");
    File owned;
    try (MigrationStageContext stage = stage()) { owned = stage.root; }
    File owner = new File(owned, ".owner"); assertTrue(owner.setLastModified(System.currentTimeMillis() - 25L * 60 * 60 * 1000));
    File unknown = new File(parent, UUID.randomUUID().toString()); assertTrue(unknown.mkdir());
    Files.write(new File(unknown, ".owner").toPath(), "not-owned".getBytes(StandardCharsets.UTF_8));
    File pending = new File(parent, "pending.json"); Files.write(pending.toPath(), "{}".getBytes(StandardCharsets.UTF_8));
    LocalMigration.initialize(source); LocalMigration.cleanAbandoned(source); assertTrue(owned.isDirectory());
    Files.delete(pending.toPath()); LocalMigration.initialize(source); LocalMigration.cleanAbandoned(source);
    assertFalse(owned.exists()); assertTrue(unknown.isDirectory());
  }

  public void testEncryptedMediaRoundTripKeepsAttachmentDraftAndGeneratedImage() throws Exception {
    String chat;
    try (ConversationStore chats = new ConversationStore(source)) { chat = chats.create().id; }
    String attachment = UUID.randomUUID().toString(); File chatFiles = new File(source.getFilesDir(), "assistant_attachments/" + chat);
    File pack = new File(chatFiles, attachment); assertTrue(pack.mkdirs());
    JSONObject meta = new JSONObject().put("id", attachment).put("name", "Synthetic photo.jpg").put("kind", "photo")
        .put("mime", "image/jpeg").put("pages", 1).put("bytes", 123).put("images", 1);
    Files.write(new File(pack, "meta.json").toPath(), meta.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(new File(pack, "text.txt").toPath(), new byte[0]);
    Files.write(new File(chatFiles, "draft.json").toPath(), new JSONObject().put("id", attachment).toString().getBytes(StandardCharsets.UTF_8));
    android.graphics.Bitmap picture = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888);
    try {
      try (OutputStream out = new FileOutputStream(new File(pack, "image-0.jpg"))) { assertTrue(picture.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)); }
      File images = new File(source.getFilesDir(), "assistant_images/" + chat); assertTrue(images.mkdirs());
      try (OutputStream out = new FileOutputStream(new File(images, UUID.randomUUID() + ".png"))) { assertTrue(picture.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)); }
    } finally { picture.recycle(); }
    File folder = new File(source.getCacheDir(), "zikra-export-" + UUID.randomUUID()); char[] password = "Synthetic-media-password-1234".toCharArray();
    try {
      File encrypted = LocalMigration.export(source, password, folder);
      try (InputStream input = new FileInputStream(encrypted); LocalMigration.Preview preview = LocalMigration.inspect(source, input, password)) {
        assertEquals(5, preview.mediaFiles);
        assertTrue(new File(preview.stage.getFilesDir(), "assistant_attachments/" + chat + "/" + attachment + "/image-0.jpg").isFile());
        assertFalse(preview.canRestore());
      }
      meta.put("id", UUID.randomUUID().toString()); Files.write(new File(pack, "meta.json").toPath(), meta.toString().getBytes(StandardCharsets.UTF_8));
      try { BackupMedia.validate(source.getFilesDir()); fail(); } catch (IOException expected) { }
    } finally { Arrays.fill(password, '\0'); if (folder.exists()) LocalMigration.discardExport(source, folder); }
  }

}
