package com.zikra.app.safety;

import android.app.job.*;
import android.net.*;
import com.zikra.app.location.*;
import com.zikra.app.R;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.*;
import okhttp3.*;

/** Best-effort periodic delivery; never starts GPS or uses exact prayer alarms. */
public final class ContextualReminderJob extends JobService {
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final OkHttpClient http = WeatherHttp.create();
  private volatile int generation;
  @Override public boolean onStartJob(JobParameters params) {
    int token = ++generation;
    worker.execute(() -> {
      try {
        android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
        if (!EarthquakeAlerts.permission(this) || manager == null || !manager.areNotificationsEnabled()) return;
        LocationPoint point = LocationStore.current(this);
        long revision = getSharedPreferences(ContextualReminders.PREFS, 0).getLong("revision", 0);
        ZonedDateTime now = ZonedDateTime.now(point.zone);
        if (token == generation && ContextualReminders.inWindow(now, ContextualReminders.minute(this, "morning"))) {
          boolean briefing = ContextualReminders.enabled(this, "briefing");
          String body = briefing ? ContextualReminders.briefing(this) : getString(R.string.contextual_notify_morning_body);
          if (briefing && ContextualReminders.enabled(this, "morning")) body += "\n" + getString(R.string.contextual_notify_morning_append);
          ContextualReminders.deliver(this, point, revision, "morning", 0,
              getString(briefing ? R.string.contextual_notify_briefing : R.string.contextual_notify_morning_title), body, briefing ? null : "Pagi & petang");
        }
        if (token == generation && ContextualReminders.inWindow(now, ContextualReminders.minute(this, "evening")))
          ContextualReminders.deliver(this, point, revision, "evening", 1,
              getString(R.string.contextual_notify_evening_title), getString(R.string.contextual_notify_evening_body), "Pagi & petang");
        if (token != generation || !ContextualReminders.enabled(this, "weather") || now.getHour() < 6 || now.getHour() >= 21) return;
        android.content.SharedPreferences prefs = getSharedPreferences(ContextualReminders.PREFS, 0);
        long last = prefs.getLong("weather_check_at", 0), current = System.currentTimeMillis();
        if (point.key().equals(prefs.getString("weather_check_location", ""))
            && last <= current && current - last < 3600000L) return;
        if (now.toLocalDate().toString().equals(prefs.getString("sent_weather", ""))) return;
        ConnectivityManager connectivity = getSystemService(ConnectivityManager.class);
        NetworkCapabilities network = connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        if (network == null || !network.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return;
        prefs.edit().putLong("weather_check_at", current).putString("weather_check_location", point.key()).commit();
        try (Response response = http.newCall(new Request.Builder().url(OpenMeteoClient.url(point)).build()).execute()) {
          if (token != generation || !response.isSuccessful() || response.body() == null) return;
          Instant fetched = Instant.now();
          BmkgParser.Forecast forecast = OpenMeteoParser.parse(BmkgClient.readBounded(response.body()), fetched, fetched, false, false, point);
          if (!WeatherOutlook.usable(forecast, fetched, OpenMeteoClient.FRESH_MS)) return;
          for (BmkgParser.Weather slot : forecast.slots) {
            String description = slot.description.toLowerCase(Locale.ROOT);
            if (slot.time.isBefore(fetched) || slot.time.isAfter(fetched.plusSeconds(3 * 3600))) continue;
            if (!(description.contains("hujan") || description.contains("petir"))) continue;
            String clock = DateTimeFormatter.ofPattern("HH:mm").format(slot.time.atZone(point.zone));
            if (token == generation) ContextualReminders.deliver(this, point, revision, "weather", 2,
                "Prakiraan cuaca sebelum beraktivitas", point.label + " · " + clock + " " + point.zoneLabel(slot.time)
                    + ": " + slot.description + ". Siapkan perlengkapan yang diperlukan. Open-Meteo; prakiraan model, bukan peringatan resmi.", null);
            break;
          }
        }
      } catch (Exception unavailable) { /* No stale forecast is turned into an alert. */ }
      finally { if (token == generation) jobFinished(params, false); }
    });
    return true;
  }
  @Override public boolean onStopJob(JobParameters params) {
    generation++; http.dispatcher().cancelAll(); return false;
  }
  @Override public void onDestroy() {
    generation++; http.dispatcher().cancelAll(); worker.shutdownNow(); super.onDestroy();
  }
}
