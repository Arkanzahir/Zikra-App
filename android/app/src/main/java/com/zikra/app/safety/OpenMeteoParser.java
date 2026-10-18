package com.zikra.app.safety;

import com.zikra.app.PrayerSchedule;
import java.time.*;
import java.util.*;
import org.json.*;

public final class OpenMeteoParser {
  private OpenMeteoParser() {}

  public static BmkgParser.Forecast parse(
      String json, Instant now, Instant fetched, boolean stored, boolean stale)
      throws JSONException {
    JSONObject root = new JSONObject(json);
    double lat = root.getDouble("latitude"), lon = root.getDouble("longitude");
    if (!Double.isFinite(lat)
        || !Double.isFinite(lon)
        || Math.abs(lat - PrayerSchedule.LATITUDE) > .1
        || Math.abs(lon - PrayerSchedule.LONGITUDE) > .1
        || !root.getString("timezone").equals("Asia/Jakarta")
        || root.getInt("utc_offset_seconds") != 25200) throw new JSONException("Location/timezone");
    JSONObject units = root.getJSONObject("hourly_units");
    String[][] expected = {
      {"time", "iso8601"},
      {"temperature_2m", "°C"},
      {"relative_humidity_2m", "%"},
      {"weather_code", "wmo code"},
      {"wind_speed_10m", "km/h"},
      {"wind_direction_10m", "°"}
    };
    for (String[] unit : expected)
      if (!units.getString(unit[0]).equals(unit[1])) throw new JSONException("Unit mismatch");
    JSONObject h = root.getJSONObject("hourly");
    JSONArray times = h.getJSONArray("time"),
        temperatures = h.getJSONArray("temperature_2m"),
        humidities = h.getJSONArray("relative_humidity_2m"),
        codes = h.getJSONArray("weather_code"),
        speeds = h.getJSONArray("wind_speed_10m"),
        directions = h.getJSONArray("wind_direction_10m");
    int count = times.length();
    if (count < 24
        || count > 168
        || temperatures.length() != count
        || humidities.length() != count
        || codes.length() != count
        || speeds.length() != count
        || directions.length() != count) throw new JSONException("Incomplete hourly series");
    List<BmkgParser.Weather> slots = new ArrayList<>();
    List<Instant> instants = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Instant time =
          LocalDateTime.parse(times.getString(i)).atZone(PrayerSchedule.ZONE).toInstant();
      double t = temperatures.getDouble(i),
          humidity = humidities.getDouble(i),
          wind = speeds.getDouble(i),
          direction = directions.getDouble(i);
      if (!Double.isFinite(t)
          || t < -90
          || t > 65
          || !Double.isFinite(humidity)
          || humidity < 0
          || humidity > 100
          || !Double.isFinite(wind)
          || wind < 0
          || wind > 500
          || !Double.isFinite(direction)
          || direction < 0
          || direction > 360
          || (!instants.isEmpty() && !time.isAfter(instants.get(instants.size() - 1))))
        throw new JSONException("Invalid weather series");
      if (codes.getDouble(i) != codes.getInt(i)) throw new JSONException("Weather code");
      String description = WeatherCode.description(codes.getInt(i));
      slots.add(
          new BmkgParser.Weather(
              "Sukolilo",
              "Surabaya",
              "Surabaya",
              number(t),
              description,
              number(humidity),
              number(wind),
              time,
              time.isBefore(now),
              WeatherCode.direction(direction),
              ""));
      instants.add(time);
    }
    return new BmkgParser.Forecast(
        slots.get(ForecastSelector.select(instants, now)),
        slots,
        "Open-Meteo",
        fetched,
        stored,
        stale);
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.0f", value);
  }
}
