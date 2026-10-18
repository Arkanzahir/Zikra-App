package com.zikra.app.safety;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Official BMKG fields only; unavailable values remain empty rather than invented. */
public final class BmkgParser {
  public static final String KEPUTIH_CODE = "35.78.09.1001";

  private BmkgParser() {}

  public static Earthquake earthquake(String json) throws JSONException {
    JSONObject quake = new JSONObject(json).getJSONObject("Infogempa").getJSONObject("gempa");
    String area = text(quake, "Wilayah"), magnitude = text(quake, "Magnitude");
    if (area.isEmpty() || magnitude.isEmpty())
      throw new JSONException("Informasi gempa tidak lengkap");
    Double latitude = null, longitude = null;
    String[] coordinates = text(quake, "Coordinates").split(",");
    if (coordinates.length == 2) {
      try {
        double lat = Double.parseDouble(coordinates[0].trim());
        double lon = Double.parseDouble(coordinates[1].trim());
        if (Double.isFinite(lat)
            && Double.isFinite(lon)
            && Math.abs(lat) <= 90
            && Math.abs(lon) <= 180) {
          latitude = lat;
          longitude = lon;
        }
      } catch (NumberFormatException ignored) {
      }
    }
    return new Earthquake(
        magnitude,
        area,
        text(quake, "Kedalaman"),
        text(quake, "Potensi"),
        text(quake, "Dirasakan"),
        text(quake, "Tanggal") + " " + text(quake, "Jam"),
        ForecastSelector.parseTimestamp(text(quake, "DateTime"), false),
        latitude,
        longitude,
        text(quake, "Lintang"),
        text(quake, "Bujur"));
  }

  public static Weather weather(String json, Instant now) throws JSONException {
    return forecast(json, now).current;
  }

  /** Flattens all days, validates location/timestamps, and orders forecast slots for the chart. */
  public static Forecast forecast(String json, Instant now) throws JSONException {
    JSONObject root = new JSONObject(json);
    JSONArray data = root.getJSONArray("data");
    JSONObject rootArea = root.optJSONObject("lokasi");
    List<Weather> slots = new ArrayList<>();
    for (int i = 0; i < data.length(); i++) {
      JSONObject entry = data.optJSONObject(i);
      if (entry == null) continue;
      JSONObject area = entry.optJSONObject("lokasi");
      if (area == null) area = rootArea;
      if (area == null || !KEPUTIH_CODE.equals(text(area, "adm4"))) continue;
      JSONArray days = entry.optJSONArray("cuaca");
      if (days == null) continue;
      for (int day = 0; day < days.length(); day++) {
        JSONArray predictions = days.optJSONArray(day);
        if (predictions == null) continue;
        for (int slot = 0; slot < predictions.length(); slot++) {
          JSONObject item = predictions.optJSONObject(slot);
          if (item == null) continue;
          Instant time = ForecastSelector.parseTimestamp(text(item, "utc_datetime"), false);
          if (time == null) time = ForecastSelector.parseTimestamp(text(item, "datetime"), false);
          if (time == null)
            time = ForecastSelector.parseTimestamp(text(item, "local_datetime"), true);
          if (time == null) continue;
          slots.add(
              new Weather(
                  text(area, "desa"),
                  text(area, "kecamatan"),
                  text(area, "kotkab"),
                  text(item, "t"),
                  text(item, "weather_desc"),
                  text(item, "hu"),
                  text(item, "ws"),
                  time,
                  time.isBefore(now),
                  text(item, "wd"),
                  text(item, "vs_text")));
        }
      }
    }
    if (slots.isEmpty()) throw new JSONException("Prakiraan Keputih belum tersedia");
    slots.sort(Comparator.comparing(slot -> slot.time));
    // A repeated timestamp can occur in adjacent forecast-day groups.
    List<Weather> unique = new ArrayList<>();
    for (Weather slot : slots) {
      if (!unique.isEmpty() && unique.get(unique.size() - 1).time.equals(slot.time)) continue;
      unique.add(slot);
    }
    List<Instant> times = new ArrayList<>();
    for (Weather slot : unique) times.add(slot.time);
    return new Forecast(unique.get(ForecastSelector.select(times, now)), unique);
  }

  private static String text(JSONObject object, String key) {
    return !object.has(key) || object.isNull(key) ? "" : object.optString(key, "").trim();
  }

  public static final class Earthquake {
    public final String magnitude,
        area,
        depth,
        potential,
        felt,
        sourceDate,
        latitudeText,
        longitudeText;
    public final Instant time;
    public final Double latitude, longitude;

    public Earthquake(
        String magnitude,
        String area,
        String depth,
        String potential,
        String felt,
        String sourceDate,
        Instant time) {
      this(magnitude, area, depth, potential, felt, sourceDate, time, null, null, "", "");
    }

    public Earthquake(
        String magnitude,
        String area,
        String depth,
        String potential,
        String felt,
        String sourceDate,
        Instant time,
        Double latitude,
        Double longitude,
        String latitudeText,
        String longitudeText) {
      this.magnitude = magnitude;
      this.area = area;
      this.depth = depth;
      this.potential = potential;
      this.felt = felt;
      this.sourceDate = sourceDate;
      this.time = time;
      this.latitude = latitude;
      this.longitude = longitude;
      this.latitudeText = latitudeText;
      this.longitudeText = longitudeText;
    }

    public boolean hasCoordinates() {
      return latitude != null && longitude != null;
    }
  }

  public static final class Forecast {
    public final Weather current;
    public final List<Weather> slots;
    public final String source;
    public final Instant fetchedAt;
    public final boolean stored, stale;

    public Forecast(Weather current, List<Weather> slots) {
      this(current, slots, "BMKG", Instant.now(), false, false);
    }

    public Forecast(
        Weather current,
        List<Weather> slots,
        String source,
        Instant fetchedAt,
        boolean stored,
        boolean stale) {
      this.source = source;
      this.fetchedAt = fetchedAt;
      this.stored = stored;
      this.stale = stale;
      this.current = current;
      this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
    }
  }

  public static final class Weather {
    public final String village,
        district,
        city,
        temperature,
        description,
        humidity,
        wind,
        windDirection,
        visibility;
    public final Instant time;
    public final boolean expired;

    public Weather(
        String village,
        String district,
        String city,
        String temperature,
        String description,
        String humidity,
        String wind,
        Instant time,
        boolean expired) {
      this(
          village, district, city, temperature, description, humidity, wind, time, expired, "", "");
    }

    public Weather(
        String village,
        String district,
        String city,
        String temperature,
        String description,
        String humidity,
        String wind,
        Instant time,
        boolean expired,
        String windDirection,
        String visibility) {
      this.village = village;
      this.district = district;
      this.city = city;
      this.temperature = temperature;
      this.description = description;
      this.humidity = humidity;
      this.wind = wind;
      this.time = time;
      this.expired = expired;
      this.windDirection = windDirection;
      this.visibility = visibility;
    }
  }
}
