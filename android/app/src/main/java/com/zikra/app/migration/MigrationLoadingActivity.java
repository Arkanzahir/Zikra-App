package com.zikra.app.migration;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import com.zikra.app.R;

/** Recovery owns this process until exit, keeping old preference caches out of normal screens. */
public final class MigrationLoadingActivity extends Activity {
  private boolean running;
  private TextView status;
  private Button action;
  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setGravity(Gravity.CENTER);
    int pad = Math.round(24 * getResources().getDisplayMetrics().density); body.setPadding(pad, pad, pad, pad);
    body.setBackgroundColor(getColor(com.zikra.app.R.color.refined_background));
    TextView title = new TextView(this); title.setText(R.string.migration_recover_title); title.setTextSize(24);
    title.setTextColor(getColor(com.zikra.app.R.color.refined_ink)); body.addView(title);
    status = new TextView(this); status.setTextSize(16); status.setPadding(0, pad, 0, pad);
    status.setTextColor(getColor(com.zikra.app.R.color.refined_ink)); body.addView(status);
    action = new Button(this); body.addView(action); setContentView(body); recover();
  }
  private void recover() {
    if (running) return; running = true; action.setEnabled(false); action.setText(R.string.migration_processing);
    status.setText(R.string.migration_recover_working);
    new Thread(() -> {
      try {
        String result = LocalMigration.recover(getApplicationContext());
        runOnUiThread(() -> {
          if (isDestroyed()) return; running = false;
          status.setText("restored".equals(result)
              ? R.string.migration_recovered
              : R.string.migration_unchanged);
          action.setEnabled(true); action.setText(R.string.migration_close); action.setOnClickListener(v -> closeProcess());
        });
      } catch (Exception failure) {
        runOnUiThread(() -> {
          if (isDestroyed()) return; running = false;
          status.setText(R.string.migration_recover_failed);
          action.setEnabled(true); action.setText(R.string.migration_retry); action.setOnClickListener(v -> recover());
        });
      }
    }, "zikra-local-recovery").start();
  }
  private void closeProcess() { finishAffinity(); android.os.Process.killProcess(android.os.Process.myPid()); }
  @Override public void onBackPressed() { if (!running) closeProcess(); }
}
