package com.zikra.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/** Non-animated thumbnail; the full timer owns animated scene frames. */
public final class StudySceneView extends View {
  private final StudyScenePainter scene;

  public StudySceneView(Context context, AttributeSet attrs) {
    super(context, attrs);
    scene = new StudyScenePainter(context);
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    setMeasuredDimension(
        MeasureSpec.getSize(widthSpec),
        MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY
            ? MeasureSpec.getSize(heightSpec)
            : Math.round(152 * getResources().getDisplayMetrics().density));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    scene.draw(canvas, getWidth(), getHeight(), 0, false, true);
  }
}
