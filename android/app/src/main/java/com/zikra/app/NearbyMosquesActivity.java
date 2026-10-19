package com.zikra.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.mosque.MosquePlace;
import com.zikra.app.mosque.MosqueSearch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/** Native OpenStreetMap list/map; turn-by-turn routing opens the navigation app. */
public final class NearbyMosquesActivity extends BaseActivity {
  private final ExecutorService work = Executors.newSingleThreadExecutor();
  private MosqueSearch search;
  private LocationPoint location;
  private volatile int generation;
  private long lastRequest = -30000;
  private long nextAllowedSearch;
  private volatile boolean resumed;
  private boolean searching, restoreResults, restoreMap;
  private MapView mapView;
  private final List<MosquePlace> places = new ArrayList<>();
  private AlertDialog sourceDialog;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    restoreResults = state != null && state.getBoolean("searched", false);
    restoreMap = state != null && state.getBoolean("map_open", false);
    setContentView(R.layout.activity_nearby_mosques);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.mosquesHeader))
        .bind(
            R.id.toolbarBack,
            R.id.featureTitle,
            R.id.featureSubtitle,
            0,
            "Masjid terdekat",
            "Tempat singgah di sekitarmu",
            0,
            null);
    setupScreen("Masjid terdekat");
    getWindow().setStatusBarColor(getColor(R.color.onboarding_paper));
    getWindow().setNavigationBarColor(getColor(R.color.onboarding_paper));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    search = new MosqueSearch(this);
    location = LocationStore.current(this);
    findViewById(R.id.mosquesLocation).setOnClickListener(v -> open(LocationActivity.class));
    findViewById(R.id.mosquesProvider).setOnClickListener(v -> chooseProvider());
    findViewById(R.id.mosquesRefresh).setOnClickListener(v -> load(lastRequest >= 0));
    findViewById(R.id.mosquesMapToggle).setOnClickListener(v -> toggleMap());
    findViewById(R.id.mosquesExternal)
        .setOnClickListener(
            v ->
                external(
                    Intent.ACTION_VIEW,
                    "https://www.google.com/maps/search/?api=1&query="
                        + android.net.Uri.encode(
                            "masjid near " + location.latitude + "," + location.longitude)));
    findViewById(R.id.mosquesAttribution).setOnClickListener(v -> chooseProvider());
    labels();
  }

  private void labels() {
    text(R.id.mosquesLocation, location.caption() + " · Ubah");
    text(R.id.mosquesProvider, "Tentang data OpenStreetMap");
    text(R.id.mosquesAttribution, "© OpenStreetMap contributors · ODbL ↗");
    if (location.source.equals("initial"))
      text(R.id.mosquesStatus, "Lokasi masih acuan awal. Pilih lokasi agar hasil sesuai tempatmu.");
    else text(R.id.mosquesStatus, "Pilih Cari masjid untuk mencari dari lokasi ini.");
  }

  private void chooseProvider() {
    sourceDialog =
        new AlertDialog.Builder(this)
            .setTitle("Data OpenStreetMap")
            .setMessage(
                "Data komunitas OpenStreetMap, dengan pencarian melalui Overpass. Kelengkapan tiap"
                    + " daerah berbeda dan layanan publik dapat dibatasi. Hasil diurutkan menurut"
                    + " jarak garis lurus; fasilitas dan jam jamaah perlu dikonfirmasi ke masjid."
                    + " Peta dimuat hanya saat dibuka.")
            .setPositiveButton("Mengerti", null)
            .setNeutralButton(
                "Sumber",
                (d, w) -> external(Intent.ACTION_VIEW, "https://www.openstreetmap.org/copyright"))
            .show();
  }

  private void load(boolean force) {
    long now = SystemClock.elapsedRealtime();
    if (now < nextAllowedSearch) {
      text(
          R.id.mosquesStatus,
          "Tunggu "
              + ((nextAllowedSearch - now + 999) / 1000)
              + " detik sebelum mencari ulang. Hasil yang sudah ada tetap bisa dibuka.");
      return;
    }
    lastRequest = now;
    nextAllowedSearch = now + 30000;
    search.cancel();
    int token = ++generation;
    LocationPoint point = location;
    searching = true;
    ((Button) findViewById(R.id.mosquesRefresh)).setEnabled(false);
    text(R.id.mosquesStatus, "Mencari tempat ibadah di sekitar lokasi pilihan…");
    work.execute(
        () -> {
          if (!current(token)) return;
          try {
            MosqueSearch.Result response = search.find(point, force);
            List<MosquePlace> found = response.places;
            runOnUiThread(
                () -> {
                  if (!current(token)) return;
                  searching = false;
                  places.clear();
                  places.addAll(found);
                  render();
                  ((Button) findViewById(R.id.mosquesRefresh)).setEnabled(true);
                  text(
                      R.id.mosquesStatus,
                      found.isEmpty()
                          ? "Belum ada hasil dalam radius 3 km. Coba lokasi lain atau periksa"
                              + " Google Maps."
                          : found.size()
                              + " tempat dalam radius 3 km · jarak garis lurus. "
                              + (response.cached ? "Data tersimpan" : "OpenStreetMap")
                              + " · "
                              + java.time.format.DateTimeFormatter.ofPattern(
                                      "d MMM HH:mm", Locale.forLanguageTag("id-ID"))
                                  .withZone(point.zone)
                                  .format(java.time.Instant.ofEpochMilli(response.fetchedAt))
                              + " "
                              + point.zoneLabel(
                                  java.time.Instant.ofEpochMilli(response.fetchedAt)));
                  text(R.id.mosquesRefresh, "Cari ulang");
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (!current(token)) return;
                  searching = false;
                  if (e instanceof MosqueSearch.RateLimited)
                    nextAllowedSearch =
                        SystemClock.elapsedRealtime() + ((MosqueSearch.RateLimited) e).retryAfterMs;
                  ((Button) findViewById(R.id.mosquesRefresh)).setEnabled(true);
                  text(
                      R.id.mosquesStatus,
                      "Pencarian belum tersedia. Periksa koneksi dan coba lagi nanti."
                          + (places.isEmpty() ? "" : " Hasil sebelumnya tetap ditampilkan."));
                });
          }
        });
  }

  private boolean current(int token) {
    return resumed && token == generation && !isFinishing() && !isDestroyed();
  }

  private void text(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }

  private void render() {
    LinearLayout list = findViewById(R.id.mosquesResults);
    list.removeAllViews();
    for (MosquePlace place : places) {
      LinearLayout card = new LinearLayout(this);
      card.setOrientation(LinearLayout.VERTICAL);
      card.setPadding(dp(16), dp(16), dp(16), dp(12));
      card.setBackgroundResource(R.drawable.prayer_bg_row);
      TextView name = new TextView(this);
      name.setText(place.name);
      name.setTextSize(18);
      name.setTextColor(getColor(R.color.pine));
      name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
      card.addView(name);
      TextView details = new TextView(this);
      String distance =
          place.distance < 1000
              ? Math.round(place.distance) + " m"
              : String.format(Locale.forLanguageTag("id-ID"), "%.1f km", place.distance / 1000);
      details.setText(
          distance
              + " · garis lurus\n"
              + (place.address.isEmpty() ? "Alamat belum tercantum" : place.address));
      details.setTextColor(getColor(R.color.onboarding_muted));
      details.setTextSize(14);
      details.setPadding(0, dp(8), 0, dp(8));
      details.setLineSpacing(dp(4), 1);
      card.addView(details);
      Button route = new Button(this);
      route.setText("Petunjuk arah ↗");
      route.setAllCaps(false);
      route.setMinHeight(dp(48));
      route.setTextColor(getColor(R.color.pine));
      route.setOnClickListener(v -> directions(place));
      card.addView(route);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(12);
      list.addView(card, lp);
    }
    updateMap();
  }

  private void directions(MosquePlace place) {
    android.net.Uri.Builder uri =
        android.net.Uri.parse("https://www.google.com/maps/dir/")
            .buildUpon()
            .appendQueryParameter("api", "1")
            .appendQueryParameter("destination", place.latitude + "," + place.longitude);
    external(Intent.ACTION_VIEW, uri.build().toString());
  }

  private void toggleMap() {
    FrameLayout container = findViewById(R.id.mosquesMap);
    if (container.getVisibility() == View.VISIBLE) {
      container.setVisibility(View.GONE);
      text(R.id.mosquesMapToggle, "Lihat peta");
      if (mapView != null) {
        mapView.setUseDataConnection(false);
        mapView.onPause();
      }
      return;
    }
    container.setVisibility(View.VISIBLE);
    text(R.id.mosquesMapToggle, "Tutup peta");
    text(
        R.id.mosquesMapStatus,
        "Peta OpenStreetMap · hasil pencarian di lokasi pilihan. Peta memerlukan koneksi; daftar"
            + " tetap bisa dipakai jika petanya belum termuat.");
    findViewById(R.id.mosquesMapStatus).setVisibility(View.VISIBLE);
    if (mapView == null) {
      org.osmdroid.config.IConfigurationProvider config = Configuration.getInstance();
      config.setUserAgentValue("Zikra-Android/1.0 (+https://github.com/Arkanzahir/Zikra-App)");
      java.io.File base = new java.io.File(getCacheDir(), "osm");
      base.mkdirs();
      config.setOsmdroidBasePath(base);
      config.setOsmdroidTileCache(new java.io.File(base, "tiles"));
      config.setTileDownloadThreads((short) 2);
      config.setCacheMapTileOvershoot((short) 0);
      mapView = new MapView(this);
      mapView.setTileSource(TileSourceFactory.MAPNIK);
      mapView.setMultiTouchControls(true);
      mapView.setMinZoomLevel(3d);
      mapView.setMaxZoomLevel(19d);
      container.addView(mapView, new FrameLayout.LayoutParams(-1, -1));
      mapView.setOnTouchListener(
          (v, e) -> {
            boolean end =
                e.getActionMasked() == android.view.MotionEvent.ACTION_UP
                    || e.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL;
            v.getParent().requestDisallowInterceptTouchEvent(!end);
            return false;
          });
      TextView attribution = new TextView(this);
      attribution.setText("© OpenStreetMap contributors ↗");
      attribution.setTextSize(12);
      attribution.setTextColor(getColor(R.color.pine));
      attribution.setBackgroundColor(0xEEFAF7EF);
      attribution.setMinHeight(dp(48));
      attribution.setGravity(android.view.Gravity.CENTER_VERTICAL);
      attribution.setPadding(dp(10), dp(4), dp(10), dp(4));
      attribution.setFocusable(true);
      attribution.setOnClickListener(
          v -> external(Intent.ACTION_VIEW, "https://www.openstreetmap.org/copyright"));
      container.addView(
          attribution, new FrameLayout.LayoutParams(-1, -2, android.view.Gravity.BOTTOM));
    }
    mapView.setUseDataConnection(true);
    mapView.onResume();
    updateMap();
  }

  private void updateMap() {
    if (mapView == null) return;
    mapView.getOverlays().clear();
    Marker origin = new Marker(mapView);
    origin.setPosition(new GeoPoint(location.latitude, location.longitude));
    origin.setTitle("Lokasi pilihan");
    origin.setSnippet(location.label);
    mapView.getOverlays().add(origin);
    for (MosquePlace p : places) {
      Marker marker = new Marker(mapView);
      marker.setPosition(new GeoPoint(p.latitude, p.longitude));
      marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
      marker.setIcon(getDrawable(R.drawable.mosque_pin));
      marker.setTitle(p.name);
      marker.setOnMarkerClickListener(
          (m, mv) -> {
            sourceDialog =
                new AlertDialog.Builder(this)
                    .setTitle(p.name)
                    .setMessage(p.address.isEmpty() ? "Alamat belum tercantum" : p.address)
                    .setPositiveButton("Petunjuk arah", (d, w) -> directions(p))
                    .setNegativeButton("Tutup", null)
                    .show();
            return true;
          });
      mapView.getOverlays().add(marker);
    }
    mapView.setContentDescription(
        "Peta OpenStreetMap, "
            + places.size()
            + " hasil. Rincian masjid juga tersedia pada daftar.");
    mapView.getController().setZoom(14d);
    mapView.getController().setCenter(new GeoPoint(location.latitude, location.longitude));
    mapView.invalidate();
  }

  @Override
  protected void onResume() {
    super.onResume();
    resumed = true;
    if (mapView != null && findViewById(R.id.mosquesMap).getVisibility() == View.VISIBLE) {
      mapView.setUseDataConnection(true);
      mapView.onResume();
    }
    LocationPoint selected = LocationStore.current(this);
    if (!selected.key().equals(location.key())) {
      location = selected;
      search.cancel();
      generation++;
      places.clear();
      lastRequest = -30000;
      nextAllowedSearch = 0;
      render();
      labels();
    }
    ((Button) findViewById(R.id.mosquesRefresh)).setEnabled(true);
    if (restoreResults) {
      restoreResults = false;
      load(false);
    }
    if (restoreMap) {
      restoreMap = false;
      toggleMap();
    }
  }

  @Override
  protected void onPause() {
    if (searching) {
      searching = false;
      text(R.id.mosquesStatus, "Pencarian dihentikan. Ketuk Cari masjid untuk mencoba lagi.");
    }
    resumed = false;
    generation++;
    search.cancel();
    if (mapView != null) {
      mapView.setUseDataConnection(false);
      mapView.onPause();
    }
    if (sourceDialog != null) sourceDialog.dismiss();
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putBoolean("searched", !places.isEmpty());
    state.putBoolean("map_open", findViewById(R.id.mosquesMap).getVisibility() == View.VISIBLE);
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    search.close();
    work.shutdownNow();
    if (mapView != null) mapView.onDetach();
    super.onDestroy();
  }
}
