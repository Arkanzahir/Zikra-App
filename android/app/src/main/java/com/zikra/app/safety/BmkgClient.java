package com.zikra.app.safety;

import android.app.Activity;
import java.io.IOException;
import okhttp3.ResponseBody;

/** Lifecycle facade for independent forecast and earthquake repositories. */
public final class BmkgClient implements AutoCloseable {
  public static final String QUAKE_URL = "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json";
  public static final String WEATHER_URL = "https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4=" + BmkgParser.KEPUTIH_CODE;
  private final OpenMeteoClient weather;
  private final EarthquakeClient earthquake;
  public interface Result<T> {
    void success(T value);
    void failure(String message);
  }
  public BmkgClient(Activity activity) {
    weather = new OpenMeteoClient(activity);
    earthquake = new EarthquakeClient(activity);
  }
  public void reset() { weather.reset(); earthquake.reset(); }
  public void weather(Result<BmkgParser.Forecast> result) { weather.load(result); }
  public void earthquake(Result<EarthquakeReport> result) { earthquake.load(result); }
  /** A malformed server response must not allocate an unbounded JSON string. */
  static String readBounded(ResponseBody body) throws IOException {
    final long limit = 262144;
    if (body.contentLength() > limit) throw new IOException("BMKG response too large");
    body.source().request(limit + 1);
    if (body.source().buffer().size() > limit) throw new IOException("BMKG response too large");
    return body.string();
  }

  @Override public void close() { weather.close(); earthquake.close(); }
}
