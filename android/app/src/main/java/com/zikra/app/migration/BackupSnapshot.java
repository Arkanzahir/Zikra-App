package com.zikra.app.migration;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.util.JsonReader;
import android.util.JsonToken;
import com.zikra.app.assistant.ConversationStore;
import com.zikra.app.data.*;
import com.zikra.app.discovery.HadithStore;
import com.zikra.app.faith.FaithStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;

/** Logical personal records only. Source SQL, credentials, content downloads and OS grants are never imported. */
public final class BackupSnapshot {
  private BackupSnapshot() {}
  private static final class Budget {
    int nodes, bytes;
    void take(String value) throws IOException {
      if (value.length() > 128000 || ++nodes > 100000) throw new IOException("Backup snapshot exceeds budget");
      bytes += 32 + value.getBytes(StandardCharsets.UTF_8).length;
      if (bytes > BackupPolicy.SNAPSHOT_BYTES) throw new IOException("Backup snapshot exceeds budget");
    }
  }
  private static void keys(JSONObject value, String... expected) throws IOException {
    Set<String> actual = new HashSet<>(); Iterator<String> names = value.keys();
    while (names.hasNext()) actual.add(names.next());
    if (!actual.equals(new HashSet<>(Arrays.asList(expected)))) throw new IOException("Unsupported backup fields");
  }
  private static void scan(JsonReader input, Budget budget, int depth) throws IOException {
    if (depth > 12) throw new IOException("Backup nesting exceeds budget");
    JsonToken token = input.peek(); budget.take("");
    if (token == JsonToken.BEGIN_OBJECT) {
      input.beginObject(); Set<String> names = new HashSet<>();
      while (input.hasNext()) { String name = input.nextName(); budget.take(name);
        if (!names.add(name)) throw new IOException("Duplicate backup field"); scan(input, budget, depth + 1); }
      input.endObject();
    } else if (token == JsonToken.BEGIN_ARRAY) {
      input.beginArray(); while (input.hasNext()) scan(input, budget, depth + 1); input.endArray();
    } else if (token == JsonToken.NULL) input.nextNull();
    else if (token == JsonToken.BOOLEAN) input.nextBoolean();
    else if (token == JsonToken.STRING || token == JsonToken.NUMBER) budget.take(input.nextString());
    else throw new IOException("Invalid backup snapshot");
  }
  public static JSONObject read(File snapshot) throws Exception {
    if (!snapshot.isFile() || snapshot.length() < 2 || snapshot.length() > BackupPolicy.SNAPSHOT_BYTES)
      throw new IOException("Backup snapshot size is invalid");
    try (JsonReader input = new JsonReader(new InputStreamReader(new FileInputStream(snapshot), StandardCharsets.UTF_8))) {
      scan(input, new Budget(), 0);
      if (input.peek() != JsonToken.END_DOCUMENT) throw new IOException("Trailing backup content");
    }
    JSONObject data = new JSONObject(new String(Files.readAllBytes(snapshot.toPath()), StandardCharsets.UTF_8));
    keys(data, "schema", "versionCode", "createdAt", "databases", "preferences");
    if (!(data.get("schema") instanceof Integer) || data.getInt("schema") != 1) throw new IOException("Unsupported backup schema");
    return data;
  }
  public static void export(Context context, File output) throws Exception {
    // Preserve legacy local chats through the same idempotent migration used when opening AI.
    try (ConversationStore conversations = new ConversationStore(context)) { conversations.getReadableDatabase(); }
    Budget budget = new Budget(); JSONObject databases = new JSONObject(), preferences = new JSONObject(); int rows = 0;
    for (Map.Entry<String, List<String>> spec : BackupPolicy.DATABASES.entrySet()) {
      File path = context.getDatabasePath(spec.getKey());
      if (!path.isFile()) { databases.put(spec.getKey(), JSONObject.NULL); continue; }
      try (SQLiteDatabase db = SQLiteDatabase.openDatabase(path.getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
        db.beginTransactionNonExclusive();
        try {
          JSONObject tables = new JSONObject();
          for (String table : spec.getValue()) {
            JSONArray columns = columns(db, table), values = new JSONArray();
            try (Cursor cursor = db.query(table, null, null, null, null, null, "rowid")) {
              while (cursor.moveToNext()) {
                if (++rows > BackupPolicy.MAX_ROWS) throw new IOException("Too many personal records");
                JSONArray row = new JSONArray();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                  Object value;
                  switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_NULL: value = JSONObject.NULL; break;
                    case Cursor.FIELD_TYPE_INTEGER: value = cursor.getLong(i); break;
                    case Cursor.FIELD_TYPE_FLOAT: value = cursor.getDouble(i); break;
                    case Cursor.FIELD_TYPE_STRING: value = cursor.getString(i); break;
                    default: throw new IOException("Unsupported personal record type");
                  }
                  budget.take(value.toString()); row.put(value);
                }
                values.put(row);
              }
            }
            tables.put(table, new JSONObject().put("columns", columns).put("rows", values));
          }
          JSONObject sequences = new JSONObject();
          for (String table : spec.getValue()) if (autoIncrement(db, table)) {
            try (Cursor sequence = db.rawQuery("SELECT seq FROM sqlite_sequence WHERE name=?", new String[]{table})) {
              if (sequence.moveToFirst()) sequences.put(table, sequence.getLong(0));
            }
          }
          databases.put(spec.getKey(), new JSONObject().put("version", db.getVersion()).put("tables", tables).put("sequences", sequences));
        } finally { db.endTransaction(); }
      }
    }
    for (String group : BackupPolicy.preferenceGroups()) {
      JSONObject values = new JSONObject();
      for (Map.Entry<String, ?> entry : new TreeMap<>(context.getSharedPreferences(group, 0).getAll()).entrySet()) {
        if (!BackupPolicy.preferenceAllowed(group, entry.getKey())) continue;
        String type = BackupPolicy.preferenceType(group, entry.getKey()); Object value = entry.getValue();
        if (value instanceof Set) {
          Set<String> ordered = new TreeSet<>();
          for (Object item : (Set<?>) value) { if (!(item instanceof String)) throw new IOException("Invalid selection"); ordered.add((String) item); }
          value = new JSONArray(ordered);
        }
        budget.take(entry.getKey()); budget.take(value.toString());
        values.put(entry.getKey(), new JSONObject().put("type", type).put("value", value));
      }
      preferences.put(group, values);
    }
    JSONObject snapshot = new JSONObject().put("schema", 1).put("versionCode",
        context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode)
        .put("createdAt", System.currentTimeMillis()).put("databases", databases).put("preferences", preferences);
    byte[] bytes = snapshot.toString().getBytes(StandardCharsets.UTF_8);
    if (bytes.length > BackupPolicy.SNAPSHOT_BYTES) throw new IOException("Backup snapshot exceeds budget");
    Files.write(output.toPath(), bytes, java.nio.file.StandardOpenOption.CREATE_NEW);
  }
  private static JSONArray columns(SQLiteDatabase db, String table) throws Exception {
    JSONArray result = new JSONArray();
    try (Cursor cursor = db.rawQuery("PRAGMA table_info(\"" + table + "\")", null)) {
      while (cursor.moveToNext()) result.put(cursor.getString(1));
    }
    if (result.length() == 0) throw new IOException("Personal table missing"); return result;
  }
  private static SQLiteOpenHelper schema(Context context, String name) throws IOException {
    switch (name) {
      case "zikra.db": return new ZikraDbHelper(context);
      case "study-schedule.db": return new WeeklyClassStore(context);
      case "assistant_chats.db": return new ConversationStore(context);
      case "faith.db": return new FaithStore(context);
      case "zikra_hadith.db": return HadithStore.migrationSchema(context);
      default: throw new IOException("Unknown personal database");
    }
  }
  private static boolean autoIncrement(SQLiteDatabase db, String table) {
    try (Cursor sql = db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", new String[]{table})) {
      return sql.moveToFirst() && sql.getString(0).toUpperCase(Locale.ROOT).contains("AUTOINCREMENT");
    }
  }
  static int restoreDatabases(MigrationStageContext isolated, JSONObject data) throws Exception {
    JSONObject databases = data.getJSONObject("databases"); keys(databases, BackupPolicy.DATABASES.keySet().toArray(new String[0]));
    int rows = 0;
    for (Map.Entry<String, List<String>> spec : BackupPolicy.DATABASES.entrySet()) {
      if (databases.isNull(spec.getKey())) continue;
      JSONObject saved = databases.getJSONObject(spec.getKey()); keys(saved, "version", "tables", "sequences");
      try (SQLiteOpenHelper helper = schema(isolated, spec.getKey())) {
        SQLiteDatabase db = helper.getWritableDatabase();
        if (saved.getInt("version") != db.getVersion()) throw new IOException("Unsupported personal database version");
        JSONObject tables = saved.getJSONObject("tables"); keys(tables, spec.getValue().toArray(new String[0]));
        db.execSQL("PRAGMA synchronous=FULL");
        db.beginTransaction();
        try {
          for (String table : spec.getValue()) {
            JSONObject savedTable = tables.getJSONObject(table); keys(savedTable, "columns", "rows");
            JSONArray expected = columns(db, table), imported = savedTable.getJSONArray("columns");
            if (!expected.toString().equals(imported.toString())) throw new IOException("Personal schema mismatch");
            db.delete(table, null, null); JSONArray entries = savedTable.getJSONArray("rows");
            for (int n = 0; n < entries.length(); n++) {
              if (++rows > BackupPolicy.MAX_ROWS) throw new IOException("Too many personal records");
              JSONArray row = entries.getJSONArray(n);
              if (row.length() != expected.length()) throw new IOException("Personal record width mismatch");
              ContentValues values = new ContentValues();
              for (int column = 0; column < row.length(); column++) {
                String key = expected.getString(column); Object value = row.get(column);
                if (value == JSONObject.NULL) values.putNull(key);
                else if (value instanceof String) values.put(key, (String) value);
                else if (value instanceof Integer || value instanceof Long) values.put(key, ((Number) value).longValue());
                else if (value instanceof Number && Double.isFinite(((Number) value).doubleValue())) values.put(key, ((Number) value).doubleValue());
                else throw new IOException("Unsupported personal record value");
              }
              if (table.equals("conversations")) {
                if (!values.getAsString("id").matches("(?:legacy-local|[a-f0-9-]{36})")) throw new IOException("Invalid conversation identifier");
                ConversationStore.validate(new JSONArray(values.getAsString("messages")));
              }
              db.insertOrThrow(table, null, values);
            }
          }
          JSONObject sequences = saved.getJSONObject("sequences"); Iterator<String> sequenceNames = sequences.keys();
          while (sequenceNames.hasNext()) {
            String name = sequenceNames.next(); Object raw = sequences.get(name);
            if (!spec.getValue().contains(name) || !autoIncrement(db, name) || !(raw instanceof Number))
              throw new IOException("Invalid personal sequence");
            long value = sequences.getLong(name);
            if (value < 0 || value == Long.MAX_VALUE || ((Number) raw).doubleValue() != value)
              throw new IOException("Invalid personal sequence");
            try (Cursor current = db.rawQuery("SELECT seq FROM sqlite_sequence WHERE name=?", new String[]{name})) {
              if (current.moveToFirst() && current.getLong(0) > value) throw new IOException("Personal sequence moved backwards");
            }
            db.delete("sqlite_sequence", "name=?", new String[]{name});
            ContentValues sequence = new ContentValues(); sequence.put("name", name); sequence.put("seq", value);
            db.insertOrThrow("sqlite_sequence", null, sequence);
          }
          try (Cursor integrity = db.rawQuery("PRAGMA quick_check", null)) {
            if (!integrity.moveToFirst() || !"ok".equals(integrity.getString(0))) throw new IOException("Personal database integrity failed");
          }
          db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
      }
    }
    return rows;
  }
  private static Object preference(String group, String name, JSONObject entry) throws Exception {
    if (!BackupPolicy.preferenceAllowed(group, name)) throw new IOException("Preference is not transferable");
    keys(entry, "type", "value");
    String type = BackupPolicy.preferenceType(group, name);
    if (!type.equals(entry.getString("type"))) throw new IOException("Preference type mismatch");
    Object value = entry.get("value");
    if (type.equals("string") && value instanceof String) return value;
    if (type.equals("boolean") && value instanceof Boolean) return value;
    if (type.equals("int") || type.equals("long")) {
      if (!(value instanceof Integer || value instanceof Long)) throw new IOException("Invalid integer preference");
      long number = ((Number) value).longValue();
      if (type.equals("long")) return number;
      if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) return (int) number;
    }
    if (type.equals("float") && value instanceof Number && Float.isFinite(((Number) value).floatValue()))
      return ((Number) value).floatValue();
    if (type.equals("set") && value instanceof JSONArray && ((JSONArray) value).length() <= 20000) {
      Set<String> result = new TreeSet<>(); JSONArray items = (JSONArray) value;
      for (int i = 0; i < items.length(); i++) {
        Object item = items.get(i);
        if (!(item instanceof String) || ((String) item).length() > 256 || !result.add((String) item))
          throw new IOException("Invalid saved selection");
      }
      return result;
    }
    throw new IOException("Invalid preference value");
  }
  private static Map<String, Object> preferences(String group, JSONObject data) throws Exception {
    JSONObject groups = data.getJSONObject("preferences");
    keys(groups, BackupPolicy.preferenceGroups().toArray(new String[0]));
    JSONObject saved = groups.getJSONObject(group); Map<String, Object> values = new TreeMap<>();
    Iterator<String> names = saved.keys();
    while (names.hasNext()) { String name = names.next(); values.put(name, preference(group, name, saved.getJSONObject(name))); }
    if (group.equals("zikra_preferences")) values.put("reminder_enabled", false);
    return values;
  }
  static void restorePreferences(MigrationStageContext isolated, JSONObject data) throws Exception {
    File folder = new File(isolated.root, "preferences");
    if (!folder.mkdir()) throw new IOException("Fresh preference staging required");
    for (String group : BackupPolicy.preferenceGroups()) {
      Map<String, Object> values = preferences(group, data);
      try (FileOutputStream output = new FileOutputStream(new File(folder, group + ".xml"))) {
        org.xmlpull.v1.XmlSerializer xml = android.util.Xml.newSerializer(); xml.setOutput(output, "UTF-8");
        xml.startDocument("UTF-8", true); xml.startTag(null, "map");
        for (Map.Entry<String, Object> entry : values.entrySet()) {
          String name = entry.getKey(); Object value = entry.getValue();
          String type = name.equals("reminder_enabled") ? "boolean" : BackupPolicy.preferenceType(group, name);
          xml.startTag(null, type).attribute(null, "name", name);
          if (value instanceof String) xml.text((String) value);
          else if (value instanceof Set) for (Object item : (Set<?>) value)
            xml.startTag(null, "string").text((String) item).endTag(null, "string");
          else xml.attribute(null, "value", value.toString());
          xml.endTag(null, type);
        }
        xml.endTag(null, "map"); xml.endDocument(); xml.flush(); output.getFD().sync();
      }
    }
  }
}
