package com.zikra.app.migration;

import android.graphics.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.JSONObject;

/** Inventory and image bounds prevent restored chat references from bypassing existing decoder limits. */
final class BackupMedia {
  private BackupMedia() {}
  static Map<String, File> collect(File files) throws Exception {
    Map<String, File> entries = new LinkedHashMap<>();
    for (String group : new String[]{"assistant_attachments", "assistant_images"}) {
      File root = new File(files, group);
      if (root.exists()) collect(root, "media/" + group, entries, 0);
    }
    return entries;
  }
  private static void collect(File file, String name, Map<String, File> entries, int depth) throws Exception {
    if (depth > 4 || Files.isSymbolicLink(file.toPath())) throw new IOException("Invalid backup media path");
    if (file.isDirectory()) {
      File[] children = file.listFiles(); if (children == null) throw new IOException("Backup media unavailable");
      for (File child : children) collect(child, name + "/" + child.getName(), entries, depth + 1);
    } else {
      if (!BackupArchive.allowed(name) || entries.size() >= BackupArchive.MAX_FILES) throw new IOException("Backup media is changing or unsupported");
      entries.put(name, file);
    }
  }
  static Map<String, File> forSnapshot(Map<String, File> entries, org.json.JSONObject data, boolean rejectOrphans) throws Exception {
    Set<String> chats = new HashSet<>(); JSONObject database = data.getJSONObject("databases").optJSONObject("assistant_chats.db");
    if (database != null) {
      JSONObject table = database.getJSONObject("tables").getJSONObject("conversations");
      org.json.JSONArray columns = table.getJSONArray("columns"), rows = table.getJSONArray("rows"); int id = -1;
      for (int i = 0; i < columns.length(); i++) if (columns.getString(i).equals("id")) id = i;
      if (id < 0) throw new IOException("Conversation identity missing");
      for (int i = 0; i < rows.length(); i++) chats.add(rows.getJSONArray(i).getString(id));
    }
    Map<String, File> selected = new LinkedHashMap<>();
    for (Map.Entry<String, File> entry : entries.entrySet()) {
      String[] parts = entry.getKey().split("/");
      if (parts.length > 3 && chats.contains(parts[2])) selected.put(entry.getKey(), entry.getValue());
      else if (rejectOrphans) throw new IOException("Unreferenced conversation media");
    }
    return selected;
  }
  static void validate(File files) throws Exception { validate(collect(files)); }
  static void validate(Map<String, File> entries) throws Exception {
    for (Map.Entry<String, File> item : entries.entrySet()) {
      String name = item.getKey(); File file = item.getValue();
      if (name.endsWith(".jpg") || name.endsWith(".png")) {
        boolean generated = name.startsWith("media/assistant_images/");
        if (file.length() < 1 || file.length() > (generated ? 2000000 : 360000)) throw new IOException("Invalid restored image size");
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        String mime = name.endsWith(".png") ? "image/png" : "image/jpeg";
        if (!mime.equals(bounds.outMimeType) || bounds.outWidth < (generated ? 64 : 1) || bounds.outHeight < (generated ? 64 : 1)
            || bounds.outWidth > 1536 || bounds.outHeight > 1536 || (long) bounds.outWidth * bounds.outHeight > 2100000)
          throw new IOException("Invalid restored image dimensions");
        BitmapFactory.Options sample = new BitmapFactory.Options(); sample.inSampleSize = 8;
        Bitmap check = BitmapFactory.decodeFile(file.getPath(), sample);
        if (check == null) throw new IOException("Damaged restored image"); check.recycle();
      } else if (name.endsWith("text.txt")) {
        if (file.length() > 48000) throw new IOException("Restored attachment text exceeds budget");
        String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(file.toPath()))).toString();
        if (text.length() > 12000 || text.indexOf('\0') >= 0) throw new IOException("Invalid restored text");
      } else {
        if (file.length() > 8192) throw new IOException("Attachment metadata exceeds budget");
        JSONObject json = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        if (!json.getString("id").matches("[a-f0-9-]{36}")) throw new IOException("Invalid attachment identifier");
        if (name.endsWith("meta.json")) {
          if (!json.getString("id").equals(file.getParentFile().getName())
              || !new File(file.getParentFile(), "text.txt").isFile()
              || !Arrays.asList("photo", "pdf", "docx", "text").contains(json.getString("kind"))
              || json.getString("name").length() > 256) throw new IOException("Invalid attachment metadata");
          int images = json.getInt("images");
          if (images < 0 || images > 3 || json.getInt("pages") < 1 || json.getInt("pages") > 3
              || json.getLong("bytes") < 0 || json.getLong("bytes") > 8000000)
            throw new IOException("Invalid attachment metadata");
          for (int i = 0; i < images; i++) if (!new File(file.getParentFile(), "image-" + i + ".jpg").isFile())
            throw new IOException("Restored attachment image missing");
        } else if (!new File(file.getParentFile(), json.getString("id") + "/meta.json").isFile())
          throw new IOException("Restored draft is incomplete");
      }
    }
  }
}
