package com.zikra.app.ui;

import android.content.Context;
import android.widget.ScrollView;

/** Leave room for native dialog title/actions, including at large system font sizes. */
public final class AssistantReviewScrollView extends ScrollView {
  public AssistantReviewScrollView(Context context) { super(context); }

  @Override protected void onMeasure(int widthSpec, int heightSpec) {
    int cap=Math.round(getResources().getDisplayMetrics().heightPixels*.45f);
    int available=MeasureSpec.getMode(heightSpec)==MeasureSpec.UNSPECIFIED?cap:MeasureSpec.getSize(heightSpec);
    super.onMeasure(widthSpec,MeasureSpec.makeMeasureSpec(Math.min(cap,available),MeasureSpec.AT_MOST));
  }
}
