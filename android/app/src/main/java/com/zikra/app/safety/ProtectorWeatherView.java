package com.zikra.app.safety;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.zikra.app.R;
import com.zikra.app.ui.AmbientMotionPolicy;
import java.time.*;
import java.util.Locale;

/**
 * Layered approved artwork with bounded foreground-only motion. No network or per-frame allocation.
 */
public final class ProtectorWeatherView extends View {
  private final Paint paint = new Paint(3);
  private final RectF target = new RectF();
  private final Rect visible = new Rect();
  private final AmbientMotionPolicy motion;
  private Bitmap city, clouds, storm;
  private boolean active, windowVisible = true, night, available;
  private int cloudMode, rain;
  private ZoneId zone = ZoneId.of("Asia/Jakarta");
  private double phase;
  private long previous;
  private float elapsed;
  private LinearGradient sky;
  private LinearGradient sideScrim, bottomScrim;
  private ColorMatrixColorFilter cityFilter;
  private int skyColor;
  private final Runnable frame =
      () -> {
        invalidate();
      };

  public ProtectorWeatherView(Context c, AttributeSet a) {
    super(c, a);
    motion = new AmbientMotionPolicy(c, this::schedule);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    city = decode(R.drawable.protector_city);
    clouds = decode(R.drawable.protector_clouds);
    storm = decode(R.drawable.protector_storm);
    setWeather(null, zone);
  }

  private Bitmap decode(int resource) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 2;
    options.inScaled = false;
    return BitmapFactory.decodeResource(getResources(), resource, options);
  }

  public void setWeather(String description, ZoneId selectedZone) {
    zone = selectedZone;
    available = description != null;
    String d = description == null ? "" : description.toLowerCase(Locale.ROOT);
    cloudMode = d.equals("cerah") ? 0 : d.contains("cerah") || d.contains("sebagian") ? 1 : 2;
    rain =
        d.contains("petir")
            ? 80
            : d.contains("lebat") ? 64 : d.contains("hujan") ? 28 : d.contains("gerimis") ? 14 : 0;
    updateClock();
    invalidate();
  }

  public void setActive(boolean value) {
    active = value;
    if (value) {
      updateClock();
      motion.start();
    } else motion.stop();
    previous = 0;
    schedule();
  }

  private void updateClock() {
    Instant now = Instant.now();
    int hour = now.atZone(zone).getHour();
    night = hour < 6 || hour >= 18;
    phase = MoonPhase.cycle(now);
    updateSky();
  }

  private void updateSky() {
    int top =
        !available ? 0xff617981 : night ? 0xff0e273c : cloudMode == 2 ? 0xff37596b : 0xff2697d1;
    int bottom =
        !available ? 0xff9db2b0 : night ? 0xff2e5668 : cloudMode == 2 ? 0xff8dafb8 : 0xffcbdcbc;
    skyColor = top;
    sky = new LinearGradient(0, 0, 0, Math.max(1, getHeight()), top, bottom, Shader.TileMode.CLAMP);
    float width = Math.max(1, getWidth()), height = Math.max(1, getHeight());
    sideScrim =
        new LinearGradient(0, 0, width * .76f, 0, 0x470a3455, 0x000a3455, Shader.TileMode.CLAMP);
    bottomScrim =
        new LinearGradient(
            0, height * .65f, 0, height, 0x00123d3e, 0x88123d3e, Shader.TileMode.CLAMP);
    float brightness = night ? .35f : cloudMode == 2 ? .69f : 1f;
    ColorMatrix matrix = new ColorMatrix();
    matrix.setScale(brightness, brightness, brightness, 1);
    cityFilter = new ColorMatrixColorFilter(matrix);
  }

  public int getSkyColor() {
    return skyColor;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    updateSky();
  }

  private boolean running() {
    return active && windowVisible && isShown() && getLocalVisibleRect(visible) && motion.enabled();
  }

  public void updateVisibility() {
    schedule();
  }

  private void schedule() {
    removeCallbacks(frame);
    if (running()) postDelayed(frame, 50);
    else previous = 0;
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    windowVisible = visibility == VISIBLE;
    if (motion != null) schedule();
  }

  @Override
  protected void onVisibilityChanged(View changed, int visibility) {
    super.onVisibilityChanged(changed, visibility);
    if (motion != null) schedule();
  }

  @Override
  protected void onDetachedFromWindow() {
    setActive(false);
    removeCallbacks(frame);
    super.onDetachedFromWindow();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    long now = android.os.SystemClock.uptimeMillis();
    if (running() && previous != 0) elapsed += Math.min(.1f, (now - previous) / 1000f);
    previous = now;
    float w = getWidth(), h = getHeight();
    paint.setAlpha(255);
    paint.setShader(sky);
    canvas.drawRect(0, 0, w, h, paint);
    paint.setShader(null);
    if (available && cloudMode < 2) {
      float x = w * .78f, y = h * .35f, r = w * .058f;
      if (night) {
        // Orthographic sphere terminator: waxing lights the right limb, waning the left.
        double light = Math.cos(2 * Math.PI * phase);
        paint.setColor(0xff263e4b);
        canvas.drawCircle(x, y, r, paint);
        paint.setColor(0xffeeedc6);
        for (int i = 0; i < 80; i++) {
          float yy = -r + 2 * r * i / 80, half = (float) Math.sqrt(Math.max(0, r * r - yy * yy));
          float left = phase < .5 ? (float) (light * half) : -half;
          float right = phase < .5 ? half : (float) (-light * half);
          canvas.drawRect(x + left, y + yy, x + right, y + yy + 2 * r / 80 + .3f, paint);
        }
      } else {
        paint.setColor(0x22ffe69d);
        canvas.drawCircle(x, y, r * 1.8f, paint);
        paint.setColor(0xffffe69d);
        canvas.drawCircle(x, y, r, paint);
      }
    }
    if (available && cloudMode > 0) {
      Bitmap layer = cloudMode == 1 ? clouds : storm;
      float offset = (float) Math.sin(elapsed * .045) * w * .09f;
      paint.setAlpha(night ? 160 : cloudMode == 1 ? 240 : 255);
      float density = getResources().getDisplayMetrics().density;
      float left = cloudMode == 1 ? w * .20f : -w * .53f;
      float layerWidth = cloudMode == 1 ? w * 1.32f : w * 1.9f;
      float top = cloudMode == 1 ? -15 * density : -100 * density;
      float layerHeight = layerWidth * layer.getHeight() / layer.getWidth();
      target.set(left + offset, top, left + offset + layerWidth, top + layerHeight);
      canvas.drawBitmap(layer, null, target, paint);
      left = cloudMode == 1 ? w * .43f : -w * .12f;
      layerWidth = cloudMode == 1 ? w * 1.25f : w * 1.7f;
      top = cloudMode == 1 ? 30 * density : -54 * density;
      layerHeight = layerWidth * layer.getHeight() / layer.getWidth();
      target.set(left - offset, top, left - offset + layerWidth, top + layerHeight);
      canvas.drawBitmap(layer, null, target, paint);
      paint.setAlpha(255);
    }
    float cityH = w * 1.55f * city.getHeight() / city.getWidth();
    float bottom = h + 7 * getResources().getDisplayMetrics().density;
    target.set(-w * .275f, bottom - cityH, w * 1.275f, bottom);
    paint.setColorFilter(cityFilter);
    canvas.drawBitmap(city, null, target, paint);
    paint.setColorFilter(null);
    if (available && rain > 0) {
      paint.setColor(0x75d8e9ef);
      paint.setStrokeWidth(Math.max(1, w / 400));
      for (int i = 0; i < rain; i++) {
        float x = ((i * 73.37f + elapsed * (rain > 40 ? 65 : 30)) % (w + 40)) - 20;
        float y = (i * 47.19f + elapsed * (rain > 40 ? 510 : 250)) % (h + 40) - 20;
        canvas.drawLine(x, y, x - 4, y + 12 + (rain > 40 ? 8 : 0), paint);
      }
    }
    paint.setAlpha(255);
    paint.setShader(sideScrim);
    canvas.drawRect(0, 0, w, h, paint);
    paint.setShader(bottomScrim);
    canvas.drawRect(0, 0, w, h, paint);
    paint.setShader(null);
    schedule();
  }
}
