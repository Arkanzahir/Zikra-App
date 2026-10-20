package com.zikra.app.ui;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** Decorative native companion. The actual timer is owned by FocusClock, never by drawing. */
public final class FocusGardenView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF shape = new RectF();
  private final Path path = new Path();
  private boolean allowed, active, running, resting, finished, framePosted;
  private long lastFrame;
  private float motionTime, phaseMix;
  private android.animation.ValueAnimator phase;
  private final Runnable frame =
      () -> {
        framePosted = false;
        if (!canAnimate()) return;
        long now = SystemClock.uptimeMillis();
        // Advance by real visible time. Slow frames must not turn the motion into slow motion.
        motionTime += Math.max(0, (now - lastFrame) / 1000f);
        invalidate();
        schedule();
      };

  public FocusGardenView(Context context, AttributeSet attrs) {
    super(context, attrs);
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
    float density = getResources().getDisplayMetrics().density;
    float scale = Math.min(1.35f * density, Math.min(getWidth() / 204f, getHeight() / 184f));
    float breath = (float) Math.sin(motionTime * 2.1f);
    float sway = (float) Math.sin(motionTime * 1.65f);
    float hand = (float) Math.sin(motionTime * 2.1f + .8f);
    canvas.save();
    canvas.translate(getWidth() / 2f, getHeight() * .56f);
    canvas.scale(scale, scale);
    // The floor stays still while the companion moves independently above it.
    oval(canvas, -53 - breath * 2, 62, 53 + breath * 2, 70, 0x182F6750);
    if (phaseMix > 0) {
      oval(canvas, -61, 47, 61, 66, blend(0xFFEDF4EF, 0xFFB3CEBE, phaseMix));
      line(canvas, -58, 57, -67, 60, 0xFF87AC98, 1.5f);
      line(canvas, 58, 57, 67, 60, 0xFF87AC98, 1.5f);
    }
    canvas.translate(sway * (1 - phaseMix) * 2.5f, breath * (resting ? 2.5f : 4.8f));
    canvas.rotate(sway * (resting ? 1.5f : 4f), 0, 38);
    // Seated feet during a break; alternating feet during focus.
    if (phaseMix > .5f) {
      round(canvas, -34, 40, 0, 58, 9, 0xFFFCFEFC);
      round(canvas, 0, 40, 34, 58, 9, 0xFFFCFEFC);
    } else {
      canvas.save();
      canvas.rotate(sway * 10, -13, 40);
      round(canvas, -26, 36, -6, 61, 8, 0xFFFCFEFC);
      line(canvas, -23, 59, -9, 59, 0xFF7F9E8E, 1.2f);
      canvas.restore();
      canvas.save();
      canvas.rotate(-sway * 10, 13, 40);
      round(canvas, 6, 36, 26, 61, 8, 0xFFFCFEFC);
      line(canvas, 9, 59, 23, 59, 0xFF7F9E8E, 1.2f);
      canvas.restore();
    }
    round(canvas, -29, -8, 29, 46, 20, 0xFFFCFEFC);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0xFF719385);
    paint.setStrokeWidth(1.4f);
    shape.set(-29, -8, 29, 46);
    canvas.drawRoundRect(shape, 20, 20, paint);
    paint.setStyle(Paint.Style.FILL);
    // A separate nod makes the reading pose visibly alive rather than shifting one static icon.
    canvas.save();
    canvas.translate(0, -1.8f * hand);
    canvas.rotate(sway * 2.2f, 0, -30);
    round(canvas, -42, -74, 42, 6, 33, blend(0xFF214D43, 0xFF447A65, phaseMix));
    round(canvas, -31, -56, 31, -5, 21, 0xFFFCFEFC);
    canvas.save();
    canvas.rotate(sway * 9, 2, -72);
    path.reset();
    path.moveTo(-2, -73);
    path.cubicTo(-21, -94, -9, -101, 4, -81);
    path.cubicTo(15, -100, 32, -90, 12, -74);
    path.close();
    paint.setColor(0xFFE5B565);
    canvas.drawPath(path, paint);
    canvas.restore();
    boolean blink = running && active && allowed && motionTime % 4.4f > 4.15f;
    if (blink || (resting && motionTime % 5.6f > 3.9f)) {
      line(canvas, -15, -30, -8, -29, 0xFF15332F, 2);
      line(canvas, 8, -29, 15, -30, 0xFF15332F, 2);
    } else {
      oval(canvas, -14 + sway, -34, -10 + sway, -28, 0xFF15332F);
      oval(canvas, 10 + sway, -34, 14 + sway, -28, 0xFF15332F);
    }
    oval(canvas, -23, -25, -13, -20, 0x55E5A853);
    oval(canvas, 13, -25, 23, -20, 0x55E5A853);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0xFF42715D);
    paint.setStrokeWidth(1.5f);
    shape.set(-5, -27, 5, -17);
    canvas.drawArc(shape, 15, 150, false, paint);
    paint.setStyle(Paint.Style.FILL);
    canvas.restore();
    canvas.save();
    canvas.translate(hand * 1.7f, hand * 2.6f);
    canvas.rotate(hand * 3, 0, 17);
    if (phaseMix < .5f) drawBook(canvas);
    else drawCup(canvas);
    canvas.restore();
    if (finished) {
      oval(canvas, 51, -51, 56, -46, 0xFFD8A759);
      oval(canvas, -55, -62, -51, -58, 0xFFD8A759);
    }
    canvas.restore();
  }

  private void drawBook(Canvas canvas) {
    // Hands and book move together. A page periodically folds across the open book.
    line(canvas, -25, 8, -25, 23, 0xFF719385, 10);
    line(canvas, 25, 8, 25, 23, 0xFF719385, 10);
    path.reset();
    path.moveTo(-27, 6);
    path.lineTo(0, 11);
    path.lineTo(27, 6);
    path.lineTo(27, 31);
    path.lineTo(0, 36);
    path.lineTo(-27, 31);
    path.close();
    paint.setColor(0xFFD8A759);
    canvas.drawPath(path, paint);
    line(canvas, 0, 12, 0, 33, 0xFF8A672E, 1.2f);
    for (int i = 0; i < 3; i++) {
      line(canvas, -21, 14 + i * 5, -6, 17 + i * 5, 0xFFFFF3DB, 1.2f);
      line(canvas, 6, 17 + i * 5, 21, 14 + i * 5, 0xFFFFF3DB, 1.2f);
    }
    float turn = motionTime % 5f;
    if (turn < 1.5f) {
      float edge = 25 * (float) Math.cos(turn / 1.5f * Math.PI);
      path.reset();
      path.moveTo(0, 11);
      path.quadTo(edge * .5f, 2, edge, 7);
      path.lineTo(edge, 30);
      path.quadTo(edge * .5f, 27, 0, 35);
      path.close();
      paint.setColor(0xFFFFF3DB);
      canvas.drawPath(path, paint);
    }
    oval(canvas, -36, 15, -22, 28, 0xFFFCFEFC);
    oval(canvas, 22, 15, 36, 28, 0xFFFCFEFC);
  }

  private void drawCup(Canvas canvas) {
    round(canvas, -14, 11, 14, 36, 6, 0xFF8DB6A3);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0xFF57927E);
    paint.setStrokeWidth(3);
    shape.set(9, 16, 24, 29);
    canvas.drawOval(shape, paint);
    paint.setStyle(Paint.Style.FILL);
    oval(canvas, -33, 19, -12, 32, 0xFFFCFEFC);
    oval(canvas, 12, 19, 33, 32, 0xFFFCFEFC);
    // Separate drifting steam strokes make the rest pose easy to distinguish in motion.
    for (int i = 0; i < 2; i++) {
      float rise = (motionTime * 11 + i * 10) % 22;
      float drift = (float) Math.sin(motionTime * 2.5f + i) * 3;
      paint.setColor(0x88719385);
      paint.setAlpha((int) (130 * (1 - rise / 22)));
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(1.7f);
      path.reset();
      path.moveTo(-4 + i * 9, 7 - rise);
      path.cubicTo(
          -9 + i * 9 + drift, 2 - rise, 2 + i * 9 + drift, -2 - rise, -3 + i * 9, -7 - rise);
      canvas.drawPath(path, paint);
      paint.setAlpha(255);
      paint.setStyle(Paint.Style.FILL);
    }
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
