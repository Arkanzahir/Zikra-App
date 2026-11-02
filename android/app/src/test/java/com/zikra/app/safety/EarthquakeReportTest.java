package com.zikra.app.safety;

import static org.junit.Assert.*;
import java.time.Instant;
import org.json.*;
import org.junit.Test;

public class EarthquakeReportTest {
  private static final Instant NOW = Instant.parse("2026-10-30T07:00:00Z");
  private static final String JSON = "{\"Infogempa\":{\"gempa\":{\"DateTime\":\"2026-10-30T06:03:45+00:00\","
      + "\"Magnitude\":\"3.7\",\"Kedalaman\":\"10 km\",\"Wilayah\":\"Sorong\",\"Coordinates\":\"-0.72,131.38\","
      + "\"Shakemap\":\"20261030130345.mmi.jpg\"}}}";

  @Test public void atomicSnapshotKeepsEventImageCoordinatesAndRetrievalTime() throws Exception {
    EarthquakeReport original = EarthquakeReport.network(JSON, NOW);
    EarthquakeReport stored = EarthquakeReport.cached(original.snapshot(), NOW.plusSeconds(120));
    assertTrue(stored.stored);assertEquals(NOW, stored.fetchedAt);
    assertEquals(original.imageKey(), stored.imageKey());
    assertEquals(-.72, stored.quake.latitude, 0);assertEquals(131.38, stored.quake.longitude, 0);
    assertTrue(stored.quake.shakemapUrl.endsWith("20261030130345.mmi.jpg"));
    assertEquals(Instant.parse("2026-10-30T06:03:45Z"), stored.quake.time);
    assertFalse(stored.fresh(NOW.plusSeconds(120)));assertTrue(stored.usable(NOW.plusSeconds(120)));
  }

  @Test public void expiredFutureCorruptOrUnknownSnapshotsAreRejected() throws Exception {
    EarthquakeReport report = EarthquakeReport.network(JSON, NOW);
    String snapshot = report.snapshot();
    for (String bad : new String[] {"{", "{}", new JSONObject(snapshot).put("version", 2).toString(),
        new JSONObject(snapshot).put("fetchedAt", NOW.plusSeconds(1).toEpochMilli()).toString(),
        new JSONObject(snapshot).put("report", "{}").toString()}) {
      try { EarthquakeReport.cached(bad, NOW);fail("Invalid cache accepted"); }
      catch (JSONException expected) { }
    }
    try { EarthquakeReport.cached(snapshot, NOW.plusMillis(EarthquakeReport.MAX_AGE_MS + 1));fail(); }
    catch (JSONException expected) { }
    assertFalse(report.usable(NOW.minusSeconds(1)));
    assertTrue(report.fresh(NOW.plusSeconds(59)));assertFalse(report.fresh(NOW.plusSeconds(60)));
  }

  @Test public void missingOffsetFutureEventAndInvalidDepthAreNotPresentedAsValid() throws Exception {
    for (String bad : new String[] {JSON.replace("+00:00", ""),JSON.replace("06:03:45", "08:03:45"),
        JSON.replace("10 km", "unknown"),JSON.replace("10 km", "1001 km"),JSON.replace("10 km", "-1 km")}) {
      try { EarthquakeReport.network(bad, NOW);fail("Invalid report accepted"); }
      catch (JSONException expected) { }
    }
  }

  @Test public void lateOlderResponseCannotReplaceNewerEventButRevisionCan() throws Exception {
    EarthquakeReport current = EarthquakeReport.network(JSON, NOW);
    EarthquakeReport old = EarthquakeReport.network(JSON.replace("06:03:45", "05:03:45"), NOW.plusSeconds(20));
    assertSame(current, EarthquakeReport.newest(current, old));
    EarthquakeReport revision = EarthquakeReport.network(JSON.replace("3.7", "3.8"), NOW.plusSeconds(30));
    assertSame(revision, EarthquakeReport.newest(current, revision));
    assertNotEquals(current.imageKey(), revision.imageKey());
  }

  @Test public void missingCoordinateOrUnsafeImageNeverInventsPinOrRemoteOrigin() throws Exception {
    EarthquakeReport value = EarthquakeReport.network(JSON.replace("-0.72,131.38", "131.38,-0.72")
        .replace("20261030130345.mmi.jpg", "https://evil.test/a.jpg"), NOW);
    assertFalse(value.quake.hasCoordinates());assertEquals("", value.quake.shakemapUrl);
  }

  @Test public void redirectOnlyAllowsOfficialCdnWithExactlyTheReportedFilename() {
    String original = "https://static.bmkg.go.id/20261030130345.mmi.jpg";
    String target = "https://cdn.bmkg.go.id/20261030130345.mmi.jpg";
    assertEquals(target, BmkgParser.shakemapRedirect(original, target));
    for (String bad : new String[] {target.replace("cdn.bmkg.go.id", "evil.test"),
        target + "?other=1", target.replace("130345", "130346"), "http://cdn.bmkg.go.id/20261030130345.mmi.jpg"})
      assertEquals("", BmkgParser.shakemapRedirect(original, bad));
  }
}
