package com.zikra.app.safety;

import java.time.*;
import java.util.*;

/** Presentation data derived only from dated provider slots in the selected location. */
public final class WeatherOutlook {
  private WeatherOutlook() {}

  public static List<BmkgParser.Weather> upcoming(BmkgParser.Forecast forecast, int limit) {
    if (limit <= 0) return Collections.emptyList();
    List<BmkgParser.Weather> result = new ArrayList<>();
    for (BmkgParser.Weather slot : forecast.slots) {
      if (slot.time.isAfter(forecast.current.time)) result.add(slot);
      if (result.size() == limit) break;
    }
    return Collections.unmodifiableList(result);
  }

  public static final class Day {
    public final LocalDate date;
    public final double minimum, maximum;
    public final BmkgParser.Weather representative;
    public final int samples;

    Day(
        LocalDate date,
        double minimum,
        double maximum,
        BmkgParser.Weather representative,
        int samples) {
      this.date = date;
      this.minimum = minimum;
      this.maximum = maximum;
      this.representative = representative;
      this.samples = samples;
    }
  }

  public static List<Day> days(BmkgParser.Forecast forecast) {
    LocalDate today = forecast.current.time.atZone(forecast.location.zone).toLocalDate();
    Map<LocalDate, List<BmkgParser.Weather>> groups = new TreeMap<>();
    for (BmkgParser.Weather slot : forecast.slots) {
      LocalDate date = slot.time.atZone(forecast.location.zone).toLocalDate();
      if (date.isBefore(today) || !date.isBefore(today.plusDays(3))) continue;
      try {
        double value = Double.parseDouble(slot.temperature);
        if (Double.isFinite(value) && value >= -90 && value <= 65)
          groups.computeIfAbsent(date, key -> new ArrayList<>()).add(slot);
      } catch (NumberFormatException ignored) {
      }
    }
    List<Day> result = new ArrayList<>();
    for (Map.Entry<LocalDate, List<BmkgParser.Weather>> entry : groups.entrySet()) {
      double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
      BmkgParser.Weather representative = entry.getValue().get(0);
      int nearestNoon = Integer.MAX_VALUE;
      for (BmkgParser.Weather slot : entry.getValue()) {
        double value = Double.parseDouble(slot.temperature);
        min = Math.min(min, value);
        max = Math.max(max, value);
        int distance = Math.abs(slot.time.atZone(forecast.location.zone).getHour() - 12);
        if (distance < nearestNoon) {
          representative = slot;
          nearestNoon = distance;
        }
      }
      result.add(new Day(entry.getKey(), min, max, representative, entry.getValue().size()));
    }
    return Collections.unmodifiableList(result);
  }

  public static boolean night(Instant time, ZoneId zone) {
    int hour = time.atZone(zone).getHour();
    return hour < 6 || hour >= 18;
  }
}
