package com.zikra.app.migration;

import org.junit.Test;
import static org.junit.Assert.*;

public final class BackupPolicyTest {
  @Test public void tokensConsentCachesAndActiveRemindersAreNotTransferablePreferences() {
    for (String key : new String[]{"assistant_endpoint", "assistant_token", "assistant_basic_consent_cloudflare_endpoint_v2",
        "assistant_history_v1", "assistant_mode", "reminder_enabled", "new_future_secret", "prayer_revision"})
      assertFalse(BackupPolicy.preferenceAllowed("zikra_preferences", key));
    for (String group : new String[]{"assistant_gateway", "quran_content_sync", "zikra_weather_openmeteo_v2", "firebase", "prayer_alarm_plan"})
      assertFalse(BackupPolicy.preferenceAllowed(group, "token"));
    assertTrue(BackupPolicy.preferenceAllowed("zikra_preferences", "last_verse_key"));
    assertTrue(BackupPolicy.preferenceAllowed("zikra_home_appearance", "theme"));
  }
  @Test public void onlyPersonalTablesAreListedAndInventoryCannotBeModified() {
    assertEquals(5, BackupPolicy.DATABASES.size());
    assertEquals(java.util.Collections.singletonList("bookmarks"), BackupPolicy.DATABASES.get("zikra_hadith.db"));
    assertFalse(BackupPolicy.DATABASES.containsKey("unknown.db"));
    try { BackupPolicy.DATABASES.put("unknown.db", java.util.Collections.emptyList()); fail(); }
    catch (UnsupportedOperationException expected) { }
  }
}
