package com.zikra.app.ui;

import android.content.Context;

/** Compatibility alias for the saved Home choice, now shared across the app. */
public final class HomeAppearance {
  public static final String PREFS = AppAppearance.PREFS;
  public static final String KEY = AppAppearance.KEY;
  private HomeAppearance() {}
  public static String selected(Context context) { return AppAppearance.selected(context); }
  public static Context wrap(Context base) { return AppAppearance.wrap(base); }
}
