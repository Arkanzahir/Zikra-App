package com.zikra.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.data.*;
import com.zikra.app.faith.*;
import java.io.File;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;

/** Dedicated emulator only: host first backs up and isolates the application's original data. */
@SuppressWarnings("deprecation")
public final class FaithJournalTest extends InstrumentationTestCase {
  private Context context;
  private Activity page, other;
  private FaithStore store;
  private ZikraDbHelper tasks;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue(
        "Isolated host fixtures required",
        new File(context.getFilesDir(), "d05-authorized").isFile());
    context.deleteDatabase("faith.db");
    context.deleteDatabase("zikra.db");
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .commit();
    store = new FaithStore(context);
    tasks = new ZikraDbHelper(context);
  }

  private void main(Runnable r) {
    getInstrumentation().runOnMainSync(r);
    getInstrumentation().waitForIdleSync();
  }

  private void launch() {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, FaithActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }

  private void click(int id) {
    main(() -> page.findViewById(id).performClick());
  }

  private Dialog dialog() {
    try {
      Field f = FaithActivity.class.getDeclaredField("dialog");
      f.setAccessible(true);
      return (Dialog) f.get(page);
    } catch (Exception error) {
      throw new AssertionError(error);
    }
  }

  private void edit(int id, String text) {
    main(() -> ((EditText) dialog().findViewById(id)).setText(text));
  }

  private void save() {
    main(
        () -> {
          View custom = dialog().findViewById(R.id.faithEditorSave);
          if (custom != null) custom.performClick();
          else ((AlertDialog) dialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });
  }

  private void cancel() {
    main(
        () -> {
          View custom = dialog().findViewById(R.id.faithEditorCancel);
          if (custom != null) custom.performClick();
          else ((AlertDialog) dialog()).getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        });
  }

  private FaithStore.Entry reading(int first, int last, long plan, LocalDate day) {
    return new FaithStore.Entry(
        0,
        0,
        UUID.randomUUID().toString(),
        FaithStore.READING,
        day,
        1,
        first,
        last,
        "DATA UJI",
        0,
        plan);
  }

  private View text(View view, String wanted) {
    if (view instanceof Button && wanted.contentEquals(((Button) view).getText())) return view;
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        View found = text(((ViewGroup) view).getChildAt(i), wanted);
        if (found != null) return found;
      }
    return null;
  }

  private void rowAction(long id, int label) {
    View row = page.findViewById(R.id.faithEntries).findViewWithTag("entry-" + id);
    assertNotNull(row);
    View action = text(row, context.getString(label));
    assertNotNull(action);
    main(action::performClick);
  }

  public void testEmptyStatePlanCancelAndDateNeverInventProgress() throws Exception {
    launch();
    assertNull(store.plan());
    assertTrue(store.activeDays().isEmpty());
    assertTrue(
        ((TextView) page.findViewById(R.id.faithSummary)).getText().toString().contains("0 hari"));
    click(R.id.faithPlanEdit);
    assertEquals(3, ((Spinner) dialog().findViewById(R.id.faithEditorDays)).getCount());
    cancel();
    assertNull(store.plan());
    click(R.id.faithNote);
    edit(R.id.faithEditorNote, "Catatan uji tanpa progres");
    save();
    assertEquals(0, FaithSummary.load(store, tasks).streak);
    UiEvidence.capture(getInstrumentation(), "D05-empty-note");
  }

  public void testReadingOverlapEditCancelDeleteAndBookmarkAreConsistent() throws Exception {
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putString("last_verse_key", "67:4")
        .commit();
    launch();
    click(R.id.faithPlanEdit);
    main(() -> ((Spinner) dialog().findViewById(R.id.faithEditorDays)).setSelection(1));
    save();
    assertEquals(90, store.plan().days);
    click(R.id.faithRead);
    edit(R.id.faithEditorLast, "4");
    save();
    click(R.id.faithRead);
    edit(R.id.faithEditorFirst, "3");
    edit(R.id.faithEditorLast, "7");
    save();
    assertEquals(7, store.coverage(store.plan().id).cardinality());
    FaithStore.Entry last = store.entries(store.today()).get(0);
    rowAction(last.id, R.string.faith_edit);
    edit(R.id.faithEditorLast, "3");
    cancel();
    assertEquals(7, store.coverage(store.plan().id).cardinality());
    rowAction(last.id, R.string.faith_edit);
    edit(R.id.faithEditorLast, "3");
    save();
    assertEquals(4, store.coverage(store.plan().id).cardinality());
    rowAction(last.id, R.string.faith_delete);
    cancel();
    assertEquals(2, store.entries(store.today()).size());
    rowAction(last.id, R.string.faith_delete);
    save();
    assertEquals(1, store.entries(store.today()).size());
    assertEquals(
        "67:4",
        context.getSharedPreferences("zikra_preferences", 0).getString("last_verse_key", ""));
    UiEvidence.capture(getInstrumentation(), "D05-recorded-reading");
  }

  public void testEditorDraftRecreatesAndInvalidRangeNeverClosesOrSaves() throws Exception {
    launch();
    click(R.id.faithPractice);
    assertEquals(114, ((Spinner) dialog().findViewById(R.id.faithEditorSurah)).getCount());
    edit(R.id.faithEditorFirst, "3");
    edit(R.id.faithEditorLast, "2");
    edit(R.id.faithEditorNote, "Draf latihan uji");
    save();
    assertTrue(dialog().isShowing());
    assertTrue(store.entries(store.today()).isEmpty());
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(FaithActivity.class.getName(), null, false);
    try {
      main(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 15000);
      assertNotNull(page);
      getInstrumentation().waitForIdleSync();
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    assertTrue(dialog().isShowing());
    assertEquals(
        "Draf latihan uji",
        ((EditText) dialog().findViewById(R.id.faithEditorNote)).getText().toString());
    edit(R.id.faithEditorLast, "4");
    View previousSave = dialog().findViewById(R.id.faithEditorSave);
    save();
    main(previousSave::performClick);
    assertEquals(1, store.entries(store.today()).size());
    assertEquals(1, FaithSummary.load(store, tasks).streak);
  }

  public void testHifzPracticePrefillsRangeButCannotCompleteTargetOrReadingPlan() throws Exception {
    long id = tasks.addHifzTask("DATA UJI", new HifzTarget(1, "Al-Fatihah", 7, 2, 3));
    store.savePlan(30, store.today(), false);
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, FaithActivity.class)
                    .putExtra("target", id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    assertEquals(
        "2", ((EditText) dialog().findViewById(R.id.faithEditorFirst)).getText().toString());
    save();
    assertEquals(id, store.entries(store.today()).get(0).targetId);
    assertFalse(tasks.getTasks().get(0).completed);
    assertEquals(0, store.coverage(store.plan().id).cardinality());
    Intent reader = StudyActivity.readingIntent(context, tasks.getTasks().get(0).hifz);
    assertEquals(2, reader.getIntExtra(QuranActivity.EXTRA_TARGET_VERSE, 0));
    other = getInstrumentation().startActivitySync(reader.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    assertEquals(0, store.coverage(store.plan().id).cardinality());
    assertFalse(tasks.getTasks().get(0).completed);
  }

  public void testHomeProfileAndJournalUseSameActualRecords() throws Exception {
    long plan = store.savePlan(30, store.today(), false);
    store.save(reading(1, 4, plan, store.today()));
    launch();
    String expected = ((TextView) page.findViewById(R.id.faithSummary)).getText().toString();
    other =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    String home = ((TextView) other.findViewById(R.id.homeFaithSummary)).getText().toString();
    assertTrue(home.contains(expected));
    assertTrue(home.contains("4/6.236"));
    main(other::finish);
    other = null;
    other =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, ProfileActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    assertTrue(allText(other.getWindow().getDecorView()).contains(expected));
    assertTrue(allText(other.getWindow().getDecorView()).contains("4/6.236"));
    UiEvidence.capture(getInstrumentation(), "D05-profile-summary");
  }

  private String allText(View view) {
    String out = view instanceof TextView ? ((TextView) view).getText().toString() : "";
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
        out += " " + allText(((ViewGroup) view).getChildAt(i));
    return out;
  }

  public void testPastDateJournalSurvivesRestartWithoutChangingTodaysChecklist() throws Exception {
    LocalDate yesterday = store.today().minusDays(1);
    long plan = store.savePlan(30, yesterday, false);
    store.save(reading(1, 3, plan, yesterday));
    tasks.addObligatoryPrayerChecklist();
    launch();
    click(R.id.faithDate);
    Field f = FaithActivity.class.getDeclaredField("calendar");
    f.setAccessible(true);
    DatePickerDialog calendar = (DatePickerDialog) f.get(page);
    main(
        () -> {
          calendar.updateDate(
              yesterday.getYear(), yesterday.getMonthValue() - 1, yesterday.getDayOfMonth());
          calendar.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });
    assertEquals(1, ((LinearLayout) page.findViewById(R.id.faithEntries)).getChildCount() - 1);
    click(R.id.faithNote);
    edit(R.id.faithEditorNote, "Refleksi kemarin uji");
    save();
    assertEquals("Refleksi kemarin uji", store.note(yesterday));
    assertEquals(0, tasks.getCompletedCount());
    assertEquals(1, FaithSummary.load(store, tasks).streak);
  }

  public void testNewCycleConfirmationPreservesArchivedReading() {
    long old = store.savePlan(30, store.today(), false);
    store.save(reading(1, 7, old, store.today()));
    launch();
    click(R.id.faithPlanNew);
    cancel();
    assertEquals(old, store.plan().id);
    click(R.id.faithPlanNew);
    save();
    save();
    assertTrue(old != store.plan().id);
    assertEquals(0, store.coverage(store.plan().id).cardinality());
    assertEquals(7, store.coverage(old).cardinality());
    assertEquals(1, store.entries(store.today()).size());
  }

  public void testKeyboardAndLargeTextKeepDraftAndSaveReachable() throws Exception {
    launch();
    click(R.id.faithPractice);
    edit(R.id.faithEditorNote, "Catatan latihan uji");
    main(
        () -> {
          EditText field = dialog().findViewById(R.id.faithEditorNote);
          field.requestFocus();
          ((android.view.inputmethod.InputMethodManager)
                  context.getSystemService(Context.INPUT_METHOD_SERVICE))
              .showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
    SystemClock.sleep(650);
    android.graphics.Rect bounds = new android.graphics.Rect();
    assertTrue(dialog().findViewById(R.id.faithEditorSave).getGlobalVisibleRect(bounds));
    assertTrue(bounds.height() >= 48 * context.getResources().getDisplayMetrics().density - 1);
    android.graphics.Rect visible = new android.graphics.Rect();
    dialog().getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
    assertTrue("Save stays above the keyboard", bounds.bottom <= visible.bottom);
    SystemClock.sleep(1800);
    UiEvidence.capture(getInstrumentation(), "D05-keyboard-draft");
    save();
    assertEquals(1, store.entries(store.today()).size());
  }

  @Override
  protected void tearDown() throws Exception {
    if (other != null) main(other::finish);
    if (page != null) main(page::finish);
    store.close();
    tasks.close();
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
