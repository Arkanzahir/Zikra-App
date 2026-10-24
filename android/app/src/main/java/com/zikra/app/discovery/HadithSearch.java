package com.zikra.app.discovery;

import java.text.Normalizer;
import java.util.Locale;

/** Search normalization never changes the Arabic or translation displayed to the reader. */
public final class HadithSearch {
  private HadithSearch() {}

  public static String normalize(String value) {
    String text = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD);
    StringBuilder out = new StringBuilder();
    for (int offset = 0; offset < text.length();) {
      int cp = text.codePointAt(offset);
      offset += Character.charCount(cp);
      if (Character.getType(cp) == Character.NON_SPACING_MARK || cp == 0x0640) continue;
      int digit = Character.digit(cp, 10);
      if (digit >= 0) out.append((char) ('0' + digit));
      else out.appendCodePoint(cp == 0x0671 ? 0x0627 : cp);
    }
    return out.toString().toLowerCase(Locale.ROOT).trim();
  }

  public static String literalLike(String normalized) {
    return "%" + normalized.replace("\\", "\\\\").replace("%", "\\%")
        .replace("_", "\\_") + "%";
  }
}
