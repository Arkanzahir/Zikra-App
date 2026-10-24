package com.zikra.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.discovery.DuaCatalog;
import com.zikra.app.discovery.DuaCatalogRepository;
import com.zikra.app.discovery.HadithSearch;
import com.zikra.app.ui.FeatureHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** The extra catalogue is optional; the four quick prayers remain available without downloading. */
public final class DuaLibraryActivity extends BaseActivity {
  private DuaCatalogRepository repository;
  private DuaCatalog catalog;
  private LinearLayout content;
  private TextView status;
  private EditText search;
  private Button groupButton, previous, next, refresh;
  private String group = "", expandedId;
  private int offset;
  private boolean loading, voiceReady;
  private TextToSpeech voice;
  private AlertDialog dialog;

  @Override protected void onCreate(Bundle state) {
    super.onCreate(state); setContentView(R.layout.activity_dua_library);
    ((FeatureHeader) findViewById(R.id.utilitiesHeader)).bind(R.id.toolbarBack, R.id.detailTitle,
        R.id.featureSubtitle, 0, getString(R.string.dua_library_title), null, 0, null);
    setupScreen(getString(R.string.dua_library_title));
    com.zikra.app.ui.DailyUi.chrome(this, R.color.daily_paper);
    repository = new DuaCatalogRepository(this);
    content = findViewById(R.id.duaLibraryContent); status = findViewById(R.id.duaLibraryStatus);
    search = findViewById(R.id.duaLibrarySearch); groupButton = findViewById(R.id.duaLibraryGroup);
    previous = findViewById(R.id.duaLibraryPrevious); next = findViewById(R.id.duaLibraryNext);
    refresh = findViewById(R.id.duaLibraryRefresh);
    if (state != null) {
      group = state.getString("group", ""); offset = state.getInt("offset", 0);
      expandedId = state.getString("expanded"); search.setText(state.getString("query", ""));
    }
    search.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int a, int c, int f) { }
      public void afterTextChanged(Editable text) { }
      public void onTextChanged(CharSequence s, int a, int b, int c) { offset = 0; render(); }
    });
    previous.setOnClickListener(v -> { offset -= 20; render(); scrollToTop(); });
    next.setOnClickListener(v -> { offset += 20; render(); scrollToTop(); });
    groupButton.setOnClickListener(v -> chooseGroup()); refresh.setOnClickListener(v -> load(true));
    findViewById(R.id.duaLibraryRemove).setOnClickListener(v -> {
      dialog = new AlertDialog.Builder(this).setTitle(R.string.dua_library_remove)
          .setMessage(R.string.dua_library_remove_hint).setNegativeButton(R.string.daily_cancel, null)
          .setPositiveButton(R.string.dua_library_remove, (d, w) -> repository.remove(() -> {
            catalog = null; offset = 0; render(); status.setText(R.string.dua_library_not_downloaded);
          })).show();
    });
    voice = new TextToSpeech(this, result -> {
      if (voice != null && result == TextToSpeech.SUCCESS)
        voiceReady = voice.setLanguage(Locale.forLanguageTag("ar")) >= TextToSpeech.LANG_AVAILABLE;
    });
    load(false);
  }

  private void load(boolean refreshSource) {
    if (loading) return; loading = true; refresh.setEnabled(false);
    findViewById(R.id.duaLibraryRemove).setEnabled(false);
    status.setText(R.string.dua_library_loading);
    repository.load(refreshSource, new DuaCatalogRepository.Result() {
      public void show(DuaCatalog value) {
        loading = false; refresh.setEnabled(true);
        findViewById(R.id.duaLibraryRemove).setEnabled(true); catalog = value; render();
      }
      public void failed() {
        loading = false; refresh.setEnabled(true);
        findViewById(R.id.duaLibraryRemove).setEnabled(true);
        status.setText(catalog == null ? R.string.dua_library_not_downloaded : R.string.dua_library_update_failed);
        if (catalog == null) render();
      }
    });
  }

  private void scrollToTop() {
    ((android.widget.ScrollView) findViewById(R.id.duaLibraryScroll)).smoothScrollTo(0, 0);
  }

  private void chooseGroup() {
    if (catalog == null) return;
    TreeSet<String> values = new TreeSet<>(); for (DuaCatalog.Entry entry : catalog.entries) values.add(entry.group);
    List<String> names = new ArrayList<>(); names.add(getString(R.string.dua_library_all_groups)); names.addAll(values);
    dialog = new AlertDialog.Builder(this).setTitle(R.string.dua_library_group)
        .setItems(names.toArray(new String[0]), (d, which) -> {
          group = which == 0 ? "" : names.get(which); offset = 0; render();
        }).setNegativeButton(R.string.daily_cancel, null).show();
  }

  private void render() {
    if (voice != null) voice.stop();
    content.removeAllViews(); groupButton.setText(group.isEmpty() ? getString(R.string.dua_library_all_groups) : group);
    List<DuaCatalog.Entry> matches = new ArrayList<>();
    String query = HadithSearch.normalize(search.getText().toString());
    if (catalog != null) for (DuaCatalog.Entry entry : catalog.entries)
      if ((group.isEmpty() || group.equals(entry.group)) && entry.search.contains(query)) matches.add(entry);
    offset = Math.max(0, Math.min(Math.max(0, matches.size() - 1) / 20 * 20, offset));
    previous.setEnabled(offset > 0); next.setEnabled(offset + 20 < matches.size());
    ((TextView) findViewById(R.id.duaLibraryCount)).setText(
        getString(R.string.dua_library_count, matches.size(), catalog == null ? 0 : catalog.entries.size()));
    if (catalog != null) status.setText(getString(R.string.dua_library_source,
        catalog.entries.size(), catalog.sourceCount, catalog.excludedWithoutReference + catalog.excludedWithoutText));
    for (int i = offset; i < Math.min(offset + 20, matches.size()); i++) {
      DuaCatalog.Entry entry = matches.get(i);
      View card = getLayoutInflater().inflate(R.layout.reading_item_dua, content, false);
      ((TextView) card.findViewById(R.id.duaTitle)).setText(entry.title);
      ((TextView) card.findViewById(R.id.duaRowCategory)).setText(entry.group);
      ((TextView) card.findViewById(R.id.duaSubtitle)).setText("EQuran · " + entry.number);
      ((TextView) card.findViewById(R.id.duaArabic)).setText(entry.arabic);
      ((TextView) card.findViewById(R.id.duaTranslation)).setText(
          (entry.latin.isEmpty() ? "" : entry.latin + "\n\n") + entry.meaning);
      ((TextView) card.findViewById(R.id.duaSource)).setText(entry.reference + "\n\nhttps://equran.id/api/doa/" + entry.number);
      ((android.widget.ImageView) card.findViewById(R.id.duaRowIcon)).setImageResource(R.drawable.home_ic_dua);
      Button audio = card.findViewById(R.id.duaListen); audio.setText(R.string.dua_library_tts); audio.setEnabled(true);
      audio.setOnClickListener(v -> {
        if (!voiceReady) { toast(getString(R.string.dua_library_voice_unavailable)); return; }
        voice.speak(entry.arabic, TextToSpeech.QUEUE_FLUSH, null, entry.id);
      });
      View expanded = card.findViewById(R.id.duaContent);
      boolean active = entry.id.equals(expandedId);
      expanded.setVisibility(active ? View.VISIBLE : View.GONE); card.setActivated(active);
      card.findViewById(R.id.duaChevron).setRotation(active ? 270 : 180);
      card.findViewById(R.id.duaExpand).setOnClickListener(v -> {
        expandedId = entry.id.equals(expandedId) ? null : entry.id; render();
      });
      card.findViewById(R.id.duaExpand).setContentDescription(entry.title + ", " + entry.group);
      card.findViewById(R.id.duaRowDivider).setVisibility(i + 1 == matches.size() ? View.GONE : View.VISIBLE);
      content.addView(card);
    }
    findViewById(R.id.duaLibraryEmpty).setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
  }
  @Override protected void onSaveInstanceState(Bundle state) {
    state.putString("group", group); state.putString("query", search.getText().toString());
    state.putString("expanded", expandedId); state.putInt("offset", offset); super.onSaveInstanceState(state);
  }
  @Override protected void onPause() { if (voice != null) voice.stop(); super.onPause(); }
  @Override protected void onDestroy() {
    if (dialog != null) dialog.dismiss(); if (repository != null) repository.close();
    if (voice != null) { voice.stop(); voice.shutdown(); } super.onDestroy();
  }
}
