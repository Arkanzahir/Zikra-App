package com.zikra.app.sky;

import android.content.Context;
import android.graphics.*;
import android.hardware.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.*;
import com.zikra.app.R;
import com.zikra.app.ui.AmbientMotionPolicy;
import java.time.*;

/** Quiet layered sky; bounded 30fps, no allocations/network/Binder reads in each frame. */
public final class HomeSkyView extends View implements SensorEventListener {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path moon = new Path();
  private final Bitmap clouds;
  private final RectF cloudBounds = new RectF();
  private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Rect visible = new Rect();
  private int paletteMinute = -1, cloudColor;
  private float scrollOffset;
  private Shader textShade;
  private final Shader sunGlow = new RadialGradient(0, 0, 1,
      new int[] {0x60FFE4A5, 0x00FFE4A5}, null, Shader.TileMode.CLAMP);
  private final Bitmap landscape;
  private final RectF landscapeBounds = new RectF();
  private final Paint landscapePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private Shader backdrop;
  private int backdropWidth, backdropHeight;
  private final AmbientMotionPolicy policy;
  private final SensorManager sensors;
  private final Sensor gravity;
  private SkyDay day;
  private LocalTime time = LocalTime.NOON;
  private boolean running, registered;
  private float aimX, aimY, offsetX, offsetY;
  private final long origin = SystemClock.uptimeMillis();

  public HomeSkyView(Context c, AttributeSet a) {
    super(c, a);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
    bitmapOptions.inScaled = false;
    bitmapOptions.inSampleSize = 2;
    landscape =
        BitmapFactory.decodeResource(
            c.getResources(), R.drawable.home_al_qibli_foreground, bitmapOptions);
    clouds = BitmapFactory.decodeResource(c.getResources(), R.drawable.home_cloud_bank, bitmapOptions);
    sensors = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
    gravity = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);
    policy =
        new AmbientMotionPolicy(
            c,
            () -> {
              syncSensor();
              invalidate();
            });
    com.zikra.app.location.LocationPoint point = com.zikra.app.location.LocationStore.current(c);
    setSky(SkyDay.estimate(LocalDate.now(point.zone), point), LocalTime.now(point.zone));
  }

  public void setSky(SkyDay value, LocalTime localTime) {
    if (day == null || !day.date.equals(value.date) || !day.rise.equals(value.rise)
        || !day.set.equals(value.set)) paletteMinute = -1;
    day = value;
    time = localTime;
    // Normalized lunar terminator. Orientation is illustrative; the lit fraction follows the
    // source.
    moon.reset();
    for (int i = 0; i <= 64; i++) {
      float y = -1 + i / 32f, x = (float) Math.sqrt(Math.max(0, 1 - y * y));
      if (i == 0) moon.moveTo(x, y);
      else moon.lineTo(x, y);
    }
    for (int i = 64; i >= 0; i--) {
      float y = -1 + i / 32f,
          x = (float) ((1 - 2 * day.illumination) * Math.sqrt(Math.max(0, 1 - y * y)));
      moon.lineTo(x, y);
    }
    moon.close();
    invalidate();
  }

  public boolean isMotionRunning() {
    return running && isShown() && hasWindowFocus() && getLocalVisibleRect(visible) && policy.enabled();
  }

  public boolean isDaytime() {
    return day.daytime(time);
  }

  public void setRunning(boolean value) {
    running = value;
    if (value) policy.start();
    else policy.stop();
    syncSensor();
    invalidate();
  }

  public void observeTouch(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_UP
        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) aimX = aimY = 0;
    else {
      aimX = (e.getX() / Math.max(1, getWidth()) - .5f) * 2;
      aimY = (e.getY() / Math.max(1, getHeight()) - .5f) * 2;
    }
  }

  private void syncSensor() {
    boolean active = isMotionRunning() && gravity != null;
    if (active && !registered)
      registered = sensors.registerListener(this, gravity, SensorManager.SENSOR_DELAY_UI);
    if (!active && registered) {
      sensors.unregisterListener(this);
      registered = false;
    }
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    syncSensor();
    invalidate();
  }

  @Override
  protected void onDetachedFromWindow() {
    setRunning(false);
    super.onDetachedFromWindow();
  }

  @Override
  public void onSensorChanged(SensorEvent e) {
    aimX = Math.max(-1, Math.min(1, -e.values[0] / 4));
    aimY = Math.max(-1, Math.min(1, e.values[1] / 5));
  }

  @Override
  public void onAccuracyChanged(Sensor s, int accuracy) {}

  public void setScrollOffset(float pixels) {
    scrollOffset = pixels;
    invalidate();
  }

  private void cloud(Canvas c, float x, float y, float scale, int alpha) {
    float width = getWidth() * scale, height = width * clouds.getHeight() / clouds.getWidth();
    cloudBounds.set(x, y, x + width, y + height);
    cloudPaint.setAlpha(alpha);
    c.drawBitmap(clouds, null, cloudBounds, cloudPaint);
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    // This background fills the hero's second layout pass; it never determines its height.
    int height =
        MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY
            ? MeasureSpec.getSize(heightSpec)
            : 0;
    setMeasuredDimension(MeasureSpec.getSize(widthSpec), height);
  }

  @Override
  protected void onDraw(Canvas c) {
    boolean animate = isMotionRunning(), light = isDaytime();
    float t = animate ? (SystemClock.uptimeMillis() - origin) / 1000f : 0;
    if (animate) {
      offsetX += (aimX - offsetX) * .10f;
      offsetY += (aimY - offsetY) * .10f;
    } else offsetX = offsetY = 0;
    float w = getWidth(), h = getHeight(), u = w / 400f;
    int minute = time.toSecondOfDay() / 60;
    if (backdrop == null || paletteMinute != minute
        || backdropWidth != getWidth() || backdropHeight != getHeight()) {
      paletteMinute = minute;
      backdropWidth = getWidth(); backdropHeight = getHeight();
      backdrop = new LinearGradient(0, 0, w, h, SkyPalette.top(day, time),
          SkyPalette.horizon(day, time), Shader.TileMode.CLAMP);
      textShade = new LinearGradient(0, 0, w, 0,
          new int[] {0xAA082F32, 0x70082F32, 0x00082F32}, new float[] {0, .48f, 1},
          Shader.TileMode.CLAMP);
      int color = SkyPalette.cloud(day, time);
      if (color != cloudColor) {
        cloudColor = color;
        cloudPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
      }
      float brightness = .38f + .62f * SkyPalette.daylight(day, time);
      float warm = .65f * (1 - SkyPalette.daylight(day, time));
      landscapePaint.setColorFilter(new ColorMatrixColorFilter(new float[] {
          brightness + warm,0,-warm,0,0, warm * .6f,brightness,-warm * .6f,0,0,
          0,0,brightness + .03f * (1-brightness),0,0, 0,0,0,1,0}));
    }
    paint.setShader(backdrop);
    paint.setAlpha(255);
    c.drawRect(0, 0, w, h, paint);
    paint.setShader(null);
    if (light) {
      float progress = day.sunProgress(time),
          x = w * (.84f + .10f * progress),
          y = h * .34f - h * .08f * (float) Math.sin(Math.PI * progress);
      c.save();
      c.translate(x + offsetX * 3 * u, y + offsetY * 2 * u);
      c.save(); c.scale(38 * u, 38 * u);
      paint.setShader(sunGlow); paint.setAlpha(255);
      c.drawCircle(0, 0, 1, paint);
      paint.setShader(null); c.restore();
      paint.setColor(0xFFFFE9AE);
      c.drawCircle(0, 0, 15 * u, paint);
      c.restore();
    } else {
      for (int i = 0; i < 19; i++) {
        float x = w * ((i * 43 + 23) % 367) / 400f, y = h * (.25f + ((i * 31) % 47) / 170f);
        // Keep the centre clock area quiet.
        if (x > w * .25f && x < w * .75f && y > h * .38f) continue;
        paint.setColor(0xFFF3DEB0);
        paint.setAlpha((int) (34 + 13 * Math.sin(t * .6 + i)));
        c.drawCircle(
            x + offsetX * 3 * u, y + offsetY * 2 * u, (i % 3 == 0 ? 1.2f : .7f) * u, paint);
      }
      c.save();
      c.translate(
          w * .87f + offsetX * 5 * u,
          h * .26f + offsetY * 3 * u + (float) Math.sin(t * .12f) * 2 * u);
      c.rotate(-18 + (float) Math.sin(t * .045f) * 6);
      c.scale(17 * u * (day.waxing ? 1 : -1), 17 * u);
      paint.setColor(0xFF63756A);
      paint.setAlpha(80);
      c.drawCircle(0, 0, 1, paint);
      paint.setColor(0xFFE7D9AF);
      paint.setAlpha(230);
      c.drawPath(moon, paint);
      c.restore();
    }
    float drift = (float) Math.sin(t * .025f);
    cloud(c, w * .33f + drift * 12 * u + offsetX * 4 * u,
        h * .23f + offsetY * 2 * u - Math.min(scrollOffset, h) * .025f, .60f, light ? 190 : 95);
    cloud(c, w * .18f + (float) Math.sin(t * .018f + 2) * 16 * u + offsetX * 8 * u,
        h * .39f - Math.min(scrollOffset, h) * .045f, .72f, light ? 120 : 58);
    c.save();
    c.translate(offsetX * 3 * u, offsetY * 1.5f * u - Math.min(scrollOffset, h) * .015f);
    float imageHeight = w * landscape.getHeight() / landscape.getWidth();
    landscapeBounds.set(-2 * u, h - imageHeight - 52 * u, w + 42 * u, h - 48 * u);
    paint.setColor(0xFF123C39); paint.setAlpha(255);
    c.drawRect(0, h - 60 * u, w, h + 4 * u, paint);
    c.drawBitmap(landscape, null, landscapeBounds, landscapePaint);
    c.restore();
    paint.setShader(textShade); paint.setAlpha(255);
    c.drawRect(0, 0, w, h, paint); paint.setShader(null);
    if (animate) postInvalidateDelayed(33);
  }
}
