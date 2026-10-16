package com.zikra.app.discovery;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

/** Small native view helpers shared only by the discovery screens. */
public final class DiscoveryUi {
  private DiscoveryUi() {}

  public static int dp(Context context, float value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }

  public static GradientDrawable background(Context context, String color, float radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(Color.parseColor(color));
    drawable.setCornerRadius(dp(context, radius));
    return drawable;
  }

  public static GradientDrawable outlined(
      Context context, String color, String outline, float radius) {
    GradientDrawable drawable = background(context, color, radius);
    drawable.setStroke(dp(context, 1), Color.parseColor(outline));
    return drawable;
  }

  public static TextView text(
      Context context, String content, int size, String color, boolean bold) {
    TextView view = new TextView(context);
    view.setText(content);
    view.setTextSize(size);
    view.setTextColor(Color.parseColor(color));
    if (bold) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    return view;
  }
}
