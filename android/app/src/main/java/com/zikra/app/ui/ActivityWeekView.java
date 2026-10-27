package com.zikra.app.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.R;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/** Read-only activity days from explicit records; viewing a page never adds a record. */
public final class ActivityWeekView extends LinearLayout {
  public ActivityWeekView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(HORIZONTAL);
    setBaselineAligned(false);
  }

  public void setDays(Set<LocalDate> days, LocalDate today) {
    removeAllViews();
    String[] labels = getResources().getStringArray(R.array.refined_weekdays_short);
    DateTimeFormatter full =
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"));
    for (int offset = 6; offset >= 0; offset--) {
      LocalDate day = today.minusDays(offset);
      boolean recorded = days.contains(day);
      LinearLayout column = new LinearLayout(getContext());
      column.setOrientation(VERTICAL);
      column.setGravity(Gravity.CENTER);
      LayoutParams slot = new LayoutParams(0, -2, 1);
      addView(column, slot);
      TextView label = new TextView(getContext());
      label.setGravity(Gravity.CENTER);
      label.setText(labels[day.getDayOfWeek().getValue() - 1]);
      label.setTextSize(11);
      label.setTextColor(getContext().getColor(R.color.refined_muted));
      column.addView(label, new LayoutParams(-1, -2));
      TextView value = new TextView(getContext());
      value.setGravity(Gravity.CENTER);
      value.setText(recorded ? "✓" : Integer.toString(day.getDayOfMonth()));
      value.setTextSize(14);
      value.setMaxLines(1);
      value.setAutoSizeTextTypeUniformWithConfiguration(
          10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
      value.setTextColor(
          getContext().getColor(recorded ? R.color.refined_on_primary : R.color.refined_ink));
      value.setBackground(
          RefinedUi.surface(
              getContext(),
              recorded ? R.color.refined_primary : R.color.refined_surface,
              !recorded,
              10));
      LayoutParams size =
          new LayoutParams(
              -1,
              RefinedUi.dp(
                  getContext(),
                  36 * Math.max(1f, Math.min(1.5f, getResources().getConfiguration().fontScale))));
      size.setMargins(
          RefinedUi.dp(getContext(), 2),
          RefinedUi.dp(getContext(), 7),
          RefinedUi.dp(getContext(), 2),
          0);
      column.addView(value, size);
      column.setContentDescription(
          getContext()
              .getString(
                  recorded
                      ? R.string.refined_profile_day_recorded
                      : R.string.refined_profile_day_empty,
                  day.format(full)));
      column.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
      label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      value.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
  }
}
