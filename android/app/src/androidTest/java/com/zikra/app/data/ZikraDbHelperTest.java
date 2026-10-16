package com.zikra.app.data;

import android.test.AndroidTestCase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/** Runs against Android SQLite, using a dedicated database and deterministic dates. */
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

  public void testSeedAndCrudUseRealProgress() {
    assertEquals(24, db.getTotalCount());
    assertEquals(8, db.getCompletedCount());
    assertEquals(33, db.getProgressPercent());
    List<Task> tasks = db.getTasks();
    Task pending = tasks.get(8);
    db.setCompleted(pending.id, true);
    assertEquals(9, db.getCompletedCount());
    assertEquals(38, db.getProgressPercent());

    long newTask = db.addTask("  Laporan PemMob  ", ZikraDbHelper.CATEGORY_STUDY);
    assertEquals(25, db.getTotalCount());
    assertEquals("Laporan PemMob", find(newTask).title);
    db.updateTask(newTask, "Tilawah sore", ZikraDbHelper.CATEGORY_IBADAH);
    assertEquals("Tilawah sore", find(newTask).title);
    assertEquals(ZikraDbHelper.CATEGORY_IBADAH, find(newTask).category);
    db.setCompleted(newTask, true);
    assertEquals(40, db.getProgressPercent());
    db.deleteTask(newTask);
    assertEquals(24, db.getTotalCount());
    assertEquals(9, db.getCompletedCount());
  }

  public void testRelaunchPreservesEditsAndDoesNotReseedAnEmptyDay() {
    for (Task task : db.getTasks()) db.deleteTask(task.id);
    assertEquals(0, db.getTotalCount());
    assertEquals(0, db.getProgressPercent());
    db.close();
    db = openAt("2026-10-15T13:00:00Z");
    assertEquals(0, db.getTasks().size());
    long id = db.addTask("Tugas yang disimpan", ZikraDbHelper.CATEGORY_STUDY);
    db.setCompleted(id, true);
    db.close();
    db = openAt("2026-10-15T13:30:00Z");
    assertEquals(1, db.getTotalCount());
    assertEquals("Tugas yang disimpan", db.getTasks().get(0).title);
    assertEquals(100, db.getProgressPercent());
  }

  public void testLocalMidnightCreatesFreshDayAndRetainsPreviousHistory() {
    long yesterdayTask = db.getTasks().get(0).id;
    db.updateTask(yesterdayTask, "Catatan kemarin", ZikraDbHelper.CATEGORY_IBADAH);
    db.close();
    // 17:01 UTC is 00:01 on the next day in Jakarta.
    db = openAt("2026-10-15T17:01:00Z");
    assertEquals(24, db.getTotalCount());
    assertEquals(0, db.getCompletedCount());
    db.deleteTask(yesterdayTask); // A stale UI ID must never modify another date.
    db.setCompleted(yesterdayTask, false);
    db.close();
    db = openAt("2026-10-15T05:00:00Z");
    assertEquals(24, db.getTotalCount());
    assertEquals(8, db.getCompletedCount());
    assertEquals("Catatan kemarin", find(yesterdayTask).title);
  }

  public void testInvalidInputDoesNotChangeChecklist() {
    int initial = db.getTotalCount();
    try {
      db.addTask("   ", ZikraDbHelper.CATEGORY_STUDY);
      fail("Blank title was accepted");
    } catch (IllegalArgumentException expected) {
      assertEquals(initial, db.getTotalCount());
    }
    try {
      db.addTask("Valid title", "Unknown");
      fail("Unknown category was accepted");
    } catch (IllegalArgumentException expected) {
      assertEquals(initial, db.getTotalCount());
    }
  }

  public void testHifzCategorySharesTheDailyProgress() {
    int hafalan = 0;
    for (Task task : db.getTasks())
      if (ZikraDbHelper.CATEGORY_HIFZ.equals(task.category)) hafalan++;
    assertEquals(2, hafalan);
    long newTarget = db.addTask("Hafalan Al-Mulk ayat 1–5", ZikraDbHelper.CATEGORY_HIFZ);
    assertEquals(25, db.getTotalCount());
    db.setCompleted(newTarget, true);
    assertEquals(9, db.getCompletedCount());
    assertEquals(36, db.getProgressPercent());
    assertEquals(ZikraDbHelper.CATEGORY_HIFZ, find(newTarget).category);
  }

  private ZikraDbHelper openAt(String instant) {
    return new ZikraDbHelper(
        getContext(), TEST_DATABASE, Clock.fixed(Instant.parse(instant), JAKARTA));
  }

  private Task find(long id) {
    for (Task task : db.getTasks()) if (task.id == id) return task;
    throw new AssertionError("Missing task " + id);
  }
}
