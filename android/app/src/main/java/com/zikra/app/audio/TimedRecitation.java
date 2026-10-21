package com.zikra.app.audio;

import com.zikra.app.reading.mushaf.WordTimeline;
import java.util.*;
import okhttp3.HttpUrl;
import org.json.*;

/** Audio URL and word offsets from the same ayah-recitation record, never from a qari name. */
public final class TimedRecitation {
  public final int chapter, verse, wordCount;
  public final long reportedDurationMs;
  public final String url, revision;
  public final List<WordTimeline.Segment> segments;

  public static int id(String reciter) {
    return new int[] {7, 6, 9}[Recitation.reciterIndex(reciter)];
  }

  public static String audioUrl(String value) {
    String absolute =
        value.startsWith("//")
            ? "https:" + value
            : value.contains("://") ? value : "https://verses.quran.foundation/" + value;
    HttpUrl url = HttpUrl.parse(absolute);
    if (url == null
        || !url.isHttps()
        || !url.username().isEmpty()
        || !url.password().isEmpty()
        || !(url.host().equals("verses.quran.foundation")
            || url.host().equals("mirrors.quranicaudio.com")))
      throw new IllegalArgumentException("Unrecognized recitation origin");
    return url.toString();
  }

  public TimedRecitation(int chapter, int verse, String audioJson, String wordsJson)
      throws JSONException {
    Recitation.globalVerse(chapter, verse);
    String key = chapter + ":" + verse;
    JSONArray files = new JSONObject(audioJson).getJSONArray("audio_files");
    if (files.length() != 1) throw new JSONException("Expected one recording");
    JSONObject audio = files.getJSONObject(0);
    JSONObject ayah = new JSONObject(wordsJson).getJSONObject("verse");
    if (!key.equals(audio.getString("verse_key")) || !key.equals(ayah.getString("verse_key")))
      throw new JSONException("Wrong ayah");
    this.chapter = chapter;
    this.verse = verse;
    url = audioUrl(audio.getString("url"));
    reportedDurationMs = Math.round(audio.getDouble("duration") * 1000);
    JSONArray words = ayah.getJSONArray("words");
    int count = 0;
    for (int i = 0; i < words.length(); i++) {
      JSONObject word = words.getJSONObject(i);
      if ("word".equals(word.getString("char_type_name"))) {
        if (word.getInt("position") != ++count) throw new JSONException("Word position gap");
      } else if (!"end".equals(word.getString("char_type_name")))
        throw new JSONException("Unexpected word type");
    }
    wordCount = count;
    List<WordTimeline.Segment> parsed = new ArrayList<>();
    JSONArray offsets = audio.optJSONArray("segments");
    if (offsets != null) {
      try {
        for (int i = 0; i < offsets.length(); i++) {
          JSONArray row = offsets.getJSONArray(i);
          if (row.length() != 4 || row.getInt(0) != i || row.getInt(1) != i + 1)
            throw new IllegalArgumentException("Unrecognized timing format");
          parsed.add(new WordTimeline.Segment(row.getInt(1), row.getLong(2), row.getLong(3)));
        }
        long end = parsed.isEmpty() ? 0 : parsed.get(parsed.size() - 1).endMs;
        new WordTimeline(
            chapter,
            verse,
            count,
            Math.max(end, reportedDurationMs),
            "0000000000000000000000000000000000000000000000000000000000000000",
            "validate",
            parsed);
      } catch (IllegalArgumentException | JSONException invalid) {
        parsed.clear();
      }
    }
    segments = Collections.unmodifiableList(parsed);
    revision = "quran.com/v4:" + url + ":" + Integer.toHexString(audioJson.hashCode());
  }

  public WordTimeline timeline(String sha256, long decodedDurationMs) {
    if (segments.isEmpty() || Math.abs(decodedDurationMs - reportedDurationMs) > 1500) return null;
    try {
      return new WordTimeline(
          chapter, verse, wordCount, decodedDurationMs, sha256, revision, segments);
    } catch (IllegalArgumentException mismatch) {
      return null;
    }
  }
}
