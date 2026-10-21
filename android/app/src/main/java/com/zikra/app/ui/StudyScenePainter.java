package com.zikra.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import com.zikra.app.R;

/** Original layered illustration. The room is fixed; face, hands and steam move separately. */
final class StudyScenePainter {
  private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private final RectF box = new RectF();
  private final int pine, mint, skin, hair, wood, gold, window, paper;

  StudyScenePainter(Context context) {
    pine = context.getColor(R.color.concept_pine);
    mint = context.getColor(R.color.concept_mint);
    skin = context.getColor(R.color.concept_skin);
    hair = context.getColor(R.color.concept_hair);
    wood = context.getColor(R.color.concept_wood);
    gold = context.getColor(R.color.concept_gold);
    window = context.getColor(R.color.concept_window);
    paper = context.getColor(R.color.concept_ivory);
  }

  private void rect(Canvas c, float x, float y, float w, float h, float radius, int color) {
    p.setStyle(Paint.Style.FILL);
    p.setColor(color);
    box.set(x, y, x + w, y + h);
    c.drawRoundRect(box, radius, radius, p);
  }

  private void oval(Canvas c, float x, float y, float w, float h, int color) {
    p.setStyle(Paint.Style.FILL);
    p.setColor(color);
    box.set(x, y, x + w, y + h);
    c.drawOval(box, p);
  }

  private void line(Canvas c, float x, float y, float x2, float y2, int color, float width) {
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(width);
    p.setStrokeCap(Paint.Cap.ROUND);
    p.setColor(color);
    c.drawLine(x, y, x2, y2, p);
    p.setStyle(Paint.Style.FILL);
  }

  private void fill(Canvas c, int color) {
    p.setColor(color);
    p.setStyle(Paint.Style.FILL);
    c.drawPath(path, p);
  }

  void draw(Canvas c, int width, int height, float time, boolean rest, boolean preview) {
    float scale =
        preview ? Math.max(width / 360f, height / 230f) : Math.min(width / 360f, height / 230f);
    c.save();
    c.translate((width - 360 * scale) / 2, (height - 230 * scale) / 2);
    c.scale(scale, scale);
    rect(c, 0, 0, 360, 230, 22, rest ? 0xFFDDE9DE : 0xFFECE9DA);
    // A quiet window and static leaves provide depth without moving the whole scene.
    rect(c, 20, 12, 118, 151, 12, 0xFFC7B18A);
    rect(c, 27, 18, 104, 139, 8, window);
    oval(c, 70, 25, 46, 46, 0xFFF4ECD0);
    oval(c, 26, 80, 64, 80, 0xFFB8CDB4);
    oval(c, 72, 105, 59, 51, 0xFF9AB69B);
    line(c, 76, 18, 76, 157, paper, 5);
    line(c, 28, 88, 131, 88, paper, 5);
    rect(c, 16, 155, 126, 9, 3, wood);
    line(c, 41, 176, 42, 120, pine, 2);
    oval(c, 23, 131, 18, 30, 0xFF71997C);
    oval(c, 43, 119, 17, 32, 0xFF7CA587);
    rect(c, 27, 161, 32, 33, 5, 0xFFB99467);
    // Chair stays still behind the student.
    rect(c, 139, 111, 99, 101, 23, 0xFF9CA78D);
    float breath = (float) Math.sin(time * 1.8f) * 1.3f;
    c.save();
    c.translate(0, breath);
    path.reset();
    path.moveTo(144, 197);
    path.cubicTo(140, 155, 142, 129, 168, 123);
    path.cubicTo(185, 118, 206, 123, 218, 139);
    path.lineTo(242, 197);
    path.close();
    fill(c, 0xFF779984);
    // Collar and neck.
    rect(c, 176, 112, 20, 24, 6, skin);
    line(c, 168, 128, 181, 138, pine, 1.5f);
    line(c, 181, 138, 198, 126, pine, 1.5f);
    c.save();
    c.rotate((float) Math.sin(time * .8f) * (rest ? 1.5f : .9f), 185, 108);
    oval(c, 158, 64, 59, 64, skin);
    oval(c, 156, 93, 11, 17, skin);
    // Hand-drawn swept hair silhouette, rather than a moving bitmap.
    path.reset();
    path.moveTo(160, 103);
    path.cubicTo(150, 90, 150, 68, 161, 63);
    path.cubicTo(154, 51, 176, 48, 181, 56);
    path.cubicTo(188, 44, 204, 53, 205, 60);
    path.cubicTo(224, 57, 227, 77, 218, 84);
    path.cubicTo(208, 89, 196, 80, 189, 71);
    path.cubicTo(185, 85, 172, 81, 167, 91);
    path.lineTo(167, 107);
    path.close();
    fill(c, hair);
    boolean blink = time % 5.2f > .0f && time % 5.2f < .16f;
    line(c, 181, 98, 187, rest || blink ? 98 : 99, hair, 1.6f);
    line(c, 201, 98, 207, rest || blink ? 98 : 99, hair, 1.6f);
    line(c, 197, 101, 195, 108, 0xFFC18A66, 1.2f);
    path.reset();
    path.moveTo(191, 116);
    path.quadTo(197, 120, 203, 114);
    p.setStyle(Paint.Style.STROKE);
    p.setColor(0xFF9F6950);
    p.setStrokeWidth(1.5f);
    c.drawPath(path, p);
    p.setStyle(Paint.Style.FILL);
    c.restore();
    if (rest) {
      // Forearms, cup and independent rising steam during break/paused pose.
      line(c, 157, 154, 184, 168, 0xFF658970, 17);
      line(c, 215, 152, 217, 169, 0xFF658970, 17);
      line(c, 184, 168, 195, 151, skin, 12);
      line(c, 217, 169, 221, 148, skin, 12);
      rect(c, 193, 135, 30, 32, 5, paper);
      p.setStyle(Paint.Style.STROKE);
      p.setColor(paper);
      p.setStrokeWidth(4);
      box.set(218, 140, 235, 157);
      c.drawOval(box, p);
      p.setStyle(Paint.Style.FILL);
      line(c, 197, 139, 218, 139, gold, 1.5f);
      for (int i = 0; i < 2; i++) {
        float drift = (time * 8 + i * 12) % 24;
        path.reset();
        path.moveTo(201 + i * 10, 129 - drift);
        path.cubicTo(
            193 + i * 10, 121 - drift, 211 + i * 10, 116 - drift, 202 + i * 10, 109 - drift);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.6f);
        p.setColor(0x90FFFFFF);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
      }
    } else {
      line(c, 153, 147, 142, 189, 0xFF658970, 19);
      line(c, 142, 189, 182, 197, skin, 12);
      line(c, 215, 147, 230, 183, 0xFF658970, 18);
      float hand = (float) Math.sin(time * 3.2f) * 2.6f;
      line(c, 230, 183, 212 + hand, 196, skin, 12);
      line(c, 211 + hand, 188, 204 + hand, 207, hair, 2);
    }
    c.restore();
    rect(c, 12, 204, 336, 12, 4, wood);
    rect(c, 27, 216, 9, 14, 1, wood);
    rect(c, 321, 216, 9, 14, 1, wood);
    // Open book remains on the desk in both states.
    path.reset();
    path.moveTo(113, 198);
    path.quadTo(143, 191, 174, 198);
    path.quadTo(200, 191, 234, 199);
    path.lineTo(234, 206);
    path.quadTo(205, 201, 174, 207);
    path.quadTo(141, 201, 111, 207);
    path.close();
    fill(c, paper);
    line(c, 174, 199, 174, 207, wood, 1);
    for (int i = 0; i < 3; i++) {
      line(c, 122, 199 + i * 2, 161, 200 + i * 2, 0xFFB9BDA9, .6f);
      line(c, 185, 200 + i * 2, 220, 199 + i * 2, 0xFFB9BDA9, .6f);
    }
    // Desk lamp and a short stack of books are fixed, with no text baked into artwork.
    line(c, 297, 191, 298, 122, pine, 3);
    line(c, 298, 122, 281, 101, pine, 3);
    path.reset();
    path.moveTo(272, 94);
    path.lineTo(285, 98);
    path.lineTo(296, 117);
    path.lineTo(260, 117);
    path.close();
    fill(c, gold);
    rect(c, 282, 192, 34, 5, 3, pine);
    rect(c, 61, 184, 38, 7, 2, pine);
    rect(c, 59, 191, 43, 6, 2, gold);
    rect(c, 63, 197, 39, 6, 2, paper);
    c.restore();
  }
}
