package com.zikra.app.discovery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Small, versioned offline selection; separate IDs from the changing provider catalogue. */
public final class DuaSelection {
  public static final List<String> CATEGORIES = Collections.unmodifiableList(java.util.Arrays.asList(
      "Semua", "Harian", "Belajar", "Perlindungan", "Makan", "Tidur", "Rumah",
      "Perjalanan", "Cuaca", "Pagi & petang"));

  public static final class Entry {
    public final String id, title, arabic, latin, meaning, reference, sourceUrl, sourceName, note, kind, search;
    public final int category;
    private Entry(JSONObject row) throws JSONException {
      id = required(row, "id"); title = required(row, "title");
      arabic = required(row, "arabic"); latin = required(row, "latin"); meaning = required(row, "meaning");
      reference = required(row, "reference"); sourceUrl = required(row, "sourceUrl");
      sourceName = required(row, "sourceName"); kind = required(row, "kind");
      note = row.optString("note", ""); category = row.getInt("category");
      if (!id.matches("daily:[a-z-]+") || category < 1 || category >= CATEGORIES.size()
          || !sourceUrl.startsWith("https://")
          || !(kind.equals("quran") || kind.equals("hadith") || kind.equals("athar")))
        throw new JSONException("Invalid selection metadata");
      search = HadithSearch.normalize(title + " " + arabic + " " + latin + " " + meaning + " "
          + reference + " " + CATEGORIES.get(category) + " " + note);
    }
    public boolean matches(String query, int selectedCategory) {
      return (selectedCategory == 0 || category == selectedCategory)
          && search.contains(HadithSearch.normalize(query));
    }
    public String sourceText() {
      return reference + "\n" + sourceUrl + "\n\n" + sourceName
          + (note.isEmpty() ? "" : "\n\n" + note);
    }
  }

  private DuaSelection() { }
  private static String required(JSONObject row, String key) throws JSONException {
    Object value = row.get(key);
    if (!(value instanceof String) || ((String) value).trim().isEmpty())
      throw new JSONException("Missing selection field: " + key);
    return ((String) value).trim();
  }
  public static List<Entry> parse(String json) throws JSONException {
    JSONObject root = new JSONObject(json);
    if (root.getInt("schema") != 1) throw new JSONException("Unsupported selection schema");
    JSONArray rows = root.getJSONArray("entries");
    if (rows.length() < 1 || rows.length() > 50) throw new JSONException("Invalid selection size");
    List<Entry> entries = new ArrayList<>(); Set<String> ids = new HashSet<>();
    for (int i = 0; i < rows.length(); i++) {
      Entry entry = new Entry(rows.getJSONObject(i));
      if (!ids.add(entry.id)) throw new JSONException("Duplicate selection ID");
      entries.add(entry);
    }
    return Collections.unmodifiableList(entries);
  }
  public static List<Entry> read(InputStream input) throws IOException, JSONException {
    try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096]; int count;
      while ((count = source.read(buffer)) != -1) {
        if (output.size() + count > 131072) throw new IOException("Selection too large");
        output.write(buffer, 0, count);
      }
      return parse(new String(output.toByteArray(), StandardCharsets.UTF_8));
    }
  }
}
