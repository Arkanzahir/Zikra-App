package com.zikra.app.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;
import com.zikra.app.R;

/**
 * One XML header with legacy ID aliases, preserving existing navigation and accessibility tests.
 */
public final class FeatureHeader extends LinearLayout {
  private final ImageButton back;
  private final TextView heading;
  private final TextView caption;
  private final ImageButton action;

  public View back() {
    return back;
  }

  public View action() {
    return action;
  }

  public TextView heading() {
    return heading;
  }

  /** Match a screen surface without changing headers on other features. */
  public void setSurfaceColor(int color) {
    setBackgroundColor(color);
    getChildAt(0).setBackgroundColor(color);
  }

  public FeatureHeader(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_feature_header, this, true);
    back = findViewById(R.id.featureBack);
    heading = findViewById(R.id.featureTitle);
    caption = findViewById(R.id.featureSubtitle);
    action = findViewById(R.id.featureAction);
    if (android.os.Build.VERSION.SDK_INT >= 28) heading.setAccessibilityHeading(true);
  }

  public void bindIds(int back, int title, int subtitle, int action) {
    this.back.setId(back);
    this.back.setImageDrawable(getContext().getDrawable(R.drawable.ic_back).mutate());
    this.back.setImageTintList(android.content.res.ColorStateList.valueOf(getContext().getColor(R.color.text)));
    heading.setId(title);
    caption.setId(subtitle);
    if (action != 0) {
      this.action.setId(action);
      this.action.setVisibility(VISIBLE);
      caption.setVisibility(VISIBLE);
      heading.setText("Zikra Protector");
    }
  }

  /** Bind one shared header while preserving each screen's established view IDs. */
  public void bind(
      int backId,
      int titleId,
      int subtitleId,
      int actionId,
      CharSequence title,
      CharSequence subtitle,
      int actionDrawable,
      CharSequence actionDescription) {
    if (backId != 0) back.setId(backId);
    if (titleId != 0) heading.setId(titleId);
    if (subtitleId != 0) caption.setId(subtitleId);
    if (actionId != 0) action.setId(actionId);
    heading.setText(title);
    caption.setText(subtitle);
    caption.setVisibility(android.text.TextUtils.isEmpty(subtitle) ? GONE : VISIBLE);
    action.setVisibility(actionDrawable == 0 ? GONE : VISIBLE);
    if (actionDrawable != 0) action.setImageResource(actionDrawable);
    action.setContentDescription(actionDescription);
  }
}
