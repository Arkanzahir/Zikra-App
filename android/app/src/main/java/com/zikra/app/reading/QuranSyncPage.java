package com.zikra.app.reading;

import org.json.JSONObject;

/** Source freshness is server-sync time, never file access time. */
public final class QuranSyncPage {
  public static final long REFRESH_MS = 6L * 86400000;
  private QuranSyncPage() {}
  public static long validate(String json, QuranChapters.Chapter chapter, int page, long now) throws Exception {
    QuranPage.parse(json, chapter, page);
    return validateMetadata(json, now);
  }
  public static long validateMetadata(String json, long now) throws Exception {
    JSONObject metadata = new JSONObject(json).getJSONObject("zikra_sync");
    long checked = metadata.getLong("checked_at");
    if (!(metadata.get("checked_at") instanceof Number) || ((Number) metadata.get("checked_at")).doubleValue() != checked
        || !(metadata.get("schema") instanceof Number) || !(metadata.get("translation") instanceof Number)
        || metadata.getInt("schema") != 1 || metadata.getInt("translation") != 33
        || !"Quran Foundation Content Sync".equals(metadata.getString("source"))
        || checked < 1 || checked > now + 60000
        || !(metadata.get("retained_during_outage") instanceof Boolean))
      throw new IllegalArgumentException("Invalid Quran source metadata");
    return checked;
  }
  public static boolean fresh(long checked, long now) {
    return checked > 0 && checked <= now + 60000 && now - checked < REFRESH_MS;
  }
}
