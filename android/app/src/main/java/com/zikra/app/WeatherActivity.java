package com.zikra.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.zikra.app.location.*;
import com.zikra.app.safety.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Native forecast detail; provider timestamps, location and cache provenance stay visible. */
public final class WeatherActivity extends BaseActivity {
  private static final Locale ID = new Locale("id", "ID");
  private LocationPoint location = LocationPoint.INITIAL;
  private BmkgClient client;
  private BmkgParser.Forecast displayed;
  private WeatherSkyView sky;
  private boolean graphExpanded;
  private final android.os.Handler timer =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private final Runnable tick =
      new Runnable() {
        @Override
        public void run() {
          refresh();
          timer.postDelayed(this, OpenMeteoClient.FRESH_MS);
        }
      };

  @Override
  protected void onCreate(Bundle saved) {
    setTheme(R.style.Theme_Zikra_Protector);
    super.onCreate(saved);
    setContentView(R.layout.weather_glass);
    setupScreen(getString(R.string.weather_title));
    sky = findViewById(R.id.weatherSky);
    client = new BmkgClient(this);
    graphExpanded = saved != null && saved.getBoolean("weather_graph");
    ((TemperatureChartView) findViewById(R.id.weatherChart)).setOnSky(true);
    if (getResources().getConfiguration().fontScale > 1.3f) {
      LinearLayout metrics = findViewById(R.id.weatherMetrics);
      metrics.setOrientation(LinearLayout.VERTICAL);
      for (int i = 0; i < metrics.getChildCount(); i++) {
        LinearLayout.LayoutParams lp =
            (LinearLayout.LayoutParams) metrics.getChildAt(i).getLayoutParams();
        lp.width = -1;
        lp.weight = 0;
        lp.leftMargin = 0;
        lp.rightMargin = 0;
      }
      findViewById(R.id.weatherChart).getLayoutParams().height = dp(240);
    }
    ProtectorNavigation.bind(this);
    findViewById(R.id.weatherLocation).setOnClickListener(v -> open(LocationActivity.class));
    findViewById(R.id.protectorRefresh).setOnClickListener(v -> refresh());
    findViewById(R.id.weatherRetry).setOnClickListener(v -> refresh());
    findViewById(R.id.weatherSource)
        .setOnClickListener(v -> external(Intent.ACTION_VIEW, "https://open-meteo.com/"));
    findViewById(R.id.weatherGraphToggle)
        .setOnClickListener(
            v -> {
              graphExpanded = !graphExpanded;
              graph();
            });
    graph();
  }

  @Override
  protected void onResume() {
    super.onResume();
    LocationPoint selected = LocationStore.current(this);
    if (!selected.key().equals(location.key())) displayed = null;
    location = selected;
    text(R.id.protectorToolbarTitle, location.label);
    text(R.id.weatherLocation, location.caption());
    sky.setWeather(
        displayed == null ? null : displayed.current.description,
        WeatherOutlook.night(Instant.now(), location.zone));
    getWindow().setStatusBarColor(sky.topColor());
    getWindow().setNavigationBarColor(getColor(R.color.app_surface));
    timer.removeCallbacks(tick);
    timer.post(tick);
  }

  @Override
  protected void onPause() {
    timer.removeCallbacks(tick);
    if (client != null) client.reset();
    super.onPause();
  }

  private void refresh() {
    client.reset();
    if (displayed != null
        && !WeatherCode.usableCache(
            displayed.fetchedAt.toEpochMilli(),
            System.currentTimeMillis(),
            OpenMeteoClient.MAX_AGE_MS)) displayed = null;
    if (displayed == null) clear();
    client.weather(
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

  private void clear() {
    text(R.id.weatherTemperature, "—°");
    text(R.id.weatherDescription, getString(R.string.weather_loading));
    text(R.id.weatherDate, "");
    text(R.id.weatherHumidity, "—");
    text(R.id.weatherWind, "—");
    text(R.id.weatherDirection, "—");
    text(R.id.weatherStatus, getString(R.string.weather_loading));
    text(R.id.weatherForecastNote, getString(R.string.weather_loading));
    text(R.id.weatherSelectedHour, "");
    ((LinearLayout) findViewById(R.id.weatherForecastList)).removeAllViews();
    ((LinearLayout) findViewById(R.id.weatherDailyList)).removeAllViews();
    ((TemperatureChartView) findViewById(R.id.weatherChart))
        .setForecast(Collections.emptyList(), location);
    findViewById(R.id.weatherDailyPanel).setVisibility(View.GONE);
    findViewById(R.id.weatherGraphPanel).setVisibility(View.GONE);
    findViewById(R.id.weatherEmptyForecast).setVisibility(View.VISIBLE);
    findViewById(R.id.weatherRetry).setVisibility(View.GONE);
    sky.setWeather(null, WeatherOutlook.night(Instant.now(), location.zone));
    getWindow().setStatusBarColor(sky.topColor());
  }

  private void showFailure(String message) {
    if (displayed == null) {
      clear();
      text(R.id.weatherDescription, getString(R.string.weather_missing));
      text(R.id.weatherForecastNote, message);
      text(R.id.weatherStatus, message);
    } else
      text(
          R.id.weatherStatus,
          OpenMeteoClient.status(displayed) + "\n" + getString(R.string.weather_update_failed));
    findViewById(R.id.weatherRetry).setVisibility(View.VISIBLE);
  }

  private void show(BmkgParser.Forecast forecast) {
    displayed = forecast;
    location = forecast.location;
    BmkgParser.Weather current = forecast.current;
    text(R.id.protectorToolbarTitle, location.label);
    text(R.id.weatherLocation, location.caption());
    text(R.id.weatherTemperature, current.temperature + "°");
    text(R.id.weatherDescription, current.description);
    text(
        R.id.weatherDate,
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)
            .withZone(location.zone)
            .format(current.time));
    text(
        R.id.weatherForecastNote,
        getString(
            R.string.weather_slot_note,
            DateTimeFormatter.ofPattern("d MMM HH:mm", ID)
                .withZone(location.zone)
                .format(current.time),
            location.zoneLabel(current.time)));
    text(R.id.weatherStatus, OpenMeteoClient.status(forecast));
    text(R.id.weatherHumidity, unit(current.humidity, "%"));
    text(R.id.weatherWind, unit(current.wind, " km/jam"));
    text(R.id.weatherDirection, getString(R.string.weather_wind, current.windDirection));
    sky.setWeather(current.description, WeatherOutlook.night(Instant.now(), location.zone));
    getWindow().setStatusBarColor(sky.topColor());
    List<BmkgParser.Weather> hours = new ArrayList<>();
    hours.add(current);
    hours.addAll(WeatherOutlook.upcoming(forecast, 4));
    WeatherForecastUi.hours(
        findViewById(R.id.weatherForecastList),
        hours,
        location,
        true,
        slot ->
            text(
                R.id.weatherSelectedHour,
                getString(
                    R.string.weather_hour_description,
                    DateTimeFormatter.ofPattern("EEE d/M HH:mm", ID)
                        .withZone(location.zone)
                        .format(slot.time),
                    slot.description,
                    location.zoneLabel(slot.time))));
    WeatherForecastUi.days(findViewById(R.id.weatherDailyList), forecast);
    findViewById(R.id.weatherDailyPanel)
        .setVisibility(WeatherOutlook.days(forecast).isEmpty() ? View.GONE : View.VISIBLE);
    ((TemperatureChartView) findViewById(R.id.weatherChart)).setForecast(hours, location);
    findViewById(R.id.weatherGraphPanel).setVisibility(View.VISIBLE);
    findViewById(R.id.weatherEmptyForecast).setVisibility(View.GONE);
    findViewById(R.id.weatherRetry).setVisibility(View.GONE);
    graph();
  }

  private void graph() {
    findViewById(R.id.weatherChart).setVisibility(graphExpanded ? View.VISIBLE : View.GONE);
    ((TextView) findViewById(R.id.weatherGraphToggle))
        .setText(graphExpanded ? R.string.weather_graph_close : R.string.weather_graph);
    findViewById(R.id.weatherGraphToggle).setSelected(graphExpanded);
  }

  private String unit(String value, String suffix) {
    return value.isEmpty() ? "—" : value + suffix;
  }

  private void text(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putBoolean("weather_graph", graphExpanded);
    super.onSaveInstanceState(out);
  }

  @Override
  protected void onDestroy() {
    if (client != null) client.close();
    super.onDestroy();
  }
}
