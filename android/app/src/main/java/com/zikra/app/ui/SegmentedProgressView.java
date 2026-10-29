package com.zikra.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Exactly 24 visual ticks; the filled fraction follows current SQLite completion. */
public final class SegmentedProgressView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private int completed = 0, total = 24;

  public SegmentedProgressView(Context c, AttributeSet a) {
    super(c, a);
  }

  public void setProgress(int completed, int total) {
    this.completed = completed;
    this.total = total;
    setContentDescription(completed + " dari " + total + " checklist selesai");
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float gap = 3 * getResources().getDisplayMetrics().density;
    float width = (getWidth() - 23 * gap) / 24f;
    int filled = total == 0 ? 0 : Math.round(24f * completed / total);
    for (int i = 0; i < 24; i++) {
      paint.setColor(getContext().getColor(i < filled ? com.zikra.app.R.color.text : com.zikra.app.R.color.app_track));
      float x = i * (width + gap);
      canvas.drawRoundRect(x, 0, x + width, getHeight(), 2, 2, paint);
    }
  }
}
