package com.zikra.app.safety;

import android.app.Activity;
import android.content.Intent;
import com.zikra.app.*;

/** Same dock destinations and appearance on overview and forecast detail. */
public final class ProtectorNavigation {
  private ProtectorNavigation() {}

  public static void bind(Activity activity) {
    int[] ids = {
      R.id.protectorNavHome,
      R.id.protectorNavQuran,
      R.id.protectorNavAi,
      R.id.protectorNavStudy,
      R.id.protectorNavProfile
    };
    Class<?>[] pages = {
      HomeActivity.class,
      QuranActivity.class,
      AiActivity.class,
      StudyActivity.class,
      ProfileActivity.class
    };
    for (int i = 0; i < ids.length; i++) {
      final Class<?> page = pages[i];
      activity
          .findViewById(ids[i])
          .setOnClickListener(
              v ->
                  activity.startActivity(
                      new Intent(activity, page).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }
  }
}
