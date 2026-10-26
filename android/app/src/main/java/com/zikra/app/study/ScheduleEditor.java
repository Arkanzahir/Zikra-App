package com.zikra.app.study;

import android.app.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.zikra.app.R;
import com.zikra.app.data.Task;
import com.zikra.app.location.LocationStore;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Optional deadline controls, kept in the task draft until Save is pressed. */
public final class ScheduleEditor {
  private final Activity activity;
  private final View root;
  private long due;
  private final Switch reminder;
  private final Switch advance;
  private final EditText subject;
  private final Spinner priority;
  private Dialog picker;
  private boolean restoring;

  public ScheduleEditor(Activity activity, View root, Task task, Runnable permission) {
    this.activity = activity;
    this.root = root;
    due = task == null ? 0 : task.dueAt;
    root.findViewById(R.id.studySchedule).setVisibility(View.VISIBLE);
    reminder = root.findViewById(R.id.studyReminder);
    advance = root.findViewById(R.id.studyAdvanceReminder);
    root.findViewById(R.id.studyAcademicDetails).setVisibility(View.VISIBLE);
    ((TextView)root.findViewById(R.id.studySubjectLabel)).setText(EducationLevel.courseLabel(activity));
    subject = root.findViewById(R.id.studySubject);
    priority = root.findViewById(R.id.studyPriority);
    subject.setText(task == null ? "" : task.subject);
    priority.setSelection(task == null ? 1 : task.priority);
    advance.setChecked(task != null && task.advanceReminder);
    reminder.setChecked(task != null && task.reminder);
    reminder.setOnCheckedChangeListener(
        (v, checked) -> {
          if (!checked) advance.setChecked(false);
          if (checked && !restoring) permission.run();
          render();
        });
    advance.setOnCheckedChangeListener((button,checked)->render());
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
    Calendar seed = Calendar.getInstance(TimeZone.getTimeZone(LocationStore.current(activity).zone));
    seed.setTimeInMillis(due == 0 ? System.currentTimeMillis() + 3600000 : due);
    picker =
        new DatePickerDialog(
            activity,
            (view, year, month, day) -> {
              picker =
                  new TimePickerDialog(
                      activity,
                      (clock, hour, minute) -> {
                        Calendar chosen =
                            Calendar.getInstance(TimeZone.getTimeZone(LocationStore.current(activity).zone));
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

  public static String label(android.content.Context context, long due) {
    if (due == 0) return "Tanpa tenggat";
    return Instant.ofEpochMilli(due)
        .atZone(LocationStore.current(context).zone)
        .format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("id-ID")));
  }

  public void render() {
    ((TextView) root.findViewById(R.id.studyDeadline))
        .setText(due == 0 ? "Atur tenggat (opsional)" : label(activity, due));
    root.findViewById(R.id.studyDeadlineClear).setVisibility(due == 0 ? View.GONE : View.VISIBLE);
    reminder.setEnabled(due > 0);
    advance.setEnabled(due > 0 && reminder.isChecked());
    ((TextView) root.findViewById(R.id.studyScheduleHint))
        .setText(
            !reminder.isChecked()
                ? "Tenggat mengikuti zona kota pilihan. Pengingat bersifat opsional."
                : StudyReminders.permitted(activity)
                    ? "Pengingat sekitar waktu tenggat; dapat tertunda oleh sistem/penghemat daya."
                    : "Notifikasi belum diizinkan. Tenggat tetap disimpan; aktifkan notifikasi"
                        + " aplikasi agar pengingat dapat tampil.");
    if(reminder.isChecked()&&advance.isChecked())((TextView)root.findViewById(R.id.studyScheduleHint)).append("\n"+activity.getString(R.string.weekly_task_advance_hint));
    if(activity.getSharedPreferences("study_alarm_ids",0).getBoolean("limited",false))((TextView)root.findViewById(R.id.studyScheduleHint)).append("\n"+activity.getString(R.string.weekly_task_alarm_limit));
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

  public String subject() { return subject.getText().toString(); }
  public int priority() { return priority.getSelectedItemPosition(); }
  public boolean advanceReminder() { return reminder() && advance.isChecked(); }

  public static String details(android.content.Context context, Task task) {
    String[] priorities = context.getResources().getStringArray(R.array.weekly_priorities);
    String info = task.subject.isEmpty()
        ? context.getString(R.string.weekly_task_priority_only, priorities[task.priority])
        : context.getString(R.string.weekly_task_details, task.subject, priorities[task.priority]);
    return label(context, task.dueAt) + "\n" + info;
  }

  public void save(Bundle state) {
    state.putLong("study_due", due);
    state.putBoolean("study_reminder", reminder());
    state.putBoolean("study_advance", advanceReminder());
    state.putString("study_subject", subject());
    state.putInt("study_priority", priority());
  }

  public void restore(Bundle state) {
    due = state.getLong("study_due", due);
    restoring = true;
    reminder.setChecked(state.getBoolean("study_reminder", false));
    advance.setChecked(state.getBoolean("study_advance", false));
    subject.setText(state.getString("study_subject", ""));
    priority.setSelection(state.getInt("study_priority", 1));
    restoring = false;
    render();
  }

  public void close() {
    if (picker != null) picker.dismiss();
  }
}
