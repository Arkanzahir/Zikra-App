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
 * SQLite source of truth for Home and Study. Each local calendar day has its own checklist. A
 * separate day ledger prevents an empty/deleted day being reseeded. The first day has eight example
 * completions matching the supplied prototype; subsequent days start with all 24 defaults
 * incomplete. Previous days are kept.
 */
public final class ZikraDbHelper extends SQLiteOpenHelper {
  private static final int VERSION = 1;
  public static final String CATEGORY_STUDY = "Study";
  public static final String CATEGORY_IBADAH = "Ibadah";
  public static final String CATEGORY_HIFZ = "Hifz";
  private final Clock clock;

  private static final String[] DEFAULT_TITLES = {
    "Sholat Subuh",
    "Dzikir pagi",
    "Doa sebelum belajar",
    "Tilawah Al-Fatihah",
    "Murojaah hafalan",
    "Sedekah pagi",
    "Membaca materi kuliah",
    "Menulis prioritas hari ini",
    "Sholat Dzuhur",
    "Sholat Ashar",
    "Sholat Maghrib",
    "Sholat Isya",
    "Sholat Dhuha",
    "Dzikir petang",
    "Tilawah Al-Baqarah",
    "Menambah hafalan",
    "Membaca terjemahan Al-Quran",
    "Doa sebelum tidur",
    "Laporan praktikum fisika",
    "Latihan soal matematika",
    "Tugas Pemrograman Mobile",
    "Meninjau catatan kuliah",
    "Belajar mandiri 25 menit",
    "Menyiapkan jadwal besok"
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
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Version 1 has no migration. Future versions must migrate without deleting history.
    throw new IllegalStateException(
        "Unsupported database migration: " + oldVersion + " to " + newVersion);
  }

  public List<Task> getTasks() {
    String day = today();
    SQLiteDatabase db = databaseForDay(day);
    List<Task> tasks = new ArrayList<>();
    try (Cursor cursor =
        db.query(
            "tasks",
            new String[] {"id", "title", "category", "completed"},
            "task_date = ?",
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
                cursor.getInt(3) == 1));
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

  public void updateTask(long id, String title, String category) {
    String day = today();
    databaseForDay(day)
        .update(
            "tasks",
            taskValues(title, category),
            "id = ? AND task_date = ?",
            new String[] {Long.toString(id), day});
  }

  public void setCompleted(long id, boolean completed) {
    String day = today();
    ContentValues values = new ContentValues();
    values.put("completed", completed ? 1 : 0);
    databaseForDay(day)
        .update("tasks", values, "id = ? AND task_date = ?", new String[] {Long.toString(id), day});
  }

  public void deleteTask(long id) {
    String day = today();
    databaseForDay(day)
        .delete("tasks", "id = ? AND task_date = ?", new String[] {Long.toString(id), day});
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

  /** One query keeps numerator and denominator from the same day/snapshot. */
  public Progress getProgress() {
    String day = today();
    try (Cursor cursor =
        databaseForDay(day)
            .rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(completed), 0) FROM tasks WHERE task_date = ?",
                new String[] {day})) {
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
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      boolean initialized;
      try (Cursor cursor =
          db.rawQuery("SELECT 1 FROM checklist_days WHERE task_date = ?", new String[] {day})) {
        initialized = cursor.moveToFirst();
      }
      if (!initialized) {
        boolean firstDay;
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM checklist_days LIMIT 1", null)) {
          firstDay = !cursor.moveToFirst();
        }
        ContentValues ledger = new ContentValues();
        ledger.put("task_date", day);
        db.insertOrThrow("checklist_days", null, ledger);
        for (int i = 0; i < DEFAULT_TITLES.length; i++) {
          String category = (i == 6 || i == 7 || i >= 18) ? CATEGORY_STUDY : CATEGORY_IBADAH;
          if (i == 4 || i == 15) category = CATEGORY_HIFZ;
          ContentValues values = taskValues(DEFAULT_TITLES[i], category);
          values.put("task_date", day);
          values.put("completed", firstDay && i < 8 ? 1 : 0);
          db.insertOrThrow("tasks", null, values);
        }
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return db;
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
