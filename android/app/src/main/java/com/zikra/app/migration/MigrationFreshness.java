package com.zikra.app.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/** Restore is intentionally unavailable over an installation containing personal records. */
final class MigrationFreshness {
  private MigrationFreshness() {}
  static boolean empty(Context context) {
    try {
      SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
      if (!prefs.getString("profile_display_name", "").trim().isEmpty() || prefs.contains("last_chapter")
          || !prefs.getStringSet("hadith_bookmarks", Collections.emptySet()).isEmpty()
          || !prefs.getString("assistant_history_v1", "[]").trim().equals("[]")
          || context.getSharedPreferences("zikra_location", 0).contains("latitude")) return false;
      for (Map.Entry<String, List<String>> spec : BackupPolicy.DATABASES.entrySet()) {
        File file = context.getDatabasePath(spec.getKey()); if (!file.exists()) continue;
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
          for (String table : spec.getValue()) {
            if (table.equals("metadata") || table.equals("checklist_days")) continue;
            String where = table.equals("conversations") ? " WHERE messages<>'[]' OR draft<>'' OR renamed<>0" : "";
            try (Cursor rows = db.rawQuery("SELECT COUNT(*) FROM \"" + table + "\"" + where, null)) {
              if (!rows.moveToFirst() || rows.getLong(0) != 0) return false;
            }
          }
          if (spec.getKey().equals("zikra_hadith.db")) try (Cursor packages = db.rawQuery("SELECT COUNT(*) FROM packages", null)) {
            if (!packages.moveToFirst() || packages.getLong(0) > 0) return false;
          }
        }
      }
      return !hasFiles(new File(context.getFilesDir(), "assistant_attachments"), 0)
          && !hasFiles(new File(context.getFilesDir(), "assistant_images"), 0);
    } catch (Exception uncertain) { return false; }
  }
  private static boolean hasFiles(File file, int depth) {
    if (depth > 4 || Files.isSymbolicLink(file.toPath())) return true;
    if (!file.exists()) return false;
    if (!file.isDirectory()) return true;
    File[] children = file.listFiles(); if (children == null) return true;
    for (File child : children) if (hasFiles(child, depth + 1)) return true;
    return false;
  }
}
