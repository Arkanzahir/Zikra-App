package com.zikra.app;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;
import com.zikra.app.data.*;
import com.zikra.app.faith.*;
import com.zikra.app.reading.QuranChapters;
import com.zikra.app.ui.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** User-reviewed local entries. Dialog drafts survive recreation; saving is never automatic. */
public final class FaithActivity extends BaseActivity {
  private FaithStore store;
  private ZikraDbHelper tasks;
  private LinearLayout content;
  private LocalDate day;
  private final DateTimeFormatter date =
      DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("id", "ID"));
  private List<QuranChapters.Chapter> chapters;
  private Dialog dialog;
  private DatePickerDialog calendar;
  private Bundle draft;
  private Spinner surah, duration;
  private EditText first, last, note;
  private TextView error;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_faith);
    getWindow().setBackgroundDrawableResource(R.color.daily_paper);
    setupScreen(getString(R.string.faith_title));
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(
            R.id.toolbarBack,
            R.id.detailTitle,
            R.id.featureSubtitle,
            0,
            getString(R.string.faith_title),
            null,
            0,
            null);
    DailyUi.chrome(this, R.color.daily_mint);
    store = new FaithStore(this);
    tasks = new ZikraDbHelper(this);
    chapters = QuranChapters.all(this);
    content = findViewById(R.id.faithContent);
    day = store.today();
    if (state != null)
      try {
        day = LocalDate.parse(state.getString("day", day.toString()));
      } catch (java.time.DateTimeException ignored) {
      }
    if (day.isAfter(store.today())) day = store.today();
    Bundle pending = state == null ? null : state.getBundle("editor");
    if (state == null && getIntent().getLongExtra("target", 0) > 0) {
      long id = getIntent().getLongExtra("target", 0);
      for (Task task : tasks.getTasks())
        if (task.id == id && task.hifz != null) {
          pending = entryDraft(FaithStore.PRACTICE, null);
          pending.putInt("chapter", task.hifz.chapterId);
          pending.putString("first", "" + task.hifz.firstVerse);
          pending.putString("last", "" + task.hifz.lastVerse);
          pending.putLong("target", id);
          break;
        }
    }
    Bundle opening = pending;
    if (opening != null) content.post(() -> showEditor(opening));
  }

  @Override
  protected void onResume() {
    super.onResume();
    render();
  }

  private void render() {
    if (store == null) return;
    content.removeAllViews();
    try {
      FaithSummary summary = FaithSummary.load(store, tasks);
      LinearLayout overview = card(R.drawable.daily_mint_card);
      label(overview, getString(R.string.faith_tagline), 22, true, 0);
      label(
          overview,
          getString(R.string.faith_summary, summary.streak, summary.week),
          17,
          true,
          R.id.faithSummary);
      label(overview, getString(R.string.faith_definition), 13, false, 0);
      label(
          overview,
          getString(R.string.faith_hifz_summary, summary.hifzDone, summary.hifzTotal),
          13,
          false,
          0);
      LinearLayout plan = card(R.drawable.daily_paper_card);
      label(plan, getString(R.string.faith_khatam), 20, true, 0);
      FaithStore.Plan current = summary.plan;
      label(
          plan,
          current == null
              ? getString(R.string.faith_no_plan)
              : getString(
                  R.string.faith_plan_summary,
                  summary.read,
                  Math.round(summary.read * 100f / FaithProgress.TOTAL),
                  current.days,
                  date.format(current.start),
                  Math.max(
                      0,
                      FaithProgress.scheduled(current.start, current.days, store.today())
                          - summary.read)),
          15,
          false,
          R.id.faithPlanSummary);
      ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
      progress.setMax(FaithProgress.TOTAL);
      progress.setProgress(summary.read);
      plan.addView(progress, new LinearLayout.LayoutParams(-1, dp(6)));
      label(plan, getString(R.string.faith_plan_method), 12, false, 0);
      button(plan, R.string.faith_plan_edit, R.id.faithPlanEdit, () -> plan(false));
      if (current != null) {
        button(
            plan,
            R.string.faith_record_reading,
            R.id.faithRead,
            () -> showEditor(entryDraft(FaithStore.READING, null)));
        if (summary.read < FaithProgress.TOTAL)
          button(plan, R.string.faith_open_next, R.id.faithNext, () -> openNext());
        else label(plan, getString(R.string.faith_plan_complete), 14, true, 0);
        button(
            plan,
            R.string.faith_plan_new,
            R.id.faithPlanNew,
            () ->
                dialog =
                    new AlertDialog.Builder(this)
                        .setMessage(R.string.faith_plan_new_confirm)
                        .setNegativeButton(R.string.faith_cancel, null)
                        .setPositiveButton(R.string.faith_plan_new, (d, w) -> plan(true))
                        .show());
      }
      LinearLayout journal = card(R.drawable.daily_white_card);
      label(journal, getString(R.string.faith_day), 20, true, 0);
      button(
          journal,
          getString(R.string.faith_select_day, date.format(day)),
          R.id.faithDate,
          this::selectDay);
      ZikraDbHelper.Progress worship = tasks.worshipHistory().get(day);
      label(
          journal,
          getString(
              R.string.faith_worship_day,
              worship == null ? 0 : worship.completed,
              worship == null ? 0 : worship.total),
          14,
          false,
          R.id.faithWorship);
      button(
          journal,
          R.string.faith_checklist,
          0,
          () ->
              startActivity(
                  new Intent(this, StudyActivity.class)
                      .putExtra("category", ZikraDbHelper.CATEGORY_IBADAH)));
      String journalNote = store.note(day);
      label(
          journal,
          journalNote.isEmpty() ? getString(R.string.faith_empty_note) : journalNote,
          14,
          false,
          R.id.faithNoteText);
      button(
          journal,
          R.string.faith_note,
          R.id.faithNote,
          () -> {
            Bundle b = new Bundle();
            b.putInt("type", 3);
            b.putString("day", day.toString());
            b.putString("note", store.note(day));
            showEditor(b);
          });
      button(
          journal,
          R.string.faith_record_practice,
          R.id.faithPractice,
          () -> showEditor(entryDraft(FaithStore.PRACTICE, null)));
      LinearLayout history = card(R.drawable.daily_white_card);
      history.setId(R.id.faithEntries);
      label(history, getString(R.string.faith_history), 20, true, 0);
      List<FaithStore.Entry> entries = store.entries(day);
      if (entries.isEmpty()) label(history, getString(R.string.faith_empty_history), 14, false, 0);
      for (FaithStore.Entry entry : entries) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(12), 0, dp(16));
        row.setTag("entry-" + entry.id);
        history.addView(row);
        label(
            row,
            getString(
                R.string.faith_entry_title,
                getString(
                    entry.kind == FaithStore.PRACTICE
                        ? R.string.faith_practice_label
                        : R.string.faith_reading_label),
                chapters.get(entry.chapter - 1).name),
            16,
            true,
            0);
        String source =
            entry.kind == FaithStore.READING && current != null && current.id != entry.planId
                ? getString(R.string.faith_archived)
                : date.format(entry.day);
        label(row, getString(R.string.faith_range, entry.first, entry.last, source), 13, false, 0);
        if (!entry.note.isEmpty()) {
          TextView text = label(row, entry.note, 14, false, 0);
          text.setMaxLines(4);
          text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }
        button(row, R.string.faith_edit, 0, () -> showEditor(entryDraft(entry.kind, entry)));
        button(row, R.string.faith_delete, 0, () -> confirmDelete(entry));
      }
    } catch (android.database.sqlite.SQLiteException
        | IllegalArgumentException
        | java.time.DateTimeException exception) {
      content.removeAllViews();
      label(content, getString(R.string.faith_unavailable), 16, false, 0);
    }
  }

  private Bundle entryDraft(int type, FaithStore.Entry entry) {
    Bundle b = new Bundle();
    b.putInt("type", type);
    b.putString("day", entry == null ? day.toString() : entry.day.toString());
    b.putLong("id", entry == null ? 0 : entry.id);
    b.putInt("revision", entry == null ? 0 : entry.revision);
    b.putString("token", entry == null ? UUID.randomUUID().toString() : entry.token);
    b.putInt("chapter", entry == null ? 1 : entry.chapter);
    b.putString("first", entry == null ? "1" : "" + entry.first);
    b.putString("last", entry == null ? "1" : "" + entry.last);
    b.putString("note", entry == null ? "" : entry.note);
    b.putLong("target", entry == null ? 0 : entry.targetId);
    FaithStore.Plan p = store.plan();
    b.putLong("plan", entry == null ? (p == null ? 0 : p.id) : entry.planId);
    return b;
  }

  private void plan(boolean newCycle) {
    FaithStore.Plan p = store.plan();
    Bundle b = new Bundle();
    b.putInt("type", 4);
    b.putBoolean("new", newCycle);
    b.putLong("plan", p == null ? 0 : p.id);
    b.putString("day", p == null || newCycle ? store.today().toString() : p.start.toString());
    b.putInt("days", p == null ? 30 : p.days);
    showEditor(b);
  }

  private void showEditor(Bundle b) {
    if (isFinishing() || isDestroyed()) return;
    if (dialog != null) dialog.dismiss();
    draft = new Bundle(b);
    surah = null;
    duration = null;
    first = null;
    last = null;
    note = null;
    int type = b.getInt("type");
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(20), dp(8), dp(20), dp(12));
    label(
        body,
        getString(R.string.faith_select_day, date.format(LocalDate.parse(b.getString("day")))),
        13,
        false,
        0);
    if (type == 4) {
      label(body, getString(R.string.faith_plan_days), 14, true, 0);
      duration = new Spinner(this);
      duration.setId(R.id.faithEditorDays);
      String[] names = {
        getString(R.string.faith_days, 30),
        getString(R.string.faith_days, 90),
        getString(R.string.faith_days, 365)
      };
      duration.setAdapter(
          new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
      body.addView(duration, new LinearLayout.LayoutParams(-1, dp(56)));
      duration.setSelection(b.getInt("days") == 365 ? 2 : b.getInt("days") == 90 ? 1 : 0);
      label(body, getString(R.string.faith_plan_method), 13, false, 0);
    } else if (type == 3) {
      note = input(body, R.string.faith_note_hint, R.id.faithEditorNote, false);
      note.setText(b.getString("note", ""));
    } else {
      label(body, getString(R.string.faith_explicit), 13, false, 0);
      TextView surahLabel = label(body, getString(R.string.faith_surah), 14, true, 0);
      surahLabel.setLabelFor(R.id.faithEditorSurah);
      surah = new Spinner(this);
      surah.setId(R.id.faithEditorSurah);
      List<String> names = new ArrayList<>();
      for (QuranChapters.Chapter c : chapters)
        names.add(getString(R.string.faith_catalog_item, c.id, c.name, c.count));
      surah.setAdapter(
          new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
      body.addView(surah, new LinearLayout.LayoutParams(-1, -2));
      surah.setMinimumHeight(dp(56));
      surah.setSelection(b.getInt("chapter", 1) - 1);
      first = input(body, R.string.faith_first, R.id.faithEditorFirst, true);
      last = input(body, R.string.faith_last, R.id.faithEditorLast, true);
      first.setText(b.getString("first", "1"));
      last.setText(b.getString("last", "1"));
      note = input(body, R.string.faith_entry_hint, R.id.faithEditorNote, false);
      note.setText(b.getString("note", ""));
    }
    error = label(body, "", 13, false, R.id.faithEditorError);
    error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(false);
    scroll.addView(body);
    int title =
        type == 4
            ? R.string.faith_plan_edit
            : type == 3
                ? R.string.faith_note
                : type == FaithStore.PRACTICE
                    ? R.string.faith_record_practice
                    : R.string.faith_record_reading;
    LinearLayout sheet = new LinearLayout(this);
    sheet.setOrientation(LinearLayout.VERTICAL);
    sheet.setBackgroundResource(R.drawable.daily_white_card);
    sheet.setPadding(dp(4), dp(12), dp(4), dp(8));
    TextView heading = label(sheet, getString(title), 19, true, 0);
    heading.setPadding(dp(20), dp(4), dp(20), dp(12));
    sheet.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout footer = new LinearLayout(this);
    footer.setPadding(dp(12), dp(8), dp(12), dp(4));
    sheet.addView(footer, new LinearLayout.LayoutParams(-1, -2));
    Button cancel = editorAction(footer, R.string.faith_cancel, R.id.faithEditorCancel);
    Button save = editorAction(footer, R.string.faith_save, R.id.faithEditorSave);
    save.setBackgroundResource(R.drawable.concept_gold_button);
    Dialog created = new Dialog(this);
    dialog = created;
    created.requestWindowFeature(Window.FEATURE_NO_TITLE);
    created.setContentView(sheet);
    cancel.setOnClickListener(v -> created.dismiss());
    save.setOnClickListener(v -> saveEditor());
    created.setOnDismissListener(
        d -> {
          if (dialog == created) draft = null;
        });
    created.show();
    Window window = created.getWindow();
    window.setBackgroundDrawableResource(android.R.color.transparent);
    window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    window.setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    Runnable fit =
        () -> {
          if (!created.isShowing()) return;
          android.graphics.Rect frame = new android.graphics.Rect();
          sheet.getWindowVisibleDisplayFrame(frame);
          int full = getResources().getDisplayMetrics().heightPixels;
          int available = frame.height() > 0 ? frame.height() : full;
          if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsets insets = sheet.getRootWindowInsets();
            if (insets != null && insets.isVisible(WindowInsets.Type.ime())) {
              int occupied =
                  insets.getInsets(WindowInsets.Type.ime()).bottom
                      + insets.getInsets(WindowInsets.Type.statusBars()).top;
              available = Math.min(available, full - occupied);
            }
          }
          int height = Math.max(dp(120), Math.min(Math.round(full * .88f), available - dp(12)));
          int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(560));
          if (window.getAttributes().height != height || window.getAttributes().width != width)
            window.setLayout(width, height);
        };
    sheet.getViewTreeObserver().addOnGlobalLayoutListener(fit::run);
    sheet.post(fit);
  }

  private Button editorAction(LinearLayout parent, int text, int id) {
    Button action = new Button(this);
    action.setId(id);
    action.setText(text);
    action.setAllCaps(false);
    action.setTextSize(14);
    action.setMinHeight(dp(48));
    action.setTextColor(getColor(R.color.text));
    action.setBackgroundResource(R.drawable.concept_mint_card);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
    params.setMargins(dp(4), 0, dp(4), 0);
    parent.addView(action, params);
    return action;
  }

  private Bundle snapshot() {
    Bundle b = new Bundle(draft);
    if (surah != null) {
      b.putInt("chapter", surah.getSelectedItemPosition() + 1);
      b.putString("first", first.getText().toString());
      b.putString("last", last.getText().toString());
    }
    if (note != null) b.putString("note", note.getText().toString());
    if (duration != null)
      b.putInt("days", new int[] {30, 90, 365}[duration.getSelectedItemPosition()]);
    return b;
  }

  private void saveEditor() {
    if (draft == null || dialog == null || !dialog.isShowing()) return;
    try {
      Bundle b = snapshot();
      int type = b.getInt("type");
      LocalDate date = LocalDate.parse(b.getString("day"));
      if (type == 4) store.savePlan(b.getInt("days"), date, b.getBoolean("new"), b.getLong("plan"));
      else if (type == 3) store.note(date, b.getString("note", ""));
      else
        store.save(
            new FaithStore.Entry(
                b.getLong("id"),
                b.getInt("revision"),
                b.getString("token"),
                type,
                date,
                b.getInt("chapter"),
                Integer.parseInt(b.getString("first")),
                Integer.parseInt(b.getString("last")),
                b.getString("note", ""),
                b.getLong("target"),
                type == FaithStore.READING ? b.getLong("plan") : 0));
      dialog.dismiss();
      render();
      Toast.makeText(this, R.string.faith_saved, Toast.LENGTH_SHORT).show();
    } catch (IllegalArgumentException | IllegalStateException failure) {
      error.setText(
          failure instanceof NumberFormatException
              ? getString(
                  R.string.hifz_invalid, chapters.get(surah.getSelectedItemPosition()).count)
              : failure.getMessage());
      error.requestRectangleOnScreen(
          new android.graphics.Rect(0, 0, error.getWidth(), error.getHeight()));
    } catch (android.database.sqlite.SQLiteException failure) {
      error.setText(R.string.faith_unavailable);
    }
  }

  private void selectDay() {
    calendar =
        new DatePickerDialog(
            this,
            (v, y, m, d) -> {
              day = LocalDate.of(y, m + 1, d);
              render();
            },
            day.getYear(),
            day.getMonthValue() - 1,
            day.getDayOfMonth());
    calendar
        .getDatePicker()
        .setMaxDate(
            store
                .today()
                .atTime(23, 59)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli());
    calendar.show();
  }

  private void openNext() {
    FaithStore.Plan p = store.plan();
    if (p == null) return;
    int global = store.coverage(p.id).nextClearBit(0) + 1;
    if (global > FaithProgress.TOTAL) return;
    int[] reference = FaithProgress.reference(global);
    startActivity(
        new Intent(this, QuranActivity.class)
            .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, reference[0])
            .putExtra(QuranActivity.EXTRA_TARGET_VERSE, reference[1]));
  }

  private void confirmDelete(FaithStore.Entry entry) {
    dialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.faith_delete)
            .setMessage(R.string.faith_delete_confirm)
            .setNegativeButton(R.string.faith_cancel, null)
            .setPositiveButton(
                R.string.faith_delete,
                (d, w) -> {
                  try {
                    if (!store.delete(entry))
                      Toast.makeText(this, R.string.faith_stale, Toast.LENGTH_LONG).show();
                    render();
                  } catch (android.database.sqlite.SQLiteException failure) {
                    Toast.makeText(this, R.string.faith_unavailable, Toast.LENGTH_LONG).show();
                  }
                })
            .show();
  }

  private LinearLayout card(int background) {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(16), dp(16), dp(16), dp(16));
    card.setBackgroundResource(background);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(12);
    content.addView(card, lp);
    return card;
  }

  private TextView label(LinearLayout parent, String text, int size, boolean heading, int id) {
    TextView view = new TextView(this);
    view.setId(id == 0 ? View.NO_ID : id);
    view.setText(text);
    view.setTextSize(size);
    view.setTextColor(getColor(heading ? R.color.pine : R.color.concept_muted));
    view.setPadding(0, dp(4), 0, dp(8));
    if (heading) {
      view.setTypeface(
          getResources().getFont(R.font.plus_jakarta_sans), android.graphics.Typeface.BOLD);
      if (android.os.Build.VERSION.SDK_INT >= 28) view.setAccessibilityHeading(true);
    }
    parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
    return view;
  }

  private void button(LinearLayout parent, int text, int id, Runnable action) {
    button(parent, getString(text), id, action);
  }

  private void button(LinearLayout parent, String text, int id, Runnable action) {
    Button button = new Button(this);
    button.setText(text);
    button.setTextSize(14);
    button.setAllCaps(false);
    button.setTextColor(getColor(R.color.text));
    button.setBackgroundResource(R.drawable.concept_mint_card);
    button.setMinimumHeight(dp(48));
    button.setId(id == 0 ? View.NO_ID : id);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(8);
    parent.addView(button, lp);
    button.setOnClickListener(
        v -> {
          try {
            action.run();
          } catch (android.database.sqlite.SQLiteException
              | IllegalArgumentException
              | java.time.DateTimeException failure) {
            Toast.makeText(this, R.string.faith_unavailable, Toast.LENGTH_LONG).show();
          }
        });
  }

  private EditText input(LinearLayout parent, int hint, int id, boolean numeric) {
    TextView title =
        label(parent, getString(numeric ? hint : R.string.faith_note_label), 13, numeric, 0);
    title.setLabelFor(id);
    EditText field = new EditText(this);
    field.setId(id);
    field.setTextSize(16);
    field.setTextColor(getColor(R.color.text));
    field.setMinimumHeight(dp(48));
    field.setInputType(
        numeric
            ? android.text.InputType.TYPE_CLASS_NUMBER
            : android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    field.setFilters(new InputFilter[] {new InputFilter.LengthFilter(numeric ? 3 : 1000)});
    if (!numeric) {
      field.setHint(hint);
      field.setMinLines(2);
      field.setMaxLines(4);
    }
    parent.addView(field, new LinearLayout.LayoutParams(-1, -2));
    return field;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putString("day", day.toString());
    if (draft != null && dialog != null && dialog.isShowing())
      state.putBundle("editor", snapshot());
  }

  @Override
  protected void onDestroy() {
    if (dialog != null) dialog.dismiss();
    if (calendar != null) calendar.dismiss();
    if (store != null) store.close();
    if (tasks != null) tasks.close();
    super.onDestroy();
  }
}
