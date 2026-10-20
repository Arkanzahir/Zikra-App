package com.zikra.app.study;

import android.app.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.zikra.app.R;
import com.zikra.app.data.Task;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Optional deadline controls, kept in the task draft until Save is pressed. */
public final class ScheduleEditor {
  private final Activity activity;
  private final View root;
  private long due;
  private final Switch reminder;
  private Dialog picker;
  private boolean restoring;

  public ScheduleEditor(Activity activity, View root, Task task, Runnable permission) {
    this.activity = activity;
    this.root = root;
    due = task == null ? 0 : task.dueAt;
    root.findViewById(R.id.studySchedule).setVisibility(View.VISIBLE);
    reminder = root.findViewById(R.id.studyReminder);
    reminder.setChecked(task != null && task.reminder);
    reminder.setOnCheckedChangeListener(
        (v, checked) -> {
          if (checked && !restoring) permission.run();
          render();
        });
    root.findViewById(R.id.studyDeadline).setOnClickListener(v -> choose());
    root.findViewById(R.id.studyDeadlineClear)
        .setOnClickListener(
            v -> {
              due = 0;
              reminder.setChecked(false);
              render();
            });
    render();
  }

  private void choose() {
    Calendar seed = Calendar.getInstance();
    seed.setTimeInMillis(due == 0 ? System.currentTimeMillis() + 3600000 : due);
    picker =
        new DatePickerDialog(
            activity,
            (view, year, month, day) -> {
              picker =
                  new TimePickerDialog(
                      activity,
                      (clock, hour, minute) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, day, hour, minute, 0);
                        chosen.set(Calendar.MILLISECOND, 0);
                        due = chosen.getTimeInMillis();
                        render();
                      },
                      seed.get(Calendar.HOUR_OF_DAY),
                      seed.get(Calendar.MINUTE),
                      true);
              picker.show();
            },
            seed.get(Calendar.YEAR),
            seed.get(Calendar.MONTH),
            seed.get(Calendar.DAY_OF_MONTH));
    picker.show();
  }

  public static String label(long due) {
    if (due == 0) return "Tanpa tenggat";
    return Instant.ofEpochMilli(due)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("id-ID")));
  }

  public void render() {
    ((TextView) root.findViewById(R.id.studyDeadline))
        .setText(due == 0 ? "Atur tenggat (opsional)" : label(due));
    root.findViewById(R.id.studyDeadlineClear).setVisibility(due == 0 ? View.GONE : View.VISIBLE);
    reminder.setEnabled(due > 0);
    ((TextView) root.findViewById(R.id.studyScheduleHint))
        .setText(
            !reminder.isChecked()
                ? "Tenggat memakai waktu lokal perangkat. Pengingat bersifat opsional."
                : StudyReminders.permitted(activity)
                    ? "Pengingat sekitar waktu tenggat; dapat tertunda oleh sistem/penghemat daya."
                    : "Notifikasi belum diizinkan. Tenggat tetap disimpan; aktifkan notifikasi"
                        + " aplikasi agar pengingat dapat tampil.");
  }

  public boolean valid() {
    if (reminder.isChecked() && due <= System.currentTimeMillis()) {
      ((TextView) root.findViewById(R.id.studyScheduleHint))
          .setText(
              "Pilih tenggat mendatang atau matikan pengingat untuk tugas yang sudah lewat"
                  + " tenggat.");
      return false;
    }
    return true;
  }

  public long dueAt() {
    return due;
  }

  public boolean reminder() {
    return reminder.isChecked();
  }

  public void save(Bundle state) {
    state.putLong("study_due", due);
    state.putBoolean("study_reminder", reminder());
  }

  public void restore(Bundle state) {
    due = state.getLong("study_due", due);
    restoring = true;
    reminder.setChecked(state.getBoolean("study_reminder", false));
    restoring = false;
    render();
  }

  public void close() {
    if (picker != null) picker.dismiss();
  }
}
