package com.zikra.app.location;

import static org.junit.Assert.*;

import com.zikra.app.PrayerSchedule;
import java.time.*;
import org.junit.Test;

public class LocationPointTest {
  private LocationPoint city(double lat, double lon, String zone) {
    return new LocationPoint(lat, lon, zone, "Kota uji", "manual", 0, -1);
  }

  @Test
  public void qiblaChangesWithLocationAndDistanceIsSymmetric() {
    LocationPoint london = city(51.5074, -0.1278, "Europe/London");
    LocationPoint ny = city(40.7128, -74.0060, "America/New_York");
    assertEquals(119.0, london.qiblaBearing(), .2);
    assertEquals(58.5, ny.qiblaBearing(), .2);
    assertEquals(
        london.distanceTo(ny.latitude, ny.longitude),
        ny.distanceTo(london.latitude, london.longitude),
        .01);
    assertEquals(0, london.distanceTo(london.latitude, london.longitude), .01);
    assertTrue(london.distanceTo(ny.latitude, ny.longitude) > 5_500_000);
  }

  @Test
  public void zonesDistinguishIndonesianRegionsAndDst() {
    Instant now = Instant.parse("2026-10-19T00:00:00Z");
    assertEquals("WIB", city(-6, 106, "Asia/Jakarta").zoneLabel(now));
    assertEquals("WITA", city(-5, 119, "Asia/Makassar").zoneLabel(now));
    assertEquals("WIT", city(-2, 140, "Asia/Jayapura").zoneLabel(now));
    LocationPoint london = city(51, -.1, "Europe/London");
    assertEquals("UTC+01:00", london.zoneLabel(Instant.parse("2026-07-01T00:00:00Z")));
    assertEquals("UTC+00:00", london.zoneLabel(Instant.parse("2026-01-01T00:00:00Z")));
  }

  @Test
  public void timetableCacheDoesNotMixCitiesOrZones() {
    LocalDate date = LocalDate.of(2026, 10, 19);
    LocationPoint jakarta = city(-6.1754, 106.8272, "Asia/Jakarta");
    LocationPoint jayapura = city(-2.5337, 140.7181, "Asia/Jayapura");
    PrayerSchedule.Day a = PrayerSchedule.forDate(date, jakarta),
        b = PrayerSchedule.forDate(date, jayapura);
    assertSame(a, PrayerSchedule.forDate(date, jakarta));
    assertNotSame(a, b);
    assertNotEquals(a.time(0), b.time(0));
    ZonedDateTime instant = Instant.parse("2026-10-19T00:00:00Z").atZone(ZoneOffset.UTC);
    assertEquals(jayapura.zone, PrayerSchedule.nextAfter(instant, jayapura).time.getZone());
    assertEquals(
        PrayerSchedule.nextAfter(instant, jayapura).time,
        PrayerSchedule.nextAfter(
                instant.withZoneSameInstant(ZoneId.of("America/New_York")), jayapura)
            .time);
  }

  @Test
  public void polarMissingEventsAreNotFabricated() {
    LocationPoint tromso = city(69.6492, 18.9553, "Europe/Oslo");
    PrayerSchedule.Day day = PrayerSchedule.forDate(LocalDate.of(2026, 6, 21), tromso);
    assertNull(day.at(1));
    assertNull(day.at(4));
    assertNull(day.at(5));
    assertEquals("—", day.time(1));
    assertNotNull(day.at(2));
    assertNull(
        PrayerSchedule.nextOccurrence(
            5, LocalDate.of(2026, 6, 21).atStartOfDay(tromso.zone), tromso));
  }

  @Test
  public void invalidCoordinatesCannotBeSelected() {
    for (double lat : new double[] {Double.NaN, Double.POSITIVE_INFINITY, 91}) {
      try {
        city(lat, 100, "Asia/Jakarta");
        fail("Invalid coordinate");
      } catch (IllegalArgumentException expected) {
      }
    }
    try {
      city(0, 181, "UTC");
      fail("Invalid longitude");
    } catch (IllegalArgumentException expected) {
    }
  }
}
