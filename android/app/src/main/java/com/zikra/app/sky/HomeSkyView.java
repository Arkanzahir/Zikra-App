package com.zikra.app.sky;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
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
  private final Path cloud = new Path(), moon = new Path();
  private final Drawable skyline;
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
    skyline = c.getDrawable(R.drawable.home_mosque_silhouette);
    skyline.setTint(0xFF9ABAA9);
    skyline.setAlpha(28);
    sensors = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
    gravity = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);
    policy =
        new AmbientMotionPolicy(
            c,
            () -> {
              syncSensor();
              invalidate();
            });
    cloud.moveTo(0, 18);
    cloud.cubicTo(5, 7, 20, 8, 25, 12);
    cloud.cubicTo(28, -5, 50, -4, 56, 9);
    cloud.cubicTo(70, 0, 85, 8, 87, 17);
    cloud.cubicTo(106, 12, 112, 23, 110, 28);
    cloud.cubicTo(74, 32, 24, 32, 0, 28);
    cloud.close();
    setSky(SkyDay.estimate(LocalDate.now(com.zikra.app.PrayerSchedule.ZONE)), LocalTime.NOON);
  }

  public void setSky(SkyDay value, LocalTime localTime) {
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
    return running && isShown() && hasWindowFocus() && policy.enabled();
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

  private void cloud(Canvas c, float x, float y, float scale, int alpha) {
    c.save();
    c.translate(x, y);
    c.scale(scale, scale);
    paint.setColor(0xFF9ABFB0);
    paint.setAlpha(alpha);
    c.drawPath(cloud, paint);
    c.restore();
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
    c.drawColor(light ? 0xFF1A4039 : 0xFF102923);
    if (light) {
      float progress = day.sunProgress(time),
          x = w * (.09f + .82f * progress),
          y = h * .43f - h * .30f * (float) Math.sin(Math.PI * progress);
      paint.setColor(0xFFEDC581);
      paint.setAlpha(20);
      c.drawCircle(x, y, 22 * u, paint);
      paint.setAlpha(230);
      c.drawCircle(x, y, 8 * u, paint);
      cloud(
          c,
          w * .72f + offsetX * 5 * u + (float) Math.sin(t * .15) * 8 * u,
          h * .23f + offsetY * 2 * u,
          .62f * u,
          28);
      cloud(
          c,
          w * .01f + offsetX * 9 * u + (float) Math.sin(t * .11) * 10 * u,
          h * .40f,
          .8f * u,
          22);
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
      c.translate(w * .78f + offsetX * 5 * u, h * .27f + offsetY * 3 * u);
      c.scale(11 * u * (day.waxing ? 1 : -1), 11 * u);
      paint.setColor(0xFF63756A);
      paint.setAlpha(80);
      c.drawCircle(0, 0, 1, paint);
      paint.setColor(0xFFE7D9AF);
      paint.setAlpha(230);
      c.drawPath(moon, paint);
      c.restore();
      cloud(
          c, -12 * u + offsetX * 7 * u + (float) Math.sin(t * .10) * 8 * u, h * .37f, .8f * u, 13);
    }
    c.save();
    c.translate(offsetX * 3 * u, offsetY * 1.5f * u);
    skyline.setBounds(-5, (int) (h - 142 * u), (int) w + 5, (int) (h + 12 * u));
    skyline.draw(c);
    c.restore();
    if (animate) postInvalidateDelayed(33);
  }
}
