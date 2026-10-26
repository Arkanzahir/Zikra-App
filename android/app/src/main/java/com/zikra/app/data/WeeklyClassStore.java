package com.zikra.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Separate local store; no schedule is created until the user saves one. */
public final class WeeklyClassStore extends SQLiteOpenHelper {
  public static final int MAX_CLASSES = 64;
  public static final class Conflict extends IllegalStateException {}
  public static final class Stale extends IllegalStateException {}
  public static final class Full extends IllegalStateException {}

  public WeeklyClassStore(Context context) { this(context, "study-schedule.db"); }
  public WeeklyClassStore(Context context, String name) {
    super(context.getApplicationContext(), name, null, 1);
  }
  @Override public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE weekly_classes (id INTEGER PRIMARY KEY AUTOINCREMENT,"
        + "revision INTEGER NOT NULL,title TEXT NOT NULL,room TEXT NOT NULL,teacher TEXT NOT NULL,"
        + "weekday INTEGER NOT NULL CHECK(weekday BETWEEN 1 AND 7),"
        + "start_minute INTEGER NOT NULL CHECK(start_minute>=0),"
        + "end_minute INTEGER NOT NULL CHECK(end_minute>start_minute AND end_minute<=1439),"
        + "reminder_minutes INTEGER NOT NULL CHECK(reminder_minutes IN (0,15,30)))");
    db.execSQL("CREATE INDEX weekly_classes_day ON weekly_classes(weekday,start_minute)");
  }
  @Override public void onUpgrade(SQLiteDatabase db, int from, int to) {
    throw new IllegalStateException("Unsupported schedule migration");
  }
  public List<WeeklyClass> all() {
    List<WeeklyClass> result = new ArrayList<>();
    try (Cursor c = getReadableDatabase().query("weekly_classes", null, null, null,
        null, null, "weekday,start_minute,id")) {
      while (c.moveToNext()) result.add(read(c));
    }
    return result;
  }
  public WeeklyClass find(long id) {
    try (Cursor c = getReadableDatabase().query("weekly_classes", null, "id=?",
        new String[] {Long.toString(id)}, null, null, null)) {
      return c.moveToFirst() ? read(c) : null;
    }
  }
  public long save(WeeklyClass item) {
    SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
    try {
      WeeklyClass old = item.id == 0 ? null : find(item.id);
      if (item.id != 0 && (old == null || old.revision != item.revision)) throw new Stale();
      if (item.id == 0 && all().size() >= MAX_CLASSES) throw new Full();
      try (Cursor c = db.rawQuery("SELECT 1 FROM weekly_classes WHERE weekday=? "
          + "AND start_minute<? AND end_minute>? AND id<>? LIMIT 1",
          new String[] {Integer.toString(item.weekday), Integer.toString(item.endMinute),
              Integer.toString(item.startMinute), Long.toString(item.id)})) {
        if (c.moveToFirst()) throw new Conflict();
      }
      ContentValues v = new ContentValues();
      v.put("revision", old == null ? 1 : old.revision + 1);
      v.put("title", item.title); v.put("room", item.room); v.put("teacher", item.teacher);
      v.put("weekday", item.weekday); v.put("start_minute", item.startMinute);
      v.put("end_minute", item.endMinute); v.put("reminder_minutes", item.reminderMinutes);
      long id = item.id;
      if (id == 0) id = db.insertOrThrow("weekly_classes", null, v);
      else if (db.update("weekly_classes", v, "id=? AND revision=?",
          new String[] {Long.toString(id), Long.toString(item.revision)}) != 1) throw new Stale();
      db.setTransactionSuccessful(); return id;
    } finally { db.endTransaction(); }
  }
  public boolean delete(long id, long revision) {
    return getWritableDatabase().delete("weekly_classes", "id=? AND revision=?",
        new String[] {Long.toString(id), Long.toString(revision)}) == 1;
  }
  private static WeeklyClass read(Cursor c) {
    return new WeeklyClass(c.getLong(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("revision")), c.getString(c.getColumnIndexOrThrow("title")),
        c.getString(c.getColumnIndexOrThrow("room")), c.getString(c.getColumnIndexOrThrow("teacher")),
        c.getInt(c.getColumnIndexOrThrow("weekday")), c.getInt(c.getColumnIndexOrThrow("start_minute")),
        c.getInt(c.getColumnIndexOrThrow("end_minute")), c.getInt(c.getColumnIndexOrThrow("reminder_minutes")));
  }
}
