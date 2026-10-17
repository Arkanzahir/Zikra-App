package com.zikra.app.safety;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Pure Java date handling shared by the BMKG parser and JVM tests. */
public final class ForecastSelector {
  private ForecastSelector() {}

  public static Instant parseTimestamp(String value, boolean local) {
    if (value == null || value.trim().isEmpty()) return null;
    String normalized = value.trim().replace(' ', 'T');
    try {
      return Instant.parse(normalized);
    } catch (DateTimeParseException ignored) {
      try {
        // Older Android java.time implementations accept only Z in Instant.parse().
        return OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
      } catch (DateTimeParseException noOffset) {
        try {
          return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
              .atZone(local ? ZoneId.of("Asia/Jakarta") : ZoneOffset.UTC)
              .toInstant();
        } catch (DateTimeParseException invalid) {
          return null;
        }
      }
    }
  }

  /** Select the nearest upcoming prediction, otherwise the latest expired one. */
  public static int select(List<Instant> times, Instant now) {
    int upcoming = -1;
    int latest = -1;
    for (int index = 0; index < times.size(); index++) {
      Instant time = times.get(index);
      if (time == null) continue;
      if (latest == -1 || time.isAfter(times.get(latest))) latest = index;
      if (!time.isBefore(now) && (upcoming == -1 || time.isBefore(times.get(upcoming)))) {
        upcoming = index;
      }
    }
    return upcoming >= 0 ? upcoming : latest;
  }
}
