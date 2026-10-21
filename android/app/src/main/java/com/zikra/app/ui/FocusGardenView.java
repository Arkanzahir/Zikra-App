package com.zikra.app.ui;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** Decorative native companion. The actual timer is owned by FocusClock, never by drawing. */
public final class FocusGardenView extends View {
  private boolean allowed, active, running, resting, finished, framePosted;
  private long lastFrame;
  private float motionTime;
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
      invalidate();
    } else schedule();
  }

  public void snapshot(
      long remaining, long total, boolean running, boolean resting, boolean finished) {
    this.running = running;
    this.finished = finished;
    this.resting = resting;
    if (canAnimate()) schedule();
    else stop();
    invalidate();
  }

  private boolean canAnimate() {
    return active
        && allowed
        && (running || (resting && !finished))
        && isAttachedToWindow()
        && isShown()
        && hasWindowFocus();
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

  private StudyScenePainter scene;

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (scene == null) scene = new StudyScenePainter(getContext());
    scene.draw(canvas, getWidth(), getHeight(), motionTime, resting, false);
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    if (focus) schedule();
    else stop();
  }

  @Override
  protected void onDetachedFromWindow() {
    stop();
    super.onDetachedFromWindow();
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == VISIBLE) schedule();
    else stop();
  }
}
