package com.zikra.app.ui;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.zikra.app.R;

/** Scoped chrome and density-aware illustration loading for the daily pages. */
public final class DailyUi {
  private DailyUi() {}

  public static void chrome(Activity page, int color) {
    page.getWindow().setStatusBarColor(page.getColor(color));
    page.getWindow().setNavigationBarColor(page.getColor(R.color.daily_paper));
    page.getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            AppAppearance.dark(page) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    FeatureHeader header = page.findViewById(R.id.utilitiesHeader);
    if (header != null) {
      header.setSurfaceColor(page.getColor(color));
      header.heading().setTextSize(23);
      ((TextView) header.heading())
          .setTypeface(
              page.getResources().getFont(R.font.plus_jakarta_sans),
              android.graphics.Typeface.BOLD);
    }
  }

  public static void artwork(ImageView image, int resource) {
    BitmapFactory.Options o = new BitmapFactory.Options();
    o.inJustDecodeBounds = true;
    BitmapFactory.decodeResource(image.getResources(), resource, o);
    int wanted = image.getResources().getDisplayMetrics().widthPixels;
    o.inSampleSize = 1;
    while (o.outWidth / (o.inSampleSize * 2) >= wanted) o.inSampleSize *= 2;
    o.inJustDecodeBounds = false;
    o.inScaled = false;
    image.setImageBitmap(BitmapFactory.decodeResource(image.getResources(), resource, o));
  }
}
