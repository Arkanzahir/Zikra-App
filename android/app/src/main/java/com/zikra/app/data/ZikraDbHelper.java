package com.zikra.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Local planning storage. Academic tasks and Hifz targets remain accessible across dates; daily
 * ibadah is scoped to the local calendar day. Existing records and completion state are retained.
 * New days start empty: only explicit user actions create tasks or prayer templates.
 */
public final class ZikraDbHelper extends SQLiteOpenHelper {
  private static final int VERSION = 2;
  public static final String CATEGORY_STUDY = "Study";
  public static final String CATEGORY_IBADAH = "Ibadah";
  public static final String CATEGORY_HIFZ = "Hifz";
  private final Clock clock;

  private static final String ACTIVE_TASKS = "(category IN ('Study','Hifz') OR task_date = ?)";
  private static final String[] OBLIGATORY_PRAYERS = {
    "Sholat Subuh", "Sholat Dzuhur", "Sholat Ashar", "Sholat Maghrib", "Sholat Isya"
  };

  public ZikraDbHelper(Context context) {
    this(context, "zikra.db", Clock.systemDefaultZone());
  }

  /** Allows a separate database and clock for deterministic instrumented tests. */
  public ZikraDbHelper(Context context, String databaseName, Clock clock) {
    super(context.getApplicationContext(), databaseName, null, VERSION);
    if (clock == null) throw new IllegalArgumentException("Clock is required");
    this.clock = clock;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE checklist_days (task_date TEXT PRIMARY KEY NOT NULL)");
    db.execSQL(
        "CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "task_date TEXT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL "
            + "CHECK(category IN ('Study','Ibadah','Hifz')), completed INTEGER NOT NULL DEFAULT 0 "
            + "CHECK(completed IN (0,1)))");
    db.execSQL("CREATE INDEX tasks_date ON tasks(task_date, id)");
    addHifzColumns(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion == 1 && newVersion == 2) {
      addHifzColumns(db);
      return;
    }
    throw new IllegalStateException(
        "Unsupported database migration: " + oldVersion + " to " + newVersion);
  }

  private static void addHifzColumns(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE tasks ADD COLUMN hifz_chapter INTEGER");
    db.execSQL("ALTER TABLE tasks ADD COLUMN hifz_name TEXT");
    db.execSQL("ALTER TABLE tasks ADD COLUMN hifz_count INTEGER");
    db.execSQL("ALTER TABLE tasks ADD COLUMN hifz_first INTEGER");
    db.execSQL("ALTER TABLE tasks ADD COLUMN hifz_last INTEGER");
  }

  public List<Task> getTasks() {
    String day = today();
    SQLiteDatabase db = databaseForDay(day);
    List<Task> tasks = new ArrayList<>();
    try (Cursor cursor =
        db.query(
            "tasks",
            new String[] {
              "id",
              "title",
              "category",
              "completed",
              "hifz_chapter",
              "hifz_name",
              "hifz_count",
              "hifz_first",
              "hifz_last"
            },
            ACTIVE_TASKS,
            new String[] {day},
            null,
            null,
            "id ASC")) {
      while (cursor.moveToNext()) {
        tasks.add(
            new Task(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getInt(3) == 1,
                readTarget(cursor)));
      }
    }
    return tasks;
  }

  public long addTask(String title, String category) {
    String day = today();
    SQLiteDatabase db = databaseForDay(day);
    ContentValues values = taskValues(title, category);
    values.put("task_date", day);
    values.put("completed", 0);
    return db.insertOrThrow("tasks", null, values);
  }

  public boolean updateTask(long id, String title, String category) {
    String day = today();
    ContentValues values = taskValues(title, category);
    if (!CATEGORY_HIFZ.equals(category)) clearTarget(values);
    return databaseForDay(day)
            .update(
                "tasks",
                values,
                "id = ? AND " + ACTIVE_TASKS,
                new String[] {Long.toString(id), day})
        == 1;
  }

  public long addHifzTask(String title, HifzTarget target) {
    if (target == null) throw new IllegalArgumentException("Target wajib diisi");
    ContentValues values = taskValues(title, CATEGORY_HIFZ);
    putTarget(values, target);
    String day = today();
    SQLiteDatabase database = databaseForDay(day);
    values.put("task_date", day);
    values.put("completed", 0);
    return database.insertOrThrow("tasks", null, values);
  }

  public boolean updateHifzTask(long id, String title, HifzTarget target) {
    if (target == null) throw new IllegalArgumentException("Target wajib diisi");
    ContentValues values = taskValues(title, CATEGORY_HIFZ);
    putTarget(values, target);
    String day = today();
    SQLiteDatabase database = databaseForDay(day);
    database.beginTransaction();
    try {
      try (Cursor old =
          database.rawQuery(
              "SELECT hifz_chapter,hifz_first,hifz_last FROM tasks WHERE id=? AND " + ACTIVE_TASKS,
              new String[] {Long.toString(id), day})) {
        if (!old.moveToFirst()) return false;
        if (old.isNull(0)
            || old.getInt(0) != target.chapterId
            || old.getInt(1) != target.firstVerse
            || old.getInt(2) != target.lastVerse) values.put("completed", 0);
      }
      boolean updated =
          database.update(
                  "tasks",
                  values,
                  "id=? AND " + ACTIVE_TASKS,
                  new String[] {Long.toString(id), day})
              == 1;
      database.setTransactionSuccessful();
      return updated;
    } finally {
      database.endTransaction();
    }
  }

  private static void putTarget(ContentValues values, HifzTarget target) {
    values.put("hifz_chapter", target.chapterId);
    values.put("hifz_name", target.chapterName);
    values.put("hifz_count", target.versesCount);
    values.put("hifz_first", target.firstVerse);
    values.put("hifz_last", target.lastVerse);
  }

  private static void clearTarget(ContentValues values) {
    for (String key :
        new String[] {"hifz_chapter", "hifz_name", "hifz_count", "hifz_first", "hifz_last"})
      values.putNull(key);
  }

  private static HifzTarget readTarget(Cursor cursor) {
    if (cursor.isNull(4) || !CATEGORY_HIFZ.equals(cursor.getString(2))) return null;
    try {
      return new HifzTarget(
          cursor.getInt(4),
          cursor.getString(5),
          cursor.getInt(6),
          cursor.getInt(7),
          cursor.getInt(8));
    } catch (IllegalArgumentException invalid) {
      return null;
    }
  }

  public boolean setCompleted(long id, boolean completed) {
    String day = today();
    ContentValues values = new ContentValues();
    values.put("completed", completed ? 1 : 0);
    return databaseForDay(day)
            .update(
                "tasks",
                values,
                "id = ? AND " + ACTIVE_TASKS,
                new String[] {Long.toString(id), day})
        == 1;
  }

  public boolean deleteTask(long id) {
    String day = today();
    return databaseForDay(day)
            .delete("tasks", "id = ? AND " + ACTIVE_TASKS, new String[] {Long.toString(id), day})
        == 1;
  }

  /** Explicit, unchecked prayer template. Repeated taps never duplicate today's matching titles. */
  public int addObligatoryPrayerChecklist() {
    String day = today();
    SQLiteDatabase database = databaseForDay(day);
    int inserted = 0;
    database.beginTransaction();
    try {
      for (String title : OBLIGATORY_PRAYERS) {
        boolean exists;
        try (Cursor cursor =
            database.rawQuery(
                "SELECT 1 FROM tasks WHERE task_date=? AND category=? "
                    + "AND LOWER(TRIM(title))=LOWER(?) LIMIT 1",
                new String[] {day, CATEGORY_IBADAH, title})) {
          exists = cursor.moveToFirst();
        }
        if (exists) continue;
        ContentValues values = taskValues(title, CATEGORY_IBADAH);
        values.put("task_date", day);
        values.put("completed", 0);
        database.insertOrThrow("tasks", null, values);
        inserted++;
      }
      database.setTransactionSuccessful();
      return inserted;
    } finally {
      database.endTransaction();
    }
  }

  public int getCompletedCount() {
    return getProgress().completed;
  }

  public int getTotalCount() {
    return getProgress().total;
  }

  public int getProgressPercent() {
    return getProgress().percent;
  }

  /** Numerator and denominator use the same active planning snapshot. */
  public Progress getProgress() {
    return queryProgress(null);
  }

  /** Category-specific active progress, without mixing academic plans and daily worship. */
  public Progress getProgress(String category) {
    if (!CATEGORY_STUDY.equals(category)
        && !CATEGORY_IBADAH.equals(category)
        && !CATEGORY_HIFZ.equals(category)) throw new IllegalArgumentException("Unknown category");
    return queryProgress(category);
  }

  private Progress queryProgress(String category) {
    String day = today();
    String where = ACTIVE_TASKS + (category == null ? "" : " AND category=?");
    String[] args = category == null ? new String[] {day} : new String[] {day, category};
    try (Cursor cursor =
        databaseForDay(day)
            .rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(completed), 0) FROM tasks WHERE " + where, args)) {
      cursor.moveToFirst();
      return new Progress(cursor.getInt(1), cursor.getInt(0));
    }
  }

  public static final class Progress {
    public final int completed;
    public final int total;
    public final int percent;

    public Progress(int completed, int total) {
      this.completed = completed;
      this.total = total;
      this.percent = total == 0 ? 0 : Math.round(completed * 100f / total);
    }
  }

  private String today() {
    return LocalDate.now(clock).toString();
  }

  private SQLiteDatabase databaseForDay(String day) {
    SQLiteDatabase database = getWritableDatabase();
    // Keep the existing day ledger; initialization never inserts invented tasks or completions.
    ContentValues ledger = new ContentValues();
    ledger.put("task_date", day);
    database.insertWithOnConflict("checklist_days", null, ledger, SQLiteDatabase.CONFLICT_IGNORE);
    return database;
  }

  private static ContentValues taskValues(String title, String category) {
    if (title == null || title.trim().isEmpty())
      throw new IllegalArgumentException("Judul tugas wajib diisi");
    String normalized = title.trim();
    if (normalized.length() > 200)
      throw new IllegalArgumentException("Judul maksimal 200 karakter");
    if (!CATEGORY_STUDY.equals(category)
        && !CATEGORY_IBADAH.equals(category)
        && !CATEGORY_HIFZ.equals(category)) {
      throw new IllegalArgumentException("Kategori harus Study, Ibadah, atau Hifz");
    }
    ContentValues values = new ContentValues();
    values.put("title", normalized);
    values.put("category", category);
    return values;
  }
}
