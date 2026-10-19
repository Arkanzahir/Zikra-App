package com.zikra.app.assistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Static resolution-independent voice cue in the shared pine, sage and gold palette. */
public final class AssistantOrbView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private boolean listening;

  public AssistantOrbView(Context context, AttributeSet attributes) {
    super(context, attributes);
  }

  public void setListening(boolean value) {
    listening = value;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float cx = getWidth() / 2f;
    float cy = getHeight() / 2f;
    float radius = Math.min(cx, cy) * .82f;
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(
        new RadialGradient(
            cx - radius * .36f,
            cy - radius * .4f,
            radius * 1.8f,
            new int[] {
              Color.WHITE,
              Color.parseColor("#D5E2D8"),
              Color.parseColor("#659B86"),
              Color.parseColor("#15332F")
            },
            new float[] {0f, .28f, .66f, 1f},
            Shader.TileMode.CLAMP));
    canvas.drawCircle(cx, cy, radius, paint);
    paint.setShader(null);
    canvas.save();
    path.reset();
    path.addCircle(cx, cy, radius, Path.Direction.CW);
    canvas.clipPath(path);
    canvas.rotate(-32, cx, cy);
    paint.setStyle(Paint.Style.STROKE);
    for (int band = 0; band < 16; band++) {
      float offset = (band - 7.5f) * radius * .15f;
      path.reset();
      for (int step = 0; step <= 64; step++) {
        float x = cx - radius * 1.4f + step * radius * 2.8f / 64f;
        float progress = (x - cx) / radius;
        float y = cy + offset + (float) Math.sin(progress * 2.4f + band * .36f) * radius * .44f;
        if (step == 0) path.moveTo(x, y);
        else path.lineTo(x, y);
      }
      paint.setStrokeWidth(radius * (band % 3 == 0 ? .068f : .025f));
      paint.setColor(
          Color.parseColor(band % 3 == 0 ? "#E5A853" : band % 3 == 1 ? "#D5E2D8" : "#315849"));
      paint.setAlpha(band % 3 == 0 ? 185 : 155);
      canvas.drawPath(path, paint);
    }
    canvas.restore();
    paint.setAlpha(255);
    paint.setColor(Color.parseColor("#659B86"));
    paint.setStrokeWidth(Math.max(1, radius * .016f));
    canvas.drawCircle(cx, cy, radius, paint);
    if (listening) {
      paint.setColor(Color.parseColor("#659B86"));
      paint.setAlpha(95);
      canvas.drawCircle(cx, cy, radius * 1.12f, paint);
      paint.setAlpha(255);
    }
  }
}

