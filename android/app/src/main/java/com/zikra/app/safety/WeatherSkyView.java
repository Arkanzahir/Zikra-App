package com.zikra.app.safety;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.zikra.app.R;

/** Static, sampled original cloud artwork; sky time is independent of the app theme. */
public final class WeatherSkyView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Bitmap clouds, storm;
  private final RectF destination = new RectF();
  private boolean night, rain;
  private LinearGradient gradient;

  public WeatherSkyView(Context context, AttributeSet attrs) {
    super(context, attrs);
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 2;
    clouds = BitmapFactory.decodeResource(getResources(), R.drawable.protector_clouds, options);
    storm = BitmapFactory.decodeResource(getResources(), R.drawable.protector_storm, options);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  public void setWeather(String description, boolean night) {
    this.night = night;
    rain = description != null && (description.contains("Hujan") || description.contains("Petir"));
    rebuild();
    invalidate();
  }

  public int topColor() {
    return night ? 0xff14293f : rain ? 0xff4b677d : 0xff347ca9;
  }

  private void rebuild() {
    gradient =
        new LinearGradient(
            0,
            0,
            0,
            Math.max(1, getHeight()),
            topColor(),
            night ? 0xff314c69 : rain ? 0xff8daabd : 0xff79b9d9,
            Shader.TileMode.CLAMP);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    rebuild();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (gradient == null) rebuild();
    paint.setShader(gradient);
    paint.setAlpha(255);
    canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    paint.setShader(null);
    Bitmap art = rain ? storm : clouds;
    if (art == null) return;
    float width = getWidth() * 1.8f, height = width * art.getHeight() / art.getWidth();
    paint.setAlpha(night ? 55 : rain ? 120 : 180);
    for (float y = getHeight() * .12f; y < getHeight(); y += Math.max(1, height * .9f)) {
      destination.set(-getWidth() * .5f, y, width - getWidth() * .5f, y + height);
      canvas.drawBitmap(art, null, destination, paint);
    }
    paint.setAlpha(255);
  }
}
