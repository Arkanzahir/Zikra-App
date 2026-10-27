package com.zikra.app.ui;

import com.zikra.app.R;

/** One feature identity across entry points. Action icons retain their own meaning. */
public final class FeatureIcons {
  private FeatureIcons() {}

  public static int destination(String key, int fallback) {
    switch (key) {
      case "quran":
        return R.drawable.feature_icon_quran;
      case "dua":
        return R.drawable.feature_icon_dua;
      case "qibla":
        return R.drawable.feature_icon_qibla;
      case "hifz":
        return R.drawable.feature_icon_hifz;
      case "ai":
        return R.drawable.feature_icon_ai;
      case "protector":
        return R.drawable.feature_icon_protector;
      case "study":
        return R.drawable.feature_icon_study;
      case "hadith":
        return R.drawable.feature_icon_hadith;
      default:
        return fallback;
    }
  }
}
