package com.zikra.app.safety;

import com.zikra.app.R;
import java.util.Locale;

/** Weather artwork follows the received description; unknown conditions use a neutral cloud. */
public final class WeatherSymbols {
  private WeatherSymbols() {}

  public static int forForecast(String description, boolean night, boolean sky) {
    String value = description == null ? "" : description.toLowerCase(Locale.ROOT);
    String key =
        value.isEmpty()
            ? "unavailable"
            : value.contains("petir")
                ? "thunderstorm"
                : value.contains("hujan lebat")
                    ? "rain"
                    : value.contains("hujan") || value.contains("gerimis")
                        ? "light_rain"
                        : value.contains("kabut")
                            ? "fog"
                            : value.contains("cerah berawan") || value.contains("sebagian")
                                ? (night ? "partly_cloudy_night" : "partly_cloudy_day")
                                : value.contains("cerah")
                                    ? (night ? "clear_night" : "clear_day")
                                    : "cloudy";
    switch (key) {
      case "clear_day":
        return sky ? R.drawable.weather_sky_clear_day : R.drawable.protector_weather_clear_day;
      case "clear_night":
        return sky ? R.drawable.weather_sky_clear_night : R.drawable.protector_weather_clear_night;
      case "partly_cloudy_day":
        return sky
            ? R.drawable.weather_sky_partly_cloudy_day
            : R.drawable.protector_weather_partly_cloudy_day;
      case "partly_cloudy_night":
        return sky
            ? R.drawable.weather_sky_partly_cloudy_night
            : R.drawable.protector_weather_partly_cloudy_night;
      case "rain":
        return sky ? R.drawable.weather_sky_rain : R.drawable.protector_weather_rain;
      case "light_rain":
        return sky ? R.drawable.weather_sky_light_rain : R.drawable.protector_weather_light_rain;
      case "thunderstorm":
        return sky
            ? R.drawable.weather_sky_thunderstorm
            : R.drawable.protector_weather_thunderstorm;
      case "fog":
        return sky ? R.drawable.weather_sky_fog : R.drawable.protector_weather_fog;
      case "unavailable":
        return sky ? R.drawable.weather_sky_unavailable : R.drawable.protector_weather_unavailable;
      default:
        return sky ? R.drawable.weather_sky_cloudy : R.drawable.protector_weather_cloudy;
    }
  }

  public static int forDescription(String description) {
    String value = description == null ? "" : description.toLowerCase(Locale.ROOT);
    if (value.contains("petir")) return R.drawable.weather_symbol_storm;
    if ((value.contains("hujan") || value.contains("gerimis")))
      return R.drawable.weather_symbol_rain;
    if (value.contains("cerah")) return R.drawable.weather_symbol_fair;
    return R.drawable.weather_symbol_cloud;
  }
}
