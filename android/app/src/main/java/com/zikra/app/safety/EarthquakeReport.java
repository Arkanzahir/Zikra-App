package com.zikra.app.safety;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.json.JSONException;
import org.json.JSONObject;

/** One source report and its actual retrieval time; storage never makes it newly retrieved. */
public final class EarthquakeReport {
  public static final long FRESH_MS = 60_000L, MAX_AGE_MS = 24 * 60 * 60_000L;
  public final BmkgParser.Earthquake quake;
  public final Instant fetchedAt;
  public final boolean stored;
  private final String json;

  private EarthquakeReport(String json, BmkgParser.Earthquake quake, Instant at, boolean stored) {
    this.json = json;
    this.quake = quake;
    this.fetchedAt = at;
    this.stored = stored;
  }

  public static EarthquakeReport network(String json, Instant at) throws JSONException {
    return parse(json, at, false);
  }

  private static EarthquakeReport parse(String json, Instant at, boolean stored) throws JSONException {
    if (json == null || json.getBytes(StandardCharsets.UTF_8).length > 65536 || at == null
        || at.toEpochMilli() <= 0) throw new JSONException("Invalid earthquake snapshot");
    JSONObject event = new JSONObject(json).getJSONObject("Infogempa").getJSONObject("gempa");
    for (String key : new String[] {"Magnitude", "Wilayah", "Kedalaman", "DateTime"})
      if (!(event.opt(key) instanceof String)) throw new JSONException("Invalid source field");
    try { java.time.OffsetDateTime.parse(event.getString("DateTime")); }
    catch (java.time.DateTimeException invalid) { throw new JSONException("Missing UTC offset"); }
    BmkgParser.Earthquake q = BmkgParser.earthquake(json);
    if (q.time == null || q.time.isAfter(at.plusSeconds(300))
        || !q.depth.matches("[0-9]{1,4}(?:[.,][0-9]{1,3})?\\s*km"))
      throw new JSONException("Invalid earthquake time/depth");
    if (Double.parseDouble(q.depth.replace("km", "").trim().replace(',', '.')) > 1000)
      throw new JSONException("Depth outside supported range");
    for (String field : new String[] {q.area, q.potential, q.felt, q.sourceDate})
      if (field.length() > 2000) throw new JSONException("Earthquake field too long");
    return new EarthquakeReport(json, q, at, stored);
  }

  public String snapshot() throws JSONException {
    return new JSONObject().put("version", 1).put("fetchedAt", fetchedAt.toEpochMilli())
        .put("report", json).toString();
  }

  public static EarthquakeReport cached(String snapshot, Instant now) throws JSONException {
    if (snapshot == null || snapshot.length() > 131072) throw new JSONException("Cache too large");
    JSONObject value = new JSONObject(snapshot);
    if (value.getInt("version") != 1) throw new JSONException("Unknown cache version");
    long at = value.getLong("fetchedAt");
    if (at <= 0 || at > now.toEpochMilli() || now.toEpochMilli() - at > MAX_AGE_MS)
      throw new JSONException("Expired earthquake cache");
    return parse(value.getString("report"), Instant.ofEpochMilli(at), true);
  }

  public boolean usable(Instant now) {
    long age = now.toEpochMilli() - fetchedAt.toEpochMilli();
    return age >= 0 && age <= MAX_AGE_MS;
  }

  public boolean fresh(Instant now) {
    return usable(now) && now.toEpochMilli() - fetchedAt.toEpochMilli() < FRESH_MS;
  }

  public String imageKey() {
    return quake.time + "|" + quake.latitude + "|" + quake.longitude + "|"
        + quake.shakemapUrl + "|" + fetchedAt;
  }

  public static EarthquakeReport newest(EarthquakeReport previous, EarthquakeReport incoming) {
    return previous != null && previous.quake.time.isAfter(incoming.quake.time) ? previous : incoming;
  }
}
