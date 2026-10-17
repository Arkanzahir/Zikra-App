package com.zikra.app.discovery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.zikra.app.R;

public final class LearningOrbitView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Drawable[] icons = new Drawable[8];
  private final int[] colors = {
    0xFFF7E4B5, 0xFFDCEFE7, 0xFFE8DEF5, 0xFFF7DCD1,
    0xFFD5EAF4, 0xFFE1EED3, 0xFFEADFF4, 0xFFF3E8C9
  };
  private final float[] angles = {-75, -15, 40, 100, 155, 210, 255, 305};
  private final float[] radii = {.42f, .30f, .43f, .29f, .42f, .30f, .43f, .20f};
  private RadialGradient glow;
  private int centerX, centerY;

  public LearningOrbitView(Context context, AttributeSet attrs) {
    super(context, attrs);
    int[] resources = {
      R.drawable.ic_graduation,
      R.drawable.home_ic_quran,
      R.drawable.home_ic_ai,
      R.drawable.home_ic_study,
      R.drawable.home_ic_dua,
      R.drawable.home_ic_protector,
      R.drawable.ic_book,
      R.drawable.ic_sun
    };
    for (int i = 0; i < icons.length; i++) {
      icons[i] = context.getDrawable(resources[i]).mutate();
      icons[i].setTint(0xFF315849);
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    centerX = w / 2;
    centerY = h / 2;
    glow =
        new RadialGradient(
            centerX,
            centerY,
            Math.max(1, Math.min(w, h) * .47f),
            new int[] {0xFF315F51, 0xFF15332F},
            null,
            Shader.TileMode.CLAMP);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float size = Math.min(getWidth(), getHeight());
    float cx = centerX, cy = centerY;
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(glow);
    canvas.drawCircle(cx, cy, size * .48f, paint);
    paint.setShader(null);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(size * .0025f);
    float[] rings = {.20f, .30f, .43f};
    for (int i = 0; i < rings.length; i++) {
      paint.setColor(i == 1 ? 0xFF8C8259 : 0xFF426759);
      canvas.drawCircle(cx, cy, size * rings[i], paint);
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(0xFF284E42);
    canvas.drawCircle(cx, cy, size * .135f, paint);
    paint.setColor(0xFFE5B768);
    canvas.drawCircle(cx, cy, size * .080f, paint);
    paint.setColor(0xFF284E42);
    canvas.drawCircle(cx + size * .031f, cy - size * .022f, size * .073f, paint);
    sparkle(canvas, cx + size * .065f, cy - size * .056f, size * .014f, 0xFFF6DEA2);
    for (int i = 0; i < icons.length; i++) {
      double angle = Math.toRadians(angles[i]);
      float x = cx + (float) Math.cos(angle) * size * radii[i];
      float y = cy + (float) Math.sin(angle) * size * radii[i];
      float disc = size * (i % 2 == 0 ? .066f : .055f);
      paint.setColor(0xFF102D27);
      canvas.drawCircle(x, y + size * .008f, disc + size * .006f, paint);
      paint.setColor(colors[i]);
      canvas.drawCircle(x, y, disc, paint);
      int inset = Math.round(disc * .54f);
      icons[i].setBounds(
          Math.round(x) - inset,
          Math.round(y) - inset,
          Math.round(x) + inset,
          Math.round(y) + inset);
      icons[i].draw(canvas);
    }
    sparkle(canvas, cx - size * .13f, cy - size * .34f, size * .010f, 0xFFB2B68C);
    sparkle(canvas, cx + size * .31f, cy + size * .25f, size * .009f, 0xFFB2B68C);
    for (int i = 0; i < 13; i++) {
      double angle = Math.toRadians(i * 28 + 4);
      paint.setColor(i % 3 == 0 ? 0xFFC1A66D : 0xFF6F9A86);
      canvas.drawCircle(
          cx + (float) Math.cos(angle) * size * .47f,
          cy + (float) Math.sin(angle) * size * .47f,
          size * .003f,
          paint);
    }
  }

  private void sparkle(Canvas canvas, float x, float y, float radius, int color) {
    paint.setColor(color);
    canvas.drawOval(x - radius * .25f, y - radius, x + radius * .25f, y + radius, paint);
    canvas.drawOval(x - radius, y - radius * .25f, x + radius, y + radius * .25f, paint);
  }
}
