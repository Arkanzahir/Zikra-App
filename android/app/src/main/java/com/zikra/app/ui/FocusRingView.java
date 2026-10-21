package com.zikra.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.zikra.app.R;

/** A display of the saved clock, never a second clock or independent animator. */
public final class FocusRingView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF bounds = new RectF();
  private float remaining = 1;
  private boolean resting;
  private final int track, pine, gold;

  public FocusRingView(Context context, AttributeSet attrs) {
    super(context, attrs);
    track = context.getColor(R.color.concept_mint);
    pine = context.getColor(R.color.concept_pine);
    gold = context.getColor(R.color.concept_gold);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeCap(Paint.Cap.ROUND);
  }

  public void snapshot(long remainingMs, long totalMs, boolean resting) {
    this.remaining = Math.max(0, Math.min(1, (float) remainingMs / Math.max(1, totalMs)));
    this.resting = resting;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    float stroke = 7 * getResources().getDisplayMetrics().density;
    float edge = Math.min(getWidth(), getHeight()) - stroke * 2;
    float left = (getWidth() - edge) / 2, top = (getHeight() - edge) / 2;
    bounds.set(left, top, left + edge, top + edge);
    paint.setStrokeWidth(stroke);
    paint.setColor(track);
    canvas.drawOval(bounds, paint);
    paint.setColor(resting ? gold : pine);
    canvas.drawArc(bounds, -90, 360 * remaining, false, paint);
  }
}
