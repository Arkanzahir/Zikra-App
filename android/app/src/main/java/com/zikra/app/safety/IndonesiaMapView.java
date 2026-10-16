package com.zikra.app.safety;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.zikra.app.R;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Offline native map using public-domain Natural Earth country boundaries. Coordinates use a
 * plate-carree projection, in longitude/latitude order. There is no placeholder earthquake: a
 * marker exists only after setEpicenter().
 */
public final class IndonesiaMapView extends View {
  private static final double WEST = 94.0;
  private static final double EAST = 142.0;
  private static final double NORTH = 8.0;
  private static final double SOUTH = -13.0;
  private static final String TAG = "IndonesiaMapView";
  private static List<LandGeometry> cachedGeometry;

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final List<LandPath> landPaths = new ArrayList<>();
  private final Path roundedClip = new Path();
  private final RectF viewBounds = new RectF();
  private final float density;
  private float projectionScale;
  private float mapLeft;
  private float mapTop;
  private LinearGradient seaGradient;
  private Double epicenterLatitude;
  private Double epicenterLongitude;
  private boolean geometryAvailable;

  public IndonesiaMapView(Context context) {
    this(context, null);
  }

  public IndonesiaMapView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public IndonesiaMapView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    density = getResources().getDisplayMetrics().density;
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    loadGeometry(context);
    updateDescription();
  }

  /** Pass null to clear the marker while loading or after a failed BMKG request. */
  public void setEpicenter(Double latitude, Double longitude) {
    if (latitude == null
        || longitude == null
        || !Double.isFinite(latitude)
        || !Double.isFinite(longitude)
        || Math.abs(latitude) > 90
        || Math.abs(longitude) > 180) {
      epicenterLatitude = null;
      epicenterLongitude = null;
    } else {
      epicenterLatitude = latitude;
      epicenterLongitude = longitude;
    }
    updateDescription();
    invalidate();
  }

  /** True only when a valid source coordinate has been supplied. */
  public boolean hasEpicenter() {
    return epicenterLatitude != null && epicenterLongitude != null;
  }

  private boolean epicenterInView() {
    return hasEpicenter()
        && epicenterLongitude >= WEST
        && epicenterLongitude <= EAST
        && epicenterLatitude >= SOUTH
        && epicenterLatitude <= NORTH;
  }

  private void updateDescription() {
    if (!hasEpicenter()) {
      setContentDescription("Peta Indonesia. Titik gempa menunggu data BMKG.");
    } else {
      setContentDescription(
          String.format(
              new Locale("id", "ID"),
              "Peta Indonesia. Pusat gempa BMKG %.2f derajat lintang, %.2f derajat bujur.%s",
              epicenterLatitude,
              epicenterLongitude,
              epicenterInView() ? "" : " Lokasi berada di luar cakupan peta."));
    }
  }

  private static synchronized List<LandGeometry> loadGeometry(Context context) {
    if (cachedGeometry != null) return cachedGeometry;
    List<LandGeometry> result = new ArrayList<>();
    try (InputStream input = context.getResources().openRawResource(R.raw.protector_indonesia)) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int count;
      while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
      JSONObject collection =
          new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
      JSONArray features = collection.getJSONArray("features");
      for (int i = 0; i < features.length(); i++) {
        JSONObject feature = features.getJSONObject(i);
        boolean indonesia = feature.getJSONObject("properties").optBoolean("is_indonesia", false);
        JSONArray polygons = feature.getJSONObject("geometry").getJSONArray("coordinates");
        for (int p = 0; p < polygons.length(); p++) {
          JSONArray rings = polygons.getJSONArray(p);
          List<float[]> parsedRings = new ArrayList<>();
          for (int r = 0; r < rings.length(); r++) {
            JSONArray coordinates = rings.getJSONArray(r);
            float[] points = new float[coordinates.length() * 2];
            for (int c = 0; c < coordinates.length(); c++) {
              JSONArray coordinate = coordinates.getJSONArray(c);
              points[c * 2] = (float) coordinate.getDouble(0);
              points[c * 2 + 1] = (float) coordinate.getDouble(1);
            }
            if (points.length >= 6) parsedRings.add(points);
          }
          if (!parsedRings.isEmpty()) result.add(new LandGeometry(indonesia, parsedRings));
        }
      }
    } catch (IOException | JSONException error) {
      Log.e(TAG, "Unable to read bundled map boundaries", error);
    }
    cachedGeometry = Collections.unmodifiableList(result);
    return cachedGeometry;
  }

  @Override
  protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    viewBounds.set(0, 0, width, height);
    roundedClip.reset();
    roundedClip.addRoundRect(viewBounds, dp(14), dp(14), Path.Direction.CW);
    seaGradient =
        new LinearGradient(
            0,
            0,
            width,
            height,
            Color.rgb(171, 207, 247),
            Color.rgb(199, 224, 250),
            Shader.TileMode.CLAMP);
    projectionScale = (float) Math.min(width / (EAST - WEST), height / (NORTH - SOUTH));
    mapLeft = (width - (float) (EAST - WEST) * projectionScale) * .5f;
    mapTop = (height - (float) (NORTH - SOUTH) * projectionScale) * .5f;
    landPaths.clear();
    List<LandGeometry> geometry = loadGeometry(getContext());
    geometryAvailable = !geometry.isEmpty();
    for (LandGeometry country : geometry) {
      Path path = new Path();
      path.setFillType(Path.FillType.EVEN_ODD);
      for (float[] ring : country.rings) {
        path.moveTo(projectX(ring[0]), projectY(ring[1]));
        for (int i = 2; i < ring.length; i += 2) {
          path.lineTo(projectX(ring[i]), projectY(ring[i + 1]));
        }
        path.close();
      }
      landPaths.add(new LandPath(country.indonesia, path));
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int save = canvas.save();
    canvas.clipPath(roundedClip);
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(seaGradient);
    canvas.drawRect(viewBounds, paint);
    paint.setShader(null);

    paint.setColor(Color.argb(65, 255, 255, 255));
    paint.setStrokeWidth(dp(.65f));
    for (int longitude = 95; longitude <= 140; longitude += 5) {
      canvas.drawLine(projectX(longitude), 0, projectX(longitude), getHeight(), paint);
    }
    for (int latitude = -10; latitude <= 5; latitude += 5) {
      canvas.drawLine(0, projectY(latitude), getWidth(), projectY(latitude), paint);
    }

    for (LandPath land : landPaths) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(land.indonesia ? Color.rgb(169, 219, 181) : Color.rgb(194, 221, 186));
      canvas.drawPath(land.path, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(dp(.6f));
      paint.setColor(Color.rgb(128, 181, 157));
      canvas.drawPath(land.path, paint);
    }
    paint.setStyle(Paint.Style.FILL);

    if (geometryAvailable) {
      label(canvas, "SUMATRA", 101.5, -2.2, 8, false);
      label(canvas, "JAWA", 111.0, -8.8, 8, false);
      label(canvas, "KALIMANTAN", 114.3, .8, 7, false);
      label(canvas, "SULAWESI", 122.1, -3.8, 7, false);
      label(canvas, "PAPUA", 137.0, -4.5, 8, false);
      label(canvas, "Laut Jawa", 111.0, -4.0, 8, true);
      label(canvas, "Laut Banda", 129.5, -6.3, 8, true);
    } else {
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTextSize(dp(12));
      paint.setColor(Color.rgb(51, 85, 112));
      canvas.drawText("Peta belum tersedia", getWidth() * .5f, getHeight() * .5f, paint);
    }

    if (epicenterInView()) {
      float x = projectX(epicenterLongitude);
      float y = projectY(epicenterLatitude);
      paint.setColor(Color.argb(40, 235, 91, 93));
      canvas.drawCircle(x, y, dp(13), paint);
      paint.setColor(Color.WHITE);
      canvas.drawCircle(x, y, dp(6.8f), paint);
      paint.setColor(Color.rgb(235, 91, 93));
      canvas.drawCircle(x, y, dp(5.2f), paint);
      paint.setColor(Color.WHITE);
      canvas.drawCircle(x, y, dp(1.7f), paint);
    }

    paint.setTextAlign(Paint.Align.RIGHT);
    paint.setTypeface(Typeface.DEFAULT);
    paint.setTextSize(dp(7));
    paint.setColor(Color.rgb(64, 103, 131));
    canvas.drawText("Natural Earth", getWidth() - dp(10), getHeight() - dp(7), paint);
    canvas.restoreToCount(save);
  }

  private void label(
      Canvas canvas,
      String text,
      double longitude,
      double latitude,
      float textSize,
      boolean water) {
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(dp(textSize));
    paint.setTypeface(water ? Typeface.create("sans-serif", Typeface.ITALIC) : Typeface.DEFAULT);
    paint.setColor(water ? Color.rgb(83, 126, 164) : Color.rgb(76, 118, 97));
    canvas.drawText(text, projectX(longitude), projectY(latitude), paint);
  }

  private float projectX(double longitude) {
    return mapLeft + (float) (longitude - WEST) * projectionScale;
  }

  private float projectY(double latitude) {
    return mapTop + (float) (NORTH - latitude) * projectionScale;
  }

  private float dp(float value) {
    return density * value;
  }

  private static final class LandGeometry {
    final boolean indonesia;
    final List<float[]> rings;

    LandGeometry(boolean indonesia, List<float[]> rings) {
      this.indonesia = indonesia;
      this.rings = rings;
    }
  }

  private static final class LandPath {
    final boolean indonesia;
    final Path path;

    LandPath(boolean indonesia, Path path) {
      this.indonesia = indonesia;
      this.path = path;
    }
  }
}
