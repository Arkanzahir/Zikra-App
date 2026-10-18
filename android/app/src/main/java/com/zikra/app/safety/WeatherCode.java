package com.zikra.app.safety;

/** WMO weather interpretation used by Open-Meteo, not an official warning category. */
public final class WeatherCode {
  private WeatherCode() {}

  public static String description(int code) {
    switch (code) {
      case 0:
        return "Cerah";
      case 1:
        return "Cerah berawan";
      case 2:
        return "Berawan sebagian";
      case 3:
        return "Berawan";
      case 45:
      case 48:
        return "Berkabut";
      case 51:
        return "Gerimis ringan";
      case 53:
        return "Gerimis";
      case 55:
        return "Gerimis lebat";
      case 56:
      case 57:
        return "Gerimis beku";
      case 61:
        return "Hujan ringan";
      case 63:
        return "Hujan sedang";
      case 65:
        return "Hujan lebat";
      case 66:
      case 67:
        return "Hujan beku";
      case 71:
      case 73:
      case 75:
      case 77:
        return "Salju";
      case 80:
        return "Hujan lokal ringan";
      case 81:
        return "Hujan lokal";
      case 82:
        return "Hujan lokal lebat";
      case 85:
      case 86:
        return "Hujan salju";
      case 95:
        return "Hujan petir";
      case 96:
      case 99:
        return "Petir dan hujan es";
      default:
        throw new IllegalArgumentException("Unknown WMO code");
    }
  }

  public static String direction(double degrees) {
    String[] values = {
      "Utara", "Timur laut", "Timur", "Tenggara", "Selatan", "Barat daya", "Barat", "Barat laut"
    };
    return values[(int) Math.round(degrees / 45) % 8];
  }

  public static boolean usableCache(long fetched, long now, long maximumAge) {
    return fetched > 0 && fetched <= now && now - fetched <= maximumAge;
  }
}
