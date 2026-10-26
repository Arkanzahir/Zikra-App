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
  public final String subject;
  public final int priority;
  public final boolean advanceReminder;

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
    this(id, title, category, completed, hifz, dueAt, reminder, "", 1, false);
  }

  public Task(long id, String title, String category, boolean completed, HifzTarget hifz,
      long dueAt, boolean reminder, String subject, int priority, boolean advanceReminder) {
    if (priority < 0 || priority > 2) throw new IllegalArgumentException("Invalid priority");
    this.id = id;
    this.title = title;
    this.category = category;
    this.completed = completed;
    this.hifz = hifz;
    this.dueAt = dueAt;
    this.reminder = reminder;
    this.subject = WeeklyClass.label(subject, 100, false);
    this.priority = priority;
    this.advanceReminder = advanceReminder;
  }
}
