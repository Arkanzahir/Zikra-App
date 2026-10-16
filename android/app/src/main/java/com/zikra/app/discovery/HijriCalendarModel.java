package com.zikra.app.discovery;

import java.time.LocalDate;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Umm al-Qura calendar calculations; not an official local sighting decision. */
public final class HijriCalendarModel {
  public static final String[] MONTHS = {
    "Muharram",
    "Safar",
    "Rabiul Awal",
    "Rabiul Akhir",
    "Jumadil Awal",
    "Jumadil Akhir",
    "Rajab",
    "Syaban",
    "Ramadan",
    "Syawal",
    "Zulkaidah",
    "Zulhijah"
  };

  private HijriCalendarModel() {}

  public static HijrahDate first(HijrahDate day) {
    return day.with(ChronoField.DAY_OF_MONTH, 1);
  }

  public static int offset(HijrahDate month) {
    return first(month).get(ChronoField.DAY_OF_WEEK) - 1;
  }

  public static boolean canMove(HijrahDate month, int amount) {
    try {
      first(month).plus(amount, ChronoUnit.MONTHS);
      return true;
    } catch (java.time.DateTimeException e) {
      return false;
    }
  }

  public static HijrahDate move(HijrahDate month, int amount) {
    return first(month).plus(amount, ChronoUnit.MONTHS);
  }

  public static String label(HijrahDate date) {
    return date.get(ChronoField.DAY_OF_MONTH)
        + " "
        + MONTHS[date.get(ChronoField.MONTH_OF_YEAR) - 1]
        + " "
        + date.get(ChronoField.YEAR)
        + " H";
  }

  public static final class Event {
    public final String title;
    public final HijrahDate date;
    public final LocalDate gregorian;

    Event(String title, HijrahDate date) {
      this.title = title;
      this.date = date;
      this.gregorian = LocalDate.from(date);
    }
  }

  public static List<Event> upcoming(LocalDate from, int limit) {
    String[] labels = {
      "Tahun Baru Hijriah",
      "Hari Asyura",
      "Maulid Nabi",
      "Isra Mikraj",
      "Awal Ramadan",
      "Nuzulul Quran",
      "Idulfitri",
      "Hari Arafah",
      "Iduladha"
    };
    int[] months = {1, 1, 3, 7, 9, 9, 10, 12, 12}, days = {1, 10, 12, 27, 1, 17, 1, 9, 10};
    int year = HijrahDate.from(from).get(ChronoField.YEAR);
    ValueRange range = HijrahChronology.INSTANCE.range(ChronoField.YEAR);
    List<Event> events = new ArrayList<>();
    for (int candidate = year;
        candidate <= year + 1 && candidate <= range.getMaximum();
        candidate++) {
      for (int i = 0; i < labels.length; i++) {
        Event event = new Event(labels[i], HijrahDate.of(candidate, months[i], days[i]));
        if (!event.gregorian.isBefore(from)) events.add(event);
      }
    }
    events.sort(Comparator.comparing(event -> event.gregorian));
    return new ArrayList<>(events.subList(0, Math.min(Math.max(limit, 0), events.size())));
  }
}
