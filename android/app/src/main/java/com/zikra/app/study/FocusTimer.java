package com.zikra.app.study;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.provider.Settings;
import com.zikra.app.FocusTimerActivity;
import com.zikra.app.R;

/** One explicitly started timer; AlarmManager provides a best-effort background notification. */
public final class FocusTimer {
  public static final String PREFS = "study_focus_timer";

  private FocusTimer() {}

  public static int boot(Context c) {
    return Settings.Global.getInt(c.getContentResolver(), Settings.Global.BOOT_COUNT, 0);
  }

  public static FocusClock load(Context c) {
    SharedPreferences p = c.getSharedPreferences(PREFS, 0);
    FocusClock clock = new FocusClock();
    clock.focusMinutes = p.getInt("focus", 25);
    clock.breakMinutes = p.getInt("break", 5);
    clock.remaining = p.getLong("remaining", clock.focusMinutes * 60000L);
    clock.end = p.getLong("end", 0);
    clock.boot = p.getInt("boot", boot(c));
    clock.taskId = p.getLong("task", -1);
    clock.session = p.getLong("session", 0);
    clock.running = p.getBoolean("running", false);
    clock.finished = p.getBoolean("finished", false);
    clock.breakPhase = p.getBoolean("break_phase", false);
    clock.update(SystemClock.elapsedRealtime(), boot(c));
    return clock;
  }

  public static void save(Context c, FocusClock clock) {
    c.getSharedPreferences(PREFS, 0)
        .edit()
        .putInt("focus", clock.focusMinutes)
        .putInt("break", clock.breakMinutes)
        .putLong("remaining", clock.remaining)
        .putLong("end", clock.end)
        .putInt("boot", clock.boot)
        .putLong("task", clock.taskId)
        .putLong("session", clock.session)
        .putBoolean("running", clock.running)
        .putBoolean("finished", clock.finished)
        .putBoolean("break_phase", clock.breakPhase)
        .commit();
  }

  private static PendingIntent pending(Context c, long session) {
    return PendingIntent.getBroadcast(
        c,
        8102,
        new Intent(c, FocusAlarmReceiver.class).putExtra("session", session),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  public static void schedule(Context c, FocusClock clock) {
    save(c, clock);
    AlarmManager alarms = c.getSystemService(AlarmManager.class);
    alarms.cancel(pending(c, clock.session));
    c.getSystemService(NotificationManager.class).cancel(8102);
    if (clock.running)
      alarms.setAndAllowWhileIdle(
          AlarmManager.ELAPSED_REALTIME_WAKEUP, clock.end, pending(c, clock.session));
  }

  public static void deliver(Context c, Intent intent) {
    FocusClock clock = load(c);
    if (intent.getLongExtra("session", -1) != clock.session || !clock.finished) return;
    SharedPreferences p = c.getSharedPreferences(PREFS, 0);
    if (p.getLong("notified", -1) == clock.session) return;
    save(c, clock);
    p.edit().putLong("notified", clock.session).commit();
    if (!StudyReminders.permitted(c)) return;
    NotificationManager n = c.getSystemService(NotificationManager.class);
    n.createNotificationChannel(
        new NotificationChannel(
            "study_focus", "Timer fokus", NotificationManager.IMPORTANCE_DEFAULT));
    n.notify(
        8102,
        new Notification.Builder(c, "study_focus")
            .setSmallIcon(R.drawable.ic_nearby_info)
            .setContentTitle(clock.breakPhase ? "Jeda selesai" : "Sesi fokus selesai")
            .setContentText("Buka Zikra untuk melanjutkan. Status tugas tetap kamu tandai sendiri.")
            .setContentIntent(
                PendingIntent.getActivity(
                    c,
                    8102,
                    new Intent(c, FocusTimerActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build());
  }

  public static void restore(Context c) {
    FocusClock clock = load(c);
    schedule(c, clock);
  }
  public static void pauseForAdhan(Context c) {
    FocusClock timer=load(c);
    if(!timer.running)return;
    timer.pause(SystemClock.elapsedRealtime(),boot(c));schedule(c,timer);
    c.getSharedPreferences(PREFS,0).edit().putLong("adhan_pause_session",timer.session).commit();
  }
}
