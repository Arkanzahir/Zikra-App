package com.zikra.app.safety;

import com.zikra.app.R;
import java.util.Locale;

/** Weather artwork follows the received description; unknown conditions use a neutral cloud. */
public final class WeatherSymbols {
  private WeatherSymbols() {}

  public static int forDescription(String description) {
    String value = description == null ? "" : description.toLowerCase(Locale.ROOT);
    if (value.contains("petir")) return R.drawable.weather_symbol_storm;
    if ((value.contains("hujan") || value.contains("gerimis")))
      return R.drawable.weather_symbol_rain;
    if (value.contains("cerah")) return R.drawable.weather_symbol_fair;
    return R.drawable.weather_symbol_cloud;
  }
}
