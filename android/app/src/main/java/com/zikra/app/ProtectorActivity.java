package com.zikra.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zikra.app.safety.BmkgClient;
import com.zikra.app.safety.BmkgParser;
import com.zikra.app.safety.IndonesiaMapView;
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_protector);
    setupScreen("Zikra Protector");
    lightChrome();
    text(
        R.id.protectorToolbarSubtitle,
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)
            .withZone(ZoneId.of("Asia/Jakarta"))
            .format(Instant.now()));
    bmkg = new BmkgClient(this);
    findViewById(R.id.protectorRefresh).setOnClickListener(v -> refresh());
    findViewById(R.id.protectorWeatherCard).setOnClickListener(v -> open(WeatherActivity.class));
    findViewById(R.id.protectorQuakeCard).setOnClickListener(v -> open(EarthquakeActivity.class));
    findViewById(R.id.protectorQuakeOpen).setOnClickListener(v -> open(EarthquakeActivity.class));
    findViewById(R.id.protectorWarnings)
        .setOnClickListener(
            v ->
                external(
                    Intent.ACTION_VIEW, "https://www.bmkg.go.id/cuaca/peringatan-dini-cuaca.bmkg"));
    findViewById(R.id.protectorDial)
        .setOnClickListener(v -> external(Intent.ACTION_DIAL, "tel:112"));
    findViewById(R.id.protectorWhatsapp)
        .setOnClickListener(
            v ->
                external(
                    Intent.ACTION_VIEW,
                    "https://wa.me/?text="
                        + android.net.Uri.encode(
                            "Saya membutuhkan bantuan. Mohon hubungi saya. Lokasi saya: ")));
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
    refresh();
  }

  private void lightChrome() {
    getWindow().setStatusBarColor(Color.WHITE);
    getWindow().setNavigationBarColor(Color.WHITE);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            getWindow().getDecorView().getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }

  private void refresh() {
    bmkg.reset();
    text(R.id.protectorWeatherTemperature, "—°C");
    text(R.id.protectorWeatherDescription, "Memuat prakiraan…");
    text(R.id.protectorWeatherTime, "Waktu prakiraan belum tersedia");
    text(R.id.protectorWeatherStatus, "Menghubungkan ke BMKG");
    text(R.id.protectorWeatherDetails, "Prakiraan Keputih, Sukolilo · Data sedang dimuat");
    text(R.id.protectorMagnitude, "M —");
    text(R.id.protectorQuakeArea, "Memuat laporan gempa…");
    text(R.id.protectorQuakeStatus, "Data resmi BMKG · Indonesia");
    text(R.id.protectorQuakeTime, "Waktu kejadian belum tersedia");
    text(R.id.protectorQuakeDetails, "Lokasi episentrum ditampilkan setelah koordinat diterima.");
    ((IndonesiaMapView) findViewById(R.id.protectorQuakeMap)).setEpicenter(null, null);
    bmkg.weather(
        new BmkgClient.Result<BmkgParser.Forecast>() {
          @Override
          public void success(BmkgParser.Forecast value) {
            showWeather(value.current);
          }

          @Override
          public void failure(String message) {
            text(R.id.protectorWeatherDescription, "Belum ada data cuaca");
            text(R.id.protectorWeatherStatus, "Ketuk ↻ untuk mencoba lagi");
            text(R.id.protectorWeatherDetails, message);
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
            text(R.id.protectorQuakeArea, "Data gempa belum tersedia");
            text(R.id.protectorQuakeStatus, "Ketuk ↻ untuk mencoba lagi");
            text(R.id.protectorQuakeDetails, message);
          }
        });
  }

  private void showWeather(BmkgParser.Weather w) {
    text(R.id.protectorWeatherTemperature, w.temperature.isEmpty() ? "—°C" : w.temperature + "°C");
    text(R.id.protectorWeatherDescription, available(w.description));
    text(R.id.protectorWeatherTime, "Prakiraan " + TIME.format(w.time));
    text(
        R.id.protectorWeatherStatus,
        w.expired
            ? "Prakiraan terakhir sudah lewat · muat ulang"
            : "BMKG · " + w.village + ", " + w.district);
    text(
        R.id.protectorWeatherDetails,
        "Kelembapan "
            + unit(w.humidity, "%")
            + "  ·  Angin "
            + unit(w.wind, " km/jam")
            + "  ·  Lihat prakiraan →");
  }

  private void showQuake(BmkgParser.Earthquake q) {
    text(R.id.protectorMagnitude, "M " + q.magnitude);
    text(R.id.protectorQuakeArea, q.area);
    text(R.id.protectorQuakeTime, q.time == null ? q.sourceDate : TIME.format(q.time));
    text(R.id.protectorQuakeStatus, "Laporan terakhir BMKG · Bukan peringatan lokasi Anda");
    text(
        R.id.protectorQuakeDetails,
        "Kedalaman " + available(q.depth) + "\n" + available(q.potential));
    ((IndonesiaMapView) findViewById(R.id.protectorQuakeMap)).setEpicenter(q.latitude, q.longitude);
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
