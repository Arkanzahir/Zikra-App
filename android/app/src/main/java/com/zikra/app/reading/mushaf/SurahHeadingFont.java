package com.zikra.app.reading.mushaf;

import android.graphics.*;
import java.io.File;
import java.util.Locale;

/** Official surah-name ligatures: 001..114 plus the separate "surah" prefix. */
public final class SurahHeadingFont {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public SurahHeadingFont(File file) {
    paint.setTypeface(Typeface.createFromFile(file));
    paint.setFontFeatureSettings("'liga' 1, 'rlig' 1");
    paint.setTextSize(100);
    if (!paint.hasGlyph("001") || !paint.hasGlyph("surah"))
      throw new IllegalArgumentException("Surah-name ligatures unavailable");
  }

  public Path outline(int chapter) {
    if (chapter < 1 || chapter > 114) throw new IllegalArgumentException("Invalid surah");
    String code = String.format(Locale.ROOT, "%03d", chapter);
    Path name = new Path(), prefix = new Path();
    paint.getTextPath(code, 0, code.length(), 0, 0, name);
    paint.getTextPath("surah", 0, 5, 0, 0, prefix);
    RectF nameBounds = new RectF(), prefixBounds = new RectF();
    name.computeBounds(nameBounds, true);
    prefix.computeBounds(prefixBounds, true);
    if (nameBounds.isEmpty() || prefixBounds.isEmpty())
      throw new IllegalArgumentException("Empty surah-name glyph");
    // Arabic reads right to left: the name sits left of the calligraphic surah prefix.
    prefix.offset(nameBounds.right + 5 - prefixBounds.left, 0);
    name.addPath(prefix);
    return name;
  }

  public void draw(Canvas canvas, int chapter, RectF box, int color) {
    Path path = outline(chapter);
    RectF bounds = new RectF();
    path.computeBounds(bounds, true);
    float scale = Math.min(box.width() / bounds.width(), box.height() / bounds.height());
    canvas.save();
    canvas.clipRect(box);
    canvas.translate(box.centerX(), box.centerY());
    canvas.scale(scale, scale);
    canvas.translate(-bounds.centerX(), -bounds.centerY());
    paint.setColor(color);
    canvas.drawPath(path, paint);
    canvas.restore();
  }
}
