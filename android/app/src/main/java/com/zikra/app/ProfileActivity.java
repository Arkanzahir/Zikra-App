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
    getWindow().setStatusBarColor(0xFFFFFFFF);
    getWindow().setNavigationBarColor(0xFFF8F9F4);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    com.zikra.app.ui.DailyUi.chrome(this, R.color.daily_mint);
    db = new ZikraDbHelper(this);
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    rows = findViewById(R.id.profileRows);
    findViewById(R.id.profileProgressCard).setOnClickListener(v -> open(StudyActivity.class));
    findViewById(R.id.dailyProfileEdit).setOnClickListener(v -> editName());
    findViewById(R.id.dailyProfileQuran).setOnClickListener(v -> open(QuranActivity.class));
    findViewById(R.id.dailyProfileHadith).setOnClickListener(v -> open(HadithActivity.class));
    findViewById(R.id.dailyProfileDua).setOnClickListener(v -> open(DuaActivity.class));
    NavDock.attach(this, (FrameLayout) findViewById(R.id.profileRoot), "profile");
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
    ZikraDbHelper.Progress progress = db.getProgress(ZikraDbHelper.CATEGORY_STUDY);
    ((TextView) findViewById(R.id.profileProgressPercent))
        .setText(progress.completed + " / " + progress.total);
    ((ProgressBar) findViewById(R.id.profileProgress)).setProgress(progress.percent);
    ((TextView) findViewById(R.id.profileProgressCaption))
        .setText(
            progress.total == 0
                ? "Belum ada tugas akademik. Tambahkan di Study Hub."
                : progress.completed + " dari " + progress.total + " tugas akademik selesai");
    ((TextView) findViewById(R.id.profileRoleCaption)).setText(roleLabel());
    findViewById(R.id.profileRoleCaption)
        .setVisibility(
            preferences.getString("user_role", "").isEmpty()
                    && preferences.getString("education_level", "").isEmpty()
                ? View.GONE
                : View.VISIBLE);
    rows.removeAllViews();
    com.zikra.app.faith.FaithSummary faith = com.zikra.app.faith.FaithSummary.load(this, db);
    row(getString(R.string.faith_title), faith.available
        ? getString(R.string.faith_summary, faith.streak, faith.week)
          + (faith.plan == null ? "" : " · " + faith.read + "/6.236 ayat")
        : getString(R.string.faith_unavailable), R.drawable.ic_book, () -> open(FaithActivity.class));
    row(getString(R.string.study_level_choose),com.zikra.app.study.EducationLevel.label(this),
        R.drawable.ic_graduation,()->profileDialog=com.zikra.app.study.EducationLevel.choose(this,this::render));
    row(
        "Pengingat sholat",
        "Waktu dan preferensi notifikasi",
        R.drawable.ic_bell,
        () -> open(PrayerActivity.class));
    row(
        "Lokasi",
        com.zikra.app.location.LocationStore.current(this).caption(),
        R.drawable.ic_compass,
        () -> open(LocationActivity.class));
    row(
        "Bantuan & keselamatan",
        "Protector dan informasi darurat",
        R.drawable.ic_shield,
        () -> open(ProtectorActivity.class));
    row(
        "Bacaan terakhir",
        preferences.getString("last_surah", "Al-Fatihah")
            + " · Ayat "
            + preferences.getInt("last_ayah", 1),
        R.drawable.ic_book,
        () -> open(QuranActivity.class));
    row(
        "Masjid terdekat",
        "Temukan tempat singgah di sekitarmu",
        R.drawable.ic_compass,
        () -> open(NearbyMosquesActivity.class));
    row(
        "Kalender Hijriah",
        "Tanggal dan hari istimewa",
        R.drawable.ic_moon,
        () -> open(CalendarActivity.class));
    row(
        "Kenali Zikra",
        "Belajar, ibadah dan hafalan sehari-hari",
        R.drawable.ic_graduation,
        () -> startActivity(new Intent(this, OnboardingActivity.class).putExtra("edit", true)));
    row(
        "Pengaturan",
        "Bantuan cepat dengan gerakan perangkat",
        R.drawable.ic_compass,
        this::settings);
    row(
        "Tentang Zikra",
        "Teman ibadah dan belajar",
        R.drawable.ic_spark,
        () ->
            profileDialog =
                new AlertDialog.Builder(this)
                    .setView(getLayoutInflater().inflate(R.layout.dialog_about_zikra, null))
                    .setPositiveButton("Mengerti", null)
                    .show());
  }

  private String roleLabel() {
    String role = preferences.getString("user_role", "");
    if ("Teacher".equals(role)) return "Pengajar";
    if ("Parents".equals(role)) return "Orang tua";
    String level = preferences.getString("education_level", "");
    if (!level.isEmpty()) return level + " · profil lokal";
    return "Student".equals(role)
        ? "Pelajar · profil lokal"
        : "Profil lokal · tersimpan di perangkat";
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
    glyph.setColorFilter(0xFF659B86);
    row.addView(glyph, new LinearLayout.LayoutParams(dp(22), dp(22)));
    LinearLayout copy = new LinearLayout(this);
    copy.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
    p.setMargins(dp(14), 0, dp(8), 0);
    row.addView(copy, p);
    TextView heading = new TextView(this);
    heading.setText(title);
    heading.setTextSize(14);
    heading.setTextColor(0xFF15332F);
    copy.addView(heading);
    TextView detail = new TextView(this);
    detail.setText(subtitle);
    detail.setTextSize(12);
    detail.setTextColor(0xFF52665F);
    detail.setPadding(0, dp(5), 0, 0);
    copy.addView(detail);
    TextView arrow = new TextView(this);
    arrow.setText("›");
    arrow.setTextSize(22);
    arrow.setTextColor(0xFF9CAD9F);
    row.addView(arrow);
    row.setOnClickListener(v -> action.run());
    rows.addView(row);
    View divider = new View(this);
    divider.setBackgroundColor(getColor(R.color.daily_line));
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
    explanation.setTextColor(0xFF52665F);
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
