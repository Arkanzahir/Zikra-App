package com.zikra.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import com.zikra.app.reading.SurahData;
import java.util.Locale;

public class QuranActivity extends BaseActivity implements SensorEventListener {
  public static final String EXTRA_RESUME_READING = "resume_reading";
  private SensorManager sensorManager;
  private Sensor lightSensor;
  private SharedPreferences preferences;
  private LinearLayout ayahContainer;
  private Switch nightSwitch;
  private TextView lightStatus;
  private Spinner surahSelector;
  private boolean nightMode;
  private boolean applyingSensor;
  private boolean manualOverride;
  private float lastAmbientLux = Float.NaN;
  private boolean firstSelection = true;
  private int selectedIndex;
  private boolean readingOpen;
  private LinearLayout surahList;
  private android.view.ViewTreeObserver.OnGlobalLayoutListener pendingBookmarkLayout;
  private final int[] secondaryLabels = {
    R.id.quranIntro,
    R.id.readingAvailable,
    R.id.lightSensorStatus,
    R.id.quranFootnote,
    R.id.quranEmpty
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_quran);
    setupScreen("Al-Quran");
    NavDock.attach(this, (FrameLayout) findViewById(R.id.quranPageRoot), "quran");
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    lightSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    ayahContainer = findViewById(R.id.ayahContainer);
    nightSwitch = findViewById(R.id.readingNightSwitch);
    lightStatus = findViewById(R.id.lightSensorStatus);
    surahSelector = findViewById(R.id.surahSelector);
    surahList = findViewById(R.id.quranSurahList);
    readingOpen =
        savedInstanceState != null
            ? savedInstanceState.getBoolean("reading_open")
            : getIntent().getBooleanExtra(EXTRA_RESUME_READING, false);
    nightMode = savedInstanceState != null && savedInstanceState.getBoolean("night_mode");
    manualOverride = savedInstanceState != null && savedInstanceState.getBoolean("manual_override");
    String lastSurah = preferences.getString("last_surah", "Al-Fatihah");
    findViewById(R.id.quranCatalogButton).setOnClickListener(view -> showCatalog());
    findViewById(R.id.quranBackToCatalog).setOnClickListener(view -> showCatalog());
    findViewById(R.id.toolbarBack)
        .setOnClickListener(
            view -> {
              if (readingOpen) showCatalog();
              else finish();
            });
    findViewById(R.id.quranResume)
        .setOnClickListener(
            view -> {
              String resume = preferences.getString("last_surah", "Al-Fatihah");
              for (int i = 0; i < SurahData.ALL.length; i++)
                if (SurahData.ALL[i].name.equals(resume)) {
                  openSurah(i, true);
                  break;
                }
            });
    ((EditText) findViewById(R.id.quranSearch))
        .addTextChangedListener(
            new TextWatcher() {
              @Override
              public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

              @Override
              public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCatalog(s.toString());
              }

              @Override
              public void afterTextChanged(Editable editable) {}
            });
    String[] labels = new String[SurahData.ALL.length];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = SurahData.ALL[i].name + " · " + SurahData.ALL[i].arabic.length + " ayat";
      if (SurahData.ALL[i].name.equals(lastSurah)) selectedIndex = i;
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setTextColor(Color.parseColor(nightMode ? "#F5F7F6" : "#15332F"));
            view.setTextSize(15);
            return view;
          }
        };
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    final int initialIndex = selectedIndex;
    surahSelector.setAdapter(adapter);
    surahSelector.setSelection(selectedIndex);
    surahSelector.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            boolean initial = firstSelection && position == initialIndex;
            selectedIndex = position;
            firstSelection = false;
            if (initial) return; // The initial surah is already bound below during onCreate.
            if (!initial) {
              preferences
                  .edit()
                  .putString("last_surah", SurahData.ALL[position].name)
                  .putInt("last_ayah", 1)
                  .apply();
              showReader();
            }
            renderSurah(initial);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    nightSwitch.setChecked(nightMode);
    nightSwitch.setOnCheckedChangeListener(
        (button, checked) -> {
          if (!applyingSensor) manualOverride = true;
          applyNightMode(checked);
          updateSensorLabel();
        });
    nightSwitch.setOnLongClickListener(
        view -> {
          manualOverride = false;
          applyAmbientLight();
          updateSensorLabel();
          toast("Mode cahaya otomatis diaktifkan kembali");
          return true;
        });
    applyNightMode(nightMode);
    updateSensorLabel();
    renderCatalog("");
    renderSurah(true);
    if (readingOpen) showReader();
  }

  private void renderCatalog(String query) {
    String normalized = query.trim().toLowerCase(Locale.ROOT).replace("-", "");
    surahList.removeAllViews();
    for (int i = 0; i < SurahData.ALL.length; i++) {
      final int index = i;
      SurahData surah = SurahData.ALL[i];
      if (!surah.name.toLowerCase(Locale.ROOT).replace("-", "").contains(normalized)
          && !surah.arabicName.contains(normalized)
          && !String.valueOf(surah.number).equals(normalized)) continue;
      View row = getLayoutInflater().inflate(R.layout.reading_item_surah, surahList, false);
      ((TextView) row.findViewById(R.id.surahListNumber)).setText(String.valueOf(surah.number));
      ((TextView) row.findViewById(R.id.surahListName)).setText(surah.name);
      ((TextView) row.findViewById(R.id.surahListInfo))
          .setText(surah.arabic.length + " ayat  ·  Juz " + surah.juz);
      ((TextView) row.findViewById(R.id.surahListArabic))
          .setText(surah.arabicName.replace("سُورَةُ ", ""));
      row.setContentDescription("Baca surah " + surah.name + ", " + surah.arabic.length + " ayat");
      row.setOnClickListener(view -> openSurah(index, false));
      surahList.addView(row);
    }
    findViewById(R.id.quranEmpty)
        .setVisibility(surahList.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    applyNightMode(nightMode);
  }

  private void openSurah(int index, boolean restoreBookmark) {
    if (!restoreBookmark)
      preferences
          .edit()
          .putString("last_surah", SurahData.ALL[index].name)
          .putInt("last_ayah", 1)
          .apply();
    selectedIndex = index;
    surahSelector.setSelection(index);
    showReader();
    renderSurah(restoreBookmark);
    InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (keyboard != null)
      keyboard.hideSoftInputFromWindow(findViewById(R.id.quranSearch).getWindowToken(), 0);
  }

  private void showReader() {
    readingOpen = true;
    findViewById(R.id.quranCatalog).setVisibility(View.GONE);
    findViewById(R.id.quranReader).setVisibility(View.VISIBLE);
    ((ScrollView) findViewById(R.id.quranScroll)).scrollTo(0, 0);
  }

  private void showCatalog() {
    readingOpen = false;
    findViewById(R.id.quranCatalog).setVisibility(View.VISIBLE);
    findViewById(R.id.quranReader).setVisibility(View.GONE);
    refreshBookmarkLabels();
    ((ScrollView) findViewById(R.id.quranScroll)).scrollTo(0, 0);
  }

  @Override
  public void onBackPressed() {
    if (readingOpen) showCatalog();
    else super.onBackPressed();
  }

  private void renderSurah(boolean restoreBookmark) {
    clearBookmarkLayout();
    SurahData surah = SurahData.ALL[selectedIndex];
    ((TextView) findViewById(R.id.surahArabicTitle)).setText(surah.arabicName);
    ((TextView) findViewById(R.id.surahMetadata))
        .setText(surah.name + " · " + surah.arabic.length + " ayat · Juz " + surah.juz);
    findViewById(R.id.surahBismillah).setVisibility(surah.number == 1 ? View.GONE : View.VISIBLE);
    ayahContainer.removeAllViews();
    int lastAyah = Math.min(surah.arabic.length, Math.max(1, preferences.getInt("last_ayah", 1)));
    for (int i = 0; i < surah.arabic.length; i++) {
      final int ayah = i + 1;
      View card = getLayoutInflater().inflate(R.layout.reading_item_ayah, ayahContainer, false);
      ((TextView) card.findViewById(R.id.ayahNumber)).setText(String.valueOf(ayah));
      ((TextView) card.findViewById(R.id.ayahArabic)).setText(surah.arabic[i]);
      ((TextView) card.findViewById(R.id.ayahTranslation)).setText(surah.translations[i]);
      card.findViewById(R.id.ayahBookmark)
          .setOnClickListener(
              view -> {
                preferences
                    .edit()
                    .putString("last_surah", surah.name)
                    .putInt("last_ayah", ayah)
                    .apply();
                refreshBookmarkLabels();
                toast(surah.name + " ayat " + ayah + " disimpan");
              });
      ayahContainer.addView(card);
    }
    refreshBookmarkLabels();
    applyNightMode(nightMode);
    if (restoreBookmark && lastAyah > 1) {
      scrollToBookmark(ayahContainer.getChildAt(lastAyah - 1));
    }
  }

  private void scrollToBookmark(View bookmark) {
    clearBookmarkLayout();
    ScrollView scroll = findViewById(R.id.quranScroll);
    pendingBookmarkLayout =
        () -> {
          if (!readingOpen || bookmark.getHeight() == 0 || scroll.getHeight() == 0) return;
          clearBookmarkLayout();
          int[] versePosition = new int[2], scrollPosition = new int[2];
          bookmark.getLocationInWindow(versePosition);
          scroll.getLocationInWindow(scrollPosition);
          scroll.scrollTo(
              0, Math.max(0, versePosition[1] - scrollPosition[1] + scroll.getScrollY() - dp(12)));
        };
    ayahContainer.getViewTreeObserver().addOnGlobalLayoutListener(pendingBookmarkLayout);
    ayahContainer.requestLayout();
  }

  private void clearBookmarkLayout() {
    if (pendingBookmarkLayout != null
        && ayahContainer != null
        && ayahContainer.getViewTreeObserver().isAlive()) {
      ayahContainer.getViewTreeObserver().removeOnGlobalLayoutListener(pendingBookmarkLayout);
      pendingBookmarkLayout = null;
    }
  }

  @Override
  protected void onDestroy() {
    clearBookmarkLayout();
    super.onDestroy();
  }

  private void refreshBookmarkLabels() {
    String lastSurah = preferences.getString("last_surah", "Al-Fatihah");
    int lastAyah = preferences.getInt("last_ayah", 1);
    ((TextView) findViewById(R.id.lastReadLabel)).setText(lastSurah + "  ·  Ayat " + lastAyah);
    for (int i = 0; i < ayahContainer.getChildCount(); i++) {
      Button button = ayahContainer.getChildAt(i).findViewById(R.id.ayahBookmark);
      boolean selected = lastSurah.equals(SurahData.ALL[selectedIndex].name) && lastAyah == i + 1;
      button.setText(selected ? "✓ Bacaan terakhir" : "Tandai terakhir dibaca");
    }
  }

  private void applyNightMode(boolean enabled) {
    nightMode = enabled;
    int foreground = Color.parseColor(enabled ? "#EEF6F2" : "#15332F");
    int secondary = Color.parseColor(enabled ? "#B4C8BE" : "#7C8B8A");
    int background = Color.parseColor(enabled ? "#10211F" : "#F8F9F5");
    int cardColor = Color.parseColor(enabled ? "#1D3530" : "#FFFFFF");
    findViewById(R.id.quranRoot).setBackgroundColor(background);
    ((TextView) findViewById(R.id.quranTitle)).setTextColor(foreground);
    ((TextView) findViewById(R.id.quranSurahHeading)).setTextColor(foreground);
    ((TextView) findViewById(R.id.lastReadLabel)).setTextColor(foreground);
    ((TextView) findViewById(R.id.quranSearch)).setTextColor(foreground);
    findViewById(R.id.quranSearchCard).setBackground(card(cardColor));
    findViewById(R.id.quranResume)
        .setBackground(card(Color.parseColor(enabled ? "#233D33" : "#EDF3EA")));
    nightSwitch.setTextColor(foreground);
    ((android.widget.ImageButton) findViewById(R.id.toolbarBack)).setColorFilter(foreground);
    for (int id : secondaryLabels) ((TextView) findViewById(id)).setTextColor(secondary);
    findViewById(R.id.readingSelectionCard).setBackground(card(cardColor));
    for (int i = 0; i < surahList.getChildCount(); i++) {
      View row = surahList.getChildAt(i);
      row.setBackground(card(cardColor));
      ((TextView) row.findViewById(R.id.surahListName)).setTextColor(foreground);
      ((TextView) row.findViewById(R.id.surahListInfo)).setTextColor(secondary);
      ((TextView) row.findViewById(R.id.surahListArabic))
          .setTextColor(enabled ? Color.parseColor("#B4C8BE") : Color.parseColor("#597E6B"));
    }
    for (int i = 0; i < ayahContainer.getChildCount(); i++) {
      View ayah = ayahContainer.getChildAt(i);
      ayah.setBackground(card(cardColor));
      ((TextView) ayah.findViewById(R.id.ayahArabic)).setTextColor(foreground);
      ((TextView) ayah.findViewById(R.id.ayahTranslation)).setTextColor(secondary);
    }
    if (surahSelector.getSelectedView() instanceof TextView)
      ((TextView) surahSelector.getSelectedView()).setTextColor(foreground);
    // Keep system icon contrast consistent with the reader's current palette.
    int decorFlags = getWindow().getDecorView().getSystemUiVisibility();
    int lightFlags =
        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(enabled ? decorFlags & ~lightFlags : decorFlags | lightFlags);
    getWindow().setStatusBarColor(background);
    getWindow().setNavigationBarColor(background);
    NavDock.setNightMode((FrameLayout) findViewById(R.id.quranPageRoot), enabled);
  }

  private GradientDrawable card(int color) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dp(22));
    return drawable;
  }

  private void updateSensorLabel() {
    lightStatus.setText(
        lightSensor == null
            ? "Sensor cahaya tidak tersedia. Gunakan sakelar mode malam."
            : manualOverride
                ? "Mode manual · tekan lama sakelar untuk kembali otomatis."
                : "Cahaya otomatis · mode malam di bawah 10 lux, terang di atas 15 lux.");
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (lightSensor != null
        && !sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
      lightSensor = null;
      updateSensorLabel();
    }
  }

  @Override
  protected void onPause() {
    if (sensorManager != null) sensorManager.unregisterListener(this);
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putBoolean("night_mode", nightMode);
    state.putBoolean("manual_override", manualOverride);
    state.putBoolean("reading_open", readingOpen);
    super.onSaveInstanceState(state);
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() != Sensor.TYPE_LIGHT || event.values.length == 0) return;
    lastAmbientLux = event.values[0];
    applyAmbientLight();
  }

  private void applyAmbientLight() {
    if (manualOverride || Float.isNaN(lastAmbientLux) || lastAmbientLux < 0f) return;
    // Light sensors report only changes. Reuse the latest measurement when the
    // user restores automatic mode, even if the room's light has not changed.
    boolean next = nightMode ? lastAmbientLux <= 15f : lastAmbientLux < 10f;
    if (next != nightMode) {
      applyingSensor = true;
      nightSwitch.setChecked(next);
      applyingSensor = false;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
