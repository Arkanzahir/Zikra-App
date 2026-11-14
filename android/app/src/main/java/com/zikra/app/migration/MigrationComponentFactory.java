package com.zikra.app.migration;

import android.app.Activity;
import android.content.Intent;

/** Notification/deep entry points cannot bypass a pending startup recovery on Android 9+. */
@android.annotation.TargetApi(28)
public final class MigrationComponentFactory extends android.app.AppComponentFactory {
  @Override public Activity instantiateActivity(ClassLoader loader, String name, Intent intent)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    return super.instantiateActivity(loader, LocalMigration.blocked()
        ? MigrationLoadingActivity.class.getName() : name, intent);
  }
}
