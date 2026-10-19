package com.zikra.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.safety.BmkgClient;
import com.zikra.app.safety.BmkgParser;
import com.zikra.app.safety.ProtectorChrome;
import com.zikra.app.safety.TemperatureChartView;
import com.zikra.app.safety.WeatherSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Forecast detail inspired by Info BMKG, using actual dated official forecast slots. */
public final class WeatherActivity extends BaseActivity {
  private final Locale locale = new Locale("id", "ID");
  private LocationPoint location = LocationPoint.INITIAL;
  private BmkgClient bmkg;
  private BmkgParser.Forecast displayed;
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
    setContentView(R.layout.activity_weather);
    location = LocationStore.current(this);
    setupScreen("Cuaca");
    ProtectorChrome.apply(this);
    text(R.id.protectorToolbarTitle, "Cuaca");
    text(R.id.protectorToolbarSubtitle, location.caption() + " · Open-Meteo");
    if (getResources().getConfiguration().fontScale > 1.3f) {
      LinearLayout metrics = findViewById(R.id.weatherMetricsHeading);
      metrics.setOrientation(LinearLayout.VERTICAL);
      ((LinearLayout.LayoutParams) findViewById(R.id.weatherHeroIcon).getLayoutParams())
              .rightMargin =
          0;
    }
    bmkg = new BmkgClient(this);
    findViewById(R.id.weatherLocation).setOnClickListener(v -> open(LocationActivity.class));
    findViewById(R.id.weatherLocation).setMinimumHeight(dp(48));
    findViewById(R.id.weatherLocation).setFocusable(true);
    findViewById(R.id.protectorRefresh).setOnClickListener(v -> refresh());
    findViewById(R.id.weatherSource)
        .setOnClickListener(v -> external(Intent.ACTION_VIEW, "https://open-meteo.com/"));
  }

  @Override
  protected void onResume() {
    super.onResume();
    LocationPoint selected = LocationStore.current(this);
    if (!selected.key().equals(location.key())) displayed = null;
    location = selected;
    text(R.id.protectorToolbarSubtitle, location.caption() + " · Open-Meteo");
    text(R.id.weatherLocation, location.label + " · Ubah lokasi");
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
    if (displayed != null
        && !com.zikra.app.safety.WeatherCode.usableCache(
            displayed.fetchedAt.toEpochMilli(),
            System.currentTimeMillis(),
            com.zikra.app.safety.OpenMeteoClient.MAX_AGE_MS)) displayed = null;
    text(
        R.id.weatherStatus,
        displayed == null
            ? "Menghubungkan ke Open-Meteo…"
            : com.zikra.app.safety.OpenMeteoClient.status(displayed) + "\nMemperbarui…");
    if (displayed == null) {
      text(R.id.weatherTemperature, "—°C");
      ((ImageView) findViewById(R.id.weatherHeroIcon))
          .setImageResource(WeatherSymbols.forDescription(null));
      text(R.id.weatherDescription, "Memuat prakiraan…");
      text(R.id.weatherStatus, "Menghubungkan ke Open-Meteo");
      text(R.id.weatherDate, "Waktu prakiraan belum tersedia");
      text(R.id.weatherHumidity, "—");
      text(R.id.weatherWind, "—");
      text(R.id.weatherDirection, "—");
      ((TemperatureChartView) findViewById(R.id.weatherChart)).setForecast(Collections.emptyList());
      ((LinearLayout) findViewById(R.id.weatherForecastList)).removeAllViews();
      findViewById(R.id.weatherEmptyForecast).setVisibility(View.VISIBLE);
      text(R.id.weatherEmptyForecast, "Prakiraan akan muncul saat data tersedia.");
    }
    bmkg.weather(
        new BmkgClient.Result<BmkgParser.Forecast>() {
          @Override
          public void success(BmkgParser.Forecast value) {
            show(value);
          }

          @Override
          public void failure(String message) {
            showFailure(message);
          }
        });
  }

  private void showFailure(String message) {
    if (displayed == null) {
      text(R.id.weatherDescription, "Cuaca belum tersedia");
      text(R.id.weatherEmptyForecast, message);
    }
    text(
        R.id.weatherStatus,
        displayed == null
            ? "Periksa koneksi, lalu pilih Muat ulang di kanan atas."
            : com.zikra.app.safety.OpenMeteoClient.status(displayed)
                + "\nPembaruan gagal. Data sebelumnya tetap ditampilkan. Pilih Muat ulang.");
  }

  private void show(BmkgParser.Forecast forecast) {
    displayed = forecast;
    BmkgParser.Weather current = forecast.current;
    ((ImageView) findViewById(R.id.weatherHeroIcon))
        .setImageResource(WeatherSymbols.forDescription(current.description));
    text(R.id.weatherLocation, forecast.location.label + " · Ubah lokasi");
    text(
        R.id.weatherDate,
        DateTimeFormatter.ofPattern("EEEE, d MMM · HH:mm", locale)
                .withZone(location.zone)
                .format(current.time)
            + " "
            + location.zoneLabel(current.time));
    text(
        R.id.weatherTemperature,
        current.temperature.isEmpty() ? "—°C" : current.temperature + "°C");
    text(R.id.weatherDescription, available(current.description));
    text(R.id.weatherStatus, com.zikra.app.safety.OpenMeteoClient.status(forecast));
    text(R.id.weatherHumidity, unit(current.humidity, "%"));
    text(R.id.weatherWind, unit(current.wind, " km/jam"));
    text(R.id.weatherDirection, available(current.windDirection));
    List<BmkgParser.Weather> relevant = new ArrayList<>();
    for (BmkgParser.Weather slot : forecast.slots)
      if (!slot.time.isBefore(current.time)) relevant.add(slot);
    ((TemperatureChartView) findViewById(R.id.weatherChart))
        .setForecast(relevant, forecast.location);
    LinearLayout list = findViewById(R.id.weatherForecastList);
    list.removeAllViews();
    for (int i = 0; i < Math.min(24, relevant.size()); i++)
      list.addView(forecastCard(list, relevant.get(i), i == 0));
    findViewById(R.id.weatherEmptyForecast).setVisibility(View.GONE);
  }

  private View forecastCard(LinearLayout parent, BmkgParser.Weather slot, boolean selected) {
    View card = getLayoutInflater().inflate(R.layout.weather_forecast_item, parent, false);
    card.setBackgroundResource(
        selected ? R.drawable.protector_action_blue : R.drawable.protector_forecast_card);
    int primary = Color.parseColor(selected ? "#FFFFFF" : "#15332F"),
        secondary = Color.parseColor(selected ? "#F0F5FF" : "#526570");
    bindCard(
        card,
        R.id.forecastDate,
        DateTimeFormatter.ofPattern("EEE, dd MMM", locale)
            .withZone(location.zone)
            .format(slot.time),
        secondary);
    bindCard(
        card,
        R.id.forecastHour,
        DateTimeFormatter.ofPattern("HH:mm", locale).withZone(location.zone).format(slot.time),
        primary);
    ((ImageView) card.findViewById(R.id.forecastIcon))
        .setImageResource(WeatherSymbols.forDescription(slot.description));
    bindCard(card, R.id.forecastTemperature, unit(slot.temperature, "°C"), primary);
    bindCard(card, R.id.forecastDescription, available(slot.description), secondary);
    return card;
  }

  private void bindCard(View card, int id, String value, int color) {
    TextView label = card.findViewById(id);
    label.setText(value);
    label.setTextColor(color);
  }

  private String available(String value) {
    return value.isEmpty() ? "—" : value;
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
