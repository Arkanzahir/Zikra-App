package com.zikra.app.reading;

import android.content.Context;
import com.zikra.app.R;
import java.io.*;
import java.util.*;
import org.json.*;

/**
 * Public Quran.com chapter metadata, bundled for catalog availability without bulk verse download.
 */
public final class QuranChapters {
  public static final class Chapter {
    public final int id, count;
    public final String name, arabic, meaning, place;

    Chapter(JSONObject row) throws JSONException {
      id = row.getInt("id");
      count = row.getInt("verses_count");
      name = row.getString("name_simple");
      arabic = row.getString("name_arabic");
      meaning = row.getJSONObject("translated_name").getString("name");
      place = row.getString("revelation_place").equals("makkah") ? "Makkiyah" : "Madaniyah";
    }
  }

  private static List<Chapter> values;

  public static synchronized List<Chapter> all(Context context) {
    if (values != null) return values;
    try (InputStream in = context.getResources().openRawResource(R.raw.quran_chapters);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[4096];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
      JSONArray rows = new JSONObject(out.toString("UTF-8")).getJSONArray("chapters");
      if (rows.length() != 114) throw new JSONException("Incomplete catalog");
      List<Chapter> list = new ArrayList<>();
      int count = 0;
      for (int i = 0; i < 114; i++) {
        Chapter c = new Chapter(rows.getJSONObject(i));
        if (c.id != i + 1 || c.count < 1 || c.count > 286)
          throw new JSONException("Invalid chapter");
        count += c.count;
        list.add(c);
      }
      if (count != 6236) throw new JSONException("Invalid verse total");
      values = Collections.unmodifiableList(list);
      return values;
    } catch (IOException | JSONException e) {
      throw new IllegalStateException("Bundled Quran metadata invalid", e);
    }
  }

  private QuranChapters() {}
}
