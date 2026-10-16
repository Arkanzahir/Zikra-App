package com.zikra.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.safety.BmkgClient;
import com.zikra.app.safety.BmkgParser;
import com.zikra.app.safety.TemperatureChartView;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Forecast detail inspired by Info BMKG, using actual dated official forecast slots. */
public final class WeatherActivity extends BaseActivity {
  private final Locale locale = new Locale("id", "ID");
  private final ZoneId wib = ZoneId.of("Asia/Jakarta");
  private BmkgClient bmkg;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_weather);
    setupScreen("Cuaca Sukolilo");
    getWindow().setStatusBarColor(Color.WHITE);
    getWindow().setNavigationBarColor(Color.WHITE);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            getWindow().getDecorView().getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    text(R.id.protectorToolbarTitle, "Cuaca");
    text(R.id.protectorToolbarSubtitle, "Info BMKG · Sukolilo, Surabaya");
    bmkg = new BmkgClient(this);
    findViewById(R.id.protectorRefresh).setOnClickListener(v -> refresh());
    findViewById(R.id.weatherSource)
        .setOnClickListener(
            v ->
                external(
                    Intent.ACTION_VIEW,
                    "https://www.bmkg.go.id/cuaca/prakiraan-cuaca/35.78.09.1001"));
    refresh();
  }

  private void refresh() {
    bmkg.reset();
    text(R.id.weatherTemperature, "—°C");
    text(R.id.weatherDescription, "Memuat prakiraan…");
    text(R.id.weatherStatus, "Menghubungkan ke BMKG");
    text(R.id.weatherDate, "Waktu prakiraan belum tersedia");
    text(R.id.weatherHumidity, "—");
    text(R.id.weatherWind, "—");
    text(R.id.weatherDirection, "—");
    ((TemperatureChartView) findViewById(R.id.weatherChart)).setForecast(Collections.emptyList());
    ((LinearLayout) findViewById(R.id.weatherForecastList)).removeAllViews();
    findViewById(R.id.weatherEmptyForecast).setVisibility(View.VISIBLE);
    text(R.id.weatherEmptyForecast, "Prakiraan akan muncul saat data BMKG tersedia.");
    bmkg.weather(
        new BmkgClient.Result<BmkgParser.Forecast>() {
          @Override
          public void success(BmkgParser.Forecast value) {
            show(value);
          }

          @Override
          public void failure(String message) {
            text(R.id.weatherDescription, "Belum ada data cuaca");
            text(R.id.weatherStatus, "Ketuk ↻ untuk mencoba lagi");
            text(R.id.weatherEmptyForecast, message);
          }
        });
  }

  private void show(BmkgParser.Forecast forecast) {
    BmkgParser.Weather current = forecast.current;
    text(R.id.weatherLocation, current.village + ", " + current.district);
    text(
        R.id.weatherDate,
        DateTimeFormatter.ofPattern("EEEE, d MMM · HH:mm 'WIB'", locale)
            .withZone(wib)
            .format(current.time));
    text(
        R.id.weatherTemperature,
        current.temperature.isEmpty() ? "—°C" : current.temperature + "°C");
    text(R.id.weatherDescription, available(current.description));
    text(
        R.id.weatherStatus,
        current.expired
            ? "Prakiraan terakhir sudah lewat · muat ulang"
            : "Prakiraan BMKG · " + current.city);
    text(R.id.weatherHumidity, unit(current.humidity, "%"));
    text(R.id.weatherWind, available(current.wind));
    text(R.id.weatherDirection, available(current.windDirection));
    List<BmkgParser.Weather> relevant = new ArrayList<>();
    for (BmkgParser.Weather slot : forecast.slots)
      if (!slot.time.isBefore(current.time)) relevant.add(slot);
    ((TemperatureChartView) findViewById(R.id.weatherChart)).setForecast(relevant);
    LinearLayout list = findViewById(R.id.weatherForecastList);
    list.removeAllViews();
    for (int i = 0; i < Math.min(24, relevant.size()); i++)
      list.addView(forecastCard(relevant.get(i), i == 0));
    findViewById(R.id.weatherEmptyForecast).setVisibility(View.GONE);
  }

  private View forecastCard(BmkgParser.Weather slot, boolean selected) {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setGravity(android.view.Gravity.CENTER);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(88), dp(163));
    params.setMarginEnd(dp(9));
    card.setLayoutParams(params);
    card.setPadding(dp(7), dp(13), dp(7), dp(12));
    card.setBackgroundResource(
        selected ? R.drawable.protector_action_blue : R.drawable.protector_forecast_card);
    int primary = Color.parseColor(selected ? "#FFFFFF" : "#254363"),
        secondary = Color.parseColor(selected ? "#DCE7FF" : "#8290A4");
    TextView date =
        label(
            DateTimeFormatter.ofPattern("EEE, dd MMM", locale).withZone(wib).format(slot.time),
            10,
            secondary);
    card.addView(date);
    TextView hour =
        label(
            DateTimeFormatter.ofPattern("HH:mm", locale).withZone(wib).format(slot.time),
            12,
            primary);
    hour.setTypeface(null, android.graphics.Typeface.BOLD);
    card.addView(hour);
    ImageView icon = new ImageView(this);
    String desc = slot.description.toLowerCase(locale);
    icon.setImageResource(
        desc.contains("cerah") && !desc.contains("berawan")
            ? R.drawable.ic_sun
            : R.drawable.weather_ic_cloud);
    icon.setColorFilter(primary);
    LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(27), dp(27));
    ip.topMargin = dp(12);
    ip.bottomMargin = dp(8);
    card.addView(icon, ip);
    icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    TextView temp = label(unit(slot.temperature, "°C"), 16, primary);
    temp.setTypeface(null, android.graphics.Typeface.BOLD);
    card.addView(temp);
    TextView description = label(available(slot.description), 9, secondary);
    description.setMaxLines(2);
    description.setGravity(android.view.Gravity.CENTER);
    description.setPadding(0, dp(5), 0, 0);
    card.addView(description);
    return card;
  }

  private TextView label(String value, int size, int color) {
    TextView label = new TextView(this);
    label.setText(value);
    label.setTextSize(size);
    label.setTextColor(color);
    label.setGravity(android.view.Gravity.CENTER);
    label.setFontFeatureSettings("tnum");
    return label;
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
