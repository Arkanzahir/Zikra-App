package com.zikra.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.discovery.DiscoveryUi;
import com.zikra.app.discovery.HadithCollection;
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
      tab.setTextSize(12);
      tab.setMinWidth(0);
      tab.setMinimumWidth(0);
      tab.setPadding(dp(16), 0, dp(16), 0);
      LinearLayout.LayoutParams p =
          new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(37));
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
    NavDock.attach(this, (FrameLayout) findViewById(R.id.hadithRoot), "hadith");
    render();
  }

  private void render() {
    for (Button tab : tabs) {
      boolean active = category.contentEquals(tab.getText());
      tab.setSelected(active);
      tab.setBackground(DiscoveryUi.background(this, active ? "#57927E" : "#EDF2EB", 22));
      tab.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#74877C"));
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
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(24), dp(8), dp(24), dp(10));
    TextView arabic = DiscoveryUi.text(this, entry.arabic, 27, "#315849", false);
    arabic.setGravity(Gravity.END);
    arabic.setTextDirection(View.TEXT_DIRECTION_RTL);
    arabic.setLineSpacing(dp(8), 1f);
    body.addView(arabic);
    TextView meaning = DiscoveryUi.text(this, entry.meaning, 15, "#315849", false);
    meaning.setPadding(0, dp(20), 0, dp(16));
    meaning.setLineSpacing(dp(5), 1f);
    body.addView(meaning);
    body.addView(
        DiscoveryUi.text(
            this, entry.source + "\nKutipan hadis · terjemahan makna", 12, "#7C8B8A", false));
    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
    scroll.addView(body);
    new AlertDialog.Builder(this)
        .setTitle(entry.title)
        .setView(scroll)
        .setPositiveButton("Tutup", null)
        .setNeutralButton(
            saved.contains(entry.id) ? "Hapus simpanan" : "Simpan",
            (dialog, which) -> {
              if (saved.contains(entry.id)) saved.remove(entry.id);
              else saved.add(entry.id);
              getSharedPreferences("zikra_preferences", MODE_PRIVATE)
                  .edit()
                  .putStringSet("hadith_bookmarks", new HashSet<>(saved))
                  .apply();
              render();
              toast(saved.contains(entry.id) ? "Hadis disimpan" : "Simpanan dihapus");
            })
        .show();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("category", category);
    state.putString("query", search.getText().toString());
    super.onSaveInstanceState(state);
  }
}
