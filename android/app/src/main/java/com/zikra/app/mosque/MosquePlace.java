package com.zikra.app.mosque;

import com.zikra.app.location.LocationPoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Provider records with measured straight-line distance; no invented facilities or prayer times.
 */
public final class MosquePlace {
  public final String id, name, address, provider;
  public final double latitude, longitude, distance;

  public MosquePlace(
      String id,
      String name,
      String address,
      String provider,
      double lat,
      double lon,
      LocationPoint origin) {
    if (!Double.isFinite(lat)
        || !Double.isFinite(lon)
        || Math.abs(lat) > 90
        || Math.abs(lon) > 180
        || id.isEmpty()
        || name.length() > 250
        || address.length() > 600) throw new IllegalArgumentException("Place");
    this.id = id;
    this.name = name.isEmpty() ? "Tempat ibadah Muslim" : name;
    this.address = address;
    this.provider = provider;
    latitude = lat;
    longitude = lon;
    distance = origin.distanceTo(lat, lon);
  }

  public static List<MosquePlace> parse(
      String json, String provider, LocationPoint origin, int radius) throws Exception {
    if (!provider.equals("osm")) throw new IllegalArgumentException("Unsupported provider");
    JSONObject root = new JSONObject(json);
    if (root.has("error") || root.has("remark"))
      throw new IllegalArgumentException("Incomplete places response");
    JSONArray rows = root.getJSONArray("elements");
    List<MosquePlace> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    if (rows.length() > 1000) throw new IllegalArgumentException("Too many places");
    for (int i = 0; i < rows.length(); i++) {
      JSONObject r = rows.getJSONObject(i);
      try {
        MosquePlace place;
        {
          JSONObject t = r.getJSONObject("tags");
          if (!t.optString("religion").equals("muslim")
              && !t.optString("building").equals("mosque")) continue;
          JSONObject center = r.has("lat") ? r : r.getJSONObject("center");
          String address = t.optString("addr:street", "");
          String number = t.optString("addr:housenumber", ""), city = t.optString("addr:city", "");
          if (!number.isEmpty()) address += (address.isEmpty() ? "" : " ") + number;
          if (!city.isEmpty()) address += (address.isEmpty() ? "" : ", ") + city;
          place =
              new MosquePlace(
                  r.getString("type") + "/" + r.getLong("id"),
                  t.optString("name", ""),
                  address,
                  provider,
                  center.getDouble("lat"),
                  center.getDouble("lon"),
                  origin);
        }
        if (place.distance <= radius + 5 && seen.add(place.id)) result.add(place);
      } catch (Exception malformed) {
        /* One bad record must not become a made-up place. */
      }
    }
    result.sort(Comparator.comparingDouble(p -> p.distance));
    return result.subList(0, Math.min(60, result.size()));
  }
}
