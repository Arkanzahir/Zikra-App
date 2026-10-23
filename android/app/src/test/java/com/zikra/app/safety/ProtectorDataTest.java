package com.zikra.app.safety;

import static org.junit.Assert.*;

import java.time.Instant;
import org.junit.Test;

public class ProtectorDataTest {
  @Test
  public void phaseUsesDateAndWrapsBeforeEpoch() {
    Instant epoch = Instant.parse("2000-01-06T18:14:00Z");
    assertEquals(0, MoonPhase.illumination(epoch), 1e-8);
    assertEquals(1, MoonPhase.illumination(epoch.plusSeconds(1275721)), .001);
    assertEquals(.5, MoonPhase.illumination(epoch.plusSeconds(637861)), .001);
    assertTrue(MoonPhase.cycle(epoch.minusSeconds(1000)) > .99);
    assertNotEquals(MoonPhase.cycle(epoch), MoonPhase.cycle(epoch.plusSeconds(86400)), .001);
  }

  @Test
  public void shakemapAcceptsOfficialFilenameOnly() {
    assertEquals(
        "https://static.bmkg.go.id/20261022175749.mmi.jpg",
        BmkgParser.shakemapUrl("20261022175749.mmi.jpg"));
    for (String bad :
        new String[] {
          "../x.jpg", "https://evil.test/a.jpg", "a.jpg?x=1", "a.svg", "a/b.jpg", "", null
        }) assertEquals("", BmkgParser.shakemapUrl(bad));
  }

  @Test
  public void invalidMagnitudeRejected() throws Exception {
    for (String bad : new String[] {"NaN", "Infinity", "-1", "11", "abc"}) {
      try {
        BmkgParser.earthquake(
            "{\"Infogempa\":{\"gempa\":{\"Wilayah\":\"Indonesia\",\"Magnitude\":\""
                + bad
                + "\"}}}");
        fail(bad);
      } catch (org.json.JSONException expected) {
      }
    }
  }

  @Test
  public void reportKeepsCoordinateOrderAndShakemap() throws Exception {
    BmkgParser.Earthquake q =
        BmkgParser.earthquake(
            "{\"Infogempa\":{\"gempa\":{\"Wilayah\":\"Laut"
                + " Jawa\",\"Magnitude\":\"5.2\",\"Coordinates\":\"-7.1,112.7\",\"Shakemap\":\"20261022175749.mmi.jpg\"}}}");
    assertEquals(-7.1, q.latitude, 0);
    assertEquals(112.7, q.longitude, 0);
    assertTrue(q.shakemapUrl.endsWith(".mmi.jpg"));
    q =
        BmkgParser.earthquake(
            "{\"Infogempa\":{\"gempa\":{\"Wilayah\":\"Laut"
                + " Jawa\",\"Magnitude\":\"5.2\",\"Coordinates\":\"112.7,-7.1\"}}}");
    assertFalse(q.hasCoordinates());
    assertEquals("", q.shakemapUrl);
  }
}
