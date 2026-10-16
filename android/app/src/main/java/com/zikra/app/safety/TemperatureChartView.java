package com.zikra.app.safety;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Native line chart of real, dated BMKG forecast slots. Empty data never produces a dummy curve.
 */
public final class TemperatureChartView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final List<BmkgParser.Weather> slots = new ArrayList<>();
  private final DateTimeFormatter time =
      DateTimeFormatter.ofPattern("HH:mm", new Locale("id", "ID"))
          .withZone(ZoneId.of("Asia/Jakarta"));

  public TemperatureChartView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setForecast(List<BmkgParser.Weather> forecast) {
    slots.clear();
    for (BmkgParser.Weather slot : forecast) {
      try {
        if (Float.isFinite(Float.parseFloat(slot.temperature))) slots.add(slot);
      } catch (NumberFormatException ignored) {
      }
      if (slots.size() == 6) break;
    }
    StringBuilder description = new StringBuilder("Suhu prakiraan BMKG: ");
    for (BmkgParser.Weather slot : slots)
      description
          .append(time.format(slot.time))
          .append(" ")
          .append(slot.temperature)
          .append(" derajat, ");
    setContentDescription(description.toString());
    invalidate();
  }

  private float dp(float n) {
    return n * getResources().getDisplayMetrics().density;
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    float w = getWidth(), h = getHeight();
    paint.setTypeface(
        android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
    paint.setTextSize(dp(10));
    paint.setTextAlign(Paint.Align.CENTER);
    if (slots.isEmpty()) {
      paint.setColor(Color.rgb(95, 127, 138));
      c.drawText("Grafik tersedia setelah data BMKG dimuat", w / 2, h / 2, paint);
      return;
    }
    float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
    for (BmkgParser.Weather slot : slots) {
      float t = Float.parseFloat(slot.temperature);
      min = Math.min(min, t);
      max = Math.max(max, t);
    }
    float span = Math.max(4, max - min + 2),
        left = dp(21),
        right = w - dp(21),
        top = dp(23),
        bottom = h - dp(30);
    paint.setColor(Color.argb(25, 41, 98, 110));
    paint.setStrokeWidth(dp(1));
    for (int i = 0; i < 3; i++)
      c.drawLine(left, top + (bottom - top) * i / 2, right, top + (bottom - top) * i / 2, paint);
    Path line = new Path();
    List<Float> xs = new ArrayList<>(), ys = new ArrayList<>();
    for (int i = 0; i < slots.size(); i++) {
      float x =
          slots.size() == 1 ? (left + right) / 2 : left + (right - left) * i / (slots.size() - 1);
      float y =
          bottom - (Float.parseFloat(slots.get(i).temperature) - (min - 1)) / span * (bottom - top);
      xs.add(x);
      ys.add(y);
      if (i == 0) line.moveTo(x, y);
      else line.lineTo(x, y);
    }
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(dp(2));
    paint.setColor(Color.rgb(44, 109, 125));
    c.drawPath(line, paint);
    paint.setStyle(Paint.Style.FILL);
    for (int i = 0; i < slots.size(); i++) {
      paint.setColor(Color.WHITE);
      c.drawCircle(xs.get(i), ys.get(i), dp(4), paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(Color.rgb(44, 109, 125));
      paint.setStrokeWidth(dp(1.5f));
      c.drawCircle(xs.get(i), ys.get(i), dp(4), paint);
      paint.setStyle(Paint.Style.FILL);
      c.drawText(slots.get(i).temperature + "°", xs.get(i), ys.get(i) - dp(10), paint);
      paint.setColor(Color.rgb(95, 127, 138));
      c.drawText(time.format(slots.get(i).time), xs.get(i), h - dp(7), paint);
    }
  }
}
