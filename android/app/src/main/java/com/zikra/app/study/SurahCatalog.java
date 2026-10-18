package com.zikra.app.study;

import android.os.Handler;
import android.os.Looper;
import com.zikra.app.reading.SurahData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Only an in-memory chapter catalog. Saved user targets are independent of this response. */
public final class SurahCatalog implements AutoCloseable {
  public static final class Chapter {
    public final int id, versesCount;
    public final String name;

    public Chapter(int id, String name, int count) {
      if (id < 1
          || id > 114
          || count < 1
          || count > 286
          || name == null
          || name.trim().isEmpty()
          || name.length() > 100) throw new IllegalArgumentException("Metadata surah tidak valid");
      this.id = id;
      this.name = name;
      versesCount = count;
    }
  }

  public interface Result {
    void ready(List<Chapter> value);

    void failed();
  }

  private static final ExecutorService WORK = Executors.newFixedThreadPool(2);
  private static final OkHttpClient HTTP =
      new OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(15, TimeUnit.SECONDS)
          .callTimeout(25, TimeUnit.SECONDS)
          .build();
  private final Handler main = new Handler(Looper.getMainLooper());
  private volatile boolean closed;
  private volatile int generation;
  private volatile Call active;

  public static List<Chapter> local() {
    List<Chapter> list = new ArrayList<>();
    for (SurahData surah : SurahData.ALL)
      list.add(new Chapter(surah.number, surah.name, surah.arabic.length));
    return Collections.unmodifiableList(list);
  }

  public static List<Chapter> parse(String body) throws JSONException {
    JSONArray rows = new JSONObject(body).getJSONArray("chapters");
    if (rows.length() != 114) throw new JSONException("Katalog tidak lengkap");
    List<Chapter> result = new ArrayList<>();
    int total = 0;
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      Chapter chapter =
          new Chapter(row.getInt("id"), row.getString("name_simple"), row.getInt("verses_count"));
      if (chapter.id != i + 1) throw new JSONException("Urutan surah tidak valid");
      total += chapter.versesCount;
      result.add(chapter);
    }
    if (total != 6236) throw new JSONException("Jumlah ayat tidak valid");
    return Collections.unmodifiableList(result);
  }

  public void load(Result result) {
    if (closed) return;
    int token = ++generation;
    Call old = active;
    if (old != null) old.cancel();
    WORK.execute(
        () -> {
          Call call =
              HTTP.newCall(
                  new Request.Builder()
                      .url("https://api.quran.com/api/v4/chapters?language=id")
                      .header("Accept", "application/json")
                      .header("User-Agent", "Zikra-Android/1.0")
                      .build());
          active = call;
          if (closed || generation != token) {
            call.cancel();
            return;
          }
          try (Response response = call.execute()) {
            if (!response.isSuccessful() || response.body() == null)
              throw new IOException("Quran catalog unavailable");
            response.body().source().request(262145);
            if (response.body().source().buffer().size() > 262144)
              throw new IOException("Catalog too large");
            List<Chapter> values = parse(response.body().source().readUtf8());
            main.post(
                () -> {
                  if (!closed && generation == token) result.ready(values);
                });
          } catch (IOException | JSONException | IllegalArgumentException error) {
            main.post(
                () -> {
                  if (!closed && generation == token) result.failed();
                });
          } finally {
            if (active == call) active = null;
          }
        });
  }

  @Override
  public void close() {
    closed = true;
    generation++;
    if (active != null) active.cancel();
    main.removeCallbacksAndMessages(null);
  }
}
