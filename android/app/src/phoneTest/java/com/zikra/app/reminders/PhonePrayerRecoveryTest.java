package com.zikra.app.reminders;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import com.zikra.app.PrayerActivity;
import com.zikra.app.location.LocationStore;
import com.zikra.app.study.StudyAlarmPolicy;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

/**
 * Host backup and explicit physical-phone coordination are mandatory; never run the whole suite
 * blindly.
 */
@SuppressWarnings("deprecation")
public final class PhonePrayerRecoveryTest extends InstrumentationTestCase {
  private android.content.Context context;
  private File state;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    assertFalse(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    assertTrue(new File(context.getFilesDir(), "d02-phone-recovery-20261111-authorized").isFile());
    state = new File(context.getFilesDir(), "d02-phone-recovery-state-private.json");
  }

  private void main(Runnable action) {
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
    getInstrumentation().waitForIdleSync();
    if (failure.get() != null) throw new AssertionError(failure.get());
  }

  private void write(String name, JSONObject value) throws Exception {
    Files.write(
        new File(context.getFilesDir(), name).toPath(),
        value.toString(2).getBytes(StandardCharsets.UTF_8));
  }

  private JSONObject read(String name) throws Exception {
    return new JSONObject(
        new String(
            Files.readAllBytes(new File(context.getFilesDir(), name).toPath()),
            StandardCharsets.UTF_8));
  }

  private JSONObject snapshot(SharedPreferences prefs) throws Exception {
    JSONObject result = new JSONObject();
    for (Map.Entry<String, ?> row : prefs.getAll().entrySet()) {
      Object value = row.getValue();
      String type = value instanceof Set ? "set" : value.getClass().getSimpleName();
      result.put(
          row.getKey(),
          new JSONObject()
              .put("type", type)
              .put(
                  "value",
                  value instanceof Set ? new JSONArray(new ArrayList<>((Set<?>) value)) : value));
    }
    return result;
  }

  private void restore(SharedPreferences prefs, JSONObject values) throws Exception {
    SharedPreferences.Editor edit = prefs.edit().clear();
    Iterator<String> keys = values.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      JSONObject row = values.getJSONObject(key);
      switch (row.getString("type")) {
        case "String":
          edit.putString(key, row.getString("value"));
          break;
        case "Boolean":
          edit.putBoolean(key, row.getBoolean("value"));
          break;
        case "Integer":
          edit.putInt(key, row.getInt("value"));
          break;
        case "Long":
          edit.putLong(key, row.getLong("value"));
          break;
        case "Float":
          edit.putFloat(key, (float) row.getDouble("value"));
          break;
        case "set":
          Set<String> set = new HashSet<>();
          JSONArray array = row.getJSONArray("value");
          for (int i = 0; i < array.length(); i++) set.add(array.getString(i));
          edit.putStringSet(key, set);
          break;
        default:
          throw new IllegalStateException("Unsupported preference type");
      }
    }
    assertTrue(edit.commit());
  }

  private void sameSnapshot(JSONObject expected, JSONObject actual, String except)
      throws Exception {
    Set<String> before = new HashSet<>(), after = new HashSet<>();
    expected.keys().forEachRemaining(before::add);
    actual.keys().forEachRemaining(after::add);
    if (except != null) {
      before.remove(except);
      after.remove(except);
    }
    assertEquals("Preference keys changed outside the test", before, after);
    for (String key : before) {
      JSONObject left = expected.getJSONObject(key), right = actual.getJSONObject(key);
      assertEquals(
          "Preference type changed: " + key, left.getString("type"), right.getString("type"));
      if ("set".equals(left.getString("type"))) {
        Set<String> a = new HashSet<>(), b = new HashSet<>();
        JSONArray x = left.getJSONArray("value"), y = right.getJSONArray("value");
        for (int i = 0; i < x.length(); i++) a.add(x.getString(i));
        for (int i = 0; i < y.length(); i++) b.add(y.getString(i));
        assertEquals("Preference changed: " + key, a, b);
      } else {
        switch (left.getString("type")) {
          case "Long":
            assertEquals(key, left.getLong("value"), right.getLong("value"));
            break;
          case "Integer":
            assertEquals(key, left.getInt("value"), right.getInt("value"));
            break;
          case "Float":
            assertEquals(
                key, (float) left.getDouble("value"), (float) right.getDouble("value"), 0f);
            break;
          default:
            assertEquals("Preference changed: " + key, left.get("value"), right.get("value"));
        }
      }
    }
  }

  public void testArmRebootWithoutAudio() throws Exception {
    assertFalse("Do not overwrite an existing recovery session", state.exists());
    assertTrue(PrayerReminders.canNotify(context));
    assertTrue(StudyAlarmPolicy.exact(context));
    List<PrayerAlarmPlan.Entry> upcoming =
        PrayerAlarmPlan.next(System.currentTimeMillis(), LocationStore.current(context));
    assertFalse(upcoming.isEmpty());
    assertTrue(
        "Do not alter modes near an actual prayer",
        upcoming.get(0).epoch - System.currentTimeMillis() > 180000);
    JSONObject original =
        new JSONObject()
            .put("prefs", snapshot(PrayerSettings.prefs(context)))
            .put("plan", snapshot(context.getSharedPreferences(PrayerReminders.PLAN, 0)))
            .put("location", LocationStore.current(context).key());
    write(state.getName(), original);
    main(
        () -> {
          PrayerSettings.prefs(context).edit().putBoolean("reminder_enabled", true).commit();
          for (int index : new int[] {0, 2, 3, 4, 5})
            PrayerSettings.setMode(
                context,
                index,
                index == 3
                    ? PrayerMode.OFF
                    : index == 2 ? PrayerMode.VIBRATE : PrayerMode.NOTIFICATION);
          PrayerReminders.sync(context);
        });
    original.put("expected_prefs", snapshot(PrayerSettings.prefs(context)));
    write(state.getName(), original);
    JSONObject armed =
        new JSONObject()
            .put("plan", snapshot(context.getSharedPreferences(PrayerReminders.PLAN, 0)))
            .put("revision", PrayerSettings.revision(context))
            .put("location", LocationStore.current(context).key());
    write("d02-reboot-expected-private.json", armed);
    write(
        "d02-reboot-armed.json",
        new JSONObject()
            .put("passed", true)
            .put(
                "alarmSlots",
                context
                    .getSharedPreferences(PrayerReminders.PLAN, 0)
                    .getStringSet("ids", Collections.emptySet())
                    .size())
            .put("fixtureHasNoAdhanMode", true)
            .put("rebootPerformedByTest", false));
  }

  public void testInspectAfterRealReboot() throws Exception {
    assertTrue(state.isFile());
    JSONObject expected = read("d02-reboot-expected-private.json");
    assertEquals(expected.getString("location"), LocationStore.current(context).key());
    assertEquals(expected.getLong("revision"), PrayerSettings.revision(context));
    assertTrue(PrayerReminders.enabled(context));
    for (int index : new int[] {0, 2, 3, 4, 5})
      assertEquals(
          index == 3 ? PrayerMode.OFF : index == 2 ? PrayerMode.VIBRATE : PrayerMode.NOTIFICATION,
          PrayerSettings.mode(context, index));
    JSONObject before = expected.getJSONObject("plan"),
        after = snapshot(context.getSharedPreferences(PrayerReminders.PLAN, 0));
    Iterator<String> keys = before.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      if (!"ids".equals(key))
        assertEquals(
            "Recovered plan: " + key,
            before.getJSONObject(key).toString(),
            after.getJSONObject(key).toString());
    }
    Set<String> ids =
        context
            .getSharedPreferences(PrayerReminders.PLAN, 0)
            .getStringSet("ids", Collections.emptySet());
    JSONArray oldIds = before.getJSONObject("ids").getJSONArray("value");
    Set<String> expectedIds = new HashSet<>();
    for (int i = 0; i < oldIds.length(); i++) expectedIds.add(oldIds.getString(i));
    assertEquals(expectedIds, ids);
    write(
        "d02-reboot-inspected.json",
        new JSONObject()
            .put("passed", true)
            .put("alarmSlots", ids.size())
            .put("modesPreserved", true)
            .put("locationPreserved", true)
            .put("syncCalledByThisTest", false)
            .put("systemAlarmsMustBeCapturedByHostBeforeInstrumentation", true));
  }

  public void testAlarmDuringForcedIdle() throws Exception {
    assertTrue(state.isFile());
    assertTrue(StudyAlarmPolicy.exact(context));
    JSONObject original = read(state.getName());
    sameSnapshot(
        original.getJSONObject("expected_prefs"), snapshot(PrayerSettings.prefs(context)), null);
    Activity page =
        getInstrumentation()
            .startActivitySync(
                new Intent(context, PrayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      main(
          () -> {
            PrayerSettings.prefs(context)
                .edit()
                .putBoolean("reminder_enabled", true)
                .putBoolean(PrayerSettings.PAUSE_FOCUS, false)
                .putBoolean(PrayerSettings.QUIET_CLASSES, false)
                .remove("prayer_delivered_2")
                .commit();
            PrayerSettings.setMode(context, 2, PrayerMode.ADHAN);
            PrayerReminders.sync(context);
          });
      long epoch = System.currentTimeMillis() + 45000;
      original
          .put("expected_prefs", snapshot(PrayerSettings.prefs(context)))
          .put("test_epoch", epoch);
      write(state.getName(), original);
      Intent event = PrayerReminders.alarmIntent(context, 2, epoch);
      String slot = event.getStringExtra("slot");
      SharedPreferences plan = context.getSharedPreferences(PrayerReminders.PLAN, 0);
      Set<String> ids = new HashSet<>(plan.getStringSet("ids", Collections.emptySet()));
      ids.add(slot);
      assertTrue(
          plan.edit()
              .putStringSet("ids", ids)
              .putLong("e:" + slot, epoch)
              .putString("l:" + slot, LocationStore.current(context).key())
              .putLong("r:" + slot, PrayerSettings.revision(context))
              .commit());
      android.app.PendingIntent pending =
          android.app.PendingIntent.getBroadcast(
              context,
              0,
              event,
              android.app.PendingIntent.FLAG_UPDATE_CURRENT
                  | android.app.PendingIntent.FLAG_IMMUTABLE);
      StudyAlarmPolicy.schedule(context, epoch, pending);
      main(() -> page.moveTaskToBack(true));
      write(
          "d02-idle-armed.json", new JSONObject().put("scheduledEpoch", epoch).put("exact", true));
      long deadline = SystemClock.elapsedRealtime() + 75000;
      while (!AdhanService.state().playing && SystemClock.elapsedRealtime() < deadline)
        SystemClock.sleep(100);
      assertTrue("Real alarm should play during forced idle", AdhanService.state().playing);
      PowerManager power = context.getSystemService(PowerManager.class);
      JSONObject proof =
          new JSONObject()
              .put("playing", true)
              .put("preview", AdhanService.state().preview)
              .put("screenOff", !power.isInteractive())
              .put("deviceIdle", power.isDeviceIdleMode())
              .put("powerSave", power.isPowerSaveMode())
              .put("scheduledEpoch", epoch)
              .put(
                  "deliveredEpoch", PrayerSettings.prefs(context).getLong("prayer_delivered_2", -1))
              .put("latenessMs", System.currentTimeMillis() - epoch);
      write("d02-idle-playing.json", proof);
      assertFalse(proof.getBoolean("preview"));
      assertTrue(proof.getBoolean("screenOff"));
      assertTrue(proof.getBoolean("deviceIdle"));
      assertTrue(proof.getBoolean("powerSave"));
      assertEquals(epoch, proof.getLong("deliveredEpoch"));
      SystemClock.sleep(5000);
      proof.put("passed", true);
      write("d02-idle-playing.json", proof);
    } finally {
      main(
          () -> {
            AdhanService.stop(context);
            page.finish();
          });
    }
  }

  public void testRestoreOriginalPreferences() throws Exception {
    if (!state.isFile()) return;
    JSONObject original = read(state.getName());
    assertEquals(
        "Do not overwrite a concurrent user location change",
        original.getString("location"),
        LocationStore.current(context).key());
    JSONObject current = snapshot(PrayerSettings.prefs(context));
    if (original.has("expected_prefs")) {
      String except = original.has("test_epoch") ? "prayer_delivered_2" : null;
      sameSnapshot(original.getJSONObject("expected_prefs"), current, except);
      if (except != null && current.has(except))
        assertEquals(
            "Only the synthetic delivery may be restored",
            original.getLong("test_epoch"),
            current.getJSONObject(except).getLong("value"));
    }
    main(
        () -> {
          AdhanService.stop(context);
          PrayerSettings.prefs(context).edit().putBoolean("reminder_enabled", false).commit();
          PrayerReminders.sync(context);
        });
    restore(PrayerSettings.prefs(context), original.getJSONObject("prefs"));
    restore(context.getSharedPreferences(PrayerReminders.PLAN, 0), original.getJSONObject("plan"));
    main(() -> PrayerReminders.sync(context));
    JSONObject restored = snapshot(PrayerSettings.prefs(context)),
        old = original.getJSONObject("prefs");
    sameSnapshot(old, restored, null);
    write(
        "d02-restored.json",
        new JSONObject()
            .put("passed", true)
            .put("preferencesRestored", true)
            .put("locationUnchanged", true)
            .put("mainAppUninstalled", false));
  }
}
