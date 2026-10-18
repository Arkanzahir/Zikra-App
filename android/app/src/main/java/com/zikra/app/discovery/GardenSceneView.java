package com.zikra.app.discovery;

import android.content.Context;
import android.graphics.*;
import android.hardware.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.zikra.app.R;

/** One decoded artwork, layered native clouds and bounded 30fps parallax; no video/WebView. */
public final class GardenSceneView extends View implements SensorEventListener {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final RectF target = new RectF();
  private final Path cloud = new Path();
  private final Bitmap garden;
  private final SensorManager sensors;
  private final Sensor gravity;
  private final com.zikra.app.ui.AmbientMotionPolicy policy;
  private boolean running, registered;
  private float aimX, aimY, offsetX, offsetY;
  private int story;
  private final long origin = SystemClock.uptimeMillis();

  public GardenSceneView(Context context, AttributeSet attrs) {
    super(context, attrs);
    garden = BitmapFactory.decodeResource(getResources(), R.drawable.onboarding_garden);
    sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    gravity = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);
    policy =
        new com.zikra.app.ui.AmbientMotionPolicy(
            context,
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
  }

  public void setStory(int value) {
    story = value;
    invalidate();
  }

  public void setRunning(boolean value) {
    running = value;
    if (value) policy.start();
    else policy.stop();
    syncSensor();
    if (value) invalidate();
  }

  public boolean isMotionRunning() {
    return canAnimate();
  }

  private boolean canAnimate() {
    return running && isShown() && hasWindowFocus() && policy.enabled();
  }

  private void syncSensor() {
    boolean active = canAnimate() && gravity != null;
    if (active && !registered)
      registered = sensors.registerListener(this, gravity, SensorManager.SENSOR_DELAY_UI);
    if (!active && registered) {
      sensors.unregisterListener(this);
      registered = false;
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    policy.stop();
    if (registered) sensors.unregisterListener(this);
    registered = false;
    super.onDetachedFromWindow();
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    syncSensor();
    if (focus) invalidate();
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (sensors != null) syncSensor();
    if (visibility == VISIBLE) invalidate();
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    aimX = Math.max(-1, Math.min(1, -event.values[0] / 4));
    aimY = Math.max(-1, Math.min(1, event.values[1] / 5));
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_UP
        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      aimX = aimY = 0;
      if (e.getActionMasked() == MotionEvent.ACTION_UP) performClick();
      return true;
    }
    aimX = (e.getX() / Math.max(1, getWidth()) - .5f) * 2;
    aimY = (e.getY() / Math.max(1, getHeight()) - .5f) * 2;
    invalidate();
    return true;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }

  private void drawCloud(Canvas c, float x, float y, float scale, int alpha) {
    c.save();
    c.translate(x, y);
    c.scale(scale, scale);
    paint.setColor(0xFF729B88);
    paint.setAlpha(alpha);
    c.drawPath(cloud, paint);
    paint.setAlpha(255);
    c.restore();
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    boolean animate = canAnimate();
    float seconds = animate ? (SystemClock.uptimeMillis() - origin) / 1000f : 0;
    if (animate) {
      offsetX += (aimX - offsetX) * .12f;
      offsetY += (aimY - offsetY) * .12f;
    } else {
      offsetX = offsetY = 0;
    }
    float unit = getWidth() / 360f;
    drawCloud(
        c,
        unit * (-12 + offsetX * 4 + (float) Math.sin(seconds * .16) * 9),
        getHeight() * .16f,
        unit * .9f,
        42);
    drawCloud(c, getWidth() * .77f + offsetX * unit * 3, getHeight() * .29f, unit * .65f, 32);
    float width = getWidth() * .99f, height = width * garden.getHeight() / garden.getWidth();
    float x = (getWidth() - width) / 2 + offsetX * unit * 8,
        y =
            (getHeight() - height) / 2
                + unit * (10 + (float) Math.sin(seconds * .6 + story * .6) * 3)
                + offsetY * unit * 4;
    target.set(x, y, x + width, y + height);
    paint.setAlpha(255);
    c.drawBitmap(garden, null, target, paint);
    drawCloud(
        c,
        getWidth() * .73f + offsetX * unit * 12,
        getHeight() * .80f + unit * (float) Math.sin(seconds * .22) * 3,
        unit * .8f,
        58);
    drawCloud(c, -unit * 20 + offsetX * unit * 10, getHeight() * .85f, unit, 45);
    if (animate) postInvalidateDelayed(33);
  }
}
