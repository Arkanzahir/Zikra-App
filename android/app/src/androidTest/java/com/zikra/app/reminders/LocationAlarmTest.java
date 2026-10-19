package com.zikra.app.reminders;

import android.content.Intent;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A delivered alarm from the old selected city must not create a notification for the new one. */
@SuppressWarnings("deprecation")
public final class LocationAlarmTest extends InstrumentationTestCase {
  public void testStaleCityAlarmDoesNotDeliver() {
    android.content.Context context = getInstrumentation().getTargetContext();
    String[] names = {LocationStore.PREFS, "zikra_preferences"};
    Map<String, Map<String, ?>> saved = new HashMap<>();
    for (String name : names)
      saved.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    try {
      context
          .getSharedPreferences("zikra_preferences", 0)
          .edit()
          .putBoolean("reminder_enabled", true)
          .remove("prayer_delivered_2")
          .commit();
      LocationStore.save(context, LocationPoint.INITIAL);
      Intent stale = PrayerReminders.alarmIntent(context, 2, System.currentTimeMillis() - 1000);
      LocationStore.save(
          context,
          new LocationPoint(-2.5337, 140.7181, "Asia/Jayapura", "Jayapura", "manual", 0, -1));
      PrayerReminders.deliver(context, stale);
      assertFalse(
          context.getSharedPreferences("zikra_preferences", 0).contains("prayer_delivered_2"));
    } finally {
      for (String name : names) {
        SharedPreferences.Editor e = context.getSharedPreferences(name, 0).edit().clear();
        for (Map.Entry<String, ?> item : saved.get(name).entrySet()) {
          Object v = item.getValue();
          String k = item.getKey();
          if (v instanceof String) e.putString(k, (String) v);
          else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
          else if (v instanceof Long) e.putLong(k, (Long) v);
          else if (v instanceof Integer) e.putInt(k, (Integer) v);
          else if (v instanceof Float) e.putFloat(k, (Float) v);
          else if (v instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) v));
        }
        e.commit();
      }
      PrayerReminders.sync(context);
    }
  }
}
