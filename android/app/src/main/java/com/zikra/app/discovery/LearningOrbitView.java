package com.zikra.app.discovery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Resolution-independent welcome artwork, drawn entirely with Android Canvas. */
public final class LearningOrbitView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public LearningOrbitView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float size = Math.min(getWidth(), getHeight());
    float cx = getWidth() / 2f, cy = getHeight() / 2f;
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(size * .003f);
    for (float r : new float[] {.20f, .32f, .43f}) {
      paint.setColor(Color.parseColor("#3B6158"));
      canvas.drawCircle(cx, cy, size * r, paint);
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.parseColor("#E5A853"));
    canvas.drawCircle(cx, cy, size * .10f, paint);
    paint.setColor(Color.parseColor("#15332F"));
    canvas.drawCircle(cx + size * .04f, cy - size * .03f, size * .09f, paint);
    String[] colors = {"#E6F5EE", "#F6E0AA", "#DECFF2", "#F8DAD2", "#C9E3F1", "#E4EFC9"};
    for (int i = 0; i < 6; i++) {
      double angle = Math.toRadians(i * 60 - 70);
      float radius = size * (i % 2 == 0 ? .43f : .32f);
      float x = cx + (float) Math.cos(angle) * radius;
      float y = cy + (float) Math.sin(angle) * radius;
      float disc = size * .063f;
      paint.setColor(Color.parseColor(colors[i]));
      canvas.drawCircle(x, y, disc, paint);
      paint.setColor(Color.parseColor("#315849"));
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(size * .006f);
      if (i % 3 == 0) {
        canvas.drawRoundRect(
            new RectF(x - disc * .55f, y - disc * .40f, x + disc * .55f, y + disc * .40f),
            disc * .1f,
            disc * .1f,
            paint);
        canvas.drawLine(x, y - disc * .4f, x, y + disc * .4f, paint);
      } else if (i % 3 == 1) {
        canvas.drawCircle(x, y - disc * .24f, disc * .22f, paint);
        canvas.drawArc(
            new RectF(x - disc * .45f, y, x + disc * .45f, y + disc * .70f),
            180,
            180,
            false,
            paint);
      } else {
        canvas.drawLine(x - disc * .45f, y, x + disc * .45f, y, paint);
        canvas.drawLine(x, y - disc * .45f, x, y + disc * .45f, paint);
        canvas.drawCircle(x, y, disc * .30f, paint);
      }
      paint.setStyle(Paint.Style.FILL);
    }
    paint.setColor(Color.parseColor("#87B3A1"));
    for (int i = 0; i < 12; i++) {
      double a = Math.toRadians(i * 30 + 10);
      canvas.drawCircle(
          cx + (float) Math.cos(a) * size * .46f,
          cy + (float) Math.sin(a) * size * .46f,
          size * .004f,
          paint);
    }
  }
}
