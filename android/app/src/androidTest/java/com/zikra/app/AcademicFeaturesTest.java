package com.zikra.app;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.widget.*;
import com.zikra.app.data.*;
import com.zikra.app.study.*;
import java.lang.reflect.Field;
import java.time.Clock;
import java.util.*;

/** Exercises durable schedules, notification guards, editor recreation and the native timer. */
@SuppressWarnings("deprecation")
public final class AcademicFeaturesTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private ZikraDbHelper db;
  private final List<Long> created = new ArrayList<>();
  private final Map<String, Map<String, ?>> saved = new HashMap<>();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    db = new ZikraDbHelper(context);
    for (String name : new String[] {FocusTimer.PREFS, "study_alarm_ids", "zikra_preferences"})
      saved.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context.getSharedPreferences(FocusTimer.PREFS, 0).edit().clear().commit();
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .commit();
  }

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }

  private Activity launch(Class<? extends Activity> type) {
    return launch(new Intent(context, type));
  }

  private Activity launch(Intent intent) {
    page = getInstrumentation().startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long until = SystemClock.uptimeMillis() + 20000;
    while (!page.hasWindowFocus() && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    assertTrue("Activity focused", page.hasWindowFocus());
    return page;
  }

  private Object field(String name) throws Exception {
    Field f = page.getClass().getDeclaredField(name);
    f.setAccessible(true);
    return f.get(page);
  }

  private void recreate() throws Exception {
    android.app.Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(page.getClass().getName(), null, false);
    try {
      main(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 20000);
      assertNotNull(page);
      getInstrumentation().waitForIdleSync();
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testVersionTwoMigrationPreservesHifzAndCompletion() throws Exception {
    String name = "academic-v2-upgrade-test.db";
    context.deleteDatabase(name);
    SQLiteDatabase old = context.openOrCreateDatabase(name, 0, null);
    old.execSQL("CREATE TABLE checklist_days(task_date TEXT PRIMARY KEY NOT NULL)");
    old.execSQL(
        "CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,task_date TEXT NOT NULL,title TEXT"
            + " NOT NULL,category TEXT NOT NULL,completed INTEGER NOT NULL DEFAULT 0,hifz_chapter"
            + " INTEGER,hifz_name TEXT,hifz_count INTEGER,hifz_first INTEGER,hifz_last INTEGER)");
    old.execSQL(
        "INSERT INTO tasks VALUES(77,'2026-10-18','[DATA UJI] Target"
            + " tersimpan','Hifz',1,1,'Al-Fatihah',7,2,5)");
    old.setVersion(2);
    old.close();
    try (ZikraDbHelper upgraded = new ZikraDbHelper(context, name, Clock.systemDefaultZone())) {
      Task task = upgraded.getTasks().get(0);
      assertEquals(77, task.id);
      assertTrue(task.completed);
      assertEquals(2, task.hifz.firstVerse);
      assertEquals(5, task.hifz.lastVerse);
      assertEquals(0, task.dueAt);
      assertFalse(task.reminder);
      assertEquals(5, upgraded.getReadableDatabase().getVersion());
      long id =
          upgraded.addAcademicTask("[DATA UJI] Tenggat", System.currentTimeMillis() + 600000, true);
      assertEquals(2, upgraded.getTasks().size());
      assertTrue(upgraded.updateAcademicTask(id, "[DATA UJI] Diubah", 0, false));
      try {
        upgraded.addAcademicTask("Tenggat tidak valid", 0, true);
        fail("Reject missing date");
      } catch (IllegalArgumentException expected) {
      }
    } finally {
      context.deleteDatabase(name);
    }
  }

  public void testDeadlineEditorDraftRecreationAndSave() throws Exception {
    launch(StudyActivity.class);
    main(() -> page.findViewById(R.id.study_add).performClick());
    Dialog editor = (Dialog) field("editorDialog");
    assertNotNull(editor);
    long due = (System.currentTimeMillis() + 3600000) / 60000 * 60000;
    Bundle draft = new Bundle();
    draft.putLong("study_due", due);
    draft.putBoolean("study_reminder", true);
    main(
        () -> {
          ((EditText) editor.findViewById(R.id.study_editor_title))
              .setText("[DATA UJI] Rangkuman kuliah");
          try {
            ((ScheduleEditor) field("scheduleEditor")).restore(draft);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    recreate();
    Dialog restored = (Dialog) field("editorDialog");
    assertNotNull(restored);
    assertTrue(restored.isShowing());
    assertEquals(due, ((ScheduleEditor) field("scheduleEditor")).dueAt());
    assertTrue(((Switch) restored.findViewById(R.id.studyReminder)).isChecked());
    UiEvidence.capture(getInstrumentation(), "AcademicDeadlineDraft");
    main(() -> restored.findViewById(R.id.study_editor_save).performClick());
    Task found = null;
    for (Task task : db.getTasks())
      if (task.title.equals("[DATA UJI] Rangkuman kuliah")) {
        found = task;
        created.add(task.id);
      }
    assertNotNull(found);
    assertEquals(due, found.dueAt);
    assertTrue(found.reminder);
    assertFalse(found.completed);
    assertTrue(
        context
            .getSharedPreferences("study_alarm_ids", 0)
            .getStringSet("ids", Collections.emptySet())
            .contains(found.id+":0"));
    UiEvidence.capture(getInstrumentation(), "AcademicStudyWithDeadline");
  }

  private void await(java.util.function.BooleanSupplier condition) {
    long until = SystemClock.uptimeMillis() + 10000;
    while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
    assertTrue("Asynchronous Android notification state becomes visible", condition.getAsBoolean());
  }

  private boolean focusNotification() {
    for (android.service.notification.StatusBarNotification n :
        context.getSystemService(NotificationManager.class).getActiveNotifications())
      if (n.getId() == 8102) return true;
    return false;
  }

  private boolean notification(long id) {
    for (android.service.notification.StatusBarNotification n :
        context.getSystemService(NotificationManager.class).getActiveNotifications())
      if (("study:" + id).equals(n.getTag())) return true;
    return false;
  }

  public void testEditedCompletedAndDeletedTasksRejectStaleReminder() {
    long future = System.currentTimeMillis() + 600000;
    long id = db.addAcademicTask("[DATA UJI] Pengingat tugas", future, true);
    created.add(id);
    StudyReminders.sync(context);
    long due = System.currentTimeMillis() - 1000;
    db.updateAcademicTask(id, "[DATA UJI] Pengingat tugas", due, true);
    StudyReminders.deliver(context, new Intent().putExtra("task", id).putExtra("due", future));
    assertFalse(notification(id));
    Intent current = new Intent().putExtra("task", id).putExtra("due", due);
    StudyReminders.deliver(context, current);
    await(() -> notification(id));
    db.setCompleted(id, true);
    StudyReminders.sync(context);
    await(() -> !notification(id));
    StudyReminders.deliver(context, current);
    assertFalse(notification(id));
    db.deleteTask(id);
    StudyReminders.deliver(context, current);
    assertFalse(notification(id));
  }

  public void testTimerSurvivesBackgroundPauseAndRecreationWithoutCompletingTask()
      throws Exception {
    long id = db.addTask("[DATA UJI] Baca satu bab", ZikraDbHelper.CATEGORY_STUDY);
    created.add(id);
    FocusClock seed = new FocusClock();
    seed.reset(25, 5, id);
    FocusTimer.save(context, seed);
    launch(new Intent(context, FocusTimerActivity.class).putExtra("task", id));
    main(() -> page.findViewById(R.id.focusPlay).performClick());
    assertTrue(FocusTimer.load(context).running);
    long before = FocusTimer.load(context).remaining;
    UiEvidence.capture(getInstrumentation(), "AcademicFocusRunning");
    Activity timer = page;
    Activity home =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    main(home::finish);
    page = timer;
    assertTrue(FocusTimer.load(context).running);
    assertTrue(FocusTimer.load(context).remaining <= before);
    main(() -> page.findViewById(R.id.focusPlay).performClick());
    assertFalse(FocusTimer.load(context).running);
    long paused = FocusTimer.load(context).remaining;
    recreate();
    assertFalse(FocusTimer.load(context).running);
    assertEquals(paused, FocusTimer.load(context).remaining);
    for (Task task : db.getTasks()) if (task.id == id) assertFalse(task.completed);
    UiEvidence.capture(getInstrumentation(), "AcademicFocusPaused");
  }

  public void testOldTimerAlarmCannotNotifyNewSession() {
    FocusClock clock = new FocusClock();
    clock.start(SystemClock.elapsedRealtime(), FocusTimer.boot(context));
    long old = clock.session;
    clock.reset(25, 5, -1);
    FocusTimer.schedule(context, clock);
    FocusTimer.deliver(context, new Intent().putExtra("session", old));
    for (android.service.notification.StatusBarNotification n :
        context.getSystemService(NotificationManager.class).getActiveNotifications())
      assertTrue(n.getId() != 8102);
    clock.start(SystemClock.elapsedRealtime(), FocusTimer.boot(context));
    clock.end = SystemClock.elapsedRealtime() - 100;
    FocusTimer.save(context, clock);
    FocusTimer.deliver(context, new Intent().putExtra("session", clock.session));
    assertTrue(FocusTimer.load(context).finished);
    assertFalse(FocusTimer.load(context).running);
    await(this::focusNotification);
  }

  public void testTimerAndFeaturedControlsAtLargeFont() {
    main(
        () -> {
          android.content.res.Configuration config =
              new android.content.res.Configuration(context.getResources().getConfiguration());
          config.fontScale = 2f;
          Context scoped =
              new android.view.ContextThemeWrapper(
                  context.createConfigurationContext(config), R.style.Theme_Zikra);
          float density = scoped.getResources().getDisplayMetrics().density;
          for (int layout :
              new int[] {R.layout.activity_focus_timer, R.layout.study_agenda_header}) {
            android.view.ViewGroup root =
                (android.view.ViewGroup)
                    android.view.LayoutInflater.from(scoped).inflate(layout, null);
            if (layout == R.layout.study_agenda_header) {
              ((TextView) root.findViewById(R.id.agenda_feature_action)).setText("Mulai fokus");
              ((TextView) root.findViewById(R.id.agenda_feature_title))
                  .setText("[DATA UJI] Tugas fokus");
            }
            int width = Math.round(320 * density);
            root.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(
                    width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(
                    Math.round(1800 * density), android.view.View.MeasureSpec.AT_MOST));
            root.layout(0, 0, width, root.getMeasuredHeight());
            for (int id :
                layout == R.layout.activity_focus_timer
                    ? new int[] {R.id.focusPlay, R.id.focusEnd}
                    : new int[] {R.id.agenda_feature_check, R.id.agenda_feature_action}) {
              android.view.View view = root.findViewById(id);
              android.graphics.Rect rect = new android.graphics.Rect();
              view.getDrawingRect(rect);
              root.offsetDescendantRectToMyCoords(view, rect);
              assertTrue(
                  "Control remains inside 320dp viewport", rect.left >= 0 && rect.right <= width);
              assertTrue(
                  "48dp touch target",
                  view.getWidth() >= 48 * density && view.getHeight() >= 48 * density);
            }
          }
        });
  }

  public void testEditorSaveRemainsVisibleAtLargeFontAndShortViewport() {
    main(
        () -> {
          android.content.res.Configuration config =
              new android.content.res.Configuration(context.getResources().getConfiguration());
          config.fontScale = 2f;
          Context scoped =
              new android.view.ContextThemeWrapper(
                  context.createConfigurationContext(config), R.style.Theme_Zikra);
          float density = scoped.getResources().getDisplayMetrics().density;
          for (int heightDp : new int[] {560, 320}) {
            android.view.ViewGroup root =
                (android.view.ViewGroup)
                    android.view.LayoutInflater.from(scoped)
                        .inflate(R.layout.study_dialog_task, null);
            root.findViewById(R.id.studySchedule).setVisibility(android.view.View.VISIBLE);
            ((EditText) root.findViewById(R.id.study_editor_title))
                .setText("[DATA UJI] Tugas dengan tenggat");
            int width = Math.round(320 * density), height = Math.round(heightDp * density);
            root.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(
                    width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(
                    height, android.view.View.MeasureSpec.AT_MOST));
            root.layout(0, 0, width, root.getMeasuredHeight());
            android.view.View save = root.findViewById(R.id.study_editor_save);
            android.graphics.Rect box = new android.graphics.Rect();
            save.getDrawingRect(box);
            root.offsetDescendantRectToMyCoords(save, box);
            assertTrue("Save remains above bottom edge", box.top >= 0 && box.bottom <= height);
            assertTrue(
                "Save has a usable touch target",
                save.getHeight() >= 48 * density && save.getWidth() >= 48 * density);
          }
        });
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (page != null) main(page::finish);
      for (long id : created) {
        db.deleteTask(id);
        context.getSystemService(NotificationManager.class).cancel("study:" + id, 1);
      }
      db.close();
      for (Map.Entry<String, Map<String, ?>> item : saved.entrySet()) {
        SharedPreferences.Editor e = context.getSharedPreferences(item.getKey(), 0).edit().clear();
        for (Map.Entry<String, ?> value : item.getValue().entrySet()) {
          String key = value.getKey();
          Object v = value.getValue();
          if (v instanceof String) e.putString(key, (String) v);
          else if (v instanceof Integer) e.putInt(key, (Integer) v);
          else if (v instanceof Long) e.putLong(key, (Long) v);
          else if (v instanceof Boolean) e.putBoolean(key, (Boolean) v);
          else if (v instanceof Float) e.putFloat(key, (Float) v);
          else if (v instanceof Set) e.putStringSet(key, (Set<String>) v);
        }
        e.commit();
      }
      StudyReminders.sync(context);
      FocusTimer.restore(context);
    } finally {
      super.tearDown();
    }
  }
}
