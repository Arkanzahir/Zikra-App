package com.zikra.app.data;

/** An immutable snapshot of one task saved in the daily checklist. */
public final class Task {
  public final long id;
  public final String title;
  public final String category;
  public final boolean completed;

  public Task(long id, String title, String category, boolean completed) {
    this.id = id;
    this.title = title;
    this.category = category;
    this.completed = completed;
  }
}
