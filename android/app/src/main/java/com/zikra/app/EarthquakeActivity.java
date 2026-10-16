package com.zikra.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.zikra.app.safety.BmkgClient;
import com.zikra.app.safety.BmkgParser;
import com.zikra.app.safety.IndonesiaMapView;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Geographic earthquake detail with an actual-coordinate Maps intent and native bottom sheet. */
public final class EarthquakeActivity extends BaseActivity {
  private BmkgClient bmkg;
  private BmkgParser.Earthquake earthquake;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_earthquake);
    setupScreen("Gempabumi BMKG");
    getWindow().setStatusBarColor(Color.WHITE);
    getWindow().setNavigationBarColor(Color.WHITE);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            getWindow().getDecorView().getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    text(R.id.protectorToolbarTitle, "Gempabumi");
    text(R.id.protectorToolbarSubtitle, "Info BMKG · Laporan terakhir Indonesia");
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
    refresh();
  }

  private void refresh() {
    bmkg.reset();
    earthquake = null;
    findViewById(R.id.quakeOpenMap).setEnabled(false);
    ((IndonesiaMapView) findViewById(R.id.quakeMap)).setEpicenter(null, null);
    text(R.id.quakeArea, "Memuat gempabumi terkini…");
    text(R.id.quakeTime, "Waktu kejadian belum tersedia");
    text(R.id.quakeMagnitude, "—");
    text(R.id.quakeDepth, "—");
    text(R.id.quakeStatus, "Menghubungkan ke BMKG…");
    text(R.id.quakePotential, "Potensi belum tersedia dari BMKG");
    text(R.id.quakeCoordinates, "Koordinat episentrum belum tersedia");
    text(R.id.quakeFelt, "Wilayah dirasakan belum tersedia");
    bmkg.earthquake(
        new BmkgClient.Result<BmkgParser.Earthquake>() {
          @Override
          public void success(BmkgParser.Earthquake value) {
            show(value);
          }

          @Override
          public void failure(String message) {
            text(R.id.quakeArea, "Data gempa belum tersedia");
            text(R.id.quakeStatus, message + " Ketuk ↻ untuk mencoba lagi.");
          }
        });
  }

  private void show(BmkgParser.Earthquake q) {
    earthquake = q;
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
    text(R.id.quakeDepth, available(q.depth));
    text(R.id.quakePotential, available(q.potential));
    text(
        R.id.quakeCoordinates,
        q.hasCoordinates()
            ? String.format(Locale.US, "Episentrum: %.3f, %.3f", q.latitude, q.longitude)
            : "Koordinat episentrum belum tersedia dari BMKG");
    text(
        R.id.quakeFelt,
        "Dirasakan: "
            + (q.felt.isEmpty() || "-".equals(q.felt) ? "Tidak dicantumkan BMKG" : q.felt));
    text(
        R.id.quakeStatus,
        "Laporan terakhir BMKG untuk Indonesia. Periksa waktu kejadian pada laporan.");
    ((IndonesiaMapView) findViewById(R.id.quakeMap)).setEpicenter(q.latitude, q.longitude);
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
    super.onDestroy();
  }
}
