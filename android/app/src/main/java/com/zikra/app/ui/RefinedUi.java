package com.zikra.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import com.zikra.app.R;

/** Scoped surfaces for the approved daily pages; other feature themes remain independent. */
public final class RefinedUi {
  private RefinedUi() {}

  public static int dp(Context context, float value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }

  public static boolean dark(Context context) {
    return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
        == Configuration.UI_MODE_NIGHT_YES;
  }

  public static GradientDrawable surface(
      Context context, int fill, boolean outlined, float radius) {
    GradientDrawable result = new GradientDrawable();
    result.setColor(context.getColor(fill));
    result.setCornerRadius(dp(context, radius));
    if (outlined) result.setStroke(dp(context, 1), context.getColor(R.color.refined_line));
    return result;
  }

  public static TextView text(
      Context context, CharSequence value, float size, int color, boolean bold) {
    TextView view = new TextView(context);
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(context.getColor(color));
    if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
    return view;
  }

  public static void chrome(Activity page, FeatureHeader header, boolean brand) {
    int surface = page.getColor(brand ? R.color.refined_primary : R.color.refined_background);
    int ink = page.getColor(brand ? R.color.refined_on_primary : R.color.refined_ink);
    page.getWindow().setStatusBarColor(surface);
    page.getWindow().setNavigationBarColor(page.getColor(R.color.refined_background));
    int flags = dark(page) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    if (!brand && !dark(page)) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    page.getWindow().getDecorView().setSystemUiVisibility(flags);
    if (header == null) return;
    header.setSurfaceColor(surface);
    header.heading().setTextColor(ink);
    header.heading().setTextSize(19);
    TextView caption = header.findViewById(R.id.featureSubtitle);
    if (caption != null)
      caption.setTextColor(
          page.getColor(brand ? R.color.refined_brand_caption : R.color.refined_muted));
    ((ImageButton) header.back()).setImageTintList(ColorStateList.valueOf(ink));
    ((ImageButton) header.action()).setImageTintList(ColorStateList.valueOf(ink));
  }
}
