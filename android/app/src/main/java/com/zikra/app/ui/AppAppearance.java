package com.zikra.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import com.zikra.app.R;

/** One persisted appearance for every app screen; preserves the original Home preference. */
public final class AppAppearance {
  public static final String PREFS = "zikra_home_appearance";
  public static final String KEY = "theme";
  private static final String[] MODES = {"light", "dark", "system"};

  private AppAppearance() {}

  public static String selected(Context context) {
    String mode = context.getSharedPreferences(PREFS, 0).getString(KEY, "light");
    return "dark".equals(mode) || "system".equals(mode) ? mode : "light";
  }

  public static int resolvedNightMode(Context context) {
    String mode = selected(context);
    if ("dark".equals(mode)) return Configuration.UI_MODE_NIGHT_YES;
    if ("light".equals(mode)) return Configuration.UI_MODE_NIGHT_NO;
    Context application = context.getApplicationContext();
    return (application == null ? context : application).getResources().getConfiguration().uiMode
        & Configuration.UI_MODE_NIGHT_MASK;
  }

  public static boolean dark(Context context) {
    return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
        == Configuration.UI_MODE_NIGHT_YES;
  }

  public static Context wrap(Context base) {
    if ("system".equals(selected(base))) return base;
    Configuration config = new Configuration(base.getResources().getConfiguration());
    config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | resolvedNightMode(base);
    return base.createConfigurationContext(config);
  }

  public static void select(Activity activity, String mode) {
    if (!"light".equals(mode) && !"dark".equals(mode) && !"system".equals(mode))
      throw new IllegalArgumentException("Unknown appearance");
    if (mode.equals(selected(activity))) return;
    activity.getSharedPreferences(PREFS, 0).edit().putString(KEY, mode).apply();
    activity.recreate();
  }

  public static String label(Context context) {
    String mode = selected(context);
    int index = "dark".equals(mode) ? 1 : "system".equals(mode) ? 2 : 0;
    return context.getResources().getStringArray(R.array.home_appearance_choices)[index];
  }

  public static AlertDialog choose(Activity activity) {
    String mode = selected(activity);
    return new AlertDialog.Builder(activity)
        .setTitle(R.string.app_appearance)
        .setSingleChoiceItems(R.array.home_appearance_choices,
            "dark".equals(mode) ? 1 : "system".equals(mode) ? 2 : 0,
            (dialog, which) -> {
              dialog.dismiss();
              select(activity, MODES[which]);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
