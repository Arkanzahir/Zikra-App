package com.zikra.app.reading;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Native Canvas compass: rosette dial, degree marks, Ka'bah and live direction arrow. */
public class QiblaCompassView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private float heading;
  private float bearing = 294f;

  public void setBearing(float bearing) {
    if(!Float.isFinite(bearing))return;
    this.bearing = CompassMath.normalize(bearing);
    invalidate();
  }

  private boolean aligned;

  public QiblaCompassView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setHeading(float heading, boolean aligned) {
    if(!Float.isFinite(heading))return;
    if (Math.abs(CompassMath.delta(this.heading, heading)) < 0.15f && this.aligned == aligned)
      return;
    this.heading = heading;
    this.aligned = aligned;
    invalidate();
  }

  private void color(String hex) {
    paint.setColor(Color.parseColor(hex));
    paint.setStyle(Paint.Style.FILL);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float scale = Math.min(getWidth(), getHeight()) / 344f;
    canvas.save();
    canvas.translate(getWidth() / 2f, getHeight() / 2f);
    canvas.scale(scale, scale);
    // Fixed top index represents the phone; the dial rotates beneath it.
    color("#E8F2EC");
    canvas.drawCircle(0, 0, 151, paint);
    canvas.save();
    canvas.rotate(-heading);
    Path rosette = new Path();
    for (int i = 0; i < 48; i++) {
      double angle = Math.toRadians(i * 7.5 - 90);
      float radius = i % 2 == 0 ? 151f : 139f;
      float x = (float) Math.cos(angle) * radius;
      float y = (float) Math.sin(angle) * radius;
      if (i == 0) rosette.moveTo(x, y);
      else rosette.lineTo(x, y);
    }
    rosette.close();
    color("#57927E");
    canvas.drawPath(rosette, paint);
    color("#FCFDFB");
    canvas.drawCircle(0, 0, 135, paint);
    color("#8EB4A2");
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1f);
    canvas.drawCircle(0, 0, 130, paint);
    for (int i = 0; i < 72; i++) {
      canvas.save();
      canvas.rotate(i * 5);
      paint.setStrokeWidth(i % 6 == 0 ? 1.8f : 0.9f);
      canvas.drawLine(0, -126, 0, i % 6 == 0 ? -115 : -121, paint);
      canvas.restore();
    }
    color("#DAE8DF");
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1f);
    canvas.drawCircle(0, 0, 106, paint);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    paint.setTextSize(9);
    for (int degrees = 30; degrees < 360; degrees += 30) {
      if (degrees % 90 == 0) continue;
      canvas.save();
      canvas.rotate(degrees);
      color("#8BA497");
      canvas.drawText(String.valueOf(degrees), 0, -96, paint);
      canvas.restore();
    }
    color("#57927E");
    paint.setTextSize(15);
    canvas.drawText("N", 0, -82, paint);
    canvas.drawText("E", 87, 5, paint);
    canvas.drawText("S", 0, 93, paint);
    canvas.drawText("W", -87, 5, paint);
    canvas.restore();
    color("#15332F");
    Path index = new Path();
    index.moveTo(0, -158);
    index.lineTo(-4, -166);
    index.lineTo(4, -166);
    index.close();
    canvas.drawPath(index, paint);
    canvas.save();
    canvas.rotate(bearing - heading);
    Path pointer = new Path();
    pointer.moveTo(0, -118);
    pointer.lineTo(-8, -87);
    pointer.lineTo(0, -94);
    pointer.lineTo(8, -87);
    pointer.close();
    color(aligned ? "#10B981" : "#57927E");
    canvas.drawPath(pointer, paint);
    paint.setStrokeWidth(1.5f);
    canvas.drawLine(0, -85, 0, -57, paint);
    canvas.restore();
    color(aligned ? "#E0F5EA" : "#EFF5F0");
    canvas.drawCircle(0, 0, 45, paint);
    canvas.save();
    canvas.translate(0, -2);
    canvas.scale(0.7f, 0.7f);
    // Three faces retain the prototype's three-dimensional Ka'bah emblem.
    Path top = new Path();
    top.moveTo(-32, -17);
    top.lineTo(0, -31);
    top.lineTo(34, -15);
    top.lineTo(2, 0);
    top.close();
    color("#45544D");
    canvas.drawPath(top, paint);
    Path left = new Path();
    left.moveTo(-32, -17);
    left.lineTo(2, 0);
    left.lineTo(2, 42);
    left.lineTo(-32, 23);
    left.close();
    color("#202D28");
    canvas.drawPath(left, paint);
    Path right = new Path();
    right.moveTo(2, 0);
    right.lineTo(34, -15);
    right.lineTo(34, 25);
    right.lineTo(2, 42);
    right.close();
    color("#354139");
    canvas.drawPath(right, paint);
    Path belt = new Path();
    belt.moveTo(-32, -6);
    belt.lineTo(2, 11);
    belt.lineTo(34, -4);
    belt.lineTo(34, 3);
    belt.lineTo(2, 18);
    belt.lineTo(-32, 1);
    belt.close();
    color("#D8B26E");
    canvas.drawPath(belt, paint);
    Path door = new Path();
    door.moveTo(21, 12);
    door.lineTo(29, 8);
    door.lineTo(29, 25);
    door.lineTo(21, 29);
    door.close();
    color("#D8B26E");
    canvas.drawPath(door, paint);
    canvas.restore();
    canvas.restore();
  }
}
