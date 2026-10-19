package com.zikra.app.ui;

import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/** Touch-driven depth between one intact illustration and its copy. No idle loop or sensors. */
public final class OnboardingParallax {
  private final ImageView artwork;
  private final View copy;
  private final AmbientMotionPolicy policy;
  private final Matrix matrix = new Matrix();
  private final Rect visible = new Rect();
  private ValueAnimator returning;
  private boolean touching;
  private float startX, startY, x, y, scroll;

  public OnboardingParallax(ImageView artwork, View copy) {
    this.artwork = artwork;
    this.copy = copy;
    policy = new AmbientMotionPolicy(artwork.getContext(), this::refresh);
    artwork.setScaleType(ImageView.ScaleType.MATRIX);
    artwork.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> apply());
  }

  public void start() {
    policy.start();
  }

  public void stop() {
    policy.stop();
  }

  public void refresh() {
    if (!enabled()) reset();
    else apply();
  }

  private boolean enabled() {
    return policy.enabled() && artwork.hasWindowFocus() && artwork.isShown();
  }

  public void reset() {
    cancelReturn();
    touching = false;
    x = y = scroll = 0;
    apply();
  }

  public void onScroll(int scrollY) {
    scroll = clamp(scrollY / Math.max(1f, artwork.getHeight()));
    apply();
  }

  /** Observe gestures without consuming them: ScrollView and page swipes retain ownership. */
  public void onTouch(MotionEvent event) {
    if (!enabled()) return;
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        touching =
            artwork.getGlobalVisibleRect(visible)
                && visible.contains((int) event.getRawX(), (int) event.getRawY());
        if (touching) {
          cancelReturn();
          startX = event.getRawX();
          startY = event.getRawY();
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (touching) {
          x = clamp((event.getRawX() - startX) / Math.max(1f, artwork.getWidth() * .25f));
          y = clamp((event.getRawY() - startY) / Math.max(1f, artwork.getHeight() * .25f));
          apply();
        }
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (touching) {
          touching = false;
          settle();
        }
        break;
      default:
        break;
    }
  }

  private void settle() {
    cancelReturn();
    if (x == 0 && y == 0) return;
    float fromX = x, fromY = y;
    returning = ValueAnimator.ofFloat(1f, 0f);
    returning.setDuration(320);
    returning.setInterpolator(new DecelerateInterpolator());
    returning.addUpdateListener(
        a -> {
          x = fromX * (float) a.getAnimatedValue();
          y = fromY * (float) a.getAnimatedValue();
          apply();
        });
    returning.start();
  }

  private void cancelReturn() {
    if (returning != null) {
      returning.cancel();
      returning = null;
    }
  }

  private void apply() {
    Drawable drawable = artwork.getDrawable();
    int width = artwork.getWidth(), height = artwork.getHeight();
    if (drawable == null || width == 0 || height == 0) return;
    int dw = drawable.getIntrinsicWidth(), dh = drawable.getIntrinsicHeight();
    if (dw <= 0 || dh <= 0) return;
    boolean motion = enabled();
    float density = artwork.getResources().getDisplayMetrics().density;
    // A 4% overscan keeps the original panel's edges covered during the small movement.
    float fit = Math.min((float) width / dw, (float) height / dh);
    float scale = fit * (motion ? 1.04f : 1f);
    float marginX = Math.max(0, (dw * scale - width) / 2f);
    float marginY = Math.max(0, (dh * scale - height) / 2f);
    float shiftX = motion ? x * Math.min(7 * density, marginX) : 0;
    float shiftY = motion ? clamp(y + scroll * .6f) * Math.min(6 * density, marginY) : 0;
    matrix.setScale(scale, scale);
    matrix.postTranslate((width - dw * scale) / 2f + shiftX, (height - dh * scale) / 2f + shiftY);
    artwork.setImageMatrix(matrix);
    copy.setTranslationX(motion ? -x * 2 * density : 0);
    copy.setTranslationY(motion ? -y * 2 * density : 0);
  }

  private static float clamp(float value) {
    return Math.max(-1f, Math.min(1f, value));
  }
}
