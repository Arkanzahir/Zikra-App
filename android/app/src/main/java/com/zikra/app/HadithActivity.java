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
import com.zikra.app.discovery.DiscoveryUi;
import com.zikra.app.discovery.HadithCollection;
import com.zikra.app.ui.FeatureHeader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Searchable small local collection; saved items are persisted on this device. */
public final class HadithActivity extends BaseActivity {
  private final String[] categories = {"Semua", "Bukhari", "Muslim", "Tersimpan"};
  private String category = "Semua";
  private EditText search;
  private Set<String> saved;
  private final java.util.ArrayList<Button> tabs = new java.util.ArrayList<>();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_hadith);
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
    search = findViewById(R.id.hadithSearch);
    if (state != null) search.setText(state.getString("query", ""));
    LinearLayout container = findViewById(R.id.hadithTabs);
    for (String value : categories) {
      Button tab = new Button(this);
      tab.setText(value);
      tab.setAllCaps(false);
      tab.setTextSize(14);
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
            render();
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });
    findViewById(R.id.hadithHero).setOnClickListener(v -> showDetail(HadithCollection.ENTRIES[1]));
    NavDock.attach(this, (FrameLayout) findViewById(R.id.hadithRoot), "home");
    com.zikra.app.ui.DailyUi.chrome(this, R.color.daily_paper);
    ((com.zikra.app.ui.BookCoverView) findViewById(R.id.dailyCoverMuslim)).setMuslim(true);
    findViewById(R.id.dailyBookBukhari).setOnClickListener(v -> chooseCollection("Bukhari"));
    findViewById(R.id.dailyBookMuslim).setOnClickListener(v -> chooseCollection("Muslim"));
    findViewById(R.id.dailyHadithResume)
        .setOnClickListener(
            v -> {
              String id =
                  getSharedPreferences("zikra_preferences", 0)
                      .getString("last_hadith_id", "intentions");
              for (HadithCollection.Entry entry : HadithCollection.ENTRIES)
                if (entry.id.equals(id)) {
                  showDetail(entry);
                  return;
                }
              showDetail(HadithCollection.ENTRIES[0]);
            });
    render();
  }

  private void chooseCollection(String value) {
    category = value;
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
      for (HadithCollection.Entry entry : HadithCollection.ENTRIES)
        if (entry.id.equals(id))
          ((TextView) findViewById(R.id.dailyHadithResume))
              .setText(
                  getString(R.string.daily_continue_hadith)
                      + "\n"
                      + entry.title
                      + " · "
                      + entry.source);
      render();
    }
  }

  private void render() {
    for (Button tab : tabs) {
      boolean active = category.contentEquals(tab.getText());
      tab.setSelected(active);
      tab.setBackground(DiscoveryUi.background(this, active ? "#15332F" : "#EDF2EB", 22));
      tab.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#52665F"));
    }
    LinearLayout list = findViewById(R.id.hadithList);
    list.removeAllViews();
    String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
    int count = 0;
    for (HadithCollection.Entry entry : HadithCollection.ENTRIES) {
      if (category.equals("Tersimpan") && !saved.contains(entry.id)) continue;
      if (!category.equals("Semua")
          && !category.equals("Tersimpan")
          && !entry.collection.contains(category)) continue;
      String haystack =
          (entry.title + " " + entry.meaning + " " + entry.arabic + " " + entry.source)
              .toLowerCase(Locale.ROOT);
      if (!haystack.contains(query)) continue;
      View row = getLayoutInflater().inflate(R.layout.hadith_item, list, false);
      ((TextView) row.findViewById(R.id.hadithItemTitle)).setText(entry.title);
      ((TextView) row.findViewById(R.id.hadithItemSummary)).setText(entry.meaning);
      ((TextView) row.findViewById(R.id.hadithItemSaved))
          .setText(saved.contains(entry.id) ? "★" : "›");
      row.setContentDescription(
          entry.title + ". " + entry.source + (saved.contains(entry.id) ? ". Tersimpan" : ""));
      row.setOnClickListener(v -> showDetail(entry));
      list.addView(row);
      count++;
    }
    ((TextView) findViewById(R.id.hadithCount))
        .setText(
            category.equals("Tersimpan")
                ? "Hadis tersimpan · " + count
                : "Pilihan hadis · " + count);
    findViewById(R.id.hadithEmpty).setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    if (count == 0 && category.equals("Tersimpan"))
      ((TextView) findViewById(R.id.hadithEmpty))
          .setText("Belum ada hadis tersimpan yang cocok.\nBuka hadis, lalu pilih Simpan.");
    else
      ((TextView) findViewById(R.id.hadithEmpty))
          .setText("Belum ada hadis yang cocok.\nCoba kata lain atau kategori Semua.");
  }

  private void showDetail(HadithCollection.Entry entry) {
    startActivity(
        new android.content.Intent(this, HadithReaderActivity.class).putExtra("entry", entry.id));
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("category", category);
    state.putString("query", search.getText().toString());
    super.onSaveInstanceState(state);
  }
}
