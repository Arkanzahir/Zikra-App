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
  private com.zikra.app.ui.FocusGardenView garden;
  private com.zikra.app.ui.AmbientMotionPolicy motion;
  private boolean foreground, entered;
  private String lastTime = "";
  private Boolean lastPlaying;
  private boolean notificationsAllowed;
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
            R.drawable.ic_nearby_info,
            getString(R.string.focus_information));
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.focusHeader))
        .setSurfaceColor(getColor(R.color.background));
    setupScreen("Fokus belajar");
    getWindow().setStatusBarColor(getColor(R.color.background));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    garden = findViewById(R.id.focusGarden);
    motion = new com.zikra.app.ui.AmbientMotionPolicy(this, this::refreshMotion);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.focusHeader))
        .action()
        .setOnClickListener(
            v -> {
              confirmation =
                  new AlertDialog.Builder(this)
                      .setTitle(R.string.focus_information)
                      .setMessage(R.string.focus_information_body)
                      .setPositiveButton("Mengerti", null)
                      .show();
            });
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
              feedback(v);
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
    String time = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    if (!lastTime.equals(time)) {
      lastTime = time;
      ((TextView) findViewById(R.id.focusTime)).setText(time);
    }
    long total = (clock.breakPhase ? clock.breakMinutes : clock.focusMinutes) * 60000L;
    boolean paused = !clock.running && !clock.finished && clock.remaining < total;
    text(
        R.id.focusPhase,
        clock.finished
            ? R.string.focus_finished
            : paused
                ? R.string.focus_paused
                : clock.breakPhase
                    ? R.string.focus_rest
                    : clock.running ? R.string.focus_active : R.string.focus_ready);
    text(
        R.id.focusTotal,
        clock.finished
            ? (clock.breakPhase ? R.string.focus_finish_break_note : R.string.focus_finish_note)
            : R.string.focus_remaining);
    Button play = findViewById(R.id.focusPlay);
    text(
        R.id.focusPlay,
        clock.running
            ? (clock.breakPhase ? R.string.focus_pause_break : R.string.focus_pause)
            : clock.finished
                ? (clock.breakPhase ? R.string.focus_start : R.string.focus_begin_break)
                : paused ? R.string.focus_continue : R.string.focus_start);
    if (lastPlaying == null || lastPlaying != clock.running) {
      lastPlaying = clock.running;
      play.setCompoundDrawablesRelativeWithIntrinsicBounds(
          clock.running ? R.drawable.audio_pause : R.drawable.audio_play, 0, 0, 0);
    }
    TextView title = findViewById(R.id.focusTask);
    if (!taskTitle.contentEquals(title.getText())) title.setText(taskTitle);
    findViewById(R.id.focusNotificationSettings)
        .setVisibility(notificationsAllowed ? View.GONE : View.VISIBLE);
    text(
        R.id.focusHint,
        notificationsAllowed ? R.string.focus_encourage : R.string.focus_notification_off);
    if (notificationsAllowed && motion != null && !motion.enabled())
      text(R.id.focusHint, R.string.design_focus_motion_off);
    else if (notificationsAllowed && (clock.breakPhase || paused))
      text(R.id.focusHint, R.string.design_focus_rest_hint);
    ((ProgressBar) findViewById(R.id.focusProgress))
        .setProgress((int) Math.max(0, Math.min(1000, 1000L - clock.remaining * 1000L / total)));
    // A pause changes only the companion pose; it does not start the break countdown.
    garden.snapshot(
        clock.remaining, total, clock.running, clock.breakPhase || paused, clock.finished);
  }

  private void text(int id, int resource) {
    TextView view = findViewById(id);
    String value = getString(resource);
    if (!value.contentEquals(view.getText())) view.setText(value);
  }

  private void refreshMotion() {
    boolean enabled = motion != null && motion.enabled();
    if (garden != null) garden.setMotion(foreground, enabled);
    if (!enabled) {
      View content = findViewById(R.id.focusContent);
      content.animate().cancel();
      content.setAlpha(1);
      content.setTranslationY(0);
      View play = findViewById(R.id.focusPlay);
      play.animate().cancel();
      play.setScaleX(1);
      play.setScaleY(1);
    }
  }

  private void feedback(View view) {
    if (motion == null || !motion.enabled()) return;
    view.animate().cancel();
    view.setScaleX(.975f);
    view.setScaleY(.975f);
    view.animate().scaleX(1).scaleY(1).setDuration(160).start();
  }

  @Override
  public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(code, permissions, grants);
    if (code == 902 && clock != null) {
      notificationsAllowed = StudyReminders.permitted(this);
      render();
    }
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
    foreground = true;
    notificationsAllowed = StudyReminders.permitted(this);
    if (motion != null) motion.start();
    if (!entered && motion != null && motion.enabled()) {
      entered = true;
      View content = findViewById(R.id.focusContent);
      content.setAlpha(0);
      content.setTranslationY(dp(10));
      content.animate().alpha(1).translationY(0).setDuration(360).start();
    }
    if (clock != null) {
      clock = FocusTimer.load(this);
      loadTitle(clock.taskId);
      handler.post(tick);
    }
  }

  @Override
  protected void onPause() {
    foreground = false;
    if (motion != null) motion.stop();
    View content = findViewById(R.id.focusContent);
    content.animate().cancel();
    content.setAlpha(1);
    content.setTranslationY(0);
    View play = findViewById(R.id.focusPlay);
    play.animate().cancel();
    play.setScaleX(1);
    play.setScaleY(1);
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
    if (motion != null) motion.stop();
    if (confirmation != null) confirmation.dismiss();
    super.onDestroy();
  }
}
