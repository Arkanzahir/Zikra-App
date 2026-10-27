package com.zikra.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.zikra.app.R;
import com.zikra.app.data.Task;
import java.util.List;

/** Each mark belongs to its actual task, rather than an assumed order of completed prayers. */
public final class WorshipSummaryView extends LinearLayout {
  public WorshipSummaryView(Context context, AttributeSet attributes) {
    super(context, attributes);
    setOrientation(HORIZONTAL);
    setBaselineAligned(false);
  }

  public void setTasks(List<Task> tasks) {
    removeAllViews();
    setVisibility(tasks.isEmpty() ? GONE : VISIBLE);
    for (int i = 0; i < Math.min(tasks.size(), 5); i++) {
      Task task = tasks.get(i);
      LinearLayout column = new LinearLayout(getContext());
      column.setOrientation(VERTICAL);
      column.setGravity(Gravity.CENTER_HORIZONTAL);
      column.setPadding(RefinedUi.dp(getContext(), 2), 0, RefinedUi.dp(getContext(), 2), 0);
      addView(column, new LayoutParams(0, -2, 1));
      ImageView state = new ImageView(getContext());
      state.setImageResource(task.completed ? R.drawable.ic_check : R.drawable.refined_minus);
      state.setImageTintList(
          ColorStateList.valueOf(
              getContext()
                  .getColor(task.completed ? R.color.refined_on_primary : R.color.refined_ink)));
      state.setPadding(
          RefinedUi.dp(getContext(), 7),
          RefinedUi.dp(getContext(), 7),
          RefinedUi.dp(getContext(), 7),
          RefinedUi.dp(getContext(), 7));
      state.setBackground(
          RefinedUi.surface(
              getContext(),
              task.completed ? R.color.refined_primary : R.color.refined_surface,
              !task.completed,
              10));
      state.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      column.addView(
          state, new LayoutParams(RefinedUi.dp(getContext(), 32), RefinedUi.dp(getContext(), 32)));
      TextView name =
          RefinedUi.text(
              getContext(),
              task.title.replaceFirst("^(Sholat|Salat) ", ""),
              11,
              R.color.refined_ink,
              false);
      name.setGravity(Gravity.CENTER);
      LayoutParams copy = new LayoutParams(-1, -2);
      copy.topMargin = RefinedUi.dp(getContext(), 6);
      column.addView(name, copy);
      name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      column.setContentDescription(
          getContext()
              .getString(
                  R.string.refined_worship_item,
                  task.title,
                  getContext()
                      .getString(
                          task.completed
                              ? R.string.refined_hifz_done
                              : R.string.refined_hifz_pending)));
      column.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
      column.setTag("worship:" + task.id + ":" + task.completed);
    }
  }
}
