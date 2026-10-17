package com.zikra.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;

/** XML shortcut container with the gently dipped top edge from the Home reference. */
public final class CurvedShortcutLayout extends LinearLayout {
  private final Paint surface = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path contour = new Path();
  private final float density;

  public CurvedShortcutLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    density = getResources().getDisplayMetrics().density;
    surface.setColor(Color.WHITE);
    setWillNotDraw(false);
    setClipToOutline(false);
    setOutlineProvider(
        new ViewOutlineProvider() {
          @Override
          public void getOutline(View view, Outline outline) {
            // Android 8 requires a convex elevation outline. Keep the actual concave contour
            // for the surface and ripple clipping; this lower outline supplies a soft shadow.
            if (view.getWidth() == 0 || view.getHeight() == 0) {
              outline.setEmpty();
              return;
            }
            outline.setRoundRect(
                0, Math.round(8 * density), view.getWidth(), view.getHeight(), 14 * density);
          }
        });
  }

  @Override
  protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    float radius = Math.min(14 * density, Math.min(width, height) / 2f);
    float dip = Math.min(8 * density, height / 8f);
    contour.reset();
    contour.moveTo(radius, 0);
    contour.cubicTo(width * 0.24f, dip * 0.4f, width * 0.36f, dip, width / 2f, dip);
    contour.cubicTo(width * 0.64f, dip, width * 0.76f, dip * 0.4f, width - radius, 0);
    contour.quadTo(width, 0, width, radius);
    contour.lineTo(width, height - radius);
    contour.quadTo(width, height, width - radius, height);
    contour.lineTo(radius, height);
    contour.quadTo(0, height, 0, height - radius);
    contour.lineTo(0, radius);
    contour.quadTo(0, 0, radius, 0);
    contour.close();
    invalidateOutline();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawPath(contour, surface);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    int checkpoint = canvas.save();
    canvas.clipPath(contour);
    super.dispatchDraw(canvas);
    canvas.restoreToCount(checkpoint);
  }
}
