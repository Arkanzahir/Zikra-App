package com.zikra.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import com.zikra.app.reading.SurahData;
import com.zikra.app.study.SurahCatalog;
import com.zikra.app.study.SurahCatalog.Chapter;
import com.zikra.app.ui.HifzEditor;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Daily checklist and structured Hifz targets; the header has no duplicate add action. */
public final class StudyActivity extends BaseActivity {
  private static final int REQUEST_VOICE_TASK = 100;
  private static final Locale ID = Locale.forLanguageTag("id-ID");
  private final Handler handler = new Handler(Looper.getMainLooper());
  private ZikraDbHelper db;
  private TaskAdapter adapter;
  private boolean diaryMode, catalogLoading;
  private String filter = "All";
  private LocalDate displayedDay;
  private Dialog editorDialog;
  private View editorView;
  private Task editorTask;
  private HifzEditor hifzEditor;
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
    setContentView(R.layout.activity_study);
    NavDock.attach(this, findViewById(R.id.studyPageRoot), "study");
    diaryMode = ZikraDbHelper.CATEGORY_HIFZ.equals(getIntent().getStringExtra("category"));
    setupScreen(getString(diaryMode ? R.string.study_hifz_title : R.string.study_title));
    getWindow().setStatusBarColor(getColor(R.color.white));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    db = new ZikraDbHelper(this);
    filter = state == null ? "All" : state.getString("study_filter", "All");
    if (diaryMode) {
      filter = ZikraDbHelper.CATEGORY_HIFZ;
      text(R.id.study_screen_title, R.string.study_hifz_title);
      text(R.id.study_screen_tagline, R.string.study_hifz_tagline);
      text(R.id.study_heading, R.string.study_refresh_hifz_heading);
      text(R.id.study_section, R.string.hifz_section);
      text(R.id.study_add, R.string.study_hifz_add);
      text(R.id.study_empty, R.string.study_hifz_empty);
      findViewById(R.id.study_filters).setVisibility(View.GONE);
      for (int id :
          new int[] {R.id.study_filter_all, R.id.study_filter_study, R.id.study_filter_ibadah})
        findViewById(id).setVisibility(View.GONE);
      findViewById(R.id.studyPageRoot).setBackgroundColor(getColor(R.color.study_warm));
      findViewById(R.id.study_summary).setBackgroundResource(R.drawable.hifz_summary);
      for (int id : new int[] {R.id.study_heading, R.id.study_percent})
        ((TextView) findViewById(id)).setTextColor(getColor(R.color.white));
      ((TextView) findViewById(R.id.study_progress_label)).setTextColor(0xFFD3E0D8);
      ((ProgressBar) findViewById(R.id.study_progress))
          .setProgressTintList(ColorStateList.valueOf(getColor(R.color.camel)));
      ((ProgressBar) findViewById(R.id.study_progress))
          .setProgressBackgroundTintList(ColorStateList.valueOf(0xFF3D5950));
      findViewById(R.id.hifz_planned).setVisibility(View.VISIBLE);
      catalog = new SurahCatalog();
      loadCatalog();
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
              startActivity(
                  new Intent(this, HomeActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
              finish();
            });
    findViewById(R.id.study_add).setOnClickListener(view -> showEditor(null, null));
    int[] ids = {
      R.id.study_filter_all,
      R.id.study_filter_study,
      R.id.study_filter_ibadah,
      R.id.study_filter_hifz
    };
    String[] categories = {
      "All",
      ZikraDbHelper.CATEGORY_STUDY,
      ZikraDbHelper.CATEGORY_IBADAH,
      ZikraDbHelper.CATEGORY_HIFZ
    };
    for (int i = 0; i < ids.length; i++) {
      final String category = categories[i];
      findViewById(ids[i])
          .setOnClickListener(
              v -> {
                filter = category;
                refreshTasks();
              });
    }
    if (state != null && state.getBoolean("editor_open", false)) {
      long id = state.getLong("editor_task_id", -1);
      Task saved = null;
      for (Task task : db.getTasks()) if (task.id == id) saved = task;
      if (id == -1 || saved != null) {
        showEditor(saved, state.getString("editor_title", ""));
        ((RadioGroup) editorView.findViewById(R.id.study_editor_category))
            .check(choice(state.getString("editor_category", ZikraDbHelper.CATEGORY_STUDY)));
        if (hifzEditor != null)
          hifzEditor.restore(state, chapters, catalogLoading, this::loadCatalog);
      }
    }
  }

  private void text(int id, int value) {
    ((TextView) findViewById(id)).setText(value);
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
    if (editorDialog != null && editorDialog.isShowing()) {
      state.putBoolean("editor_open", true);
      state.putLong("editor_task_id", editorTask == null ? -1 : editorTask.id);
      state.putString(
          "editor_title",
          ((EditText) editorView.findViewById(R.id.study_editor_title)).getText().toString());
      state.putString("editor_category", selectedCategory());
      if (hifzEditor != null) hifzEditor.save(state);
    }
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (editorDialog != null) editorDialog.dismiss();
    if (catalog != null) catalog.close();
    if (db != null) db.close();
    super.onDestroy();
  }

  private void renderDate() {
    displayedDay = LocalDate.now();
    String date = displayedDay.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID));
    ((TextView) findViewById(R.id.study_date))
        .setText(date.substring(0, 1).toUpperCase(ID) + date.substring(1));
  }

  private void refreshTasks() {
    List<Task> filtered = new ArrayList<>();
    for (Task task : db.getTasks())
      if ("All".equals(filter) || task.category.equals(filter)) filtered.add(task);
    if (diaryMode) filtered.sort(Comparator.comparing(task -> task.hifz == null));
    adapter.items = filtered;
    adapter.notifyDataSetChanged();
    ZikraDbHelper.Progress progress = db.getProgress();
    int completed = progress.completed, total = progress.total;
    if (diaryMode) {
      total = filtered.size();
      completed = 0;
      int ayahs = 0;
      for (Task task : filtered) {
        if (task.completed) completed++;
        if (task.hifz != null) ayahs += task.hifz.count();
      }
      ((TextView) findViewById(R.id.hifz_planned))
          .setText(ayahs == 0 ? "" : getString(R.string.hifz_planned, ayahs));
    }
    ((TextView) findViewById(R.id.study_percent))
        .setText(
            getString(
                R.string.study_percent, total == 0 ? 0 : Math.round(100f * completed / total)));
    ((TextView) findViewById(R.id.study_progress_label))
        .setText(
            getString(
                diaryMode ? R.string.study_hifz_progress_label : R.string.study_progress_label,
                completed,
                total));
    ((ProgressBar) findViewById(R.id.study_progress))
        .setProgress(total == 0 ? 0 : Math.round(100f * completed / total));
    int[] ids = {
      R.id.study_filter_all,
      R.id.study_filter_study,
      R.id.study_filter_ibadah,
      R.id.study_filter_hifz
    };
    String[] categories = {
      "All",
      ZikraDbHelper.CATEGORY_STUDY,
      ZikraDbHelper.CATEGORY_IBADAH,
      ZikraDbHelper.CATEGORY_HIFZ
    };
    for (int i = 0; i < ids.length; i++) {
      TextView tab = findViewById(ids[i]);
      boolean selected = categories[i].equals(filter);
      tab.setSelected(selected);
      tab.setBackgroundResource(selected ? R.drawable.bg_card : android.R.color.transparent);
      tab.setTextColor(getColor(selected ? R.color.pine : R.color.study_secondary));
    }
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
    EditText title = editor.findViewById(R.id.study_editor_title);
    RadioGroup category = editor.findViewById(R.id.study_editor_category);
    title.setText(draft != null ? draft : task == null ? "" : task.title);
    category.check(
        choice(diaryMode ? ZikraDbHelper.CATEGORY_HIFZ : task == null ? filter : task.category));
    ((TextView) editor.findViewById(R.id.study_editor_heading))
        .setText(
            diaryMode
                ? (task == null ? R.string.study_hifz_new : R.string.study_hifz_edit)
                : (task == null ? R.string.study_refresh_new : R.string.study_edit));
    ((TextView) editor.findViewById(R.id.study_editor_save))
        .setText(diaryMode ? R.string.hifz_save : R.string.study_save_activity);
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
    title.setSelection(title.length());
    editor.findViewById(R.id.study_editor_close).setOnClickListener(v -> dialog.dismiss());
    editor.findViewById(R.id.study_editor_cancel).setOnClickListener(v -> dialog.dismiss());
    editor.findViewById(R.id.study_editor_voice).setOnClickListener(v -> startVoiceTask());
    editor
        .findViewById(R.id.study_editor_save)
        .setOnClickListener(
            v -> {
              String value = title.getText().toString().trim(), selected = selectedCategory();
              if (diaryMode) {
                HifzTarget target = hifzEditor.target();
                if (target == null) return;
                if (value.isEmpty()) value = canonical(target);
                if (task == null) db.addHifzTask(value, target);
                else db.updateHifzTask(task.id, value, target);
              } else {
                if (value.isEmpty()) {
                  title.setError(getString(R.string.study_required));
                  title.requestFocus();
                  return;
                }
                if (task == null) db.addTask(value, selected);
                else db.updateTask(task.id, value, selected);
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

  private int choice(String category) {
    return ZikraDbHelper.CATEGORY_HIFZ.equals(category)
        ? R.id.study_editor_hifz
        : ZikraDbHelper.CATEGORY_IBADAH.equals(category)
            ? R.id.study_editor_ibadah
            : R.id.study_editor_study;
  }

  private String selectedCategory() {
    if (diaryMode) return ZikraDbHelper.CATEGORY_HIFZ;
    int id =
        ((RadioGroup) editorView.findViewById(R.id.study_editor_category))
            .getCheckedRadioButtonId();
    return id == R.id.study_editor_hifz
        ? ZikraDbHelper.CATEGORY_HIFZ
        : id == R.id.study_editor_ibadah
            ? ZikraDbHelper.CATEGORY_IBADAH
            : ZikraDbHelper.CATEGORY_STUDY;
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
                    (d, w) -> {
                      db.deleteTask(task.id);
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
    for (SurahData surah : SurahData.ALL)
      if (surah.number == target.chapterId)
        return new Intent(context, QuranActivity.class)
            .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, target.chapterId)
            .putExtra(QuranActivity.EXTRA_TARGET_VERSE, target.firstVerse);
    return new Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://quran.com/" + target.chapterId + "/" + target.firstVerse));
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
    public View getView(int position, View reusable, ViewGroup parent) {
      View row =
          reusable == null
              ? getLayoutInflater()
                  .inflate(
                      diaryMode ? R.layout.item_hifz_target : R.layout.item_task, parent, false)
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
      tag.setText(
          task.category.equals(ZikraDbHelper.CATEGORY_HIFZ)
              ? R.string.study_category_hifz
              : task.category.equals(ZikraDbHelper.CATEGORY_IBADAH)
                  ? R.string.study_category_ibadah
                  : R.string.study_category_study);
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
            db.setCompleted(task.id, checked);
            refreshTasks();
          });
      more.setContentDescription(getString(R.string.study_actions_for, task.title));
      more.setOnClickListener(v -> showTaskMenu(v, task));
      return row;
    }
  }
}
