package com.zikra.app.reading.mushaf;

import android.content.Context;
import android.graphics.Typeface;
import android.os.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.concurrent.*;
import okhttp3.*;
import org.json.*;

/** Online pages only. No raw Content API data is persisted or bundled as an offline corpus. */
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
      new OkHttpClient.Builder().callTimeout(35, TimeUnit.SECONDS).build();
  private final File fonts;
  private volatile int generation;
  private volatile boolean closed;

  public MushafRepository(Context context) {
    fonts = new File(context.getCacheDir(), "qcf-v2-fonts");
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
    com.zikra.app.audio.Recitation.globalVerse(chapter, verse);
    int token = ++generation;
    http.dispatcher().cancelAll();
    worker.execute(
        () -> {
          if (closed || token != generation) return;
          try {
            JSONObject root =
                new JSONObject(
                    json("verses/by_key/" + chapter + ":" + verse + "?mushaf=1&words=true"));
            JSONArray words = root.getJSONObject("verse").getJSONArray("words");
            int page = words.getJSONObject(0).getInt("page_number");
            for (int i = 0; i < words.length(); i++)
              if (words.getJSONObject(i).getInt("page_number") == preferred) page = preferred;
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
                    json(
                        "verses/by_page/"
                            + page
                            + "?mushaf=1&words=true&word_fields=code_v2,text_uthmani&per_page=50"),
                    page);
            if (closed || token != generation) return;
            ensureHeadingFont();
            ensureBasmalahFont();
            if (!fonts.isDirectory() && !fonts.mkdirs()) throw new IOException("Font folder");
            File file = new File(fonts, "p" + page + ".ttf");
            Typeface font = null;
            if (file.isFile() && System.currentTimeMillis() - file.lastModified() < 7L * 86400000) {
              try {
                font = Typeface.createFromFile(file);
              } catch (RuntimeException ignored) {
              }
            }
            if (font == null) {
              byte[] bytes =
                  fetch(
                      "https://verses.quran.foundation/fonts/quran/hafs/v2/ttf/p" + page + ".ttf",
                      3 * 1024 * 1024);
              AtomicFile atomic = new AtomicFile(file);
              FileOutputStream out = atomic.startWrite();
              try {
                out.write(bytes);
                atomic.finishWrite(out);
              } catch (IOException error) {
                atomic.failWrite(out);
                throw error;
              }
              font = Typeface.createFromFile(file);
            }
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

  @Override
  public void close() {
    closed = true;
    generation++;
    http.dispatcher().cancelAll();
    worker.shutdownNow();
  }

  private void ensureHeadingFont() throws IOException {
    File folder = new File(fonts.getParentFile(), "qcf-title-font");
    if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Title font folder");
    File file = new File(folder, "surah-names-v1.ttf");
    if (file.isFile() && System.currentTimeMillis() - file.lastModified() < 7L * 86400000) {
      try {
        new SurahHeadingFont(file);
        return;
      } catch (RuntimeException invalid) {
      }
    }
    byte[] bytes =
        fetch(
            "https://verses.quran.foundation/fonts/quran/surah-names/v1/sura_names.ttf",
            1024 * 1024);
    AtomicFile atomic = new AtomicFile(file);
    FileOutputStream out = atomic.startWrite();
    try {
      out.write(bytes);
      atomic.finishWrite(out);
    } catch (IOException error) {
      atomic.failWrite(out);
      throw error;
    }
    new SurahHeadingFont(file);
  }

  private void ensureBasmalahFont() throws IOException {
    File folder = new File(fonts.getParentFile(), "qcf-basmalah");
    if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Basmalah font folder");
    File file = new File(folder, "p1.ttf");
    if (file.isFile() && System.currentTimeMillis() - file.lastModified() < 7L * 86400000) {
      try {
        new BasmalahRenderer(file);
        return;
      } catch (IOException | RuntimeException invalid) {
      }
    }
    byte[] bytes =
        fetch("https://verses.quran.foundation/fonts/quran/hafs/v2/ttf/p1.ttf", 3 * 1024 * 1024);
    AtomicFile atomic = new AtomicFile(file);
    FileOutputStream out = atomic.startWrite();
    try {
      out.write(bytes);
      atomic.finishWrite(out);
    } catch (IOException error) {
      atomic.failWrite(out);
      throw error;
    }
    new BasmalahRenderer(file);
  }
}
