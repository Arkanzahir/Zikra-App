package com.zikra.app.reading.mushaf;

import android.graphics.*;
import java.io.*;

/** Verified QCF V2 page 1, Al-Fatihah 1:1 words 1–4, without the verse medallion. */
public final class BasmalahRenderer {
  public static final String QCF_BASMALAH = "\ufc41 \ufc42 \ufc43 \ufc44";
  private final QcfCanvasFont font;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF bounds;

  public BasmalahRenderer(File file) throws IOException {
    font = new QcfCanvasFont(file);
    paint.setTextSize(100);
    bounds = font.inkBounds(QCF_BASMALAH, paint);
    if (bounds.isEmpty()) throw new IOException("Missing basmalah outlines");
  }

  public RectF inkBounds() {
    return new RectF(bounds);
  }

  public void draw(Canvas canvas, RectF box, int color) {
    float scale = Math.min(box.width() / bounds.width(), box.height() / bounds.height());
    canvas.save();
    canvas.clipRect(box);
    canvas.translate(box.centerX(), box.centerY());
    canvas.scale(scale, scale);
    canvas.translate(-bounds.centerX(), -bounds.centerY());
    paint.setColor(color);
    font.draw(canvas, QCF_BASMALAH, 0, 0, paint);
    canvas.restore();
  }
}
