package com.zikra.app.discovery;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.JsonReader;
import android.util.JsonToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Explicit, checksum-pinned book downloads. Only bounded pages are materialized as UI models. */
public final class HadithStore implements AutoCloseable {
  public static final int PAGE_SIZE = 20;
  public static final String REVISION = "8e15b4f9e7585822426a0d470e8dfec27e2a1707";
  public static final Book BUKHARI = new Book("bukhari", 6638, 7008, 13415826L,
      "65e7a48f8ae0b792e3f8374f9de3e45d3119a1fdb5b6322aa6182db585348bb8");
  public static final Book MUSLIM = new Book("muslim", 4930, 5362, 12049465L,
      "c9dcee026176991c52c955628c7242f60a7ffafbb714c2cd387b14759bb89a71");
  private static final OkHttpClient HTTP = new OkHttpClient.Builder()
      .followRedirects(false).followSslRedirects(false)
      .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
      .callTimeout(180, TimeUnit.SECONDS).build();
  private final Context context;
  private final Database helper;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private volatile boolean closed, cancelled;
  private volatile Call call;

  public static final class Book {
    public final String id, sha256;
    public final int count, maximumNumber;
    public final long bytes;
    private Book(String id, int count, int maximumNumber, long bytes, String sha256) {
      this.id = id; this.count = count; this.maximumNumber = maximumNumber;
      this.bytes = bytes; this.sha256 = sha256;
    }
  }

  public static Book book(String id) {
    if ("bukhari".equals(id)) return BUKHARI;
    if ("muslim".equals(id)) return MUSLIM;
    throw new IllegalArgumentException("Book");
  }

  public static final class Page {
    public final List<HadithCollection.Entry> entries;
    public final int total, offset, unavailableSaved;
    public final boolean bukhariInstalled, muslimInstalled;
    private Page(List<HadithCollection.Entry> entries, int total, int offset, int missing,
        boolean bukhari, boolean muslim) {
      this.entries = Collections.unmodifiableList(entries); this.total = total;
      this.offset = offset; this.unavailableSaved = missing;
      this.bukhariInstalled = bukhari; this.muslimInstalled = muslim;
    }
  }

  public interface Result<T> { void show(T value); void failed(); }
  public interface Download { void progress(int percent); void finished(); void failed(String code); }

  public HadithStore(Context context) { this(context, "zikra_hadith.db"); }
  public HadithStore(Context context, String databaseName) {
    this.context = context.getApplicationContext();
    helper = new Database(this.context, databaseName);
  }

  private void post(Runnable action) { main.post(() -> { if (!closed) action.run(); }); }

  public void query(String query, String collection, Set<String> bookmarks, int offset, Result<Page> result) {
    String text = HadithSearch.normalize(query);
    Set<String> saved = new HashSet<>(bookmarks);
    worker.execute(() -> {
      try { Page page = queryNow(text, collection, saved, offset); post(() -> result.show(page)); }
      catch (Exception failure) { post(result::failed); }
    });
  }

  private Page queryNow(String query, String collection, Set<String> saved, int requestedOffset) {
    SQLiteDatabase db = helper.getWritableDatabase();
    syncBookmarks(db, saved);
    List<String> args = new ArrayList<>();
    String where = "1=1";
    if ("Bukhari".equals(collection) || "Muslim".equals(collection)) {
      where += " AND book=?"; args.add(collection.toLowerCase(java.util.Locale.ROOT));
    } else if ("Tersimpan".equals(collection)) where += " AND id IN (SELECT id FROM bookmarks)";
    if (query.matches("[0-9]{1,6}")) { where += " AND number=?"; args.add(query); }
    else if (!query.isEmpty()) {
      for (String token : query.split("\\s+", 9)) {
        where += " AND search LIKE ? ESCAPE '\\'"; args.add(HadithSearch.literalLike(token));
      }
    }
    String[] values = args.toArray(new String[0]);
    int total;
    try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM entries WHERE " + where, values)) {
      c.moveToFirst(); total = c.getInt(0);
    }
    int offset = Math.max(0, Math.min(Math.max(0, total - 1) / PAGE_SIZE * PAGE_SIZE, requestedOffset));
    List<HadithCollection.Entry> rows = new ArrayList<>();
    try (Cursor c = db.query("entries", null, where, values, null, null,
        "excerpt DESC,book,number,id", offset + "," + PAGE_SIZE)) {
      while (c.moveToNext()) rows.add(entry(c));
    }
    int missing = 0;
    try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM bookmarks b LEFT JOIN entries e ON e.id=b.id WHERE e.id IS NULL", null)) {
      c.moveToFirst(); missing = c.getInt(0);
    }
    return new Page(rows, total, offset, missing, installed(db, "bukhari"), installed(db, "muslim"));
  }

  private void syncBookmarks(SQLiteDatabase db, Set<String> saved) {
    db.beginTransaction();
    try {
      db.delete("bookmarks", null, null);
      for (String id : saved) {
        if (id == null || id.length() > 120) continue;
        ContentValues v = new ContentValues(); v.put("id", id);
        db.insertWithOnConflict("bookmarks", null, v, SQLiteDatabase.CONFLICT_IGNORE);
      }
      db.setTransactionSuccessful();
    } finally { db.endTransaction(); }
  }

  private static boolean installed(SQLiteDatabase db, String book) {
    try (Cursor c = db.rawQuery("SELECT 1 FROM packages WHERE book=? AND revision=?",
        new String[]{book, REVISION})) { return c.moveToFirst(); }
  }

  public void get(String id, Result<HadithCollection.Entry> result) {
    worker.execute(() -> {
      try {
        HadithCollection.Entry value = null;
        try (Cursor c = helper.getReadableDatabase().query("entries", null, "id=?",
            new String[]{id == null ? "" : id}, null, null, null)) {
          if (c.moveToFirst()) value = entry(c);
        }
        HadithCollection.Entry found = value;
        post(() -> result.show(found));
      } catch (Exception failure) { post(result::failed); }
    });
  }

  private static HadithCollection.Entry entry(Cursor c) {
    String id = c.getString(c.getColumnIndexOrThrow("id"));
    if (c.getInt(c.getColumnIndexOrThrow("excerpt")) == 1) {
      for (HadithCollection.Entry e : HadithCollection.ENTRIES) if (e.id.equals(id)) return e;
      throw new IllegalStateException("Unknown excerpt");
    }
    return HadithCollection.sourced(c.getString(c.getColumnIndexOrThrow("book")),
        c.getInt(c.getColumnIndexOrThrow("number")), c.getString(c.getColumnIndexOrThrow("arab")),
        c.getString(c.getColumnIndexOrThrow("meaning")));
  }

  public void download(String id, Download result) {
    Book book = book(id);
    cancelled = false;
    worker.execute(() -> {
      File temp = new File(context.getCacheDir(), "hadith-" + book.id + ".part");
      try {
        if (new StatFs(context.getFilesDir().getAbsolutePath()).getAvailableBytes() < book.bytes * 4)
          throw new IllegalStateException("storage");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String url = "https://raw.githubusercontent.com/gadingnst/hadith-api/" + REVISION
            + "/books/" + book.id + ".json";
        Call request = HTTP.newCall(new Request.Builder().url(url).build()); call = request;
        if (closed || cancelled) { request.cancel(); throw new IllegalStateException("cancelled"); }
        long bytes = 0; int lastPercent = -1;
        try (Response response = request.execute()) {
          if (!response.isSuccessful() || response.body() == null
              || response.body().contentLength() > book.bytes) throw new IllegalStateException("network");
          try (InputStream input = response.body().byteStream(); FileOutputStream output = new FileOutputStream(temp)) {
            byte[] buffer = new byte[8192]; int size;
            while ((size = input.read(buffer)) != -1) {
              if (closed || cancelled) throw new IllegalStateException("cancelled");
              bytes += size;
              if (bytes > book.bytes) throw new IllegalStateException("checksum");
              output.write(buffer, 0, size); digest.update(buffer, 0, size);
              int percent = (int) (bytes * 90 / book.bytes);
              if (percent != lastPercent) { lastPercent = percent; post(() -> result.progress(percent)); }
            }
          }
        }
        StringBuilder hash = new StringBuilder();
        for (byte b : digest.digest()) hash.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        if (bytes != book.bytes || !book.sha256.equals(hash.toString())) throw new IllegalStateException("checksum");
        importValidated(temp, book);
        post(result::finished);
      } catch (Exception failure) {
        String message = failure.getMessage();
        String code = closed || cancelled ? "cancelled" : "storage".equals(message) ? "storage"
            : "checksum".equals(message) ? "checksum" : "network";
        post(() -> result.failed(code));
      } finally { call = null; temp.delete(); }
    });
  }

  /** The hash is checked before this transaction; any parse/cancel failure restores the old package. */
  private void importValidated(File file, Book book) throws Exception {
    SQLiteDatabase db = helper.getWritableDatabase(); db.beginTransaction();
    try (JsonReader json = new JsonReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      db.delete("entries", "book=?", new String[]{book.id});
      json.setLenient(false); json.beginArray(); int count = 0, maximum = 0;
      while (json.hasNext()) {
        if (closed || cancelled) throw new IllegalStateException("cancelled");
        int number = 0; String arab = null, meaning = null; Set<String> fields = new HashSet<>();
        json.beginObject();
        while (json.hasNext()) {
          String key = json.nextName();
          if (!fields.add(key)) throw new IllegalStateException("duplicate field");
          if ("number".equals(key)) number = json.nextInt();
          else if ("arab".equals(key)) arab = json.nextString();
          else if ("id".equals(key)) meaning = json.nextString();
          else json.skipValue();
        }
        json.endObject();
        if (number < 1 || number > book.maximumNumber || arab == null || arab.trim().isEmpty()
            || arab.length() > 200000 || meaning == null || meaning.trim().isEmpty()
            || meaning.length() > 200000 || ++count > book.count)
          throw new IllegalStateException("invalid record");
        HadithCollection.Entry entry = HadithCollection.sourced(book.id, number, arab, meaning);
        db.insertOrThrow("entries", null, values(entry, book.id, number));
        maximum = Math.max(maximum, number);
      }
      json.endArray();
      if (json.peek() != JsonToken.END_DOCUMENT || count != book.count || maximum != book.maximumNumber
          || closed || cancelled) throw new IllegalStateException("incomplete package");
      ContentValues metadata = new ContentValues(); metadata.put("book", book.id);
      metadata.put("revision", REVISION); metadata.put("count", count);
      db.insertWithOnConflict("packages", null, metadata, SQLiteDatabase.CONFLICT_REPLACE);
      db.setTransactionSuccessful();
    } finally { db.endTransaction(); }
  }

  public void remove(String id, Runnable done) {
    remove(id, new Result<Boolean>() {
      public void show(Boolean removed) { done.run(); }
      public void failed() { done.run(); }
    });
  }

  public void remove(String id, Result<Boolean> result) {
    Book selected = book(id);
    worker.execute(() -> {
      try {
        SQLiteDatabase db = helper.getWritableDatabase(); db.beginTransaction();
        try {
          db.delete("entries", "book=?", new String[]{selected.id});
          db.delete("packages", "book=?", new String[]{selected.id});
          db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        post(() -> result.show(true));
      } catch (Exception failure) { post(result::failed); }
    });
  }

  public void cancel() { cancelled = true; Call active = call; if (active != null) active.cancel(); }
  @Override public void close() {
    closed = true; cancel();
    worker.execute(helper::close); worker.shutdown();
  }

  private static ContentValues values(HadithCollection.Entry entry, String book, Integer number) {
    ContentValues v = new ContentValues(); v.put("id", entry.id); v.put("book", book);
    if (number == null) v.putNull("number"); else v.put("number", number);
    v.put("title", entry.title); v.put("arab", entry.arabic); v.put("meaning", entry.meaning);
    v.put("source", entry.source); v.put("excerpt", entry.excerpt ? 1 : 0);
    v.put("search", HadithSearch.normalize(entry.title + " " + entry.arabic + " " + entry.meaning + " " + entry.source));
    return v;
  }

  private static final class Database extends SQLiteOpenHelper {
    Database(Context context, String name) { super(context, name, null, 1); }
    @Override public void onConfigure(SQLiteDatabase db) {
      // New optional-content databases return deleted pages to the filesystem.
      db.execSQL("PRAGMA auto_vacuum=FULL");
    }
    @Override public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE entries(id TEXT PRIMARY KEY NOT NULL,book TEXT NOT NULL,number INTEGER,"
          + "title TEXT NOT NULL,arab TEXT NOT NULL,meaning TEXT NOT NULL,source TEXT NOT NULL,"
          + "search TEXT NOT NULL,excerpt INTEGER NOT NULL,UNIQUE(book,number))");
      db.execSQL("CREATE INDEX entries_book_number ON entries(book,number)");
      db.execSQL("CREATE TABLE packages(book TEXT PRIMARY KEY NOT NULL,revision TEXT NOT NULL,count INTEGER NOT NULL)");
      db.execSQL("CREATE TABLE bookmarks(id TEXT PRIMARY KEY NOT NULL)");
      for (HadithCollection.Entry entry : HadithCollection.ENTRIES)
        db.insertOrThrow("entries", null, values(entry, "legacy", null));
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      throw new IllegalStateException("Unsupported content migration");
    }
  }
}
