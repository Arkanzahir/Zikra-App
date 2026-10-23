package com.zikra.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.zikra.app.location.*;
import com.zikra.app.safety.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Approved weather/map composition, backed by existing lifecycle-bound data clients. */
public final class ProtectorActivity extends BaseActivity {
  private static final Locale ID = new Locale("id", "ID");
  private static final DateTimeFormatter CLOCK =
      DateTimeFormatter.ofPattern("HH:mm 'WIB'", ID).withZone(ZoneId.of("Asia/Jakarta"));
  private BmkgClient bmkg;
  private BmkgParser.Forecast weather;
  private BmkgParser.Earthquake quake;
  private LocationPoint location = LocationPoint.INITIAL;
  private Instant retrieved;
  private ProtectorWeatherView scene;
  private EarthquakeMapView map;
  private final android.os.Handler timer =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private final Runnable tick =
      new Runnable() {
        public void run() {
          refresh();
          timer.postDelayed(this, OpenMeteoClient.FRESH_MS);
        }
      };

  @Override
  protected void onCreate(Bundle saved) {
    setTheme(R.style.Theme_Zikra_Protector);
    super.onCreate(saved);
    setContentView(R.layout.activity_protector);
    setupScreen("Protector");
    getWindow().setStatusBarColor(0xff2697d1);
    getWindow().setNavigationBarColor(0xfffaf8f0);
    scene = findViewById(R.id.protectorWeatherScene);
    map = findViewById(R.id.protectorQuakeMap);
    map.setCompact(true);
    bmkg = new BmkgClient(this);
    findViewById(R.id.protectorScroll)
        .setOnScrollChangeListener((v, x, y, oldX, oldY) -> scene.updateVisibility());
    float scale = getResources().getConfiguration().fontScale;
    if (scale > 1.15f)
      findViewById(R.id.protectorWeatherCard).getLayoutParams().height =
          dp(284 + 160 * (scale - 1));
    findViewById(R.id.protectorRefresh).setOnClickListener(v -> refresh());
    findViewById(R.id.protectorLocation).setOnClickListener(v -> open(LocationActivity.class));
    findViewById(R.id.protectorWeatherTemperature)
        .setOnClickListener(v -> open(WeatherActivity.class));
    findViewById(R.id.protectorWeatherDetails).setOnClickListener(v -> open(WeatherActivity.class));
    findViewById(R.id.protectorQuakeCard).setOnClickListener(v -> open(EarthquakeActivity.class));
    findViewById(R.id.protectorQuakeOpen).setOnClickListener(v -> open(EarthquakeActivity.class));
    findViewById(R.id.protectorSource)
        .setOnClickListener(
            v -> external(Intent.ACTION_VIEW, "https://data.bmkg.go.id/gempabumi/"));
    findViewById(R.id.protectorWarnings)
        .setOnClickListener(
            v ->
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Saat terjadi gempa")
                    .setMessage(
                        "Lindungi kepala dan leher, berlindung di bawah meja yang kuat, lalu"
                            + " berpegangan. Jauhi kaca. Setelah guncangan berhenti, keluar dengan"
                            + " tertib dan jangan memakai lift. Di dekat pantai, ikuti arahan"
                            + " evakuasi resmi.\n\n"
                            + "Laporan ini adalah gempa terakhir di Indonesia, bukan peringatan"
                            + " dini untuk lokasimu.")
                    .setPositiveButton(
                        "Panduan BMKG",
                        (d, w) ->
                            external(
                                Intent.ACTION_VIEW,
                                "https://www.bmkg.go.id/gempabumi/antisipasi-gempabumi"))
                    .setNegativeButton("Tutup", null)
                    .show());
    findViewById(R.id.protectorEmergency).setOnClickListener(v -> showEmergencyActions());
    findViewById(R.id.protectorNavHome)
        .setOnClickListener(
            v ->
                startActivity(
                    new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    findViewById(R.id.protectorNavQuran).setOnClickListener(v -> open(QuranActivity.class));
    findViewById(R.id.protectorNavAi).setOnClickListener(v -> open(AiActivity.class));
    findViewById(R.id.protectorNavStudy).setOnClickListener(v -> open(StudyActivity.class));
    findViewById(R.id.protectorNavProfile).setOnClickListener(v -> open(ProfileActivity.class));
  }

  @Override
  protected void onResume() {
    super.onResume();
    LocationPoint selected = LocationStore.current(this);
    if (!selected.key().equals(location.key())) weather = null;
    location = selected;
    text(R.id.protectorLocation, location.label + " ⌄");
    scene.setActive(true);
    map.resume();
    timer.post(tick);
  }

  @Override
  protected void onPause() {
    timer.removeCallbacks(tick);
    scene.setActive(false);
    map.pause();
    bmkg.reset();
    super.onPause();
  }

  private void refresh() {
    bmkg.reset();
    if (weather != null
        && !WeatherCode.usableCache(
            weather.fetchedAt.toEpochMilli(),
            System.currentTimeMillis(),
            OpenMeteoClient.MAX_AGE_MS)) weather = null;
    if (weather == null) {
      scene.setWeather(null, location.zone);
      getWindow().setStatusBarColor(scene.getSkyColor());
      text(R.id.protectorWeatherTemperature, "—°");
      text(R.id.protectorWeatherDescription, "Memuat prakiraan…");
      text(R.id.protectorWeatherTime, "Menghubungkan ke Open-Meteo");
      text(R.id.protectorWeatherDetails, "Kelembapan — · Angin —");
    }
    bmkg.weather(
        new BmkgClient.Result<BmkgParser.Forecast>() {
          public void success(BmkgParser.Forecast f) {
            showWeather(f);
          }

          public void failure(String message) {
            if (weather == null) {
              text(R.id.protectorWeatherDescription, "Cuaca belum tersedia");
              text(R.id.protectorWeatherTime, "Periksa koneksi, lalu muat ulang");
            } else text(R.id.protectorWeatherTime, "Pembaruan gagal · prakiraan tersimpan");
          }
        });
    text(
        R.id.protectorQuakeStatus,
        quake == null ? "Menghubungkan ke BMKG…" : status() + " · Memperbarui…");
    bmkg.earthquake(
        new BmkgClient.Result<BmkgParser.Earthquake>() {
          public void success(BmkgParser.Earthquake q) {
            showQuake(q);
          }

          public void failure(String message) {
            if (quake == null) text(R.id.protectorQuakeArea, "Laporan gempa belum tersedia");
            text(
                R.id.protectorQuakeStatus,
                quake == null
                    ? "BMKG belum terhubung · ketuk muat ulang"
                    : status() + "\nPembaruan gagal; laporan sebelumnya ditampilkan.");
          }
        });
  }

  private void showWeather(BmkgParser.Forecast f) {
    weather = f;
    BmkgParser.Weather w = f.current;
    scene.setWeather(w.description, f.location.zone);
    getWindow().setStatusBarColor(scene.getSkyColor());
    text(R.id.protectorWeatherTemperature, w.temperature + "°");
    text(R.id.protectorWeatherDescription, w.description);
    text(
        R.id.protectorWeatherTime,
        "Open-Meteo · "
            + (f.stored ? "tersimpan" : "prakiraan")
            + " · "
            + DateTimeFormatter.ofPattern("d MMM HH:mm", ID)
                .withZone(f.location.zone)
                .format(w.time)
            + " "
            + f.location.zoneLabel(w.time));
    text(
        R.id.protectorWeatherDetails,
        "Kelembapan " + w.humidity + "%  ·  Angin " + w.wind + " km/jam");
  }

  private void showQuake(BmkgParser.Earthquake q) {
    quake = q;
    retrieved = Instant.now();
    text(R.id.protectorMagnitude, q.magnitude);
    text(R.id.protectorDepth, q.depth);
    text(
        R.id.protectorClock,
        q.time == null
            ? "—"
            : DateTimeFormatter.ofPattern("d MMM · HH:mm", ID)
                .withZone(ZoneId.of("Asia/Jakarta"))
                .format(q.time));
    text(R.id.protectorQuakeArea, q.area);
    text(R.id.protectorQuakeStatus, status());
    map.setEpicenter(q.latitude, q.longitude);
  }

  private String status() {
    return "Diambil "
        + CLOCK.format(retrieved)
        + " · laporan terakhir Indonesia\nBukan peringatan untuk lokasimu.";
  }

  private void text(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }

  @Override
  protected void onDestroy() {
    if (bmkg != null) bmkg.close();
    if (map != null) map.close();
    super.onDestroy();
  }
}
