package com.zikra.app.safety;

import static org.junit.Assert.*;

import com.zikra.app.location.LocationPoint;
import java.time.Instant;
import org.json.*;
import org.junit.Test;

public class OpenMeteoParserTest {
  private final Instant start = Instant.parse("2026-11-11T00:00:00Z");

  private JSONObject fixture(LocationPoint point) throws Exception {
    JSONObject units = new JSONObject();
    units
        .put("time", "unixtime")
        .put("temperature_2m", "°C")
        .put("relative_humidity_2m", "%")
        .put("weather_code", "wmo code")
        .put("wind_speed_10m", "km/h")
        .put("wind_direction_10m", "°");
    JSONObject hours = new JSONObject();
    String[] names = {
      "time",
      "temperature_2m",
      "relative_humidity_2m",
      "weather_code",
      "wind_speed_10m",
      "wind_direction_10m"
    };
    for (String name : names) hours.put(name, new JSONArray());
    for (int i = 0; i < 72; i++) {
      hours.getJSONArray("time").put(start.plusSeconds(i * 3600L).getEpochSecond());
      hours.getJSONArray("temperature_2m").put(28);
      hours.getJSONArray("relative_humidity_2m").put(75);
      hours.getJSONArray("weather_code").put(i == 5 ? 95 : 3);
      hours.getJSONArray("wind_speed_10m").put(12);
      hours.getJSONArray("wind_direction_10m").put(90);
    }
    return new JSONObject()
        .put("latitude", point.latitude)
        .put("longitude", point.longitude)
        .put("timezone", point.zone.getId())
        .put("utc_offset_seconds", 0)
        .put("hourly_units", units)
        .put("hourly", hours);
  }

  private BmkgParser.Forecast parse(JSONObject json, Instant now, LocationPoint point)
      throws Exception {
    return OpenMeteoParser.parse(json.toString(), now, now, true, false, point);
  }

  private void rejected(JSONObject json, Instant now, LocationPoint point) throws Exception {
    try {
      parse(json, now, point);
      fail("Invalid forecast accepted");
    } catch (JSONException expected) {
    }
  }

  @Test
  public void currentForecastStaysWithinUpcomingHourForSelectedZone() throws Exception {
    LocationPoint[] points = {
      LocationPoint.INITIAL,
      new LocationPoint(-8.65, 115.22, "Asia/Makassar", "Fixture", "manual", 0, -1),
      new LocationPoint(40.71, -74.0, "America/New_York", "Fixture", "manual", 0, -1)
    };
    for (LocationPoint point : points) {
      BmkgParser.Forecast f = parse(fixture(point), start.plusSeconds(4 * 3600 + 1800), point);
      assertEquals(start.plusSeconds(5 * 3600), f.current.time);
      assertEquals("Hujan petir", f.current.description);
      assertEquals(point.key(), f.location.key());
      assertEquals(72, f.slots.size());
    }
  }

  @Test
  public void newlyFetchedExpiredSeriesCannotBecomeCurrentForecast() throws Exception {
    JSONObject json = fixture(LocationPoint.INITIAL);
    rejected(json, start.plusSeconds(72 * 3600), LocationPoint.INITIAL);
    rejected(json, start.plusSeconds(5 * 86400), LocationPoint.INITIAL);
    assertEquals(
        start.plusSeconds(71 * 3600),
        parse(json, start.plusSeconds(72 * 3600 - 1), LocationPoint.INITIAL).current.time);
  }

  @Test
  public void futureSeriesCannotClaimToCoverNow() throws Exception {
    rejected(fixture(LocationPoint.INITIAL), start.minusSeconds(3601), LocationPoint.INITIAL);
    assertEquals(
        start,
        parse(fixture(LocationPoint.INITIAL), start.minusSeconds(3600), LocationPoint.INITIAL)
            .current
            .time);
  }

  @Test
  public void gapDuplicateAndFractionalEpochAreRejected() throws Exception {
    for (double offset : new double[] {0, 7200, 3600.5}) {
      JSONObject json = fixture(LocationPoint.INITIAL);
      json.getJSONObject("hourly").getJSONArray("time").put(1, start.getEpochSecond() + offset);
      rejected(json, start, LocationPoint.INITIAL);
    }
  }

  @Test
  public void mismatchedLocationZoneAndUnitsRemainRejected() throws Exception {
    LocationPoint point = LocationPoint.INITIAL;
    rejected(fixture(point).put("longitude", 0), start, point);
    rejected(fixture(point).put("timezone", "UTC"), start, point);
    JSONObject json = fixture(point);
    json.getJSONObject("hourly_units").put("wind_speed_10m", "m/s");
    rejected(json, start, point);
  }
}
