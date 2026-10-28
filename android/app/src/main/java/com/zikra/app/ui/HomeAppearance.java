package com.zikra.app.ui;

import android.content.Context;
import android.content.res.Configuration;

/** A Home-only choice; sky illumination still follows the selected location and real time. */
public final class HomeAppearance {
  public static final String PREFS = "zikra_home_appearance";
  public static final String KEY = "theme";

  private HomeAppearance() {}

  public static String selected(Context context) {
    String value =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "light");
    return "dark".equals(value) || "system".equals(value) ? value : "light";
  }

  public static Context wrap(Context base) {
    String selected = selected(base);
    if ("system".equals(selected)) return base;
    Configuration config = new Configuration(base.getResources().getConfiguration());
    config.uiMode =
        (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
            | ("dark".equals(selected)
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO);
    return base.createConfigurationContext(config);
  }
}
