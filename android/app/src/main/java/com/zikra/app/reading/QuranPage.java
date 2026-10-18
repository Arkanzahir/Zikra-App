package com.zikra.app.reading;

import java.util.*;
import org.json.*;

/** Validates stable verse keys and pagination before any network or cached page is displayed. */
public final class QuranPage {
  public static final int PAGE_SIZE = 50, TRANSLATION = 33;
  public final int chapter, page, pages;
  public final List<Verse> verses;

  public static final class Verse {
    public final int number;
    public final String key, arabic, translation;

    Verse(int chapter, int number, String arabic, String translation) {
      this.number = number;
      key = chapter + ":" + number;
      this.arabic = arabic;
      this.translation = translation;
    }
  }

  private QuranPage(int chapter, int page, int pages, List<Verse> verses) {
    this.chapter = chapter;
    this.page = page;
    this.pages = pages;
    this.verses = Collections.unmodifiableList(verses);
  }

  public static int pageFor(int verse) {
    return (verse - 1) / PAGE_SIZE + 1;
  }

  public static QuranPage parse(String body, QuranChapters.Chapter chapter, int page)
      throws JSONException {
    JSONObject root = new JSONObject(body), p = root.getJSONObject("pagination");
    int pages = (chapter.count + PAGE_SIZE - 1) / PAGE_SIZE;
    if (page < 1
        || page > pages
        || p.getInt("per_page") != PAGE_SIZE
        || p.getInt("current_page") != page
        || p.getInt("total_pages") != pages
        || p.getInt("total_records") != chapter.count
        || (page < pages ? p.optInt("next_page", -1) != page + 1 : !p.isNull("next_page")))
      throw new JSONException("Invalid pagination");
    JSONArray rows = root.getJSONArray("verses");
    int first = (page - 1) * PAGE_SIZE + 1;
    if (rows.length() != Math.min(PAGE_SIZE, chapter.count - first + 1))
      throw new JSONException("Incomplete page");
    List<Verse> verses = new ArrayList<>();
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      int n = first + i;
      if (row.getInt("verse_number") != n
          || !row.getString("verse_key").equals(chapter.id + ":" + n))
        throw new JSONException("Verse key/order mismatch");
      String arabic = row.getString("text_uthmani").trim(), translation = null;
      JSONArray translations = row.getJSONArray("translations");
      for (int j = 0; j < translations.length(); j++)
        if (translations.getJSONObject(j).getInt("resource_id") == TRANSLATION)
          translation = translations.getJSONObject(j).getString("text").trim();
      if (arabic.isEmpty() || translation == null || translation.isEmpty())
        throw new JSONException("Missing text");
      verses.add(new Verse(chapter.id, n, arabic, translation));
    }
    return new QuranPage(chapter.id, page, pages, verses);
  }

  public static QuranPage builtIn(int id) {
    for (SurahData s : SurahData.ALL)
      if (s.number == id) {
        List<Verse> verses = new ArrayList<>();
        for (int i = 0; i < s.arabic.length; i++)
          verses.add(new Verse(id, i + 1, s.arabic[i], s.translations[i]));
        return new QuranPage(id, 1, 1, verses);
      }
    return null;
  }
}
