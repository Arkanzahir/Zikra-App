package com.zikra.app.reading;

import android.content.Context;
import com.zikra.app.assistant.AssistantConnection;
import java.io.IOException;
import java.util.function.Consumer;
import okhttp3.*;

/** Installation credential goes only to the existing Zikra origin, never to QF. */
public final class QuranSyncGateway {
  public static final class SourceRemoved extends IOException {
    SourceRemoved() { super("Quran source temporarily unavailable"); }
  }
  private QuranSyncGateway() {}
  public static boolean sourceRemoved(Context context) {
    return context.getSharedPreferences("quran_content_sync", 0).getBoolean("source_removed", false);
  }
  public static boolean configured(Context context) {
    try {
      AssistantConnection connection = new AssistantConnection(context);
      HttpUrl url = HttpUrl.parse(connection.endpoint());
      return connection.configured() && url != null && url.isHttps() && url.port() == 443
          && "zikra-ai.vercel.app".equals(url.host()) && "/v1/assistant".equals(url.encodedPath());
    } catch (RuntimeException unavailable) { return false; }
  }
  public static String fetch(Context context, OkHttpClient http, QuranChapters.Chapter chapter,
      int page, Consumer<Call> active) throws Exception {
    HttpUrl url = base().addQueryParameter("chapter", String.valueOf(chapter.id)).addQueryParameter("page", String.valueOf(page)).build();
    String json = request(context, http, url, active);
    QuranSyncPage.validate(json, chapter, page, System.currentTimeMillis());
    available(context); return json;
  }
  public static String fetchMushaf(Context context, OkHttpClient http, int page, Consumer<Call> active) throws Exception {
    String json = request(context, http, base().addQueryParameter("kind", "mushaf")
        .addQueryParameter("page", String.valueOf(page)).build(), active);
    com.zikra.app.reading.mushaf.MushafDocument.parse(json, page);
    QuranSyncPage.validateMetadata(json, System.currentTimeMillis());
    available(context); return json;
  }
  public static int locate(Context context, OkHttpClient http, int chapter, int verse,
      int position, int preferred, int juz, Consumer<Call> active) throws Exception {
    HttpUrl url = base().addQueryParameter("kind", "locate").addQueryParameter("chapter", String.valueOf(chapter))
        .addQueryParameter("verse", String.valueOf(verse)).addQueryParameter("position", String.valueOf(position))
        .addQueryParameter("preferred", String.valueOf(preferred)).addQueryParameter("juz", String.valueOf(juz)).build();
    String json = request(context, http, url, active);
    QuranSyncPage.validateMetadata(json, System.currentTimeMillis());
    int page = new org.json.JSONObject(json).getInt("page_number");
    if (page < 1 || page > 604) throw new IOException("Invalid Mushaf location");
    available(context); return page;
  }
  private static HttpUrl.Builder base() { return HttpUrl.parse("https://zikra-ai.vercel.app/v1/quran").newBuilder(); }
  private static void available(Context context) {
    context.getSharedPreferences("quran_content_sync", 0).edit().putBoolean("source_removed", false).commit();
  }
  private static String request(Context context, OkHttpClient http, HttpUrl url, Consumer<Call> active) throws Exception {
    if (!configured(context)) throw new IOException("Quran synchronization unavailable");
    String access = new AssistantConnection(context).accessCode();
    Call call = http.newCall(new Request.Builder().url(url).header("Authorization", "Bearer " + access)
        .header("Accept", "application/json").header("User-Agent", "Zikra-Quran/1.0").build());
    active.accept(call);
    try (Response response = call.execute()) {
      if (!response.isSuccessful() || response.body() == null) {
        if (response.body() != null && response.code() == 503) {
          String error = QuranSyncStore.readBounded(response.body().byteStream(), 4096);
          String code = new org.json.JSONObject(error).optString("error");
          if ("quran_resource_unavailable".equals(code) || "quran_source_access_unavailable".equals(code)) {
            context.getSharedPreferences("quran_content_sync", 0).edit().putBoolean("source_removed", true).commit();
            throw new SourceRemoved();
          }
        }
        throw new IOException("Quran source unavailable");
      }
      response.body().source().request(524289);
      if (response.body().source().buffer().size() > 524288) throw new IOException("Quran page budget exceeded");
      return response.body().string();
    }
  }
}
