package com.zikra.app.discovery;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import com.zikra.app.R;

/** Layered native artwork; motion only runs while the welcome screen is visible. */
public final class LearningOrbitView extends View implements SensorEventListener {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Drawable[] icons = new Drawable[6];
  private final int[] colors = {
    0xFFF4D697, 0xFFD6EEE4, 0xFFD9C9EF, 0xFFCCE3EE, 0xFFF4D7CA, 0xFFE0EBC6
  };
  private final float[] positions = {
    -.31f, -.29f, .30f, -.17f, .31f, .28f, -.33f, .22f, -.04f, .38f, .12f, -.40f
  };
  private final float[] depths = {1f, .65f, 1.15f, .8f, .5f, .4f};
  private final Shader[] tileShaders = new Shader[6];
  private final RectF ring = new RectF();
  private final RectF tile = new RectF();
  private final Path moon = new Path();
  private SensorManager sensorManager;
  private Sensor tiltSensor;
  private Shader glow, medallion;
  private boolean foreground, running, touching, baselineReady;
  private float targetX, targetY, offsetX, offsetY, baselineX, baselineY;
  private float phase;
  private long lastFrame;
  private final Runnable frame =
      new Runnable() {
        @Override
        public void run() {
          if (!running) return;
          if (!ValueAnimator.areAnimatorsEnabled()) {
            updateMotion();
            return;
          }
          long now = SystemClock.uptimeMillis();
          float seconds = Math.min(.1f, (now - lastFrame) / 1000f);
          lastFrame = now;
          phase = (phase + seconds * .65f) % ((float) Math.PI * 2);
          float smoothing = 1f - (float) Math.exp(-seconds * 7f);
          offsetX += (targetX - offsetX) * smoothing;
          offsetY += (targetY - offsetY) * smoothing;
          invalidate();
          postOnAnimationDelayed(this, 32);
        }
      };

  public LearningOrbitView(Context context, AttributeSet attrs) {
    super(context, attrs);
    int[] resources = {
      R.drawable.feature_icon_study,
      R.drawable.feature_icon_quran,
      R.drawable.feature_icon_ai,
      R.drawable.feature_icon_dua,
      R.drawable.feature_icon_study,
      R.drawable.feature_icon_protector
    };
    for (int i = 0; i < icons.length; i++) {
      icons[i] = context.getDrawable(resources[i]).mutate();
      icons[i].setTint(0xFF244C42);
      tileShaders[i] =
          new LinearGradient(
              -30, -30, 32, 38, new int[] {0xFFFFFEF7, colors[i]}, null, Shader.TileMode.CLAMP);
    }
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    if (sensorManager != null) {
      tiltSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
      if (tiltSensor == null)
        tiltSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
    setClickable(true);
    setContentDescription("Orbit interaktif ilmu dan ibadah. Geser perlahan untuk efek kedalaman.");
  }

  public void setForeground(boolean foreground) {
    this.foreground = foreground;
    updateMotion();
  }

  public boolean isMotionRunning() {
    return running;
  }

  private void updateMotion() {
    boolean shouldRun =
        foreground
            && isAttachedToWindow()
            && isShown()
            && hasWindowFocus()
            && ValueAnimator.areAnimatorsEnabled();
    if (shouldRun == running) return;
    running = shouldRun;
    removeCallbacks(frame);
    if (sensorManager != null) sensorManager.unregisterListener(this);
    baselineReady = false;
    touching = false;
    targetX = targetY = 0;
    if (running) {
      lastFrame = SystemClock.uptimeMillis();
      if (tiltSensor != null) sensorManager.registerListener(this, tiltSensor, 50_000);
      postOnAnimation(frame);
    } else {
      offsetX = offsetY = 0;
      invalidate();
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    updateMotion();
  }

  @Override
  protected void onDetachedFromWindow() {
    foreground = false;
    running = false;
    removeCallbacks(frame);
    if (sensorManager != null) sensorManager.unregisterListener(this);
    super.onDetachedFromWindow();
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    updateMotion();
  }

  @Override
  protected void onVisibilityChanged(View changed, int visibility) {
    super.onVisibilityChanged(changed, visibility);
    // View constructors can dispatch visibility before this class initializes its fields.
    if (frame != null) updateMotion();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!running) return super.onTouchEvent(event);
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        touching = true;
        getParent().requestDisallowInterceptTouchEvent(true);
        updateTouch(event);
        return true;
      case MotionEvent.ACTION_MOVE:
        updateTouch(event);
        return true;
      case MotionEvent.ACTION_UP:
        performClick();
        // Fall through so the composition eases back after release.
      case MotionEvent.ACTION_CANCEL:
        touching = false;
        targetX = targetY = 0;
        baselineReady = false;
        getParent().requestDisallowInterceptTouchEvent(false);
        return true;
      default:
        return true;
    }
  }

  private void updateTouch(MotionEvent event) {
    targetX = clamp((event.getX() / Math.max(1, getWidth()) - .5f) * 2);
    targetY = clamp((event.getY() / Math.max(1, getHeight()) - .5f) * 2);
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (!running || touching) return;
    float x = event.values[0] / SensorManager.GRAVITY_EARTH;
    float y = event.values[1] / SensorManager.GRAVITY_EARTH;
    WindowManager windows = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    int rotation = windows.getDefaultDisplay().getRotation();
    float displayX = x, displayY = y;
    if (rotation == Surface.ROTATION_90) {
      displayX = -y;
      displayY = x;
    } else if (rotation == Surface.ROTATION_180) {
      displayX = -x;
      displayY = -y;
    } else if (rotation == Surface.ROTATION_270) {
      displayX = y;
      displayY = -x;
    }
    if (!baselineReady) {
      baselineX = displayX;
      baselineY = displayY;
      baselineReady = true;
    }
    targetX = clamp(-(displayX - baselineX) * 2.5f);
    targetY = clamp((displayY - baselineY) * 2.5f);
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  private static float clamp(float value) {
    return Math.max(-1, Math.min(1, value));
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    glow =
        new RadialGradient(
            0,
            0,
            190,
            new int[] {0xFF3E7460, 0xFF234E43, 0xFF15332F},
            new float[] {0, .50f, 1},
            Shader.TileMode.CLAMP);
    medallion =
        new LinearGradient(
            -50, -60, 50, 65, new int[] {0xFF568575, 0xFF1B4136}, null, Shader.TileMode.CLAMP);
    moon.reset();
    moon.addCircle(0, 0, 29, Path.Direction.CW);
    Path cutout = new Path();
    cutout.addCircle(13, -9, 28, Path.Direction.CW);
    moon.op(cutout, Path.Op.DIFFERENCE);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float size = Math.min(getWidth(), getHeight()) / 400f;
    canvas.save();
    canvas.translate(getWidth() * .5f, getHeight() * .5f);
    canvas.scale(size, size);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(0xFFFFFFFF);
    paint.setShader(glow);
    canvas.drawCircle(offsetX * 3, offsetY * 3, 192, paint);
    paint.setShader(null);
    for (int i = 0; i < 26; i++) {
      double a = i * 2.39996;
      float radius = 65 + (i % 7) * 18;
      paint.setColor(i % 4 == 0 ? 0xFFBCA778 : 0xFF6C9A84);
      canvas.drawCircle(
          (float) Math.cos(a) * radius + offsetX * 4,
          (float) Math.sin(a) * radius + offsetY * 4,
          i % 4 == 0 ? 1.6f : .8f,
          paint);
    }
    for (int i = 0; i < 3; i++) {
      canvas.save();
      canvas.translate(offsetX * (5 + i * 2), offsetY * (5 + i * 2));
      canvas.rotate(-30 + i * 43 + (float) Math.sin(phase) * 2);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(i == 1 ? 1.2f : .8f);
      paint.setColor(i == 1 ? 0x998DAB89 : 0x556A9D84);
      ring.set(-163 + i * 16, -95 - i * 13, 163 - i * 16, 95 + i * 13);
      canvas.drawOval(ring, paint);
      paint.setColor(0x99CFA763);
      canvas.drawArc(ring, 214, 55, false, paint);
      canvas.restore();
    }
    paint.setStyle(Paint.Style.FILL);
    canvas.save();
    canvas.translate(offsetX * 10, offsetY * 10 + (float) Math.sin(phase) * 3);
    paint.setColor(0x4410201D);
    canvas.drawCircle(0, 9, 62, paint);
    paint.setColor(0xFFFFFFFF);
    paint.setShader(medallion);
    canvas.drawCircle(0, 0, 60, paint);
    paint.setShader(null);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1.2f);
    paint.setColor(0x88B8D4BC);
    canvas.drawCircle(0, 0, 59, paint);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(0xFFEAC581);
    canvas.drawPath(moon, paint);
    sparkle(canvas, 26, -22, 5);
    canvas.restore();
    for (int i = 0; i < icons.length; i++) {
      float depth = depths[i];
      float x = positions[i * 2] * 370 + offsetX * 19 * depth;
      float y =
          positions[i * 2 + 1] * 370
              + offsetY * 19 * depth
              + (float) Math.sin(phase + i * 1.3) * (3 + depth * 2);
      canvas.save();
      canvas.translate(x, y);
      canvas.rotate((i % 2 == 0 ? -9 : 8) + offsetX * 3 * depth);
      float half = 23 + depth * 7;
      paint.setColor(0x33101F1A);
      tile.set(-half - 2, -half + 7, half + 2, half + 11);
      canvas.drawRoundRect(tile, 17, 17, paint);
      tile.set(-half, -half, half, half);
      paint.setColor(0xFFFFFFFF);
      paint.setShader(tileShaders[i]);
      canvas.drawRoundRect(tile, 15, 15, paint);
      paint.setShader(null);
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(0xCCFFF9E8);
      paint.setStrokeWidth(1);
      canvas.drawRoundRect(tile, 15, 15, paint);
      paint.setStyle(Paint.Style.FILL);
      int iconSize = (int) (half * .55f);
      icons[i].setBounds(-iconSize, -iconSize, iconSize, iconSize);
      icons[i].draw(canvas);
      canvas.restore();
    }
    paint.setColor(0xFFCCB77E);
    sparkle(canvas, -87 + offsetX * 7, -66 + offsetY * 7, 3);
    sparkle(canvas, 85 + offsetX * 9, 77 + offsetY * 9, 3.5f);
    canvas.restore();
  }

  private void sparkle(Canvas canvas, float x, float y, float r) {
    canvas.drawOval(x - r * .2f, y - r, x + r * .2f, y + r, paint);
    canvas.drawOval(x - r, y - r * .2f, x + r, y + r * .2f, paint);
  }
}
