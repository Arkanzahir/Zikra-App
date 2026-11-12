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

/** Reader for attributed excerpts and explicitly downloaded source collections. */
public final class HadithReaderActivity extends BaseActivity {
  private HadithCollection.Entry entry;
  private SharedPreferences preferences;
  private int size;
  private AlertDialog sizeDialog;
  private AlertDialog sourceDialog;
  private HadithStore store;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    String id = getIntent().getStringExtra("entry");
    setContentView(R.layout.activity_hadith_reader);
    adaptReaderActions();
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(R.id.toolbarBack, R.id.detailTitle, R.id.featureSubtitle, R.id.dailyReaderSize,
            getString(R.string.daily_hadith), null, R.drawable.reading_ic_text_size,
            getString(R.string.daily_reader_size));
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
    adaptReaderActions();
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(
            R.id.toolbarBack,
            R.id.detailTitle,
            R.id.featureSubtitle,
            R.id.dailyReaderSize,
            getString(R.string.daily_hadith),
            null,
            R.drawable.reading_ic_text_size,
            getString(R.string.daily_reader_size));
    setupScreen(entry.title);
    DailyUi.chrome(this, R.color.daily_paper);
    text(R.id.hadithReaderKind, getString(entry.encyclopedia != null ? R.string.hadeethenc_kind
        : entry.excerpt ? R.string.hadith_refined_excerpt : R.string.hadith_refined_full));
    text(R.id.hadithReaderBook, entry.collection);
    text(R.id.hadithReaderMeaningLabel, getString(entry.excerpt ? R.string.daily_translation : R.string.hadith_refined_translation));
    text(R.id.hadithReaderNote, getString(entry.encyclopedia != null ? R.string.hadeethenc_note
        : entry.excerpt ? R.string.daily_quote_label : R.string.hadith_source_notice));
    text(R.id.dailyReaderTitle, entry.title);
    text(R.id.dailyReaderArabic, entry.arabic);
    text(R.id.dailyReaderMeaning, entry.meaning);
    text(R.id.dailyReaderSource, entry.source);
    if (entry.encyclopedia != null) bindEncyclopedia();
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
    ((FeatureHeader) findViewById(R.id.utilitiesHeader)).action()
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

  private void adaptReaderActions() {
    if (getResources().getConfiguration().fontScale <= 1.3f) return;
    android.widget.LinearLayout actions = findViewById(R.id.hadithReaderActions);
    actions.setOrientation(android.widget.LinearLayout.VERTICAL);
    for (int i = 0; i < actions.getChildCount(); i++) {
      android.view.View control = actions.getChildAt(i);
      control.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
    }
  }

  private void text(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }

  private String shareText() {
    if (entry.encyclopedia != null) return completeSourceText();
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

  private void bindEncyclopedia() {
    ((TextView) findViewById(R.id.dailyReaderTitle)).setTextSize(20);
    findViewById(R.id.hadeethencDetails).setVisibility(android.view.View.VISIBLE);
    text(R.id.hadeethencGrade, entry.encyclopedia.cell(11));
    optionalSourceText(R.id.hadeethencExplanationLabel, R.id.hadeethencExplanation, entry.encyclopedia.cell(6));
    optionalSourceText(R.id.hadeethencBenefitsLabel, R.id.hadeethencBenefits, entry.encyclopedia.cell(8));
    findViewById(R.id.hadeethencOpenSource).setOnClickListener(v -> openSource(entry.encyclopedia.cell(14)));
    findViewById(R.id.hadeethencFullSource).setOnClickListener(v -> {
      android.widget.ScrollView scroll = new android.widget.ScrollView(this);
      android.widget.LinearLayout panel = new android.widget.LinearLayout(this);
      panel.setOrientation(android.widget.LinearLayout.VERTICAL);
      android.widget.Button version = new android.widget.Button(this);
      version.setId(R.id.hadeethencVersionCheck);
      version.setText(R.string.hadeethenc_check_version); version.setAllCaps(false);
      version.setTextSize(14); version.setMinimumHeight((int) (48 * getResources().getDisplayMetrics().density));
      version.setOnClickListener(button -> openSource(com.zikra.app.discovery.HadeethEncCsv.UPDATE_URL));
      panel.addView(version, new android.widget.LinearLayout.LayoutParams(-1, -2));
      TextView content = new TextView(this);
      content.setText(completeSourceText()); content.setTextIsSelectable(true);
      content.setTextColor(getColor(R.color.daily_ink)); content.setTextSize(14);
      int inset = (int) (20 * getResources().getDisplayMetrics().density);
      content.setPadding(inset, inset, inset, inset); panel.addView(content); scroll.addView(panel);
      sourceDialog = new AlertDialog.Builder(this).setTitle(R.string.hadeethenc_full_source)
          .setView(scroll).setPositiveButton(android.R.string.ok, null).show();
    });
  }

  private void optionalSourceText(int label, int view, String value) {
    int visibility = value.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE;
    findViewById(label).setVisibility(visibility); findViewById(view).setVisibility(visibility);
    text(view, value);
  }

  private String completeSourceText() {
    String[] labels = getResources().getStringArray(R.array.hadeethenc_field_labels);
    StringBuilder value = new StringBuilder(entry.encyclopedia.metadata);
    for (int i = 0; i < labels.length; i++)
      value.append("\n\n").append(labels[i]).append("\n").append(entry.encyclopedia.cell(i));
    return value.toString();
  }

  private void openSource(String url) {
    try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))); }
    catch (ActivityNotFoundException unavailable) { toast(getString(R.string.hadith_load_failed)); }
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
    ((TextView) findViewById(R.id.dailyReaderArabic)).setTextSize(28 + size * 4);
    ((TextView) findViewById(R.id.dailyReaderMeaning)).setTextSize(15 + size * 2);
    if (entry.encyclopedia != null)
      for (int id : new int[]{R.id.hadeethencGrade, R.id.hadeethencExplanation, R.id.hadeethencBenefits})
        ((TextView) findViewById(id)).setTextSize(15 + size * 2);
  }

  @Override
  protected void onDestroy() {
    if (sizeDialog != null) sizeDialog.dismiss();
    if (sourceDialog != null) sourceDialog.dismiss();
    if (store != null) store.close();
    super.onDestroy();
  }
}
