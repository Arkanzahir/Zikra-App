package com.zikra.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import com.zikra.app.data.HifzTarget;
import com.zikra.app.data.Task;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.study.SurahCatalog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("deprecation")
public final class StudyRecoveryTest extends InstrumentationTestCase {
  private static final String SCOPED_DATABASE = "study-persistent-ui-test.db";
  private StudyActivity page;
  private Activity reader;
  private ZikraDbHelper db;
  private final List<Long> created = new ArrayList<>();
  private SharedPreferences prefs;
  private String oldName;
  private int oldAyah;
  private boolean hadName, hadAyah;
  private final java.util.Map<Long, Boolean> changed = new java.util.HashMap<>();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    db = new ZikraDbHelper(getInstrumentation().getTargetContext());
    prefs = getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    hadName = prefs.contains("last_surah");
    hadAyah = prefs.contains("last_ayah");
    oldName = prefs.getString("last_surah", "");
    oldAyah = prefs.getInt("last_ayah", 1);
  }

  private void launch(boolean hifz) {
    launchScope(hifz ? ZikraDbHelper.CATEGORY_HIFZ : ZikraDbHelper.CATEGORY_STUDY);
  }

  private void launchScope(String category) {
    Intent i =
        new Intent(getInstrumentation().getTargetContext(), StudyActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    i.putExtra("category", category);
    page = (StudyActivity) getInstrumentation().startActivitySync(i);
    await(() -> page.hasWindowFocus(), 30000);
  }

  private void replacePageStorage(ZikraDbHelper isolated) {
    onMain(
        () -> {
          try {
            Field field = StudyActivity.class.getDeclaredField("db");
            field.setAccessible(true);
            if (field.get(page) != isolated) ((ZikraDbHelper) field.get(page)).close();
            field.set(page, isolated);
            java.lang.reflect.Method refresh =
                StudyActivity.class.getDeclaredMethod("refreshTasks");
            refresh.setAccessible(true);
            refresh.invoke(page);
          } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
          }
        });
  }

  private static final class PlanningClock extends java.time.Clock {
    private java.time.Instant now = java.time.Instant.parse("2026-10-18T03:00:00Z");

    @Override
    public java.time.ZoneId getZone() {
      return java.time.ZoneId.of("Asia/Jakarta");
    }

    @Override
    public java.time.Clock withZone(java.time.ZoneId zone) {
      return java.time.Clock.fixed(now, zone);
    }

    @Override
    public java.time.Instant instant() {
      return now;
    }

    void nextDay() {
      now = now.plusSeconds(86400);
    }
  }

  private Dialog editor() {
    try {
      Field f = StudyActivity.class.getDeclaredField("editorDialog");
      f.setAccessible(true);
      return (Dialog) f.get(page);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private EditText input(int id) {
    return editor().findViewById(id);
  }

  private void open() {
    onMain(() -> page.findViewById(R.id.study_add).performClick());
    await(() -> editor() != null && editor().getWindow().getDecorView().hasWindowFocus(), 15000);
  }

  private void choose(int id) {
    onMain(
        () -> {
          Spinner s = editor().findViewById(R.id.hifz_surah);
          for (int i = 1; i < s.getCount(); i++)
            if (s.getItemAtPosition(i).toString().startsWith(id + ".")) {
              s.setSelection(i);
              return;
            }
          fail("Surah absent");
        });
    await(() -> !input(R.id.hifz_first).getText().toString().isEmpty(), 10000);
  }

  public void testDateAndStudyEditorValidationDictationAndSave() {
    launch(false);
    assertTrue(
        ((TextView) page.findViewById(R.id.study_date)).getText().toString().contains("2026"));
    assertFalse(page.findViewById(R.id.study_date).isClickable());
    assertEquals(5, ((LinearLayout) page.findViewById(R.id.navItems)).getChildCount());
    capture("study");
    open();
    capture("study-editor");
    int before = db.getTotalCount();
    onMain(
        () -> {
          editor().findViewById(R.id.study_editor_save).performClick();
          assertNotNull(input(R.id.study_editor_title).getError());
          Intent voice =
              new Intent()
                  .putStringArrayListExtra(
                      RecognizerIntent.EXTRA_RESULTS,
                      new ArrayList<>(Arrays.asList("ZZ_RECOVERY_STUDY")));
          page.onActivityResult(100, Activity.RESULT_OK, voice);
          assertEquals("ZZ_RECOVERY_STUDY", input(R.id.study_editor_title).getText().toString());
          ((RadioGroup) editor().findViewById(R.id.study_editor_category))
              .check(R.id.study_editor_ibadah);
        });
    assertEquals(before, db.getTotalCount());
    onMain(() -> editor().findViewById(R.id.study_editor_save).performClick());
    Task saved = named("ZZ_RECOVERY_STUDY");
    assertNotNull(saved);
    created.add(saved.id);
    assertEquals(
        "Academic scope cannot be changed by stale/hidden category state",
        ZikraDbHelper.CATEGORY_STUDY,
        saved.category);
    open();
    onMain(() -> editor().findViewById(R.id.study_editor_cancel).performClick());
    assertEquals(before + 1, db.getTotalCount());
  }

  public void testCatalogLiveRangeValidationDraftAndCancelledRecognition() throws Exception {
    launch(true);
    open();
    onMain(
        () -> {
          editor().findViewById(R.id.study_editor_save).performClick();
          assertEquals(View.VISIBLE, editor().findViewById(R.id.hifz_validation).getVisibility());
        });
    await(() -> ((Spinner) editor().findViewById(R.id.hifz_surah)).getCount() == 115, 45000);
    choose(67);
    onMain(
        () -> {
          input(R.id.hifz_first).setText("3");
          input(R.id.hifz_last).setText("31");
          editor().findViewById(R.id.study_editor_save).performClick();
          assertTrue(editor().isShowing());
          input(R.id.hifz_last).setText("7");
        });
    capture("hifz-editor");
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(StudyActivity.class.getName(), null, false);
    try {
      onMain(page::recreate);
      page = (StudyActivity) getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
      await(() -> editor() != null && editor().isShowing(), 25000);
      onMain(
          () -> {
            assertEquals("3", input(R.id.hifz_first).getText().toString());
            assertEquals("7", input(R.id.hifz_last).getText().toString());
            assertTrue(
                ((Spinner) editor().findViewById(R.id.hifz_surah))
                    .getSelectedItem()
                    .toString()
                    .startsWith("67."));
            editor().findViewById(R.id.hifz_note_toggle).performClick();
            input(R.id.study_editor_title).setText("ZZ_RECOVERY_HIFZ");
            page.onActivityResult(100, Activity.RESULT_CANCELED, null);
            assertEquals("ZZ_RECOVERY_HIFZ", input(R.id.study_editor_title).getText().toString());
            editor().findViewById(R.id.study_editor_save).performClick();
          });
      Task task = named("ZZ_RECOVERY_HIFZ");
      assertNotNull(task);
      created.add(task.id);
      assertEquals(67, task.hifz.chapterId);
      assertEquals(5, task.hifz.count());
      assertTrue(
          ((TextView) page.findViewById(R.id.agenda_feature_detail))
              .getText()
              .toString()
              .contains("ZZ_RECOVERY_HIFZ"));
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testTargetCardsCompletionAndLocalReaderRoute() {
    long mulk = db.addHifzTask("Al-Mulk · Ayat 3–7", new HifzTarget(67, "Al-Mulk", 30, 3, 7));
    created.add(mulk);
    long ikhlas = db.addHifzTask("Al-Ikhlas · Ayat 2–4", new HifzTarget(112, "Al-Ikhlas", 4, 2, 4));
    created.add(ikhlas);
    launch(true);
    scrollTo(ikhlas);
    await(() -> row(ikhlas) != null, 15000);
    onMain(() -> ((CheckBox) row(ikhlas).findViewById(R.id.study_task_checkbox)).performClick());
    assertTrue(find(ikhlas).completed);
    onMain(() -> ((ListView) page.findViewById(R.id.study_tasks)).setSelection(0));
    capture("hifz");
    scrollTo(Long.MIN_VALUE);
    await(() -> page.findViewById(R.id.agenda_completed_toggle) != null, 15000);
    tapCompleted();
    await(() -> adapterContains(ikhlas), 10000);
    scrollTo(ikhlas);
    await(() -> row(ikhlas) != null, 15000);
    Intent online = StudyActivity.readingIntent(page, new HifzTarget(67, "Al-Mulk", 30, 3, 7));
    assertEquals(QuranActivity.class.getName(), online.getComponent().getClassName());
    assertEquals(67, online.getIntExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 0));
    assertEquals(3, online.getIntExtra(QuranActivity.EXTRA_TARGET_VERSE, 0));
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      onMain(() -> row(ikhlas).findViewById(R.id.hifz_read).performClick());
      reader = getInstrumentation().waitForMonitorWithTimeout(monitor, 25000);
      assertNotNull(reader);
      await(
          () ->
              prefs.getString("last_surah", "").equals("Al-Ikhlas")
                  && prefs.getInt("last_ayah", 0) == 2,
          15000);
      assertEquals(View.VISIBLE, reader.findViewById(R.id.quranReader).getVisibility());
      capture("local-reading");
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testCatalogRejectsIncompleteAndMisorderedMetadata() throws Exception {
    assertEquals(4, SurahCatalog.local().size());
    try {
      SurahCatalog.parse("{\"chapters\":[]}");
      fail("Incomplete catalog accepted");
    } catch (org.json.JSONException expected) {
    }
    JSONArray rows = new JSONArray();
    for (int i = 1; i <= 114; i++)
      rows.put(
          new JSONObject().put("id", i).put("name_simple", "Chapter " + i).put("verses_count", 1));
    try {
      SurahCatalog.parse(new JSONObject().put("chapters", rows).toString());
      fail("Wrong ayah total accepted");
    } catch (org.json.JSONException expected) {
    }
    rows.getJSONObject(0).put("id", 2);
    try {
      SurahCatalog.parse(new JSONObject().put("chapters", rows).toString());
      fail("Misordered catalog accepted");
    } catch (org.json.JSONException expected) {
    }
  }

  public void testAgendaCompletionFilterAndRecreation() {
    created.add(db.addTask("Lanjutkan rangkuman kuliah", ZikraDbHelper.CATEGORY_STUDY));
    created.add(db.addTask("Baca ulang materi besok", ZikraDbHelper.CATEGORY_STUDY));
    long done = db.addTask("Susun catatan pekan ini", ZikraDbHelper.CATEGORY_STUDY);
    created.add(done);
    db.setCompleted(done, true);
    launch(false);
    assertEquals(View.GONE, page.findViewById(R.id.study_filters).getVisibility());
    List<Task> pending = new ArrayList<>();
    int completed = 0, total = 0;
    for (Task t : db.getTasks())
      if (t.category.equals(ZikraDbHelper.CATEGORY_STUDY)) {
        total++;
        if (t.completed) completed++;
        else pending.add(t);
      }
    Task first = pending.get(0), next = pending.get(1);
    assertEquals(
        first.title,
        ((TextView) page.findViewById(R.id.agenda_feature_title)).getText().toString());
    assertEquals(
        page.getString(R.string.agenda_progress, completed, total),
        ((TextView) page.findViewById(R.id.study_progress_label)).getText().toString());
    assertFalse(adapterContains(done));
    capture("study-focus");
    changed.put(first.id, first.completed);
    onMain(() -> ((CheckBox) page.findViewById(R.id.agenda_feature_check)).performClick());
    assertTrue(find(first.id).completed);
    assertEquals(
        next.title, ((TextView) page.findViewById(R.id.agenda_feature_title)).getText().toString());
    assertFalse(adapterContains(first.id));
    scrollTo(Long.MIN_VALUE);
    await(() -> page.findViewById(R.id.agenda_completed_toggle) != null, 15000);
    tapCompleted();
    await(() -> adapterContains(first.id), 10000);
    assertTrue(adapterContains(first.id));
    assertTrue(adapterContains(done));
    scrollTo(first.id);
    capture("study-completed");
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(StudyActivity.class.getName(), null, false);
    try {
      onMain(page::recreate);
      page = (StudyActivity) getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
      await(() -> page.hasWindowFocus(), 15000);
      assertTrue(adapterContains(first.id));
      assertTrue(adapterContains(done));
      // Completion controls are below the introduction; the whole agenda scrolls together.
      scrollTo(Long.MIN_VALUE);
      await(() -> page.findViewById(R.id.agenda_completed_toggle) != null, 15000);
      tapCompleted();
      await(() -> !adapterContains(first.id), 10000);
      assertFalse(adapterContains(first.id));
      assertTrue(find(first.id).completed);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testAcademicAndPrayerScopesKeepExistingTasks() {
    int before = db.getTotalCount();
    long academic = db.addTask("ZZ_RECOVERY_ACADEMIC_SCOPE", ZikraDbHelper.CATEGORY_STUDY);
    long prayer = db.addTask("ZZ_RECOVERY_PRAYER_SCOPE", ZikraDbHelper.CATEGORY_IBADAH);
    created.add(academic);
    created.add(prayer);
    launch(false);
    assertTrue(
        adapterContains(academic)
            || ((TextView) page.findViewById(R.id.agenda_feature_title))
                .getText()
                .toString()
                .equals("ZZ_RECOVERY_ACADEMIC_SCOPE"));
    assertFalse(adapterContains(prayer));
    assertEquals(
        "Akademik",
        ((TextView) page.findViewById(R.id.agenda_feature_category)).getText().toString());
    assertEquals(before + 2, db.getTotalCount());
    onMain(page::finish);
    reader =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), PrayerActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    await(reader::hasWindowFocus, 30000);
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(StudyActivity.class.getName(), null, false);
    try {
      onMain(() -> reader.findViewById(R.id.prayerChecklist).performClick());
      page = (StudyActivity) getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull(page);
      await(page::hasWindowFocus, 15000);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    assertEquals(
        "Checklist ibadah",
        ((TextView) page.findViewById(R.id.study_screen_title)).getText().toString());
    assertFalse(adapterContains(academic));
    assertTrue(
        adapterContains(prayer)
            || ((TextView) page.findViewById(R.id.agenda_feature_title))
                .getText()
                .toString()
                .equals("ZZ_RECOVERY_PRAYER_SCOPE"));
    UiEvidence.capture(getInstrumentation(), "RebuiltIbadahScope");
    open();
    onMain(
        () -> {
          input(R.id.study_editor_title).setText("ZZ_RECOVERY_NEW_IBADAH");
          editor().findViewById(R.id.study_editor_save).performClick();
        });
    Task saved = named("ZZ_RECOVERY_NEW_IBADAH");
    assertNotNull(saved);
    created.add(saved.id);
    assertEquals(ZikraDbHelper.CATEGORY_IBADAH, saved.category);
    assertEquals(before + 3, db.getTotalCount());
  }

  public void testEmptyWorshipTemplateAndStaleEditorKeepDraftWithoutFalseSave() {
    getInstrumentation().getTargetContext().deleteDatabase(SCOPED_DATABASE);
    PlanningClock clock = new PlanningClock();
    ZikraDbHelper isolated =
        new ZikraDbHelper(getInstrumentation().getTargetContext(), SCOPED_DATABASE, clock);
    launchScope(ZikraDbHelper.CATEGORY_IBADAH);
    replacePageStorage(isolated);
    assertEquals(0, isolated.getTotalCount());
    assertEquals(View.VISIBLE, page.findViewById(R.id.ibadah_template).getVisibility());
    onMain(() -> page.findViewById(R.id.ibadah_template).performClick());
    assertEquals(5, isolated.getTotalCount());
    assertEquals(0, isolated.getCompletedCount());
    assertEquals(View.GONE, page.findViewById(R.id.ibadah_template).getVisibility());
    long original = isolated.getTasks().get(0).id;
    onMain(() -> page.findViewById(R.id.agenda_feature_action).performClick());
    await(() -> editor() != null && editor().isShowing(), 15000);
    onMain(() -> input(R.id.study_editor_title).setText("Ibadah dari draf kemarin"));
    clock.nextDay();
    onMain(() -> editor().findViewById(R.id.study_editor_save).performClick());
    assertTrue(editor().isShowing());
    assertEquals("Ibadah dari draf kemarin", input(R.id.study_editor_title).getText().toString());
    assertEquals(0, isolated.getTotalCount());
    assertEquals(View.VISIBLE, editor().findViewById(R.id.study_editor_status).getVisibility());
    assertEquals(
        page.getString(R.string.planner_add_today),
        ((TextView) editor().findViewById(R.id.study_editor_save)).getText().toString());
    try (android.database.Cursor c =
        isolated
            .getReadableDatabase()
            .rawQuery(
                "SELECT title,completed,task_date FROM tasks WHERE id=?",
                new String[] {Long.toString(original)})) {
      assertTrue(c.moveToFirst());
      assertEquals("Sholat Subuh", c.getString(0));
      assertEquals(0, c.getInt(1));
      assertEquals("2026-10-18", c.getString(2));
    }
    // Dismiss listeners are delivered asynchronously; wait for the actual dialog to close.
    // The second action explicitly creates today's copy; yesterday's record is untouched.
    onMain(() -> editor().findViewById(R.id.study_editor_save).performClick());
    await(() -> editor() == null, 5000);
    assertNull(editor());
    assertEquals(1, isolated.getTotalCount());
    assertEquals("Ibadah dari draf kemarin", isolated.getTasks().get(0).title);
    assertEquals(0, isolated.getCompletedCount());
  }

  public void testAcademicPlansRemainVisibleAfterMidnightAndCompletedCollapse() {
    getInstrumentation().getTargetContext().deleteDatabase(SCOPED_DATABASE);
    PlanningClock clock = new PlanningClock();
    ZikraDbHelper isolated =
        new ZikraDbHelper(getInstrumentation().getTargetContext(), SCOPED_DATABASE, clock);
    long older = isolated.addTask("Rangkuman yang berlanjut besok", ZikraDbHelper.CATEGORY_STUDY);
    long second = isolated.addTask("Tinjau kembali catatan", ZikraDbHelper.CATEGORY_STUDY);
    launch(false);
    replacePageStorage(isolated);
    assertEquals(View.GONE, page.findViewById(R.id.ibadah_template).getVisibility());
    clock.nextDay();
    replacePageStorage(isolated);
    assertEquals(
        "Rangkuman yang berlanjut besok",
        ((TextView) page.findViewById(R.id.agenda_feature_title)).getText().toString());
    onMain(() -> page.findViewById(R.id.agenda_feature_action).performClick());
    await(() -> editor() != null && editor().isShowing(), 15000);
    onMain(
        () -> {
          input(R.id.study_editor_title).setText("Rangkuman lintas hari diperbarui");
          editor().findViewById(R.id.study_editor_save).performClick();
        });
    assertEquals(older, isolated.getTasks().get(0).id);
    assertEquals("Rangkuman lintas hari diperbarui", isolated.getTasks().get(0).title);
    onMain(() -> ((CheckBox) page.findViewById(R.id.agenda_feature_check)).performClick());
    assertTrue(isolated.getTasks().get(0).completed);
    assertEquals(
        "Tinjau kembali catatan",
        ((TextView) page.findViewById(R.id.agenda_feature_title)).getText().toString());
    assertFalse(adapterContains(older));
    scrollTo(Long.MIN_VALUE);
    await(() -> page.findViewById(R.id.agenda_completed_toggle) != null, 15000);
    tapCompleted();
    await(() -> adapterContains(older), 10000);
    assertEquals(2, isolated.getTotalCount());
    assertEquals(1, isolated.getCompletedCount());
  }

  private void tapCompleted() {
    android.graphics.Rect bounds = new android.graphics.Rect();
    onMain(
        () -> {
          View toggle = page.findViewById(R.id.agenda_completed_toggle);
          assertNotNull(toggle);
          assertTrue("Completed control must accept real touch input", toggle.isEnabled());
          assertTrue(toggle.getGlobalVisibleRect(bounds));
        });
    long now = SystemClock.uptimeMillis();
    android.view.MotionEvent down =
        android.view.MotionEvent.obtain(
            now, now, android.view.MotionEvent.ACTION_DOWN, bounds.centerX(), bounds.centerY(), 0);
    android.view.MotionEvent up =
        android.view.MotionEvent.obtain(
            now,
            now + 60,
            android.view.MotionEvent.ACTION_UP,
            bounds.centerX(),
            bounds.centerY(),
            0);
    down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
    up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
    try {
      assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(down, true));
      assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(up, true));
    } finally {
      down.recycle();
      up.recycle();
    }
    getInstrumentation().waitForIdleSync();
  }

  private boolean adapterContains(long id) {
    android.widget.ListAdapter a = ((ListView) page.findViewById(R.id.study_tasks)).getAdapter();
    for (int i = 0; i < a.getCount(); i++) if (a.getItemId(i) == id) return true;
    return false;
  }

  private void scrollTo(long id) {
    onMain(
        () -> {
          ListView list = page.findViewById(R.id.study_tasks);
          for (int i = 0; i < list.getAdapter().getCount(); i++)
            if (list.getAdapter().getItemId(i) == id) {
              list.setSelection(i);
              return;
            }
          fail("Missing agenda row " + id);
        });
    getInstrumentation().waitForIdleSync();
  }

  private Task named(String name) {
    for (Task t : db.getTasks()) if (t.title.equals(name)) return t;
    return null;
  }

  private Task find(long id) {
    for (Task t : db.getTasks()) if (t.id == id) return t;
    throw new AssertionError("Missing task");
  }

  private View row(long id) {
    ListView list = page.findViewById(R.id.study_tasks);
    for (int i = 0; i < list.getChildCount(); i++)
      if (list.getAdapter().getItemId(list.getFirstVisiblePosition() + i) == id)
        return list.getChildAt(i);
    return null;
  }

  private void capture(String name) {
    try {
      getInstrumentation().waitForIdleSync();
      getInstrumentation().getUiAutomation().waitForIdle(750, 10000);
      SystemClock.sleep(1800);
      File dir =
          new File(getInstrumentation().getTargetContext().getExternalFilesDir(null), "agenda");
      if (!dir.exists() && !dir.mkdirs())
        throw new IllegalStateException("No screenshot directory");
      try (ParcelFileDescriptor fd =
              getInstrumentation().getUiAutomation().executeShellCommand("screencap -p");
          InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(fd);
          FileOutputStream output = new FileOutputStream(new File(dir, name + ".png"))) {
        byte[] buffer = new byte[32768];
        int size;
        while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
      }
    } catch (Exception error) {
      throw new AssertionError(error);
    }
  }

  private void onMain(Runnable action) {
    AtomicReference<Throwable> failure = new AtomicReference<>();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                action.run();
              } catch (Throwable error) {
                failure.set(error);
              }
            });
    if (failure.get() != null) throw new AssertionError(failure.get());
  }

  private void await(BooleanSupplier test, long duration) {
    long deadline = SystemClock.uptimeMillis() + duration;
    boolean[] ready = {false};
    while (SystemClock.uptimeMillis() < deadline) {
      onMain(() -> ready[0] = test.getAsBoolean());
      if (ready[0]) return;
      SystemClock.sleep(150);
    }
    fail("Native screen not ready");
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (reader != null) onMain(reader::finish);
      if (page != null) onMain(page::finish);
      getInstrumentation().waitForIdleSync();
      for (Task task : db.getTasks())
        if (task.title.startsWith("ZZ_RECOVERY_") && !created.contains(task.id))
          created.add(task.id);
      for (long id : created) db.deleteTask(id);
      for (java.util.Map.Entry<Long, Boolean> e : changed.entrySet())
        db.setCompleted(e.getKey(), e.getValue());
      db.close();
      getInstrumentation().getTargetContext().deleteDatabase(SCOPED_DATABASE);
    } finally {
      SharedPreferences.Editor e = prefs.edit();
      if (hadName) e.putString("last_surah", oldName);
      else e.remove("last_surah");
      if (hadAyah) e.putInt("last_ayah", oldAyah);
      else e.remove("last_ayah");
      e.commit();
      super.tearDown();
    }
  }
}
