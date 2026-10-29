package com.zikra.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.discovery.DuaSelection;
import com.zikra.app.ui.FeatureHeader;
import com.zikra.app.ui.DuaReadingUi;
import java.util.List;
import java.util.Locale;

public class DuaActivity extends BaseActivity implements TextToSpeech.OnInitListener {
  private List<DuaSelection.Entry> duas;
  private static final List<String> CATEGORY_NAMES = DuaSelection.CATEGORIES;
  private TextToSpeech tts;
  private TextView voiceStatus;
  private Button installVoice;
  private Button[] listenButtons;
  private boolean initialized;
  private boolean ready;
  private boolean destroyed;
  private boolean returningFromVoiceInstall;
  private int speakingIndex = -1;
  private String currentUtterance;
  private int utteranceSequence;
  private View[] duaCards;
  private final TextView[] categoryChips = new TextView[CATEGORY_NAMES.size()];
  private int selectedCategory;
  private int expandedIndex = -1;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_dua);
    try {
      duas = DuaSelection.read(getResources().openRawResource(R.raw.dua_selected));
    } catch (java.io.IOException | org.json.JSONException invalidBundledSelection) {
      throw new IllegalStateException("Invalid bundled prayer selection", invalidBundledSelection);
    }
    duaCards = new View[duas.size()];
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(
            R.id.toolbarBack,
            R.id.detailTitle,
            R.id.featureSubtitle,
            0,
            "Doa Harian",
            null,
            0,
            null);
    setupScreen("Doa Harian");
    findViewById(R.id.duaLibraryOpen).setOnClickListener(v -> startActivity(new Intent(this, DuaLibraryActivity.class)));
    NavDock.attach(this, (FrameLayout) findViewById(R.id.duaRoot), "home");
    getWindow().setStatusBarColor(getColor(R.color.ivory));
    getWindow().setNavigationBarColor(getColor(R.color.ivory));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    com.zikra.app.ui.DailyUi.chrome(this, R.color.daily_paper);
    voiceStatus = findViewById(R.id.duaVoiceStatus);
    installVoice = findViewById(R.id.duaInstallVoice);
    LinearLayout container = findViewById(R.id.duaContainer);
    listenButtons = new Button[duas.size()];
    if (state != null) {
      selectedCategory = state.getInt("dua_category", 0);
      expandedIndex = state.getInt("dua_expanded", -1);
      String expandedId = state.getString("dua_expanded_id");
      if (expandedId != null) {
        expandedIndex = -1;
        for (int i = 0; i < duas.size(); i++) if (expandedId.equals(duas.get(i).id)) expandedIndex = i;
      }
    }
    selectedCategory = Math.max(0, Math.min(selectedCategory, CATEGORY_NAMES.size() - 1));
    expandedIndex = expandedIndex >= 0 && expandedIndex < duas.size() ? expandedIndex : -1;
    for (int i = 0; i < duas.size(); i++) {
      final int index = i;
      View card = getLayoutInflater().inflate(R.layout.reading_item_dua, container, false);
      ((TextView) card.findViewById(R.id.duaTitle)).setText(duas.get(i).title);
      ((TextView) card.findViewById(R.id.duaRowCategory)).setText(CATEGORY_NAMES.get(duas.get(i).category).toUpperCase(Locale.ROOT));
      ((TextView) card.findViewById(R.id.duaSubtitle)).setText(duas.get(i).reference);
      DuaReadingUi.bind(card, duas.get(i).arabic, duas.get(i).latin, duas.get(i).meaning,
          duas.get(i).reference, duas.get(i).sourceText());
      listenButtons[i] = card.findViewById(R.id.duaListen);
      listenButtons[i].setText(R.string.dua_library_tts);
      listenButtons[i].setOnClickListener(view -> playDua(index));
      ((ImageView) card.findViewById(R.id.duaRowIcon)).setImageResource(rowIcon(duas.get(i).category));
      card.findViewById(R.id.duaExpand)
          .setOnClickListener(
              view -> {
                expandedIndex = expandedIndex == index ? -1 : index;
                if (speakingIndex >= 0 && speakingIndex != expandedIndex) {
                  if (tts != null) tts.stop();
                  resetPlayback();
                }
                updateExpandedCards();
              });
      duaCards[i] = card;
      container.addView(card);
    }
    LinearLayout categories = findViewById(R.id.duaCategories);
    for (int i = 0; i < CATEGORY_NAMES.size(); i++) {
      final int category = i;
      TextView chip = new TextView(this);
      chip.setText(CATEGORY_NAMES.get(i));
      chip.setTextSize(13);
      chip.setGravity(Gravity.CENTER);
      chip.setPadding(dp(15), 0, dp(15), 0);
      chip.setMinHeight(dp(48));
      chip.setClickable(true);
      chip.setFocusable(true);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      params.setMarginEnd(dp(8));
      categories.addView(chip, params);
      categoryChips[i] = chip;
      chip.setOnClickListener(
          view -> {
            selectedCategory = category;
            filterDuas();
          });
    }
    ((EditText) findViewById(R.id.duaSearch))
        .addTextChangedListener(
            new TextWatcher() {
              @Override
              public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

              @Override
              public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDuas();
              }

              @Override
              public void afterTextChanged(Editable editable) {}
            });
    updateExpandedCards();
    filterDuas();
    installVoice.setOnClickListener(
        view -> {
          try {
            returningFromVoiceInstall = true;
            startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
          } catch (ActivityNotFoundException exception) {
            returningFromVoiceInstall = false;
            toast(
                "Pasang mesin Text-to-Speech yang mendukung bahasa Arab melalui pengaturan"
                    + " perangkat");
          }
        });
    tts = new TextToSpeech(this, this);
  }

  private int rowIcon(int category) {
    if (category == 2) return R.drawable.ic_book;
    if (category == 3) return R.drawable.ic_shield;
    if (category == 4) return R.drawable.dua_ic_meal;
    if (category == 5) return R.drawable.ic_moon;
    if (category == 6) return R.drawable.ic_home;
    if (category == 7) return R.drawable.home_ic_qibla;
    if (category == 8) return R.drawable.dua_ic_rain;
    if (category == 9) return R.drawable.ic_sun;
    return R.drawable.home_ic_dua;
  }

  private void updateExpandedCards() {
    for (int i = 0; i < duaCards.length; i++) {
      boolean expanded = i == expandedIndex;
      duaCards[i].setActivated(expanded);
      duaCards[i].findViewById(R.id.duaContent).setVisibility(expanded ? View.VISIBLE : View.GONE);
      duaCards[i].findViewById(R.id.duaChevron).setRotation(expanded ? 270 : 180);
      duaCards[i]
          .findViewById(R.id.duaExpand)
          .setContentDescription(duas.get(i).title + (expanded ? ", tutup doa" : ", buka doa dan arti"));
    }
    updateVoicePanel();
  }

  private void updateVoicePanel() {
    boolean open =
        expandedIndex >= 0
            && expandedIndex < duaCards.length
            && duaCards[expandedIndex].getVisibility() == View.VISIBLE;
    findViewById(R.id.duaVoicePanel).setVisibility(open ? View.VISIBLE : View.GONE);
  }

  private void filterDuas() {
    String query = ((EditText) findViewById(R.id.duaSearch)).getText().toString();
    int visibleCount = 0;
    View lastVisible = null;
    for (int i = 0; i < duas.size(); i++) {
      boolean matches = duas.get(i).matches(query, selectedCategory);
      duaCards[i].setVisibility(matches ? View.VISIBLE : View.GONE);
      duaCards[i].findViewById(R.id.duaRowDivider).setVisibility(View.VISIBLE);
      if (matches) {
        visibleCount++;
        lastVisible = duaCards[i];
      } else if (speakingIndex == i) {
        if (tts != null) tts.stop();
        resetPlayback();
      }
    }
    if (lastVisible != null) lastVisible.findViewById(R.id.duaRowDivider).setVisibility(View.GONE);
    ((TextView) findViewById(R.id.duaCount))
        .setText(getString(R.string.dua_collection_count, visibleCount));
    findViewById(R.id.duaEmpty).setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
    updateVoicePanel();
    for (int i = 0; i < categoryChips.length; i++) {
      boolean selected = i == selectedCategory;
      GradientDrawable background = new GradientDrawable();
      background.setColor(getColor(selected ? R.color.pine : android.R.color.transparent));
      background.setCornerRadius(dp(14));
      if (!selected) background.setStroke(dp(1), getColor(R.color.daily_line));
      categoryChips[i].setBackground(background);
      categoryChips[i].setTextColor(getColor(selected ? android.R.color.white : R.color.dua_text_secondary));
      categoryChips[i].setSelected(selected);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putInt("dua_category", selectedCategory);
    state.putInt("dua_expanded", expandedIndex);
    if (expandedIndex >= 0 && expandedIndex < duas.size())
      state.putString("dua_expanded_id", duas.get(expandedIndex).id);
    super.onSaveInstanceState(state);
  }

  @Override
  public void onInit(int status) {
    if (destroyed) return;
    initialized = status == TextToSpeech.SUCCESS;
    if (!initialized) {
      voiceStatus.setText("Mesin suara belum tersedia. Teks dan arti doa tetap dapat dibaca.");
      installVoice.setVisibility(View.VISIBLE);
      return;
    }
    checkArabicVoice();
    tts.setOnUtteranceProgressListener(
        new UtteranceProgressListener() {
          @Override
          public void onStart(String utteranceId) {}

          @Override
          public void onDone(String utteranceId) {
            finishUtterance(utteranceId, false);
          }

          @Override
          public void onError(String utteranceId) {
            finishUtterance(utteranceId, true);
          }
        });
  }

  private void checkArabicVoice() {
    if (!initialized || tts == null) return;
    int availability = tts.setLanguage(Locale.forLanguageTag("ar"));
    ready =
        availability != TextToSpeech.LANG_MISSING_DATA
            && availability != TextToSpeech.LANG_NOT_SUPPORTED;
    for (Button button : listenButtons) button.setEnabled(ready);
    installVoice.setVisibility(ready ? View.GONE : View.VISIBLE);
    voiceStatus.setText(
        ready
            ? "Audio pelafalan Arab · Text-to-Speech perangkat, bukan rekaman qari."
            : "Suara bahasa Arab belum tersedia. Pasang data suara untuk mendengarkan doa.");
    if (ready) tts.setSpeechRate(0.8f);
  }

  private void playDua(int index) {
    if (!ready || tts == null) return;
    if (speakingIndex == index) {
      tts.stop();
      resetPlayback();
      return;
    }
    tts.stop();
    resetPlayback();
    speakingIndex = index;
    listenButtons[index].setText("Hentikan audio");
    currentUtterance = "dua-" + index + "-" + (++utteranceSequence);
    int result = tts.speak(duas.get(index).arabic, TextToSpeech.QUEUE_FLUSH, null, currentUtterance);
    if (result == TextToSpeech.ERROR) {
      resetPlayback();
      toast("Audio belum dapat diputar. Periksa mesin suara di pengaturan perangkat.");
    }
  }

  private void finishUtterance(String utteranceId, boolean error) {
    runOnUiThread(
        () -> {
          if (destroyed
              || speakingIndex < 0
              || currentUtterance == null
              || !currentUtterance.equals(utteranceId)) return;
          resetPlayback();
          if (error)
            voiceStatus.setText(
                "Suara gagal diputar. Periksa data bahasa Arab atau koneksi mesin suara"
                    + " perangkat.");
        });
  }

  private void resetPlayback() {
    speakingIndex = -1;
    currentUtterance = null;
    for (Button button : listenButtons) button.setText(getString(R.string.dua_library_tts));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (returningFromVoiceInstall && !initialized) {
      if (tts != null) tts.shutdown();
      tts = new TextToSpeech(this, this);
    }
    returningFromVoiceInstall = false;
    if (initialized) checkArabicVoice();
  }

  @Override
  protected void onPause() {
    if (tts != null) tts.stop();
    resetPlayback();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    destroyed = true;
    if (tts != null) {
      tts.stop();
      tts.shutdown();
    }
    super.onDestroy();
  }
}
