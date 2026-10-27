package com.zikra.app.faith;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import com.zikra.app.audio.Recitation;
import com.zikra.app.location.LocationStore;
import java.time.*;
import java.util.*;

/** Separate, local-only journal; existing tasks, targets and reading preferences stay untouched. */
public final class FaithStore extends SQLiteOpenHelper {
  public static final int PRACTICE = 1, READING = 2;
  private final Context context;
  private final Clock clock;

  public FaithStore(Context context) {
    this(context, "faith.db", null);
  }

  public FaithStore(Context context, String name, Clock clock) {
    super(context.getApplicationContext(), name, null, 1);
    this.context = context.getApplicationContext();
    this.clock = clock;
  }

  public LocalDate today() {
    return clock == null ? LocationStore.today(context) : LocalDate.now(clock);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE plans(id INTEGER PRIMARY KEY AUTOINCREMENT,start TEXT NOT NULL,days INTEGER"
            + " NOT NULL CHECK(days IN(30,90,365)),active INTEGER NOT NULL CHECK(active IN(0,1)))");
    db.execSQL("CREATE UNIQUE INDEX one_active_plan ON plans(active) WHERE active=1");
    db.execSQL(
        "CREATE TABLE entries(id INTEGER PRIMARY KEY AUTOINCREMENT,token TEXT UNIQUE NOT"
            + " NULL,revision INTEGER NOT NULL DEFAULT 1,kind INTEGER NOT NULL CHECK(kind"
            + " IN(1,2)),day TEXT NOT NULL,chapter INTEGER NOT NULL,first_verse INTEGER NOT"
            + " NULL,last_verse INTEGER NOT NULL,note TEXT NOT NULL,target_id INTEGER NOT"
            + " NULL,plan_id INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX entries_day ON entries(day,id)");
    db.execSQL("CREATE INDEX entries_plan ON entries(plan_id,kind)");
    db.execSQL("CREATE TABLE day_notes(day TEXT PRIMARY KEY NOT NULL,note TEXT NOT NULL)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int from, int to) {
    throw new IllegalStateException("Migrasi jurnal belum didukung");
  }

  public static final class Plan {
    public final long id;
    public final LocalDate start;
    public final int days;

    Plan(long id, String start, int days) {
      this.id = id;
      this.start = LocalDate.parse(start);
      this.days = FaithProgress.duration(days);
    }
  }

  public Plan plan() {
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT id,start,days FROM plans WHERE active=1", null)) {
      return c.moveToFirst() ? new Plan(c.getLong(0), c.getString(1), c.getInt(2)) : null;
    }
  }

  public long savePlan(int days, LocalDate start, boolean newCycle) {
    Plan current = plan();
    return savePlan(days, start, newCycle, current == null ? 0 : current.id);
  }

  public long savePlan(int days, LocalDate start, boolean newCycle, long expectedPlan) {
    FaithProgress.duration(days);
    validDay(start);
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      Plan current = plan();
      if ((current == null ? 0 : current.id) != expectedPlan)
        throw new IllegalStateException("Target berubah; buka kembali formulir");
      ContentValues v = new ContentValues();
      v.put("days", days);
      long id;
      if (current != null && !newCycle) {
        if (!current.start.equals(start))
          throw new IllegalArgumentException(
              "Tanggal awal target lama tetap; mulai target baru untuk tanggal lain");
        id = current.id;
        db.update("plans", v, "id=?", new String[] {"" + id});
      } else {
        db.execSQL("UPDATE plans SET active=0 WHERE active=1");
        v.put("start", start.toString());
        v.put("active", 1);
        id = db.insertOrThrow("plans", null, v);
      }
      db.setTransactionSuccessful();
      return id;
    } finally {
      db.endTransaction();
    }
  }

  public static final class Entry {
    public final long id, targetId, planId;
    public final int revision, kind, chapter, first, last;
    public final String token, note;
    public final LocalDate day;

    public Entry(
        long id,
        int revision,
        String token,
        int kind,
        LocalDate day,
        int chapter,
        int first,
        int last,
        String note,
        long targetId,
        long planId) {
      if (id < 0
          || revision < 0
          || token == null
          || !token.matches("[a-fA-F0-9-]{36}")
          || day == null
          || targetId < 0
          || planId < 0
          || kind != PRACTICE && kind != READING)
        throw new IllegalArgumentException("Catatan tidak valid");
      Recitation.globalVerse(chapter, first);
      Recitation.globalVerse(chapter, last);
      if (last < first) throw new IllegalArgumentException("Ayat akhir harus setelah ayat awal");
      this.note = checkedNote(note);
      this.id = id;
      this.revision = revision;
      this.token = token;
      this.kind = kind;
      this.day = day;
      this.chapter = chapter;
      this.first = first;
      this.last = last;
      this.targetId = targetId;
      this.planId = planId;
    }
  }

  private static String checkedNote(String note) {
    if (note == null || note.length() > 1000)
      throw new IllegalArgumentException("Catatan maksimal 1.000 karakter");
    return note.trim();
  }

  private void validDay(LocalDate day) {
    if (day == null || day.isAfter(today()) || day.isBefore(LocalDate.of(1970, 1, 1)))
      throw new IllegalArgumentException("Tanggal catatan harus hari ini atau sebelumnya");
  }

  public long save(Entry e) {
    validDay(e.day);
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      if (e.kind == READING) {
        try (Cursor c =
            db.rawQuery(
                "SELECT start,active FROM plans WHERE id=?", new String[] {"" + e.planId})) {
          if (!c.moveToFirst()
              || e.day.isBefore(LocalDate.parse(c.getString(0)))
              || e.id == 0 && c.getInt(1) != 1)
            throw new IllegalStateException("Target khatam berubah; buka kembali formulir");
        }
      }
      ContentValues v = new ContentValues();
      v.put("kind", e.kind);
      v.put("day", e.day.toString());
      v.put("chapter", e.chapter);
      v.put("first_verse", e.first);
      v.put("last_verse", e.last);
      v.put("note", e.note);
      v.put("target_id", e.targetId);
      v.put("plan_id", e.planId);
      long id = e.id;
      if (id == 0) {
        try (Cursor c =
            db.query("entries", null, "token=?", new String[] {e.token}, null, null, null)) {
          if (c.moveToFirst()) {
            Entry old = read(c);
            if (old.kind != e.kind
                || !old.day.equals(e.day)
                || old.chapter != e.chapter
                || old.first != e.first
                || old.last != e.last
                || !old.note.equals(e.note)
                || old.planId != e.planId
                || old.targetId != e.targetId)
              throw new IllegalStateException("Catatan sudah tersimpan dengan isi berbeda");
            db.setTransactionSuccessful();
            return old.id;
          }
        }
        checkDayCapacity(db, e);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM entries", null)) {
          c.moveToFirst();
          if (c.getInt(0) >= 5000)
            throw new IllegalStateException(
                "Riwayat penuh; hapus catatan yang dipilih sebelum menambah");
        }
        v.put("token", e.token);
        v.put("revision", 1);
        id = db.insertOrThrow("entries", null, v);
      } else {
        checkDayCapacity(db, e);
        v.put("revision", e.revision + 1);
        if (db.update(
                "entries",
                v,
                "id=? AND revision=? AND token=?",
                new String[] {"" + id, "" + e.revision, e.token})
            != 1)
          throw new IllegalStateException("Catatan berubah atau dihapus; buka kembali riwayat");
      }
      db.setTransactionSuccessful();
      return id;
    } finally {
      db.endTransaction();
    }
  }

  private static void checkDayCapacity(SQLiteDatabase db, Entry e) {
    try (Cursor c =
        db.rawQuery(
            "SELECT COUNT(*) FROM entries WHERE day=? AND id<>?",
            new String[] {e.day.toString(), "" + e.id})) {
      c.moveToFirst();
      if (c.getInt(0) >= 100)
        throw new IllegalStateException(
            "Maksimal 100 catatan per tanggal; hapus catatan yang dipilih sebelum menambah");
    }
  }

  public boolean delete(Entry e) {
    return getWritableDatabase()
            .delete(
                "entries",
                "id=? AND revision=? AND token=?",
                new String[] {"" + e.id, "" + e.revision, e.token})
        == 1;
  }

  public List<Entry> entries(LocalDate day) {
    List<Entry> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query(
                "entries", null, "day=?", new String[] {day.toString()}, null, null, "id DESC")) {
      while (c.moveToNext()) out.add(read(c));
    }
    return out;
  }

  private static Entry read(Cursor c) {
    return new Entry(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getInt(c.getColumnIndexOrThrow("revision")),
        c.getString(c.getColumnIndexOrThrow("token")),
        c.getInt(c.getColumnIndexOrThrow("kind")),
        LocalDate.parse(c.getString(c.getColumnIndexOrThrow("day"))),
        c.getInt(c.getColumnIndexOrThrow("chapter")),
        c.getInt(c.getColumnIndexOrThrow("first_verse")),
        c.getInt(c.getColumnIndexOrThrow("last_verse")),
        c.getString(c.getColumnIndexOrThrow("note")),
        c.getLong(c.getColumnIndexOrThrow("target_id")),
        c.getLong(c.getColumnIndexOrThrow("plan_id")));
  }

  public BitSet coverage(long plan) {
    List<int[]> ranges = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT chapter,first_verse,last_verse FROM entries WHERE kind=2 AND plan_id=?",
                new String[] {"" + plan})) {
      while (c.moveToNext())
        ranges.add(
            new int[] {
              Recitation.globalVerse(c.getInt(0), c.getInt(1)),
              Recitation.globalVerse(c.getInt(0), c.getInt(2))
            });
    }
    return FaithProgress.coverage(ranges);
  }

  public Set<LocalDate> activeDays() {
    Set<LocalDate> out = new HashSet<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT DISTINCT day FROM entries WHERE day<=?",
                new String[] {today().toString()})) {
      while (c.moveToNext()) out.add(LocalDate.parse(c.getString(0)));
    }
    return out;
  }

  public String note(LocalDate day) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT note FROM day_notes WHERE day=?", new String[] {day.toString()})) {
      return c.moveToFirst() ? c.getString(0) : "";
    }
  }

  public void note(LocalDate day, String note) {
    validDay(day);
    String text = checkedNote(note);
    if (text.isEmpty())
      getWritableDatabase().delete("day_notes", "day=?", new String[] {day.toString()});
    else {
      ContentValues v = new ContentValues();
      v.put("day", day.toString());
      v.put("note", text);
      getWritableDatabase()
          .insertWithOnConflict("day_notes", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }
  }
}
