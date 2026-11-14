package com.zikra.app.prayer;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** The operating system may defer refresh; cached dates and alarm delivery remain offline. */
public final class PrayerRefreshJob extends JobService {
  @Override public boolean onStartJob(JobParameters params) {
    if (com.zikra.app.migration.LocalMigration.blocked()) return false;
    PrayerTimetables.get(this).refresh(false, () -> jobFinished(params, false));
    return true;
  }
  @Override public boolean onStopJob(JobParameters params) {
    if (com.zikra.app.migration.LocalMigration.blocked()) return false;
    PrayerTimetables.get(this).cancel();
    return true;
  }
}
