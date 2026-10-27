package com.zikra.app.faith;

import android.test.AndroidTestCase;
import com.zikra.app.data.*;
import java.time.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class FaithStorageTest extends AndroidTestCase {
  private static final String NAME = "d05-storage.db", TASKS = "d05-tasks.db";
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-27T05:00:00Z"), ZoneId.of("Asia/Jakarta"));
  private FaithStore store;
  private ZikraDbHelper tasks;
  private LocalDate day = LocalDate.of(2026, 10, 27);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getContext().deleteDatabase(NAME);
    getContext().deleteDatabase(TASKS);
    store = new FaithStore(getContext(), NAME, CLOCK);
    tasks = new ZikraDbHelper(getContext(), TASKS, CLOCK);
  }

  @Override
  protected void tearDown() throws Exception {
    store.close();
    tasks.close();
    getContext().deleteDatabase(NAME);
    getContext().deleteDatabase(TASKS);
    super.tearDown();
  }

  private FaithStore.Entry entry(int type, int first, int last, long plan) {
    return new FaithStore.Entry(
        0, 0, UUID.randomUUID().toString(), type, day, 1, first, last, "Data uji", 0, plan);
  }

  private void rejects(Runnable operation) {
    try {
      operation.run();
      fail("Invalid/stale mutation accepted");
    } catch (IllegalArgumentException | IllegalStateException expected) {
    }
  }

  public void testUniqueCoverageReceiptsEditDeleteAndStaleConflictsSurviveRestart() {
    long plan = store.savePlan(30, day, false);
    FaithStore.Entry a = entry(2, 1, 4, plan), b = entry(2, 2, 7, plan);
    long id = store.save(a);
    assertEquals(id, store.save(a));
    store.save(b);
    assertEquals(7, store.coverage(plan).cardinality());
    store.close();
    store = new FaithStore(getContext(), NAME, CLOCK);
    assertEquals(2, store.entries(day).size());
    FaithStore.Entry old = store.entries(day).get(0);
    FaithStore.Entry edit =
        new FaithStore.Entry(
            old.id, old.revision, old.token, old.kind, old.day, 1, 3, 4, "Ubah", 0, plan);
    store.save(edit);
    assertEquals(4, store.coverage(plan).cardinality());
    rejects(() -> store.save(edit));
    assertFalse(store.delete(old));
    FaithStore.Entry updated = store.entries(day).get(0);
    assertTrue(store.delete(updated));
    assertEquals(4, store.coverage(plan).cardinality());
  }

  public void testPlanDurationPreservesReadingAndNewCycleArchivesWithoutDroppingHistory() {
    long plan = store.savePlan(30, day, false);
    store.save(entry(2, 1, 7, plan));
    assertEquals(plan, store.savePlan(90, day, false));
    assertEquals(7, store.coverage(plan).cardinality());
    assertEquals(plan, store.savePlan(365, day, false));
    assertEquals(365, store.plan().days);
    long next = store.savePlan(30, day, true);
    assertTrue(next != plan);
    assertEquals(0, store.coverage(next).cardinality());
    assertEquals(7, store.coverage(plan).cardinality());
    assertEquals(1, store.entries(day).size());
    rejects(() -> store.savePlan(90, day, false, plan));
    rejects(() -> store.save(entry(2, 1, 1, plan)));
    assertEquals(next, store.plan().id);
  }

  public void testNotesAndElapsedDaysDoNotInventStreakOrProgressAndUndoRecalculates() {
    store.note(day, "Refleksi uji");
    assertEquals(0, FaithSummary.load(store, tasks).streak);
    store.savePlan(30, day.minusDays(90), false);
    assertEquals(0, FaithSummary.load(store, tasks).read);
    tasks.addObligatoryPrayerChecklist();
    assertEquals(0, FaithSummary.load(store, tasks).streak);
    long task = tasks.getTasks().get(0).id;
    tasks.setCompleted(task, true);
    assertEquals(1, FaithSummary.load(store, tasks).streak);
    store.save(entry(1, 1, 3, 0));
    tasks.setCompleted(task, false);
    assertEquals(1, FaithSummary.load(store, tasks).streak);
    store.delete(store.entries(day).get(0));
    assertEquals(0, FaithSummary.load(store, tasks).streak);
    assertEquals("Refleksi uji", store.note(day));
    store.note(day, "");
    assertEquals("", store.note(day));
  }

  public void testHifzSnapshotRemainsAfterTargetEditDeleteAndNeverCompletesTarget() {
    long id = tasks.addHifzTask("Uji", new HifzTarget(1, "Al-Fatihah", 7, 1, 3));
    FaithStore.Entry e =
        new FaithStore.Entry(0, 0, UUID.randomUUID().toString(), 1, day, 1, 1, 3, "Latihan", id, 0);
    store.save(e);
    assertFalse(tasks.getTasks().get(0).completed);
    tasks.setCompleted(id, true);
    tasks.updateHifzTask(id, "Rentang baru", new HifzTarget(1, "Al-Fatihah", 7, 4, 7));
    assertFalse(tasks.getTasks().get(0).completed);
    tasks.deleteTask(id);
    assertEquals(1, store.entries(day).size());
    assertEquals(3, store.entries(day).get(0).last);
    assertEquals(5, tasks.getReadableDatabase().getVersion());
  }

  public void testFutureInvalidOversizeAndPerDayCapacityDoNotDropEarlierRecords() {
    rejects(() -> store.note(day.plusDays(1), "Masa depan"));
    rejects(() -> store.note(day, new String(new char[1001]).replace((char) 0, 'x')));
    rejects(
        () ->
            new FaithStore.Entry(0, 0, UUID.randomUUID().toString(), 1, day, 114, 1, 7, "", 0, 0));
    rejects(() -> store.savePlan(31, day, false));
    rejects(() -> store.save(entry(2, 1, 1, 0)));
    FaithStore.Entry original = entry(1, 1, 1, 0);
    long id = store.save(original);
    for (int i = 1; i < 100; i++) store.save(entry(1, 1, 1, 0));
    assertEquals(id, store.save(original));
    rejects(() -> store.save(entry(1, 1, 1, 0)));
    assertEquals(100, store.entries(day).size());
  }

  public void testSelectedTimezoneMidnightKeepsHistoricalDatesAndStreak() {
    store.save(entry(1, 1, 1, 0));
    store.close();
    Clock tomorrow = Clock.fixed(Instant.parse("2026-10-27T17:01:00Z"), ZoneId.of("Asia/Jakarta"));
    store = new FaithStore(getContext(), NAME, tomorrow);
    assertEquals(day.plusDays(1), store.today());
    assertEquals(1, FaithSummary.load(store, tasks).streak);
    assertTrue(store.entries(day.plusDays(1)).isEmpty());
    assertEquals(1, store.entries(day).size());
  }

  public void testMovingToEarlierLocalDateCannotRemoveRecordedReadingCoverage() {
    long plan = store.savePlan(30, day, false);
    store.save(entry(2, 1, 7, plan));
    store.close();
    store =
        new FaithStore(
            getContext(), NAME, Clock.fixed(CLOCK.instant(), ZoneId.of("Pacific/Honolulu")));
    assertEquals(day.minusDays(1), store.today());
    assertEquals(7, store.coverage(plan).cardinality());
    assertEquals(day, store.entries(day).get(0).day);
    rejects(() -> store.save(entry(1, 1, 1, 0)));
  }
}
