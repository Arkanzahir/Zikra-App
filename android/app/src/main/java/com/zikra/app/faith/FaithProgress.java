package com.zikra.app.faith;

import com.zikra.app.audio.Recitation;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Calculations use explicit records, never playback time, bookmarks or elapsed plan days. */
public final class FaithProgress {
  private FaithProgress() {}

  public static final int TOTAL = 6236;

  public static int duration(int days) {
    if (days != 30 && days != 90 && days != 365)
      throw new IllegalArgumentException("Pilih 30, 90 atau 365 hari");
    return days;
  }

  public static int scheduled(LocalDate start, int days, LocalDate today) {
    duration(days);
    long elapsed = Math.max(0, Math.min(days, ChronoUnit.DAYS.between(start, today) + 1));
    return (int) ((TOTAL * elapsed + days - 1) / days);
  }

  public static BitSet coverage(List<int[]> ranges) {
    BitSet result = new BitSet(TOTAL);
    for (int[] range : ranges) {
      if (range.length != 2 || range[0] < 1 || range[1] < range[0] || range[1] > TOTAL)
        throw new IllegalArgumentException("Rentang bacaan tidak valid");
      result.set(range[0] - 1, range[1]);
    }
    return result;
  }

  public static int[] reference(int global) {
    if (global < 1 || global > TOTAL) throw new IllegalArgumentException("Ayat tidak valid");
    for (int chapter = 1; chapter <= 114; chapter++) {
      if (global <= Recitation.count(chapter)) return new int[] {chapter, global};
      global -= Recitation.count(chapter);
    }
    throw new IllegalArgumentException("Ayat tidak valid");
  }

  public static int streak(Set<LocalDate> days, LocalDate today) {
    LocalDate day = days.contains(today) ? today : today.minusDays(1);
    int count = 0;
    while (days.contains(day)) {
      count++;
      day = day.minusDays(1);
    }
    return count;
  }

  public static int week(Set<LocalDate> days, LocalDate today) {
    int count = 0;
    for (int i = 0; i < 7; i++) if (days.contains(today.minusDays(i))) count++;
    return count;
  }
}
