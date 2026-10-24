package com.zikra.app.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** A sourced catalogue. Entries missing a reference are counted but never offered as verified text. */
public final class DuaCatalog {
  public final List<Entry> entries;
  public final int sourceCount, excludedWithoutReference, excludedWithoutText;
  public static final class Entry {
    public final int number;
    public final String id, title, group, arabic, latin, meaning, reference, search;
    private Entry(int number, String title, String group, String arabic, String latin,
        String meaning, String reference, String tags) {
      this.number = number; this.id = "equran-doa:" + number; this.title = title; this.group = group;
      this.arabic = arabic; this.latin = latin; this.meaning = meaning; this.reference = reference;
      search = HadithSearch.normalize(title + " " + group + " " + arabic + " " + latin
          + " " + meaning + " " + reference + " " + tags);
    }
  }
  private DuaCatalog(List<Entry> entries, int sourceCount, int excluded, int withoutText) {
    this.entries = Collections.unmodifiableList(entries);
    this.sourceCount = sourceCount; this.excludedWithoutReference = excluded; this.excludedWithoutText = withoutText;
  }
  private static String text(JSONObject row, String key, int limit, boolean required) throws JSONException {
    Object value = row.get(key);
    if (!(value instanceof String)) throw new JSONException("Text field required");
    String result = ((String) value).trim();
    if (result.length() > limit || required && result.isEmpty()) throw new JSONException("Invalid text size");
    return result;
  }
  public static DuaCatalog parse(String json) throws JSONException {
    org.json.JSONTokener tokenizer = new org.json.JSONTokener(json);
    Object root = tokenizer.nextValue();
    if (tokenizer.nextClean() != 0) throw new JSONException("Trailing catalogue data");
    JSONArray rows = root instanceof JSONArray ? (JSONArray) root
        : root instanceof JSONObject ? ((JSONObject) root).optJSONArray("data") : null;
    if (rows == null || rows.length() < 1 || rows.length() > 500) throw new JSONException("Invalid catalogue size");
    List<Entry> entries = new ArrayList<>(); Set<Integer> seen = new HashSet<>(); int excluded = 0, withoutText = 0;
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      Object rawId = row.get("id");
      if (!(rawId instanceof Integer)) throw new JSONException("Integer ID required");
      int id = (Integer) rawId;
      if (id < 1 || id > 100000 || !seen.add(id)) throw new JSONException("Invalid or duplicate ID");
      String title = text(row, "nama", 300, true), group = text(row, "grup", 300, true);
      String arab = text(row, "ar", 20000, false), latin = text(row, "tr", 30000, false);
      String meaning = text(row, "idn", 30000, false), reference = text(row, "tentang", 80000, false);
      JSONArray tags = row.getJSONArray("tag");
      if (tags.length() > 30) throw new JSONException("Too many tags");
      StringBuilder tagText = new StringBuilder();
      for (int j = 0; j < tags.length(); j++) {
        if (!(tags.get(j) instanceof String) || tags.getString(j).length() > 100)
          throw new JSONException("Invalid tag");
        tagText.append(' ').append(tags.getString(j));
      }
      if (reference.isEmpty()) { excluded++; continue; }
      if (arab.isEmpty() || meaning.isEmpty()) { withoutText++; continue; }
      entries.add(new Entry(id, title, group, arab, latin, meaning, reference, tagText.toString()));
    }
    if (entries.isEmpty()) throw new JSONException("No referenced entries");
    return new DuaCatalog(entries, rows.length(), excluded, withoutText);
  }
}
