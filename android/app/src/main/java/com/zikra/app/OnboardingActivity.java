package com.zikra.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zikra.app.discovery.DiscoveryUi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class OnboardingActivity extends BaseActivity {
  public static final String[] INTERESTS = {
    "Pemrograman", "Sains", "Desain", "Bahasa", "Bisnis", "Agama", "Hafalan", "Produktivitas"
  };
  private static final String[] LEVELS = {"SMP", "SMA / SMK", "Mahasiswa"};
  private static final int[] LEVEL_IDS = {
    R.id.onboardLevelSmp, R.id.onboardLevelSma, R.id.onboardLevelUniversity
  };
  private int step;
  private String level = "";
  private final Set<String> interests = new HashSet<>();
  private final ArrayList<Button> interestButtons = new ArrayList<>();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    SharedPreferences preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    if (state != null) {
      step = Math.max(0, Math.min(2, state.getInt("step")));
      level = state.getString("education_level", "");
      ArrayList<String> saved = state.getStringArrayList("interests");
      if (saved != null) interests.addAll(saved);
    } else {
      if (getIntent().getBooleanExtra("edit", false)) step = 1;
      level = preferences.getString("education_level", "");
      interests.addAll(preferences.getStringSet("learning_interests", new HashSet<>()));
    }
    if (!java.util.Arrays.asList(LEVELS).contains(level)) level = "";
    Set<String> valid = new HashSet<>();
    for (String interest : INTERESTS) {
      if (interests.contains(interest) && valid.size() < 3) valid.add(interest);
    }
    interests.clear();
    interests.addAll(valid);
    setContentView(R.layout.activity_onboarding);
    setupScreen("Selamat datang di Zikra");
    findViewById(R.id.onboardBack).setOnClickListener(v -> back());
    findViewById(R.id.onboardSkip).setOnClickListener(v -> complete());
    findViewById(R.id.onboardContinue)
        .setOnClickListener(
            v -> {
              if (step == 1 && level.isEmpty()) return;
              if (step < 2) {
                step++;
                render();
              } else complete();
            });
    for (int i = 0; i < LEVELS.length; i++) {
      final String choice = LEVELS[i];
      findViewById(LEVEL_IDS[i])
          .setOnClickListener(
              v -> {
                level = choice;
                renderLevels();
              });
    }
    GridLayout grid = findViewById(R.id.onboardInterests);
    for (String interest : INTERESTS) {
      Button button =
          (Button) getLayoutInflater().inflate(R.layout.onboarding_interest_item, grid, false);
      button.setText(interest);
      button.setOnClickListener(
          v -> {
            if (interests.contains(interest)) interests.remove(interest);
            else if (interests.size() < 3) interests.add(interest);
            else toast("Maksimal 3 minat. Hapus satu pilihan untuk menggantinya.");
            renderInterests();
          });
      interestButtons.add(button);
      grid.addView(button);
    }
    render();
  }

  private void render() {
    boolean welcome = step == 0;
    int foreground = Color.parseColor(welcome ? "#C4D8D0" : "#587668");
    int background = getColor(welcome ? R.color.pine : R.color.ivory);
    findViewById(R.id.onboardRoot).setBackgroundColor(background);
    findViewById(R.id.onboardWelcome).setVisibility(welcome ? View.VISIBLE : View.GONE);
    findViewById(R.id.onboardIdentity).setVisibility(step == 1 ? View.VISIBLE : View.GONE);
    findViewById(R.id.onboardTopics).setVisibility(step == 2 ? View.VISIBLE : View.GONE);
    findViewById(R.id.onboardBack).setVisibility(welcome ? View.INVISIBLE : View.VISIBLE);
    ((android.widget.ImageButton) findViewById(R.id.onboardBack))
        .setColorFilter(getColor(R.color.pine));
    ((TextView) findViewById(R.id.onboardWordmark))
        .setTextColor(getColor(welcome ? R.color.white : R.color.pine));
    ((TextView) findViewById(R.id.onboardSkip)).setTextColor(foreground);
    ((TextView) findViewById(R.id.onboardFooter)).setTextColor(foreground);
    ((Button) findViewById(R.id.onboardContinue))
        .setText(welcome ? "Mulai perjalanan" : step == 1 ? "Lanjutkan" : "Mulai bersama Zikra");
    TextView progress = findViewById(R.id.onboardStep);
    progress.setText(step == 0 ? "●  ━  ○  ━  ○" : step == 1 ? "○  ━  ●  ━  ○" : "○  ━  ○  ━  ●");
    progress.setTextColor(foreground);
    getWindow().setStatusBarColor(background);
    getWindow().setNavigationBarColor(background);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            welcome
                ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    renderLevels();
    renderInterests();
    ((ScrollView) findViewById(R.id.onboardScroll)).scrollTo(0, 0);
  }

  private void renderLevels() {
    for (int i = 0; i < LEVEL_IDS.length; i++) {
      View card = findViewById(LEVEL_IDS[i]);
      boolean selected = LEVELS[i].equals(level);
      card.setSelected(selected);
      card.setContentDescription(LEVELS[i] + (selected ? ", dipilih" : ", belum dipilih"));
      card.setBackground(
          DiscoveryUi.outlined(
              this, selected ? "#E6F3EB" : "#F3F6F1", selected ? "#57927E" : "#E3EBE4", 20));
    }
    Button action = findViewById(R.id.onboardContinue);
    boolean enabled = step != 1 || !level.isEmpty();
    action.setEnabled(enabled);
    action.setTextColor(Color.parseColor(enabled ? "#FFFFFF" : "#75857C"));
  }

  private void renderInterests() {
    for (Button button : interestButtons) {
      boolean selected = interests.contains(button.getText().toString());
      button.setSelected(selected);
      button.setBackground(
          DiscoveryUi.outlined(
              this, selected ? "#15332F" : "#F0F4EF", selected ? "#15332F" : "#E0E8E0", 28));
      button.setTextColor(Color.parseColor(selected ? "#FFFFFF" : "#315849"));
      button.setContentDescription(button.getText() + (selected ? ", dipilih" : ", belum dipilih"));
    }
    ((TextView) findViewById(R.id.onboardInterestCount))
        .setText(interests.size() + " dari 3 minat dipilih");
  }

  private void complete() {
    SharedPreferences.Editor editor =
        getSharedPreferences("zikra_preferences", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_complete", true)
            .putStringSet("learning_interests", new HashSet<>(interests));
    if (!level.isEmpty())
      editor.putString("education_level", level).putString("user_role", "Student");
    editor.apply();
    if (getIntent().getBooleanExtra("edit", false)) {
      finish();
      return;
    }
    startActivity(
        new Intent(this, HomeActivity.class)
            .addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    finish();
  }

  private void back() {
    if (step > 0) {
      step--;
      render();
    } else super.onBackPressed();
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onBackPressed() {
    back();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putInt("step", step);
    state.putString("education_level", level);
    state.putStringArrayList("interests", new ArrayList<>(interests));
    super.onSaveInstanceState(state);
  }
}
