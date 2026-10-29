package com.zikra.app.assistant;

import com.zikra.app.data.WeeklyClass;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** One seven-day snapshot. Private room, teacher, credits and reminder settings never leave the store. */
public final class AssistantClassContext {
  public static final int LIMIT = 10;
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT);
  private AssistantClassContext() {}

  public static JSONObject snapshot(List<WeeklyClass> classes, LocalDate from, ZoneId zone) throws JSONException {
    List<WeeklyClass> ordered = new ArrayList<>(classes);
    int today = from.getDayOfWeek().getValue();
    ordered.sort(Comparator.comparingInt((WeeklyClass item) -> (item.weekday - today + 7) % 7)
        .thenComparingInt(item -> item.startMinute).thenComparingLong(item -> item.id));
    JSONArray items = new JSONArray();
    for (WeeklyClass item : ordered) {
      if (items.length() == LIMIT) break;
      LocalDate date = from.plusDays((item.weekday - today + 7) % 7);
      items.put(new JSONObject().put("date", date.toString()).put("title", item.title)
          .put("start", TIME.format(LocalTime.of(item.startMinute / 60, item.startMinute % 60)))
          .put("end", TIME.format(LocalTime.of(item.endMinute / 60, item.endMinute % 60))));
    }
    return new JSONObject().put("fromDate", from.toString()).put("throughDate", from.plusDays(6).toString())
        .put("zone", zone.getId()).put("items", items).put("omitted", Math.max(0, classes.size() - LIMIT));
  }
}
