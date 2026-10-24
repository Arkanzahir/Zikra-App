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
import com.zikra.app.discovery.DuaReview;
import com.zikra.app.discovery.DuaCatalogRepository;
import com.zikra.app.discovery.HadithSearch;
import com.zikra.app.ui.FeatureHeader;
import com.zikra.app.ui.DuaReadingUi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** The extra catalogue is optional; the bundled selected prayers remain available without downloading. */
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
    if (catalog != null) {
      int unreviewed = 0;
      for (DuaCatalog.Entry entry : catalog.entries) if (entry.review == DuaReview.SOURCE_CHANGED) unreviewed++;
      status.setText(getString(R.string.dua_library_source, catalog.entries.size(), catalog.sourceCount,
          catalog.excludedWithoutReference + catalog.excludedWithoutText, unreviewed));
    }
    for (int i = offset; i < Math.min(offset + 20, matches.size()); i++) {
      DuaCatalog.Entry entry = matches.get(i);
      View card = getLayoutInflater().inflate(R.layout.reading_item_dua, content, false);
      String displayTitle = entry.review == DuaReview.WIND ? getString(R.string.dua_review_wind_title) : entry.review.displayTitle(entry.title);
      ((TextView) card.findViewById(R.id.duaTitle)).setText(displayTitle);
      ((TextView) card.findViewById(R.id.duaRowCategory)).setText(entry.group);
      ((TextView) card.findViewById(R.id.duaSubtitle)).setText("EQuran · " + entry.number);
      String sourceLabel = entry.review == DuaReview.NONE ? "EQuran.id · " + entry.number
          : getString(entry.review == DuaReview.SOURCE_CHANGED ? R.string.dua_review_unreviewed_source_label
              : R.string.dua_review_source_label, entry.number);
      DuaReadingUi.bind(card, entry.arabic, entry.latin, entry.meaning, sourceLabel,
          reviewNote(entry) + entry.reference + "\n\nhttps://equran.id/api/doa/" + entry.number);
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
      card.findViewById(R.id.duaExpand).setContentDescription(displayTitle + ", " + entry.group);
      card.findViewById(R.id.duaRowDivider).setVisibility(i + 1 == matches.size() ? View.GONE : View.VISIBLE);
      content.addView(card);
    }
    findViewById(R.id.duaLibraryEmpty).setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
  }
  private String reviewNote(DuaCatalog.Entry entry) {
    int note;
    switch (entry.review) {
      case TITLE_TYPO:
      case TITLE_SCOPE:
        return getString(R.string.dua_review_title) + "\n" + entry.title + "\n\n";
      case ARABIC_INSTRUCTIONS:
        return getString(R.string.dua_review_instructions) + "\n"
            + entry.sourceArabic.substring(0, entry.sourceArabic.indexOf('\n')).trim() + "\n\n";
      case WITR_LAYOUT: note = R.string.dua_review_witr_layout; break;
      case WAKE_UP: note = R.string.dua_review_wake; break;
      case WIND: note = R.string.dua_review_wind; break;
      case RAIN_NUMBERING: note = R.string.dua_review_rain; break;
      case AFTER_MEAL_NUMBERING: note = R.string.dua_review_after_meal; break;
      case GRADING_CONTEXT: note = R.string.dua_review_grading; break;
      case SOURCE_CHANGED: note = R.string.dua_review_changed; break;
      case AYYUB_ADAPTED: note = R.string.dua_review_ayyub; break;
      case MARKET_SCOPE: note = R.string.dua_review_market; break;
      case ANAS_ADAPTED: note = R.string.dua_review_anas; break;
      case BIRTH_GREETING: note = R.string.dua_review_birth; break;
      case CHILD_ADAPTED: note = R.string.dua_review_child_adapted; break;
      case CHILD_REFERENCE: note = R.string.dua_review_child_reference; break;
      case FIQH_ADAPTED: note = R.string.dua_review_fiqh; break;
      case JARIR_ADAPTED: note = R.string.dua_review_jarir; break;
      case IBN_MASUD_ATHAR: note = R.string.dua_review_ibn_masud; break;
      case IMAN_ROUTES: note = R.string.dua_review_iman_routes; break;
      case EID_GREETING: note = R.string.dua_review_eid; break;
      case MEANING_REPEAT: note = R.string.dua_review_meaning_repeat; break;
      case LABBAIK_LETTER: note = R.string.dua_review_labbaik; break;
      case WORD_SPACING: note = R.string.dua_review_word_spacing; break;
      case COMPOSITE_KHUTBA: note = R.string.dua_review_composite; break;
      case SLEEP_GRADING: note = R.string.dua_review_sleep_grading; break;
      case DEBT_GRADING: note = R.string.dua_review_debt_grading; break;
      case MEANING_PERSON: note = R.string.dua_review_meaning_person; break;
      case DEATH_NUMBERING: note = R.string.dua_review_death_number; break;
      case POVERTY_NUMBERING: note = R.string.dua_review_poverty_number; break;
      case GOOD_DEEDS_NUMBERING: note = R.string.dua_review_good_deeds_number; break;
      case GHAYB_HARAKAT: note = R.string.dua_review_ghayb; break;
      default: return "";
    }
    StringBuilder details = new StringBuilder(getString(note)).append("\n\n");
    if (!entry.arabic.equals(entry.sourceArabic)) details.append(getString(R.string.dua_review_original_arabic))
        .append('\n').append(entry.sourceArabic).append("\n\n");
    if (!entry.latin.equals(entry.sourceLatin)) details.append(getString(R.string.dua_review_original_latin))
        .append('\n').append(entry.sourceLatin).append("\n\n");
    if (!entry.meaning.equals(entry.sourceMeaning)) details.append(getString(R.string.dua_review_original_meaning))
        .append('\n').append(entry.sourceMeaning).append("\n\n");
    if (!entry.title.equals(entry.review.displayTitle(entry.title))) details.append(getString(R.string.dua_review_original_title))
        .append('\n').append(entry.title).append("\n\n");
    return details.toString();
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
