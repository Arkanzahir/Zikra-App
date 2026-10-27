package com.zikra.app.storage;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.util.Base64;
import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.time.*;
import java.util.*;
import org.json.*;

/** Test-only reflection lets the same helper exercise actual old installed APK classes. */
@SuppressWarnings("deprecation")
public final class UpgradeFixtureTest extends InstrumentationTestCase {
  private Context context;
  private static final String[] PREFS = {
    "zikra_preferences", "zikra_location", "quran_audio_options", "assistant_gateway"
  };
  private static final String ACCESS = "D06FixtureAccessCodeOnlyNotARealCredential32";
  private static final String CHAT = "d0600000-0000-4000-8000-000000000001";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue(
        "Host must isolate original data before testing",
        new File(context.getFilesDir(), "d06-authorized").isFile());
  }

  private Object object(String name) throws Exception {
    try {
      return Class.forName(name, true, context.getClassLoader())
          .getConstructor(Context.class)
          .newInstance(context);
    } catch (ClassNotFoundException unavailable) {
      return null;
    }
  }

  private SQLiteOpenHelper helper(String name) throws Exception {
    return (SQLiteOpenHelper) object(name);
  }

  private File file(String name) {
    return new File(context.getFilesDir(), name);
  }

  private void json(File f, JSONObject value) throws Exception {
    Files.write(f.toPath(), value.toString(2).getBytes(StandardCharsets.UTF_8));
  }

  private JSONObject json(File f) throws Exception {
    return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
  }

  private String hash(byte[] data) throws Exception {
    StringBuilder h = new StringBuilder();
    for (byte b : MessageDigest.getInstance("SHA-256").digest(data))
      h.append(String.format(Locale.ROOT, "%02x", b & 255));
    return h.toString();
  }

  private Set<String> aliases() throws Exception {
    KeyStore k = KeyStore.getInstance("AndroidKeyStore");
    k.load(null);
    return new TreeSet<>(Collections.list(k.aliases()));
  }

  public void testRecordKeystoreBaseline() throws Exception {
    json(
        new File(context.getFilesDir().getParentFile(), ".d06-key-baseline.json"),
        new JSONObject().put("aliases", new JSONArray(aliases())));
  }

  public void testRestoreFixtureKeystore() throws Exception {
    File baseline = new File(context.getFilesDir().getParentFile(), ".d06-key-baseline.json");
    JSONArray names = json(baseline).getJSONArray("aliases");
    Set<String> original = new TreeSet<>();
    for (int i = 0; i < names.length(); i++) original.add(names.getString(i));
    KeyStore k = KeyStore.getInstance("AndroidKeyStore");
    k.load(null);
    String added = "zikra-assistant-gateway-v1";
    if (!original.contains(added) && k.containsAlias(added)) k.deleteEntry(added);
    assertTrue("Original non-exportable key aliases remain", aliases().containsAll(original));
    assertTrue(baseline.delete());
  }

  private Set<String> columns(SQLiteDatabase db, String table) {
    Set<String> result = new HashSet<>();
    try (Cursor c = db.rawQuery("PRAGMA table_info(" + quoted(table) + ")", null)) {
      while (c.moveToNext()) result.add(c.getString(1));
    }
    return result;
  }

  private String quoted(String name) {
    if (!name.matches("[A-Za-z_][A-Za-z_0-9]*"))
      throw new IllegalArgumentException("Fixture identifier");
    return "\"" + name + "\"";
  }

  private void insert(SQLiteDatabase db, String table, Object... pairs) {
    ContentValues v = new ContentValues();
    Set<String> fields = columns(db, table);
    for (int i = 0; i < pairs.length; i += 2) {
      String key = (String) pairs[i];
      Object value = pairs[i + 1];
      if (!fields.contains(key)) continue;
      if (value == null) v.putNull(key);
      else if (value instanceof Number) v.put(key, ((Number) value).longValue());
      else v.put(key, value.toString());
    }
    db.insertOrThrow(table, null, v);
  }

  private String today() {
    return LocalDate.now(ZoneId.of("Asia/Makassar")).toString();
  }

  public void testSeedPopulatedOldVersion() throws Exception {
    SharedPreferences p = context.getSharedPreferences("zikra_preferences", 0);
    JSONArray legacy =
        new JSONArray()
            .put(
                new JSONObject()
                    .put("role", "user")
                    .put("text", "DATA UJI D06 legacy")
                    .put("mode", "offline"))
            .put(
                new JSONObject()
                    .put("role", "assistant")
                    .put("text", "Balasan uji lokal")
                    .put("mode", "offline"));
    p.edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .putString("profile_display_name", "DATA UJI D06")
        .putString("user_role", "Student")
        .putString("last_surah", "Al-Fatihah")
        .putInt("last_chapter", 1)
        .putInt("last_ayah", 3)
        .putString("last_verse_key", "1:3")
        .putString("reading_theme", "night")
        .putString("assistant_mode_v2", "offline")
        .putString("assistant_history_v1", legacy.toString())
        .putBoolean("reminder_enabled", false)
        .putBoolean("reminder_subuh", false)
        .putBoolean("reminder_dzuhur", true)
        .putBoolean("reminder_ashar", true)
        .putBoolean("reminder_maghrib", false)
        .putBoolean("reminder_isya", true)
        .commit();
    int levels =
        context.getResources().getIdentifier("study_levels", "array", context.getPackageName());
    if (levels != 0)
      p.edit()
          .putString("education_level", context.getResources().getStringArray(levels)[0])
          .commit();
    boolean modes;
    try {
      Class.forName("com.zikra.app.reminders.PrayerSettings", true, context.getClassLoader());
      modes = true;
    } catch (ClassNotFoundException e) {
      modes = false;
    }
    if (modes)
      p.edit()
          .putString("prayer_mode_subuh", "ADHAN")
          .putString("prayer_mode_dzuhur", "NOTIFICATION")
          .putString("prayer_mode_ashar", "VIBRATE")
          .putString("prayer_mode_maghrib", "OFF")
          .putString("prayer_mode_isya", "ADHAN")
          .commit();
    context
        .getSharedPreferences("zikra_location", 0)
        .edit()
        .putLong("latitude", Double.doubleToLongBits(-5.1477))
        .putLong("longitude", Double.doubleToLongBits(119.4327))
        .putString("zone", "Asia/Makassar")
        .putString("label", "Makassar DATA UJI")
        .putString("source", "manual")
        .putLong("selected_at", System.currentTimeMillis())
        .putFloat("accuracy", -1)
        .commit();
    context
        .getSharedPreferences("quran_audio_options", 0)
        .edit()
        .putString("reciter", "ar.husary")
        .putBoolean("follow", false)
        .putBoolean("loop", true)
        .putInt("repeats", 3)
        .commit();
    try (SQLiteOpenHelper h = helper("com.zikra.app.data.ZikraDbHelper")) {
      assertNotNull(h);
      SQLiteDatabase db = h.getWritableDatabase();
      String d = today();
      insert(
          db,
          "tasks",
          "id",
          41,
          "task_date",
          d,
          "title",
          "DATA UJI D06 tugas",
          "category",
          "Study",
          "completed",
          0,
          "due_at",
          System.currentTimeMillis() + 172800000L,
          "study_reminder",
          1,
          "subject",
          "Matematika",
          "priority",
          2,
          "advance_reminder",
          1);
      insert(
          db,
          "tasks",
          "id",
          42,
          "task_date",
          d,
          "title",
          "DATA UJI D06 Hifz",
          "category",
          "Hifz",
          "completed",
          1,
          "hifz_chapter",
          1,
          "hifz_name",
          "Al-Fatihah",
          "hifz_count",
          7,
          "hifz_first",
          2,
          "hifz_last",
          4);
      insert(
          db,
          "tasks",
          "id",
          43,
          "task_date",
          d,
          "title",
          "DATA UJI D06 ibadah",
          "category",
          "Ibadah",
          "completed",
          1);
      insert(
          db,
          "tasks",
          "id",
          44,
          "task_date",
          LocalDate.parse(d).minusDays(1).toString(),
          "title",
          "DATA UJI kemarin",
          "category",
          "Ibadah",
          "completed",
          1);
    }
    try (SQLiteOpenHelper h = helper("com.zikra.app.data.WeeklyClassStore")) {
      if (h != null)
        insert(
            h.getWritableDatabase(),
            "weekly_classes",
            "id",
            61,
            "revision",
            1,
            "title",
            "DATA UJI D06 kelas",
            "room",
            "R1",
            "teacher",
            "Pengajar uji",
            "weekday",
            LocalDate.parse(today()).plusDays(1).getDayOfWeek().getValue(),
            "start_minute",
            480,
            "end_minute",
            540,
            "reminder_minutes",
            15,
            "credits",
            3);
    }
    boolean chats = false;
    try (SQLiteOpenHelper h = helper("com.zikra.app.assistant.ConversationStore")) {
      if (h != null) {
        chats = true;
        insert(
            h.getWritableDatabase(),
            "conversations",
            "id",
            CHAT,
            "title",
            "DATA UJI D06 chat",
            "updated",
            System.currentTimeMillis(),
            "messages",
            legacy.toString(),
            "draft",
            "Draf uji tersimpan",
            "renamed",
            1);
      }
    }
    boolean faith = false;
    try (SQLiteOpenHelper h = helper("com.zikra.app.faith.FaithStore")) {
      if (h != null) {
        faith = true;
        SQLiteDatabase db = h.getWritableDatabase();
        insert(db, "plans", "id", 71, "start", today(), "days", 90, "active", 1);
        insert(
            db,
            "entries",
            "id",
            81,
            "token",
            "d0600000-0000-4000-8000-000000000081",
            "revision",
            1,
            "kind",
            2,
            "day",
            today(),
            "chapter",
            1,
            "first_verse",
            1,
            "last_verse",
            4,
            "note",
            "DATA UJI bacaan",
            "target_id",
            0,
            "plan_id",
            71);
        insert(
            db,
            "entries",
            "id",
            82,
            "token",
            "d0600000-0000-4000-8000-000000000082",
            "revision",
            1,
            "kind",
            1,
            "day",
            today(),
            "chapter",
            1,
            "first_verse",
            2,
            "last_verse",
            4,
            "note",
            "DATA UJI latihan",
            "target_id",
            42,
            "plan_id",
            0);
        insert(db, "day_notes", "day", today(), "note", "DATA UJI jurnal bertanggal");
      }
    }
    Object hadith = object("com.zikra.app.discovery.HadithStore");
    if (hadith != null) {
      Field field = hadith.getClass().getDeclaredField("helper");
      field.setAccessible(true);
      try (SQLiteOpenHelper h = (SQLiteOpenHelper) field.get(hadith)) {
        SQLiteDatabase db = h.getWritableDatabase();
        try (Cursor c = db.rawQuery("SELECT id FROM entries ORDER BY id LIMIT 1", null)) {
          if (c.moveToFirst()) {
            String id = c.getString(0);
            insert(db, "bookmarks", "id", id);
            p.edit().putStringSet("hadith_bookmarks", new HashSet<>(Arrays.asList(id))).commit();
          }
        }
      }
    }
    boolean encrypted = false;
    Object connection = object("com.zikra.app.assistant.AssistantConnection");
    if (connection != null) {
      connection
          .getClass()
          .getMethod("save", String.class, String.class)
          .invoke(connection, "https://d06.invalid/v1/assistant", ACCESS);
      connection.getClass().getMethod("offline").invoke(connection);
      context
          .getSharedPreferences("assistant_gateway", 0)
          .edit()
          .putBoolean("d06_fixture", true)
          .commit();
      encrypted = true;
    }
    File audio = file("quran-audio/ar.husary/1/1.mp3");
    assertTrue(audio.getParentFile().isDirectory() || audio.getParentFile().mkdirs());
    ByteBuffer wave = ByteBuffer.allocate(444).order(ByteOrder.LITTLE_ENDIAN);
    wave.put("RIFF".getBytes(StandardCharsets.US_ASCII))
        .putInt(436)
        .put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII))
        .putInt(16)
        .putShort((short) 1)
        .putShort((short) 1)
        .putInt(8000)
        .putInt(16000)
        .putShort((short) 2)
        .putShort((short) 16)
        .put("data".getBytes(StandardCharsets.US_ASCII))
        .putInt(400);
    Files.write(audio.toPath(), wave.array());
    File sentinel = file("d06-private-note.txt");
    Files.write(sentinel.toPath(), "DATA UJI file lokal".getBytes(StandardCharsets.UTF_8));
    JSONObject snapshot = snapshot();
    PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    snapshot
        .put("seedVersion", info.versionName)
        .put("seedVersionCode", info.versionCode)
        .put("hasChats", chats)
        .put("hasFaith", faith)
        .put("hasGateway", encrypted)
        .put("hasPrayerModes", modes);
    json(file("d06-snapshot.json"), snapshot);
    json(
        file("d06-seed-result.json"),
        new JSONObject()
            .put("version", info.versionName)
            .put("versionCode", info.versionCode)
            .put("databaseCount", snapshot.getJSONObject("databases").length())
            .put("privateFileCount", snapshot.getJSONObject("files").length())
            .put("hasChats", chats)
            .put("hasFaith", faith)
            .put("hasEncryptedGateway", encrypted)
            .put("hasPrayerModes", modes));
  }

  private JSONObject preferenceValue(Object value) throws Exception {
    Object normalized = value;
    if (value instanceof Set) {
      List<String> sorted = new ArrayList<>();
      for (Object s : (Set<?>) value) sorted.add(s.toString());
      Collections.sort(sorted);
      normalized = new JSONArray(sorted);
    }
    return new JSONObject().put("type", value.getClass().getSimpleName()).put("value", normalized);
  }

  private JSONObject snapshot() throws Exception {
    JSONObject out = new JSONObject(),
        prefs = new JSONObject(),
        databases = new JSONObject(),
        files = new JSONObject();
    for (String name : PREFS) {
      JSONObject values = new JSONObject();
      for (Map.Entry<String, ?> row : context.getSharedPreferences(name, 0).getAll().entrySet())
        values.put(row.getKey(), preferenceValue(row.getValue()));
      prefs.put(name, values);
    }
    for (String name : context.databaseList())
      if (!name.endsWith("-journal") && !name.endsWith("-wal") && !name.endsWith("-shm")) {
        File path = context.getDatabasePath(name);
        if (!path.isFile()) continue;
        try (SQLiteDatabase db =
            SQLiteDatabase.openDatabase(path.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
          JSONObject tables = new JSONObject();
          try (Cursor c =
              db.rawQuery(
                  "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
                      + " AND name<>'android_metadata' ORDER BY name",
                  null)) {
            while (c.moveToNext()) {
              String table = c.getString(0);
              try (Cursor rows = db.rawQuery("SELECT * FROM " + quoted(table), null)) {
                JSONArray names = new JSONArray(Arrays.asList(rows.getColumnNames())),
                    data = new JSONArray();
                while (rows.moveToNext()) data.put(row(rows));
                tables.put(table, new JSONObject().put("columns", names).put("rows", data));
              }
            }
          }
          databases.put(
              name, new JSONObject().put("version", db.getVersion()).put("tables", tables));
        }
      }
    for (String name : new String[] {"quran-audio/ar.husary/1/1.mp3", "d06-private-note.txt"})
      files.put(name, hash(Files.readAllBytes(file(name).toPath())));
    return out.put("preferences", prefs).put("databases", databases).put("files", files);
  }

  private JSONArray row(Cursor c) throws Exception {
    JSONArray row = new JSONArray();
    for (int i = 0; i < c.getColumnCount(); i++)
      row.put(
          c.isNull(i)
              ? JSONObject.NULL
              : c.getType(i) == Cursor.FIELD_TYPE_BLOB
                  ? Base64.encodeToString(c.getBlob(i), Base64.NO_WRAP)
                  : c.getString(i));
    return row;
  }

  public void testVerifyUpgradeAndRepeatedOpen() throws Exception {
    JSONObject expected = json(file("d06-snapshot.json"));
    try (SQLiteOpenHelper h = helper("com.zikra.app.data.ZikraDbHelper")) {
      assertEquals(5, h.getWritableDatabase().getVersion());
    }
    try (SQLiteOpenHelper h = helper("com.zikra.app.data.WeeklyClassStore")) {
      assertEquals(2, h.getWritableDatabase().getVersion());
    }
    try (SQLiteOpenHelper h = helper("com.zikra.app.assistant.ConversationStore")) {
      SQLiteDatabase db = h.getWritableDatabase();
      try (Cursor c =
          db.rawQuery("SELECT messages FROM conversations WHERE id='legacy-local'", null)) {
        assertTrue("Legacy history imported exactly once", c.moveToFirst());
        assertEquals(
            "DATA UJI D06 legacy",
            new JSONArray(c.getString(0)).getJSONObject(0).getString("text"));
        assertFalse(c.moveToNext());
      }
    }
    try (SQLiteOpenHelper h = helper("com.zikra.app.faith.FaithStore")) {
      assertEquals(1, h.getWritableDatabase().getVersion());
    }
    check(expected, false);
    for (int round = 0; round < 2; round++)
      for (String name :
          new String[] {
            "HomeActivity",
            "ProfileActivity",
            "StudyActivity",
            "QuranActivity",
            "AiActivity",
            "FaithActivity"
          }) {
        Activity page =
            getInstrumentation()
                .startActivitySync(
                    new Intent()
                        .setClassName(context.getPackageName(), "com.zikra.app." + name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getInstrumentation().waitForIdleSync();
        getInstrumentation().runOnMainSync(page::finish);
        getInstrumentation().waitForIdleSync();
      }
    check(expected, true);
    if (expected.getBoolean("hasGateway")) {
      Object c = object("com.zikra.app.assistant.AssistantConnection");
      assertEquals(ACCESS, c.getClass().getMethod("accessCode").invoke(c));
    }
    json(
        file("d06-verify-result.json"),
        new JSONObject()
            .put("seedVersion", expected.getString("seedVersion"))
            .put(
                "currentVersion",
                context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName)
            .put("originalRowsAndValuesPreserved", true)
            .put("originalPreferenceValuesPreserved", true)
            .put("durableFileHashesMatch", true)
            .put("legacyHistoryImportedOnce", true)
            .put("gatewayDecryptionAfterUpgrade", expected.getBoolean("hasGateway"))
            .put("databaseCountBefore", expected.getJSONObject("databases").length())
            .put("warmedActivities", 12)
            .put("faithWasPopulated", expected.getBoolean("hasFaith")));
  }

  public void testVerifyProcessRestart() throws Exception {
    JSONObject expected = json(file("d06-snapshot.json"));
    check(expected, true);
    if (expected.getBoolean("hasGateway")) {
      Object c = object("com.zikra.app.assistant.AssistantConnection");
      assertEquals(ACCESS, c.getClass().getMethod("accessCode").invoke(c));
    }
  }

  private void check(JSONObject expected, boolean warmed) throws Exception {
    JSONObject now = snapshot();
    JSONObject beforePrefs = expected.getJSONObject("preferences"),
        afterPrefs = now.getJSONObject("preferences");
    for (String name : PREFS) {
      JSONObject before = beforePrefs.getJSONObject(name), after = afterPrefs.getJSONObject(name);
      for (Iterator<String> it = before.keys(); it.hasNext(); ) {
        String key = it.next();
        assertEquals(
            "Preference retained: " + name + "/" + key,
            before.getJSONObject(key).toString(),
            after.getJSONObject(key).toString());
      }
    }
    JSONObject databases = expected.getJSONObject("databases");
    for (Iterator<String> it = databases.keys(); it.hasNext(); ) {
      String name = it.next();
      JSONObject tables = databases.getJSONObject(name).getJSONObject("tables");
      try (SQLiteDatabase db =
          SQLiteDatabase.openDatabase(
              context.getDatabasePath(name).getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
        for (Iterator<String> ts = tables.keys(); ts.hasNext(); ) {
          String table = ts.next();
          JSONObject old = tables.getJSONObject(table);
          JSONArray names = old.getJSONArray("columns");
          List<Integer> kept = new ArrayList<>();
          StringBuilder selection = new StringBuilder();
          for (int i = 0; i < names.length(); i++) {
            if (warmed
                && name.equals("assistant_chats.db")
                && table.equals("conversations")
                && names.getString(i).equals("updated")) continue;
            if (selection.length() > 0) selection.append(',');
            selection.append(quoted(names.getString(i)));
            kept.add(i);
          }
          List<String> actual = new ArrayList<>();
          try (Cursor c = db.rawQuery("SELECT " + selection + " FROM " + quoted(table), null)) {
            while (c.moveToNext()) actual.add(row(c).toString());
          }
          JSONArray rows = old.getJSONArray("rows");
          for (int i = 0; i < rows.length(); i++) {
            JSONArray projection = new JSONArray();
            for (int column : kept) projection.put(rows.getJSONArray(i).get(column));
            assertTrue(
                "Original row retained: " + name + "/" + table,
                actual.remove(projection.toString()));
          }
        }
      }
    }
    JSONObject files = expected.getJSONObject("files");
    for (Iterator<String> it = files.keys(); it.hasNext(); ) {
      String name = it.next();
      assertEquals(
          "File bytes retained: " + name,
          files.getString(name),
          hash(Files.readAllBytes(file(name).toPath())));
    }
  }
}
