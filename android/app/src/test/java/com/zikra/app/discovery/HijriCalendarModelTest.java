package com.zikra.app.discovery;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import org.junit.Test;

public class HijriCalendarModelTest {
  @Test
  public void calendarCoversEveryDayAndMatchesGregorianWeekday() {
    for (int year = 1445; year <= 1449; year++)
      for (int month = 1; month <= 12; month++) {
        HijrahDate first = HijrahDate.of(year, month, 1);
        int length = first.lengthOfMonth();
        assertTrue(length == 29 || length == 30);
        assertEquals(
            LocalDate.from(first).getDayOfWeek().getValue() - 1, HijriCalendarModel.offset(first));
        assertEquals(first.plus(length, ChronoUnit.DAYS), HijriCalendarModel.move(first, 1));
      }
  }

  @Test
  public void upcomingEventsAreOrderedAndNeverBeforeSelectedDay() {
    LocalDate selected = LocalDate.of(2026, 10, 16);
    LocalDate previous = selected;
    for (HijriCalendarModel.Event event : HijriCalendarModel.upcoming(selected, 9)) {
      assertFalse(event.gregorian.isBefore(previous));
      assertEquals(event.gregorian, LocalDate.from(event.date));
      previous = event.gregorian;
    }
  }

  @Test
  public void monthMovementStopsAtSupportedCalendarBoundaries() {
    int min = (int) java.time.chrono.HijrahChronology.INSTANCE.range(ChronoField.YEAR).getMinimum();
    int max = (int) java.time.chrono.HijrahChronology.INSTANCE.range(ChronoField.YEAR).getMaximum();
    assertFalse(HijriCalendarModel.canMove(HijrahDate.of(min, 1, 1), -1));
    assertFalse(HijriCalendarModel.canMove(HijrahDate.of(max, 12, 1), 1));
  }
}
