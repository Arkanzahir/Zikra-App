package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.data.Task;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.discovery.HijriCalendarModel;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.HijrahDate;

/** User-visible utility routes, truthful local identity, and enlarged-text regression checks. */
@SuppressWarnings("deprecation")
public final class UtilitiesPolishTest extends InstrumentationTestCase {
  private Activity page;

  private <T extends Activity> T launch(Class<T> type) {
    getInstrumentation()
        .getTargetContext()
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putBoolean("onboarding_complete", true)
        .commit();
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    return type.cast(page);
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }

  public void testProfileUsesAcademicProgressAndDoesNotInventIdentity() {
    ProfileActivity profile = launch(ProfileActivity.class);
    int completed = 0, total = 0;
    try (ZikraDbHelper database = new ZikraDbHelper(getInstrumentation().getTargetContext())) {
      for (Task task : database.getTasks()) {
        if (!ZikraDbHelper.CATEGORY_STUDY.equals(task.category)) continue;
        total++;
        if (task.completed) completed++;
      }
    }
    assertEquals(
        "Pengguna Zikra",
        ((TextView) profile.findViewById(R.id.profileDisplayName)).getText().toString());
    String caption =
        ((TextView) profile.findViewById(R.id.profileProgressCaption)).getText().toString();
    assertTrue(caption.contains("tugas akademik"));
    if (total > 0) assertEquals(completed + " dari " + total + " tugas akademik selesai", caption);
    UiEvidence.capture(getInstrumentation(), "UtilityProfile");
    Instrumentation.ActivityMonitor monitor =
        getInstrumentation().addMonitor(StudyActivity.class.getName(), null, false);
    try {
      getInstrumentation()
          .runOnMainSync(() -> profile.findViewById(R.id.profileProgressCard).performClick());
      Activity target = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
      assertNotNull("Academic progress must open Study Hub", target);
      assertNull(
          "The academic destination must not request Hifz/Ibadah scope",
          target.getIntent().getStringExtra("category"));
      getInstrumentation().runOnMainSync(target::finish);
    } finally {
      getInstrumentation().removeMonitor(monitor);
    }
  }

  public void testCalendarRefreshesTodayWhileKeepingABrowsedDate() {
    CalendarActivity calendar = launch(CalendarActivity.class);
    LocalDate nextDay = LocalDate.now(ZoneId.of("Asia/Jakarta")).plusDays(1);
    getInstrumentation().runOnMainSync(() -> calendar.refreshToday(nextDay));
    assertTrue(
        ((TextView) calendar.findViewById(R.id.calendarSelection))
            .getText()
            .toString()
            .startsWith(HijriCalendarModel.label(HijrahDate.from(nextDay))));
    getInstrumentation()
        .runOnMainSync(() -> calendar.findViewById(R.id.calendarNext).performClick());
    String browsed =
        ((TextView) calendar.findViewById(R.id.calendarSelection)).getText().toString();
    getInstrumentation().runOnMainSync(() -> calendar.refreshToday(nextDay.plusDays(1)));
    assertEquals(
        "Midnight refresh must preserve a deliberately selected date",
        browsed,
        ((TextView) calendar.findViewById(R.id.calendarSelection)).getText().toString());
    UiEvidence.capture(getInstrumentation(), "UtilityCalendar");
  }

  public void testDuaSearchAndExpandWorkWithoutAnArabicVoice() {
    DuaActivity dua = launch(DuaActivity.class);
    LinearLayout list = dua.findViewById(R.id.duaContainer);
    View first = list.getChildAt(0);
    getInstrumentation().runOnMainSync(() -> first.findViewById(R.id.duaExpand).performClick());
    assertEquals(View.VISIBLE, first.findViewById(R.id.duaContent).getVisibility());
    assertTrue(
        ((TextView) first.findViewById(R.id.duaSource)).getText().toString().contains("20:114"));
    getInstrumentation()
        .runOnMainSync(
            () -> ((EditText) dua.findViewById(R.id.duaSearch)).setText("no-match-zz91"));
    assertEquals(View.VISIBLE, dua.findViewById(R.id.duaEmpty).getVisibility());
    getInstrumentation()
        .runOnMainSync(() -> ((EditText) dua.findViewById(R.id.duaSearch)).setText(""));
    assertEquals(View.VISIBLE, first.getVisibility());
    UiEvidence.capture(getInstrumentation(), "UtilityDua");
  }

  public void testWrittenAssistantRestoresHistoryAndUsesTheRealHifzDestination() {
    SharedPreferences preferences =
        getInstrumentation().getTargetContext().getSharedPreferences("zikra_preferences", 0);
    boolean hadHistory = preferences.contains("assistant_history_v1");
    String previousHistory = preferences.getString("assistant_history_v1", "[]");
    try {
      preferences.edit().remove("assistant_history_v1").commit();
      AiActivity assistant = launch(AiActivity.class);
      getInstrumentation()
          .runOnMainSync(
              () -> {
                ((EditText) assistant.findViewById(R.id.aiInput)).setText("Bantu target hafalan");
                assistant.findViewById(R.id.aiSend).performClick();
              });
      LinearLayout thread = assistant.findViewById(R.id.aiChatThread);
      assertEquals(2, thread.getChildCount());
      assertTrue(preferences.getString("assistant_history_v1", "").contains("Diary Hifz"));
      Instrumentation.ActivityMonitor monitor =
          getInstrumentation().addMonitor(AiActivity.class.getName(), null, false);
      try {
        getInstrumentation().runOnMainSync(assistant::recreate);
        Activity restored = getInstrumentation().waitForMonitorWithTimeout(monitor, 30000);
        assertNotNull("A recreated assistant must reopen", restored);
        page = restored;
        getInstrumentation().waitForIdleSync();
        assertEquals(2, ((LinearLayout) restored.findViewById(R.id.aiChatThread)).getChildCount());
        UiEvidence.capture(getInstrumentation(), "UtilityAssistant");
      } finally {
        getInstrumentation().removeMonitor(monitor);
      }
    } finally {
      SharedPreferences.Editor editor = preferences.edit();
      if (hadHistory) editor.putString("assistant_history_v1", previousHistory);
      else editor.remove("assistant_history_v1");
      editor.commit();
    }
  }

  public void testLargeFontAssistantKeepsActionsAndComposerUsable() throws Exception {
    float previous =
        android.provider.Settings.System.getFloat(
            getInstrumentation().getTargetContext().getContentResolver(), "font_scale", 1f);
    try {
      shell("settings put system font_scale 2.0");
      SystemClock.sleep(1500);
      AiActivity assistant = launch(AiActivity.class);
      assertTrue(
          "The enlarged-font setting must actually reach the Activity",
          assistant.getResources().getConfiguration().fontScale >= 1.9f);
      LinearLayout actions = assistant.findViewById(R.id.aiEntryActions);
      assertEquals(LinearLayout.VERTICAL, actions.getOrientation());
      for (int i = 0; i < actions.getChildCount(); i++) {
        LinearLayout tile = (LinearLayout) actions.getChildAt(i);
        View last = tile.getChildAt(tile.getChildCount() - 1);
        assertTrue(
            "Entry text must fit inside its growing tile",
            tile.getHeight() >= last.getBottom() + tile.getPaddingBottom());
      }
      float density = assistant.getResources().getDisplayMetrics().density;
      assertTrue(assistant.findViewById(R.id.aiSend).getWidth() >= Math.round(48 * density));
      String heading = ((TextView) assistant.findViewById(R.id.detailTitle)).getText().toString();
      assertEquals("Pendamping Zikra", heading);
      UiEvidence.capture(getInstrumentation(), "UtilityAssistantLargeFont");
    } finally {
      if (page != null) {
        getInstrumentation().runOnMainSync(page::finish);
        page = null;
      }
      shell("settings put system font_scale " + previous);
      SystemClock.sleep(1000);
    }
  }

  private void shell(String command) throws Exception {
    try (ParcelFileDescriptor descriptor =
            getInstrumentation().getUiAutomation().executeShellCommand(command);
        java.io.InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
      while (input.read() != -1) {}
    }
  }
}
