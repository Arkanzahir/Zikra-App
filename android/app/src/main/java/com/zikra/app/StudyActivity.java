package com.zikra.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import com.zikra.app.data.Task;
import com.zikra.app.data.ZikraDbHelper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Daily SQLite checklist, including CRUD and native speech-to-task entry. */
public final class StudyActivity extends BaseActivity {
  private static final int REQUEST_VOICE_TASK = 100;
  private static final String STATE_FILTER = "study_filter";
  private static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");
  private final Handler handler = new Handler(Looper.getMainLooper());
  private ZikraDbHelper db;
  private TaskAdapter adapter;
  private String filter = "All";
  private boolean diaryMode;
  private LocalDate displayedDay;
  private final Runnable dayChangeCheck =
      new Runnable() {
        @Override
        public void run() {
          if (!LocalDate.now().equals(displayedDay)) {
            renderWeek();
            refreshTasks();
          }
          handler.postDelayed(this, 60_000);
        }
      };

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_study);
    NavDock.attach(this, (android.widget.FrameLayout) findViewById(R.id.studyPageRoot), "study");
    diaryMode = ZikraDbHelper.CATEGORY_HIFZ.equals(getIntent().getStringExtra("category"));
    setupScreen(getString(diaryMode ? R.string.study_hifz_title : R.string.study_title));
    if (diaryMode) {
      ((TextView) findViewById(R.id.study_screen_title)).setText(R.string.study_hifz_title);
      ((TextView) findViewById(R.id.study_screen_tagline)).setText(R.string.study_hifz_tagline);
      filter = ZikraDbHelper.CATEGORY_HIFZ;
    }
    db = new ZikraDbHelper(this);
    if (state != null) filter = state.getString(STATE_FILTER, "All");
    if (diaryMode) {
      filter = ZikraDbHelper.CATEGORY_HIFZ;
      for (int id :
          new int[] {R.id.study_filter_all, R.id.study_filter_study, R.id.study_filter_ibadah})
        findViewById(id).setVisibility(View.GONE);
      ((TextView) findViewById(R.id.study_add)).setText(R.string.study_hifz_add);
      ((TextView) findViewById(R.id.study_empty)).setText(R.string.study_hifz_empty);
    }
    adapter = new TaskAdapter();
    ListView list = findViewById(R.id.study_tasks);
    list.setAdapter(adapter);
    list.setEmptyView(findViewById(R.id.study_empty));
    list.setOnItemClickListener(
        (parent, view, position, id) -> showEditor(adapter.getItem(position), null));
    findViewById(R.id.study_back)
        .setOnClickListener(
            view -> {
              Intent intent = new Intent(this, HomeActivity.class);
              intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
              startActivity(intent);
              finish();
            });
    findViewById(R.id.study_add).setOnClickListener(view -> showEditor(null, null));
    findViewById(R.id.study_voice).setOnClickListener(view -> startVoiceTask());
    findViewById(R.id.study_filter_all).setOnClickListener(view -> selectFilter("All"));
    findViewById(R.id.study_filter_study)
        .setOnClickListener(view -> selectFilter(ZikraDbHelper.CATEGORY_STUDY));
    findViewById(R.id.study_filter_ibadah)
        .setOnClickListener(view -> selectFilter(ZikraDbHelper.CATEGORY_IBADAH));
    findViewById(R.id.study_filter_hifz)
        .setOnClickListener(view -> selectFilter(ZikraDbHelper.CATEGORY_HIFZ));
  }

  @Override
  protected void onResume() {
    super.onResume();
    renderWeek();
    refreshTasks();
    handler.removeCallbacks(dayChangeCheck);
    handler.postDelayed(dayChangeCheck, 60_000);
  }

  @Override
  protected void onPause() {
    handler.removeCallbacks(dayChangeCheck);
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString(STATE_FILTER, filter);
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (db != null) db.close();
    super.onDestroy();
  }

  private void renderWeek() {
    displayedDay = LocalDate.now();
    LocalDate monday = displayedDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LinearLayout week = findViewById(R.id.study_week);
    week.removeAllViews();
    for (int i = 0; i < 7; i++) {
      LocalDate day = monday.plusDays(i);
      View item = getLayoutInflater().inflate(R.layout.study_item_day, week, false);
      TextView name = item.findViewById(R.id.study_day_name);
      TextView number = item.findViewById(R.id.study_day_number);
      name.setText(day.format(DateTimeFormatter.ofPattern("EE", INDONESIAN)));
      number.setText(String.valueOf(day.getDayOfMonth()));
      item.setContentDescription(
          day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", INDONESIAN)));
      if (day.equals(displayedDay)) {
        item.setBackgroundResource(R.drawable.bg_pine);
        name.setTextColor(getColor(R.color.white));
        number.setTextColor(getColor(R.color.white));
        item.findViewById(R.id.study_day_dot).setVisibility(View.VISIBLE);
      }
      week.addView(item);
    }
  }

  private void selectFilter(String selected) {
    filter = selected;
    refreshTasks();
  }

  private void refreshTasks() {
    List<Task> filtered = new ArrayList<>();
    for (Task task : db.getTasks()) {
      if ("All".equals(filter) || task.category.equals(filter)) filtered.add(task);
    }
    adapter.items = filtered;
    adapter.notifyDataSetChanged();
    ZikraDbHelper.Progress progress = db.getProgress();
    int completed = progress.completed, total = progress.total, percent = progress.percent;
    if (diaryMode) {
      total = filtered.size();
      completed = 0;
      for (Task task : filtered) if (task.completed) completed++;
      percent = total == 0 ? 0 : Math.round(100f * completed / total);
    }
    ((TextView) findViewById(R.id.study_percent))
        .setText(getString(R.string.study_percent, percent));
    ((TextView) findViewById(R.id.study_progress_label))
        .setText(
            getString(
                diaryMode ? R.string.study_hifz_progress_label : R.string.study_progress_label,
                completed,
                total));
    ((ProgressBar) findViewById(R.id.study_progress)).setProgress(percent, true);
    styleFilter(R.id.study_filter_all, "All");
    styleFilter(R.id.study_filter_study, ZikraDbHelper.CATEGORY_STUDY);
    styleFilter(R.id.study_filter_ibadah, ZikraDbHelper.CATEGORY_IBADAH);
    styleFilter(R.id.study_filter_hifz, ZikraDbHelper.CATEGORY_HIFZ);
  }

  private void styleFilter(int viewId, String category) {
    TextView tab = findViewById(viewId);
    boolean selected = filter.equals(category);
    tab.setSelected(selected);
    tab.setTextColor(getColor(selected ? R.color.pine : R.color.muted));
    tab.setBackgroundResource(selected ? R.drawable.bg_card : android.R.color.transparent);
  }

  private void showEditor(Task task, String recognizedTitle) {
    View editor = getLayoutInflater().inflate(R.layout.study_dialog_task, null);
    EditText title = editor.findViewById(R.id.study_editor_title);
    Spinner category = editor.findViewById(R.id.study_editor_category);
    if (task != null) {
      title.setText(task.title);
      category.setSelection(categoryPosition(task.category));
    } else {
      title.setText(recognizedTitle == null ? "" : recognizedTitle);
      category.setSelection(
          recognizedTitle != null && !diaryMode
              ? 0
              : categoryPosition(
                  "All".equals(filter) && diaryMode ? ZikraDbHelper.CATEGORY_HIFZ : filter));
    }
    title.setSelection(title.length());
    if (diaryMode) {
      title.setHint(R.string.study_hifz_hint);
      category.setSelection(categoryPosition(ZikraDbHelper.CATEGORY_HIFZ));
      category.setEnabled(false);
    }
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(
                diaryMode
                    ? (task == null ? R.string.study_hifz_new : R.string.study_hifz_edit)
                    : (task == null ? R.string.study_new : R.string.study_edit))
            .setView(editor)
            .setNegativeButton(R.string.study_cancel, null)
            .setPositiveButton(R.string.study_save, null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    view -> {
                      String text = title.getText().toString().trim();
                      if (text.isEmpty()) {
                        title.setError(getString(R.string.study_required));
                        title.requestFocus();
                        return;
                      }
                      String selectedCategory =
                          category.getSelectedItemPosition() == 2
                              ? ZikraDbHelper.CATEGORY_HIFZ
                              : category.getSelectedItemPosition() == 1
                                  ? ZikraDbHelper.CATEGORY_IBADAH
                                  : ZikraDbHelper.CATEGORY_STUDY;
                      if (task == null) db.addTask(text, selectedCategory);
                      else db.updateTask(task.id, text, selectedCategory);
                      // Keep the saved item visible if its category changed under an active filter.
                      if (!"All".equals(filter)) filter = selectedCategory;
                      refreshTasks();
                      toast(getString(R.string.study_saved));
                      dialog.dismiss();
                    }));
    dialog.show();
  }

  private int categoryPosition(String category) {
    return ZikraDbHelper.CATEGORY_HIFZ.equals(category)
        ? 2
        : ZikraDbHelper.CATEGORY_IBADAH.equals(category) ? 1 : 0;
  }

  private void showTaskMenu(View anchor, Task task) {
    PopupMenu menu = new PopupMenu(this, anchor);
    menu.getMenu().add(0, 1, 0, R.string.study_edit);
    menu.getMenu().add(0, 2, 1, R.string.study_delete);
    menu.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == 1) showEditor(task, null);
          else
            new AlertDialog.Builder(this)
                .setTitle(R.string.study_delete)
                .setMessage(getString(R.string.study_delete_confirm, task.title))
                .setNegativeButton(R.string.study_cancel, null)
                .setPositiveButton(
                    R.string.study_delete,
                    (dialog, which) -> {
                      db.deleteTask(task.id);
                      refreshTasks();
                      toast(getString(R.string.study_deleted));
                    })
                .show();
          return true;
        });
    menu.show();
  }

  private void startVoiceTask() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.study_voice_prompt));
    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    if (intent.resolveActivity(getPackageManager()) == null) {
      toast(getString(R.string.study_voice_missing));
      showEditor(null, null);
      return;
    }
    try {
      // Recognition is delegated to the system provider; this app never records audio.
      startActivityForResult(intent, REQUEST_VOICE_TASK);
    } catch (ActivityNotFoundException | SecurityException exception) {
      toast(getString(R.string.study_voice_missing));
      showEditor(null, null);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != REQUEST_VOICE_TASK || resultCode != RESULT_OK) return;
    ArrayList<String> results =
        data == null ? null : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    if (results == null
        || results.isEmpty()
        || results.get(0) == null
        || results.get(0).trim().isEmpty()) {
      toast(getString(R.string.study_voice_empty));
      return;
    }
    // Let the user correct recognition before committing the task to SQLite.
    showEditor(null, results.get(0));
  }

  private final class TaskAdapter extends BaseAdapter {
    private List<Task> items = new ArrayList<>();

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
      return getItem(position).id;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row =
          convertView == null
              ? LayoutInflater.from(StudyActivity.this).inflate(R.layout.item_task, parent, false)
              : convertView;
      Task task = getItem(position);
      TextView title = row.findViewById(R.id.study_task_title);
      TextView detail = row.findViewById(R.id.study_task_detail);
      TextView tag = row.findViewById(R.id.study_task_tag);
      CheckBox checkbox = row.findViewById(R.id.study_task_checkbox);
      ImageButton more = row.findViewById(R.id.study_task_more);
      title.setText(task.title);
      title.setPaintFlags(
          task.completed
              ? title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
              : title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
      title.setTextColor(getColor(task.completed ? R.color.muted : R.color.pine));
      detail.setText(
          ZikraDbHelper.CATEGORY_HIFZ.equals(task.category)
              ? R.string.study_daily_hifz
              : ZikraDbHelper.CATEGORY_IBADAH.equals(task.category)
                  ? R.string.study_daily_ibadah
                  : R.string.study_daily_study);
      tag.setText(task.completed ? R.string.study_done : R.string.study_today);
      checkbox.setOnCheckedChangeListener(null);
      checkbox.setChecked(task.completed);
      checkbox.setContentDescription(
          getString(
              task.completed ? R.string.study_uncomplete_task : R.string.study_complete_task,
              task.title));
      checkbox.setOnCheckedChangeListener(
          (button, checked) -> {
            db.setCompleted(task.id, checked);
            refreshTasks();
          });
      more.setContentDescription(getString(R.string.study_actions_for, task.title));
      more.setOnClickListener(view -> showTaskMenu(view, task));
      return row;
    }
  }
}
