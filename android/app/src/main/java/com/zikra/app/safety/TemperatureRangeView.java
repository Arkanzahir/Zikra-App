package com.zikra.app.safety;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import com.zikra.app.R;

/** A shared Celsius scale across the available forecast days. */
public final class TemperatureRangeView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private float start, end;
  private LinearGradient gradient;

  public TemperatureRangeView(Context context) {
    this(context, 0, 1, 0, 1);
  }

  public TemperatureRangeView(
      Context context, double low, double high, double minimum, double maximum) {
    super(context);
    float span = (float) Math.max(1, maximum - minimum);
    start = (float) (low - minimum) / span;
    end = maximum == minimum ? 1 : (float) (high - minimum) / span;
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    gradient =
        new LinearGradient(
            0,
            0,
            Math.max(1, w),
            0,
            getContext().getColor(R.color.weather_range_cool),
            getContext().getColor(R.color.weather_range_warm),
            Shader.TileMode.CLAMP);
  }

  @Override
  protected void onDraw(Canvas c) {
    float height = 5 * getResources().getDisplayMetrics().density;
    float y = (getHeight() - height) / 2;
    paint.setColor(getContext().getColor(R.color.weather_range_track));
    c.drawRoundRect(0, y, getWidth(), y + height, height, height, paint);
    float left = start * getWidth(), right = Math.max(left + height, end * getWidth());
    right = Math.min(getWidth(), right);
    left = Math.min(left, right - height);
    paint.setColor(Color.WHITE);
    paint.setShader(gradient);
    c.drawRoundRect(left, y, right, y + height, height, height, paint);
    paint.setShader(null);
  }
}
