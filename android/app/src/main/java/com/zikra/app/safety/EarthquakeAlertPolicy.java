package com.zikra.app.safety;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import org.json.JSONException;

/** Strict data-only push contract; independently testable without Firebase or Android. */
public final class EarthquakeAlertPolicy {
  public static final long MAX_ALERT_AGE_MS = 60 * 60 * 1000L;

  private EarthquakeAlertPolicy() {}

  public static String id(EarthquakeReport report) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256")
              .digest(
                  ("bmkg|" + report.quake.time.toEpochMilli()).getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : hash)
        result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
      return result.toString();
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  public static EarthquakeReport validate(Map<String, String> data, Instant now)
      throws JSONException {
    if (data == null
        || data.size() != 3
        || !"1".equals(data.get("schema"))
        || data.get("event_id") == null
        || !data.get("event_id").matches("[a-f0-9]{64}")
        || data.get("quake_report") == null
        || data.get("quake_report").getBytes(StandardCharsets.UTF_8).length > 2800)
      throw new JSONException("Invalid earthquake push");
    EarthquakeReport report = EarthquakeReport.cached(data.get("quake_report"), now);
    long age = now.toEpochMilli() - report.quake.time.toEpochMilli();
    if (age < 0 || age > MAX_ALERT_AGE_MS || !id(report).equals(data.get("event_id")))
      throw new JSONException("Expired or mismatched earthquake push");
    return report;
  }
}
