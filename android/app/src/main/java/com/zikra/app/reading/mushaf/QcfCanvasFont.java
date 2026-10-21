package com.zikra.app.reading.mushaf;

import android.annotation.TargetApi;
import android.graphics.*;
import android.graphics.fonts.Font;
import java.io.*;

/** Android 12+ drawing bypasses Unicode combining-mark shaping of QCF glyph codes. */
@TargetApi(31)
public final class QcfCanvasFont {
  public final QcfGlyphMetrics metrics;
  private final Font font;

  public QcfCanvasFont(File file) throws IOException {
    metrics = new QcfGlyphMetrics(file);
    font = new Font.Builder(file).build();
  }

  public void draw(Canvas canvas, String text, float right, float baseline, Paint paint) {
    int count = text.codePointCount(0, text.length());
    int[] ids = new int[count];
    float[] positions = new float[count * 2];
    int rendered = 0;
    for (int i = 0, at = 0; i < count; i++) {
      int cp = text.codePointAt(at);
      at += Character.charCount(cp);
      if (cp == ' ') {
        right -= paint.getTextSize() * .08f;
        continue;
      }
      ids[rendered] = metrics.glyph(cp);
      right -= metrics.advance(ids[rendered], paint.getTextSize());
      positions[rendered * 2] = right;
      positions[rendered * 2 + 1] = baseline;
      rendered++;
    }
    canvas.drawGlyphs(ids, 0, positions, 0, rendered, font, paint);
  }

  public RectF inkBounds(String text, Paint paint) {
    RectF result = new RectF();
    float right = 0;
    for (int at = 0; at < text.length(); ) {
      int cp = text.codePointAt(at);
      at += Character.charCount(cp);
      if (cp == ' ') {
        right -= paint.getTextSize() * .08f;
        continue;
      }
      int glyph = metrics.glyph(cp);
      right -= metrics.advance(glyph, paint.getTextSize());
      RectF box = new RectF();
      font.getGlyphBounds(glyph, paint, box);
      box.offset(right, 0);
      result.union(box);
    }
    return result;
  }
}
