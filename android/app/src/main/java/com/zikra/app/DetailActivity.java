package com.zikra.app;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** XML-based shared secondary screen shell. */
public abstract class DetailActivity extends BaseActivity {
  protected LinearLayout content;
  protected Button action;

  protected void detail(String title, String headline, String description, int icon) {
    setContentView(R.layout.activity_detail);
    setupScreen(title);
    ((TextView) findViewById(R.id.detailTitle)).setText(title);
    ((TextView) findViewById(R.id.detailHeadline)).setText(headline);
    ((TextView) findViewById(R.id.detailDescription)).setText(description);
    ((ImageView) findViewById(R.id.detailIcon)).setImageResource(icon);
    content = findViewById(R.id.detailContent);
    action = findViewById(R.id.detailAction);
  }

  protected TextView line(String text) {
    TextView view = new TextView(this);
    view.setText(text);
    view.setTextSize(14);
    view.setTextColor(getColor(R.color.text));
    view.setPadding(0, dp(10), 0, dp(10));
    view.setLineSpacing(dp(5), 1);
    content.addView(view);
    return view;
  }
}
