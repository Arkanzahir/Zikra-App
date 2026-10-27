package com.zikra.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.data.*;
import com.zikra.app.faith.FaithStore;
import com.zikra.app.location.*;
import com.zikra.app.sky.HomeSkyView;
import java.io.*;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;

/** Host-isolated emulator only. Checks actual records, navigation and visible layout. */
@SuppressWarnings("deprecation")
public final class RefinedExperienceTest extends InstrumentationTestCase {
  private Context context;
  private Activity page, other;
  private ZikraDbHelper tasks;
  private final List<Long> prayers = new ArrayList<>();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertTrue(
        "Host must isolate original data",
        new File(context.getFilesDir(), "refined-native-authorized").isFile());
    assertEquals("1", shell("getprop ro.kernel.qemu").trim());
    context.deleteDatabase("zikra.db");
    context.deleteDatabase("faith.db");
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .clear()
        .putBoolean("onboarding_complete", true)
        .putBoolean("shake_enabled", false)
        .putString("profile_display_name", "Nadia")
        .putString("last_surah", "Al-Kahf")
        .putInt("last_chapter", 18)
        .putInt("last_ayah", 12)
        .putString("last_verse_key", "18:12")
        .putString("user_role", "Student")
        .commit();
    LocationStore.save(context, LocationPoint.INITIAL);
    tasks = new ZikraDbHelper(context);
    long study = tasks.addTask("DATA UJI rencana belajar", ZikraDbHelper.CATEGORY_STUDY);
    tasks.setCompleted(study, true);
    tasks.addTask("DATA UJI rencana berikutnya", ZikraDbHelper.CATEGORY_STUDY);
    tasks.addHifzTask("Latihan Al-Mulk", new HifzTarget(67, "Al-Mulk", 30, 1, 5));
    long finished =
        tasks.addHifzTask("Latihan Al-Ikhlas", new HifzTarget(112, "Al-Ikhlas", 4, 1, 4));
    tasks.setCompleted(finished, true);
    for (String name : new String[] {"Subuh", "Zuhur", "Asar", "Maghrib", "Isya"})
      prayers.add(tasks.addTask("Salat " + name, ZikraDbHelper.CATEGORY_IBADAH));
    tasks.setCompleted(prayers.get(2), true);
    android.content.ContentValues old = new android.content.ContentValues();
    old.put("task_date", LocationStore.today(context).minusDays(1).toString());
    old.put("title", "DATA UJI kemarin");
    old.put("category", ZikraDbHelper.CATEGORY_IBADAH);
    old.put("completed", 1);
    tasks.getWritableDatabase().insertOrThrow("tasks", null, old);
    try (FaithStore ignored = new FaithStore(context)) {
      ignored.getWritableDatabase();
    }
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      main(
          () -> {
            if (other != null) other.finish();
            if (page != null) page.finish();
          });
      if (tasks != null) tasks.close();
    } finally {
      super.tearDown();
    }
  }

  private void main(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }

  private void launch(Class<? extends Activity> type) {
    if (page != null) main(page::finish);
    page =
        getInstrumentation()
            .startActivitySync(new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }

  private void hifz() {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, StudyActivity.class)
                    .putExtra("category", ZikraDbHelper.CATEGORY_HIFZ)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }

  private String text(int id) {
    return ((TextView) page.findViewById(id)).getText().toString();
  }

  private String shell(String command) throws Exception {
    try (InputStream in =
        new ParcelFileDescriptor.AutoCloseInputStream(
            getInstrumentation().getUiAutomation().executeShellCommand(command))) {
      return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  private long count() {
    try (android.database.Cursor c =
        tasks.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tasks", null)) {
      c.moveToFirst();
      return c.getLong(0);
    }
  }

  private void shot(String label) {
    UiEvidence.capture(getInstrumentation(), "REFINED-" + label);
  }

  public void testProfileShowsActualWeekAndReadingWithoutWritingProgress() {
    long before = count();
    launch(ProfileActivity.class);
    assertEquals("Nadia", text(R.id.profileDisplayName));
    assertEquals("Al-Kahf · Ayat 12", text(R.id.profileLastRead));
    assertEquals(
        context.getString(R.string.refined_profile_week_count, 2), text(R.id.profileWeekCount));
    LinearLayout days = page.findViewById(R.id.profileWeekDays);
    assertEquals(7, days.getChildCount());
    int recorded = 0;
    for (int i = 0; i < 7; i++)
      if (days.getChildAt(i).getContentDescription().toString().endsWith("aktivitas tercatat"))
        recorded++;
    assertEquals(2, recorded);
    assertEquals(before, count());
    assertEquals(1, tasks.getProgress(ZikraDbHelper.CATEGORY_IBADAH).completed);
    shot("profile-personal");
    main(
        () ->
            page.findViewById(R.id.profileWeekCard)
                .requestRectangleOnScreen(
                    new Rect(
                        0,
                        0,
                        page.findViewById(R.id.profileWeekCard).getWidth(),
                        page.findViewById(R.id.profileWeekCard).getHeight()),
                    true));
    SystemClock.sleep(200);
    shot("profile-week");
    main(() -> ((ScrollView) page.findViewById(R.id.profileScroll)).fullScroll(View.FOCUS_DOWN));
    SystemClock.sleep(350);
    shot("profile-preferences");
  }

  public void testCalendarSevenColumnsFitAndDateSelectionPersists() {
    launch(CalendarActivity.class);
    assertGridFits();
    GridLayout grid = page.findViewById(R.id.calendarGrid);
    TextView candidate = null;
    for (int i = 0; i < grid.getChildCount(); i++) {
      TextView child = (TextView) grid.getChildAt(i);
      if (child.isClickable() && !child.isSelected()) {
        candidate = child;
        break;
      }
    }
    assertNotNull(candidate);
    TextView selected = candidate;
    main(selected::performClick);
    String selection = text(R.id.calendarSelection);
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(CalendarActivity.class.getName(), null, false);
    try {
      main(page::recreate);
      page = getInstrumentation().waitForMonitorWithTimeout(monitor, 15000);
      assertNotNull(page);
      getInstrumentation().waitForIdleSync();
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
    assertEquals(selection, text(R.id.calendarSelection));
    assertGridFits();
    shot("calendar-selected");
    main(() -> page.findViewById(R.id.calendarToday).performClick());
    assertTrue(
        text(R.id.calendarSelection)
            .contains(Integer.toString(LocationStore.today(context).getYear())));
    main(() -> page.findViewById(R.id.calendarNext).performClick());
    assertGridFits();
    main(() -> page.findViewById(R.id.calendarPrev).performClick());
    assertGridFits();
  }

  private void assertGridFits() {
    TextView month = page.findViewById(R.id.calendarMonth);
    assertNotNull(month.getLayout());
    assertTrue(
        "Month and Hijri year remain fully visible",
        month.getLayout().getHeight()
            <= month.getHeight()
                - month.getCompoundPaddingTop()
                - month.getCompoundPaddingBottom());
    LinearLayout weekdays = page.findViewById(R.id.calendarWeekdays);
    assertEquals(7, weekdays.getChildCount());
    for (int i = 0; i < weekdays.getChildCount(); i++) {
      TextView label = (TextView) weekdays.getChildAt(i);
      assertTrue(
          "Weekday fits its column",
          label.getPaint().measureText(label.getText().toString()) <= label.getWidth());
    }
    GridLayout grid = page.findViewById(R.id.calendarGrid);
    assertTrue(grid.getWidth() > 0);
    for (int i = 0; i < grid.getChildCount(); i++) {
      View cell = grid.getChildAt(i);
      assertTrue(
          "No calendar cell clipped horizontally",
          cell.getLeft() >= 0 && cell.getRight() <= grid.getWidth());
      if (cell.isClickable()) assertTrue(cell.getWidth() > 0);
    }
  }

  public void testHifzEmptyActionAndReadingNeverAutoCompleteTarget() throws Exception {
    tasks
        .getWritableDatabase()
        .delete("tasks", "category=?", new String[] {ZikraDbHelper.CATEGORY_HIFZ});
    hifz();
    assertEquals(View.GONE, page.findViewById(R.id.study_summary).getVisibility());
    assertEquals(context.getString(R.string.refined_hifz_empty_title), text(R.id.study_empty));
    assertTrue(page.findViewById(R.id.hifzEmptyAdd).isShown());
    shot("hifz-empty");
    main(() -> page.findViewById(R.id.hifzEmptyAdd).performClick());
    Field field = StudyActivity.class.getDeclaredField("editorDialog");
    field.setAccessible(true);
    Dialog editor = (Dialog) field.get(page);
    assertNotNull(editor);
    assertTrue(editor.isShowing());
    main(editor::dismiss);
    main(page::finish);
    page = null;
    long id = tasks.addHifzTask("DATA UJI rentang", new HifzTarget(1, "Al-Fatihah", 7, 2, 4));
    hifz();
    shot("hifz-target");
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(QuranActivity.class.getName(), null, false);
    try {
      main(() -> page.findViewById(R.id.agenda_feature_action).performClick());
      other = getInstrumentation().waitForMonitorWithTimeout(monitor, 15000);
      assertNotNull(other);
      assertEquals(1, other.getIntent().getIntExtra(QuranActivity.EXTRA_TARGET_CHAPTER, -1));
      assertEquals(2, other.getIntent().getIntExtra(QuranActivity.EXTRA_RANGE_FIRST, -1));
      assertEquals(4, other.getIntent().getIntExtra(QuranActivity.EXTRA_RANGE_LAST, -1));
      for (Task task : tasks.getTasks())
        if (task.id == id) assertFalse("Reading alone never completes a target", task.completed);
      main(other::finish);
      other = null;
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testSharedFeatureIconsAcrossHomeAndProfile() {
    launch(HomeActivity.class);
    Bitmap quran = drawable(firstImage(page.findViewById(R.id.dockQuran)));
    Bitmap study = drawable(firstImage(page.findViewById(R.id.dockStudy)));
    Bitmap ai = drawable(firstImage(page.findViewById(R.id.dockAi)));
    launch(ProfileActivity.class);
    assertTrue(quran.sameAs(drawable(firstImage(page.findViewById(R.id.dockQuran)))));
    assertTrue(study.sameAs(drawable(firstImage(page.findViewById(R.id.dockStudy)))));
    assertTrue(ai.sameAs(drawable(firstImage(page.findViewById(R.id.dockAi)))));
    assertTrue(quran.sameAs(drawable(firstImage(page.findViewById(R.id.dailyProfileQuran)))));
  }

  private ImageView firstImage(View view) {
    if (view instanceof ImageView) return (ImageView) view;
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        ImageView found = firstImage(((ViewGroup) view).getChildAt(i));
        if (found != null) return found;
      }
    return null;
  }

  private Bitmap drawable(ImageView view) {
    assertNotNull(view);
    Drawable d = view.getDrawable().getConstantState().newDrawable().mutate();
    d.clearColorFilter();
    d.setTint(Color.BLACK);
    d.setBounds(0, 0, 64, 64);
    Bitmap image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
    d.draw(new Canvas(image));
    return image;
  }

  public void testHomeClockWorshipMarksAndLiveClouds() throws Exception {
    launch(HomeActivity.class);
    assertTrue(text(R.id.homeWallClock).matches("[0-9]{2}\\.[0-9]{2}"));
    TextView dockLabel = page.findViewById(R.id.dockAi).findViewById(R.id.navDestinationLabel);
    Rect labelBounds = new Rect();
    assertTrue(dockLabel.getGlobalVisibleRect(labelBounds));
    int bottomInset =
        page.getWindow().getDecorView().getRootWindowInsets().getSystemWindowInsetBottom();
    assertTrue(
        "Dock label remains above navigation gesture area",
        labelBounds.bottom <= page.getWindow().getDecorView().getHeight() - bottomInset);
    assertTrue(text(R.id.homeClock).matches("[0-9]{2}:[0-9]{2}:[0-9]{2}"));
    assertEquals("WIB", text(R.id.homeWallZone));
    LinearLayout states = page.findViewById(R.id.homeWorshipStates);
    assertEquals(5, states.getChildCount());
    Set<String> actual = new HashSet<>();
    for (int i = 0; i < states.getChildCount(); i++)
      actual.add(states.getChildAt(i).getTag().toString());
    for (int i = 0; i < 5; i++)
      assertTrue(actual.contains("worship:" + prayers.get(i) + ":" + (i == 2)));
    main(
        () ->
            ((ScrollView) page.findViewById(R.id.homeScroll))
                .scrollTo(0, states.getTop() + ((View) states.getParent()).getTop()));
    SystemClock.sleep(400);
    shot("home-worship");
    main(() -> ((ScrollView) page.findViewById(R.id.homeScroll)).scrollTo(0, 0));
    HomeSkyView sky = page.findViewById(R.id.homeSky);
    long timeout = SystemClock.elapsedRealtime() + 10000;
    while (!sky.isMotionRunning() && SystemClock.elapsedRealtime() < timeout)
      SystemClock.sleep(100);
    assertTrue(sky.isMotionRunning());
    SystemClock.sleep(1000);
    Bitmap first = getInstrumentation().getUiAutomation().takeScreenshot();
    SystemClock.sleep(3500);
    Bitmap second = getInstrumentation().getUiAutomation().takeScreenshot();
    int[] position = new int[2];
    main(() -> sky.getLocationOnScreen(position));
    int changed = 0;
    for (int x = position[0] + sky.getWidth() * 45 / 100;
        x < position[0] + sky.getWidth() * 75 / 100;
        x += 5)
      for (int y = position[1] + sky.getHeight() * 23 / 100;
          y < position[1] + sky.getHeight() * 40 / 100;
          y += 5)
        if (x < first.getWidth()
            && y < first.getHeight()
            && first.getPixel(x, y) != second.getPixel(x, y)) changed++;
    assertTrue("Visible cloud area changes over real frames", changed > 20);
    first.recycle();
    second.recycle();
    shot("home-clock-sky");
    String old = shell("settings get global animator_duration_scale").trim();
    try {
      shell("settings put global animator_duration_scale 0");
      SystemClock.sleep(500);
      assertFalse("System motion preference respected", sky.isMotionRunning());
    } finally {
      shell(
          old.equals("null")
              ? "settings delete global animator_duration_scale"
              : "settings put global animator_duration_scale " + old);
    }
  }
}
