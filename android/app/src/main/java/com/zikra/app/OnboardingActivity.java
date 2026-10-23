package com.zikra.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Three still illustrations with optional touch/scroll depth. No character animation or sensors.
 */
public final class OnboardingActivity extends BaseActivity {
  private com.zikra.app.ui.OnboardingParallax parallax;
  private String[] titles, captions, eyebrows;
  private static final String[] SCENES = {
    "Masjid Indonesia beratap tumpang dengan halaman yang tenang.",
    "Pelajar berkacamata membaca Quran di atas rehal.",
    "Pelajar yang sama menulis di meja belajar."
  };
  private static final int[] ARTWORK = {
    R.drawable.daily_onboarding_welcome,
    R.drawable.daily_onboarding_read,
    R.drawable.daily_onboarding_study
  };
  private int step;
  private float downX, downY;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    step = state == null ? 0 : Math.max(0, Math.min(2, state.getInt("step")));
    setContentView(R.layout.activity_onboarding);
    titles = getResources().getStringArray(R.array.onboarding_titles);
    captions = getResources().getStringArray(R.array.onboarding_captions);
    eyebrows = getResources().getStringArray(R.array.onboarding_eyebrows);
    setupScreen("Selamat datang di Zikra");
    getWindow().setStatusBarColor(getColor(R.color.onboarding_wall));
    getWindow().setNavigationBarColor(getColor(R.color.onboarding_paper));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    findViewById(R.id.onboardContinue)
        .setOnClickListener(
            v -> {
              if (step < 2) move(1);
              else complete();
            });
    findViewById(R.id.onboardBack).setOnClickListener(v -> move(-1));
    findViewById(R.id.onboardSkip).setOnClickListener(v -> complete());
    parallax =
        new com.zikra.app.ui.OnboardingParallax(
            findViewById(R.id.onboardIllustration), findViewById(R.id.onboardCopy));
    findViewById(R.id.onboardScroll)
        .setOnScrollChangeListener((v, x, y, oldX, oldY) -> parallax.onScroll(y));
    render();
  }

  private void move(int delta) {
    int target = Math.max(0, Math.min(2, step + delta));
    if (target == step) return;
    step = target;
    render();
    findViewById(R.id.onboardStory).announceForAccessibility(titles[step]);
  }

  private void render() {
    ((TextView) findViewById(R.id.onboardEyebrow)).setText(eyebrows[step]);
    ((TextView) findViewById(R.id.onboardTitle)).setText(titles[step]);
    ((TextView) findViewById(R.id.onboardDescription)).setText(captions[step]);
    TextView progress = findViewById(R.id.onboardStep);
    progress.setText(String.format(java.util.Locale.ROOT, "%02d / 03", step + 1));
    progress.setContentDescription("Langkah " + (step + 1) + " dari 3");
    ((TextView) findViewById(R.id.onboardContinue))
        .setText(step == 2 ? getString(R.string.daily_onboard_finish) : "Lanjut");
    ((TextView) findViewById(R.id.dailyOnboardFeature))
        .setText(
            new int[] {
                  R.string.daily_onboard_feature_welcome,
                  R.string.daily_onboard_feature_quran,
                  R.string.daily_onboard_feature_study
                }
                [step]);
    findViewById(R.id.onboardContinue)
        .setBackgroundResource(step == 2 ? R.drawable.daily_gold_card : R.drawable.daily_pine_card);
    ((TextView) findViewById(R.id.onboardContinue))
        .setTextColor(getColor(step == 2 ? R.color.daily_ink : R.color.white));
    // Keep indicators in the same position across all three stories.
    findViewById(R.id.onboardBack).setVisibility(step == 0 ? View.INVISIBLE : View.VISIBLE);
    ImageView artwork = findViewById(R.id.onboardIllustration);
    artwork.setContentDescription(SCENES[step]);
    // Decode the current illustration only, at the resolution needed by this display.
    // The controller fits the image and adds a small overscan only while motion is enabled.
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeResource(getResources(), ARTWORK[step], options);
    int wanted = Math.max(1, getResources().getDisplayMetrics().widthPixels);
    options.inSampleSize = 1;
    while (options.outWidth / (options.inSampleSize * 2) >= wanted) options.inSampleSize *= 2;
    options.inJustDecodeBounds = false;
    options.inScaled = false;
    Bitmap image = BitmapFactory.decodeResource(getResources(), ARTWORK[step], options);
    artwork.setImageBitmap(image);
    int wall = getColor(R.color.daily_paper);
    findViewById(R.id.onboardHeader).setBackgroundColor(wall);
    getWindow().setStatusBarColor(wall);
    if (android.os.Build.VERSION.SDK_INT >= 28)
      findViewById(R.id.onboardTitle).setAccessibilityHeading(true);
    ((ScrollView) findViewById(R.id.onboardScroll)).scrollTo(0, 0);
    parallax.reset();
  }

  private void complete() {
    getSharedPreferences("zikra_preferences", MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putInt("onboarding_story_version", 6)
        .apply();
    startActivity(
        new Intent(this, HomeActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    finish();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (parallax != null) parallax.onTouch(event);
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      downX = event.getX();
      downY = event.getY();
    } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      float dx = event.getX() - downX, dy = event.getY() - downY;
      if (Math.abs(dx) > dp(72) && Math.abs(dx) > Math.abs(dy) * 1.5f) {
        // Consume the swipe so a wide Continue button cannot also advance a second step.
        MotionEvent cancel = MotionEvent.obtain(event);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(cancel);
        cancel.recycle();
        move(dx < 0 ? 1 : -1);
        return true;
      }
    }
    return super.dispatchTouchEvent(event);
  }

  @Override
  protected boolean usesShakeShortcut() {
    return false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (parallax != null) parallax.start();
  }

  @Override
  protected void onPause() {
    if (parallax != null) parallax.stop();
    super.onPause();
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    if (parallax != null) parallax.refresh();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putInt("step", step);
    super.onSaveInstanceState(state);
  }

  @Override
  public void onBackPressed() {
    if (step > 0) move(-1);
    else super.onBackPressed();
  }
}
