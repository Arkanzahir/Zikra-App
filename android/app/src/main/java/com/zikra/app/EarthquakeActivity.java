package com.zikra.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.zikra.app.safety.BmkgClient;
import com.zikra.app.safety.BmkgParser;
import com.zikra.app.safety.EarthquakeMapView;
import com.zikra.app.safety.ShakemapLoader;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Geographic earthquake detail with an actual-coordinate Maps intent and native bottom sheet. */
public final class EarthquakeActivity extends BaseActivity {
  private BmkgClient bmkg;
  private EarthquakeMapView map;
  private ShakemapLoader shaking;
  private boolean shakingSelected;
  private BmkgParser.Earthquake earthquake;
  private java.time.Instant retrievedAt;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.Theme_Zikra_Protector);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_earthquake);
    setupScreen("Gempabumi BMKG");
    getWindow().setStatusBarColor(0xfffaf8f0);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    map = findViewById(R.id.quakeMap);
    shaking =
        new ShakemapLoader(
            this, findViewById(R.id.quakeShakingImage), findViewById(R.id.quakeShakingStatus));
    findViewById(R.id.quakeTabEpicenter).setOnClickListener(v -> selectMap(false));
    findViewById(R.id.quakeTabShaking).setOnClickListener(v -> selectMap(true));
    selectMap(false);
    int available = getResources().getDisplayMetrics().heightPixels;
    findViewById(R.id.quakeSheet).getLayoutParams().height =
        Math.min(dp(320), Math.round(available * .44f));
    bmkg = new BmkgClient(this);
    findViewById(R.id.protectorRefresh).setOnClickListener(v -> refresh());
    findViewById(R.id.quakeSource)
        .setOnClickListener(
            v -> external(Intent.ACTION_VIEW, "https://data.bmkg.go.id/gempabumi/"));
    findViewById(R.id.quakeOpenMap)
        .setOnClickListener(
            v -> {
              if (earthquake != null && earthquake.hasCoordinates())
                external(
                    Intent.ACTION_VIEW,
                    "https://www.google.com/maps/search/?api=1&query="
                        + earthquake.latitude
                        + ","
                        + earthquake.longitude);
            });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!shakingSelected) map.resume();
    else shaking.load(earthquake == null ? "" : earthquake.shakemapUrl);
    refresh();
  }

  private void selectMap(boolean selected) {
    shakingSelected = selected;
    findViewById(R.id.quakeShakingPanel)
        .setVisibility(selected ? android.view.View.VISIBLE : android.view.View.GONE);
    for (int id : new int[] {R.id.quakeTabEpicenter, R.id.quakeTabShaking}) {
      boolean active = (id == R.id.quakeTabShaking) == selected;
      findViewById(id)
          .setBackgroundResource(
              active
                  ? R.drawable.protector_reference_selected
                  : R.drawable.protector_reference_outline);
      ((TextView) findViewById(id)).setTextColor(active ? 0xffffffff : 0xff123337);
    }
    if (selected) {
      map.pause();
      shaking.load(earthquake == null ? "" : earthquake.shakemapUrl);
    } else {
      shaking.pause();
      map.resume();
    }
  }

  @Override
  protected void onPause() {
    map.pause();
    shaking.pause();
    if (bmkg != null) bmkg.reset();
    super.onPause();
  }

  private void refresh() {
    bmkg.reset();
    text(
        R.id.quakeStatus,
        earthquake == null ? "Menghubungkan ke BMKG…" : sourceStatus() + "\nMemperbarui…");
    if (earthquake == null) {
      findViewById(R.id.quakeOpenMap).setEnabled(false);
      map.setEpicenter(null, null);
      text(R.id.quakeArea, "Memuat gempabumi terkini…");
      text(R.id.quakeTime, "Waktu kejadian belum tersedia");
      text(R.id.quakeMagnitude, "—");
      text(R.id.quakeClock, "—");
      text(R.id.quakeDepth, "—");
      text(R.id.quakeStatus, "Menghubungkan ke BMKG…");
      text(R.id.quakePotential, "Potensi belum tersedia dari BMKG");
      text(R.id.quakeCoordinates, "Koordinat episentrum belum tersedia");
      text(R.id.quakeFelt, "Wilayah dirasakan belum tersedia");
    }
    bmkg.earthquake(
        new BmkgClient.Result<BmkgParser.Earthquake>() {
          @Override
          public void success(BmkgParser.Earthquake value) {
            show(value);
          }

          @Override
          public void failure(String message) {
            showFailure(message);
          }
        });
  }

  private void showFailure(String message) {
    if (earthquake == null) text(R.id.quakeArea, "Laporan gempa belum tersedia");
    text(
        R.id.quakeStatus,
        earthquake == null
            ? message + " Pilih Muat ulang di kanan atas."
            : sourceStatus()
                + "\nPembaruan gagal. Laporan sebelumnya tetap ditampilkan. Pilih Muat ulang.");
  }

  private String sourceStatus() {
    return "Laporan terakhir BMKG · Indonesia\nDiambil "
        + DateTimeFormatter.ofPattern("d MMM HH:mm 'WIB'", new Locale("id", "ID"))
            .withZone(ZoneId.of("Asia/Jakarta"))
            .format(retrievedAt)
        + "\nBukan peringatan untuk lokasi Anda.";
  }

  private void show(BmkgParser.Earthquake q) {
    String previous = earthquake == null ? "" : earthquake.shakemapUrl;
    earthquake = q;
    if (!previous.equals(q.shakemapUrl)) shaking.clear();
    if (shakingSelected) shaking.load(q.shakemapUrl);
    retrievedAt = java.time.Instant.now();
    text(R.id.quakeArea, q.area);
    text(
        R.id.quakeTime,
        q.time == null
            ? q.sourceDate
            : DateTimeFormatter.ofPattern(
                    "EEEE, d MMM yyyy · HH:mm:ss 'WIB'", new Locale("id", "ID"))
                .withZone(ZoneId.of("Asia/Jakarta"))
                .format(q.time));
    text(R.id.quakeMagnitude, q.magnitude);
    text(
        R.id.quakeClock,
        q.time == null
            ? "—"
            : DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.of("Asia/Jakarta"))
                .format(q.time));
    text(R.id.quakeDepth, available(q.depth));
    text(R.id.quakePotential, available(q.potential));
    text(
        R.id.quakeCoordinates,
        q.hasCoordinates()
            ? String.format(
                new Locale("id", "ID"),
                "%.3f° %s · %.3f° %s",
                Math.abs(q.latitude),
                q.latitude < 0 ? "LS" : "LU",
                Math.abs(q.longitude),
                q.longitude < 0 ? "BB" : "BT")
            : "Koordinat episentrum belum tersedia dari BMKG");
    text(
        R.id.quakeFelt, q.felt.isEmpty() || "-".equals(q.felt) ? "Tidak dicantumkan BMKG" : q.felt);
    text(R.id.quakeStatus, sourceStatus());
    map.setEpicenter(q.latitude, q.longitude);
    findViewById(R.id.quakeOpenMap).setEnabled(q.hasCoordinates());
  }

  private String available(String value) {
    return value.isEmpty() ? "Belum tersedia" : value;
  }

  private void text(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }

  @Override
  protected void onDestroy() {
    if (bmkg != null) bmkg.close();
    if (map != null) map.close();
    if (shaking != null) shaking.close();
    super.onDestroy();
  }
}
