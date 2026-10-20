package com.zikra.app;

import android.Manifest;
import android.app.AlertDialog;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.zikra.app.data.*;
import com.zikra.app.study.*;
import java.util.Locale;

/**
 * Native focus/break timer. Its deadline survives Activity/process recreation without a service.
 */
public final class FocusTimerActivity extends BaseActivity {
  private FocusClock clock;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private AlertDialog confirmation;
  private String taskTitle = "Satu langkah untuk belajarmu";
  private final Runnable tick =
      new Runnable() {
        public void run() {
          render();
          handler.postDelayed(this, 500);
        }
      };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_focus_timer);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.focusHeader))
        .bind(
            R.id.toolbarBack,
            R.id.featureTitle,
            R.id.featureSubtitle,
            0,
            "Fokus belajar",
            null,
            0,
            null);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.focusHeader))
        .setSurfaceColor(getColor(R.color.background));
    setupScreen("Fokus belajar");
    getWindow().setStatusBarColor(getColor(R.color.background));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    clock = FocusTimer.load(this);
    long task = getIntent().getLongExtra("task", -1);
    if (task != -1 && task != clock.taskId) {
      if (clock.running
          || clock.finished
          || clock.breakPhase
          || clock.remaining != clock.focusMinutes * 60000L) {
        confirmation =
            new AlertDialog.Builder(this)
                .setTitle("Ganti tugas fokus?")
                .setMessage("Timer yang tersimpan akan dimulai ulang untuk tugas pilihan.")
                .setNegativeButton("Lanjutkan timer", null)
                .setPositiveButton(
                    "Ganti tugas", (d, w) -> reset(clock.focusMinutes, clock.breakMinutes, task))
                .show();
      } else reset(clock.focusMinutes, clock.breakMinutes, task);
    }
    ((RadioGroup) findViewById(R.id.focusDuration))
        .check(clock.focusMinutes == 50 ? R.id.focusLong : R.id.focusShort);
    ((RadioGroup) findViewById(R.id.focusDuration))
        .setOnCheckedChangeListener(
            (g, id) -> {
              int focus = id == R.id.focusLong ? 50 : 25;
              if (clock.focusMinutes == focus) return;
              confirmation =
                  new AlertDialog.Builder(this)
                      .setTitle("Ubah durasi?")
                      .setMessage("Timer akan kembali ke awal sesi fokus.")
                      .setPositiveButton(
                          "Ubah", (d, w) -> reset(focus, focus == 50 ? 10 : 5, clock.taskId))
                      .setNegativeButton("Batal", (d, w) -> restoreChoice())
                      .setOnCancelListener(d -> restoreChoice())
                      .show();
            });
    findViewById(R.id.focusPlay)
        .setOnClickListener(
            v -> {
              if (clock.running) clock.pause(SystemClock.elapsedRealtime(), FocusTimer.boot(this));
              else {
                clock.start(SystemClock.elapsedRealtime(), FocusTimer.boot(this));
                if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED
                    && !getSharedPreferences(FocusTimer.PREFS, 0)
                        .getBoolean("notification_asked", false)) {
                  getSharedPreferences(FocusTimer.PREFS, 0)
                      .edit()
                      .putBoolean("notification_asked", true)
                      .apply();
                  requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 902);
                }
              }
              FocusTimer.schedule(this, clock);
              render();
            });
    findViewById(R.id.focusNotificationSettings)
        .setOnClickListener(
            v -> {
              try {
                startActivity(
                    new android.content.Intent(
                            android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
              } catch (android.content.ActivityNotFoundException unavailable) {
                toast("Pengaturan notifikasi belum tersedia.");
              }
            });
    findViewById(R.id.focusReset)
        .setOnClickListener(
            v -> {
              confirmation =
                  new AlertDialog.Builder(this)
                      .setTitle("Ulang timer?")
                      .setMessage("Waktu sesi akan kembali ke awal. Status tugas tetap tersimpan.")
                      .setNegativeButton("Batal", null)
                      .setPositiveButton(
                          "Ulang",
                          (d, w) -> reset(clock.focusMinutes, clock.breakMinutes, clock.taskId))
                      .show();
            });
  }

  private void restoreChoice() {
    ((RadioGroup) findViewById(R.id.focusDuration))
        .check(clock.focusMinutes == 50 ? R.id.focusLong : R.id.focusShort);
  }

  private void reset(int focus, int pause, long task) {
    loadTitle(task);
    clock.reset(focus, pause, task);
    FocusTimer.schedule(this, clock);
    restoreChoice();
    render();
  }

  private void render() {
    clock.update(SystemClock.elapsedRealtime(), FocusTimer.boot(this));
    long seconds = (clock.remaining + 999) / 1000;
    ((TextView) findViewById(R.id.focusTime))
        .setText(String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60));
    ((TextView) findViewById(R.id.focusPhase))
        .setText(clock.finished ? "SESI SELESAI" : clock.breakPhase ? "WAKTU JEDA" : "WAKTU FOKUS");
    ((TextView) findViewById(R.id.focusPlay))
        .setText(
            clock.running
                ? "Jeda"
                : clock.finished ? (clock.breakPhase ? "Mulai fokus" : "Mulai jeda") : "Mulai");
    ((TextView) findViewById(R.id.focusTask)).setText(taskTitle);
    findViewById(R.id.focusNotificationSettings)
        .setVisibility(StudyReminders.permitted(this) ? View.GONE : View.VISIBLE);
    ((TextView) findViewById(R.id.focusHint))
        .setText(
            StudyReminders.permitted(this)
                ? "Timer tetap dihitung saat layar ditutup. Notifikasi dapat tertunda oleh"
                    + " penghemat daya. Tugas ditandai selesai sendiri."
                : "Timer tetap berjalan; notifikasi belum diizinkan. Buka halaman ini untuk melihat"
                    + " sisa waktu. Tugas ditandai selesai sendiri.");
  }

  private void loadTitle(long id) {
    taskTitle = "Satu langkah untuk belajarmu";
    try (ZikraDbHelper db = new ZikraDbHelper(this)) {
      for (Task t : db.getTasks())
        if (t.id == id) {
          taskTitle = t.title + (t.completed ? " · tugas sudah selesai" : "");
          break;
        }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (clock != null) {
      clock = FocusTimer.load(this);
      loadTitle(clock.taskId);
      handler.post(tick);
    }
  }

  @Override
  protected void onPause() {
    handler.removeCallbacks(tick);
    if (clock != null) {
      clock.update(SystemClock.elapsedRealtime(), FocusTimer.boot(this));
      FocusTimer.save(this, clock);
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (confirmation != null) confirmation.dismiss();
    super.onDestroy();
  }
}
