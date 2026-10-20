package com.zikra.app.ui;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** Decorative native companion. Animation never owns or advances the actual countdown. */
public final class FocusGardenView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF ring = new RectF(), shape = new RectF();
  private final Path path = new Path();
  private boolean allowed, active, running, resting, finished, framePosted;
  private long lastFrame;
  private float elapsed, density, motionTime, phaseMix;
  private android.animation.ValueAnimator phase;
  private final Runnable frame =
      () -> {
        framePosted = false;
        if (!canAnimate()) return;
        long now = SystemClock.uptimeMillis();
        motionTime += Math.min(.05f, Math.max(.001f, (now - lastFrame) / 1000f));
        invalidate();
        schedule();
      };

  public FocusGardenView(Context context, AttributeSet attrs) {
    super(context, attrs);
    density = getResources().getDisplayMetrics().density;
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    setFocusable(false);
    setClickable(false);
  }

  public void setMotion(boolean active, boolean allowed) {
    this.active = active;
    this.allowed = allowed;
    if (!active || !allowed) {
      stop();
      if (phase != null) phase.cancel();
      phaseMix = resting ? 1f : 0f;
      invalidate();
    } else schedule();
  }

  public void snapshot(
      long remaining, long total, boolean running, boolean resting, boolean finished) {
    elapsed = total <= 0 ? 0 : Math.max(0, Math.min(1, 1f - (float) remaining / total));
    this.running = running;
    this.finished = finished;
    if (this.resting != resting) {
      this.resting = resting;
      if (phase != null) phase.cancel();
      if (active && allowed && hasWindowFocus()) {
        phase = android.animation.ValueAnimator.ofFloat(phaseMix, resting ? 1f : 0f);
        phase.setDuration(420);
        phase.addUpdateListener(
            a -> {
              phaseMix = (float) a.getAnimatedValue();
              invalidate();
            });
        phase.start();
      } else phaseMix = resting ? 1f : 0f;
    }
    if (canAnimate()) schedule();
    else stop();
    invalidate();
  }

  private boolean canAnimate() {
    return active && allowed && running && isAttachedToWindow() && isShown() && hasWindowFocus();
  }

  private void schedule() {
    if (canAnimate() && !framePosted) {
      framePosted = true;
      lastFrame = SystemClock.uptimeMillis();
      postOnAnimationDelayed(frame, 33);
    }
  }

  private void stop() {
    removeCallbacks(frame);
    framePosted = false;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    float r = Math.min(w * .29f, h * .26f), cy = h * .31f;
    ring.set(w / 2f - r, cy - r, w / 2f + r, cy + r);
  }

  private static int blend(int a, int b, float t) {
    return Color.rgb(
        (int) (Color.red(a) * (1 - t) + Color.red(b) * t),
        (int) (Color.green(a) * (1 - t) + Color.green(b) * t),
        (int) (Color.blue(a) * (1 - t) + Color.blue(b) * t));
  }

  private void oval(Canvas c, float left, float top, float right, float bottom, int color) {
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(color);
    shape.set(left, top, right, bottom);
    c.drawOval(shape, paint);
  }

  private void round(
      Canvas c, float left, float top, float right, float bottom, float radius, int color) {
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(color);
    shape.set(left, top, right, bottom);
    c.drawRoundRect(shape, radius, radius, paint);
  }

  private void line(Canvas c, float x, float y, float x2, float y2, int color, float stroke) {
    paint.setColor(color);
    paint.setStrokeWidth(stroke);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeCap(Paint.Cap.ROUND);
    c.drawLine(x, y, x2, y2, paint);
    paint.setStyle(Paint.Style.FILL);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int accent = blend(0xFFCF9447, 0xFF57927E, phaseMix);
    float cx = ring.centerX(), cy = ring.centerY(), r = ring.width() / 2;
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(blend(0xFFEAF3EE, 0xFFE3F2EC, phaseMix));
    canvas.drawOval(ring, paint);
    paint.setColor(0xFFBCCFC5);
    for (int i = 0; i < 12; i++) {
      double a = Math.toRadians(i * 30 - 90);
      canvas.drawCircle(
          cx + (r + 8 * density) * (float) Math.cos(a),
          cy + (r + 8 * density) * (float) Math.sin(a),
          1.2f * density,
          paint);
    }
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(3.5f * density);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setColor(0xFFD0E0D6);
    canvas.drawOval(ring, paint);
    paint.setColor(accent);
    if (elapsed > 0) canvas.drawArc(ring, -90, 360 * elapsed, false, paint);
    paint.setStyle(Paint.Style.FILL);
    double end = Math.toRadians(360 * elapsed - 90);
    float hx = cx + r * (float) Math.cos(end), hy = cy + r * (float) Math.sin(end);
    paint.setColor(accent);
    paint.setAlpha(30);
    canvas.drawCircle(hx, hy, 8 * density, paint);
    paint.setAlpha(255);
    canvas.drawCircle(hx, hy, 3.5f * density, paint);
    // Original leaf companion; it is decorative artwork, not a replacement for the brand logo.
    float scale = Math.min(density, getWidth() / 280f);
    canvas.save();
    canvas.translate(getWidth() / 2f, getHeight() - 59 * density);
    canvas.scale(scale, scale);
    float breath = (float) Math.sin(motionTime * 1.7f);
    oval(canvas, -51, 47, 51, 55, 0x182F6750);
    oval(canvas, -44, 35, 44, 49, blend(0xFFDCE9E0, 0xFFD4E7E0, phaseMix));
    canvas.translate(0, breath * 1.8f);
    // Feet and the white body retain a calm silhouette in both poses.
    round(canvas, -22, 26, -4, 44, 7, 0xFFFCFEFC);
    round(canvas, 4, 26, 22, 44, 7, 0xFFFCFEFC);
    round(canvas, -25, -8, 25, 36, 17, 0xFFFCFEFC);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0xFF719385);
    paint.setStrokeWidth(1.5f);
    shape.set(-25, -8, 25, 36);
    canvas.drawRoundRect(shape, 17, 17, paint);
    paint.setStyle(Paint.Style.FILL);
    // Original Pine hood with a gold leaf.
    oval(canvas, -37, -68, 37, 4, blend(0xFF214D43, 0xFF447A65, phaseMix));
    oval(canvas, -27, -54, 27, -5, 0xFFFCFEFC);
    path.reset();
    path.moveTo(-1, -65);
    path.cubicTo(-15, -85, -4, -91, 6, -72);
    path.cubicTo(18, -91, 31, -82, 13, -67);
    path.close();
    paint.setColor(0xFFE5B565);
    canvas.drawPath(path, paint);
    boolean blink = running && active && allowed && motionTime % 5.2f > 4.95f;
    if (blink) {
      line(canvas, -13, -30, -6, -30, 0xFF15332F, 2);
      line(canvas, 6, -30, 13, -30, 0xFF15332F, 2);
    } else {
      oval(canvas, -12, -34, -8, -28, 0xFF15332F);
      oval(canvas, 8, -34, 12, -28, 0xFF15332F);
    }
    oval(canvas, -21, -25, -12, -20, 0x55E5A853);
    oval(canvas, 12, -25, 21, -20, 0x55E5A853);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0xFF42715D);
    paint.setStrokeWidth(1.4f);
    shape.set(-5, -28, 5, -18);
    canvas.drawArc(shape, 15, 150, false, paint);
    paint.setStyle(Paint.Style.FILL);
    if (phaseMix < .5f) {
      // Open book during focus, held independently of the live countdown.
      path.reset();
      path.moveTo(-23, 2);
      path.lineTo(0, 7);
      path.lineTo(23, 2);
      path.lineTo(23, 24);
      path.lineTo(0, 29);
      path.lineTo(-23, 24);
      path.close();
      paint.setColor(0xFFD8A759);
      canvas.drawPath(path, paint);
      line(canvas, 0, 8, 0, 26, 0xFF8A672E, 1.2f);
      line(canvas, -17, 10, -5, 13, 0xFFFFF3DB, 1.4f);
      line(canvas, 5, 13, 17, 10, 0xFFFFF3DB, 1.4f);
      oval(canvas, -31, 7, -18, 20, 0xFFFCFEFC);
      oval(canvas, 18, 7, 31, 20, 0xFFFCFEFC);
    } else {
      round(canvas, -12, 4, 12, 26, 5, 0xFF8DB6A3);
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(0xFF57927E);
      paint.setStrokeWidth(3);
      shape.set(7, 8, 21, 20);
      canvas.drawOval(shape, paint);
      paint.setStyle(Paint.Style.FILL);
      oval(canvas, -30, 10, -10, 24, 0xFFFCFEFC);
      oval(canvas, 10, 10, 30, 24, 0xFFFCFEFC);
      float steam = (float) Math.sin(motionTime * 1.3f) * 2;
      line(canvas, -3, -2, -4, -9 - steam, 0x88719385, 1.3f);
      line(canvas, 4, -3, 6, -11 - steam, 0x88719385, 1.3f);
    }
    if (finished) {
      oval(canvas, 43, -42, 48, -37, accent);
      oval(canvas, -46, -55, -42, -51, accent);
    }
    canvas.restore();
  }

  @Override
  public void onWindowFocusChanged(boolean focused) {
    super.onWindowFocusChanged(focused);
    if (focused) schedule();
    else {
      stop();
      if (phase != null) phase.cancel();
      phaseMix = resting ? 1f : 0f;
      invalidate();
    }
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == VISIBLE) schedule();
    else stop();
  }

  @Override
  protected void onDetachedFromWindow() {
    stop();
    if (phase != null) phase.cancel();
    super.onDetachedFromWindow();
  }
}
