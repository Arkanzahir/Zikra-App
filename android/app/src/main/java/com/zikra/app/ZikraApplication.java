package com.zikra.app;

import android.app.Application;
import com.zikra.app.prayer.PrayerTimetables;

/** Loads cached schedules before UI or boot/alarm receivers request prayer times. */
public final class ZikraApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();
    com.zikra.app.migration.LocalMigration.initialize(this);
    if (!com.zikra.app.migration.LocalMigration.blocked()) finishStartup();
  }
  private boolean initialized;
  public synchronized void finishStartup() {
    if (initialized || com.zikra.app.migration.LocalMigration.blocked()) return;
    initialized = true;
    new Thread(() -> com.zikra.app.migration.LocalMigration.cleanAbandoned(this), "zikra-local-cleanup").start();
    PrayerTimetables.get(this);
    PrayerTimetables.scheduleRefresh(this);
    com.zikra.app.safety.ContextualReminders.schedule(this);
    if (com.zikra.app.safety.EarthquakeAlerts.enabled(this)) com.zikra.app.safety.EarthquakeAlerts.sync(this);
  }
}
