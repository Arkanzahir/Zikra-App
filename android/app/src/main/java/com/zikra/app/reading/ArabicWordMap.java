package com.zikra.app.reading;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Full sequence match to source words; count-only matching can color the wrong Arabic word. */
public final class ArabicWordMap {
  private static final Pattern TOKEN = Pattern.compile("\\S+");
  public final int[] starts, ends;

  private ArabicWordMap(List<Integer> starts, List<Integer> ends) {
    this.starts = new int[starts.size()];
    this.ends = new int[ends.size()];
    for (int i = 0; i < starts.size(); i++) {
      this.starts[i] = starts.get(i);
      this.ends[i] = ends.get(i);
    }
  }

  private static String key(String text) {
    StringBuilder result = new StringBuilder();
    text.codePoints()
        .forEach(
            cp -> {
              // Uthmani marks, tatweel and ayah/waqf signs do not alter lexical order.
              if (cp == 0x640) return;
              if (Character.isLetter(cp)
                  && Character.UnicodeScript.of(cp) == Character.UnicodeScript.ARABIC
                  && !(cp >= 0x6D6 && cp <= 0x6ED)
                  && cp != 0x670) result.appendCodePoint(cp == 0x671 ? 0x627 : cp);
            });
    return result.toString();
  }

  public static ArabicWordMap match(String verse, List<String> source) {
    if (verse == null || source == null || source.isEmpty()) return null;
    List<Integer> starts = new ArrayList<>(), ends = new ArrayList<>();
    Matcher matcher = TOKEN.matcher(verse);
    int index = 0;
    while (matcher.find()) {
      String word = key(matcher.group());
      if (word.isEmpty()) continue;
      if (index >= source.size() || !word.equals(key(source.get(index)))) return null;
      starts.add(matcher.start());
      ends.add(matcher.end());
      index++;
    }
    return index == source.size() ? new ArabicWordMap(starts, ends) : null;
  }
}
