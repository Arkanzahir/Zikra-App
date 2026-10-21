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
    for (int i = 0, at = 0; i < count; i++) {
      int cp = text.codePointAt(at);
      at += Character.charCount(cp);
      ids[i] = metrics.glyph(cp);
      right -= metrics.advance(ids[i], paint.getTextSize());
      positions[i * 2] = right;
      positions[i * 2 + 1] = baseline;
    }
    canvas.drawGlyphs(ids, 0, positions, 0, count, font, paint);
  }
}
