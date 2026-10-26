package com.zikra.app;

import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import com.zikra.app.data.WeeklyClass;
import com.zikra.app.data.WeeklyClassStore;
import com.zikra.app.location.LocationStore;
import com.zikra.app.study.ClassReminders;
import com.zikra.app.study.StudyAlarmPolicy;
import com.zikra.app.study.StudyReminders;
import com.zikra.app.ui.FeatureHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WeeklyClassesActivity extends BaseActivity {
  private WeeklyClassStore store;
  private int selectedDay;
  private final ClassAdapter adapter = new ClassAdapter();
  private List<WeeklyClass> rows = new ArrayList<>();
  private AlertDialog editor, deleteDialog;
  private TimePickerDialog picker;
  private View form;
  private long draftId, draftRevision;
  private int startMinute, endMinute;
  private boolean storageUnavailable;

  @Override protected void onCreate(Bundle saved) {
    super.onCreate(saved); setContentView(R.layout.activity_weekly_classes);
    setupScreen(getString(R.string.weekly_title));
    FeatureHeader header = findViewById(R.id.weeklyHeader);
    header.bind(0, 0, 0, 0, getString(R.string.weekly_title), null, 0, null);
    header.setSurfaceColor(getColor(R.color.studio_cream));
    header.back().setOnClickListener(v -> finish());
    getWindow().setStatusBarColor(getColor(R.color.studio_cream));
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    store = new WeeklyClassStore(this);
    selectedDay = saved == null ? getIntent().getIntExtra("weekday", LocationStore.today(this).getDayOfWeek().getValue())
        : saved.getInt("selected_day", 1);
    if (selectedDay < 1 || selectedDay > 7) selectedDay = 1;
    ListView list = findViewById(R.id.weeklyList); list.setAdapter(adapter);
    list.setOnItemClickListener((parent, view, position, id) -> {
      if (!rows.isEmpty()) edit(rows.get(position), null);
    });
    findViewById(R.id.weeklyAdd).setOnClickListener(v -> edit(null, null));
    findViewById(R.id.weeklyPermissions).setOnClickListener(v -> permissions());
    refresh();
    if (saved != null && saved.getBoolean("class_editor_open")) edit(null, saved);
  }

  @Override protected void onResume() {
    super.onResume(); if (store != null) { refresh(); ClassReminders.sync(this); }
  }
  @Override protected void onDestroy() {
    if (picker != null) picker.dismiss();
    if (deleteDialog != null) deleteDialog.dismiss();
    if (editor != null) editor.dismiss();
    if (store != null) store.close();
    super.onDestroy();
  }
  @Override protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out); out.putInt("selected_day", selectedDay);
    if (editor == null || !editor.isShowing()) return;
    out.putBoolean("class_editor_open", true); out.putLong("class_id", draftId);
    out.putLong("class_revision", draftRevision); out.putInt("class_start", startMinute);
    out.putInt("class_end", endMinute);
    out.putString("class_title", text(R.id.weeklyTitle));
    out.putString("class_room", text(R.id.weeklyRoom));
    out.putString("class_teacher", text(R.id.weeklyTeacher));
    out.putInt("class_day", ((Spinner) form.findViewById(R.id.weeklyDay)).getSelectedItemPosition());
    out.putInt("class_lead", ((Spinner) form.findViewById(R.id.weeklyLead)).getSelectedItemPosition());
  }

  private void refresh() {
    rows = new ArrayList<>();
    storageUnavailable=false;
    try { for (WeeklyClass item : store.all()) if (item.weekday == selectedDay) rows.add(item); }
    catch(android.database.sqlite.SQLiteException unavailable){storageUnavailable=true;}
    findViewById(R.id.weeklyAdd).setEnabled(!storageUnavailable);
    adapter.notifyDataSetChanged();
    LinearLayout days = findViewById(R.id.weeklyDays); days.removeAllViews();
    String[] labels = getResources().getStringArray(R.array.weekly_days);
    for (int day = 1; day <= 7; day++) {
      final int chosen = day;
      Button button = new Button(this); button.setText(labels[day - 1]);
      button.setTextSize(14); button.setAllCaps(false); button.setMinHeight(dp(48));
      button.setTextColor(getColor(R.color.pine)); button.setBackgroundResource(R.drawable.weekly_day_choice);
      button.setSelected(day == selectedDay); button.setActivated(day == selectedDay);
      button.setOnClickListener(v -> { selectedDay = chosen; refresh(); });
      days.addView(button, new LinearLayout.LayoutParams(-2, -2));
    }
    String hint = getString(R.string.weekly_zone, LocationStore.current(this).zone.getId()) + "\n"
        + getString(!StudyReminders.permitted(this) ? R.string.weekly_permission_off
            : StudyAlarmPolicy.exact(this) ? R.string.weekly_exact : R.string.weekly_inexact);
    ((TextView) findViewById(R.id.weeklyHint)).setText(hint);
    Button permissions = findViewById(R.id.weeklyPermissions);
    permissions.setText(StudyReminders.permitted(this)
        ? R.string.weekly_alarm_settings : R.string.weekly_notification_settings);
    permissions.setVisibility(StudyReminders.permitted(this) && StudyAlarmPolicy.exact(this)
        ? View.GONE : View.VISIBLE);
  }

  private void permissions() {
    try {
    if (!StudyReminders.permitted(this)) {
      if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 904); return;
      }
      startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
          .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
    } else if (Build.VERSION.SDK_INT >= 31 && !StudyAlarmPolicy.exact(this)) {
      startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
          Uri.parse("package:" + getPackageName())));
    }
    } catch (android.content.ActivityNotFoundException | SecurityException unavailable) {
      toast(getString(R.string.weekly_settings_unavailable));
    }
  }
  @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] result) {
    super.onRequestPermissionsResult(request, permissions, result);
    if (request == 904) { refresh(); ClassReminders.sync(this); }
  }

  private void edit(WeeklyClass item, Bundle saved) {
    if (editor != null) editor.dismiss();
    form = getLayoutInflater().inflate(R.layout.dialog_weekly_class, null);
    draftId = item == null ? 0 : item.id; draftRevision = item == null ? 0 : item.revision;
    startMinute = item == null ? 8 * 60 : item.startMinute;
    endMinute = item == null ? 9 * 60 : item.endMinute;
    ((EditText) form.findViewById(R.id.weeklyTitle)).setText(item == null ? "" : item.title);
    ((EditText) form.findViewById(R.id.weeklyRoom)).setText(item == null ? "" : item.room);
    ((EditText) form.findViewById(R.id.weeklyTeacher)).setText(item == null ? "" : item.teacher);
    ((Spinner) form.findViewById(R.id.weeklyDay)).setSelection((item == null ? selectedDay : item.weekday) - 1);
    ((Spinner) form.findViewById(R.id.weeklyLead)).setSelection(item == null ? 0
        : item.reminderMinutes == 30 ? 2 : item.reminderMinutes == 15 ? 1 : 0);
    if (saved != null) {
      draftId = saved.getLong("class_id"); draftRevision = saved.getLong("class_revision");
      startMinute = saved.getInt("class_start"); endMinute = saved.getInt("class_end");
      ((EditText) form.findViewById(R.id.weeklyTitle)).setText(saved.getString("class_title", ""));
      ((EditText) form.findViewById(R.id.weeklyRoom)).setText(saved.getString("class_room", ""));
      ((EditText) form.findViewById(R.id.weeklyTeacher)).setText(saved.getString("class_teacher", ""));
      ((Spinner) form.findViewById(R.id.weeklyDay)).setSelection(saved.getInt("class_day"));
      ((Spinner) form.findViewById(R.id.weeklyLead)).setSelection(saved.getInt("class_lead"));
    }
    renderTimes();
    form.findViewById(R.id.weeklyStart).setOnClickListener(v -> time(true));
    form.findViewById(R.id.weeklyEnd).setOnClickListener(v -> time(false));
    form.findViewById(R.id.weeklyDelete).setVisibility(draftId == 0 ? View.GONE : View.VISIBLE);
    form.findViewById(R.id.weeklyDelete).setOnClickListener(v -> confirmDelete());
    final AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(draftId == 0 ? R.string.weekly_new : R.string.weekly_edit).setView(form)
        .setNegativeButton(R.string.weekly_cancel, null).setPositiveButton(R.string.weekly_save, null).create();
    editor = dialog;
    dialog.setOnShowListener(v -> {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> save());
      dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
          | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    });
    dialog.setOnDismissListener(v -> { if (editor == dialog) { editor = null; form = null; } });
    dialog.show();
  }
  private void time(boolean start) {
    int value = start ? startMinute : endMinute;
    picker = new TimePickerDialog(this, (clock, hour, minute) -> {
      if (start) startMinute = hour * 60 + minute; else endMinute = hour * 60 + minute;
      renderTimes();
    }, value / 60, value % 60, true);
    picker.show();
  }
  private void renderTimes() {
    ((Button) form.findViewById(R.id.weeklyStart)).setText(getString(R.string.weekly_start, clock(startMinute)));
    ((Button) form.findViewById(R.id.weeklyEnd)).setText(getString(R.string.weekly_end, clock(endMinute)));
  }
  private void save() {
    if (editor == null || !editor.isShowing()) return;
    try {
      int choice = ((Spinner) form.findViewById(R.id.weeklyLead)).getSelectedItemPosition();
      WeeklyClass item = new WeeklyClass(draftId, draftRevision, text(R.id.weeklyTitle),
          text(R.id.weeklyRoom), text(R.id.weeklyTeacher),
          ((Spinner) form.findViewById(R.id.weeklyDay)).getSelectedItemPosition() + 1,
          startMinute, endMinute, choice == 2 ? 30 : choice == 1 ? 15 : 0);
      store.save(item); editor.dismiss(); refresh(); ClassReminders.sync(this);
      if (item.reminderMinutes > 0 && Build.VERSION.SDK_INT >= 33
          && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 904);
    } catch (WeeklyClassStore.Conflict conflict) { error(R.string.weekly_overlap); }
    catch (WeeklyClassStore.Stale stale) { error(R.string.weekly_stale); }
    catch (WeeklyClassStore.Full full) { error(R.string.weekly_full); }
    catch (IllegalArgumentException invalid) { error(R.string.weekly_invalid); }
    catch (android.database.sqlite.SQLiteException failed) { error(R.string.weekly_save_failed); }
  }
  private void confirmDelete() {
    final long id = draftId, revision = draftRevision;
    deleteDialog = new AlertDialog.Builder(this).setTitle(R.string.weekly_delete)
        .setMessage(getString(R.string.weekly_delete_confirm, text(R.id.weeklyTitle)))
        .setNegativeButton(R.string.weekly_cancel, null)
        .setPositiveButton(R.string.weekly_delete, (dialog, which) -> {
          try { if (!store.delete(id, revision)) { error(R.string.weekly_stale); return; } }
          catch(android.database.sqlite.SQLiteException failed){error(R.string.weekly_save_failed);return;}
          if (editor != null) editor.dismiss(); refresh(); ClassReminders.sync(this);
        }).show();
  }
  private void error(int resource) {
    if (form != null) ((TextView) form.findViewById(R.id.weeklyError)).setText(resource);
  }
  private String text(int id) { return ((EditText) form.findViewById(id)).getText().toString(); }
  public static String clock(int minute) { return String.format(Locale.ROOT, "%02d.%02d", minute / 60, minute % 60); }
  private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
  private final class ClassAdapter extends BaseAdapter {
    @Override public int getCount() { return Math.max(1, rows.size()); }
    @Override public Object getItem(int position) { return rows.isEmpty() ? null : rows.get(position); }
    @Override public long getItemId(int position) { return rows.isEmpty() ? -1 : rows.get(position).id; }
    @Override public boolean isEnabled(int position) { return !rows.isEmpty(); }
    @Override public boolean areAllItemsEnabled() { return !rows.isEmpty(); }
    @Override public View getView(int position, View reusable, ViewGroup parent) {
      LinearLayout row = reusable instanceof LinearLayout ? (LinearLayout)reusable : new LinearLayout(WeeklyClassesActivity.this);
      row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(8), dp(16), dp(8), dp(16));
      row.setMinimumHeight(dp(72));
      TextView title;
      TextView detail;
      if(row.getChildCount()==2){title=(TextView)row.getChildAt(0);detail=(TextView)row.getChildAt(1);}
      else {title=new TextView(WeeklyClassesActivity.this);detail=new TextView(WeeklyClassesActivity.this);row.addView(title);row.addView(detail);}
      title.setTextSize(16); title.setTextColor(getColor(R.color.pine)); title.setTypeface(null, Typeface.BOLD);
      detail.setVisibility(rows.isEmpty()?View.GONE:View.VISIBLE);
      if (rows.isEmpty()) { title.setText(storageUnavailable?R.string.weekly_read_failed:R.string.weekly_empty); return row; }
      WeeklyClass item = rows.get(position); title.setText(item.title);
      detail.setTextSize(14); detail.setTextColor(getColor(R.color.concept_muted));
      detail.setPadding(0, dp(6), 0, 0);
      detail.setText(clock(item.startMinute) + "–" + clock(item.endMinute)
          + (item.room.isEmpty() ? "" : " · " + item.room)
          + (item.teacher.isEmpty() ? "" : "\n" + item.teacher));
      return row;
    }
  }
}
