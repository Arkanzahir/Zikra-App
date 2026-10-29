package com.zikra.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.ui.FeatureHeader;

/** Local profile and entry points to faith, learning and device preferences. */
public final class ProfileActivity extends BaseActivity {
  private ZikraDbHelper db;
  private SharedPreferences preferences;
  private LinearLayout rows;
  private AlertDialog profileDialog;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_profile);
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(R.id.toolbarBack, R.id.detailTitle, R.id.featureSubtitle, 0, "Profil", null, 0, null);
    setupScreen("Profil");
    com.zikra.app.ui.RefinedUi.chrome(this, findViewById(R.id.utilitiesHeader), true);
    db = new ZikraDbHelper(this);
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    rows = findViewById(R.id.profileRows);
    findViewById(R.id.profileProgressCard).setOnClickListener(v -> open(StudyActivity.class));
    findViewById(R.id.dailyProfileEdit).setOnClickListener(v -> editName());
    findViewById(R.id.profileReadingCard)
        .setOnClickListener(
            v ->
                startActivity(
                    new Intent(this, QuranActivity.class)
                        .putExtra(QuranActivity.EXTRA_RESUME_READING, true)));
    findViewById(R.id.profileWeekCard).setOnClickListener(v -> open(FaithActivity.class));
    findViewById(R.id.profileHifzCard)
        .setOnClickListener(
            v ->
                startActivity(
                    new Intent(this, StudyActivity.class)
                        .putExtra("category", ZikraDbHelper.CATEGORY_HIFZ)));

    findViewById(R.id.dailyProfileQuran).setOnClickListener(v -> open(QuranActivity.class));
    findViewById(R.id.dailyProfileHadith).setOnClickListener(v -> open(HadithActivity.class));
    findViewById(R.id.dailyProfileDua).setOnClickListener(v -> open(DuaActivity.class));
    NavDock.attach(this, (FrameLayout) findViewById(R.id.profileRoot), "profile");
    NavDock.setNightMode(findViewById(R.id.profileRoot), com.zikra.app.ui.RefinedUi.dark(this));
  }

  @Override
  protected void onResume() {
    super.onResume();
    render();
  }

  private void render() {
    if (db == null) return;
    String name =
        preferences.getString(
            "profile_display_name", getString(R.string.daily_profile_default_name));
    if (name.trim().isEmpty()) name = getString(R.string.daily_profile_default_name);
    ((TextView) findViewById(R.id.profileDisplayName)).setText(name);
    ((TextView) findViewById(R.id.dailyProfileInitial))
        .setText(
            name.equals(getString(R.string.daily_profile_default_name))
                ? "Z"
                : name.substring(0, name.offsetByCodePoints(0, 1))
                    .toUpperCase(java.util.Locale.ROOT));
    ((TextView) findViewById(R.id.profileRoleCaption)).setText(roleLabel());
    boolean hasReading = preferences.contains("last_surah");
    ((TextView) findViewById(R.id.profileReadingLabel))
        .setText(
            hasReading ? R.string.refined_profile_reading : R.string.refined_profile_start_reading);
    ((TextView) findViewById(R.id.profileLastRead))
        .setText(
            getString(
                R.string.refined_reading_reference,
                preferences.getString("last_surah", "Al-Fatihah"),
                preferences.getInt("last_ayah", 1)));
    com.zikra.app.faith.FaithSummary faith = com.zikra.app.faith.FaithSummary.load(this, db);
    TextView week = findViewById(R.id.profileWeekCount);
    week.setText(
        !faith.available
            ? getString(R.string.faith_unavailable)
            : faith.week == 0
                ? getString(R.string.refined_profile_week_empty)
                : getString(R.string.refined_profile_week_count, faith.week));
    com.zikra.app.ui.ActivityWeekView days = findViewById(R.id.profileWeekDays);
    days.setVisibility(faith.available ? View.VISIBLE : View.GONE);
    if (faith.available) days.setDays(faith.days, com.zikra.app.location.LocationStore.today(this));
    ZikraDbHelper.Progress progress = db.getProgress(ZikraDbHelper.CATEGORY_STUDY);
    ((TextView) findViewById(R.id.profileProgressPercent))
        .setText(
            progress.total == 0
                ? getString(R.string.refined_profile_tasks)
                : getString(R.string.refined_count, progress.completed, progress.total));
    ((ProgressBar) findViewById(R.id.profileProgress)).setProgress(progress.percent);
    findViewById(R.id.profileProgress)
        .setVisibility(progress.total == 0 ? View.GONE : View.VISIBLE);
    ((TextView) findViewById(R.id.profileProgressCaption))
        .setText(
            progress.total == 0
                ? getString(R.string.refined_profile_no_tasks)
                : getString(
                    R.string.refined_profile_study_count, progress.completed, progress.total));
    ((TextView) findViewById(R.id.profileHifzCount))
        .setText(
            !faith.available
                ? getString(R.string.faith_unavailable)
                : faith.hifzTotal == 0
                    ? getString(R.string.refined_profile_hifz)
                    : getString(R.string.refined_count, faith.hifzDone, faith.hifzTotal));
    ((TextView) findViewById(R.id.profileHifzCaption))
        .setText(
            !faith.available
                ? getString(R.string.faith_unavailable)
                : faith.hifzTotal == 0
                    ? getString(R.string.refined_profile_no_hifz)
                    : getString(R.string.refined_hifz_count, faith.hifzDone, faith.hifzTotal));
    rows.removeAllViews();
    row(getString(R.string.app_appearance),
        com.zikra.app.ui.AppAppearance.label(this), R.drawable.ic_home_appearance,
        () -> profileDialog = com.zikra.app.ui.AppAppearance.choose(this));
    row(
        getString(R.string.study_level_choose),
        com.zikra.app.study.EducationLevel.label(this),
        R.drawable.feature_icon_study,
        () -> profileDialog = com.zikra.app.study.EducationLevel.choose(this, this::render));
    row(
        getString(R.string.refined_prayer_preferences),
        getString(R.string.refined_prayer_detail),
        R.drawable.ic_bell,
        () -> open(PrayerActivity.class));
    row(
        getString(R.string.refined_location),
        com.zikra.app.location.LocationStore.current(this).caption(),
        R.drawable.home_location_pin,
        () -> open(LocationActivity.class));
    row(
        getString(R.string.refined_calendar_title),
        getString(R.string.refined_calendar_detail),
        R.drawable.ai_balanced_calendar,
        () -> open(CalendarActivity.class));
    row(
        getString(R.string.refined_settings),
        getString(R.string.refined_settings_detail),
        R.drawable.study_scene_settings_icon,
        this::settings);
    TextView group = new TextView(this);
    group.setText(R.string.refined_profile_other);
    group.setTextSize(16);
    group.setTextColor(getColor(R.color.refined_ink));
    group.setTypeface(group.getTypeface(), android.graphics.Typeface.BOLD);
    group.setPadding(0, dp(22), 0, dp(8));
    if (android.os.Build.VERSION.SDK_INT >= 28) group.setAccessibilityHeading(true);
    rows.addView(group);
    row(
        getString(R.string.refined_safety),
        getString(R.string.refined_safety_detail),
        R.drawable.feature_icon_protector,
        () -> open(ProtectorActivity.class));
    row(
        getString(R.string.refined_mosques),
        getString(R.string.refined_mosques_detail),
        R.drawable.qibla_ic_mosque,
        () -> open(NearbyMosquesActivity.class));
    row(
        getString(R.string.refined_intro),
        getString(R.string.refined_intro_detail),
        R.drawable.qibla_ic_info,
        () -> startActivity(new Intent(this, OnboardingActivity.class).putExtra("edit", true)));
    row(
        getString(R.string.refined_about),
        getString(R.string.refined_about_detail),
        R.drawable.qibla_ic_info,
        () ->
            profileDialog =
                new AlertDialog.Builder(this)
                    .setView(getLayoutInflater().inflate(R.layout.dialog_about_zikra, null))
                    .setPositiveButton("Mengerti", null)
                    .show());
  }

  private String roleLabel() {
    String role = preferences.getString("user_role", "");
    if ("Teacher".equals(role)) return getString(R.string.refined_role_teacher);
    if ("Parents".equals(role)) return getString(R.string.refined_role_parent);
    String level = preferences.getString("education_level", "");
    if (!level.isEmpty()) return getString(R.string.refined_role_level, level);
    return "Student".equals(role)
        ? getString(R.string.refined_role_student)
        : getString(R.string.refined_role_local);
  }

  private void row(String title, String subtitle, int icon, Runnable action) {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(12), 0, dp(12));
    row.setMinimumHeight(dp(64));
    row.setClickable(true);
    row.setFocusable(true);
    row.setContentDescription(title + ". " + subtitle);
    row.setBackgroundResource(android.R.drawable.list_selector_background);
    ImageView glyph = new ImageView(this);
    glyph.setImageResource(icon);
    glyph.setColorFilter(getColor(R.color.refined_ink));
    row.addView(glyph, new LinearLayout.LayoutParams(dp(22), dp(22)));
    LinearLayout copy = new LinearLayout(this);
    copy.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
    p.setMargins(dp(14), 0, dp(8), 0);
    row.addView(copy, p);
    TextView heading = new TextView(this);
    heading.setText(title);
    heading.setTextSize(14);
    heading.setTextColor(getColor(R.color.refined_ink));
    copy.addView(heading);
    TextView detail = new TextView(this);
    detail.setText(subtitle);
    detail.setTextSize(12);
    detail.setTextColor(getColor(R.color.refined_muted));
    detail.setPadding(0, dp(5), 0, 0);
    copy.addView(detail);
    TextView arrow = new TextView(this);
    arrow.setText("›");
    arrow.setTextSize(22);
    arrow.setTextColor(getColor(R.color.refined_muted));
    row.addView(arrow);
    row.setOnClickListener(v -> action.run());
    rows.addView(row);
    View divider = new View(this);
    divider.setBackgroundColor(getColor(R.color.refined_line));
    rows.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
  }

  private void editName() {
    android.widget.EditText input = new android.widget.EditText(this);
    input.setSingleLine(true);
    input.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    input.setFilters(
        new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(32)});
    input.setHint(R.string.daily_profile_name_hint);
    input.setText(
        preferences.getString(
            "profile_display_name", getString(R.string.daily_profile_default_name)));
    LinearLayout body = new LinearLayout(this);
    body.setPadding(dp(24), dp(8), dp(24), dp(8));
    body.addView(input, new LinearLayout.LayoutParams(-1, -2));
    profileDialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.daily_profile_edit_title)
            .setView(body)
            .setNegativeButton(R.string.daily_cancel, null)
            .setPositiveButton(R.string.daily_save, null)
            .create();
    profileDialog.setOnShowListener(
        d ->
            profileDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      String name = input.getText().toString().trim();
                      if (name.isEmpty()) {
                        input.setError(getString(R.string.daily_profile_name_error));
                        return;
                      }
                      preferences.edit().putString("profile_display_name", name).apply();
                      profileDialog.dismiss();
                      render();
                    }));
    profileDialog.show();
  }

  private void settings() {
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(24), dp(12), dp(24), dp(12));
    Switch shake = new Switch(this);
    shake.setText("Goyangkan untuk bantuan cepat");
    shake.setTextSize(16);
    shake.setMinHeight(dp(48));
    shake.setChecked(preferences.getBoolean("shake_enabled", true));
    shake.setOnCheckedChangeListener(
        (button, on) -> preferences.edit().putBoolean("shake_enabled", on).apply());
    content.addView(shake);
    TextView explanation = new TextView(this);
    explanation.setText(
        "Diterapkan saat halaman dibuka kembali. Akses bantuan juga tersedia melalui Protector"
            + " BMKG.");
    explanation.setTextSize(14);
    explanation.setTextColor(getColor(R.color.refined_muted));
    explanation.setPadding(0, dp(16), 0, 0);
    content.addView(explanation);
    profileDialog =
        new AlertDialog.Builder(this)
            .setTitle("Pengaturan")
            .setView(content)
            .setPositiveButton("Selesai", null)
            .show();
  }

  @Override
  protected void onDestroy() {
    if (profileDialog != null) profileDialog.dismiss();
    if (db != null) db.close();
    super.onDestroy();
  }
}
