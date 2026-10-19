package com.zikra.app.data;

import android.database.Cursor;
import android.test.AndroidTestCase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Runs against Android SQLite, using a dedicated database and deterministic local dates. */
@SuppressWarnings("deprecation")
public final class ZikraDbHelperTest extends AndroidTestCase {
  private static final String TEST_DATABASE = "zikra-instrumentation-test.db";
  private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
  private ZikraDbHelper db;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getContext().deleteDatabase(TEST_DATABASE);
    db = openAt("2026-10-15T05:00:00Z");
  }

  @Override
  protected void tearDown() throws Exception {
    db.close();
    getContext().deleteDatabase(TEST_DATABASE);
    super.tearDown();
  }

  public void testFreshInstallStartsEmptyAndCrudReflectsOnlyRealActions() {
    assertEquals(0, db.getTotalCount());
    assertEquals(0, db.getCompletedCount());
    assertEquals(0, db.getProgressPercent());
    long first = db.addTask("  Baca materi  ", ZikraDbHelper.CATEGORY_STUDY);
    long second = db.addTask("Rangkuman", ZikraDbHelper.CATEGORY_STUDY);
    assertEquals("Baca materi", find(first).title);
    assertTrue(db.setCompleted(first, true));
    assertEquals(50, db.getProgressPercent());
    assertTrue(db.updateTask(second, "Latihan soal", ZikraDbHelper.CATEGORY_STUDY));
    assertEquals("Latihan soal", find(second).title);
    assertTrue(db.deleteTask(first));
    assertEquals(1, db.getTotalCount());
    assertEquals(0, db.getCompletedCount());
    assertFalse(db.updateTask(first, "Missing", ZikraDbHelper.CATEGORY_STUDY));
  }

  public void testRelaunchAndNextDayNeverReseedAnEmptyList() {
    long id = db.addTask("Tugas yang dihapus", ZikraDbHelper.CATEGORY_STUDY);
    assertTrue(db.deleteTask(id));
    reopenAt("2026-10-15T13:00:00Z");
    assertEquals(0, db.getTasks().size());
    reopenAt("2026-10-15T17:01:00Z");
    assertEquals(0, db.getTasks().size());
    assertEquals(0, db.getTotalCount());
    assertEquals(0, db.getCompletedCount());
  }

  public void testAcademicAndHifzPersistAcrossLocalMidnightWithoutCopies() {
    long academic = db.addTask("Baca materi", ZikraDbHelper.CATEGORY_STUDY);
    long done = db.addTask("Catatan selesai", ZikraDbHelper.CATEGORY_STUDY);
    long hifz = db.addHifzTask("Murajaah", new HifzTarget(112, "Al-Ikhlas", 4, 1, 3));
    long ibadah = db.addTask("Sholat Dzuhur", ZikraDbHelper.CATEGORY_IBADAH);
    db.setCompleted(done, true);
    db.setCompleted(hifz, true);
    db.setCompleted(ibadah, true);
    reopenAt("2026-10-15T17:01:00Z"); // 00:01 next day in Jakarta.
    assertEquals(3, db.getTotalCount());
    assertEquals(2, db.getCompletedCount());
    assertEquals(67, db.getProgressPercent());
    assertTrue(find(done).completed);
    assertTrue(db.updateTask(academic, "Materi yang diperbarui", ZikraDbHelper.CATEGORY_STUDY));
    assertTrue(db.setCompleted(academic, true));
    assertTrue(db.updateHifzTask(hifz, "Catatan baru", new HifzTarget(112, "Al-Ikhlas", 4, 1, 3)));
    assertTrue(find(hifz).completed); // A note edit keeps the target completion.
    assertTrue(db.updateHifzTask(hifz, "Target baru", new HifzTarget(112, "Al-Ikhlas", 4, 1, 4)));
    assertFalse(find(hifz).completed); // Changing the requested range starts unfinished.
    assertEquals(2, db.getProgress(ZikraDbHelper.CATEGORY_STUDY).total);
    assertEquals(2, db.getProgress(ZikraDbHelper.CATEGORY_STUDY).completed);
    assertEquals(0, db.getProgress(ZikraDbHelper.CATEGORY_IBADAH).total);
    assertEquals(4, storedCount()); // Original IDs remain; no day-to-day copies.
    assertTrue(db.deleteTask(academic));
    assertEquals(3, storedCount());
    try (Cursor c =
        db.getReadableDatabase()
            .rawQuery(
                "SELECT task_date FROM tasks WHERE id=?", new String[] {Long.toString(hifz)})) {
      assertTrue(c.moveToFirst());
      assertEquals("2026-10-15", c.getString(0));
    }
  }

  public void testStaleDailyWorshipCannotClaimSavedOrAlterYesterday() {
    long id = db.addTask("Sholat Subuh", ZikraDbHelper.CATEGORY_IBADAH);
    db.setCompleted(id, true);
    reopenAt("2026-10-15T17:01:00Z");
    assertFalse(db.updateTask(id, "Changed", ZikraDbHelper.CATEGORY_IBADAH));
    assertFalse(db.updateHifzTask(id, "Changed", new HifzTarget(112, "Al-Ikhlas", 4, 1, 4)));
    assertFalse(db.setCompleted(id, false));
    assertFalse(db.deleteTask(id));
    assertEquals(0, db.getTotalCount());
    reopenAt("2026-10-15T05:00:00Z");
    assertEquals("Sholat Subuh", find(id).title);
    assertTrue(find(id).completed);
  }

  public void testPrayerTemplateRequiresExplicitActionAndAvoidsDuplicates() {
    assertEquals(0, db.getTasks().size());
    long existing = db.addTask("Sholat Subuh", ZikraDbHelper.CATEGORY_IBADAH);
    db.setCompleted(existing, true);
    assertEquals(4, db.addObligatoryPrayerChecklist());
    assertEquals(0, db.addObligatoryPrayerChecklist());
    assertEquals(5, db.getTotalCount());
    assertEquals(1, db.getCompletedCount()); // Existing user action is preserved.
    reopenAt("2026-10-15T17:01:00Z");
    assertEquals(0, db.getTotalCount());
    assertEquals(5, db.addObligatoryPrayerChecklist());
    assertEquals(5, db.getTotalCount());
    assertEquals(0, db.getCompletedCount());
    assertEquals(10, storedCount());
  }

  public void testVersionTwoLegacyRowsKeepIdsDatesCategoriesAndCompletion() {
    // This mirrors existing prototype data. A product fix must not erase or reclassify it.
    db.getWritableDatabase()
        .execSQL(
            "INSERT INTO tasks(id,task_date,title,category,completed) VALUES"
                + "(41,'2026-10-14','Legacy example','Study',1),"
                + "(42,'2026-10-14','Legacy ibadah','Ibadah',1)");
    reopenAt("2026-10-15T17:01:00Z");
    assertEquals(2, db.getReadableDatabase().getVersion());
    assertEquals(1, db.getTotalCount());
    assertEquals(41, find(41).id);
    assertEquals("Legacy example", find(41).title);
    assertTrue(find(41).completed);
    assertEquals(2, storedCount());
    try (Cursor c =
        db.getReadableDatabase()
            .rawQuery("SELECT task_date,category,completed FROM tasks WHERE id=42", null)) {
      assertTrue(c.moveToFirst());
      assertEquals("2026-10-14", c.getString(0));
      assertEquals(ZikraDbHelper.CATEGORY_IBADAH, c.getString(1));
      assertEquals(1, c.getInt(2));
    }
  }

  public void testInvalidInputDoesNotChangePlans() {
    try {
      db.addTask("   ", ZikraDbHelper.CATEGORY_STUDY);
      fail("Blank title was accepted");
    } catch (IllegalArgumentException expected) {
      assertEquals(0, db.getTotalCount());
    }
    try {
      db.addTask("Valid title", "Unknown");
      fail("Unknown category was accepted");
    } catch (IllegalArgumentException expected) {
      assertEquals(0, db.getTotalCount());
    }
  }

  private int storedCount() {
    try (Cursor c = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tasks", null)) {
      c.moveToFirst();
      return c.getInt(0);
    }
  }

  private ZikraDbHelper openAt(String instant) {
    return new ZikraDbHelper(
        getContext(), TEST_DATABASE, Clock.fixed(Instant.parse(instant), JAKARTA));
  }

  private void reopenAt(String instant) {
    db.close();
    db = openAt(instant);
  }

  private Task find(long id) {
    for (Task task : db.getTasks()) if (task.id == id) return task;
    throw new AssertionError("Missing task " + id);
  }
}
