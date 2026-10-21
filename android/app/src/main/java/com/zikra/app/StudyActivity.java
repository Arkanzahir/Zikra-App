package com.zikra.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.zikra.app.data.HifzTarget;
import com.zikra.app.data.Task;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.study.*;
import com.zikra.app.study.SurahCatalog;
import com.zikra.app.study.SurahCatalog.Chapter;
import com.zikra.app.ui.HifzEditor;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Persistent academic/Hifz plans and a separately scoped daily worship checklist. */
public final class StudyActivity extends BaseActivity {
  private static final int REQUEST_VOICE_TASK = 100;
  private static final Locale ID = Locale.forLanguageTag("id-ID");
  private final Handler handler = new Handler(Looper.getMainLooper());
  private ZikraDbHelper db;
  private TaskAdapter adapter;
  private boolean diaryMode, ibadahMode, catalogLoading, completedExpanded, editorRetained;
  private String scopeCategory;
  private Task featuredTask;
  private View agendaHeader;
  private String filter = "All";
  private LocalDate displayedDay;
  private LocalDate selectedAgendaDay;
  private Dialog editorDialog;
  private android.app.DatePickerDialog agendaDateDialog;
  private View editorView;
  private Task editorTask;
  private HifzEditor hifzEditor;
  private ScheduleEditor scheduleEditor;
  private SurahCatalog catalog;
  private List<Chapter> chapters = SurahCatalog.local();
  private final Runnable dayCheck =
      new Runnable() {
        @Override
        public void run() {
          if (!LocalDate.now().equals(displayedDay)) {
            renderDate();
            refreshTasks();
          }
          handler.postDelayed(this, 60000);
        }
      };

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    java.util.List<Chapter> bundled = new java.util.ArrayList<>();
    for (com.zikra.app.reading.QuranChapters.Chapter c :
        com.zikra.app.reading.QuranChapters.all(this))
      bundled.add(new Chapter(c.id, c.name, c.count));
    chapters = java.util.Collections.unmodifiableList(bundled);
    setContentView(R.layout.activity_study);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.sharedFeatureHeader))
        .bindIds(R.id.study_back, R.id.study_screen_title, R.id.study_screen_tagline, 0);

    ListView agenda = findViewById(R.id.study_tasks);
    agendaHeader = getLayoutInflater().inflate(R.layout.study_agenda_header, agenda, false);
    agenda.addHeaderView(agendaHeader, null, false);
    completedExpanded = state != null && state.getBoolean("completed_expanded", false);
    if (state != null && state.getString("agenda_day") != null)
      try {
        selectedAgendaDay = LocalDate.parse(state.getString("agenda_day"));
      } catch (java.time.DateTimeException ignored) {
      }
    diaryMode = ZikraDbHelper.CATEGORY_HIFZ.equals(getIntent().getStringExtra("category"));
    ibadahMode = ZikraDbHelper.CATEGORY_IBADAH.equals(getIntent().getStringExtra("category"));
    scopeCategory =
        diaryMode
            ? ZikraDbHelper.CATEGORY_HIFZ
            : ibadahMode ? ZikraDbHelper.CATEGORY_IBADAH : ZikraDbHelper.CATEGORY_STUDY;
    NavDock.attach(this, findViewById(R.id.studyPageRoot), ibadahMode ? "prayer" : "study");
    setupScreen(
        ibadahMode
            ? "Checklist ibadah"
            : getString(diaryMode ? R.string.study_hifz_title : R.string.study_title));
    getWindow().setStatusBarColor(getColor(R.color.white));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    db = new ZikraDbHelper(this);
    filter = scopeCategory;
    agendaHeader.findViewById(R.id.study_filters).setVisibility(View.GONE);
    text(R.id.study_screen_title, diaryMode ? R.string.study_hifz_title : R.string.study_title);
    if (diaryMode) {
      filter = ZikraDbHelper.CATEGORY_HIFZ;
      text(R.id.study_screen_title, R.string.study_hifz_title);
      text(R.id.study_screen_tagline, R.string.study_hifz_tagline);
      text(R.id.study_heading, R.string.agenda_hifz_heading);
      text(R.id.study_section, R.string.agenda_hifz_remaining);
      text(R.id.study_add, R.string.study_hifz_add);
      text(R.id.study_empty, R.string.study_hifz_empty);
      agendaHeader.findViewById(R.id.study_filters).setVisibility(View.GONE);
      catalog = new SurahCatalog();
      loadCatalog();
    }
    if (ibadahMode) {
      ((TextView) findViewById(R.id.study_screen_title)).setText("Checklist ibadah");
      ((TextView) findViewById(R.id.study_screen_tagline))
          .setText("Jaga kebiasaan baik, satu per satu.");
      ((TextView) agendaHeader.findViewById(R.id.study_heading)).setText("Jeda untuk\nibadah.");
      ((TextView) agendaHeader.findViewById(R.id.study_section)).setText("Ibadah lainnya");
      ((TextView) findViewById(R.id.study_add)).setText("Tambah ibadah");
    }
    if (!diaryMode && !ibadahMode) {
      ((TextView) agendaHeader.findViewById(R.id.study_heading))
          .setText(R.string.concept_study_heading);
      agendaHeader.findViewById(R.id.study_scope_hint).setVisibility(View.GONE);
      View focus = agendaHeader.findViewById(R.id.studyFocus);
      focus.setVisibility(View.VISIBLE);
      focus.setOnClickListener(
          v -> {
            Intent intent = new Intent(this, FocusTimerActivity.class);
            if (featuredTask != null) intent.putExtra("task", featuredTask.id);
            startActivity(intent);
          });
      com.zikra.app.ui.FeatureHeader header = findViewById(R.id.sharedFeatureHeader);
      header.back().setVisibility(View.GONE);
      header.setSurfaceColor(getColor(R.color.concept_ivory));
      View chrome = header.getChildAt(0);
      chrome.setMinimumHeight(dp(56));
      chrome.setPadding(dp(8), dp(4), dp(8), dp(4));
      ((TextView) findViewById(R.id.study_screen_title)).setTextSize(18);
      android.widget.ImageButton calendar = (android.widget.ImageButton) header.action();
      calendar.setImageResource(R.drawable.ic_tasks);
      calendar.setVisibility(View.VISIBLE);
      calendar.setBackgroundResource(R.drawable.circle_mint);
      calendar.setContentDescription(getString(R.string.study_scene_calendar));
      calendar.setOnClickListener(v -> chooseAgendaDay());
      findViewById(R.id.study_add).setVisibility(View.GONE);
      agendaHeader
          .findViewById(R.id.studyAddInline)
          .setOnClickListener(v -> showEditor(null, null));
      getWindow().setStatusBarColor(getColor(R.color.concept_ivory));
      agendaHeader.findViewById(R.id.hifz_planned).setVisibility(View.GONE);
    }
    agendaHeader
        .findViewById(R.id.studyFocusMetric)
        .setVisibility(diaryMode || ibadahMode ? View.GONE : View.VISIBLE);
    agendaHeader
        .findViewById(R.id.studyAddInline)
        .setVisibility(diaryMode || ibadahMode ? View.GONE : View.VISIBLE);
    ((TextView) agendaHeader.findViewById(R.id.studySummaryCaption))
        .setText(
            ibadahMode
                ? R.string.study_scene_worship_done
                : diaryMode ? R.string.study_scene_targets_done : R.string.study_scene_tasks_done);
    adapter = new TaskAdapter();
    ListView list = findViewById(R.id.study_tasks);
    list.setAdapter(adapter);
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          Object item = parent.getItemAtPosition(position);
          if (item instanceof Task) showEditor((Task) item, null);
        });
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.sharedFeatureHeader))
        .back()
        .setOnClickListener(
            view -> {
              if (ibadahMode) {
                finish();
                return;
              }
              startActivity(
                  new Intent(this, HomeActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
              finish();
            });
    findViewById(R.id.study_add).setOnClickListener(view -> showEditor(null, null));
    agendaHeader
        .findViewById(R.id.ibadah_template)
        .setOnClickListener(
            v -> {
              int added = db.addObligatoryPrayerChecklist();
              refreshTasks();
              toast(
                  getString(
                      added == 0
                          ? R.string.planner_prayers_exist
                          : R.string.planner_prayers_added));
            });
    text(
        R.id.study_scope_hint,
        diaryMode
            ? R.string.planner_hifz_scope
            : ibadahMode ? R.string.planner_ibadah_scope : R.string.planner_study_scope);
    ((TextView) agendaHeader.findViewById(R.id.agenda_feature_caption))
        .setText(ibadahMode ? R.string.planner_continue_today : R.string.agenda_continue);
    if (state != null && state.getBoolean("editor_open", false)) {
      long id = state.getLong("editor_task_id", -1);
      Task saved = null;
      for (Task task : db.getTasks())
        if (task.id == id && scopeCategory.equals(task.category)) saved = task;
      showEditor(saved, state.getString("editor_title", ""));
      ((RadioGroup) editorView.findViewById(R.id.study_editor_category))
          .check(choice(scopeCategory));
      if (scheduleEditor != null) scheduleEditor.restore(state);
      if (hifzEditor != null)
        hifzEditor.restore(state, chapters, catalogLoading, this::loadCatalog);
      if (id != -1 && saved == null) {
        retainDraftForNewTask();
      } else if (state.getBoolean("editor_retained", false)) {
        retainDraftForNewTask();
      }
    }
  }

  private void text(int id, int value) {
    TextView view = findViewById(id);
    if (view == null) view = agendaHeader.findViewById(id);
    view.setText(value);
  }

  @Override
  protected void onResume() {
    super.onResume();
    renderDate();
    refreshTasks();
    handler.removeCallbacks(dayCheck);
    handler.postDelayed(dayCheck, 60000);
  }

  @Override
  protected void onPause() {
    handler.removeCallbacks(dayCheck);
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("study_filter", filter);
    if (selectedAgendaDay != null) state.putString("agenda_day", selectedAgendaDay.toString());
    state.putBoolean("completed_expanded", completedExpanded);
    if (editorDialog != null && editorDialog.isShowing()) {
      state.putBoolean("editor_open", true);
      state.putLong("editor_task_id", editorTask == null ? -1 : editorTask.id);
      state.putString(
          "editor_title",
          ((EditText) editorView.findViewById(R.id.study_editor_title)).getText().toString());
      state.putString("editor_category", selectedCategory());
      state.putBoolean("editor_retained", editorRetained);
      if (hifzEditor != null) hifzEditor.save(state);
      if (scheduleEditor != null) scheduleEditor.save(state);
    }
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (editorDialog != null) editorDialog.dismiss();
    if (catalog != null) catalog.close();
    if (agendaDateDialog != null) agendaDateDialog.dismiss();
    if (db != null) db.close();
    super.onDestroy();
  }

  private void chooseAgendaDay() {
    LocalDate selected = selectedAgendaDay == null ? LocalDate.now() : selectedAgendaDay;
    agendaDateDialog =
        new android.app.DatePickerDialog(
            this,
            (picker, year, month, day) -> {
              selectedAgendaDay = LocalDate.of(year, month + 1, day);
              renderWeek();
              refreshTasks();
            },
            selected.getYear(),
            selected.getMonthValue() - 1,
            selected.getDayOfMonth());
    agendaDateDialog.show();
  }

  private void renderDate() {
    displayedDay = LocalDate.now();
    String date =
        displayedDay.format(
            DateTimeFormatter.ofPattern(
                diaryMode || ibadahMode ? "EEEE, d MMMM yyyy" : "EEEE, d MMMM", ID));
    ((TextView) agendaHeader.findViewById(R.id.study_date))
        .setText(date.substring(0, 1).toUpperCase(ID) + date.substring(1));
    renderWeek();
  }

  private void renderWeek() {
    android.widget.LinearLayout week = agendaHeader.findViewById(R.id.studyWeek);
    week.setVisibility(diaryMode || ibadahMode ? View.GONE : View.VISIBLE);
    if (diaryMode || ibadahMode) return;
    week.removeAllViews();
    LocalDate today = LocalDate.now();
    LocalDate anchor = selectedAgendaDay == null ? today : selectedAgendaDay;
    LocalDate monday =
        anchor.with(
            java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    for (int i = 0; i < 7; i++) {
      LocalDate day = monday.plusDays(i);
      TextView cell = new TextView(this);
      boolean selected = anchor.equals(day);
      cell.setText(day.format(DateTimeFormatter.ofPattern("EEE\nd", ID)));
      cell.setTextSize(12);
      cell.setGravity(Gravity.CENTER);
      cell.setMinHeight(dp(56));
      cell.setTextColor(getColor(selected ? R.color.white : R.color.pine));
      cell.setBackgroundResource(selected ? R.drawable.concept_selected_day : 0);
      cell.setSelected(selected);
      cell.setContentDescription(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)));
      cell.setOnClickListener(
          v -> {
            selectedAgendaDay = day;
            renderWeek();
            refreshTasks();
          });
      cell.setOnLongClickListener(
          v -> {
            selectedAgendaDay = null;
            renderWeek();
            refreshTasks();
            return true;
          });
      week.addView(cell, new android.widget.LinearLayout.LayoutParams(dp(44), -2));
    }
    com.zikra.app.study.FocusClock timer = com.zikra.app.study.FocusTimer.load(this);
    ((TextView) agendaHeader.findViewById(R.id.studyFocusDuration))
        .setText(
            getString(R.string.concept_focus_duration, timer.focusMinutes, timer.breakMinutes));
    ((TextView) agendaHeader.findViewById(R.id.studySessionCaption))
        .setText(
            getString(
                R.string.study_scene_elapsed,
                timer.breakPhase
                    ? timer.focusMinutes
                    : Math.max(0, timer.focusMinutes - (int) ((timer.remaining + 59999) / 60000))));
    int elapsedPercent =
        timer.breakPhase
            ? 100
            : (int)
                Math.max(
                    0, Math.min(100, 100 - timer.remaining * 100L / (timer.focusMinutes * 60000L)));
    ((ProgressBar) agendaHeader.findViewById(R.id.studyElapsedProgress))
        .setProgress(elapsedPercent);
    ((TextView) agendaHeader.findViewById(R.id.study_section))
        .setText(
            selectedAgendaDay == null
                ? getString(R.string.study_scene_agenda)
                : selectedAgendaDay.equals(today)
                    ? getString(R.string.concept_study_agenda)
                    : getString(
                        R.string.concept_study_agenda_date,
                        selectedAgendaDay.format(DateTimeFormatter.ofPattern("d MMM", ID))));
  }

  private void refreshTasks() {
    List<Task> filtered = new ArrayList<>();
    for (Task task : db.getTasks()) {
      if (!scopeCategory.equals(task.category)) continue;
      if (!diaryMode && !ibadahMode && selectedAgendaDay != null && task.dueAt > 0) {
        LocalDate due =
            java.time.Instant.ofEpochMilli(task.dueAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        if (!due.equals(selectedAgendaDay)
            && !(selectedAgendaDay.equals(LocalDate.now())
                && due.isBefore(selectedAgendaDay)
                && !task.completed)) continue;
      }
      filtered.add(task);
    }
    // Keep user insertion order within each group; structured Hifz targets precede legacy notes.
    if (diaryMode) filtered.sort(Comparator.comparing(task -> task.hifz == null));
    if (!diaryMode && !ibadahMode)
      filtered.sort(
          Comparator.comparingLong(task -> task.dueAt == 0 ? Long.MAX_VALUE : task.dueAt));
    List<Task> pending = new ArrayList<>(), done = new ArrayList<>();
    for (Task task : filtered) (task.completed ? done : pending).add(task);
    featuredTask =
        pending.isEmpty() ? null : (!diaryMode && !ibadahMode ? pending.get(0) : pending.remove(0));
    List<Task> rows = new ArrayList<>(pending);
    if (!done.isEmpty()) {
      rows.add(null); // Section control is a distinct adapter view type, never a task.
      if (completedExpanded) rows.addAll(done);
    }
    adapter.completedCount = done.size();
    adapter.items = rows;
    adapter.notifyDataSetChanged();
    bindFeatured();
    if (!diaryMode && !ibadahMode)
      agendaHeader.findViewById(R.id.agenda_feature).setVisibility(View.GONE);
    if (!diaryMode && !ibadahMode) StudyReminders.sync(this);
    int completed = done.size(), total = filtered.size();
    int percent = total == 0 ? 0 : Math.round(100f * completed / total);
    ((TextView) agendaHeader.findViewById(R.id.study_percent))
        .setText(getString(R.string.agenda_percent, percent));
    ((TextView) agendaHeader.findViewById(R.id.study_progress_label))
        .setText(
            getString(
                !diaryMode && !ibadahMode
                    ? R.string.study_scene_task_count
                    : R.string.agenda_progress,
                completed,
                total));
    ((ProgressBar) agendaHeader.findViewById(R.id.study_progress)).setProgress(percent);
    ((TextView) agendaHeader.findViewById(R.id.hifz_planned))
        .setText(getString(R.string.agenda_pending, pending.size()));
    agendaHeader.findViewById(R.id.study_summary).setVisibility(View.VISIBLE);
    agendaHeader
        .findViewById(R.id.agenda_section)
        .setVisibility(!diaryMode && !ibadahMode || !pending.isEmpty() ? View.VISIBLE : View.GONE);
    TextView empty = agendaHeader.findViewById(R.id.study_empty);
    empty.setVisibility(featuredTask == null ? View.VISIBLE : View.GONE);
    empty.setText(
        total > 0
            ? (diaryMode ? R.string.agenda_hifz_all_done : R.string.agenda_all_done)
            : (diaryMode ? R.string.study_hifz_empty : R.string.study_empty));
    if (!diaryMode && !ibadahMode && total == 0) empty.setText(R.string.study_scene_empty);
    agendaHeader
        .findViewById(R.id.ibadah_template)
        .setVisibility(ibadahMode && total == 0 ? View.VISIBLE : View.GONE);
    if (ibadahMode)
      empty.setText(
          total > 0
              ? "Ibadah hari ini sudah selesai."
              : "Belum ada checklist ibadah. Mulai satu kebiasaan baik.");
  }

  private int categoryLabel(Task task) {
    return ZikraDbHelper.CATEGORY_HIFZ.equals(task.category)
        ? R.string.study_category_hifz
        : ZikraDbHelper.CATEGORY_IBADAH.equals(task.category)
            ? R.string.study_category_ibadah
            : R.string.study_category_study;
  }

  private void bindFeatured() {
    View feature = agendaHeader.findViewById(R.id.agenda_feature);
    feature.setVisibility(featuredTask == null ? View.GONE : View.VISIBLE);
    if (featuredTask == null) return;
    // Capture the currently rendered task so rapid taps cannot complete its successor.
    Task task = featuredTask;
    TextView title = agendaHeader.findViewById(R.id.agenda_feature_title);
    TextView detail = agendaHeader.findViewById(R.id.agenda_feature_detail);
    TextView category = agendaHeader.findViewById(R.id.agenda_feature_category);
    TextView action = agendaHeader.findViewById(R.id.agenda_feature_action);
    category.setText(categoryLabel(task));
    title.setText(diaryMode && task.hifz != null ? task.hifz.chapterName : task.title);
    detail.setText(
        diaryMode
            ? (task.hifz == null
                ? getString(R.string.hifz_no_range)
                : getString(
                    R.string.hifz_range,
                    task.hifz.firstVerse,
                    task.hifz.lastVerse,
                    task.hifz.count()))
            : getString(
                ZikraDbHelper.CATEGORY_IBADAH.equals(task.category)
                    ? R.string.study_daily_ibadah
                    : ZikraDbHelper.CATEGORY_HIFZ.equals(task.category)
                        ? R.string.study_daily_hifz
                        : R.string.study_daily_study));
    if (!diaryMode && !ibadahMode)
      detail.setText(
          task.dueAt == 0
              ? "Tanpa tenggat · mulai satu sesi fokus"
              : ((task.dueAt < System.currentTimeMillis()) ? "Lewat tenggat · " : "Tenggat · ")
                  + ScheduleEditor.label(task.dueAt));
    if (diaryMode && task.hifz != null && !task.title.equals(canonical(task.hifz))) {
      detail.append("\n" + task.title);
    }
    action.setText(
        diaryMode
            ? (task.hifz == null
                ? R.string.hifz_configure
                : readingIntent(this, task.hifz).getComponent() == null
                    ? R.string.hifz_read_online
                    : R.string.hifz_read)
            : R.string.agenda_edit);
    if (!diaryMode && !ibadahMode) action.setText("Mulai fokus");
    action.setContentDescription(action.getText() + " " + title.getText());
    action.setOnClickListener(
        v -> {
          if (diaryMode && task.hifz != null) openReading(task.hifz);
          else if (!diaryMode && !ibadahMode)
            startActivity(new Intent(this, FocusTimerActivity.class).putExtra("task", task.id));
          else showEditor(task, null);
        });
    View more = agendaHeader.findViewById(R.id.agenda_feature_more);
    more.setContentDescription(getString(R.string.study_actions_for, task.title));
    more.setOnClickListener(v -> showTaskMenu(v, task));
    CheckBox check = agendaHeader.findViewById(R.id.agenda_feature_check);
    check.setOnCheckedChangeListener(null);
    check.setChecked(false);
    check.setContentDescription(getString(R.string.study_complete_task, task.title));
    check.setOnCheckedChangeListener(
        (button, checked) -> {
          if (checked) {
            if (!db.setCompleted(task.id, true)) toast(getString(R.string.planner_stale_action));
            refreshTasks();
          }
        });
  }

  private void loadCatalog() {
    if (catalog == null || catalogLoading || chapters.size() == 114) return;
    catalogLoading = true;
    if (hifzEditor != null) hifzEditor.catalog(chapters, true, this::loadCatalog);
    catalog.load(
        new SurahCatalog.Result() {
          @Override
          public void ready(List<Chapter> values) {
            chapters = values;
            catalogLoading = false;
            if (hifzEditor != null)
              hifzEditor.catalog(chapters, false, StudyActivity.this::loadCatalog);
          }

          @Override
          public void failed() {
            catalogLoading = false;
            if (hifzEditor != null)
              hifzEditor.catalog(chapters, false, StudyActivity.this::loadCatalog);
          }
        });
  }

  private static String canonical(HifzTarget target) {
    return target.chapterName + " · Ayat " + target.firstVerse + "–" + target.lastVerse;
  }

  private void showEditor(Task task, String draft) {
    if (editorDialog != null) editorDialog.dismiss();
    Dialog dialog = new Dialog(this, R.style.StudySheet);
    View editor = getLayoutInflater().inflate(R.layout.study_dialog_task, null);
    editorDialog = dialog;
    editorView = editor;
    editorTask = task;
    scheduleEditor = null;
    editorRetained = false;
    final boolean[] saveAsNew = {task == null};
    EditText title = editor.findViewById(R.id.study_editor_title);
    RadioGroup category = editor.findViewById(R.id.study_editor_category);
    title.setText(draft != null ? draft : task == null ? "" : task.title);
    category.check(choice(scopeCategory));
    category.setVisibility(View.GONE);
    editor.findViewById(R.id.study_editor_category_label).setVisibility(View.GONE);
    ((TextView) editor.findViewById(R.id.study_editor_heading))
        .setText(
            diaryMode
                ? (task == null ? R.string.study_hifz_new : R.string.study_hifz_edit)
                : (task == null ? R.string.study_refresh_new : R.string.study_edit));
    ((TextView) editor.findViewById(R.id.study_editor_save))
        .setText(diaryMode ? R.string.hifz_save : R.string.study_save_activity);
    if (ibadahMode) {
      ((TextView) editor.findViewById(R.id.study_editor_context)).setText("Checklist ibadah");
      ((TextView) editor.findViewById(R.id.study_editor_heading))
          .setText(task == null ? "Ibadah baru" : "Ubah ibadah");
      ((TextView) editor.findViewById(R.id.study_editor_description))
          .setText("Satu kebiasaan baik yang ingin kamu jaga.");
      ((TextView) editor.findViewById(R.id.study_editor_title_label)).setText("Nama ibadah");
      title.setHint("Contoh: Sholat Dzuhur");
      ((TextView) editor.findViewById(R.id.study_editor_save)).setText("Simpan ibadah");
    }
    if (diaryMode) {
      ((TextView) editor.findViewById(R.id.study_editor_context))
          .setText(R.string.study_hifz_title);
      ((TextView) editor.findViewById(R.id.study_editor_description))
          .setText(R.string.hifz_description);
      title.setHint(R.string.hifz_note_hint);
      title.setMinLines(1);
      if (draft == null
          && task != null
          && task.hifz != null
          && task.title.equals(canonical(task.hifz))) title.setText("");
      ((TextView) editor.findViewById(R.id.study_editor_title_label)).setText(R.string.hifz_note);
      ((TextView) editor.findViewById(R.id.study_editor_voice)).setText(R.string.hifz_dictate);
      ((TextView) editor.findViewById(R.id.study_editor_voice_hint))
          .setText(R.string.hifz_dictate_hint);
      category.setVisibility(View.GONE);
      editor.findViewById(R.id.study_editor_category_label).setVisibility(View.GONE);
      hifzEditor = new HifzEditor(editor, task == null ? null : task.hifz);
      hifzEditor.catalog(chapters, catalogLoading, this::loadCatalog);
      editor.findViewById(R.id.hifz_note_toggle).setVisibility(View.VISIBLE);
      showNotes(title.length() > 0);
      editor
          .findViewById(R.id.hifz_note_toggle)
          .setOnClickListener(v -> showNotes(title.getVisibility() != View.VISIBLE));
    }
    if (!diaryMode && !ibadahMode)
      scheduleEditor =
          new ScheduleEditor(
              this,
              editor,
              task,
              () -> {
                if (android.os.Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED)
                  requestPermissions(
                      new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 903);
              });
    title.setSelection(title.length());
    editor.findViewById(R.id.study_editor_close).setOnClickListener(v -> dialog.dismiss());
    editor.findViewById(R.id.study_editor_cancel).setOnClickListener(v -> dialog.dismiss());
    editor.findViewById(R.id.study_editor_voice).setOnClickListener(v -> startVoiceTask());
    editor
        .findViewById(R.id.study_editor_save)
        .setOnClickListener(
            v -> {
              String value = title.getText().toString().trim(), selected = selectedCategory();
              boolean saved = true;
              try {
                if (diaryMode) {
                  HifzTarget target = hifzEditor.target();
                  if (target == null) return;
                  if (value.isEmpty()) value = canonical(target);
                  if (saveAsNew[0]) db.addHifzTask(value, target);
                  else saved = db.updateHifzTask(task.id, value, target);
                } else {
                  if (value.isEmpty()) {
                    title.setError(getString(R.string.study_required));
                    title.requestFocus();
                    return;
                  }
                  if (scheduleEditor != null) {
                    if (!scheduleEditor.valid()) return;
                    if (saveAsNew[0])
                      db.addAcademicTask(value, scheduleEditor.dueAt(), scheduleEditor.reminder());
                    else
                      saved =
                          db.updateAcademicTask(
                              task.id, value, scheduleEditor.dueAt(), scheduleEditor.reminder());
                  } else {
                    if (saveAsNew[0]) db.addTask(value, selected);
                    else saved = db.updateTask(task.id, value, selected);
                  }
                }
              } catch (android.database.sqlite.SQLiteException error) {
                TextView status = editor.findViewById(R.id.study_editor_status);
                status.setText(R.string.planner_save_failed);
                status.setVisibility(View.VISIBLE);
                return;
              }
              if (!saved) {
                saveAsNew[0] = true;
                retainDraftForNewTask();
                refreshTasks();
                return;
              }
              if (!"All".equals(filter)) filter = selected;
              refreshTasks();
              toast(getString(R.string.study_saved));
              dialog.dismiss();
            });
    dialog.setContentView(editor);
    dialog.setOnDismissListener(
        ignored -> {
          if (editorDialog == dialog) {
            editorDialog = null;
            editorView = null;
            editorTask = null;
            hifzEditor = null;
            if (scheduleEditor != null) scheduleEditor.close();
            scheduleEditor = null;
          }
        });
    dialog.show();
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
      window.setSoftInputMode(
          WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
              | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      int width = Math.min(getResources().getDisplayMetrics().widthPixels, dp(560)),
          height = Math.round(getResources().getDisplayMetrics().heightPixels * .9f);
      editor.measure(
          View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
      window.setLayout(width, Math.min(height, editor.getMeasuredHeight()));
    }
  }

  private void retainDraftForNewTask() {
    editorTask = null;
    editorRetained = true;
    TextView status = editorView.findViewById(R.id.study_editor_status);
    status.setText(ibadahMode ? R.string.planner_stale_ibadah : R.string.planner_missing_task);
    status.setVisibility(View.VISIBLE);
    ((TextView) editorView.findViewById(R.id.study_editor_save))
        .setText(ibadahMode ? R.string.planner_add_today : R.string.planner_add_again);
  }

  private void showNotes(boolean expanded) {
    for (int id :
        new int[] {
          R.id.study_editor_title_label,
          R.id.study_editor_title,
          R.id.study_editor_voice,
          R.id.study_editor_voice_hint
        }) editorView.findViewById(id).setVisibility(expanded ? View.VISIBLE : View.GONE);
    ((TextView) editorView.findViewById(R.id.hifz_note_toggle))
        .setText(expanded ? R.string.hifz_note_hide : R.string.hifz_note_add);
  }

  @Override
  public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(code, permissions, grants);
    if (code == 903) {
      if (scheduleEditor != null) scheduleEditor.render();
      StudyReminders.sync(this);
    }
  }

  private int choice(String category) {
    return ZikraDbHelper.CATEGORY_HIFZ.equals(category)
        ? R.id.study_editor_hifz
        : ZikraDbHelper.CATEGORY_IBADAH.equals(category)
            ? R.id.study_editor_ibadah
            : R.id.study_editor_study;
  }

  private String selectedCategory() {
    return scopeCategory;
  }

  private void showTaskMenu(View anchor, Task task) {
    PopupMenu menu = new PopupMenu(this, anchor);
    menu.getMenu()
        .add(
            0,
            1,
            0,
            diaryMode
                ? "Ubah target"
                : ibadahMode ? "Ubah ibadah" : getString(R.string.study_edit));
    menu.getMenu().add(0, 2, 1, R.string.study_delete);
    if (!diaryMode && !ibadahMode) menu.getMenu().add(0, 3, 2, R.string.concept_focus_action);
    menu.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == 3)
            startActivity(new Intent(this, FocusTimerActivity.class).putExtra("task", task.id));
          else if (item.getItemId() == 1) showEditor(task, null);
          else
            new AlertDialog.Builder(this)
                .setTitle(R.string.study_delete)
                .setMessage(getString(R.string.study_delete_confirm, task.title))
                .setNegativeButton(R.string.study_cancel, null)
                .setPositiveButton(
                    R.string.study_delete,
                    (d, w) -> {
                      if (!db.deleteTask(task.id)) toast(getString(R.string.planner_stale_action));
                      refreshTasks();
                    })
                .show();
          return true;
        });
    menu.show();
  }

  private void startVoiceTask() {
    Intent intent =
        new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            .putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                getString(diaryMode ? R.string.hifz_voice_prompt : R.string.study_voice_prompt))
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    try {
      startActivityForResult(intent, REQUEST_VOICE_TASK);
    } catch (ActivityNotFoundException | SecurityException error) {
      toast(getString(R.string.study_voice_missing));
    }
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != REQUEST_VOICE_TASK || result != RESULT_OK) return;
    ArrayList<String> results =
        data == null ? null : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    if (results == null
        || results.isEmpty()
        || results.get(0) == null
        || results.get(0).trim().isEmpty()) {
      toast(getString(R.string.study_voice_empty));
      return;
    }
    if (editorDialog == null || !editorDialog.isShowing()) {
      showEditor(null, results.get(0));
      return;
    }
    if (diaryMode) showNotes(true);
    EditText title = editorView.findViewById(R.id.study_editor_title);
    title.setText(results.get(0).trim());
    title.setSelection(title.length());
  }

  public static Intent readingIntent(android.content.Context context, HifzTarget target) {
    return new Intent(context, QuranActivity.class)
        .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, target.chapterId)
        .putExtra(QuranActivity.EXTRA_TARGET_VERSE, target.firstVerse)
        .putExtra(QuranActivity.EXTRA_RANGE_FIRST, target.firstVerse)
        .putExtra(QuranActivity.EXTRA_RANGE_LAST, target.lastVerse);
  }

  private void openReading(HifzTarget target) {
    try {
      startActivity(readingIntent(this, target));
    } catch (ActivityNotFoundException error) {
      toast("Aplikasi pembaca tautan belum tersedia.");
    }
  }

  private final class TaskAdapter extends BaseAdapter {
    private List<Task> items = new ArrayList<>();
    private int completedCount;

    @Override
    public int getCount() {
      return items.size();
    }

    @Override
    public Task getItem(int position) {
      return items.get(position);
    }

    @Override
    public long getItemId(int position) {
      Task item = getItem(position);
      return item == null ? Long.MIN_VALUE : item.id;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      return getItem(position) == null ? 1 : 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return true;
    }

    @Override
    public boolean isEnabled(int position) {
      return true;
    }

    @Override
    public View getView(int position, View reusable, ViewGroup parent) {
      if (getItem(position) == null) {
        TextView toggle =
            (TextView)
                (reusable == null
                    ? getLayoutInflater().inflate(R.layout.agenda_completed, parent, false)
                    : reusable);
        toggle.setText(
            getString(
                completedExpanded
                    ? R.string.agenda_completed_open
                    : R.string.agenda_completed_closed,
                completedCount));
        toggle.setContentDescription(
            getString(
                completedExpanded
                    ? R.string.agenda_completed_collapse
                    : R.string.agenda_completed_expand,
                completedCount));
        toggle.setOnClickListener(
            v -> {
              completedExpanded = !completedExpanded;
              refreshTasks();
            });
        return toggle;
      }
      View row =
          reusable == null
              ? getLayoutInflater()
                  .inflate(
                      diaryMode
                          ? R.layout.item_hifz_target
                          : ibadahMode ? R.layout.item_task : R.layout.item_study_scene_task,
                      parent,
                      false)
              : reusable;
      Task task = getItem(position);
      TextView title = row.findViewById(R.id.study_task_title),
          detail = row.findViewById(R.id.study_task_detail),
          tag = row.findViewById(R.id.study_task_tag);
      CheckBox checkbox = row.findViewById(R.id.study_task_checkbox);
      ImageButton more = row.findViewById(R.id.study_task_more);
      title.setText(task.title);
      title.setPaintFlags(
          task.completed && !diaryMode
              ? title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
              : title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
      title.setTextColor(getColor(task.completed ? R.color.study_secondary : R.color.pine));
      detail.setText(
          task.category.equals(ZikraDbHelper.CATEGORY_HIFZ)
              ? R.string.study_daily_hifz
              : task.category.equals(ZikraDbHelper.CATEGORY_IBADAH)
                  ? R.string.study_daily_ibadah
                  : R.string.study_daily_study);
      int accent =
          ZikraDbHelper.CATEGORY_HIFZ.equals(task.category)
              ? 0xFF8B622F
              : ZikraDbHelper.CATEGORY_IBADAH.equals(task.category) ? 0xFF42715D : 0xFF15332F;
      tag.setTextColor(accent);
      View marker = row.findViewById(R.id.agenda_category_marker);
      if (marker != null) marker.setBackgroundTintList(ColorStateList.valueOf(accent));
      tag.setText(
          task.category.equals(ZikraDbHelper.CATEGORY_HIFZ)
              ? R.string.study_category_hifz
              : task.category.equals(ZikraDbHelper.CATEGORY_IBADAH)
                  ? R.string.study_category_ibadah
                  : R.string.study_category_study);
      if (!diaryMode && !ibadahMode && task.dueAt > 0)
        detail.setText("Tenggat · " + ScheduleEditor.label(task.dueAt));
      if (diaryMode) {
        HifzTarget target = task.hifz;
        title.setText(target == null ? task.title : target.chapterName);
        tag.setText(target == null ? R.string.hifz_legacy : R.string.hifz_kind);
        ((TextView) row.findViewById(R.id.hifz_number))
            .setText(target == null ? "—" : String.format(Locale.ROOT, "%03d", target.chapterId));
        ((TextView) row.findViewById(R.id.hifz_range))
            .setText(
                target == null
                    ? getString(R.string.hifz_no_range)
                    : getString(
                        R.string.hifz_range, target.firstVerse, target.lastVerse, target.count()));
        TextView note = row.findViewById(R.id.hifz_note);
        note.setText(task.title);
        note.setVisibility(
            target != null && !task.title.equals(canonical(target)) ? View.VISIBLE : View.GONE);
        detail.setText(task.completed ? R.string.hifz_done : R.string.hifz_pending);
        TextView read = row.findViewById(R.id.hifz_read);
        boolean local =
            target != null && readingIntent(StudyActivity.this, target).getComponent() != null;
        read.setText(
            target == null
                ? R.string.hifz_configure
                : local ? R.string.hifz_read : R.string.hifz_read_online);
        read.setContentDescription(
            target == null
                ? getString(R.string.hifz_configure)
                : getString(local ? R.string.hifz_read : R.string.hifz_read_online)
                    + " "
                    + target.chapterName
                    + ", ayat "
                    + target.firstVerse);
        read.setOnClickListener(
            v -> {
              if (target == null) showEditor(task, null);
              else openReading(target);
            });
      }
      checkbox.setOnCheckedChangeListener(null);
      checkbox.setChecked(task.completed);
      checkbox.setContentDescription(
          getString(
              task.completed ? R.string.study_uncomplete_task : R.string.study_complete_task,
              task.title));
      checkbox.setOnCheckedChangeListener(
          (v, checked) -> {
            if (!db.setCompleted(task.id, checked)) toast(getString(R.string.planner_stale_action));
            refreshTasks();
          });
      more.setContentDescription(getString(R.string.study_actions_for, task.title));
      more.setOnClickListener(v -> showTaskMenu(v, task));
      return row;
    }
  }
}
