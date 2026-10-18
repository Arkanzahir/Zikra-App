package com.zikra.app;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zikra.app.discovery.GardenSceneView;

/** Three stories, without requesting an education level or interests. */
public final class OnboardingActivity extends BaseActivity {
  private static final String[] TITLES = {
    "Belajar fokus.\nIbadah terjaga.",
    "Satu niat baik.\nSetiap hari.",
    "Ayat demi ayat.\nLebih dekat."
  };
  private static final String[] CAPTIONS = {
    "Ruang untuk tugas, waktu sholat,\ndan bacaan yang menemani harimu.",
    "Tata kegiatan dan ibadahmu.\nMulai dari satu hal yang bisa kamu tuntaskan.",
    "Pilih target hafalan dan lanjutkan bacaan.\nSedikit demi sedikit, dengan ritmemu."
  };
  private static final String[] EYEBROWS = {
    "SELAMAT DATANG DI ZIKRA", "RUANG UNTUK BERTUMBUH", "JAGA YANG BERARTI"
  };
  private int step;
  private float downX, downY;
  private GardenSceneView garden;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    step = state == null ? 0 : Math.max(0, Math.min(2, state.getInt("step")));
    setContentView(R.layout.activity_onboarding);
    setupScreen("Selamat datang di Zikra");
    getWindow().setNavigationBarColor(getColor(R.color.pine));
    getWindow().getDecorView().setSystemUiVisibility(0);
    garden = findViewById(R.id.onboardGarden);
    findViewById(R.id.onboardContinue)
        .setOnClickListener(
            v -> {
              if (step < 2) move(1);
              else complete();
            });
    findViewById(R.id.onboardBack).setOnClickListener(v -> move(-1));
    findViewById(R.id.onboardSkip).setOnClickListener(v -> complete());
    render(false);
  }

  private void move(int delta) {
    int target = Math.max(0, Math.min(2, step + delta));
    if (target != step) {
      step = target;
      render(true);
    }
  }

  private void render(boolean animate) {
    ((TextView) findViewById(R.id.onboardTitle)).setText(TITLES[step]);
    ((TextView) findViewById(R.id.onboardEyebrow)).setText(EYEBROWS[step]);
    ((TextView) findViewById(R.id.onboardDescription)).setText(CAPTIONS[step]);
    ((TextView) findViewById(R.id.onboardStep)).setText("Langkah " + (step + 1) + " dari 3");
    ((TextView) findViewById(R.id.onboardContinue))
        .setText(step == 2 ? "Mulai harimu" : step == 1 ? "Lanjut" : "Kenalan dulu");
    findViewById(R.id.onboardBack).setVisibility(step == 0 ? View.GONE : View.VISIBLE);
    int[] dots = {R.id.onboardDot0, R.id.onboardDot1, R.id.onboardDot2};
    for (int i = 0; i < 3; i++)
      findViewById(dots[i]).setBackgroundColor(i == step ? getColor(R.color.camel) : 0xFF49685D);
    garden.setStory(step);
    ((ScrollView) findViewById(R.id.onboardScroll)).scrollTo(0, 0);
    View story = findViewById(R.id.onboardStory);
    story.animate().cancel();
    story.setAlpha(1);
    story.setTranslationY(0);
    if (animate && ValueAnimator.areAnimatorsEnabled()) {
      story.setAlpha(.35f);
      story.setTranslationY(dp(10));
      story.animate().alpha(1).translationY(0).setDuration(280).start();
      story.announceForAccessibility(TITLES[step].replace('\n', ' '));
    }
  }

  private void complete() {
    getSharedPreferences("zikra_preferences", MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putInt("onboarding_story_version", 2)
        .apply();
    startActivity(
        new Intent(this, HomeActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    finish();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
      downX = e.getX();
      downY = e.getY();
    } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
      float dx = e.getX() - downX, dy = e.getY() - downY;
      if (Math.abs(dx) > dp(72) && Math.abs(dx) > Math.abs(dy) * 1.5f) move(dx < 0 ? 1 : -1);
    }
    return super.dispatchTouchEvent(e);
  }

  @Override
  protected boolean usesShakeShortcut() {
    return false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (garden != null) garden.setRunning(true);
  }

  @Override
  protected void onPause() {
    if (garden != null) garden.setRunning(false);
    super.onPause();
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
