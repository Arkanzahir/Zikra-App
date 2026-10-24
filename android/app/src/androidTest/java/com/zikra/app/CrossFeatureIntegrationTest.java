package com.zikra.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.location.CitySearch;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.study.ScheduleEditor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Targeted integration checks; persistence probes require a separate force-stop between runs. */
@SuppressWarnings("deprecation")
public final class CrossFeatureIntegrationTest extends InstrumentationTestCase {
  private Context context;
  private final List<Activity> pages = new ArrayList<>();
  private final Map<String, Map<String, ?>> originals = new HashMap<>();
  private boolean keepProbeState;
  private static final String[] PREFS = {"zikra_preferences", LocationStore.PREFS};

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    // Test fixtures must never replace settings on a user's physical phone.
    try (java.io.InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))) {
      assertEquals("Emulator-only integration fixtures", "1",
          new String(input.readAllBytes(), StandardCharsets.UTF_8).trim());
    }
    for (String name : PREFS)
      originals.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
    context.getSharedPreferences("zikra_preferences", 0).edit()
        .putBoolean("onboarding_complete", true).putBoolean("reminder_enabled", false).commit();
  }

  private <T extends Activity> T launch(Class<T> type) {
    T page = type.cast(getInstrumentation().startActivitySync(
        new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
    pages.add(page);
    getInstrumentation().waitForIdleSync();
    return page;
  }

  private void finish(Activity page) {
    getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
  }

  private static Object field(Object owner, String name) throws Exception {
    Field f = owner.getClass().getDeclaredField(name);
    f.setAccessible(true);
    return f.get(owner);
  }

  private static String text(Activity page, int id) {
    return ((TextView) page.findViewById(id)).getText().toString();
  }

  private static TextView containing(View root, String text) {
    if (root instanceof TextView && ((TextView) root).getText().toString().contains(text))
      return (TextView) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        TextView found = containing(group.getChildAt(i), text);
        if (found != null) return found;
      }
    }
    return null;
  }

  public void testSelectedCityUsedAcrossMenusAndReopenedCalendar() throws Exception {
    LocationPoint city = CitySearch.bundled("Jayapura").get(0);
    LocationStore.save(context, city);
    for (Class<? extends Activity> type : new Class[] {HomeActivity.class, PrayerActivity.class,
        QiblaActivity.class, ProtectorActivity.class, WeatherActivity.class, NearbyMosquesActivity.class}) {
      Activity page = launch(type);
      assertEquals(type.getSimpleName(), city.key(), ((LocationPoint) field(page, "location")).key());
      assertNotNull(type.getSimpleName() + " shows selected city",
          containing(page.getWindow().getDecorView(), "Jayapura"));
      finish(page);
    }
    CalendarActivity calendar = launch(CalendarActivity.class);
    assertEquals(LocationStore.today(context), field(calendar, "today"));
    finish(calendar);
    HomeActivity home = launch(HomeActivity.class);
    assertEquals(city.key(), ((LocationPoint) field(home, "location")).key());
    assertEquals("Asia/Jayapura", LocationStore.current(context).zone.getId());
  }

  public void testCityMidnightChangesDailyChecklistWithoutLosingStudy() throws Exception {
    Instant now = Instant.parse("2026-10-24T16:30:00Z");
    Clock fixed = Clock.fixed(now, ZoneOffset.UTC);
    Constructor<ZikraDbHelper> constructor = ZikraDbHelper.class.getDeclaredConstructor(
        Context.class, String.class, Clock.class, boolean.class);
    constructor.setAccessible(true);
    String name = "a04-zone-" + System.nanoTime() + ".db";
    ZikraDbHelper db = constructor.newInstance(context, name, fixed, true);
    try {
      LocationStore.save(context, CitySearch.bundled("Jakarta").get(0));
      assertEquals(LocalDate.of(2026, 10, 24), LocationStore.localDate(context, now));
      long prayer = db.addTask("A04 daily check", ZikraDbHelper.CATEGORY_IBADAH);
      long study = db.addAcademicTask("A04 persistent study", now.toEpochMilli(), false);
      db.setCompleted(prayer, true);
      db.setCompleted(study, true);
      LocationStore.save(context, CitySearch.bundled("Jayapura").get(0));
      assertEquals(LocalDate.of(2026, 10, 25), LocationStore.localDate(context, now));
      assertEquals(0, db.getProgress(ZikraDbHelper.CATEGORY_IBADAH).total);
      assertEquals(1, db.getProgress(ZikraDbHelper.CATEGORY_STUDY).completed);
      assertTrue(ScheduleEditor.label(context, now.toEpochMilli()).contains("25 Okt 2026"));
      assertTrue(ScheduleEditor.label(context, now.toEpochMilli()).contains("01:30"));
      LocationStore.save(context, CitySearch.bundled("Jakarta").get(0));
      assertEquals(1, db.getProgress(ZikraDbHelper.CATEGORY_IBADAH).completed);
    } finally {
      db.close();
      context.deleteDatabase(name);
    }
  }

  public void testMushafPositionRefreshesExistingCatalogAndHome() throws Exception {
    context.getSharedPreferences("zikra_preferences", 0).edit()
        .putInt("last_chapter", 1).putInt("last_ayah", 1)
        .putString("last_surah", "Al-Fatihah").putString("last_verse_key", "1:1").commit();
    HomeActivity home = launch(HomeActivity.class);
    QuranActivity catalog = launch(QuranActivity.class);
    MushafActivity mushaf = launch(MushafActivity.class);
    // This is the native handler used when a verse is tapped, independent of tile/font downloads.
    getInstrumentation().runOnMainSync(() -> mushaf.verse(112, 4));
    assertEquals("112:4", context.getSharedPreferences("zikra_preferences", 0)
        .getString("last_verse_key", ""));
    finish(mushaf);
    assertTrue(text(catalog, R.id.lastReadLabel).contains("Al-Ikhlas"));
    assertTrue(text(catalog, R.id.lastReadLabel).contains("Ayat 4"));
    finish(catalog);
    assertTrue(text(home, R.id.homeLastRead).contains("Al-Ikhlas"));
    assertTrue(text(home, R.id.homeLastRead).contains("Ayat 4"));
    finish(home);
    HomeActivity reopened = launch(HomeActivity.class);
    assertTrue(text(reopened, R.id.homeLastRead).contains("Ayat 4"));
    UiEvidence.capture(getInstrumentation(), "A04-Home-Resume");
  }

  private File probeFile() {
    return new File(context.getFilesDir(), "a04-integration-probe.json");
  }

  public void testSeedPersistentIntegrationProbe() throws Exception {
    assertFalse("Finish/clean previous probe before seeding", probeFile().exists());
    JSONObject probe = new JSONObject();
    JSONObject backups = new JSONObject();
    // Only these non-secret settings are touched by the cross-process probe.
    String[] keys = {"profile_display_name", "last_chapter", "last_ayah", "last_surah",
        "last_verse_key", "onboarding_complete", "reminder_enabled"};
    for (String name : PREFS) {
      JSONObject values = new JSONObject();
      Map<String, ?> original = originals.get(name);
      for (String key : name.equals(LocationStore.PREFS) ? original.keySet().toArray(new String[0]) : keys)
        values.put(key, original.containsKey(key) ? original.get(key) : JSONObject.NULL);
      backups.put(name, values);
    }
    probe.put("originalSettings", backups);
    ZikraDbHelper db = new ZikraDbHelper(context);
    try {
      LocationStore.save(context, CitySearch.bundled("Makassar").get(0));
      JSONArray ids = new JSONArray();
      probe.put("ids", ids);
      // Persist recovery metadata before and after each fixture insertion.
      Files.write(probeFile().toPath(), probe.toString().getBytes(StandardCharsets.UTF_8));
      long first = db.addAcademicTask("A04 reopen target", 0, false);
      ids.put(first);
      Files.write(probeFile().toPath(), probe.toString().getBytes(StandardCharsets.UTF_8));
      long second = db.addAcademicTask("A04 reopen second", 0, false);
      ids.put(second);
      db.setCompleted(first, true);
      ZikraDbHelper.Progress expected = db.getProgress(ZikraDbHelper.CATEGORY_STUDY);
      probe.put("completed", expected.completed).put("total", expected.total);
      probe.put("seedProcessPid", android.os.Process.myPid());
      Files.write(probeFile().toPath(), probe.toString().getBytes(StandardCharsets.UTF_8));
      context.getSharedPreferences("zikra_preferences", 0).edit()
          .putString("profile_display_name", "A04 Probe")
          .putInt("last_chapter", 112).putInt("last_ayah", 3)
          .putString("last_surah", "Al-Ikhlas").putString("last_verse_key", "112:3").commit();
      ProfileActivity profile = launch(ProfileActivity.class);
      assertEquals(expected.completed + " / " + expected.total, text(profile, R.id.profileProgressPercent));
      keepProbeState = true;
    } finally {
      db.close();
    }
  }

  public void testVerifyPersistentIntegrationProbeAfterForceStop() throws Exception {
    assertTrue("Seed probe in a previous process", probeFile().isFile());
    JSONObject probe = new JSONObject(new String(Files.readAllBytes(probeFile().toPath()), StandardCharsets.UTF_8));
    ZikraDbHelper db = new ZikraDbHelper(context);
    try {
      assertTrue("Verification runs in a different Android process",
          probe.getInt("seedProcessPid") != android.os.Process.myPid());
      assertEquals("Asia/Makassar", LocationStore.current(context).zone.getId());
      HomeActivity home = launch(HomeActivity.class);
      assertTrue(text(home, R.id.homeLastRead).contains("Ayat 3"));
      finish(home);
      QuranActivity quran = launch(QuranActivity.class);
      assertTrue(text(quran, R.id.lastReadLabel).contains("Ayat 3"));
      finish(quran);
      StudyActivity study = launch(StudyActivity.class);
      assertEquals(probe.getInt("completed"), db.getProgress(ZikraDbHelper.CATEGORY_STUDY).completed);
      assertEquals(probe.getInt("total"), db.getProgress(ZikraDbHelper.CATEGORY_STUDY).total);
      assertEquals(probe.getInt("completed") + "/" + probe.getInt("total"),
          text(study, R.id.study_progress_label));
      finish(study);
      ProfileActivity profile = launch(ProfileActivity.class);
      assertEquals("A04 Probe", text(profile, R.id.profileDisplayName));
      assertEquals(probe.getInt("completed") + " / " + probe.getInt("total"),
          text(profile, R.id.profileProgressPercent));
      UiEvidence.capture(getInstrumentation(), "A04-Profile-After-Restart");
    } finally {
      JSONArray ids = probe.getJSONArray("ids");
      for (int i = 0; i < ids.length(); i++) db.deleteTask(ids.getLong(i));
      db.close();
      for (String name : PREFS) {
        SharedPreferences.Editor edit = context.getSharedPreferences(name, 0).edit();
        if (name.equals(LocationStore.PREFS)) edit.clear();
        JSONObject values = probe.getJSONObject("originalSettings").getJSONObject(name);
        for (java.util.Iterator<String> it = values.keys(); it.hasNext();) {
          String key = it.next();
          if (values.isNull(key)) edit.remove(key);
          else {
            Object value = values.get(key);
            if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
            else if (value instanceof String) edit.putString(key, (String) value);
            else if (name.equals(LocationStore.PREFS) && key.equals("accuracy"))
              edit.putFloat(key, ((Number) value).floatValue());
            else if (name.equals(LocationStore.PREFS)) edit.putLong(key, ((Number) value).longValue());
            else edit.putInt(key, ((Number) value).intValue());
          }
        }
        edit.commit();
      }
      Files.delete(probeFile().toPath());
      // The saved pre-seed settings now supersede this method's post-seed setup snapshot.
      keepProbeState = true;
    }
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      for (int i = pages.size() - 1; i >= 0; i--)
        if (!pages.get(i).isDestroyed()) finish(pages.get(i));
      if (!keepProbeState) for (String name : PREFS) {
        SharedPreferences.Editor edit = context.getSharedPreferences(name, 0).edit().clear();
        for (Map.Entry<String, ?> entry : originals.get(name).entrySet()) {
          Object value = entry.getValue(); String key = entry.getKey();
          if (value instanceof String) edit.putString(key, (String) value);
          else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
          else if (value instanceof Integer) edit.putInt(key, (Integer) value);
          else if (value instanceof Long) edit.putLong(key, (Long) value);
          else if (value instanceof Float) edit.putFloat(key, (Float) value);
          else if (value instanceof java.util.Set)
            edit.putStringSet(key, new java.util.HashSet<>((java.util.Set<String>) value));
        }
        edit.commit();
      }
    } finally {
      super.tearDown();
    }
  }
}
