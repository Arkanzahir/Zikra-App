package com.zikra.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import com.zikra.app.discovery.DiscoveryUi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Three-step, completely native onboarding. Choices survive Activity recreation. */
public final class OnboardingActivity extends BaseActivity {
  public static final String[] INTERESTS = {
    "Pemrograman", "Sains", "Desain", "Bahasa", "Bisnis", "Agama"
  };
  private int step;
  private String role = "Student";
  private final Set<String> interests = new HashSet<>();
  private final ArrayList<Button> interestButtons = new ArrayList<>();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    if (state != null) {
      step = state.getInt("step");
      role = state.getString("role", "Student");
      ArrayList<String> saved = state.getStringArrayList("interests");
      if (saved != null) interests.addAll(saved);
    } else {
      if (getIntent().getBooleanExtra("edit", false)) step = 1;
      role =
          getSharedPreferences("zikra_preferences", MODE_PRIVATE).getString("user_role", "Student");
      interests.addAll(
          getSharedPreferences("zikra_preferences", MODE_PRIVATE)
              .getStringSet("learning_interests", new HashSet<>()));
    }
    setContentView(R.layout.activity_onboarding);
    setupScreen("Selamat datang di Zikra");
    findViewById(R.id.onboardBack).setOnClickListener(v -> back());
    findViewById(R.id.onboardSkip).setOnClickListener(v -> complete());
    findViewById(R.id.onboardContinue)
        .setOnClickListener(
            v -> {
              if (step < 2) {
                step++;
                render();
              } else complete();
            });
    findViewById(R.id.onboardRoleStudent).setOnClickListener(v -> selectRole("Student"));
    findViewById(R.id.onboardRoleTeacher).setOnClickListener(v -> selectRole("Teacher"));
    findViewById(R.id.onboardRoleParents).setOnClickListener(v -> selectRole("Parents"));
    GridLayout grid = findViewById(R.id.onboardInterests);
    for (String interest : INTERESTS) {
      Button button = new Button(this);
      button.setText(interest);
      button.setTextSize(13);
      button.setAllCaps(false);
      GridLayout.LayoutParams params = new GridLayout.LayoutParams();
      params.width = 0;
      params.height = dp(58);
      params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
      params.setMargins(dp(5), dp(6), dp(5), dp(6));
      button.setLayoutParams(params);
      button.setOnClickListener(
          v -> {
            if (interests.contains(interest)) interests.remove(interest);
            else interests.add(interest);
            renderInterests();
          });
      interestButtons.add(button);
      grid.addView(button);
    }
    render();
  }

  private void selectRole(String value) {
    role = value;
    renderRoles();
  }

  private void render() {
    boolean welcome = step == 0;
    findViewById(R.id.onboardRoot)
        .setBackgroundColor(Color.parseColor(welcome ? "#15332F" : "#FFFEFB"));
    findViewById(R.id.onboardWelcome).setVisibility(step == 0 ? View.VISIBLE : View.GONE);
    findViewById(R.id.onboardIdentity).setVisibility(step == 1 ? View.VISIBLE : View.GONE);
    findViewById(R.id.onboardTopics).setVisibility(step == 2 ? View.VISIBLE : View.GONE);
    findViewById(R.id.onboardBack).setVisibility(welcome ? View.INVISIBLE : View.VISIBLE);
    ((android.widget.ImageButton) findViewById(R.id.onboardBack))
        .setColorFilter(Color.parseColor(welcome ? "#FFFFFF" : "#15332F"));
    ((TextView) findViewById(R.id.onboardWordmark))
        .setTextColor(Color.parseColor(welcome ? "#FFFFFF" : "#15332F"));
    ((TextView) findViewById(R.id.onboardSkip))
        .setTextColor(Color.parseColor(welcome ? "#B5CEC4" : "#57927E"));
    ((TextView) findViewById(R.id.onboardContinue))
        .setText(
            step == 0 ? "Mulai perjalanan" : step == 1 ? "Lanjutkan" : "Selesai, masuk ke Zikra");
    ((TextView) findViewById(R.id.onboardStep))
        .setText(step == 0 ? "●  ○  ○" : step == 1 ? "○  ●  ○" : "○  ○  ●");
    getWindow().setStatusBarColor(Color.parseColor(welcome ? "#15332F" : "#FFFEFB"));
    getWindow().setNavigationBarColor(Color.parseColor(welcome ? "#15332F" : "#FFFEFB"));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            welcome
                ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    renderRoles();
    renderInterests();
  }

  private void renderRoles() {
    int[] ids = {R.id.onboardRoleStudent, R.id.onboardRoleTeacher, R.id.onboardRoleParents};
    String[] roles = {"Student", "Teacher", "Parents"};
    for (int i = 0; i < ids.length; i++) {
      View card = findViewById(ids[i]);
      boolean selected = roles[i].equals(role);
      card.setSelected(selected);
      card.setBackground(
          DiscoveryUi.outlined(
              this, selected ? "#E8F4EC" : "#F5F8F6", selected ? "#57927E" : "#E5EBE7", 20));
    }
  }

  private void renderInterests() {
    for (Button button : interestButtons) {
      boolean selected = interests.contains(button.getText().toString());
      button.setSelected(selected);
      button.setBackground(DiscoveryUi.background(this, selected ? "#57927E" : "#F0F4EF", 30));
      button.setTextColor(Color.parseColor(selected ? "#FFFFFF" : "#315849"));
      button.setContentDescription(button.getText() + (selected ? ", dipilih" : ", belum dipilih"));
    }
  }

  private void complete() {
    getSharedPreferences("zikra_preferences", MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putString("user_role", role)
        .putStringSet("learning_interests", new HashSet<>(interests))
        .apply();
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
    state.putString("role", role);
    state.putStringArrayList("interests", new ArrayList<>(interests));
    super.onSaveInstanceState(state);
  }
}
