package com.zikra.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import com.zikra.app.data.ZikraDbHelper;

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
    setupScreen("Profil Saya");
    db = new ZikraDbHelper(this);
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    rows = findViewById(R.id.profileRows);
    findViewById(R.id.profileProgressCard).setOnClickListener(v -> open(StudyActivity.class));
    NavDock.attach(this, (FrameLayout) findViewById(R.id.profileRoot), "profile");
  }

  @Override
  protected void onResume() {
    super.onResume();
    render();
  }

  private void render() {
    if (db == null) return;
    ZikraDbHelper.Progress progress = db.getProgress();
    ((TextView) findViewById(R.id.profileProgressPercent)).setText(progress.percent + "%");
    ((ProgressBar) findViewById(R.id.profileProgress)).setProgress(progress.percent);
    ((TextView) findViewById(R.id.profileProgressCaption))
        .setText(progress.completed + " dari " + progress.total + " checklist selesai");
    ((TextView) findViewById(R.id.profileRoleCaption)).setText("Profil lokal · Sukolilo, Surabaya");
    rows.removeAllViews();
    row(
        "Bacaan terakhir",
        preferences.getString("last_surah", "Al-Fatihah")
            + " · Ayat "
            + preferences.getInt("last_ayah", 1),
        R.drawable.ic_book,
        () -> open(QuranActivity.class));
    row(
        "Hadis & simpanan",
        "Kutipan pilihan dan bookmark",
        R.drawable.ic_heart,
        () -> open(HadithActivity.class));
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
        "Pengingat sholat",
        "Preferensi jadwal di perangkat",
        R.drawable.ic_bell,
        () -> open(PrayerActivity.class));
    row("Pengaturan", "Goyangan bantuan cepat", R.drawable.ic_compass, this::settings);
    row(
        "Bantuan & keselamatan",
        "Protector BMKG dan darurat",
        R.drawable.ic_shield,
        () -> open(ProtectorActivity.class));
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
    String role = preferences.getString("user_role", "Student");
    if ("Teacher".equals(role)) return "Pengajar";
    if ("Parents".equals(role)) return "Orang tua";
    String level = preferences.getString("education_level", "");
    return level.isEmpty() ? "Pelajar" : level;
  }

  private void row(String title, String subtitle, int icon, Runnable action) {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(15), 0, dp(15));
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
    heading.setTextSize(13);
    heading.setTextColor(0xFF15332F);
    copy.addView(heading);
    TextView detail = new TextView(this);
    detail.setText(subtitle);
    detail.setTextSize(10);
    detail.setTextColor(0xFF8B9B91);
    detail.setPadding(0, dp(5), 0, 0);
    copy.addView(detail);
    TextView arrow = new TextView(this);
    arrow.setText("›");
    arrow.setTextSize(22);
    arrow.setTextColor(0xFF9CAD9F);
    row.addView(arrow);
    row.setOnClickListener(v -> action.run());
    rows.addView(row);
  }

  private void settings() {
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(24), dp(12), dp(24), dp(12));
    Switch shake = new Switch(this);
    shake.setText("Goyangkan untuk bantuan cepat");
    shake.setTextSize(13);
    shake.setChecked(preferences.getBoolean("shake_enabled", true));
    shake.setOnCheckedChangeListener(
        (button, on) -> preferences.edit().putBoolean("shake_enabled", on).apply());
    content.addView(shake);
    TextView explanation = new TextView(this);
    explanation.setText(
        "Diterapkan saat halaman dibuka kembali. Akses bantuan juga tersedia melalui Protector"
            + " BMKG.");
    explanation.setTextSize(12);
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
