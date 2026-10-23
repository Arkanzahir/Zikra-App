package com.zikra.app.safety;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.*;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;
import com.zikra.app.R;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/** Interactive OSM with explicit attribution and a bundled offline boundary fallback. */
public final class EarthquakeMapView extends FrameLayout {
  private MapView map;
  private final IndonesiaMapView fallback;
  private final TextView notice;
  private final LinearLayout controls;
  private Marker marker;
  private Double latitude, longitude;
  private boolean resumed, tiles;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable timeout = () -> offline();
  private final Handler events =
      new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
          if (!resumed) return;
          if (message.what == org.osmdroid.tileprovider.MapTileProviderBase.MAPTILE_SUCCESS_ID) {
            tiles = true;
            notice.setVisibility(GONE);
            map.setAlpha(1f);
            handler.removeCallbacks(timeout);
          }
        }
      };

  public EarthquakeMapView(Context c, AttributeSet a) {
    super(c, a);
    setBackgroundResource(R.drawable.protector_reference_outline);
    setClipToOutline(true);
    fallback = new IndonesiaMapView(c);
    addView(fallback, new LayoutParams(-1, -1));
    notice = label(c.getString(R.string.protector_map_loading), 11);
    notice.setBackgroundColor(0xeefaf8f0);
    LayoutParams note = new LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
    note.setMargins(dp(6), dp(6), dp(6), 0);
    addView(notice, note);
    TextView attribution = label(c.getString(R.string.protector_osm_attribution), 10);
    attribution.setBackgroundColor(0xeefaf8f0);
    attribution.setMinHeight(dp(28));
    attribution.setContentDescription(c.getString(R.string.protector_osm_attribution));
    attribution.setOnClickListener(
        v ->
            c.startActivity(
                new Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.openstreetmap.org/copyright"))));
    addView(attribution, new LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START));
    controls = new LinearLayout(c);
    controls.setOrientation(LinearLayout.VERTICAL);
    controls.setBackgroundColor(0xfafaf8f0);
    for (int i = 0; i < 3; i++) {
      final int index = i;
      TextView b = label(i == 0 ? "+" : i == 1 ? "−" : "◎", 22);
      b.setGravity(Gravity.CENTER);
      b.setContentDescription(
          c.getString(
              i == 0
                  ? R.string.protector_zoom_in
                  : i == 1 ? R.string.protector_zoom_out : R.string.protector_map_reset));
      b.setOnClickListener(
          v -> {
            if (map != null) {
              if (index == 0) map.getController().zoomIn();
              else if (index == 1) map.getController().zoomOut();
              else center();
            }
          });
      controls.addView(b, new LinearLayout.LayoutParams(dp(40), dp(40)));
    }
    LayoutParams buttons = new LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL);
    buttons.setMargins(0, 0, dp(6), 0);
    addView(controls, buttons);
    notice.setOnClickListener(
        v -> {
          if (resumed) {
            tiles = false;
            map.setAlpha(1);
            map.setUseDataConnection(true);
            map.invalidate();
            notice.setText(R.string.protector_map_loading);
            handler.postDelayed(timeout, 12000);
          }
        });
  }

  private void ensureMap() {
    if (map != null) return;
    org.osmdroid.config.IConfigurationProvider config = Configuration.getInstance();
    config.setUserAgentValue("Zikra-Android/1.5 (+https://github.com/Arkanzahir/Zikra-App)");
    java.io.File base = new java.io.File(getContext().getCacheDir(), "osm");
    base.mkdirs();
    config.setOsmdroidBasePath(base);
    config.setOsmdroidTileCache(new java.io.File(base, "tiles"));
    config.setTileDownloadThreads((short) 2);
    config.setCacheMapTileOvershoot((short) 0);
    config.setExpirationExtendedDuration(7L * 86400 * 1000);
    map = new MapView(getContext());
    map.setTileSource(TileSourceFactory.MAPNIK);
    map.setMinZoomLevel(3d);
    map.setMaxZoomLevel(12d);
    map.setMultiTouchControls(true);
    map.getZoomController()
        .setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
    map.getTileProvider().getTileRequestCompleteHandlers().add(events);
    addView(map, 1, new LayoutParams(-1, -1));
    map.setOnTouchListener(
        (v, e) -> {
          getParent()
              .requestDisallowInterceptTouchEvent(
                  e.getActionMasked() != MotionEvent.ACTION_UP
                      && e.getActionMasked() != MotionEvent.ACTION_CANCEL);
          return false;
        });
    marker = new Marker(map);
    marker.setAnchor(.5f, 1f);
    marker.setTitle("Episentrum BMKG");
    Bitmap b = Bitmap.createBitmap(dp(28), dp(36), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(b);
    Paint p = new Paint(3);
    canvas.scale(dp(28) / 28f, dp(36) / 36f);
    Path path = new Path();
    path.moveTo(14, 35);
    path.cubicTo(9, 26, 2, 21, 2, 14);
    path.cubicTo(2, -2, 26, -2, 26, 14);
    path.cubicTo(26, 21, 19, 28, 14, 35);
    p.setColor(0xffc38b28);
    canvas.drawPath(path, p);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(1.5f);
    p.setColor(Color.WHITE);
    canvas.drawPath(path, p);
    p.setStyle(Paint.Style.FILL);
    canvas.drawCircle(14, 13, 4, p);
    marker.setIcon(new BitmapDrawable(getResources(), b));
    updateMarker();
    center();
  }

  public void setEpicenter(Double lat, Double lon) {
    boolean valid =
        lat != null
            && lon != null
            && Double.isFinite(lat)
            && Double.isFinite(lon)
            && Math.abs(lat) <= 90
            && Math.abs(lon) <= 180;
    latitude = valid ? lat : null;
    longitude = valid ? lon : null;
    fallback.setEpicenter(latitude, longitude);
    if (map != null) {
      updateMarker();
      center();
    }
  }

  public boolean hasEpicenter() {
    return latitude != null && longitude != null;
  }

  public void setCompact(boolean compact) {
    controls.setVisibility(compact ? GONE : VISIBLE);
  }

  private void updateMarker() {
    map.getOverlays().remove(marker);
    if (latitude != null) {
      marker.setPosition(new GeoPoint(latitude, longitude));
      map.getOverlays().add(marker);
    }
    map.invalidate();
  }

  private void center() {
    map.getController().setZoom(latitude == null ? 4d : 5d);
    map.getController()
        .setCenter(
            new GeoPoint(latitude == null ? -3 : latitude, longitude == null ? 118 : longitude));
  }

  public void resume() {
    ensureMap();
    resumed = true;
    map.onResume();
    map.setUseDataConnection(true);
    if (!tiles) {
      notice.setVisibility(VISIBLE);
      handler.postDelayed(timeout, 12000);
    }
  }

  public void pause() {
    resumed = false;
    handler.removeCallbacksAndMessages(null);
    events.removeCallbacksAndMessages(null);
    if (map != null) {
      map.setUseDataConnection(false);
      map.onPause();
    }
  }

  public void close() {
    pause();
    if (map != null) {
      map.getTileProvider().getTileRequestCompleteHandlers().remove(events);
      map.onDetach();
      map = null;
    }
  }

  private void offline() {
    if (!resumed || tiles) return;
    map.setAlpha(0);
    map.setUseDataConnection(false);
    notice.setText(R.string.protector_map_fallback);
    notice.setVisibility(VISIBLE);
  }

  private TextView label(String s, int size) {
    TextView t = new TextView(getContext());
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(0xff123337);
    t.setPadding(dp(6), dp(4), dp(6), dp(4));
    return t;
  }

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }
}
