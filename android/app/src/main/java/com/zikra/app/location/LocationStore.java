package com.zikra.app.location;

import android.content.Context;
import android.content.SharedPreferences;
import com.zikra.app.reminders.PrayerReminders;
import java.time.Instant;
import java.time.LocalDate;

/** One device-local location choice shared across features; never starts GPS in the background. */
public final class LocationStore {
  public static final String PREFS = "zikra_location";

  private LocationStore() {}

  public static LocalDate today(Context context) {
    return localDate(context, Instant.now());
  }

  /** Use the selected city for daily records as well as the prayer/sky date. */
  public static LocalDate localDate(Context context, Instant instant) {
    return instant.atZone(current(context).zone).toLocalDate();
  }

  public static LocationPoint current(Context context) {
    SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    if (!p.contains("latitude")) return LocationPoint.INITIAL;
    try {
      return new LocationPoint(
          Double.longBitsToDouble(p.getLong("latitude", 0)),
          Double.longBitsToDouble(p.getLong("longitude", 0)),
          p.getString("zone", ""),
          p.getString("label", ""),
          p.getString("source", "manual"),
          p.getLong("selected_at", 0),
          p.getFloat("accuracy", -1));
    } catch (RuntimeException invalid) {
      return LocationPoint.INITIAL;
    }
  }

  public static void save(Context context, LocationPoint point) {
    context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putLong("latitude", Double.doubleToLongBits(point.latitude))
        .putLong("longitude", Double.doubleToLongBits(point.longitude))
        .putString("zone", point.zone.getId())
        .putString("label", point.label)
        .putString("source", point.source)
        .putLong("selected_at", point.selectedAt)
        .putFloat("accuracy", point.accuracy)
        .apply();
    PrayerReminders.sync(context);
  }
}
