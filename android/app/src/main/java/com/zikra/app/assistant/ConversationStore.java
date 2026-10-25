package com.zikra.app.assistant;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
import org.json.*;

/** Local conversations are separate from Study/Quran data and from gateway credentials. */
public final class ConversationStore extends SQLiteOpenHelper {
  public static final int MAX_CHATS = 40, MAX_MESSAGES = 200, MAX_CHAT_CHARS = 128000;
  private static final String LEGACY = "assistant_history_v1";
  private final SharedPreferences prefs;
  public static final class Chat {
    public final String id, title, draft;
    public final long updated;
    public final JSONArray messages;
    Chat(Cursor c) throws JSONException {
      id=c.getString(0); title=c.getString(1); updated=c.getLong(2);
      messages=new JSONArray(c.getString(3)); draft=c.getString(4);
    }
  }
  public ConversationStore(Context context) {
    super(context, "assistant_chats.db", null, 1);
    prefs=context.getSharedPreferences("zikra_preferences",0);
    migrate();
  }
  @Override public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE conversations(id TEXT PRIMARY KEY,title TEXT NOT NULL,updated INTEGER NOT NULL,messages TEXT NOT NULL,draft TEXT NOT NULL DEFAULT '',renamed INTEGER NOT NULL DEFAULT 0)");
    db.execSQL("CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
  }
  @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
    throw new IllegalStateException("Unsupported conversation migration");
  }
  private void migrate() {
    SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
    try(Cursor c=db.rawQuery("SELECT value FROM metadata WHERE key='legacy_imported'",null)) {
      if(!c.moveToFirst()) {
        String raw=prefs.getString(LEGACY,"[]");
        if(raw.length()<=50000) try {
          JSONArray rows=new JSONArray(raw); validate(rows);
          if(rows.length()>0) {
            ContentValues v=new ContentValues();v.put("id","legacy-local");v.put("title",titleFor(rows));
            v.put("updated",System.currentTimeMillis());v.put("messages",rows.toString());v.put("draft","");
            db.insertOrThrow("conversations",null,v);
          }
        } catch(JSONException|IllegalArgumentException invalid) {
          // Leave the original preference untouched, including malformed recoverable data.
        }
        db.execSQL("INSERT INTO metadata(key,value) VALUES('legacy_imported','1')");
      }
      db.setTransactionSuccessful();
    } finally {db.endTransaction();}
  }
  public static void validate(JSONArray rows) {
    if(rows==null||rows.length()>MAX_MESSAGES||rows.toString().length()>MAX_CHAT_CHARS)
      throw new IllegalArgumentException("Conversation storage limit");
    for(int i=0;i<rows.length();i++) {
      JSONObject r=rows.optJSONObject(i);
      if(r==null||!(r.optString("role").equals("user")||r.optString("role").equals("assistant"))
          ||!(r.opt("text") instanceof String)||r.optString("text").isEmpty()||r.optString("text").length()>8000)
        throw new IllegalArgumentException("Invalid conversation");
    }
  }
  public Chat get(String id) {
    try(Cursor c=getReadableDatabase().rawQuery("SELECT id,title,updated,messages,draft FROM conversations WHERE id=?",new String[]{id})) {
      return c.moveToFirst()?new Chat(c):null;
    }catch(JSONException invalid){return null;}
  }
  public List<Chat> list(String query) {
    List<Chat> result=new ArrayList<>();
    String q="%"+(query==null?"":query.trim().replace("\\","\\\\").replace("%","\\%").replace("_","\\_"))+"%";
    try(Cursor c=getReadableDatabase().rawQuery("SELECT id,title,updated,'[]','' FROM conversations WHERE title LIKE ? ESCAPE '\\' ORDER BY updated DESC,id",new String[]{q})) {
      while(c.moveToNext()) try{result.add(new Chat(c));}catch(JSONException ignored){}
    }
    return result;
  }
  public Chat create() {
    if(list("").size()>=MAX_CHATS)throw new IllegalStateException("Maksimal 40 percakapan. Hapus salah satu sebelum membuat chat baru.");
    String id=UUID.randomUUID().toString();ContentValues v=new ContentValues();
    v.put("id",id);v.put("title","Percakapan baru");v.put("updated",System.currentTimeMillis());v.put("messages","[]");v.put("draft","");
    getWritableDatabase().insertOrThrow("conversations",null,v);return get(id);
  }
  public void save(String id,JSONArray messages,String draft) {
    validate(messages);
    if(draft==null||draft.length()>2000)throw new IllegalArgumentException("Draft too large");
    ContentValues v=new ContentValues();v.put("messages",messages.toString());v.put("draft",draft);v.put("updated",System.currentTimeMillis());
    SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
    try {
      try(Cursor c=db.rawQuery("SELECT renamed FROM conversations WHERE id=?",new String[]{id})) {
        if(!c.moveToFirst())throw new IllegalArgumentException("Missing conversation");
        if(c.getInt(0)==0&&messages.length()>0)v.put("title",titleFor(messages));
      }
      db.update("conversations",v,"id=?",new String[]{id});db.setTransactionSuccessful();
    }finally{db.endTransaction();}
  }
  public void rename(String id,String title) {
    String value=title==null?"":title.trim();
    if(value.isEmpty()||value.length()>80)throw new IllegalArgumentException("Judul harus 1–80 karakter");
    ContentValues v=new ContentValues();v.put("title",value);v.put("renamed",1);
    getWritableDatabase().update("conversations",v,"id=?",new String[]{id});
  }
  public void delete(String id) {
    getWritableDatabase().delete("conversations","id=?",new String[]{id});
    if("legacy-local".equals(id))prefs.edit().remove(LEGACY).apply();
  }
  private static String titleFor(JSONArray rows) {
    for(int i=0;i<rows.length();i++) {
      JSONObject r=rows.optJSONObject(i);
      if(r!=null&&"user".equals(r.optString("role"))) {
        String text=r.optString("text").replaceAll("\\s+"," ").trim();
        if(text.isEmpty())continue;
        return text.length()>64?text.substring(0,63)+"…":text;
      }
    }
    return "Percakapan lama";
  }
}
