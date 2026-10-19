package com.zikra.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.location.CitySearch;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Foreground-only location acquisition and an explicit, offline-capable city picker. */
public final class LocationActivity extends BaseActivity {
  private static final int REQUEST_LOCATION = 804;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final ExecutorService work = Executors.newSingleThreadExecutor();
  private final CitySearch service = new CitySearch();
  private LocationManager manager;
  private volatile int generation;
  private boolean busy, tracking;
  private AlertDialog confirmation;
  private final Runnable timeout =
      () -> {
        stopTracking();
        busy = false;
        status("Lokasi belum ditemukan. Coba di tempat terbuka atau pilih kota secara manual.");
      };
  private final LocationListener listener =
      new LocationListener() {
        @Override
        public void onLocationChanged(Location fix) {
          if (tracking) resolve(fix);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
      };

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_location);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.locationHeader))
        .bind(
            R.id.toolbarBack, R.id.featureTitle, R.id.featureSubtitle, 0, "Lokasi", null, 0, null);
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.locationHeader))
        .setSurfaceColor(getColor(R.color.onboarding_paper));
    setupScreen("Lokasi");
    getWindow().setStatusBarColor(getColor(R.color.onboarding_paper));
    getWindow().setNavigationBarColor(getColor(R.color.onboarding_paper));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    manager = (LocationManager) getSystemService(LOCATION_SERVICE);
    findViewById(R.id.locationAutomatic).setOnClickListener(v -> automatic());
    findViewById(R.id.locationSearch).setOnClickListener(v -> search());
    findViewById(R.id.locationMore).setOnClickListener(v -> locationDetails());
    ((EditText) findViewById(R.id.locationQuery))
        .setOnEditorActionListener(
            (v, action, e) -> {
              if (action == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
              }
              return false;
            });
    findViewById(R.id.locationSettings)
        .setOnClickListener(
            v -> {
              try {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
              } catch (android.content.ActivityNotFoundException e) {
                toast("Pengaturan tidak tersedia.");
              }
            });
    findViewById(R.id.locationAttribution)
        .setOnClickListener(
            v -> external(Intent.ACTION_VIEW, "https://open-meteo.com/en/docs/geocoding-api"));
    results(CitySearch.bundled(""), "Pilihan kota offline · pusat kota");
    showCurrent();
  }

  private void showCurrent() {
    LocationPoint p = LocationStore.current(this);
    ((TextView) findViewById(R.id.locationCurrent)).setText(p.label);
    String source =
        p.source.equals("initial")
            ? "Lokasi awal; belum mengikuti posisimu"
            : p.source.equals("device")
                ? "Lokasi perangkat · diambil saat diminta"
                : "Kota pilihan · pusat kota";
    ((TextView) findViewById(R.id.locationDetails))
        .setText(source + "\nZona waktu " + p.zoneLabel(Instant.now()));
  }

  private void locationDetails() {
    LocationPoint p = LocationStore.current(this);
    confirmation =
        new AlertDialog.Builder(this)
            .setTitle("Rincian lokasi")
            .setMessage(
                p.label
                    + "\n"
                    + p.zone.getId()
                    + " · "
                    + p.zoneLabel(Instant.now())
                    + "\n"
                    + String.format(Locale.ROOT, "%.4f, %.4f", p.latitude, p.longitude)
                    + (p.accuracy >= 0
                        ? "\nPerkiraan akurasi ±" + Math.round(p.accuracy) + " m"
                        : ""))
            .setPositiveButton("Tutup", null)
            .show();
  }

  private void status(String text) {
    ((TextView) findViewById(R.id.locationStatus)).setText(text);
  }

  private void search() {
    EditText query = findViewById(R.id.locationQuery);
    String name = query.getText().toString().trim();
    if (name.length() < 2) {
      query.setError("Tulis setidaknya dua huruf nama kota");
      return;
    }
    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
        .hideSoftInputFromWindow(query.getWindowToken(), 0);
    stopTracking();
    service.cancel();
    int token = ++generation;
    busy = true;
    status("Mencari kota…");
    work.execute(
        () -> {
          if (!current(token)) return;
          try {
            List<LocationPoint> found = service.search(name);
            runOnUiThread(
                () -> {
                  if (!current(token)) return;
                  busy = false;
                  results(found, "Hasil Open-Meteo / GeoNames · pusat kota");
                  status(
                      found.isEmpty()
                          ? "Kota belum ditemukan. Coba nama atau ejaan lain."
                          : "Pilih kota yang sesuai, lalu konfirmasi.");
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (!current(token)) return;
                  busy = false;
                  List<LocationPoint> offline = CitySearch.bundled(name);
                  results(offline, "Pilihan offline · pusat kota");
                  status(
                      "Pencarian online belum tersedia. "
                          + (offline.isEmpty()
                              ? "Coba lagi saat tersambung atau cari kota lain dalam pilihan"
                                  + " offline."
                              : "Kamu tetap bisa memilih kota offline berikut."));
                });
          }
        });
  }

  private void results(List<LocationPoint> points, String label) {
    ((TextView) findViewById(R.id.locationResultsLabel)).setText(label);
    LinearLayout list = findViewById(R.id.locationResults);
    list.removeAllViews();
    for (LocationPoint point : points) {
      TextView row = new TextView(this);
      String labelText = point.label + "\n" + point.zoneLabel(Instant.now()) + " · pusat kota";
      android.text.SpannableString rowLabel = new android.text.SpannableString(labelText);
      rowLabel.setSpan(
          new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
          0,
          point.label.length(),
          android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      rowLabel.setSpan(
          new android.text.style.ForegroundColorSpan(getColor(R.color.onboarding_muted)),
          point.label.length() + 1,
          rowLabel.length(),
          android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      row.setText(rowLabel);
      row.setTextSize(15);
      row.setTextColor(getColor(R.color.pine));
      row.setLineSpacing(dp(4), 1);
      row.setMinHeight(dp(64));
      row.setGravity(android.view.Gravity.CENTER_VERTICAL);
      row.setPadding(dp(16), dp(14), dp(16), dp(14));
      row.setBackgroundResource(R.drawable.bg_nearby_card);
      row.setFocusable(true);
      row.setOnClickListener(v -> confirm(point));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(8);
      list.addView(row, lp);
    }
  }

  private void confirm(LocationPoint point) {
    if (confirmation != null) confirmation.dismiss();
    confirmation =
        new AlertDialog.Builder(this)
            .setTitle("Gunakan lokasi ini?")
            .setMessage(
                point.label
                    + "\n"
                    + point.zone.getId()
                    + " · "
                    + point.zoneLabel(Instant.now())
                    + "\n\nJadwal sholat, pengingat, cuaca dan Kiblat akan mengikuti lokasi ini.")
            .setPositiveButton(
                "Gunakan lokasi",
                (d, which) -> {
                  LocationStore.save(this, point);
                  showCurrent();
                  status("Lokasi disimpan. Fitur yang terkait akan memakai pilihan ini.");
                })
            .setNegativeButton("Batal", null)
            .show();
  }

  private void automatic() {
    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(
          new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
          },
          REQUEST_LOCATION);
      return;
    }
    stopTracking();
    service.cancel();
    generation++;
    busy = true;
    findViewById(R.id.locationSettings).setVisibility(View.GONE);
    if (manager == null
        || (android.os.Build.VERSION.SDK_INT >= 28 && !manager.isLocationEnabled())) {
      busy = false;
      status("Lokasi perangkat belum aktif. Aktifkan di pengaturan atau pilih kota secara manual.");
      findViewById(R.id.locationSettings).setVisibility(View.VISIBLE);
      return;
    }
    Location recent = null;
    boolean requested = false;
    for (String provider :
        new String[] {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER}) {
      try {
        if (!manager.isProviderEnabled(provider)) continue;
        Location last = manager.getLastKnownLocation(provider);
        if (last != null) {
          long age = SystemClock.elapsedRealtimeNanos() - last.getElapsedRealtimeNanos();
          if (age >= 0
              && age < 3 * 60_000_000_000L
              && (recent == null
                  || last.getElapsedRealtimeNanos() > recent.getElapsedRealtimeNanos()))
            recent = last;
        }
        manager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
        requested = tracking = true;
      } catch (SecurityException | IllegalArgumentException unavailable) {
        /* Try the other provider. */
      }
    }
    if (recent != null) {
      resolve(recent);
      return;
    }
    if (!requested) {
      busy = false;
      status("Layanan lokasi belum tersedia. Pilih kota secara manual atau periksa izin aplikasi.");
      findViewById(R.id.locationSettings).setVisibility(View.VISIBLE);
      return;
    }
    status("Mencari posisi perangkat… Kamu tetap bisa memilih kota secara manual.");
    handler.postDelayed(timeout, 25000);
  }

  private void resolve(Location fix) {
    stopTracking();
    int token = ++generation;
    busy = true;
    status("Menyesuaikan zona waktu lokasi…");
    work.execute(
        () -> {
          if (!current(token)) return;
          try {
            LocationPoint point = service.device(fix);
            runOnUiThread(
                () -> {
                  if (current(token)) {
                    busy = false;
                    status("Posisi ditemukan. Periksa dan konfirmasi lokasinya.");
                    confirm(point);
                  }
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (!current(token)) return;
                  busy = false;
                  status(
                      "Posisi ditemukan, tetapi zona waktu belum bisa diperiksa. Pilih zona waktu"
                          + " yang sesuai atau gunakan pencarian kota.");
                  String[] zones = {
                    "Asia/Jakarta", "Asia/Makassar", "Asia/Jayapura", ZoneId.systemDefault().getId()
                  };
                  confirmation =
                      new AlertDialog.Builder(this)
                          .setTitle("Zona waktu lokasi")
                          .setItems(
                              new String[] {"WIB", "WITA", "WIT", "Zona perangkat: " + zones[3]},
                              (d, n) ->
                                  confirm(
                                      new LocationPoint(
                                          fix.getLatitude(),
                                          fix.getLongitude(),
                                          zones[n],
                                          "Lokasi perangkat",
                                          "device",
                                          System.currentTimeMillis(),
                                          fix.hasAccuracy() ? fix.getAccuracy() : -1)))
                          .setNegativeButton("Batal", null)
                          .show();
                });
          }
        });
  }

  private boolean current(int token) {
    return token == generation && !isFinishing() && !isDestroyed();
  }

  private void stopTracking() {
    handler.removeCallbacks(timeout);
    if (manager != null && tracking) {
      try {
        manager.removeUpdates(listener);
      } catch (SecurityException ignored) {
      }
    }
    tracking = false;
  }

  @Override
  public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(code, permissions, grants);
    if (code == REQUEST_LOCATION) {
      if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
              == PackageManager.PERMISSION_GRANTED
          || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
              == PackageManager.PERMISSION_GRANTED) automatic();
      else status("Izin lokasi belum diberikan. Semua pilihan kota manual tetap bisa dipakai.");
    }
  }

  @Override
  protected void onPause() {
    stopTracking();
    service.cancel();
    generation++;
    if (busy) {
      busy = false;
      status("Pencarian dihentikan. Pilih Cari kota atau Gunakan lokasi saya untuk mencoba lagi.");
    }
    if (confirmation != null) confirmation.dismiss();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    service.close();
    work.shutdownNow();
    super.onDestroy();
  }
}
