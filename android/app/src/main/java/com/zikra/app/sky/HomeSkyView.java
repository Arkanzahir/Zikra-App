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

/** Location-clock illustration. One building silhouette; bounded 30fps, cached paints. */
public final class HomeSkyView extends View implements SensorEventListener {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Paint landscapePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final MosqueInteriorLights interiorLights = new MosqueInteriorLights();
  private final RectF cloudBounds = new RectF(), landscapeBounds = new RectF();
  private final Rect visible = new Rect();
  private final Path moon = new Path();
  private final Bitmap clouds, landscape;
  private final Shader sunGlow = new RadialGradient(0, 0, 1,
      new int[] {0x35FFF2C1, 0x0CFFF2C1, 0x00FFF2C1},
      new float[] {0, .35f, 1}, Shader.TileMode.CLAMP);
  private final Shader moonGlow = new RadialGradient(0, 0, 1.8f,
      new int[] {0x16DCE8FF, 0x00DCE8FF}, null, Shader.TileMode.CLAMP);
  private final Shader lunarDetail = new RadialGradient(-.24f, -.2f, .7f,
      new int[] {0x19293C47, 0x00293C47}, null, Shader.TileMode.CLAMP);
  private final Shader lampGlow = new RadialGradient(0, 0, 1,
      new int[] {0xD9FFC771, 0x85FFB354, 0x00FFAE4D},
      new float[] {0, .35f, 1}, Shader.TileMode.CLAMP);
  private static final float[] LAMP_X = {430, 486, 548, 618, 706, 730, 792, 850, 911};
  private final Shader reflectedLight = new RadialGradient(0,0,1,
      new int[]{0x76FFC579,0x00FFB354},null,Shader.TileMode.CLAMP);
  private final AmbientMotionPolicy policy;
  private final SensorManager sensors;
  private final Sensor gravity;
  private SkyDay day;
  private LocalTime time = LocalTime.NOON;
  private boolean running, registered, touching;
  private float aimX, aimY, offsetX, offsetY, scrollOffset, nightBlend, lampStrength;
  private int paletteSecond = -1, backdropWidth, backdropHeight;
  private Shader backdrop, textShade;
  private long lastFrame, motionMillis;

  public HomeSkyView(Context context, AttributeSet attributes) {
    super(context, attributes);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    options.inSampleSize = 2;
    landscape = BitmapFactory.decodeResource(
        context.getResources(), R.drawable.home_al_qibli_day_unlit, options);
    clouds = BitmapFactory.decodeResource(
        context.getResources(), R.drawable.home_cloud_realistic, options);
    sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    gravity = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);
    policy = new AmbientMotionPolicy(context, () -> { syncSensor(); invalidate(); });
    com.zikra.app.location.LocationPoint point =
        com.zikra.app.location.LocationStore.current(context);
    setSky(SkyDay.estimate(LocalDate.now(point.zone), point), LocalTime.now(point.zone));
  }

  public void setSky(SkyDay value, LocalTime localTime) {
    boolean phaseChanged = day == null || day.illumination != value.illumination;
    if (day != value) paletteSecond = -1;
    day = value;
    time = localTime;
    if (phaseChanged) {
      moon.reset();
      for (int i = 0; i <= 64; i++) {
        float y = -1 + i / 32f, x = (float) Math.sqrt(Math.max(0, 1 - y * y));
        if (i == 0) moon.moveTo(x, y);
        else moon.lineTo(x, y);
      }
      for (int i = 64; i >= 0; i--) {
        float y = -1 + i / 32f;
        moon.lineTo((float) ((1 - 2 * day.illumination)
            * Math.sqrt(Math.max(0, 1 - y * y))), y);
      }
      moon.close();
    }
    invalidate();
  }

  public boolean isMotionRunning() {
    return running && isShown() && hasWindowFocus() && getLocalVisibleRect(visible)
        && policy.enabled();
  }

  public boolean isDaytime() { return day.daytime(time); }

  public void setRunning(boolean value) {
    if (running == value) return;
    running = value;
    if (value) policy.start();
    else policy.stop();
    syncSensor();
    invalidate();
  }

  public void observeTouch(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_UP
        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      touching = false;
      aimX = aimY = 0;
    }
    else {
      touching = true;
      aimX = Math.max(-1, Math.min(1, (event.getX() / Math.max(1, getWidth()) - .5f) * 2));
      aimY = Math.max(-1, Math.min(1, (event.getY() / Math.max(1, getHeight()) - .5f) * 2));
    }
  }

  private void syncSensor() {
    boolean active = isMotionRunning() && gravity != null;
    if (active && !registered)
      registered = sensors.registerListener(this, gravity, SensorManager.SENSOR_DELAY_UI);
    if (!active && registered) { sensors.unregisterListener(this); registered = false; }
  }

  @Override public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    syncSensor();
    invalidate();
  }

  @Override protected void onDetachedFromWindow() {
    setRunning(false);
    super.onDetachedFromWindow();
  }

  @Override public void onSensorChanged(SensorEvent event) {
    if (touching) return;
    aimX = Math.max(-1, Math.min(1, -event.values[0] / 4));
    aimY = Math.max(-1, Math.min(1, event.values[1] / 5));
  }

  @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  public void setScrollOffset(float pixels) { scrollOffset = pixels; invalidate(); }

  @Override protected void onMeasure(int widthSpec, int heightSpec) {
    setMeasuredDimension(MeasureSpec.getSize(widthSpec),
        MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY
            ? MeasureSpec.getSize(heightSpec) : 0);
  }

  private void cloud(Canvas canvas, float x, float y, float scale, int alpha) {
    float width = getWidth() * scale;
    cloudBounds.set(x, y, x + width, y + width * clouds.getHeight() / clouds.getWidth());
    cloudPaint.setAlpha(alpha);
    canvas.drawBitmap(clouds, null, cloudBounds, cloudPaint);
  }

  @Override protected void onDraw(Canvas canvas) {
    boolean animate = isMotionRunning();
    long now = SystemClock.uptimeMillis();
    if (animate) {
      if (lastFrame != 0) motionMillis += Math.min(100, now - lastFrame);
      lastFrame = now;
      offsetX += (aimX - offsetX) * .10f;
      offsetY += (aimY - offsetY) * .10f;
    } else { lastFrame = 0; offsetX = offsetY = 0; }
    float t = motionMillis / 1000f, w = getWidth(), h = getHeight(), u = w / 400f;
    int second = time.toSecondOfDay();
    if (backdrop == null || paletteSecond != second
        || backdropWidth != getWidth() || backdropHeight != getHeight()) {
      paletteSecond = second;
      backdropWidth = getWidth(); backdropHeight = getHeight();
      backdrop = new LinearGradient(0, 0, 0, h, SkyPalette.top(day, time),
          SkyPalette.horizon(day, time), Shader.TileMode.CLAMP);
      textShade = new LinearGradient(0, 0, w, 0,
          new int[] {0x98082F32, 0x48082F32, 0x00082F32},
          new float[] {0, .5f, 1}, Shader.TileMode.CLAMP);
      cloudPaint.setColorFilter(new PorterDuffColorFilter(
          SkyPalette.cloud(day, time), PorterDuff.Mode.MULTIPLY));
      float daylight = SkyPalette.daylight(day, time);
      nightBlend = 1 - daylight;
      lampStrength = SkyPalette.lampVisibility(day,time);
      float brightness = .43f + .57f * daylight;
      landscapePaint.setColorFilter(new ColorMatrixColorFilter(new float[] {
          brightness,0,0,0,0, 0,brightness,0,0,0, 0,0,brightness,0,0, 0,0,0,1,0}));
    }
    paint.setShader(backdrop); paint.setAlpha(255);
    canvas.drawRect(0, 0, w, h, paint); paint.setShader(null);
    float progress = day.sunProgress(time), elevation = (float) Math.sin(Math.PI * progress);
    float sunX = w * (.17f + .71f * progress), sunY = h * (.76f - .47f * elevation);
    float sunAlpha = SkyPalette.sunVisibility(day, time);
    if (sunAlpha > 0) {
      canvas.save(); canvas.translate(sunX + offsetX * 2 * u, sunY + offsetY * u);
      canvas.save(); canvas.scale(47 * u, 47 * u);
      paint.setShader(sunGlow); paint.setAlpha(Math.round(255 * sunAlpha));
      canvas.drawCircle(0, 0, 1, paint); canvas.restore(); paint.setShader(null);
      paint.setColor(elevation < .3f ? 0xFFFFD399 : 0xFFFFFBE3);
      paint.setAlpha(Math.round(255 * sunAlpha));
      canvas.drawCircle(0, 0, (7.5f + (1 - elevation)) * u, paint); canvas.restore();
    }
    float night = SkyPalette.nightVisibility(day, time);
    for (int i = 0; night > 0 && i < 24; i++) {
      float x = w * (.51f + ((i * 43 + 23) % 46) / 100f);
      float y = h * (.17f + ((i * 31) % 44) / 100f);
      paint.setColor(0xFFEDF4FF); paint.setAlpha(Math.round(night * (85 + i % 5 * 22)));
      canvas.drawCircle(x + offsetX * 2 * u, y + offsetY * u,
          (i % 5 == 0 ? .8f : .5f) * u, paint);
    }
    if (night > 0) {
      canvas.save();
      canvas.translate(w * .81f + offsetX * 2 * u, h * .26f + offsetY * 1.5f * u);
      canvas.scale(9.5f * u * (day.waxing ? 1 : -1), 9.5f * u);
      paint.setShader(moonGlow); paint.setAlpha(Math.round(255 * night));
      canvas.drawCircle(0, 0, 1.8f, paint); paint.setShader(null);
      paint.setColor(0xFFEBEDE8); paint.setAlpha(Math.round(242 * night));
      canvas.drawPath(moon, paint); canvas.clipPath(moon);
      paint.setShader(lunarDetail); paint.setAlpha(Math.round(255 * night));
      canvas.drawCircle(0, 0, 1, paint); paint.setShader(null); canvas.restore();
    }
    float daylight = 1 - nightBlend;
    cloud(canvas, w * .19f + (float) Math.sin(t * .055f) * 20 * u + offsetX * 4 * u,
        h * .17f + offsetY * 2 * u - Math.min(scrollOffset, h) * .025f,
        .91f, Math.round(255 * (.065f + .12f * daylight)));
    cloud(canvas, w * .43f + (float) Math.sin(t * .08f) * 32 * u + offsetX * 9 * u,
        h * .32f + offsetY * 4 * u - Math.min(scrollOffset, h) * .045f,
        .83f, Math.round(255 * (.10f + .20f * daylight)));
    canvas.save();
    canvas.translate(-offsetX * 2 * u,
        -offsetY * 1.4f * u - Math.min(scrollOffset, h) * .015f);
    float sceneWidth = w * 1.025f;
    float sceneHeight = sceneWidth * landscape.getHeight() / landscape.getWidth();
    float left = (w - sceneWidth) / 2, top = h - sceneHeight + 9 * u;
    landscapeBounds.set(left, top, left + sceneWidth, top + sceneHeight);
    canvas.drawBitmap(landscape, null, landscapeBounds, landscapePaint);
    if (lampStrength > 0) {
      canvas.translate(left, top); canvas.scale(sceneWidth / 960, sceneHeight / 640);
      interiorLights.draw(canvas,lampStrength);
      for (float x : LAMP_X) {
        canvas.save();canvas.translate(x,475);
        canvas.save();canvas.scale(22,43);
        paint.setShader(lampGlow);paint.setAlpha(Math.round(255*lampStrength));
        canvas.drawCircle(0,0,1,paint);canvas.restore();paint.setShader(null);
        paint.setColor(0xFFFFE4A8);paint.setAlpha(Math.round(245*lampStrength));
        canvas.drawRoundRect(-1.4f,-3,1.4f,3,1,1,paint);
        canvas.translate(0,34);canvas.scale(13,32);
        paint.setShader(reflectedLight);paint.setAlpha(Math.round(180*lampStrength));
        canvas.drawCircle(0,0,1,paint);paint.setShader(null);canvas.restore();
      }
    }
    canvas.restore();
    paint.setShader(textShade); paint.setAlpha(255);
    canvas.drawRect(0, 0, w, h, paint); paint.setShader(null);
    if (animate) postInvalidateDelayed(33);
  }
}
