package com.zikra.app.safety;

import android.test.AndroidTestCase;
import java.time.Instant;
import org.json.JSONException;

/** Representative BMKG fixtures run against Android's actual JSON implementation. */
@SuppressWarnings("deprecation")
public final class BmkgParserTest extends AndroidTestCase {
  public void testEarthquakeUsesOfficialFieldsAndUtcTimestamp() throws Exception {
    String json =
        """
        {"Infogempa":{"gempa":{
          "DateTime":"2026-10-15T06:23:10+00:00","Tanggal":"15 Okt 2026","Jam":"13:23:10 WIB",
          "Magnitude":"5.1","Kedalaman":"10 km","Wilayah":"Pusat gempa di laut",
          "Potensi":"Tidak berpotensi tsunami","Dirasakan":"II Surabaya"
        }}}
        """;
    BmkgParser.Earthquake quake = BmkgParser.earthquake(json);
    assertEquals("5.1", quake.magnitude);
    assertEquals("10 km", quake.depth);
    assertEquals("Pusat gempa di laut", quake.area);
    assertEquals("II Surabaya", quake.felt);
    assertEquals(Instant.parse("2026-10-15T06:23:10Z"), quake.time);
  }

  public void testEarthquakeRejectsMissingMagnitudeRatherThanInventingData() throws Exception {
    try {
      BmkgParser.earthquake("{\"Infogempa\":{\"gempa\":{\"Wilayah\":\"Surabaya\"}}}");
      fail("Incomplete earthquake was accepted");
    } catch (JSONException expected) {
      // The screen must present an unavailable state for incomplete reports.
    }
  }

  public void testWeatherSelectsKeputihAndNearestUpcomingTime() throws Exception {
    String json =
        """
{"data":[
  {"lokasi":{"adm4":"35.78.09.1002","desa":"Other village"},
   "cuaca":[[{"utc_datetime":"2026-10-15 04:10:00","t":99}]]},
  {"lokasi":{"adm4":"35.78.09.1001","desa":"Keputih","kecamatan":"Sukolilo","kotkab":"Kota Surabaya"},
   "cuaca":[
     [{"utc_datetime":"2026-10-15 09:00:00","t":32,"weather_desc":"Cerah"},
      {"utc_datetime":"2026-10-15 03:00:00","t":28}],
     [{"utc_datetime":"2026-10-15 06:00:00","t":30,"weather_desc":"Cerah Berawan","hu":75,"ws":8.2}]
   ]}
]}
""";
    BmkgParser.Weather weather = BmkgParser.weather(json, Instant.parse("2026-10-15T04:00:00Z"));
    assertEquals("Keputih", weather.village);
    assertEquals("Sukolilo", weather.district);
    assertEquals("Kota Surabaya", weather.city);
    assertEquals("30", weather.temperature);
    assertEquals("75", weather.humidity);
    assertEquals("8.2", weather.wind);
    assertEquals("Cerah Berawan", weather.description);
    assertEquals(Instant.parse("2026-10-15T06:00:00Z"), weather.time);
    assertFalse(weather.expired);
  }

  public void testLocalWeatherTimestampUsesWibAndOptionalValuesStayEmpty() throws Exception {
    String json =
        """
        {"lokasi":{"adm4":"35.78.09.1001","desa":"Keputih"},
         "data":[{"cuaca":[[{"local_datetime":"2026-10-15 13:00:00","t":null}]]}]}
        """;
    BmkgParser.Weather weather = BmkgParser.weather(json, Instant.parse("2026-10-15T05:00:00Z"));
    assertEquals(Instant.parse("2026-10-15T06:00:00Z"), weather.time);
    assertEquals("", weather.temperature);
    assertEquals("", weather.description);
    assertEquals("", weather.humidity);
    assertEquals("", weather.wind);
    assertFalse(weather.expired);
  }

  public void testWeatherMarksLatestPastForecastAsExpired() throws Exception {
    String json =
        """
        {"data":[{"lokasi":{"adm4":"35.78.09.1001","desa":"Keputih"},
         "cuaca":[[{"datetime":"2026-10-15T03:00:00Z","t":28},
                    {"datetime":"2026-10-15T06:00:00Z","t":30}]]}]}
        """;
    BmkgParser.Weather weather = BmkgParser.weather(json, Instant.parse("2026-10-15T07:00:00Z"));
    assertEquals("30", weather.temperature);
    assertTrue(weather.expired);
  }

  public void testWeatherRejectsInvalidTimesAndWrongLocation() throws Exception {
    String[] fixtures = {
      "{\"data\":[{\"lokasi\":{\"adm4\":\"35.78.09.1001\"},\"cuaca\":[[{\"datetime\":\"invalid\",\"t\":30}]]}]}",
      "{\"data\":[{\"lokasi\":{\"adm4\":\"35.78.09.1002\"},\"cuaca\":[[{\"datetime\":\"2026-10-15T06:00:00Z\",\"t\":30}]]}]}"
    };
    for (String fixture : fixtures) {
      try {
        BmkgParser.weather(fixture, Instant.parse("2026-10-15T04:00:00Z"));
        fail("Invalid forecast was accepted");
      } catch (JSONException expected) {
        // Invalid timestamps and another village must never become Sukolilo data.
      }
    }
  }

  public void testEarthquakeCoordinatesKeepLatitudeLongitudeOrderAndRejectInvalidValues()
      throws Exception {
    String prefix =
        "{\"Infogempa\":{\"gempa\":{\"Wilayah\":\"Laut"
            + " Jawa\",\"Magnitude\":\"4.2\",\"Coordinates\":\"";
    BmkgParser.Earthquake valid = BmkgParser.earthquake(prefix + "-7.25,112.75\"}}}");
    assertTrue(valid.hasCoordinates());
    assertEquals(-7.25, valid.latitude, 0.00001);
    assertEquals(112.75, valid.longitude, 0.00001);
    for (String coordinates : new String[] {"112.75,-7.25", "NaN,112.75", "-7.25,181", "invalid"}) {
      assertFalse(BmkgParser.earthquake(prefix + coordinates + "\"}}}").hasCoordinates());
    }
  }

  public void testForecastReturnsAllSortedUniqueOfficialSlotsForChart() throws Exception {
    String json =
        """
        {"data":[{"lokasi":{"adm4":"35.78.09.1001","desa":"Keputih"},
         "cuaca":[[{"utc_datetime":"2026-10-15 09:00:00","t":32,"wd":"SE"},
                    {"utc_datetime":"2026-10-15 03:00:00","t":28}],
                   [{"utc_datetime":"2026-10-15 06:00:00","t":30,"wd":"W","vs_text":"> 10 km"},
                    {"utc_datetime":"2026-10-15 09:00:00","t":32},
                    {"utc_datetime":"invalid","t":99}]]}]}
        """;
    BmkgParser.Forecast forecast = BmkgParser.forecast(json, Instant.parse("2026-10-15T04:00:00Z"));
    assertEquals(3, forecast.slots.size());
    assertEquals("28", forecast.slots.get(0).temperature);
    assertEquals("30", forecast.current.temperature);
    assertEquals("32", forecast.slots.get(2).temperature);
    assertEquals("W", forecast.current.windDirection);
    assertEquals("> 10 km", forecast.current.visibility);
    try {
      forecast.slots.clear();
      fail("Forecast slots must remain immutable");
    } catch (UnsupportedOperationException expected) {
    }
  }
}
