package com.zikra.app.sky;

import android.app.Activity;
import android.content.SharedPreferences;
import com.zikra.app.PrayerSchedule;
import java.io.IOException;
import java.time.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;

/** One USNO daily response per local date; exact-location/date validation, no secrets. */
public final class SkyRepository implements AutoCloseable {
  private final Activity owner;
  private final SharedPreferences cache;
  private static final OkHttpClient client =
      new OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build();
  private Call call;
  private int generation;
  private boolean closed;

  public interface Result {
    void show(SkyDay day);
  }

  public SkyRepository(Activity owner) {
    this.owner = owner;
    cache = owner.getSharedPreferences("zikra_sky", 0);
  }

  public void load(LocalDate date, boolean force, Result result) {
    int version = ++generation;
    if (call != null) call.cancel();
    result.show(SkyDay.estimate(date));
    if (!force) {
      try {
        result.show(parse(cache.getString("json", ""), date));
        return;
      } catch (JSONException | RuntimeException ignored) {
      }
    }
    String url =
        "https://aa.usno.navy.mil/api/rstt/oneday?date="
            + date
            + "&coords="
            + PrayerSchedule.LATITUDE
            + ","
            + PrayerSchedule.LONGITUDE
            + "&tz=7";
    call =
        client.newCall(
            new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Zikra-Android/1.0")
                .build());
    call.enqueue(
        new Callback() {
          @Override
          public void onFailure(Call call, IOException e) {
            /* labelled estimate remains */
          }

          @Override
          public void onResponse(Call call, Response response) {
            try (Response source = response) {
              if (!source.isSuccessful() || source.body() == null) return;
              String json = source.body().string();
              SkyDay day = parse(json, date);
              owner.runOnUiThread(
                  () -> {
                    if (closed
                        || version != generation
                        || owner.isFinishing()
                        || owner.isDestroyed()) return;
                    cache.edit().putString("json", json).apply();
                    result.show(day);
                  });
            } catch (IOException | JSONException | RuntimeException ignored) {
            }
          }
        });
  }

  public static SkyDay parse(String json, LocalDate expected) throws JSONException {
    JSONObject root = new JSONObject(json);
    JSONArray coords = root.getJSONObject("geometry").getJSONArray("coordinates");
    if (Math.abs(coords.getDouble(0) - PrayerSchedule.LONGITUDE) > .02
        || Math.abs(coords.getDouble(1) - PrayerSchedule.LATITUDE) > .02)
      throw new JSONException("Location");
    JSONObject data = root.getJSONObject("properties").getJSONObject("data");
    LocalDate date = LocalDate.of(data.getInt("year"), data.getInt("month"), data.getInt("day"));
    if (!date.equals(expected) || data.getDouble("tz") != 7 || data.optBoolean("isdst", false))
      throw new JSONException("Date/time zone");
    LocalTime rise = null, noon = null, set = null;
    JSONArray events = data.getJSONArray("sundata");
    for (int i = 0; i < events.length(); i++) {
      JSONObject e = events.getJSONObject(i);
      switch (e.getString("phen")) {
        case "Rise":
          rise = LocalTime.parse(e.getString("time"));
          break;
        case "Upper Transit":
          noon = LocalTime.parse(e.getString("time"));
          break;
        case "Set":
          set = LocalTime.parse(e.getString("time"));
          break;
      }
    }
    String fraction = data.getString("fracillum");
    if (!fraction.endsWith("%") || rise == null || noon == null || set == null)
      throw new JSONException("Missing events");
    double illumination = Double.parseDouble(fraction.substring(0, fraction.length() - 1)) / 100;
    String phase = data.getString("curphase");
    if (!(phase.startsWith("Waxing")
        || phase.startsWith("Waning")
        || phase.equals("First Quarter")
        || phase.equals("Last Quarter")
        || phase.equals("Full Moon")
        || phase.equals("New Moon"))) throw new JSONException("Unknown phase");
    return new SkyDay(
        date,
        rise,
        noon,
        set,
        illumination,
        phase.startsWith("Waxing") || phase.equals("First Quarter") || phase.equals("New Moon"),
        true);
  }

  @Override
  public void close() {
    closed = true;
    generation++;
    if (call != null) call.cancel();
  }
}
