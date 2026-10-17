package com.zikra.app.safety;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

/** Shared light system bars for the three Info BMKG screens. */
public final class ProtectorChrome {
  private ProtectorChrome() {}

  public static void apply(Activity activity) {
    activity.getWindow().setStatusBarColor(Color.WHITE);
    activity.getWindow().setNavigationBarColor(Color.WHITE);
    View decor = activity.getWindow().getDecorView();
    decor.setSystemUiVisibility(
        decor.getSystemUiVisibility()
            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }
}
