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
  private final View back;

  public View back() {
    return back;
  }

  public FeatureHeader(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_feature_header, this, true);
    back = findViewById(R.id.featureBack);
  }

  public void bindIds(int back, int title, int subtitle, int action) {
    ImageButton backButton = (ImageButton) this.back;
    backButton.setId(back);
    backButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_back).mutate());
    backButton.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF15332F));
    TextView heading = findViewById(R.id.featureTitle);
    heading.setId(title);
    TextView caption = findViewById(R.id.featureSubtitle);
    caption.setId(subtitle);
    if (action != 0) {
      View refresh = findViewById(R.id.featureAction);
      refresh.setId(action);
      refresh.setVisibility(VISIBLE);
      caption.setVisibility(VISIBLE);
      heading.setText("Zikra Protector");
    }
  }
}
