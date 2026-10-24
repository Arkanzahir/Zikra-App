package com.zikra.app.assistant;

import org.json.JSONException;
import org.json.JSONObject;

/** Untrusted suggestion. Only the review screen may ask the database to apply it. */
public final class TaskProposal {
  public final String id, kind, title, expectedTitle;
  public final long dueAt, taskId, expectedDueAt;

  public TaskProposal(String id, String kind, String title, long dueAt, long taskId,
      String expectedTitle, long expectedDueAt) {
    if (id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        || !("add_task".equals(kind) || "edit_task".equals(kind))
        || title == null || title.trim().isEmpty() || title.length() > 200
        || dueAt < 0 || dueAt > 4102444800000L
        || ("edit_task".equals(kind) && (taskId < 1 || expectedTitle == null
          || expectedTitle.trim().isEmpty() || expectedTitle.length() > 200 || expectedDueAt < 0)))
      throw new IllegalArgumentException("Usulan tugas tidak valid");
    this.id = id; this.kind = kind; this.title = title.trim(); this.dueAt = dueAt;
    this.taskId = taskId; this.expectedTitle = expectedTitle; this.expectedDueAt = expectedDueAt;
  }

  public static TaskProposal fromJson(JSONObject value) throws JSONException {
    if (value == null) throw new JSONException("Missing proposal");
    for (java.util.Iterator<String> keys = value.keys(); keys.hasNext(); ) {
      String key = keys.next();
      if (!java.util.Arrays.asList("id", "kind", "title", "dueAt", "taskId", "expectedTitle", "expectedDueAt").contains(key))
        throw new JSONException("Unknown proposal field");
    }
    return new TaskProposal(value.getString("id"), value.getString("kind"), value.getString("title"),
        value.getLong("dueAt"), value.optLong("taskId", 0), value.optString("expectedTitle", ""),
        value.optLong("expectedDueAt", 0));
  }

  public TaskProposal reviewed(String newTitle, long newDue) {
    return new TaskProposal(id, kind, newTitle, newDue, taskId, expectedTitle, expectedDueAt);
  }
}
