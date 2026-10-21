package com.zikra.app.assistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import com.zikra.app.R;
import com.zikra.app.ui.AmbientMotionPolicy;

/** Native decorative glass motion; no microphone sampling, bitmaps or background animation. */
public final class AssistantOrbView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private final Path clip = new Path();
  private final Rect visible = new Rect();
  private final AmbientMotionPolicy policy;
  private final int[] glassColors, bandColors;
  private final int borderColor;
  private boolean listening;
  private boolean foreground, framePosted;
  private long lastTick;
  private float phase, cx, cy, radius;
  private Shader glass;
  private final ViewTreeObserver.OnScrollChangedListener scrollChanged = this::refreshMotion;
  private final ViewTreeObserver.OnGlobalLayoutListener layoutChanged = this::refreshMotion;
  private final Runnable frame = new Runnable() {
    @Override
    public void run() {
      framePosted = false;
      if (!canAnimate()) { stopFrames(); return; }
      long now = SystemClock.uptimeMillis();
      phase = (phase + Math.min(100, Math.max(0, now - lastTick)) * .00075f)
          % ((float) Math.PI * 2f);
      lastTick = now;
      invalidate();
      scheduleFrame();
    }
  };

  public AssistantOrbView(Context context, AttributeSet attributes) {
    super(context, attributes);
    glassColors = new int[] {context.getColor(R.color.white),
      context.getColor(R.color.orb_glass_light), context.getColor(R.color.orb_glass_mid),
      context.getColor(R.color.orb_glass_dark)};
    bandColors = new int[] {context.getColor(R.color.orb_ribbon_cyan),
      context.getColor(R.color.orb_ribbon_pink), context.getColor(R.color.orb_ribbon_purple)};
    borderColor = context.getColor(R.color.orb_border);
    policy = new AmbientMotionPolicy(context, this::refreshMotion);
  }

  public void setActive(boolean active) {
    foreground = active;
    if (active) policy.start(); else policy.stop();
    refreshMotion();
  }

  private boolean canAnimate() {
    return foreground && policy.enabled() && radius > 0 && isAttachedToWindow()
        && isShown() && getWindowVisibility() == VISIBLE && hasWindowFocus()
        && getGlobalVisibleRect(visible);
  }

  private void refreshMotion() {
    if (canAnimate()) scheduleFrame(); else stopFrames();
  }

  private void scheduleFrame() {
    if (!framePosted && canAnimate()) {
      if (lastTick == 0) lastTick = SystemClock.uptimeMillis();
      framePosted = true;
      postOnAnimationDelayed(frame, 50);
    }
  }

  private void stopFrames() {
    removeCallbacks(frame);
    framePosted = false;
    lastTick = 0;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    cx = w / 2f; cy = h / 2f;
    radius = Math.min(cx, cy) * .82f;
    clip.reset();
    if (radius > 0) {
      clip.addCircle(cx, cy, radius, Path.Direction.CW);
      glass = new RadialGradient(cx - radius * .36f, cy - radius * .4f,
          radius * 1.8f, glassColors, new float[] {0f, .28f, .66f, 1f}, Shader.TileMode.CLAMP);
    }
    refreshMotion();
  }

  public void setListening(boolean value) {
    listening = value;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (radius <= 0 || glass == null) return;
    canvas.save();
    float breathe = 1f + (float) Math.sin(phase) * .022f;
    canvas.scale(breathe, breathe, cx, cy);
    paint.setStyle(Paint.Style.FILL);
    paint.setAlpha(255);
    paint.setShader(glass);
    canvas.drawCircle(cx, cy, radius, paint);
    paint.setShader(null);
    canvas.save();
    canvas.clipPath(clip);
    canvas.rotate(-32 + (float) Math.sin(phase) * 12f, cx, cy);
    paint.setStyle(Paint.Style.STROKE);
    for (int band = 0; band < 16; band++) {
      float offset = (band - 7.5f) * radius * .15f;
      path.reset();
      for (int step = 0; step <= 48; step++) {
        float x = cx - radius * 1.4f + step * radius * 2.8f / 48f;
        float progress = (x - cx) / radius;
        float y = cy + offset + (float) Math.sin(progress * 2.4f + band * .36f + phase) * radius * .44f;
        if (step == 0) path.moveTo(x, y);
        else path.lineTo(x, y);
      }
      paint.setStrokeWidth(radius * (band % 3 == 0 ? .068f : .025f));
      paint.setColor(bandColors[band % 3]);
      paint.setAlpha(band % 3 == 0 ? 185 : 155);
      canvas.drawPath(path, paint);
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.WHITE);
    paint.setAlpha(50);
    float glint = (float) Math.sin(phase) * radius * .18f;
    canvas.drawOval(cx - radius * .7f + glint, cy - radius * .72f,
        cx + radius * .15f + glint, cy - radius * .35f, paint);
    canvas.restore();
    paint.setAlpha(255);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(borderColor);
    paint.setStrokeWidth(Math.max(1, radius * .016f));
    canvas.drawCircle(cx, cy, radius, paint);
    if (listening) {
      paint.setAlpha(95);
      canvas.drawCircle(cx, cy, radius * (1.08f + .025f * (float) Math.sin(phase * 2)), paint);
      paint.setAlpha(255);
    }
    canvas.restore();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    getViewTreeObserver().addOnScrollChangedListener(scrollChanged);
    getViewTreeObserver().addOnGlobalLayoutListener(layoutChanged);
    if (foreground) policy.start();
    refreshMotion();
  }

  @Override
  public void onWindowFocusChanged(boolean focused) {
    super.onWindowFocusChanged(focused);
    refreshMotion();
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (policy != null) refreshMotion();
  }

  @Override
  protected void onDetachedFromWindow() {
    stopFrames();
    policy.stop();
    if (getViewTreeObserver().isAlive()) {
      getViewTreeObserver().removeOnScrollChangedListener(scrollChanged);
      getViewTreeObserver().removeOnGlobalLayoutListener(layoutChanged);
    }
    super.onDetachedFromWindow();
  }
}
