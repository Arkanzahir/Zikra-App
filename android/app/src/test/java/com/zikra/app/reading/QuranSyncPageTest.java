package com.zikra.app.reading;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class QuranSyncPageTest {
  private static final long NOW = 1800000000000L;
  private QuranChapters.Chapter chapter() throws Exception {
    return new QuranChapters.Chapter(new JSONObject().put("id", 1).put("verses_count", 7)
        .put("name_simple", "Fixture").put("name_arabic", "Fixture").put("revelation_place", "makkah")
        .put("translated_name", new JSONObject().put("name", "Fixture")));
  }
  private JSONObject body() throws Exception {
    JSONArray verses = new JSONArray();
    for (int i = 1; i <= 7; i++) verses.put(new JSONObject().put("verse_number", i).put("verse_key", "1:" + i)
        .put("text_uthmani", "Synthetic Arabic fixture " + i)
        .put("translations", new JSONArray().put(new JSONObject().put("resource_id", 33).put("text", "Synthetic meaning " + i))));
    return new JSONObject().put("verses", verses).put("pagination", new JSONObject().put("per_page", 50)
        .put("current_page", 1).put("total_pages", 1).put("total_records", 7).put("next_page", JSONObject.NULL))
        .put("zikra_sync", new JSONObject().put("schema", 1).put("translation", 33).put("checked_at", NOW - 1000)
            .put("source", "Quran Foundation Content Sync").put("retained_during_outage", false));
  }
  @Test public void sourceTimeControlsFreshnessNotCacheAccess() throws Exception {
    long checked = QuranSyncPage.validate(body().toString(), chapter(), 1, NOW);
    assertTrue(QuranSyncPage.fresh(checked, NOW));
    assertFalse(QuranSyncPage.fresh(checked, NOW + QuranSyncPage.REFRESH_MS));
  }
  @Test public void legacyPagesCannotMasqueradeAsSyncedData() throws Exception {
    JSONObject json = body(); json.remove("zikra_sync");
    try { QuranSyncPage.validate(json.toString(), chapter(), 1, NOW); fail(); }
    catch (JSONException expected) { }
  }
  @Test public void wrongSourceResourceFutureTimeAndCoercedTimeAreRejected() throws Exception {
    for (int index = 0; index < 4; index++) {
      JSONObject json = body(), metadata = json.getJSONObject("zikra_sync");
      if (index == 0) metadata.put("source", "unverified");
      if (index == 1) metadata.put("translation", 99);
      if (index == 2) metadata.put("checked_at", NOW + 60001);
      if (index == 3) metadata.put("checked_at", String.valueOf(NOW));
      try { QuranSyncPage.validate(json.toString(), chapter(), 1, NOW); fail(); }
      catch (IllegalArgumentException expected) { }
    }
  }
  @Test public void partialAndReorderedVersesFailBeforeStorage() throws Exception {
    JSONObject json = body(); json.getJSONArray("verses").getJSONObject(1).put("verse_key", "1:1");
    try { QuranSyncPage.validate(json.toString(), chapter(), 1, NOW); fail(); }
    catch (JSONException expected) { }
  }
}
