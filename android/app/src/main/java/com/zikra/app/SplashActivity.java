package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/** The same XML wordmark is used on splash, onboarding, Home and About. */
public final class SplashActivity extends Activity {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable enter =
      () -> {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
      };

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_splash);
    getWindow().getDecorView().setSystemUiVisibility(0);
  }

  @Override
  protected void onResume() {
    super.onResume();
    handler.postDelayed(enter, android.animation.ValueAnimator.areAnimatorsEnabled() ? 300 : 0);
  }

  @Override
  protected void onPause() {
    handler.removeCallbacks(enter);
    super.onPause();
  }
}
