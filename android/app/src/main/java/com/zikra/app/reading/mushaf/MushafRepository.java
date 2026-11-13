package com.zikra.app.reading.mushaf;

import android.content.Context;
import android.graphics.Typeface;
import android.os.*;
import android.util.AtomicFile;
import com.zikra.app.reading.QuranSyncGateway;
import com.zikra.app.reading.QuranSyncPage;
import com.zikra.app.reading.QuranSyncStore;
import java.io.*;
import java.util.concurrent.*;
import okhttp3.*;
import org.json.*;

/** Bounded Content Sync page cache; ordinary API responses remain online-only. */
public final class MushafRepository implements AutoCloseable {
  public interface Result {
    void ready(MushafDocument document, Typeface font);

    void failed();
  }

  public interface Located {
    void ready(int page);

    void failed();
  }

  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final OkHttpClient http =
      new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(55, TimeUnit.SECONDS)
          .callTimeout(60, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
          .retryOnConnectionFailure(false).build();
  private static final Object CACHE = new Object();
  private final File fonts, pages;
  private final Context context;
  private volatile int generation;
  private volatile boolean closed;

  public MushafRepository(Context context) {
    this.context = context.getApplicationContext();
    fonts = new File(context.getCacheDir(), "qcf-v2-fonts");
    pages = new File(context.getFilesDir(), "quran-mushaf-sync-pages");
  }

  private byte[] fetch(String url, int limit) throws IOException {
    try (Response response =
        http.newCall(
                new Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "Zikra-Android/1.1 (+https://github.com/Arkanzahir/Zikra-App)")
                    .build())
            .execute()) {
      if (response.code() == 401 || response.code() == 403) throw new MushafFontCache.AccessDenied();
      if (!response.isSuccessful() || response.body() == null) throw new IOException("HTTP");
      response.body().source().request(limit + 1L);
      if (response.body().source().buffer().size() > limit) throw new IOException("Too large");
      return response.body().bytes();
    }
  }

  private String json(String path) throws IOException {
    return new String(
        fetch("https://api.quran.com/api/v4/" + path, 1024 * 1024),
        java.nio.charset.StandardCharsets.UTF_8);
  }

  public void locate(int chapter, int verse, int preferred, Located result) {
    locate(chapter, verse, preferred, 0, result);
  }

  public void locateWord(int chapter, int verse, int position, Located result) {
    if (position < 1) throw new IllegalArgumentException("Invalid word position");
    locate(chapter, verse, 0, position, result);
  }

  private void locate(int chapter, int verse, int preferred, int position, Located result) {
    com.zikra.app.audio.Recitation.globalVerse(chapter, verse);
    int token = ++generation;
    http.dispatcher().cancelAll();
    worker.execute(
        () -> {
          if (closed || token != generation) return;
          try {
            if (QuranSyncGateway.configured(context)) {
              try {
                int found = QuranSyncGateway.locate(context, http, chapter, verse, position, preferred, 0,
                    call -> { if (closed || token != generation) call.cancel(); });
                main.post(() -> { if (!closed && token == generation) result.ready(found); });
                return;
              } catch (QuranSyncGateway.SourceRemoved removed) { throw removed; }
              catch (Exception unavailable) {
                int cached = cachedLocation(chapter, verse, position, preferred);
                if (cached > 0) {
                  main.post(() -> { if (!closed && token == generation) result.ready(cached); });
                  return;
                }
              }
            }
            if (QuranSyncGateway.sourceRemoved(context)) throw new IOException("Quran source unavailable");
            JSONObject root =
                new JSONObject(
                    json("verses/by_key/" + chapter + ":" + verse + "?mushaf=1&words=true"));
            JSONArray words = root.getJSONObject("verse").getJSONArray("words");
            int page = position == 0 ? words.getJSONObject(0).getInt("page_number") : 0;
            for (int i = 0; i < words.length(); i++) {
              JSONObject word = words.getJSONObject(i);
              if (position > 0
                  ? word.getInt("position") == position
                  : word.getInt("page_number") == preferred) page = word.getInt("page_number");
            }
            if (page < 1 || page > 604) throw new IOException("Page range");
            int found = page;
            main.post(
                () -> {
                  if (!closed && token == generation) result.ready(found);
                });
          } catch (Exception failure) {
            main.post(
                () -> {
                  if (!closed && token == generation) result.failed();
                });
          }
        });
  }

  public void locateJuz(int juz, Located result) {
    if (juz < 1 || juz > 30) throw new IllegalArgumentException("Invalid juz");
    int token = ++generation;
    http.dispatcher().cancelAll();
    worker.execute(
        () -> {
          if (closed || token != generation) return;
          try {
            if (QuranSyncGateway.configured(context)) {
              try {
                int found = QuranSyncGateway.locate(context, http, 0, 0, 0, 0, juz,
                    call -> { if (closed || token != generation) call.cancel(); });
                main.post(() -> { if (!closed && token == generation) result.ready(found); });
                return;
              } catch (QuranSyncGateway.SourceRemoved removed) { throw removed; }
              catch (Exception unavailable) { /* Online-only compatibility path below. */ }
            }
            if (QuranSyncGateway.sourceRemoved(context)) throw new IOException("Quran source unavailable");
            JSONObject root =
                new JSONObject(json("verses/by_juz/" + juz + "?mushaf=1&words=true&per_page=1"));
            JSONObject verse = root.getJSONArray("verses").getJSONObject(0);
            if (verse.getInt("juz_number") != juz) throw new IOException("Juz mismatch");
            int found = verse.getJSONArray("words").getJSONObject(0).getInt("page_number");
            if (found < 1 || found > 604) throw new IOException("Page range");
            main.post(
                () -> {
                  if (!closed && token == generation) result.ready(found);
                });
          } catch (Exception failure) {
            main.post(
                () -> {
                  if (!closed && token == generation) result.failed();
                });
          }
        });
  }

  public void load(int page, Result result) {
    if (page < 1 || page > 604) throw new IllegalArgumentException("Invalid page");
    int token = ++generation;
    http.dispatcher().cancelAll();
    worker.execute(
        () -> {
          if (closed || token != generation) return;
          try {
            MushafDocument data =
                MushafDocument.parse(
                    pageData(page, token),
                    page);
            if (closed || token != generation) return;
            ensureHeadingFont();
            ensureBasmalahFont();
            if (!fonts.isDirectory() && !fonts.mkdirs()) throw new IOException("Font folder");
            File file = new File(fonts, "p" + page + ".ttf");
            Typeface font = MushafFontCache.load(file,
                () -> fetch("https://verses.quran.foundation/fonts/quran/hafs/v2/ttf/p" + page + ".ttf", 3 * 1024 * 1024),
                candidate -> {
                  new QcfGlyphMetrics(candidate);
                  return Typeface.createFromFile(candidate);
                });
            Typeface loaded = font;
            File[] cached = fonts.listFiles((dir, name) -> name.endsWith(".ttf"));
            if (cached != null && cached.length > 8) {
              java.util.Arrays.sort(cached, java.util.Comparator.comparingLong(File::lastModified));
              for (int i = 0, count = cached.length; count > 8 && i < cached.length; i++)
                if (!cached[i].equals(file) && cached[i].delete()) count--;
            }
            main.post(
                () -> {
                  if (!closed && token == generation) result.ready(data, loaded);
                });
          } catch (Exception failure) {
            main.post(
                () -> {
                  if (!closed && token == generation) result.failed();
                });
          }
        });
  }

  private String cachedPage(int page) throws Exception {
    synchronized (CACHE) {
      String json = QuranSyncStore.readBounded(new AtomicFile(new File(pages, page + ".json")).openRead(), 524288);
      QuranSyncPage.validateMetadata(json, System.currentTimeMillis());
      MushafDocument.parse(json, page);
      return json;
    }
  }

  private String pageData(int page, int token) throws Exception {
    String cached = null;
    try { cached = cachedPage(page); } catch (Exception missing) { /* No valid synchronized page. */ }
    if (cached != null && !QuranSyncGateway.sourceRemoved(context)
        && QuranSyncPage.fresh(QuranSyncPage.validateMetadata(cached, System.currentTimeMillis()), System.currentTimeMillis())) return cached;
    if (QuranSyncGateway.configured(context)) {
      try {
        String json = QuranSyncGateway.fetchMushaf(context, http, page,
            call -> { if (closed || token != generation) call.cancel(); });
        if (closed || token != generation) throw new IOException("Cancelled");
        synchronized (CACHE) {
          if (!pages.isDirectory() && !pages.mkdirs()) return json;
          AtomicFile target = new AtomicFile(new File(pages, page + ".json"));
          FileOutputStream out = null;
          try {
            out = target.startWrite(); out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)); target.finishWrite(out);
          } catch (IOException failedWrite) { if (out != null) target.failWrite(out); }
          File[] files = pages.listFiles((dir, name) -> name.matches("[1-9][0-9]{0,2}\\.json"));
          if (files != null && files.length > 8) {
            java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < files.length - 8; i++) files[i].delete();
          }
        }
        return json;
      } catch (QuranSyncGateway.SourceRemoved removed) { throw removed; }
      catch (Exception unavailable) { if (closed || token != generation) throw unavailable; }
    }
    if (QuranSyncGateway.sourceRemoved(context)) throw new IOException("Quran source unavailable");
    if (cached != null) return cached;
    return json("verses/by_page/" + page + "?mushaf=1&words=true&word_fields=code_v2,text_uthmani&per_page=50");
  }

  private int cachedLocation(int chapter, int verse, int position, int preferred) {
    if (QuranSyncGateway.sourceRemoved(context)) return 0;
    File[] files = pages.listFiles((dir, name) -> name.matches("[1-9][0-9]{0,2}\\.json"));
    if (files == null || files.length > 8) return 0;
    java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
    int fallback = 0;
    for (File file : files) {
      try {
        int page = Integer.parseInt(file.getName().replace(".json", ""));
        MushafDocument document = MushafDocument.parse(cachedPage(page), page);
        for (MushafPage.Line line : document.page.lines) for (MushafPage.Word word : line.words)
          if (word.chapter == chapter && word.verse == verse && (position > 0 ? word.position == position : page == preferred || word.position == 1)) {
            if (page == preferred) return page;
            fallback = fallback == 0 ? page : Math.min(page, fallback);
          }
      } catch (Exception invalid) { /* Ignore damaged cached pages without deleting them. */ }
    }
    return fallback;
  }

  @Override
  public void close() {
    closed = true;
    generation++;
    http.dispatcher().cancelAll();
    worker.shutdownNow();
  }

  private void ensureHeadingFont() throws IOException {
    File file = new File(fonts.getParentFile(), "qcf-title-font/surah-names-v1.ttf");
    MushafFontCache.load(file,
        () -> fetch("https://verses.quran.foundation/fonts/quran/surah-names/v1/sura_names.ttf", 1024 * 1024),
        SurahHeadingFont::new);
  }

  private void ensureBasmalahFont() throws IOException {
    File file = new File(fonts.getParentFile(), "qcf-basmalah/p1.ttf");
    MushafFontCache.load(file,
        () -> fetch("https://verses.quran.foundation/fonts/quran/hafs/v2/ttf/p1.ttf", 3 * 1024 * 1024),
        BasmalahRenderer::new);
  }
}
