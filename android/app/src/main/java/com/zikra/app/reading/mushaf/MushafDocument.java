package com.zikra.app.reading.mushaf;

import com.zikra.app.audio.Recitation;
import java.util.*;
import org.json.*;

/** Public v4 page adapter. Raw database word IDs are not consecutive reading positions. */
public final class MushafDocument {
  public final MushafPage page;
  public final int juz;
  public final String accessibleText;

  private MushafDocument(MushafPage page, int juz, String accessibleText) {
    this.page = page;
    this.juz = juz;
    this.accessibleText = accessibleText;
  }

  public static MushafDocument parse(String json, int number) throws JSONException {
    if (number < 1 || number > 604) throw new JSONException("Invalid QCF V2 page");
    JSONObject root = new JSONObject(json), pagination = root.getJSONObject("pagination");
    JSONArray verses = root.getJSONArray("verses");
    if (verses.length() == 0
        || pagination.getInt("current_page") != 1
        || pagination.getInt("total_pages") != 1
        || !pagination.isNull("next_page")
        || pagination.getInt("total_records") != verses.length())
      throw new JSONException("Incomplete mushaf page response");
    TreeMap<Integer, List<MushafPage.Word>> groups = new TreeMap<>();
    int ordinal = 0, lastVerse = 0, lastLine = 0, juz = 0;
    StringBuilder readable = new StringBuilder();
    for (int i = 0; i < verses.length(); i++) {
      JSONObject verse = verses.getJSONObject(i);
      String[] key = verse.getString("verse_key").split(":");
      if (key.length != 2) throw new JSONException("Invalid verse key");
      int chapter, ayah, global;
      try {
        chapter = Integer.parseInt(key[0]);
        ayah = Integer.parseInt(key[1]);
        global = Recitation.globalVerse(chapter, ayah);
      } catch (IllegalArgumentException invalid) {
        throw new JSONException("Invalid verse number");
      }
      if (lastVerse > 0 && global != lastVerse + 1) throw new JSONException("Verse order mismatch");
      lastVerse = global;
      int verseJuz = verse.getInt("juz_number");
      if (verseJuz < 1 || verseJuz > 30) throw new JSONException("Invalid juz");
      if (juz == 0) juz = verseJuz;
      JSONArray words = verse.getJSONArray("words");
      int previousPosition = 0, included = 0;
      for (int j = 0; j < words.length(); j++) {
        JSONObject word = words.getJSONObject(j);
        int position = word.getInt("position");
        if (previousPosition > 0 && position != previousPosition + 1)
          throw new JSONException("Missing or duplicate word position");
        previousPosition = position;
        if (word.getInt("page_number") != number) continue;
        int line = word.getInt("line_number");
        String glyph = word.getString("code_v2"), kind = word.getString("char_type_name");
        if (line < 1
            || line > 15
            || line < lastLine
            || glyph.trim().isEmpty()
            || !("word".equals(kind) || "end".equals(kind)))
          throw new JSONException("Missing glyph/type/line metadata");
        lastLine = line;
        groups
            .computeIfAbsent(line, ignored -> new ArrayList<>())
            .add(
                new MushafPage.Word(++ordinal, chapter, ayah, position, glyph, "end".equals(kind)));
        String text = word.optString("text_uthmani", "");
        if (!text.isEmpty()) readable.append(text).append(' ');
        included++;
      }
      if (included == 0) throw new JSONException("Verse has no words on requested page");
    }
    List<MushafPage.Line> lines = new ArrayList<>();
    for (Map.Entry<Integer, List<MushafPage.Word>> row : groups.entrySet()) {
      List<MushafPage.Word> words = row.getValue();
      MushafPage.Word first = words.get(0), last = words.get(words.size() - 1);
      boolean centered =
          number <= 2
              || (last.verse == Recitation.count(last.chapter)
                  && (row.getKey() == groups.lastKey()
                      || groups.get(groups.higherKey(row.getKey())).get(0).chapter
                          != last.chapter));
      lines.add(
          new MushafPage.Line(row.getKey(), MushafPage.Kind.AYAH, first.chapter, centered, words));
    }
    try {
      return new MushafDocument(
          new MushafPage(
              "qcf-v2-mushaf-1", "public-v4-online", "p" + number + ".ttf", number, 604, 15, lines),
          juz,
          readable.toString().trim());
    } catch (IllegalArgumentException invalid) {
      throw new JSONException("Invalid page word layout");
    }
  }
}
