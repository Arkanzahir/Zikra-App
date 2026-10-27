package com.zikra.app.faith;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.*;
import org.junit.Test;

public final class FaithProgressTest {
  @Test
  public void everySupportedPlanDistributesExactly6236AyahsWithoutInventingReading() {
    LocalDate start = LocalDate.of(2026, 10, 27);
    for (int days : new int[] {30, 90, 365}) {
      assertEquals(0, FaithProgress.scheduled(start, days, start.minusDays(1)));
      int previous = 0, total = 0;
      for (int day = 0; day < days; day++) {
        int target = FaithProgress.scheduled(start, days, start.plusDays(day));
        assertTrue(target > previous);
        total += target - previous;
        previous = target;
      }
      assertEquals(6236, total);
      assertEquals(6236, FaithProgress.scheduled(start, days, start.plusYears(2)));
      assertEquals(0, FaithProgress.coverage(Collections.emptyList()).cardinality());
    }
  }

  @Test
  public void overlappingRepeatedAndOutOfOrderRangesCountOnlyUniqueAyahs() {
    BitSet bits =
        FaithProgress.coverage(
            Arrays.asList(
                new int[] {5, 10}, new int[] {1, 7}, new int[] {5, 10}, new int[] {6236, 6236}));
    assertEquals(11, bits.cardinality());
    assertEquals(10, bits.nextClearBit(0));
    assertEquals(
        6236, FaithProgress.coverage(Collections.singletonList(new int[] {1, 6236})).cardinality());
    for (int[] invalid : new int[][] {{0, 1}, {2, 1}, {1, 6237}})
      try {
        FaithProgress.coverage(Collections.singletonList(invalid));
        fail();
      } catch (IllegalArgumentException expected) {
      }
  }

  @Test
  public void globalNumberingRoundTripsAllAyahsIncludingBothBoundaries() {
    for (int global = 1; global <= 6236; global++) {
      int[] ref = FaithProgress.reference(global);
      assertEquals(global, com.zikra.app.audio.Recitation.globalVerse(ref[0], ref[1]));
    }
    assertArrayEquals(new int[] {114, 6}, FaithProgress.reference(6236));
  }

  @Test
  public void streakCrossesMonthAndAllowsTodayStillPendingButBreaksOnMissingDay() {
    LocalDate today = LocalDate.of(2026, 11, 2);
    Set<LocalDate> days =
        new HashSet<>(
            Arrays.asList(
                today.minusDays(1), today.minusDays(2), today.minusDays(3), today.plusDays(1)));
    assertEquals(3, FaithProgress.streak(days, today));
    assertEquals(3, FaithProgress.week(days, today));
    days.add(today);
    assertEquals(4, FaithProgress.streak(days, today));
    days.remove(today.minusDays(1));
    assertEquals(1, FaithProgress.streak(days, today));
    days.remove(today);
    assertEquals(0, FaithProgress.streak(days, today));
    try {
      FaithProgress.duration(31);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
