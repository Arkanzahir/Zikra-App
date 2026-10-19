package com.zikra.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zikra.app.safety.BmkgClient;
import com.zikra.app.safety.BmkgParser;
import com.zikra.app.safety.IndonesiaMapView;
import com.zikra.app.safety.ProtectorChrome;
import com.zikra.app.safety.WeatherSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Native Info BMKG dashboard: official forecasts, recent earthquake, and emergency actions. */
public final class ProtectorActivity extends BaseActivity {
  private static final Locale ID = new Locale("id", "ID");
  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm 'WIB'", ID)
          .withZone(ZoneId.of("Asia/Jakarta"));
  private BmkgClient bmkg;
  private BmkgParser.Forecast displayedWeather;
  private BmkgParser.Earthquake displayedQuake;
  private Instant quakeRetrievedAt;
  private final android.os.Handler refreshTimer =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private final Runnable refreshTick =
      new Runnable() {
        @Override
        public void run() {
          refresh();
          refreshTimer.postDelayed(this, com.zikra.app.safety.OpenMeteoClient.FRESH_MS);
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_protector);
    setupScreen("Zikra Protector");
    ProtectorChrome.apply(this);
    text(
        R.id.protectorToolbarSubtitle,
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)
            .withZone(ZoneId.of("Asia/Jakarta"))
            .format(Instant.now()));
    if (getResources().getConfiguration().fontScale > 1.3f) {
      android.widget.LinearLayout metrics = findViewById(R.id.protectorWeatherMetricsHeading);
      metrics.setOrientation(android.widget.LinearLayout.VERTICAL);
      for (int i = 1; i < metrics.getChildCount(); i++) {
        android.view.View child = metrics.getChildAt(i);
        android.widget.LinearLayout.LayoutParams params =
            (android.widget.LinearLayout.LayoutParams) child.getLayoutParams();
        params.width = -1;
        params.weight = 0;
      }
    }
    bmkg = new BmkgClient(this);
    findViewById(R.id.protectorRefresh).setOnClickListener(v -> refresh());
    findViewById(R.id.protectorWeatherCard).setOnClickListener(v -> open(WeatherActivity.class));
    findViewById(R.id.protectorQuakeCard).setOnClickListener(v -> open(EarthquakeActivity.class));
    findViewById(R.id.protectorQuakeOpen).setOnClickListener(v -> open(EarthquakeActivity.class));
    findViewById(R.id.protectorWarnings)
        .setOnClickListener(
            v ->
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Informasi resmi BMKG")
                    .setMessage(
                        "Buka situs BMKG untuk memeriksa informasi cuaca. Akses halaman peringatan"
                            + " dapat dibatasi atau berubah. Prakiraan Open-Meteo di Zikra bukan"
                            + " peringatan dini.")
                    .setPositiveButton(
                        "Buka BMKG",
                        (dialog, which) -> external(Intent.ACTION_VIEW, "https://www.bmkg.go.id/"))
                    .setNeutralButton(
                        "Sumber data BMKG",
                        (dialog, which) ->
                            external(
                                Intent.ACTION_VIEW, "https://data.bmkg.go.id/prakiraan-cuaca/"))
                    .setNegativeButton("Tutup", null)
                    .show());
    findViewById(R.id.protectorDial)
        .setOnClickListener(v -> external(Intent.ACTION_DIAL, "tel:112"));
    findViewById(R.id.protectorWhatsapp)
        .setOnClickListener(
            v ->
                external(
                    Intent.ACTION_VIEW,
                    "https://wa.me/?text="
                        + android.net.Uri.encode(
                            "Saya membutuhkan bantuan. Mohon hubungi saya. Saya akan menuliskan"
                                + " lokasi saya di pesan ini.")));
    findViewById(R.id.protectorMaps)
        .setOnClickListener(
            v ->
                external(
                    Intent.ACTION_VIEW,
                    "https://www.google.com/maps/search/?api=1&query=Masjid+Manarul+Ilmi+ITS+Surabaya"));
    findViewById(R.id.protectorSource)
        .setOnClickListener(v -> external(Intent.ACTION_VIEW, "https://data.bmkg.go.id/"));
    findViewById(R.id.protectorTabHome)
        .setOnClickListener(
            v -> ((ScrollView) findViewById(R.id.protectorScroll)).smoothScrollTo(0, 0));
    findViewById(R.id.protectorTabWeather).setOnClickListener(v -> open(WeatherActivity.class));
    findViewById(R.id.protectorTabQuake).setOnClickListener(v -> open(EarthquakeActivity.class));
    findViewById(R.id.protectorTabHelp).setOnClickListener(v -> showEmergencyActions());
  }

  @Override
  protected void onResume() {
    super.onResume();
    refreshTimer.post(refreshTick);
  }

  @Override
  protected void onPause() {
    refreshTimer.removeCallbacks(refreshTick);
    if (bmkg != null) bmkg.reset();
    super.onPause();
  }

  private void refresh() {
    bmkg.reset();
    if (displayedWeather != null
        && !com.zikra.app.safety.WeatherCode.usableCache(
            displayedWeather.fetchedAt.toEpochMilli(),
            System.currentTimeMillis(),
            com.zikra.app.safety.OpenMeteoClient.MAX_AGE_MS)) displayedWeather = null;
    text(
        R.id.protectorWeatherStatus,
        displayedWeather == null
            ? "Menghubungkan ke Open-Meteo…"
            : com.zikra.app.safety.OpenMeteoClient.status(displayedWeather) + "\nMemperbarui…");
    if (displayedWeather == null) {
      text(R.id.protectorWeatherTemperature, "—°C");
      ((ImageView) findViewById(R.id.protectorWeatherIcon))
          .setImageResource(WeatherSymbols.forDescription(null));
      text(R.id.protectorWeatherDescription, "Memuat prakiraan…");
      text(R.id.protectorWeatherTime, "Waktu prakiraan belum tersedia");
      text(R.id.protectorWeatherStatus, "Menghubungkan ke Open-Meteo");
      text(R.id.protectorWeatherDetails, "Acuan Sukolilo, Surabaya · Data sedang dimuat");
    }
    text(
        R.id.protectorQuakeStatus,
        displayedQuake == null ? "Menghubungkan ke BMKG…" : quakeStatus() + "\nMemperbarui…");
    if (displayedQuake == null) {
      text(R.id.protectorMagnitude, "M —");
      text(R.id.protectorQuakeArea, "Memuat laporan gempa…");
      text(R.id.protectorQuakeStatus, "Data resmi BMKG · Indonesia");
      text(R.id.protectorQuakeTime, "Waktu kejadian belum tersedia");
      text(R.id.protectorQuakeDetails, "Lokasi episentrum ditampilkan setelah koordinat diterima.");
      ((IndonesiaMapView) findViewById(R.id.protectorQuakeMap)).setEpicenter(null, null);
    }
    bmkg.weather(
        new BmkgClient.Result<BmkgParser.Forecast>() {
          @Override
          public void success(BmkgParser.Forecast value) {
            showWeather(value);
          }

          @Override
          public void failure(String message) {
            if (displayedWeather == null) {
              text(R.id.protectorWeatherDescription, "Cuaca belum tersedia");
              text(R.id.protectorWeatherDetails, message);
            }
            text(
                R.id.protectorWeatherStatus,
                displayedWeather == null
                    ? "Periksa koneksi, lalu pilih Muat ulang di kanan atas."
                    : com.zikra.app.safety.OpenMeteoClient.status(displayedWeather)
                        + "\nPembaruan gagal. Pilih Muat ulang.");
          }
        });
    bmkg.earthquake(
        new BmkgClient.Result<BmkgParser.Earthquake>() {
          @Override
          public void success(BmkgParser.Earthquake value) {
            showQuake(value);
          }

          @Override
          public void failure(String message) {
            if (displayedQuake == null) {
              text(R.id.protectorQuakeArea, "Laporan gempa belum tersedia");
              text(R.id.protectorQuakeDetails, message);
            }
            text(
                R.id.protectorQuakeStatus,
                displayedQuake == null
                    ? "Periksa koneksi, lalu pilih Muat ulang di kanan atas."
                    : quakeStatus() + "\nPembaruan gagal. Laporan sebelumnya tetap ditampilkan.");
          }
        });
  }

  private void showWeather(BmkgParser.Forecast forecast) {
    displayedWeather = forecast;
    BmkgParser.Weather w = forecast.current;
    ((ImageView) findViewById(R.id.protectorWeatherIcon))
        .setImageResource(WeatherSymbols.forDescription(w.description));
    text(R.id.protectorWeatherTemperature, w.temperature.isEmpty() ? "—°C" : w.temperature + "°C");
    text(R.id.protectorWeatherDescription, available(w.description));
    text(R.id.protectorWeatherTime, "Prakiraan " + TIME.format(w.time));
    text(R.id.protectorWeatherStatus, com.zikra.app.safety.OpenMeteoClient.status(forecast));
    text(
        R.id.protectorWeatherDetails,
        "Kelembapan "
            + unit(w.humidity, "%")
            + "  ·  Angin "
            + unit(w.wind, " km/jam")
            + "  ·  Lihat prakiraan →");
  }

  private void showQuake(BmkgParser.Earthquake q) {
    displayedQuake = q;
    quakeRetrievedAt = Instant.now();
    text(R.id.protectorMagnitude, "M " + q.magnitude);
    text(R.id.protectorQuakeArea, q.area);
    text(R.id.protectorQuakeTime, q.time == null ? q.sourceDate : TIME.format(q.time));
    text(R.id.protectorQuakeStatus, quakeStatus());
    text(
        R.id.protectorQuakeDetails,
        "Kedalaman " + available(q.depth) + "\n" + available(q.potential));
    ((IndonesiaMapView) findViewById(R.id.protectorQuakeMap)).setEpicenter(q.latitude, q.longitude);
  }

  private String quakeStatus() {
    return "Laporan terakhir BMKG · Indonesia\nDiambil "
        + TIME.format(quakeRetrievedAt)
        + "\nBukan peringatan untuk lokasi Anda.";
  }

  private String available(String value) {
    return value.isEmpty() ? "Belum tersedia" : value;
  }

  private String unit(String value, String unit) {
    return value.isEmpty() ? "—" : value + unit;
  }

  private void text(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }

  @Override
  protected void onDestroy() {
    if (bmkg != null) bmkg.close();
    super.onDestroy();
  }
}
