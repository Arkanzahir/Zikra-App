package com.zikra.app.study;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Stage 0: due; 1: previous local day; 2: due day, up to one hour before due. */
public final class DeadlinePlan {
  private DeadlinePlan() {}
  public static long at(long due, int stage, ZoneId zone) {
    if (due <= 0 || stage < 0 || stage > 2) throw new IllegalArgumentException("Invalid deadline");
    if (stage == 0) return due;
    ZonedDateTime date = Instant.ofEpochMilli(due).atZone(zone);
    if (stage == 1) return date.minusDays(1).toInstant().toEpochMilli();
    return Math.max(date.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli(), due - 3600000);
  }
  public static boolean deliverable(long due, int stage, ZoneId zone, long now) {
    long trigger = at(due, stage, zone);
    if (now < trigger) return false;
    if (stage == 0) return now - due < 86400000L;
    if (stage == 2) return trigger < due && now < due;
    return Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        .isBefore(Instant.ofEpochMilli(due).atZone(zone).toLocalDate());
  }
}
