package com.zikra.app.maps;

import android.test.InstrumentationTestCase;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

@SuppressWarnings("deprecation")
public final class OsmMapPolicyTest extends InstrumentationTestCase {
  public void testServerExpiryIsNotExtendedAndMissingHeadersUseSevenDays() throws Exception {
    android.content.Context context = getInstrumentation().getTargetContext();
    IConfigurationProvider config = Configuration.getInstance();
    Long previousOverride = config.getExpirationOverrideDuration();
    long previousExtension = config.getExpirationExtendedDuration();
    try {
      config.setExpirationExtendedDuration(7L * 86400000);
      config.setExpirationOverrideDuration(900000000L);
      OsmMapPolicy.configure(context);
      assertNull(config.getExpirationOverrideDuration()); assertEquals(0L, config.getExpirationExtendedDuration());
      assertTrue(config.getUserAgentValue().contains("Zikra-Android/1.9.48"));
      assertEquals("osm-http-v2", config.getOsmdroidBasePath().getName());
      assertEquals((short)2, config.getTileDownloadThreads());
      long now = java.time.Instant.parse("2026-11-14T00:00:00Z").toEpochMilli();
      org.osmdroid.tileprovider.tilesource.TileSourcePolicy policy = TileSourceFactory.MAPNIK.getTileSourcePolicy();
      assertEquals(now + 3600000L, policy.computeExpirationTime(null, "max-age=3600", now));
      assertEquals(now + 3600000L, policy.computeExpirationTime("Sat, 14 Nov 2026 01:00:00 GMT", null, now));
      assertEquals(now + 7L * 86400000, policy.computeExpirationTime(null, null, now));
      OsmMapPolicy.configure(context);
      assertEquals(now + 3600000L, policy.computeExpirationTime(null, "max-age=3600", now));
    } finally {
      config.setExpirationOverrideDuration(previousOverride);
      config.setExpirationExtendedDuration(previousExtension);
    }
  }
}
