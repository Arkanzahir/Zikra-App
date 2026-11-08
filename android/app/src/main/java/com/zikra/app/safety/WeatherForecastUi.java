package com.zikra.app.safety;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.zikra.app.R;
import com.zikra.app.location.LocationPoint;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/** Shared native forecast tiles; no provider calls or persisted user state. */
public final class WeatherForecastUi {
  private static final Locale ID = new Locale("id", "ID");

  private WeatherForecastUi() {}

  public static void hours(
      LinearLayout parent,
      List<BmkgParser.Weather> slots,
      LocationPoint point,
      boolean sky,
      Consumer<BmkgParser.Weather> selected) {
    parent.removeAllViews();
    Context c = parent.getContext();
    int columns = sky ? 5 : 4;
    float font = c.getResources().getConfiguration().fontScale;
    int width =
        Math.max(
            Math.round(58 * Math.max(1, font)),
            (c.getResources().getConfiguration().screenWidthDp - (sky ? 60 : 32)) / columns);
    for (BmkgParser.Weather slot : slots) {
      LinearLayout tile = new LinearLayout(c);
      tile.setOrientation(LinearLayout.VERTICAL);
      tile.setGravity(Gravity.CENTER);
      tile.setPadding(dp(c, 3), dp(c, 10), dp(c, 3), dp(c, 10));
      tile.setLayoutParams(new LinearLayout.LayoutParams(dp(c, width), -2));
      tile.setMinimumHeight(dp(c, 102));
      String date =
          DateTimeFormatter.ofPattern("EEE, d MMM HH:mm", ID)
              .withZone(point.zone)
              .format(slot.time);
      tile.setContentDescription(
          c.getString(
              R.string.weather_hour_description, date, slot.description, slot.temperature + "°C"));
      tile.setFocusable(true);
      tile.setClickable(true);
      TextView time =
          label(
              c,
              DateTimeFormatter.ofPattern("HH:mm", ID).withZone(point.zone).format(slot.time),
              12,
              sky);
      time.setGravity(Gravity.CENTER);
      tile.addView(time);
      ImageView icon = new ImageView(c);
      icon.setImageResource(
          WeatherSymbols.forForecast(
              slot.description, WeatherOutlook.night(slot.time, point.zone), sky));
      icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      LinearLayout.LayoutParams image = new LinearLayout.LayoutParams(dp(c, 30), dp(c, 30));
      image.topMargin = dp(c, 10);
      image.bottomMargin = dp(c, 10);
      tile.addView(icon, image);
      TextView temperature = label(c, slot.temperature + "°", 17, sky);
      temperature.setGravity(Gravity.CENTER);
      temperature.setTypeface(null, Typeface.BOLD);
      tile.addView(temperature);
      tile.setOnClickListener(
          v -> {
            for (int i = 0; i < parent.getChildCount(); i++) {
              View child = parent.getChildAt(i);
              child.setSelected(child == tile);
              child.setBackgroundResource(
                  child == tile
                      ? (sky
                          ? R.drawable.weather_glass_selected
                          : R.drawable.weather_overview_selected)
                      : android.R.color.transparent);
            }
            selected.accept(slot);
          });
      parent.addView(tile);
    }
    if (!slots.isEmpty()) parent.getChildAt(0).performClick();
  }

  public static void days(LinearLayout parent, BmkgParser.Forecast forecast) {
    parent.removeAllViews();
    Context c = parent.getContext();
    List<WeatherOutlook.Day> days = WeatherOutlook.days(forecast);
    double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
    for (WeatherOutlook.Day day : days) {
      minimum = Math.min(minimum, day.minimum);
      maximum = Math.max(maximum, day.maximum);
    }
    boolean large = c.getResources().getConfiguration().fontScale > 1.3f;
    for (WeatherOutlook.Day day : days) {
      LinearLayout row = new LinearLayout(c);
      row.setOrientation(LinearLayout.VERTICAL);
      row.setPadding(0, dp(c, 12), 0, dp(c, 12));
      row.setContentDescription(
          c.getString(
              R.string.weather_range_description,
              day.date.toString(),
              number(day.minimum),
              number(day.maximum),
              day.samples));
      LinearLayout line = new LinearLayout(c);
      line.setGravity(Gravity.CENTER_VERTICAL);
      String name =
          day.date.equals(forecast.current.time.atZone(forecast.location.zone).toLocalDate())
              ? c.getString(R.string.weather_today)
              : day.date.format(DateTimeFormatter.ofPattern("EEE d/M", ID));
      TextView date = label(c, name, 14, true);
      line.addView(date, new LinearLayout.LayoutParams(large ? 0 : dp(c, 66), -2, large ? 1 : 0));
      ImageView icon = new ImageView(c);
      icon.setImageResource(
          WeatherSymbols.forForecast(day.representative.description, false, true));
      icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      line.addView(icon, new LinearLayout.LayoutParams(dp(c, 28), dp(c, 28)));
      row.addView(line);
      LinearLayout range = large ? new LinearLayout(c) : line;
      range.setGravity(Gravity.CENTER_VERTICAL);
      TextView low = label(c, number(day.minimum) + "°", 14, true);
      low.setGravity(Gravity.END);
      range.addView(low, new LinearLayout.LayoutParams(large ? -2 : dp(c, 38), -2));
      TemperatureRangeView bar =
          new TemperatureRangeView(c, day.minimum, day.maximum, minimum, maximum);
      LinearLayout.LayoutParams track = new LinearLayout.LayoutParams(0, dp(c, 28), 1);
      track.leftMargin = dp(c, 8);
      track.rightMargin = dp(c, 8);
      range.addView(bar, track);
      TextView high = label(c, number(day.maximum) + "°", 14, true);
      range.addView(high, new LinearLayout.LayoutParams(-2, -2));
      if (large) row.addView(range);
      if (day.samples < 24)
        row.addView(label(c, c.getString(R.string.weather_partial_day), 12, true));
      parent.addView(row);
    }
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.0f", value);
  }

  private static TextView label(Context c, String text, int size, boolean sky) {
    TextView view = new TextView(c);
    view.setText(text);
    view.setTextSize(size);
    view.setTextColor(c.getColor(sky ? R.color.weather_sky_text : R.color.text));
    view.setFontFeatureSettings("tnum");
    return view;
  }

  private static int dp(Context c, float n) {
    return Math.round(n * c.getResources().getDisplayMetrics().density);
  }
}
