package com.zikra.app;

import android.app.Application;
import com.zikra.app.prayer.PrayerTimetables;

/** Loads cached schedules before UI or boot/alarm receivers request prayer times. */
public final class ZikraApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();
    PrayerTimetables.get(this);
    PrayerTimetables.scheduleRefresh(this);
  }
}
