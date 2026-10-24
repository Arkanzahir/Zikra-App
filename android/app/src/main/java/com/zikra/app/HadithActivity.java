package com.zikra.app;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;
import com.zikra.app.discovery.HadithCollection;
import com.zikra.app.discovery.HadithStore;
import com.zikra.app.ui.FeatureHeader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Searchable small local collection; saved items are persisted on this device. */
public final class HadithActivity extends BaseActivity {
  private final String[] categories = {"Semua", "Tersimpan"};
  private String category = "Semua";
  private EditText search;
  private Set<String> saved;
  private HadithStore store;
  private int offset, generation;
  private boolean downloading, bukhariInstalled, muslimInstalled;
  private android.app.AlertDialog dialog;
  private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
  private Runnable pendingSearch;
  private final java.util.ArrayList<Button> tabs = new java.util.ArrayList<>();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_hadith);
    store = new HadithStore(this);
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(R.id.toolbarBack, R.id.detailTitle, R.id.featureSubtitle, 0, "Hadis", null, 0, null);
    setupScreen("Hadis");
    getWindow().setStatusBarColor(Color.parseColor("#F8F9F4"));
    getWindow().setNavigationBarColor(Color.parseColor("#F8F9F4"));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    saved =
        new HashSet<>(
            getSharedPreferences("zikra_preferences", MODE_PRIVATE)
                .getStringSet("hadith_bookmarks", new HashSet<>()));
    if (state != null) category = state.getString("category", "Semua");
    if (state != null) offset = state.getInt("offset", 0);
    String requestedBook = getIntent().getStringExtra("book");
    if (state == null && "bukhari".equals(requestedBook)) category = "Bukhari";
    if (state == null && "muslim".equals(requestedBook)) category = "Muslim";
    search = findViewById(R.id.hadithSearch);
    if (state != null) search.setText(state.getString("query", ""));
    LinearLayout container = findViewById(R.id.hadithTabs);
    for (String value : categories) {
      Button tab = new Button(this);
      tab.setText(value);
      tab.setAllCaps(false);
      tab.setTextSize(13);
      tab.setStateListAnimator(null);
      tab.setElevation(0);
      tab.setMinimumHeight(dp(48));
      tab.setMinHeight(dp(48));
      tab.setMinWidth(0);
      tab.setMinimumWidth(0);
      tab.setPadding(dp(16), 0, dp(16), 0);
      LinearLayout.LayoutParams p =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      p.setMargins(dp(4), 0, dp(4), 0);
      tab.setLayoutParams(p);
      tab.setOnClickListener(
          v -> {
            category = value;
            offset = 0;
            render();
          });
      tabs.add(tab);
      container.addView(tab);
    }
    search.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            offset = 0;
            render();
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });
    findViewById(R.id.hadithHero).setOnClickListener(v -> showDetail(HadithCollection.ENTRIES[1]));
    NavDock.attach(this, (FrameLayout) findViewById(R.id.hadithRoot), "home");
    com.zikra.app.ui.DailyUi.chrome(this, R.color.daily_paper);
    ((com.zikra.app.ui.BookCoverView) findViewById(R.id.dailyCoverMuslim)).setMuslim(true);
    if (getResources().getConfiguration().fontScale > 1.3f) {
      ((LinearLayout) findViewById(R.id.hadithBooks)).setOrientation(LinearLayout.VERTICAL);
      for (int book : new int[]{R.id.dailyBookBukhari, R.id.dailyBookMuslim}) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        findViewById(book).setLayoutParams(params);
      }
    }
    findViewById(R.id.dailyBookBukhari).setOnClickListener(v -> chooseCollection("Bukhari"));
    findViewById(R.id.dailyBookMuslim).setOnClickListener(v -> chooseCollection("Muslim"));
    findViewById(R.id.dailyHadithResume)
        .setOnClickListener(
            v -> {
              String id =
                  getSharedPreferences("zikra_preferences", 0)
                      .getString("last_hadith_id", "intentions");
              startActivity(new android.content.Intent(this, HadithReaderActivity.class).putExtra("entry", id));
            });
    findViewById(R.id.hadithPrevious).setOnClickListener(v -> { offset -= HadithStore.PAGE_SIZE; render(); });
    findViewById(R.id.hadithNext).setOnClickListener(v -> { offset += HadithStore.PAGE_SIZE; render(); });
    findViewById(R.id.hadithDownloads).setOnClickListener(v -> chooseDownload());
    findViewById(R.id.hadithSource).setOnClickListener(v -> {
      String notice;
      try (java.io.InputStream input = getResources().openRawResource(R.raw.hadith_source_notice);
          java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
        byte[] buffer = new byte[2048]; int size;
        while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
        notice = output.toString("UTF-8");
      } catch (java.io.IOException unavailable) { notice = getString(R.string.hadith_source_notice); }
      dialog = new android.app.AlertDialog.Builder(this).setTitle(R.string.hadith_source_title)
          .setMessage(notice).setPositiveButton(android.R.string.ok, null).show();
    });
    findViewById(R.id.hadithCancelDownload).setOnClickListener(v -> store.cancel());
    render();
  }

  private void chooseCollection(String value) {
    category = value;
    offset = 0;
    search.setText("");
    render();
    findViewById(R.id.hadithCount)
        .post(
            () ->
                findViewById(R.id.hadithCount)
                    .requestRectangleOnScreen(
                        new android.graphics.Rect(
                            0, 0, findViewById(R.id.hadithCount).getWidth(), dp(160)),
                        false));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (saved != null) {
      saved =
          new HashSet<>(
              getSharedPreferences("zikra_preferences", 0)
                  .getStringSet("hadith_bookmarks", new HashSet<>()));
      String id =
          getSharedPreferences("zikra_preferences", 0).getString("last_hadith_id", "intentions");
      store.get(id, new HadithStore.Result<HadithCollection.Entry>() {
        public void show(HadithCollection.Entry entry) {
          if (entry == null) {
            ((TextView) findViewById(R.id.dailyHadithResume)).setText(R.string.hadith_resume_missing);
            return;
          }
          ((TextView) findViewById(R.id.dailyHadithResume))
              .setText(
                  getString(R.string.daily_continue_hadith)
                      + "\n"
                      + entry.title
                      + " · "
                      + entry.collection);
        }
        public void failed() { }
      });
      render();
    }
  }

  private void render() {
    if (store == null || downloading) return;
    for (Button tab : tabs) {
      boolean active = category.contentEquals(tab.getText());
      tab.setSelected(active);
      GradientDrawable background = new GradientDrawable();
      background.setColor(getColor(active ? R.color.daily_ink : android.R.color.transparent));
      background.setCornerRadius(dp(14));
      if (!active) background.setStroke(dp(1), getColor(R.color.daily_line));
      tab.setBackground(background);
      tab.setTextColor(getColor(active ? android.R.color.white : R.color.hadith_secondary));
    }
    findViewById(R.id.dailyBookBukhari).setActivated("Bukhari".equals(category));
    findViewById(R.id.dailyBookMuslim).setActivated("Muslim".equals(category));
    if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
    int token = ++generation;
    pendingSearch = () -> {
      ((TextView) findViewById(R.id.hadithStatus)).setText(R.string.hadith_loading);
      store.query(search.getText().toString(), category, saved, offset, new HadithStore.Result<HadithStore.Page>() {
        public void show(HadithStore.Page page) {
          if (token != generation) return;
          showPage(page);
        }
        public void failed() {
          if (token != generation) return;
          ((TextView) findViewById(R.id.hadithStatus)).setText(R.string.hadith_load_failed);
        }
      });
    };
    handler.postDelayed(pendingSearch, 200);
  }

  private void showPage(HadithStore.Page page) {
    bukhariInstalled = page.bukhariInstalled; muslimInstalled = page.muslimInstalled;
    offset = page.offset;
    LinearLayout list = findViewById(R.id.hadithList); list.removeAllViews();
    for (HadithCollection.Entry entry : page.entries) {
      View row = getLayoutInflater().inflate(R.layout.hadith_item, list, false);
      ((TextView) row.findViewById(R.id.hadithItemTitle)).setText(entry.title);
      ((TextView) row.findViewById(R.id.hadithItemSummary)).setText(entry.meaning);
      ((TextView) row.findViewById(R.id.hadithItemReference)).setText(entry.collection.toUpperCase(Locale.ROOT));
      ImageView marker = row.findViewById(R.id.hadithItemSaved);
      marker.setImageResource(saved.contains(entry.id) ? R.drawable.reading_ic_bookmark_filled : R.drawable.home_ic_chevron);
      marker.setImageTintList(android.content.res.ColorStateList.valueOf(
          getColor(saved.contains(entry.id) ? R.color.hadith_bronze : R.color.daily_ink)));
      row.findViewById(R.id.hadithRowDivider).setVisibility(
          list.getChildCount() + 1 == page.entries.size() ? View.GONE : View.VISIBLE);
      row.setContentDescription(
          entry.title + ". " + entry.source + (saved.contains(entry.id) ? ". Tersimpan" : ""));
      row.setOnClickListener(v -> showDetail(entry));
      list.addView(row);
    }
    ((TextView) findViewById(R.id.hadithCount))
        .setText(page.total == 0 ? getString(R.string.hadith_no_results)
            : getString(R.string.hadith_page_range, page.offset + 1, page.offset + page.entries.size(), page.total));
    ((TextView) findViewById(R.id.hadithStatus)).setText(
        getString(R.string.hadith_search_scope)
            + (page.unavailableSaved == 0 ? "" : "\n" + getString(R.string.hadith_missing_saved, page.unavailableSaved)));
    findViewById(R.id.hadithPager).setVisibility(page.total > HadithStore.PAGE_SIZE ? View.VISIBLE : View.GONE);
    findViewById(R.id.hadithPrevious).setEnabled(page.offset > 0);
    findViewById(R.id.hadithNext).setEnabled(page.offset + page.entries.size() < page.total);
    findViewById(R.id.hadithEmpty).setVisibility(page.total == 0 ? View.VISIBLE : View.GONE);
    if (page.total == 0 && category.equals("Tersimpan"))
      ((TextView) findViewById(R.id.hadithEmpty))
          .setText(R.string.hadith_saved_empty);
    else
      ((TextView) findViewById(R.id.hadithEmpty))
          .setText(R.string.hadith_collection_empty);
  }

  private void chooseDownload() {
    if (downloading) return;
    dialog = new android.app.AlertDialog.Builder(this).setTitle(R.string.hadith_downloads)
        .setItems(new String[]{getString(R.string.hadith_book_download, "Bukhari", 6638, "13,4"),
            getString(R.string.hadith_book_download, "Muslim", 4930, "12,0")},
            (d, index) -> manageDownload(index == 0 ? "bukhari" : "muslim"))
        .setNegativeButton(R.string.daily_cancel, null).show();
  }

  private void manageDownload(String book) {
    boolean installed = "bukhari".equals(book) ? bukhariInstalled : muslimInstalled;
    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this)
        .setTitle("bukhari".equals(book) ? R.string.daily_book_bukhari : R.string.daily_book_muslim)
        .setMessage(R.string.hadith_package_info)
        .setNegativeButton(R.string.daily_cancel, null)
        .setPositiveButton(installed ? R.string.hadith_redownload : R.string.hadith_download,
            (d, w) -> beginDownload(book));
    if (installed) builder.setNeutralButton(R.string.hadith_remove_package,
        (d, w) -> store.remove(book, new HadithStore.Result<Boolean>() {
          public void show(Boolean removed) { render(); }
          public void failed() { toast(getString(R.string.hadith_load_failed)); }
        }));
    dialog = builder.show();
  }

  private void beginDownload(String book) {
    downloading = true; generation++;
    if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
    findViewById(R.id.hadithDownloads).setEnabled(false);
    findViewById(R.id.hadithCancelDownload).setVisibility(View.VISIBLE);
    store.download(book, new HadithStore.Download() {
      public void progress(int percent) {
        ((TextView) findViewById(R.id.hadithStatus)).setText(getString(R.string.hadith_downloading, percent));
      }
      private void end() {
        downloading = false;
        findViewById(R.id.hadithDownloads).setEnabled(true);
        findViewById(R.id.hadithCancelDownload).setVisibility(View.GONE);
      }
      public void finished() { end(); offset = 0; render(); toast(getString(R.string.hadith_downloaded)); }
      public void failed(String code) {
        end();
        ((TextView) findViewById(R.id.hadithStatus)).setText(
            "cancelled".equals(code) ? R.string.hadith_download_cancelled
                : "storage".equals(code) ? R.string.hadith_storage_error : R.string.hadith_download_error);
      }
    });
  }

  private void showDetail(HadithCollection.Entry entry) {
    startActivity(
        new android.content.Intent(this, HadithReaderActivity.class).putExtra("entry", entry.id));
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("category", category);
    state.putString("query", search.getText().toString());
    state.putInt("offset", offset);
    super.onSaveInstanceState(state);
  }

  @Override protected void onDestroy() {
    if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
    if (dialog != null) dialog.dismiss();
    if (store != null) store.close();
    super.onDestroy();
  }
}
