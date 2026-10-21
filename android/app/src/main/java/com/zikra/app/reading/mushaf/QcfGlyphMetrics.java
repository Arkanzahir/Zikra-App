package com.zikra.app.reading.mushaf;

import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.util.*;

/** Original SFNT cmap/hmtx metrics. Font bytes are never rewritten or re-encoded. */
public final class QcfGlyphMetrics {
  private final ByteBuffer bytes;
  private final int cmap, format, hmtx, metrics, glyphCount;
  public final int unitsPerEm;

  public QcfGlyphMetrics(File file) throws IOException {
    byte[] data = Files.readAllBytes(file.toPath());
    if (data.length < 64 || data.length > 3 * 1024 * 1024)
      throw new IOException("Invalid font size");
    bytes = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
    try {
      int count = u16(4);
      if (count < 1 || count > 100) throw new IOException("Invalid table directory");
      Map<String, Integer> tables = new HashMap<>();
      for (int i = 0; i < count; i++) {
        int at = 12 + i * 16, offset = u32(at + 8), length = u32(at + 12);
        if (offset < 0 || length < 0 || offset > data.length - length)
          throw new IOException("Invalid table");
        tables.put(new String(data, at, 4, java.nio.charset.StandardCharsets.US_ASCII), offset);
      }
      unitsPerEm = u16(required(tables, "head") + 18);
      metrics = u16(required(tables, "hhea") + 34);
      glyphCount = u16(required(tables, "maxp") + 4);
      hmtx = required(tables, "hmtx");
      if (unitsPerEm < 16 || metrics < 1 || metrics > glyphCount)
        throw new IOException("Invalid metrics");
      int base = required(tables, "cmap"), selected = -1, kind = 0;
      for (int i = 0; i < u16(base + 2); i++) {
        int at = base + 4 + i * 8, platform = u16(at), encoding = u16(at + 2);
        if (platform != 0 && !(platform == 3 && (encoding == 1 || encoding == 10))) continue;
        int table = base + u32(at + 4), candidate = u16(table);
        if (candidate == 12 || (candidate == 4 && kind == 0)) {
          selected = table;
          kind = candidate;
        }
      }
      if (selected < 0) throw new IOException("No Unicode cmap");
      cmap = selected;
      format = kind;
    } catch (IndexOutOfBoundsException | IllegalArgumentException error) {
      throw new IOException("Malformed font", error);
    }
  }

  private int required(Map<String, Integer> tables, String tag) throws IOException {
    if (!tables.containsKey(tag)) throw new IOException("Missing font table");
    return tables.get(tag);
  }

  private int u16(int at) {
    return bytes.getShort(at) & 0xffff;
  }

  private int u32(int at) {
    return bytes.getInt(at);
  }

  public int glyph(int cp) {
    int result = 0;
    if (format == 12) {
      int low = 0, high = u32(cmap + 12) - 1;
      while (low <= high) {
        int mid = (low + high) >>> 1, at = cmap + 16 + mid * 12;
        if (cp < u32(at)) high = mid - 1;
        else if (cp > u32(at + 4)) low = mid + 1;
        else {
          result = u32(at + 8) + cp - u32(at);
          break;
        }
      }
    } else if (cp >= 0 && cp <= 0xffff) {
      int count = u16(cmap + 6) / 2, ends = cmap + 14, starts = ends + count * 2 + 2;
      int deltas = starts + count * 2, ranges = deltas + count * 2;
      for (int i = 0; i < count; i++) {
        int start = u16(starts + i * 2), end = u16(ends + i * 2);
        if (cp > end) continue;
        if (cp < start) break;
        int delta = bytes.getShort(deltas + i * 2), offset = u16(ranges + i * 2);
        if (offset == 0) result = (cp + delta) & 0xffff;
        else {
          result = u16(ranges + i * 2 + offset + (cp - start) * 2);
          if (result != 0) result = (result + delta) & 0xffff;
        }
        break;
      }
    }
    if (result <= 0 || result >= glyphCount)
      throw new IllegalArgumentException("Missing QCF glyph");
    return result;
  }

  public float advance(int glyph, float size) {
    if (glyph < 1 || glyph >= glyphCount || size <= 0)
      throw new IllegalArgumentException("Invalid glyph metric");
    return u16(hmtx + Math.min(glyph, metrics - 1) * 4) * size / unitsPerEm;
  }

  public float width(String text, float size) {
    float width = 0;
    for (int i = 0; i < text.length(); ) {
      int cp = text.codePointAt(i);
      width += cp == ' ' ? size * .08f : advance(glyph(cp), size);
      i += Character.charCount(cp);
    }
    return width;
  }
}
