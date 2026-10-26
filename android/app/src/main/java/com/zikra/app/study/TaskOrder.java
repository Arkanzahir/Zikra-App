package com.zikra.app.study;

import com.zikra.app.data.Task;
import java.util.Comparator;

public final class TaskOrder {
  private TaskOrder() {}
  public static final Comparator<Task> ACADEMIC = Comparator
      .comparingLong((Task t) -> t.dueAt == 0 ? Long.MAX_VALUE : t.dueAt)
      .thenComparing(Comparator.comparingInt((Task t) -> t.priority).reversed())
      .thenComparingLong(t -> t.id);
}
