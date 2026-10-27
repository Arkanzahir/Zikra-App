package com.zikra.app.faith;

import android.content.Context;
import com.zikra.app.data.ZikraDbHelper;
import java.time.LocalDate;
import java.util.*;

/** Shared Home/Profile/journal snapshot, explicitly unavailable on storage failure. */
public final class FaithSummary {
  public boolean available;
  public int streak, week, read, hifzDone, hifzTotal;
  public FaithStore.Plan plan;
  public Set<LocalDate> days = new HashSet<>();

  public static FaithSummary load(Context context, ZikraDbHelper db) {
    try (FaithStore store = new FaithStore(context)) {
      return load(store, db);
    } catch (android.database.sqlite.SQLiteException
        | IllegalArgumentException
        | java.time.DateTimeException error) {
      return new FaithSummary();
    }
  }

  public static FaithSummary load(FaithStore store, ZikraDbHelper db) {
    FaithSummary summary = new FaithSummary();
    summary.days.addAll(store.activeDays());
    LocalDate today = store.today();
    for (Map.Entry<LocalDate, ZikraDbHelper.Progress> day : db.worshipHistory().entrySet())
      if (day.getValue().completed > 0 && !day.getKey().isAfter(today))
        summary.days.add(day.getKey());
    summary.streak = FaithProgress.streak(summary.days, today);
    summary.week = FaithProgress.week(summary.days, today);
    summary.plan = store.plan();
    summary.read = summary.plan == null ? 0 : store.coverage(summary.plan.id).cardinality();
    ZikraDbHelper.Progress hifz = db.getProgress(ZikraDbHelper.CATEGORY_HIFZ);
    summary.hifzDone = hifz.completed;
    summary.hifzTotal = hifz.total;
    summary.available = true;
    return summary;
  }
}
