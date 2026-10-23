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
import com.zikra.app.ui.FeatureHeader;
import java.util.Locale;

public class DuaActivity extends BaseActivity implements TextToSpeech.OnInitListener {
  private static final String[][] DUAS = {
    {
      "Sebelum belajar",
      "رَبِّ زِدْنِي عِلْمًا",
      "Ya Tuhanku, tambahkanlah ilmu kepadaku.",
      "Al-Quran · Taha 20:114"
    },
    {
      "Ketika hujan turun",
      "اللَّهُمَّ صَيِّبًا نَافِعًا",
      "Ya Allah, jadikanlah hujan ini bermanfaat.",
      "Sahih al-Bukhari · 1032"
    },
    {
      "Kebaikan dunia dan akhirat",
      "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
      "Ya Tuhan kami, berilah kami kebaikan di dunia dan kebaikan di akhirat, serta lindungilah"
          + " kami dari azab neraka.",
      "Al-Quran · Al-Baqarah 2:201"
    },
    {
      "Saat tertimpa musibah",
      "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ، اللَّهُمَّ أْجُرْنِي فِي مُصِيبَتِي وَأَخْلِفْ"
          + " لِي خَيْرًا مِنْهَا",
      "Sesungguhnya kami milik Allah dan kepada-Nya kami kembali. Ya Allah, berilah pahala atas"
          + " musibahku dan gantilah dengan yang lebih baik.",
      "Sahih Muslim · 918 · doa umum saat musibah"
    }
  };
  private static final String[] CATEGORY_NAMES = {"Semua", "Harian", "Belajar", "Perlindungan"};
  private static final int[] DUA_CATEGORIES = {2, 1, 1, 3};
  private static final String[] SUBTITLES = {
    "Memohon ilmu yang bermanfaat",
    "Mensyukuri hujan yang membawa rahmat",
    "Memohon kebaikan dunia dan akhirat",
    "Penguat hati ketika menghadapi ujian"
  };
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
  private final View[] duaCards = new View[DUAS.length];
  private final TextView[] categoryChips = new TextView[CATEGORY_NAMES.length];
  private int selectedCategory;
  private int expandedIndex = -1;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_dua);
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
    NavDock.attach(this, (FrameLayout) findViewById(R.id.duaRoot), "home");
    getWindow().setStatusBarColor(Color.parseColor("#F8F9F5"));
    getWindow().setNavigationBarColor(Color.parseColor("#F8F9F5"));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    com.zikra.app.ui.DailyUi.chrome(this, R.color.daily_paper);
    voiceStatus = findViewById(R.id.duaVoiceStatus);
    installVoice = findViewById(R.id.duaInstallVoice);
    LinearLayout container = findViewById(R.id.duaContainer);
    listenButtons = new Button[DUAS.length];
    if (state != null) {
      selectedCategory = state.getInt("dua_category", 0);
      expandedIndex = state.getInt("dua_expanded", -1);
    }
    for (int i = 0; i < DUAS.length; i++) {
      final int index = i;
      View card = getLayoutInflater().inflate(R.layout.reading_item_dua, container, false);
      ((TextView) card.findViewById(R.id.duaTitle)).setText(DUAS[i][0]);
      ((TextView) card.findViewById(R.id.duaSubtitle)).setText(SUBTITLES[i]);
      ((TextView) card.findViewById(R.id.duaArabic)).setText(DUAS[i][1]);
      ((TextView) card.findViewById(R.id.duaTranslation)).setText(DUAS[i][2]);
      ((TextView) card.findViewById(R.id.duaSource)).setText(DUAS[i][3]);
      listenButtons[i] = card.findViewById(R.id.duaListen);
      listenButtons[i].setOnClickListener(view -> playDua(index));
      ((ImageView) card.findViewById(R.id.duaRowIcon))
          .setImageResource(
              i == 0 ? R.drawable.ic_book : i == 3 ? R.drawable.ic_shield : R.drawable.ic_heart);
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
    for (int i = 0; i < CATEGORY_NAMES.length; i++) {
      final int category = i;
      TextView chip = new TextView(this);
      chip.setText(CATEGORY_NAMES[i]);
      chip.setTextSize(14);
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

  private void updateExpandedCards() {
    for (int i = 0; i < duaCards.length; i++) {
      boolean expanded = i == expandedIndex;
      duaCards[i].findViewById(R.id.duaContent).setVisibility(expanded ? View.VISIBLE : View.GONE);
      duaCards[i].findViewById(R.id.duaChevron).setRotation(expanded ? 270 : 180);
      duaCards[i]
          .findViewById(R.id.duaExpand)
          .setContentDescription(DUAS[i][0] + (expanded ? ", tutup doa" : ", buka doa dan arti"));
    }
  }

  private void filterDuas() {
    String query =
        ((EditText) findViewById(R.id.duaSearch))
            .getText()
            .toString()
            .trim()
            .toLowerCase(Locale.ROOT);
    int visibleCount = 0;
    for (int i = 0; i < DUAS.length; i++) {
      boolean matches = selectedCategory == 0 || selectedCategory == DUA_CATEGORIES[i];
      matches &=
          (DUAS[i][0] + " " + DUAS[i][1] + " " + DUAS[i][2])
              .toLowerCase(Locale.ROOT)
              .contains(query);
      duaCards[i].setVisibility(matches ? View.VISIBLE : View.GONE);
      if (matches) visibleCount++;
      else if (speakingIndex == i) {
        if (tts != null) tts.stop();
        resetPlayback();
      }
    }
    ((TextView) findViewById(R.id.duaCount)).setText(visibleCount + " doa");
    findViewById(R.id.duaEmpty).setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
    for (int i = 0; i < categoryChips.length; i++) {
      boolean selected = i == selectedCategory;
      GradientDrawable background = new GradientDrawable();
      background.setColor(Color.parseColor(selected ? "#15332F" : "#EDF1E9"));
      background.setCornerRadius(dp(22));
      categoryChips[i].setBackground(background);
      categoryChips[i].setTextColor(Color.parseColor(selected ? "#FFFFFF" : "#52665F"));
      categoryChips[i].setSelected(selected);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putInt("dua_category", selectedCategory);
    state.putInt("dua_expanded", expandedIndex);
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
    int result = tts.speak(DUAS[index][1], TextToSpeech.QUEUE_FLUSH, null, currentUtterance);
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
    for (Button button : listenButtons) button.setText("Dengarkan doa");
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
