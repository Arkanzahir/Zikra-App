package com.zikra.app.location;

import java.util.ArrayList;
import java.util.List;

/** Human-readable administrative names, without street addresses or invented nearby cities. */
public final class PlaceNames {
  private PlaceNames() {}

  public static String label(String district, String city, String county, String region) {
    List<String> parts = new ArrayList<>();
    for (String value : new String[] {district, city, county, region}) {
      if (value == null) continue;
      String clean = value.replaceAll("[\\p{Cntrl}\\p{Cf}]", " ").trim().replaceAll("\\s+", " ");
      if (clean.isEmpty() || clean.length() > 80) continue;
      boolean duplicate = false;
      for (String part : parts) if (part.equalsIgnoreCase(clean)) duplicate = true;
      if (!duplicate) parts.add(clean);
      if (parts.size() == 3) break;
    }
    String label = String.join(", ", parts);
    return label.isEmpty() || label.length() > 200 ? "Lokasi perangkat" : label;
  }
}
