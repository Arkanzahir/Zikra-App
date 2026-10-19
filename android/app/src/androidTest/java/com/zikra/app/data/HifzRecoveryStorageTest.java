package com.zikra.app.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@SuppressWarnings("deprecation")
public final class HifzRecoveryStorageTest extends AndroidTestCase {
  private static final String NAME = "hifz-recovery-test.db";
  private static final Clock TODAY =
      Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Jakarta"));
  private ZikraDbHelper db;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getContext().deleteDatabase(NAME);
  }

  @Override
  protected void tearDown() throws Exception {
    if (db != null) db.close();
    getContext().deleteDatabase(NAME);
    super.tearDown();
  }

  private void open() {
    db = new ZikraDbHelper(getContext(), NAME, TODAY);
  }

  private Task find(long id) {
    for (Task task : db.getTasks()) if (task.id == id) return task;
    throw new AssertionError("Missing task");
  }

  public void testVersionOneMigrationKeepsDatesIdsAndCompletion() {
    SQLiteDatabase old = getContext().openOrCreateDatabase(NAME, 0, null);
    old.execSQL("CREATE TABLE checklist_days(task_date TEXT PRIMARY KEY NOT NULL)");
    old.execSQL(
        "CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,task_date TEXT NOT NULL,title TEXT"
            + " NOT NULL,category TEXT NOT NULL,completed INTEGER NOT NULL DEFAULT 0)");
    old.execSQL("CREATE INDEX tasks_date ON tasks(task_date,id)");
    old.execSQL("INSERT INTO checklist_days VALUES('2026-10-17'),('2026-10-18')");
    old.execSQL(
        "INSERT INTO tasks VALUES(41,'2026-10-17','Riwayat','Hifz',1),(42,'2026-10-18','Target"
            + " lama','Hifz',1),(43,'2026-10-18','Tugas','Study',0)");
    old.setVersion(1);
    old.close();
    open();
    assertEquals(3, db.getTotalCount());
    assertEquals(2, db.getCompletedCount());
    assertNull(find(42).hifz);
    assertTrue(find(42).completed);
    try (Cursor cursor =
        db.getReadableDatabase().rawQuery("SELECT title,completed FROM tasks WHERE id=41", null)) {
      assertTrue(cursor.moveToFirst());
      assertEquals("Riwayat", cursor.getString(0));
      assertEquals(1, cursor.getInt(1));
    }
    assertEquals(2, db.getReadableDatabase().getVersion());
    db.close();
    open();
    assertEquals(3, db.getTotalCount());
  }

  public void testRangePersistsAndNewRangeResetsCompletionButNoteEditDoesNot() {
    open();
    HifzTarget target = new HifzTarget(112, "Al-Ikhlas", 4, 1, 3);
    long id = db.addHifzTask("Catatan", target);
    db.setCompleted(id, true);
    db.close();
    open();
    assertEquals(3, find(id).hifz.count());
    assertTrue(find(id).completed);
    db.updateHifzTask(id, "Catatan baru", target);
    assertTrue(find(id).completed);
    db.updateHifzTask(id, "Empat ayat", new HifzTarget(112, "Al-Ikhlas", 4, 1, 4));
    assertFalse(find(id).completed);
    db.updateTask(id, "Belajar", ZikraDbHelper.CATEGORY_STUDY);
    assertNull(find(id).hifz);
    db.deleteTask(id);
    assertEquals(0, db.getTotalCount());
  }

  public void testInvalidBoundsAndPriorDateTargetsRemainEditable() {
    open();
    int size = db.getTotalCount();
    for (int[] bounds : new int[][] {{0, 2}, {3, 2}, {1, 5}}) {
      try {
        db.addHifzTask("Salah", new HifzTarget(112, "Al-Ikhlas", 4, bounds[0], bounds[1]));
        fail("Invalid range accepted");
      } catch (IllegalArgumentException expected) {
        assertEquals(size, db.getTotalCount());
      }
    }
    long id = db.addHifzTask("Kemarin", new HifzTarget(112, "Al-Ikhlas", 4, 1, 4));
    db.close();
    db =
        new ZikraDbHelper(
            getContext(),
            NAME,
            Clock.fixed(Instant.parse("2026-10-19T03:00:00Z"), ZoneId.of("Asia/Jakarta")));
    assertTrue(db.updateHifzTask(id, "Beda hari", new HifzTarget(113, "Al-Falaq", 5, 1, 5)));
    db.close();
    open();
    assertEquals("Beda hari", find(id).title);
    assertEquals(113, find(id).hifz.chapterId);
  }
}
