package com.zikra.app.safety;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

/** Shared themed system bars for the three Info BMKG screens. */
public final class ProtectorChrome {
  private ProtectorChrome() {}

  public static void apply(Activity activity) {
    ((com.zikra.app.ui.FeatureHeader) activity.findViewById(com.zikra.app.R.id.sharedFeatureHeader))
        .bindIds(
            com.zikra.app.R.id.toolbarBack,
            com.zikra.app.R.id.protectorToolbarTitle,
            com.zikra.app.R.id.protectorToolbarSubtitle,
            com.zikra.app.R.id.protectorRefresh);
    activity
        .findViewById(com.zikra.app.R.id.toolbarBack)
        .setOnClickListener(v -> activity.finish());
    activity.getWindow().setStatusBarColor(activity.getColor(com.zikra.app.R.color.app_surface));
    activity.getWindow().setNavigationBarColor(activity.getColor(com.zikra.app.R.color.app_surface));
    View decor = activity.getWindow().getDecorView();
    int mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    decor.setSystemUiVisibility((decor.getSystemUiVisibility() & ~mask)
        | (com.zikra.app.ui.AppAppearance.dark(activity) ? 0 : mask));
  }
}
