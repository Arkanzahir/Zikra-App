package com.zikra.app.data;

/** An immutable snapshot of one persistent plan or daily ibadah item. */
public final class Task {
  public final long id;
  public final String title;
  public final String category;
  public final boolean completed;
  public final HifzTarget hifz;
  public final long dueAt;
  public final boolean reminder;

  public Task(long id, String title, String category, boolean completed) {
    this(id, title, category, completed, null);
  }

  public Task(long id, String title, String category, boolean completed, HifzTarget hifz) {
    this(id, title, category, completed, hifz, 0, false);
  }

  public Task(
      long id,
      String title,
      String category,
      boolean completed,
      HifzTarget hifz,
      long dueAt,
      boolean reminder) {
    this.id = id;
    this.title = title;
    this.category = category;
    this.completed = completed;
    this.hifz = hifz;
    this.dueAt = dueAt;
    this.reminder = reminder;
  }
}
