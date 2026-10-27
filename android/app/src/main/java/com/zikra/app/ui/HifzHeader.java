package com.zikra.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.zikra.app.R;

/** Visual presentation only: all completion and target actions remain owned by StudyActivity. */
public final class HifzHeader {
  private final Context context;
  private final View root;
  private final LinearLayout emptyPanel;
  private final TextView empty;
  private final TextView detail;
  private final Button add;

  public HifzHeader(Context context, View root, Runnable addTarget) {
    this.context = context;
    this.root = root;
    recolor(root);
    ((TextView) root.findViewById(R.id.study_heading)).setText(R.string.refined_hifz_today);
    ((TextView) root.findViewById(R.id.study_heading)).setTextSize(24);
    ((TextView) root.findViewById(R.id.study_date)).setTextSize(13);
    root.findViewById(R.id.study_scope_hint).setVisibility(View.GONE);
    ((TextView) root.findViewById(R.id.study_section)).setText(R.string.refined_hifz_targets);
    View metric = root.findViewById(R.id.studyTaskMetric);
    metric.setBackgroundResource(R.drawable.refined_mint_card);
    metric.setPadding(dp(16), dp(16), dp(16), dp(16));
    ProgressBar progress = root.findViewById(R.id.study_progress);
    progress.setProgressTintList(ColorStateList.valueOf(context.getColor(R.color.refined_ink)));
    progress.setProgressBackgroundTintList(
        ColorStateList.valueOf(context.getColor(R.color.refined_track)));
    ((TextView) root.findViewById(R.id.studySummaryCaption)).setTextSize(12);

    ImageView metricIcon = firstImage(metric);
    if (metricIcon != null) {
      metricIcon.setImageResource(R.drawable.feature_icon_hifz);
      metricIcon.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.refined_ink)));
      metricIcon.setBackground(null);
      metricIcon.setPadding(0, 0, 0, 0);
    }
    root.findViewById(R.id.agenda_feature).setBackgroundResource(R.drawable.refined_card);
    ((TextView) root.findViewById(R.id.agenda_feature_check))
        .setTextColor(context.getColor(R.color.refined_ink));
    ((CheckBox) root.findViewById(R.id.agenda_feature_check))
        .setButtonTintList(ColorStateList.valueOf(context.getColor(R.color.refined_ink)));
    ((ImageButton) root.findViewById(R.id.agenda_feature_more))
        .setImageTintList(ColorStateList.valueOf(context.getColor(R.color.refined_ink)));
    root.findViewById(R.id.agenda_feature_action)
        .setBackgroundResource(R.drawable.refined_mint_card);
    ((TextView) root.findViewById(R.id.studyFaithJournal)).setText(R.string.refined_hifz_notes);
    ((TextView) root.findViewById(R.id.studyFaithJournal))
        .setTextColor(context.getColor(R.color.refined_ink));
    root.findViewById(R.id.studyFaithJournal).setBackground(null);
    if (root instanceof LinearLayout) {
      LinearLayout host = (LinearLayout) root;
      View journal = root.findViewById(R.id.studyFaithJournal);
      host.removeView(journal);
      host.addView(
          journal,
          host.indexOfChild(root.findViewById(R.id.study_summary)) + 1,
          new LinearLayout.LayoutParams(-1, -2));
    }

    empty = root.findViewById(R.id.study_empty);
    ViewGroup parent = (ViewGroup) empty.getParent();
    int index = parent.indexOfChild(empty);
    parent.removeView(empty);
    emptyPanel = new LinearLayout(context);
    emptyPanel.setOrientation(LinearLayout.VERTICAL);
    emptyPanel.setGravity(Gravity.CENTER_HORIZONTAL);
    emptyPanel.setPadding(dp(16), dp(26), dp(16), dp(20));
    parent.addView(emptyPanel, index, new LinearLayout.LayoutParams(-1, -2));
    ImageView symbol = new ImageView(context);
    symbol.setImageResource(R.drawable.feature_icon_hifz);
    symbol.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.refined_ink)));
    symbol.setPadding(dp(17), dp(17), dp(17), dp(17));
    symbol.setBackgroundResource(R.drawable.refined_mint_card);
    symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    emptyPanel.addView(symbol, new LinearLayout.LayoutParams(dp(76), dp(80)));
    empty.setBackground(null);
    empty.setTextSize(20);
    empty.setGravity(Gravity.CENTER);
    empty.setPadding(0, dp(20), 0, dp(10));
    emptyPanel.addView(empty, new LinearLayout.LayoutParams(-1, -2));
    detail =
        RefinedUi.text(
            context,
            context.getString(R.string.refined_hifz_empty_detail),
            13,
            R.color.refined_muted,
            false);
    detail.setGravity(Gravity.CENTER);
    detail.setLineSpacing(dp(3), 1);
    emptyPanel.addView(detail, new LinearLayout.LayoutParams(-1, -2));
    add = new Button(context);
    add.setId(R.id.hifzEmptyAdd);
    add.setText(R.string.study_hifz_add);
    add.setTextSize(14);
    add.setTextColor(context.getColor(R.color.refined_on_primary));
    add.setAllCaps(false);
    add.setMinimumHeight(dp(48));
    add.setBackgroundResource(R.drawable.refined_primary_button);
    add.setPadding(dp(20), dp(12), dp(20), dp(12));
    LinearLayout.LayoutParams button = new LinearLayout.LayoutParams(-1, -2);
    button.topMargin = dp(22);
    emptyPanel.addView(add, button);
    add.setOnClickListener(v -> addTarget.run());
  }

  public void show(int completed, int total, boolean pending) {
    root.findViewById(R.id.study_summary).setVisibility(total == 0 ? View.GONE : View.VISIBLE);
    ((TextView) root.findViewById(R.id.study_progress_label))
        .setText(context.getString(R.string.refined_hifz_count, completed, total));
    emptyPanel.setVisibility(pending ? View.GONE : View.VISIBLE);
    empty.setVisibility(pending ? View.GONE : View.VISIBLE);
    if (total == 0) empty.setText(R.string.refined_hifz_empty_title);
    detail.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    add.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
  }

  private int dp(float value) {
    return RefinedUi.dp(context, value);
  }

  private void recolor(View view) {
    if (view instanceof TextView)
      ((TextView) view).setTextColor(context.getColor(R.color.refined_ink));
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
        recolor(((ViewGroup) view).getChildAt(i));
  }

  private ImageView firstImage(View view) {
    if (view instanceof ImageView) return (ImageView) view;
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        ImageView found = firstImage(((ViewGroup) view).getChildAt(i));
        if (found != null) return found;
      }
    return null;
  }
}
