package com.zikra.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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
  private boolean searching, hasSearched, restoreResults, restoreMap;
  private MapView mapView;
  private final android.os.Handler mapStatus =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private boolean mapHasTiles;
  private long mapRetryAt;
  private final Runnable mapTimeout = () -> mapUnavailable();
  private final android.os.Handler tileEvents =
      new android.os.Handler(
          android.os.Looper.getMainLooper(),
          message -> {
            if (!resumed
                || mapView == null
                || findViewById(R.id.mosquesMap).getVisibility() != View.VISIBLE) return true;
            if (message.what == org.osmdroid.tileprovider.MapTileProviderBase.MAPTILE_SUCCESS_ID) {
              mapHasTiles = true;
              mapStatus.removeCallbacks(mapTimeout);
              findViewById(R.id.mosquesMapNotice).setVisibility(View.GONE);
              findViewById(R.id.mosquesMapRetry).setVisibility(View.GONE);
              text(R.id.mosquesMapStatus, getString(R.string.nearby_map_ready));
            } else if (message.what == org.osmdroid.tileprovider.MapTileProviderBase.MAPTILE_FAIL_ID
                && !mapHasTiles) mapUnavailable();
            return true;
          });
  private MosquePlace selectedPlace;
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
            null,
            0,
            null);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.mosquesHeader))
        .setSurfaceColor(getColor(R.color.nearby_surface));
    setupScreen("Masjid terdekat");
    getWindow().setStatusBarColor(getColor(R.color.nearby_surface));
    getWindow().setNavigationBarColor(getColor(R.color.nearby_surface));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    search = new MosqueSearch(this);
    location = LocationStore.current(this);
    findViewById(R.id.mosquesLocation).setOnClickListener(v -> open(LocationActivity.class));
    findViewById(R.id.mosquesProvider).setOnClickListener(v -> chooseProvider());
    findViewById(R.id.mosquesRefresh).setOnClickListener(v -> load(lastRequest >= 0));
    ((RadioGroup) findViewById(R.id.mosquesViewMode))
        .setOnCheckedChangeListener((group, id) -> showMap(id == R.id.mosquesMapToggle));
    findViewById(R.id.mosquesExternal)
        .setOnClickListener(
            v ->
                external(
                    Intent.ACTION_VIEW,
                    "https://www.google.com/maps/search/?api=1&query="
                        + android.net.Uri.encode(
                            "masjid near " + location.latitude + "," + location.longitude)));
    findViewById(R.id.mosquesAttribution).setOnClickListener(v -> chooseProvider());
    findViewById(R.id.mosquesMapRetry).setOnClickListener(v -> retryMap());
    labels();
  }

  private void labels() {
    text(
        R.id.mosquesLocation,
        location.label + "\nUbah lokasi · " + location.zoneLabel(java.time.Instant.now()));
    text(R.id.mosquesProvider, "Tentang data OpenStreetMap");
    text(R.id.mosquesAttribution, "© OpenStreetMap contributors · ODbL ↗");
    if (location.source.equals("initial"))
      text(R.id.mosquesStatus, "Lokasi masih acuan awal. Pilih lokasi agar hasil sesuai tempatmu.");
    else text(R.id.mosquesStatus, "Pilih Cari masjid untuk mencari dari lokasi ini.");
    text(R.id.mosquesCount, "Di sekitarmu");
    text(R.id.mosquesRefresh, getString(R.string.nearby_search));
    findViewById(R.id.mosquesUpdated).setVisibility(View.GONE);
    emptyState(R.string.nearby_empty_title, R.string.nearby_empty_body);
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
    if (places.isEmpty()) text(R.id.mosquesCount, getString(R.string.nearby_loading));
    findViewById(R.id.mosquesProgress).setVisibility(View.VISIBLE);
    findViewById(R.id.mosquesEmpty).setVisibility(View.GONE);
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
                  hasSearched = true;
                  findViewById(R.id.mosquesProgress).setVisibility(View.GONE);
                  places.clear();
                  places.addAll(found);
                  selectedPlace = places.isEmpty() ? null : places.get(0);
                  render();
                  ((Button) findViewById(R.id.mosquesRefresh)).setEnabled(true);
                  text(R.id.mosquesCount, found.size() + " tempat ditemukan");
                  text(
                      R.id.mosquesStatus,
                      found.isEmpty()
                          ? "Belum ada hasil dalam radius 3 km."
                          : "Terdekat lebih dulu · jarak garis lurus");
                  if (found.isEmpty())
                    emptyState(R.string.nearby_no_results_title, R.string.nearby_no_results_body);
                  text(
                      R.id.mosquesUpdated,
                      (response.cached ? "Data tersimpan" : "Diperbarui")
                          + " · "
                          + java.time.format.DateTimeFormatter.ofPattern(
                                  "d MMM HH:mm", Locale.forLanguageTag("id-ID"))
                              .withZone(point.zone)
                              .format(java.time.Instant.ofEpochMilli(response.fetchedAt))
                          + " "
                          + point.zoneLabel(java.time.Instant.ofEpochMilli(response.fetchedAt)));
                  findViewById(R.id.mosquesUpdated).setVisibility(View.VISIBLE);
                  text(R.id.mosquesRefresh, getString(R.string.nearby_refresh));
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (!current(token)) return;
                  searching = false;
                  findViewById(R.id.mosquesProgress).setVisibility(View.GONE);
                  boolean limited = e instanceof MosqueSearch.RateLimited;
                  if (limited)
                    nextAllowedSearch =
                        SystemClock.elapsedRealtime() + ((MosqueSearch.RateLimited) e).retryAfterMs;
                  ((Button) findViewById(R.id.mosquesRefresh)).setEnabled(true);
                  text(
                      R.id.mosquesStatus,
                      (limited
                              ? getString(
                                  R.string.nearby_rate_limit,
                                  (((MosqueSearch.RateLimited) e).retryAfterMs + 999) / 1000)
                              : "Pencarian belum tersedia. Periksa koneksi dan coba lagi nanti.")
                          + (places.isEmpty() ? "" : " Hasil sebelumnya tetap ditampilkan."));
                  if (places.isEmpty()) {
                    text(R.id.mosquesCount, getString(R.string.nearby_incomplete));
                    text(R.id.mosquesRefresh, getString(R.string.nearby_retry));
                    emptyState(
                        limited ? R.string.nearby_rate_limit_title : R.string.nearby_error_title,
                        limited ? R.string.nearby_rate_limit_body : R.string.nearby_error_body);
                  }
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
      View card = mosqueCard(place, list);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(12);
      list.addView(card, lp);
    }
    updateMap();
    updateSelection();
  }

  private String distance(MosquePlace place) {
    return place.distance < 1000
        ? Math.round(place.distance) + " m"
        : String.format(Locale.forLanguageTag("id-ID"), "%.1f km", place.distance / 1000);
  }

  private View mosqueCard(MosquePlace place, android.view.ViewGroup parent) {
    View card = getLayoutInflater().inflate(R.layout.item_nearby_mosque, parent, false);
    ((TextView) card.findViewById(R.id.mosqueName)).setText(place.name);
    ((TextView) card.findViewById(R.id.mosqueDistance)).setText(distance(place) + " · garis lurus");
    card.setContentDescription(
        getString(R.string.nearby_details, place.name) + ", " + distance(place));
    card.setOnClickListener(v -> showDetails(place));
    View route = card.findViewById(R.id.mosqueDirections);
    route.setContentDescription(getString(R.string.nearby_route, place.name));
    route.setOnClickListener(v -> directions(place));
    return card;
  }

  private void showDetails(MosquePlace place) {
    sourceDialog =
        new AlertDialog.Builder(this)
            .setTitle(place.name)
            .setMessage(
                distance(place)
                    + " · jarak garis lurus\n\n"
                    + (place.address.isEmpty()
                        ? "Alamat belum tercantum di OpenStreetMap."
                        : place.address)
                    + "\n\nSumber: OpenStreetMap. Petunjuk arah dibuka melalui Google Maps.")
            .setPositiveButton("Petunjuk arah", (d, w) -> directions(place))
            .setNegativeButton("Tutup", null)
            .show();
  }

  private void emptyState(int title, int body) {
    text(R.id.mosquesEmptyTitle, getString(title));
    text(R.id.mosquesEmptyBody, getString(body));
    findViewById(R.id.mosquesEmpty).setVisibility(View.VISIBLE);
  }

  private void updateSelection() {
    FrameLayout selection = findViewById(R.id.mosquesSelection);
    selection.removeAllViews();
    boolean visible =
        findViewById(R.id.mosquesMap).getVisibility() == View.VISIBLE && selectedPlace != null;
    selection.setVisibility(visible ? View.VISIBLE : View.GONE);
    if (visible) selection.addView(mosqueCard(selectedPlace, selection));
  }

  private void directions(MosquePlace place) {
    android.net.Uri.Builder uri =
        android.net.Uri.parse("https://www.google.com/maps/dir/")
            .buildUpon()
            .appendQueryParameter("api", "1")
            .appendQueryParameter("destination", place.latitude + "," + place.longitude);
    external(Intent.ACTION_VIEW, uri.build().toString());
  }

  private void showMap(boolean show) {
    FrameLayout container = findViewById(R.id.mosquesMap);
    findViewById(R.id.mosquesResults).setVisibility(show ? View.GONE : View.VISIBLE);
    findViewById(R.id.mosquesMapStatus).setVisibility(show ? View.VISIBLE : View.GONE);
    if (!show) {
      container.setVisibility(View.GONE);
      mapStatus.removeCallbacks(mapTimeout);
      findViewById(R.id.mosquesMapRetry).setVisibility(View.GONE);
      if (mapView != null) {
        mapView.setUseDataConnection(false);
        mapView.onPause();
      }
      updateSelection();
      return;
    }
    container.setVisibility(View.VISIBLE);
    text(
        R.id.mosquesMapStatus,
        "Ketuk penanda untuk memilih masjid. Jika peta belum termuat, gunakan tampilan Daftar.");
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
      mapView.getMapOverlay().setLoadingBackgroundColor(getColor(R.color.nearby_panel));
      mapView.getMapOverlay().setLoadingLineColor(getColor(R.color.nearby_panel));
      mapView.getTileProvider().getTileRequestCompleteHandlers().add(tileEvents);
      container.addView(mapView, 0, new FrameLayout.LayoutParams(-1, -1));
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
      attribution.setTextColor(getColor(R.color.text));
      attribution.setBackgroundColor(getColor(R.color.nearby_surface));
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
    updateSelection();
    if (!mapHasTiles) mapLoading();
  }

  private void mapLoading() {
    text(R.id.mosquesMapNotice, getString(R.string.nearby_map_loading));
    findViewById(R.id.mosquesMapNotice).setVisibility(View.VISIBLE);
    findViewById(R.id.mosquesMapRetry).setVisibility(View.GONE);
    mapStatus.removeCallbacks(mapTimeout);
    mapStatus.postDelayed(mapTimeout, 12000);
  }

  private void mapUnavailable() {
    mapStatus.removeCallbacks(mapTimeout);
    text(R.id.mosquesMapNotice, getString(R.string.nearby_map_unavailable));
    findViewById(R.id.mosquesMapNotice).setVisibility(View.VISIBLE);
    findViewById(R.id.mosquesMapRetry).setVisibility(View.VISIBLE);
    text(R.id.mosquesMapStatus, getString(R.string.nearby_map_help));
  }

  private void retryMap() {
    long now = android.os.SystemClock.elapsedRealtime();
    if (now < mapRetryAt) {
      text(R.id.mosquesMapStatus, getString(R.string.nearby_map_wait));
      return;
    }
    mapRetryAt = now + 15000;
    mapLoading();
    mapView.setUseDataConnection(true);
    mapView.invalidate();
  }

  private void updateMap() {
    if (mapView == null) return;
    mapView.getOverlays().clear();
    Marker origin = new Marker(mapView);
    origin.setPosition(new GeoPoint(location.latitude, location.longitude));
    origin.setIcon(getDrawable(R.drawable.ic_nearby_pin));
    origin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
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
            selectedPlace = p;
            updateSelection();
            View selection = findViewById(R.id.mosquesSelection);
            selection.announceForAccessibility(p.name + ", " + distance(p));
            selection.requestRectangleOnScreen(
                new android.graphics.Rect(0, 0, selection.getWidth(), selection.getHeight()), true);
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
      if (!mapHasTiles) mapLoading();
    }
    LocationPoint selected = LocationStore.current(this);
    if (!selected.key().equals(location.key())) {
      location = selected;
      search.cancel();
      generation++;
      places.clear();
      selectedPlace = null;
      mapHasTiles = false;
      searching = false;
      hasSearched = false;
      findViewById(R.id.mosquesProgress).setVisibility(View.GONE);
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
      ((RadioGroup) findViewById(R.id.mosquesViewMode)).check(R.id.mosquesMapToggle);
    }
  }

  @Override
  protected void onPause() {
    if (searching) {
      searching = false;
      findViewById(R.id.mosquesProgress).setVisibility(View.GONE);
      if (places.isEmpty()) emptyState(R.string.nearby_empty_title, R.string.nearby_empty_body);
      if (places.isEmpty()) text(R.id.mosquesCount, getString(R.string.nearby_incomplete));
      text(R.id.mosquesStatus, "Pencarian dihentikan. Ketuk Cari masjid untuk mencoba lagi.");
    }
    resumed = false;
    mapStatus.removeCallbacks(mapTimeout);
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
    state.putBoolean("searched", hasSearched);
    state.putBoolean("map_open", findViewById(R.id.mosquesMap).getVisibility() == View.VISIBLE);
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    search.close();
    work.shutdownNow();
    mapStatus.removeCallbacksAndMessages(null);
    tileEvents.removeCallbacksAndMessages(null);
    if (mapView != null) {
      mapView.getTileProvider().getTileRequestCompleteHandlers().remove(tileEvents);
      mapView.onDetach();
    }
    super.onDestroy();
  }
}
