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

/** Three static illustrated stories. No animation player, sensor or education/interests form. */
public final class OnboardingActivity extends BaseActivity {
  private String[] titles, captions, eyebrows;
  private static final String[] SCENES = {
    "Pelajar berkacamata menulis di meja belajar.",
    "Perempuan bermukena duduk di atas sajadah.",
    "Laki-laki berpeci membaca Quran dengan rehal."
  };
  private static final int[] ARTWORK = {
    R.drawable.onboarding_study_static,
    R.drawable.onboarding_prayer_static,
    R.drawable.onboarding_quran_static
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
    ((TextView) findViewById(R.id.onboardContinue)).setText(step == 2 ? "Mulai harimu" : "Lanjut");
    // Keep indicators in the same position across all three stories.
    findViewById(R.id.onboardBack).setVisibility(step == 0 ? View.INVISIBLE : View.VISIBLE);
    ImageView artwork = findViewById(R.id.onboardIllustration);
    artwork.setContentDescription(SCENES[step]);
    // Decode the current illustration only, at the resolution needed by this display.
    // Keep all details in the image; ImageView fitCenter handles the responsive viewport.
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
    int wall = image.getPixel(image.getWidth() / 2, 0);
    findViewById(R.id.onboardHeader).setBackgroundColor(wall);
    getWindow().setStatusBarColor(wall);
    if (android.os.Build.VERSION.SDK_INT >= 28)
      findViewById(R.id.onboardTitle).setAccessibilityHeading(true);
    ((ScrollView) findViewById(R.id.onboardScroll)).scrollTo(0, 0);
  }

  private void complete() {
    getSharedPreferences("zikra_preferences", MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putInt("onboarding_story_version", 5)
        .apply();
    startActivity(
        new Intent(this, HomeActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    finish();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
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
