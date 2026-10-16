package com.zikra.app.reading;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Native Canvas compass: rosette dial, degree marks, Ka'bah and live direction arrow. */
public class QiblaCompassView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private float heading;
  private boolean aligned;

  public QiblaCompassView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setHeading(float heading, boolean aligned) {
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
    float scale = Math.min(getWidth(), getHeight()) / 328f;
    canvas.save();
    canvas.translate(getWidth() / 2f, getHeight() / 2f);
    canvas.scale(scale, scale);
    canvas.save();
    canvas.rotate(-heading);
    Path rosette = new Path();
    for (int i = 0; i < 40; i++) {
      double angle = Math.toRadians(i * 9 - 90);
      float radius = i % 2 == 0 ? 155f : 138f;
      float x = (float) Math.cos(angle) * radius;
      float y = (float) Math.sin(angle) * radius;
      if (i == 0) rosette.moveTo(x, y);
      else rosette.lineTo(x, y);
    }
    rosette.close();
    color("#659B86");
    canvas.drawPath(rosette, paint);
    color("#FCFDFB");
    canvas.drawCircle(0, 0, 136, paint);
    color("#659B86");
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1.2f);
    canvas.drawCircle(0, 0, 128, paint);
    for (int i = 0; i < 72; i++) {
      canvas.save();
      canvas.rotate(i * 5);
      paint.setStrokeWidth(i % 6 == 0 ? 1.7f : 1f);
      canvas.drawLine(0, -127, 0, i % 6 == 0 ? -115 : -120, paint);
      canvas.restore();
    }
    color("#FCFDFB");
    canvas.drawCircle(0, 0, 109, paint);
    color("#99B9A4");
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1.3f);
    canvas.drawCircle(0, 0, 109, paint);
    color("#659B86");
    paint.setTextSize(15);
    paint.setTextAlign(Paint.Align.CENTER);
    canvas.drawText("N", 0, -84, paint);
    canvas.drawText("E", 91, 5, paint);
    canvas.drawText("S", 0, 96, paint);
    canvas.drawText("W", -91, 5, paint);
    canvas.restore();
    canvas.save();
    canvas.rotate(294f - heading);
    Path pointer = new Path();
    pointer.moveTo(0, -119);
    pointer.lineTo(-10, -89);
    pointer.lineTo(0, -95);
    pointer.lineTo(10, -89);
    pointer.close();
    color(aligned ? "#10B981" : "#659B86");
    canvas.drawPath(pointer, paint);
    paint.setStrokeWidth(2);
    canvas.drawLine(0, -87, 0, -54, paint);
    canvas.restore();
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
