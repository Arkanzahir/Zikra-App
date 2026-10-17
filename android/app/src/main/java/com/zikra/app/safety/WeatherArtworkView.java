package com.zikra.app.safety;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Decorative weather illustration; never encodes an observed weather condition. */
public final class WeatherArtworkView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public WeatherArtworkView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float w = getWidth(), h = getHeight();
    paint.setColor(Color.argb(25, 255, 255, 255));
    canvas.drawCircle(w * .85f, h * .0f, w * .46f, paint);
    canvas.drawCircle(w * 1.01f, h * .22f, w * .32f, paint);
    Path wave = new Path();
    wave.moveTo(0, h * .86f);
    wave.cubicTo(w * .25f, h * .62f, w * .45f, h * 1.2f, w * .75f, h * .85f);
    wave.quadTo(w * .9f, h * .7f, w, h * .8f);
    wave.lineTo(w, h);
    wave.lineTo(0, h);
    wave.close();
    paint.setColor(Color.argb(25, 255, 255, 255));
    canvas.drawPath(wave, paint);
    cloud(canvas, w * .94f, h * .61f, w * .17f, Color.argb(35, 255, 255, 255));
    cloud(canvas, w * .04f, h * .48f, w * .10f, Color.argb(28, 255, 255, 255));
  }

  private void cloud(Canvas c, float x, float y, float width, int color) {
    paint.setColor(color);
    c.drawRoundRect(
        x - width * .5f,
        y - width * .04f,
        x + width * .52f,
        y + width * .26f,
        width * .17f,
        width * .17f,
        paint);
    c.drawCircle(x - width * .17f, y, width * .23f, paint);
    c.drawCircle(x + width * .1f, y - width * .12f, width * .29f, paint);
  }
}
