package com.zikra.app.safety;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class EarthquakeAlertPolicyTest {
  private static final Instant NOW = Instant.parse("2026-11-09T06:00:00Z");

  private Map<String, String> message(Instant event) throws Exception {
    JSONObject quake =
        new JSONObject()
            .put("DateTime", event.toString())
            .put("Magnitude", "5.1")
            .put("Kedalaman", "10 km")
            .put("Wilayah", "Contoh wilayah uji")
            .put("Coordinates", "-7.28,112.79");
    String json =
        new JSONObject().put("Infogempa", new JSONObject().put("gempa", quake)).toString();
    EarthquakeReport report = EarthquakeReport.network(json, NOW);
    Map<String, String> data = new HashMap<>();
    data.put("schema", "1");
    data.put("event_id", EarthquakeAlertPolicy.id(report));
    data.put("quake_report", report.snapshot());
    return data;
  }

  private void rejected(Map<String, String> data, Instant now) throws Exception {
    try {
      EarthquakeAlertPolicy.validate(data, now);
      fail("Invalid push accepted");
    } catch (JSONException expected) {
    }
  }

  @Test
  public void notificationRetainsSelectedEventAndStableIdentityAcrossRevisions() throws Exception {
    Map<String, String> data = message(NOW.minusSeconds(120));
    EarthquakeReport report = EarthquakeAlertPolicy.validate(data, NOW);
    assertEquals(NOW.minusSeconds(120), report.quake.time);
    assertEquals("Contoh wilayah uji", report.quake.area);
    assertEquals(data.get("event_id"), EarthquakeAlertPolicy.id(report));
    data.put("quake_report", data.get("quake_report").replace("5.1", "5.2"));
    assertEquals(
        data.get("event_id"), EarthquakeAlertPolicy.id(EarthquakeAlertPolicy.validate(data, NOW)));
  }

  @Test
  public void ageBoundaryPermitsOneHourButRejectsDelayedAndFutureEvents() throws Exception {
    EarthquakeAlertPolicy.validate(message(NOW.minusSeconds(3600)), NOW);
    rejected(message(NOW.minusSeconds(3600).minusMillis(1)), NOW);
    rejected(message(NOW.plusSeconds(1)), NOW);
    rejected(message(NOW), NOW.plusSeconds(3601));
  }

  @Test
  public void schemaIdentitySizeAndUnrequestedFieldsFailClosed() throws Exception {
    rejected(null, NOW);
    for (Map.Entry<String, String> patch :
        Map.of(
                "schema",
                "2",
                "event_id",
                "0".repeat(64),
                "quake_report",
                "x".repeat(2801),
                "unrequested",
                "value")
            .entrySet()) {
      Map<String, String> data = message(NOW);
      data.put(patch.getKey(), patch.getValue());
      rejected(data, NOW);
    }
    Map<String, String> data = message(NOW);
    data.remove("schema");
    rejected(data, NOW);
    data = message(NOW);
    data.put("quake_report", new JSONObject(data.get("quake_report")).put("version", 2).toString());
    rejected(data, NOW);
  }
}
