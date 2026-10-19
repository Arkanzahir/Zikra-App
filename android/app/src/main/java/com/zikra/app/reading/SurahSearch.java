package com.zikra.app.reading;

import java.text.Normalizer;
import java.util.Locale;

/** Search normalization only. Quran text and displayed chapter names are never modified. */
public final class SurahSearch {
  private SurahSearch() {}

  public static String normalize(String value) {
    if (value == null) return "";
    String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD).toLowerCase(Locale.ROOT);
    StringBuilder result = new StringBuilder();
    for (int offset = 0; offset < decomposed.length(); ) {
      int c = decomposed.codePointAt(offset);
      offset += Character.charCount(c);
      int kind = Character.getType(c);
      if (kind == Character.NON_SPACING_MARK
          || kind == Character.COMBINING_SPACING_MARK
          || kind == Character.ENCLOSING_MARK
          || Character.isWhitespace(c)
          || c == 0x0640 // Tatweel, used only for presentation.
          || c == '\''
          || c == 0x2019
          || c == '-') continue;
      if (c == 0x0671) c = 0x0627; // Alef wasla; other alef accents decompose above.
      if (c == 0x0649) c = 0x064A; // Final alef maqsura / ya in search input.
      if (c >= 0x0660 && c <= 0x0669) c = '0' + c - 0x0660;
      if (c >= 0x06F0 && c <= 0x06F9) c = '0' + c - 0x06F0;
      result.appendCodePoint(c);
    }
    return result.toString();
  }

  public static boolean matches(String query, int id, String name, String meaning, String arabic) {
    String normalized = normalize(query);
    return normalized.isEmpty()
        || String.valueOf(id).equals(normalized)
        || normalize(name).contains(normalized)
        || normalize(meaning).contains(normalized)
        || normalize(arabic).contains(normalized);
  }
}
