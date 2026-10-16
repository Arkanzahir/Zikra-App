package com.zikra.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

/** Native campus giving guide; payments remain with the mosque's official channels. */
public final class InfaqActivity extends BaseActivity {
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_infaq);
    setupScreen("Infaq Kampus");
    getWindow().setStatusBarColor(0xFFFCFDFB);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    findViewById(R.id.infaqMaps)
        .setOnClickListener(
            v ->
                external(
                    Intent.ACTION_VIEW,
                    "https://www.google.com/maps/dir/?api=1&destination=Masjid+Manarul+Ilmi+ITS+Surabaya&travelmode=walking"));
    NavDock.attach(this, (FrameLayout) findViewById(R.id.infaqRoot), "infaq");
  }
}
