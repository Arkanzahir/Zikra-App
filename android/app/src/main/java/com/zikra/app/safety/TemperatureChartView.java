package com.zikra.app.safety;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Dated model forecast; geometry and labels are rebuilt only when data or size changes. */
public final class TemperatureChartView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path line = new Path();
  private final List<BmkgParser.Weather> slots = new ArrayList<>();
  private final float[] xs = new float[6], ys = new float[6], temperatures = new float[6];
  private final String[] hours = new String[6], labels = new String[6];
  private final DateTimeFormatter time =
      DateTimeFormatter.ofPattern("HH:mm", new Locale("id", "ID"))
          .withZone(ZoneId.of("Asia/Jakarta"));

  public TemperatureChartView(Context c, AttributeSet a) {
    super(c, a);
    paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    paint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
    paint.setTextAlign(Paint.Align.CENTER);
  }

  public void setForecast(List<BmkgParser.Weather> forecast) {
    slots.clear();
    for (BmkgParser.Weather slot : forecast) {
      try {
        float t = Float.parseFloat(slot.temperature);
        if (!Float.isFinite(t)) continue;
        int i = slots.size();
        temperatures[i] = t;
        hours[i] = time.format(slot.time);
        labels[i] = slot.temperature + "°";
        slots.add(slot);
      } catch (NumberFormatException ignored) {
      }
      if (slots.size() == (getResources().getConfiguration().fontScale > 1.2f ? 3 : 6)) break;
    }
    StringBuilder description = new StringBuilder("Suhu prakiraan Sukolilo, WIB: ");
    for (int i = 0; i < slots.size(); i++)
      description
          .append(
              java.time.format.DateTimeFormatter.ofPattern("d MMM", new Locale("id", "ID"))
                  .withZone(ZoneId.of("Asia/Jakarta"))
                  .format(slots.get(i).time))
          .append(" ")
          .append(hours[i])
          .append(" ")
          .append(labels[i])
          .append(", ");
    setContentDescription(description.toString());
    rebuild();
    invalidate();
  }

  private float dp(float x) {
    return x * getResources().getDisplayMetrics().density;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    rebuild();
  }

  private void rebuild() {
    line.reset();
    if (slots.isEmpty()) return;
    float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
    for (int i = 0; i < slots.size(); i++) {
      min = Math.min(min, temperatures[i]);
      max = Math.max(max, temperatures[i]);
    }
    float span = Math.max(4, max - min + 2),
        left = dp(21),
        right = getWidth() - dp(21),
        top = dp(23),
        bottom = getHeight() - dp(30);
    for (int i = 0; i < slots.size(); i++) {
      xs[i] =
          slots.size() == 1 ? (left + right) / 2 : left + (right - left) * i / (slots.size() - 1);
      ys[i] = bottom - (temperatures[i] - (min - 1)) / span * (bottom - top);
      if (i == 0) line.moveTo(xs[i], ys[i]);
      else line.lineTo(xs[i], ys[i]);
    }
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    float w = getWidth(), h = getHeight();
    if (slots.isEmpty()) {
      paint.setColor(0xFF5F7F8A);
      c.drawText("Belum ada prakiraan", w / 2, h / 2, paint);
      return;
    }
    float left = dp(21), right = w - dp(21), top = dp(23), bottom = h - dp(30);
    paint.setColor(0x1929626E);
    paint.setStrokeWidth(dp(1));
    for (int i = 0; i < 3; i++)
      c.drawLine(left, top + (bottom - top) * i / 2, right, top + (bottom - top) * i / 2, paint);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(dp(2));
    paint.setColor(0xFF2C6D7D);
    c.drawPath(line, paint);
    paint.setStyle(Paint.Style.FILL);
    for (int i = 0; i < slots.size(); i++) {
      paint.setColor(Color.WHITE);
      c.drawCircle(xs[i], ys[i], dp(4), paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(0xFF2C6D7D);
      paint.setStrokeWidth(dp(1.5f));
      c.drawCircle(xs[i], ys[i], dp(4), paint);
      paint.setStyle(Paint.Style.FILL);
      c.drawText(labels[i], xs[i], ys[i] - dp(10), paint);
      paint.setColor(0xFF5F7F8A);
      c.drawText(hours[i], xs[i], h - dp(7), paint);
    }
  }
}
