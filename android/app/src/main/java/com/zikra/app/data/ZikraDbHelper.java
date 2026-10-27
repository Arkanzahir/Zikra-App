package com.zikra.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.zikra.app.location.LocationStore;
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
  private static final int VERSION = 5;
  public static final String CATEGORY_STUDY = "Study";
  public static final String CATEGORY_IBADAH = "Ibadah";
  public static final String CATEGORY_HIFZ = "Hifz";
  private final Clock clock;
  private final Context context;
  private final boolean followsLocation;

  private static final String ACTIVE_TASKS = "(category IN ('Study','Hifz') OR task_date = ?)";
  private static final String[] OBLIGATORY_PRAYERS = {
    "Sholat Subuh", "Sholat Dzuhur", "Sholat Ashar", "Sholat Maghrib", "Sholat Isya"
  };

  public ZikraDbHelper(Context context) {
    this(context, "zikra.db", Clock.systemUTC(), true);
  }

  /** Allows a separate database and clock for deterministic instrumented tests. */
  public ZikraDbHelper(Context context, String databaseName, Clock clock) {
    this(context, databaseName, clock, false);
  }

  private ZikraDbHelper(Context context, String databaseName, Clock clock, boolean followsLocation) {
    super(context.getApplicationContext(), databaseName, null, VERSION);
    if (clock == null) throw new IllegalArgumentException("Clock is required");
    this.clock = clock;
    this.context = context.getApplicationContext();
    this.followsLocation = followsLocation;
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
    addAcademicColumns(db);
    addAssistantReceipts(db);
    addAcademicDetails(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 1 || newVersion != VERSION)
      throw new IllegalStateException("Unsupported migration");
    if (oldVersion < 2) addHifzColumns(db);
    if (oldVersion < 3) addAcademicColumns(db);
    if (oldVersion < 4) addAssistantReceipts(db);
    if (oldVersion < 5) addAcademicDetails(db);
  }

  private static void addAcademicDetails(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE tasks ADD COLUMN subject TEXT NOT NULL DEFAULT ''");
    db.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 1 CHECK(priority BETWEEN 0 AND 2)");
    db.execSQL("ALTER TABLE tasks ADD COLUMN advance_reminder INTEGER NOT NULL DEFAULT 0 CHECK(advance_reminder IN (0,1))");
  }

  private static void addAssistantReceipts(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE assistant_actions (action_id TEXT PRIMARY KEY NOT NULL, task_id INTEGER NOT NULL)");
  }

  public boolean assistantProposalApplied(String id) {
    try (Cursor cursor = getReadableDatabase().rawQuery(
        "SELECT 1 FROM assistant_actions WHERE action_id=?", new String[] {id})) {
      return cursor.moveToFirst();
    }
  }

  /** Call only after explicit review. Receipt and mutation commit together, surviving retries/restarts. */
  public long applyReviewedProposal(com.zikra.app.assistant.TaskProposal proposal) {
    SQLiteDatabase db = databaseForDay(today());
    db.beginTransaction();
    try {
      try (Cursor receipt = db.rawQuery("SELECT task_id FROM assistant_actions WHERE action_id=?",
          new String[] {proposal.id})) {
        if (receipt.moveToFirst()) { db.setTransactionSuccessful(); return receipt.getLong(0); }
      }
      long taskId;
      if ("add_task".equals(proposal.kind)) {
        ContentValues values = academicValues(proposal.title, proposal.dueAt, false);
        values.put("task_date", today()); values.put("completed", 0);
        taskId = db.insertOrThrow("tasks", null, values);
      } else {
        ContentValues values = new ContentValues();
        values.put("title", proposal.title); values.put("due_at", proposal.dueAt);
        if (proposal.dueAt == 0) {
          values.put("study_reminder", 0);
          values.put("advance_reminder", 0);
        }
        if (db.update("tasks", values,
            "id=? AND category=? AND completed=0 AND title=? AND due_at=?",
            new String[] {Long.toString(proposal.taskId), CATEGORY_STUDY,
              proposal.expectedTitle, Long.toString(proposal.expectedDueAt)}) != 1)
          throw new IllegalStateException("Tugas berubah atau sudah selesai. Minta usulan baru.");
        taskId = proposal.taskId;
      }
      ContentValues receipt = new ContentValues();
      receipt.put("action_id", proposal.id); receipt.put("task_id", taskId);
      db.insertOrThrow("assistant_actions", null, receipt);
      db.setTransactionSuccessful();
      return taskId;
    } finally { db.endTransaction(); }
  }

  private static void addAcademicColumns(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE tasks ADD COLUMN due_at INTEGER NOT NULL DEFAULT 0");
    db.execSQL("ALTER TABLE tasks ADD COLUMN study_reminder INTEGER NOT NULL DEFAULT 0");
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
              "hifz_last",
              "due_at",
              "study_reminder", "subject", "priority", "advance_reminder"
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
                readTarget(cursor),
                cursor.getLong(9),
                cursor.getInt(10) == 1, cursor.getString(11), cursor.getInt(12), cursor.getInt(13) == 1));
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
    if (!CATEGORY_STUDY.equals(category)) {
      values.put("due_at", 0);
      values.put("study_reminder", 0);
    }
    return databaseForDay(day)
            .update(
                "tasks",
                values,
                "id = ? AND " + ACTIVE_TASKS,
                new String[] {Long.toString(id), day})
        == 1;
  }

  public long addAcademicTask(String title, long dueAt, boolean reminder) {
    ContentValues values = academicValues(title, dueAt, reminder);
    values.put("task_date", today());
    values.put("completed", 0);
    return databaseForDay(today()).insertOrThrow("tasks", null, values);
  }

  public boolean updateAcademicTask(long id, String title, long dueAt, boolean reminder) {
    return databaseForDay(today())
            .update(
                "tasks",
                academicValues(title, dueAt, reminder),
                "id=? AND category=?",
                new String[] {Long.toString(id), CATEGORY_STUDY})
        == 1;
  }

  public long addAcademicTask(String title, String subject, int priority, long dueAt,
      boolean reminder, boolean advance) {
    ContentValues values = academicDetails(title, subject, priority, dueAt, reminder, advance);
    values.put("task_date", today()); values.put("completed", 0);
    return databaseForDay(today()).insertOrThrow("tasks", null, values);
  }

  public boolean updateAcademicTask(long id, String title, String subject, int priority,
      long dueAt, boolean reminder, boolean advance) {
    return databaseForDay(today()).update("tasks",
        academicDetails(title, subject, priority, dueAt, reminder, advance),
        "id=? AND category=?", new String[] {Long.toString(id), CATEGORY_STUDY}) == 1;
  }

  private static ContentValues academicDetails(String title, String subject, int priority,
      long dueAt, boolean reminder, boolean advance) {
    if (priority < 0 || priority > 2 || (advance && !reminder))
      throw new IllegalArgumentException("Invalid academic details");
    ContentValues values = academicValues(title, dueAt, reminder);
    values.put("subject", WeeklyClass.label(subject, 100, false));
    values.put("priority", priority); values.put("advance_reminder", advance ? 1 : 0);
    return values;
  }

  private static ContentValues academicValues(String title, long dueAt, boolean reminder) {
    if (dueAt < 0 || (reminder && dueAt == 0))
      throw new IllegalArgumentException("Tenggat tidak valid");
    ContentValues values = taskValues(title, CATEGORY_STUDY);
    values.put("due_at", dueAt);
    values.put("study_reminder", reminder ? 1 : 0);
    if (!reminder) values.put("advance_reminder", 0);
    return values;
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

  /** Historical local-date worship records; reading never creates/checks a task. */
  public java.util.Map<LocalDate, Progress> worshipHistory() {
    java.util.Map<LocalDate, Progress> result = new java.util.TreeMap<>();
    try (Cursor c = getReadableDatabase().rawQuery(
        "SELECT task_date,SUM(completed),COUNT(*) FROM tasks WHERE category=? GROUP BY task_date",
        new String[] {CATEGORY_IBADAH})) {
      while (c.moveToNext()) result.put(LocalDate.parse(c.getString(0)), new Progress(c.getInt(1), c.getInt(2)));
    }
    return result;
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
    return (followsLocation
            ? LocationStore.localDate(context, clock.instant())
            : LocalDate.now(clock))
        .toString();
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
