package com.zikra.app.sky;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Static architecture and hills. Sky motion is drawn separately by HomeSkyView. */
final class MosqueSkyPainter {
  private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private final RectF box = new RectF();

  private void rect(Canvas c, float x, float y, float w, float h, int color) {
    p.setColor(color);
    box.set(x, y, x + w, y + h);
    c.drawRect(box, p);
  }

  private void dome(Canvas c, float x, float y, float width, float height, int color) {
    path.reset();
    path.moveTo(x, y + height);
    path.cubicTo(
        x - 3, y + height * .50f, x + width * .34f, y + height * .35f, x + width * .50f, y);
    path.cubicTo(
        x + width * .66f,
        y + height * .35f,
        x + width + 3,
        y + height * .50f,
        x + width,
        y + height);
    path.close();
    p.setColor(color);
    c.drawPath(path, p);
    p.setStrokeWidth(.8f);
    c.drawLine(x + width * .5f, y - 6, x + width * .5f, y, p);
  }

  private void tower(Canvas c, float x, float y, float h, int color) {
    rect(c, x, y, 9, h, color);
    rect(c, x - 3, y + 9, 15, 3, color);
    dome(c, x - 2, y - 18, 13, 18, color);
    rect(c, x + 3, y + 5, 2, 5, 0xFFB5A56B);
  }

  void draw(Canvas c, float width, float height, boolean light) {
    c.save();
    float scale = width / 400f;
    c.translate(0, height * .47f);
    c.scale(scale, scale);
    float bottom = height * .53f / scale;
    p.setColor(light ? 0xFF446F60 : 0xFF244B43);
    path.reset();
    path.moveTo(0, bottom);
    path.lineTo(0, 90);
    path.cubicTo(60, 57, 78, 109, 128, 75);
    path.cubicTo(196, 24, 232, 97, 290, 65);
    path.cubicTo(334, 35, 354, 74, 400, 45);
    path.lineTo(400, bottom);
    path.close();
    c.drawPath(path, p);
    p.setColor(light ? 0xFF30574A : 0xFF1A3E36);
    path.reset();
    path.moveTo(0, bottom);
    path.lineTo(0, 118);
    path.cubicTo(80, 82, 119, 135, 188, 101);
    path.cubicTo(263, 67, 316, 104, 400, 82);
    path.lineTo(400, bottom);
    path.close();
    c.drawPath(path, p);
    int ink = light ? 0xFF244A3C : 0xFF122F29;
    float floor = Math.max(136, bottom - 35);
    rect(c, 213, 86, 158, floor - 86, ink);
    dome(c, 266, 33, 70, 57, ink);
    dome(c, 221, 62, 40, 32, ink);
    dome(c, 340, 62, 33, 32, ink);
    tower(c, 244, 19, floor - 19, ink);
    tower(c, 359, 41, floor - 41, ink);
    rect(c, 207, 91, 171, 5, ink);
    for (int i = 0; i < 9; i++) {
      float x = 221 + i * 16;
      p.setColor(light ? 0xFFAFAD6B : 0xFFB4A56B);
      box.set(x, 105, x + 5, 121);
      c.drawRoundRect(box, 3, 3, p);
    }
    p.setColor(light ? 0xFFABAD78 : 0xFF9D945B);
    box.set(294, 111, 312, 143);
    c.drawRoundRect(box, 9, 9, p);
    c.restore();
  }
}
