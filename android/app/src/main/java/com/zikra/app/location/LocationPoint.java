package com.zikra.app.location;

import java.time.Instant;
import java.time.ZoneId;

/** A selected coordinate and its explicit IANA time zone, independent of device clock settings. */
public final class LocationPoint {
  public static final LocationPoint INITIAL =
      new LocationPoint(-7.280, 112.790, "Asia/Jakarta", "Sukolilo, Surabaya", "initial", 0, -1);
  public final double latitude, longitude;
  public final ZoneId zone;
  public final String label, source;
  public final long selectedAt;
  public final float accuracy;

  public LocationPoint(
      double latitude,
      double longitude,
      String zone,
      String label,
      String source,
      long selectedAt,
      float accuracy) {
    if (!Double.isFinite(latitude)
        || !Double.isFinite(longitude)
        || Math.abs(latitude) > 90
        || Math.abs(longitude) > 180
        || label == null
        || label.trim().isEmpty()
        || label.length() > 200
        || !("initial".equals(source) || "manual".equals(source) || "device".equals(source))
        || !Float.isFinite(accuracy)
        || accuracy < -1) throw new IllegalArgumentException("Location");
    this.latitude = latitude;
    this.longitude = longitude;
    this.zone = ZoneId.of(zone);
    this.label = label.trim();
    this.source = source;
    this.selectedAt = selectedAt;
    this.accuracy = accuracy;
  }

  public String key() {
    return Double.toHexString(latitude) + ":" + Double.toHexString(longitude) + ":" + zone.getId();
  }

  public String zoneLabel(Instant at) {
    switch (zone.getId()) {
      case "Asia/Jakarta":
      case "Asia/Pontianak":
        return "WIB";
      case "Asia/Makassar":
        return "WITA";
      case "Asia/Jayapura":
        return "WIT";
      default:
        return "UTC" + zone.getRules().getOffset(at).getId().replace("Z", "+00:00");
    }
  }

  public String caption() {
    return label
        + " · "
        + zoneLabel(Instant.now())
        + (source.equals("initial") ? " · lokasi awal" : "");
  }

  public double distanceTo(double lat, double lon) {
    double a = Math.toRadians(lat - latitude), b = Math.toRadians(lon - longitude);
    double h =
        Math.sin(a / 2) * Math.sin(a / 2)
            + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(lat))
                * Math.sin(b / 2)
                * Math.sin(b / 2);
    return 6371008.8 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))));
  }

  public float qiblaBearing() {
    double targetLat = Math.toRadians(21.422487), delta = Math.toRadians(39.826206 - longitude);
    double lat = Math.toRadians(latitude);
    return (float)
        ((Math.toDegrees(
                    Math.atan2(
                        Math.sin(delta),
                        Math.cos(lat) * Math.tan(targetLat) - Math.sin(lat) * Math.cos(delta)))
                + 360)
            % 360);
  }
}
