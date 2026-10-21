package com.zikra.app.reading.mushaf;

import android.content.Context;
import android.graphics.*;
import com.zikra.app.R;

/** User-supplied frame with native medallions; Quran text remains independent. */
final class MushafOrnaments {
  private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint numeral = new Paint(Paint.ANTI_ALIAS_FLAG);

  private final Bitmap reference;
  private final Paint image = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

  MushafOrnaments(Context context) {
    reference =
        BitmapFactory.decodeResource(context.getResources(), R.drawable.mushaf_teal_reference);
    line.setStyle(Paint.Style.STROKE);
    line.setStrokeJoin(Paint.Join.ROUND);
    line.setStrokeCap(Paint.Cap.ROUND);
    numeral.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    numeral.setTextAlign(Paint.Align.CENTER);
  }

  void border(Canvas canvas, float width, float height, boolean night) {
    float scale = width / reference.getWidth();
    image.setColorFilter(
        night
            ? new ColorMatrixColorFilter(
                new ColorMatrix(
                    new float[] {
                      -.1488f, -.5006f, -.0506f, 0, 189, -.1807f, -.6079f, -.0614f, 0, 246, -.1807f,
                      -.6079f, -.0614f, 0, 242, 0, 0, 0, 1, 0
                    }))
            : null);
    // Source image stays unchanged. Draw only its frame; app text occupies the title center.
    slice(canvas, 0, 0, 650, 60, 0, 0, width, 60 * scale);
    slice(canvas, 0, 737, 650, 877, 0, height - 140 * scale, width, height);
    slice(canvas, 0, 60, 177, 136, 0, 60 * scale, 177 * scale, 136 * scale);
    slice(canvas, 477, 60, 650, 136, 477 * scale, 60 * scale, width, 136 * scale);
    slice(canvas, 177, 60, 477, 68, 177 * scale, 60 * scale, 477 * scale, 68 * scale);
    slice(canvas, 177, 128, 477, 136, 177 * scale, 128 * scale, 477 * scale, 136 * scale);
    float end = height - 140 * scale;
    slice(canvas, 0, 136, 60, 737, 0, 136 * scale, 60 * scale, end);
    slice(canvas, 585, 136, 650, 737, 585 * scale, 136 * scale, width, end);
  }

  private void slice(
      Canvas canvas,
      int left,
      int top,
      int right,
      int bottom,
      float x1,
      float y1,
      float x2,
      float y2) {
    canvas.drawBitmap(
        reference, new Rect(left, top, right, bottom), new RectF(x1, y1, x2, y2), image);
  }

  void corner(
      Canvas canvas, float x, float y, float scale, int horizontal, int vertical, int color) {
    canvas.save();
    canvas.translate(x, y);
    canvas.scale(scale * horizontal, scale * vertical);
    line.setColor(color);
    line.setStrokeWidth(.65f);
    Path stem = new Path();
    stem.moveTo(1, 40);
    stem.cubicTo(18, 39, 38, 19, 40, 1);
    canvas.drawPath(stem, line);
    stem.reset();
    stem.moveTo(1, 27);
    stem.cubicTo(15, 27, 27, 15, 27, 1);
    canvas.drawPath(stem, line);
    for (int i = 0; i < 5; i++) {
      float t = i / 4f, px = 6 + 29 * t, py = 37 - 29 * t;
      leaf(canvas, px, py, -112, 7.5f, 2.7f);
      leaf(canvas, px, py, 17, 7.5f, 2.7f);
    }
    flower(canvas, 12, 12, 3.4f);
    flower(canvas, 4, 32, 1.7f);
    flower(canvas, 32, 4, 1.7f);
    canvas.restore();
  }

  private void leaf(Canvas canvas, float x, float y, float angle, float length, float breadth) {
    canvas.save();
    canvas.translate(x, y);
    canvas.rotate(angle);
    Path leaf = new Path();
    leaf.moveTo(0, 0);
    leaf.quadTo(length * .35f, -breadth, length, 0);
    leaf.quadTo(length * .35f, breadth, 0, 0);
    canvas.drawPath(leaf, line);
    canvas.restore();
  }

  private void flower(Canvas canvas, float x, float y, float radius) {
    for (int i = 0; i < 4; i++) leaf(canvas, x, y, 45 + 90 * i, radius, radius * .55f);
    canvas.drawCircle(x, y, radius * .22f, line);
  }

  void finial(Canvas canvas, float x, float y, float size, int direction, int gold) {
    canvas.save();
    canvas.translate(x, y);
    canvas.scale(direction * size / 32, size / 32);
    line.setColor(gold);
    line.setStrokeWidth(.65f);
    Path stem = new Path();
    stem.moveTo(0, 0);
    stem.cubicTo(10, -5, 17, 4, 27, 0);
    canvas.drawPath(stem, line);
    for (int i = 0; i < 3; i++) {
      leaf(canvas, 4 + i * 6, 0, -65, 8 - i, 2.8f);
      leaf(canvas, 4 + i * 6, 0, 65, 8 - i, 2.8f);
    }
    flower(canvas, 26, 0, 3);
    canvas.restore();
  }

  void medallion(
      Canvas canvas,
      float right,
      float baseline,
      float width,
      float size,
      int verse,
      int gold,
      int ink) {
    float cx = right - width / 2, cy = baseline - size * .31f;
    float rx = Math.min(width * .43f, size * .34f), ry = size * .43f;
    line.setColor(gold);
    line.setStrokeWidth(Math.max(1, size / 75));
    Path outline = new Path();
    for (int i = 0; i < 16; i++) {
      double angle = -Math.PI / 2 + i * Math.PI / 8;
      float radius = i % 2 == 0 ? 1 : .82f;
      float x = cx + (float) Math.cos(angle) * rx * radius;
      float y = cy + (float) Math.sin(angle) * ry * radius;
      if (i == 0) outline.moveTo(x, y);
      else outline.lineTo(x, y);
    }
    outline.close();
    canvas.drawPath(outline, line);
    canvas.save();
    canvas.scale(.84f, .86f, cx, cy);
    line.setAlpha(130);
    canvas.drawPath(outline, line);
    line.setAlpha(255);
    canvas.restore();
    StringBuilder digits = new StringBuilder();
    for (char digit : Integer.toString(verse).toCharArray())
      digits.append((char) ('٠' + digit - '0'));
    numeral.setColor(ink);
    numeral.setTextSize(size * .48f);
    float measured = numeral.measureText(digits.toString());
    if (measured > rx * 1.3f) numeral.setTextSize(numeral.getTextSize() * rx * 1.3f / measured);
    Paint.FontMetrics metrics = numeral.getFontMetrics();
    canvas.drawText(digits.toString(), cx, cy - (metrics.ascent + metrics.descent) / 2, numeral);
  }
}
