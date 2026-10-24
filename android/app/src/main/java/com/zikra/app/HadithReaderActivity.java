package com.zikra.app;

import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.widget.TextView;
import com.zikra.app.discovery.HadithCollection;
import com.zikra.app.discovery.HadithStore;
import com.zikra.app.ui.*;
import java.util.HashSet;
import java.util.Set;

/** Focused reader for the attributed excerpts already shipped in the app. */
public final class HadithReaderActivity extends BaseActivity {
  private HadithCollection.Entry entry;
  private SharedPreferences preferences;
  private int size;
  private AlertDialog sizeDialog;
  private HadithStore store;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    String id = getIntent().getStringExtra("entry");
    setContentView(R.layout.activity_hadith_reader);
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(R.id.toolbarBack, R.id.detailTitle, R.id.featureSubtitle, 0,
            getString(R.string.daily_hadith), null, 0, null);
    setupScreen(getString(R.string.daily_hadith));
    DailyUi.chrome(this, R.color.daily_paper);
    text(R.id.dailyReaderMeaning, getString(R.string.hadith_loading));
    store = new HadithStore(this);
    store.get(id, new HadithStore.Result<HadithCollection.Entry>() {
      public void show(HadithCollection.Entry value) {
        if (value == null) { unavailable(id); return; }
        entry = value; bindEntry();
      }
      public void failed() { unavailable(id); }
    });
  }

  private void unavailable(String id) {
    String book = HadithCollection.bookForId(id);
    text(R.id.dailyReaderTitle, getString(R.string.hadith_unavailable));
    text(R.id.dailyReaderMeaning, getString(R.string.hadith_resume_missing));
    for (int control : new int[]{R.id.dailyReaderCopy, R.id.dailyReaderShare, R.id.dailyReaderSize})
      findViewById(control).setEnabled(false);
    TextView open = findViewById(R.id.dailyReaderSave);
    open.setText(R.string.hadith_downloads);
    open.setOnClickListener(v -> startActivity(new Intent(this, HadithActivity.class).putExtra("book", book)));
    setupScreen(getString(R.string.daily_hadith));
  }

  private void bindEntry() {
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    preferences.edit().putString("last_hadith_id", entry.id).apply();
    size = Math.max(0, Math.min(2, preferences.getInt("hadith_reader_size", 0)));
    setContentView(R.layout.activity_hadith_reader);
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(
            R.id.toolbarBack,
            R.id.detailTitle,
            R.id.featureSubtitle,
            0,
            getString(R.string.daily_hadith),
            null,
            0,
            null);
    setupScreen(entry.title);
    DailyUi.chrome(this, R.color.daily_paper);
    text(R.id.dailyReaderTitle, entry.title);
    text(R.id.dailyReaderArabic, entry.arabic);
    text(R.id.dailyReaderMeaning, entry.meaning);
    text(R.id.dailyReaderSource, entry.source);
    findViewById(R.id.dailyReaderSave).setOnClickListener(v -> toggleBookmark());
    findViewById(R.id.dailyReaderCopy)
        .setOnClickListener(
            v -> {
              ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                  .setPrimaryClip(ClipData.newPlainText(entry.title, shareText()));
              toast(getString(R.string.daily_reader_copied));
            });
    findViewById(R.id.dailyReaderShare)
        .setOnClickListener(
            v -> {
              Intent intent =
                  new Intent(Intent.ACTION_SEND)
                      .setType("text/plain")
                      .putExtra(Intent.EXTRA_TEXT, shareText());
              startActivity(Intent.createChooser(intent, getString(R.string.daily_reader_share)));
            });
    findViewById(R.id.dailyReaderSize)
        .setOnClickListener(
            v -> {
              sizeDialog =
                  new AlertDialog.Builder(this)
                      .setTitle(R.string.daily_reader_size)
                      .setSingleChoiceItems(
                          new String[] {
                            getString(R.string.daily_reader_size_small),
                            getString(R.string.daily_reader_size_large),
                            getString(R.string.daily_reader_size_largest)
                          },
                          size,
                          (d, index) -> {
                            size = index;
                            preferences.edit().putInt("hadith_reader_size", size).apply();
                            applySize();
                            d.dismiss();
                          })
                      .setNegativeButton(R.string.daily_cancel, null)
                      .show();
            });
    applySize();
    updateBookmark();
  }

  private void text(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }

  private String shareText() {
    return entry.title
        + "\n\n"
        + entry.arabic
        + "\n\n"
        + entry.meaning
        + "\n\n"
        + entry.source
        + "\n"
        + getString(entry.excerpt ? R.string.daily_quote_label : R.string.hadith_source_notice);
  }

  private Set<String> bookmarks() {
    return new HashSet<>(preferences.getStringSet("hadith_bookmarks", new HashSet<>()));
  }

  private void toggleBookmark() {
    Set<String> saved = bookmarks();
    if (!saved.remove(entry.id)) saved.add(entry.id);
    preferences.edit().putStringSet("hadith_bookmarks", saved).apply();
    updateBookmark();
  }

  private void updateBookmark() {
    boolean saved = bookmarks().contains(entry.id);
    TextView button = findViewById(R.id.dailyReaderSave);
    button.setText(saved ? R.string.daily_reader_saved : R.string.daily_reader_bookmark);
    button.setSelected(saved);
  }

  private void applySize() {
    ((TextView) findViewById(R.id.dailyReaderArabic)).setTextSize(30 + size * 5);
    ((TextView) findViewById(R.id.dailyReaderMeaning)).setTextSize(16 + size * 2);
  }

  @Override
  protected void onDestroy() {
    if (sizeDialog != null) sizeDialog.dismiss();
    if (store != null) store.close();
    super.onDestroy();
  }
}
