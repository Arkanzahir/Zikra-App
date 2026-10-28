package com.zikra.app.prayer;

import com.zikra.app.location.LocationPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Provider IDs are opaque. Only an unambiguous named region can be selected automatically. */
public final class PrayerRegion {
  public final String id, name;
  private final String matchName;

  public PrayerRegion(String id, String name) {
    if (id == null || !id.matches("[a-f0-9]{32}") || name == null
        || name.length() > 100 || !name.matches("[A-Z][A-Z .'-]{2,99}"))
      throw new IllegalArgumentException("Prayer region");
    this.id = id;
    this.name = name;
    this.matchName = " " + normalize(name) + " ";
  }

  public static List<PrayerRegion> parse(String text) throws Exception {
    JSONObject root = new JSONObject(text);
    if (!root.getBoolean("status")) throw new IllegalArgumentException("Region status");
    JSONArray rows = root.getJSONArray("data");
    if (rows.length() < 1 || rows.length() > 600) throw new IllegalArgumentException("Regions");
    List<PrayerRegion> result = new ArrayList<>();
    java.util.Set<String> ids = new java.util.HashSet<>();
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      PrayerRegion region = new PrayerRegion(row.getString("id"), row.getString("lokasi"));
      if (!ids.add(region.id)) throw new IllegalArgumentException("Duplicate region");
      result.add(region);
    }
    return java.util.Collections.unmodifiableList(result);
  }

  public static PrayerRegion automatic(List<PrayerRegion> regions, LocationPoint point) {
    if (!indonesianPoint(point)) return null;
    PrayerRegion selected = null;
    String label = " " + normalize(point.label) + " ";
    for (PrayerRegion region : regions) {
      if (!label.contains(region.matchName)) continue;
      if (selected != null) return null; // e.g. Kota and Kabupaten Bogor must not be conflated.
      selected = region;
    }
    return selected;
  }

  public static boolean indonesianPoint(LocationPoint point) {
    String zone = point.zone.getId();
    return point.latitude >= -12 && point.latitude <= 7 && point.longitude >= 94
        && point.longitude <= 142 && (zone.equals("Asia/Jakarta")
        || zone.equals("Asia/Pontianak") || zone.equals("Asia/Makassar")
        || zone.equals("Asia/Jayapura"));
  }

  static String normalize(String text) {
    return text.toUpperCase(Locale.ROOT).replaceAll("\\b(KOTA|KABUPATEN|KAB|INDONESIA)\\b", " ")
        .replaceAll("[^A-Z0-9]", " ").replaceAll("\\s+", " ").trim();
  }

  @Override public String toString() { return name; }
}
