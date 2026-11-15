package com.zikra.app.migration;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.test.InstrumentationTestCase;
import java.io.File;
import java.util.Map;
import java.util.List;
import org.json.JSONObject;

/** Explicitly selected read-only receipt. No content values, tokens, names or coordinates are emitted. */
public final class PhoneMigrationReceiptTest extends InstrumentationTestCase {
  public void testPhysicalFeatureThemesOnRestoredRelease() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    File marker = new File(context.getFilesDir(), "physical-beta-smoke-authorized");
    boolean existing = marker.exists();
    if (!existing) assertTrue(marker.createNewFile());
    try {
      com.zikra.app.PhysicalBetaSmokeTest smoke = new com.zikra.app.PhysicalBetaSmokeTest();
      smoke.injectInstrumentation(getInstrumentation());
      smoke.testThemesAndAiDockOnPreservedInstallation();
    } finally {
      if (!existing) assertTrue(marker.delete());
    }
  }

  public void testPersonalDataCountsAfterReleaseRestore() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    assertEquals("1.9.55", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
    assertFalse(LocalMigration.awaitingRestart(context));
    JSONObject counts = new JSONObject();
    for (Map.Entry<String, List<String>> spec : BackupPolicy.DATABASES.entrySet()) {
      File path = context.getDatabasePath(spec.getKey());
      if (!path.isFile()) continue;
      JSONObject tables = new JSONObject();
      try (SQLiteDatabase db = SQLiteDatabase.openDatabase(path.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
        try (Cursor integrity = db.rawQuery("PRAGMA quick_check", null)) {
          assertTrue(integrity.moveToFirst()); assertEquals("ok", integrity.getString(0));
        }
        for (String table : spec.getValue()) {
          try (Cursor rows = db.rawQuery("SELECT COUNT(*) FROM [" + table + "]", null)) {
            assertTrue(rows.moveToFirst()); tables.put(table, rows.getLong(0));
          }
        }
      }
      counts.put(spec.getKey(), tables);
    }
    Bundle result = new Bundle(); result.putString("personalTableCounts", counts.toString());
    result.putString("receiptVersion", "1.9.55-release");
    getInstrumentation().sendStatus(0, result);
  }
}
