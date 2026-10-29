package com.zikra.app.assistant;

import android.content.Context;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.data.Task;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.data.WeeklyClassStore;
import com.zikra.app.prayer.PrayerMonth;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import java.time.LocalDate;
import org.json.*;

/** A per-send snapshot. No field is selected by default or silently added later. */
public final class AssistantSelection {
  private AssistantSelection() {}

  public static JSONArray conversation(JSONArray stored, boolean history) throws JSONException {
    JSONArray selected = new JSONArray();
    for (int i = 0; i < stored.length(); i++) {
      JSONObject row = stored.optJSONObject(i);
      if (row != null && ((history && "online".equals(row.optString("mode"))) || i == stored.length() - 1))
        selected.put(row);
    }
    return OnlineAssistantClient.context(selected);
  }

  public static JSONObject build(Context context, JSONArray stored, boolean name, boolean tasks,
      boolean prayer, boolean history, boolean proposals, JSONObject verse) throws JSONException {
    return build(context, stored, name, tasks, prayer, history, proposals, verse, false);
  }

  public static JSONObject build(Context context, JSONArray stored, boolean name, boolean tasks,
      boolean prayer, boolean history, boolean proposals, JSONObject verse, boolean classes) throws JSONException {
    JSONObject selected = new JSONObject();
    if (name) {
      String value = context.getSharedPreferences("zikra_preferences", 0).getString("profile_display_name", "").trim();
      if (!value.isEmpty()) selected.put("name", value);
    }
    if (tasks) {
      JSONArray rows = new JSONArray();
      try (ZikraDbHelper db = new ZikraDbHelper(context)) {
        for (Task task : db.getTasks()) {
          if (!ZikraDbHelper.CATEGORY_STUDY.equals(task.category)) continue;
          rows.put(new JSONObject().put("id", Long.toString(task.id)).put("title", task.title)
              .put("dueAt", task.dueAt).put("completed", task.completed));
          if (rows.length() == 10) break;
        }
      }
      selected.put("tasks", rows);
    }
    if (prayer) {
      LocationPoint point = LocationStore.current(context);
      LocalDate day = LocalDate.now(point.zone);
      PrayerSchedule.Day schedule = PrayerSchedule.forDate(day, point);
      JSONArray items = new JSONArray();
      for (int i = 0; i < 6; i++) if (PrayerSchedule.isPrayer(i) && schedule.at(i) != null)
        items.put(new JSONObject().put("name", PrayerSchedule.NAMES[i]).put("time", schedule.time(i)));
      selected.put("prayer", new JSONObject().put("date", day.toString()).put("zone", point.zone.getId())
          .put("source", PrayerMonth.SOURCE.equals(schedule.source) ? "kemenag" : "local").put("items", items));
    }
    if (classes) {
      LocationPoint point = LocationStore.current(context);
      try (WeeklyClassStore store = new WeeklyClassStore(context)) {
        selected.put("classes", AssistantClassContext.snapshot(store.all(), LocalDate.now(point.zone), point.zone));
      }
    }
    JSONObject last = stored.optJSONObject(stored.length() - 1);
    JSONObject request = new JSONObject().put("messages", conversation(stored, history)).put("context", selected)
        .put("requestId", last == null ? "" : last.optString("request_id"))
        .put("features", new JSONObject().put("taskProposals", proposals));
    if (verse != null) request.put("verse", verse);
    return bound(request);
  }

  public static JSONObject bound(JSONObject request) throws JSONException {
    JSONArray messages = request.getJSONArray("messages");
    while (request.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 48 * 1024
        && messages.length() > 1) messages.remove(0);
    if (request.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 48 * 1024)
      throw new JSONException("Selected request exceeds byte budget");
    return request;
  }

  public static String preview(JSONObject request) {
    StringBuilder text = new StringBuilder();
    JSONObject context = request.optJSONObject("context");
    if (context != null) {
      if (context.has("name")) text.append("Nama: ").append(context.optString("name")).append('\n');
      JSONArray tasks = context.optJSONArray("tasks");
      if (tasks != null) for (int i = 0; i < tasks.length(); i++) {
        JSONObject task = tasks.optJSONObject(i);
        text.append("Tugas: ").append(task.optString("title"))
            .append(task.optBoolean("completed") ? " · selesai" : " · belum selesai");
        long due = task.optLong("dueAt");
        if (due > 0) text.append(" · ").append(java.time.Instant.ofEpochMilli(due));
        text.append('\n');
      }
      JSONObject prayer = context.optJSONObject("prayer");
      if (prayer != null) {
        text.append("Salat: ").append(prayer.optString("date")).append(" · ").append(prayer.optString("zone")).append('\n');
        text.append("Sumber: ").append("kemenag".equals(prayer.optString("source"))
            ? "jadwal Kemenag tersimpan" : "hitungan lokal cadangan").append('\n');
        JSONArray items = prayer.optJSONArray("items");
        if (items != null) for (int i = 0; i < items.length(); i++) {
          JSONObject item = items.optJSONObject(i);
          text.append(item.optString("name")).append(' ').append(item.optString("time")).append('\n');
        }
      }
      JSONObject classes = context.optJSONObject("classes");
      if (classes != null) {
        text.append("Jadwal kelas: ").append(classes.optString("fromDate")).append(" — ")
            .append(classes.optString("throughDate")).append(" · ").append(classes.optString("zone")).append('\n');
        JSONArray items = classes.optJSONArray("items");
        if (items != null && items.length() == 0) text.append("Belum ada kelas tersimpan.\n");
        if (items != null) for (int i = 0; i < items.length(); i++) {
          JSONObject item = items.optJSONObject(i);
          text.append(item.optString("date")).append(" · ").append(item.optString("start")).append('–')
              .append(item.optString("end")).append(" · ").append(item.optString("title")).append('\n');
        }
        if (classes.optInt("omitted") > 0) text.append(classes.optInt("omitted"))
            .append(" kelas lain tidak disertakan (batas 10).\n");
      }
    }
    JSONObject verse = request.optJSONObject("verse");
    if (verse != null) text.append("Tadabbur: surah ").append(verse.optInt("chapter")).append(" ayat ").append(verse.optInt("number")).append('\n');
    JSONArray messages = request.optJSONArray("messages");
    if (messages != null) for (int i = 0; i < messages.length(); i++) {
      JSONObject row = messages.optJSONObject(i);
      text.append("user".equals(row.optString("role")) ? "Pesanmu: " : "Balasan terdahulu: ")
          .append(row.optString("content")).append('\n');
    }
    return text.toString().trim();
  }
}
