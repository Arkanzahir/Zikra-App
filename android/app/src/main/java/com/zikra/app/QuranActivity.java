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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import com.zikra.app.reading.*;
import java.util.List;
import java.util.Locale;

public class QuranActivity extends BaseActivity implements SensorEventListener {
  public static final String EXTRA_TARGET_CHAPTER = "target_chapter";
  public static final String EXTRA_TARGET_VERSE = "target_verse";
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
  private List<QuranChapters.Chapter> chapters;
  private QuranRepository repository;
  private QuranPage loadedPage;
  private int pendingVerse = 1, currentPage = 1;
  private boolean suspended, loading;
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
    String theme = preferences.getString("reading_theme", "light");
    nightMode =
        savedInstanceState != null
            ? savedInstanceState.getBoolean("night_mode")
            : "dark".equals(theme);
    manualOverride =
        savedInstanceState != null
            ? savedInstanceState.getBoolean("manual_override")
            : !"auto".equals(theme);
    chapters = QuranChapters.all(this);
    repository = new QuranRepository(this);
    int lastId = preferences.getInt("last_chapter", 0);
    if (lastId < 1 || lastId > 114) {
      lastId = 1;
      String old = preferences.getString("last_surah", "Al-Fatihah");
      for (QuranChapters.Chapter c : chapters) if (c.name.equals(old)) lastId = c.id;
      preferences.edit().putInt("last_chapter", lastId).apply();
    }
    selectedIndex =
        savedInstanceState != null ? savedInstanceState.getInt("chapter", lastId) - 1 : lastId - 1;
    pendingVerse =
        savedInstanceState != null
            ? savedInstanceState.getInt("verse", 1)
            : preferences.getInt("last_ayah", 1);
    if (savedInstanceState == null) {
      int target = getIntent().getIntExtra(EXTRA_TARGET_CHAPTER, 0);
      if (target >= 1 && target <= 114) {
        selectedIndex = target - 1;
        pendingVerse = getIntent().getIntExtra(EXTRA_TARGET_VERSE, 1);
        readingOpen = true;
      }
    }
    selectedIndex = Math.max(0, Math.min(113, selectedIndex));
    pendingVerse = Math.max(1, Math.min(chapters.get(selectedIndex).count, pendingVerse));

    findViewById(R.id.quranCatalogButton).setOnClickListener(view -> showCatalog());
    findViewById(R.id.quranHeaderSearch).setOnClickListener(v -> toggleSearch());
    findViewById(R.id.quranReadingOptions).setOnClickListener(v -> showReadingOptions());
    findViewById(R.id.quranBackToCatalog).setOnClickListener(view -> showCatalog());
    findViewById(R.id.toolbarBack)
        .setOnClickListener(
            view -> {
              if (readingOpen) showCatalog();
              else finish();
            });
    findViewById(R.id.quranResume)
        .setOnClickListener(
            v -> {
              int id = preferences.getInt("last_chapter", 1);
              openSurah(Math.max(0, Math.min(113, id - 1)), true);
            });
    findViewById(R.id.quranRetry).setOnClickListener(v -> renderSurah(true));
    findViewById(R.id.quranPreviousPage)
        .setOnClickListener(
            v -> {
              pendingVerse = Math.max(1, (currentPage - 2) * QuranPage.PAGE_SIZE + 1);
              renderSurah(true);
            });
    findViewById(R.id.quranNextPage)
        .setOnClickListener(
            v -> {
              pendingVerse = currentPage * QuranPage.PAGE_SIZE + 1;
              renderSurah(true);
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
    String[] labels = new String[chapters.size()];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = chapters.get(i).id + ". " + chapters.get(i).name;
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setTextColor(Color.parseColor(nightMode ? "#F5F7F6" : "#15332F"));
            view.setTextSize(15);
            view.setGravity(android.view.Gravity.CENTER);
            return view;
          }
        };
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    surahSelector.setAdapter(adapter);
    surahSelector.setSelection(selectedIndex);
    surahSelector.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position == selectedIndex) return;
            openSurah(position, false);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    nightSwitch.setChecked(nightMode);
    nightSwitch.setOnCheckedChangeListener(
        (button, checked) -> {
          if (!applyingSensor) {
            manualOverride = true;
            preferences.edit().putString("reading_theme", checked ? "dark" : "light").apply();
          }
          applyNightMode(checked);
          updateSensorLabel();
        });
    nightSwitch.setOnLongClickListener(
        view -> {
          manualOverride = false;
          preferences.edit().putString("reading_theme", "auto").apply();
          applyAmbientLight();
          updateSensorLabel();
          toast("Mode cahaya otomatis diaktifkan kembali");
          return true;
        });
    applyNightMode(nightMode);
    updateSensorLabel();
    renderCatalog("");
    refreshBookmarkLabels();
    if (readingOpen) {
      showReader();
      renderSurah(true);
    } else showCatalog();
  }

  private void renderCatalog(String query) {
    String normalized = normalize(query);
    surahList.removeAllViews();
    for (int i = 0; i < chapters.size(); i++) {
      final int index = i;
      QuranChapters.Chapter surah = chapters.get(i);
      if (!normalize(surah.name).contains(normalized)
          && !normalize(surah.meaning).contains(normalized)
          && !surah.arabic.contains(normalized)
          && !String.valueOf(surah.id).equals(normalized)) continue;
      View row = getLayoutInflater().inflate(R.layout.reading_item_surah, surahList, false);
      ((TextView) row.findViewById(R.id.surahListNumber)).setText(String.valueOf(surah.id));
      ((TextView) row.findViewById(R.id.surahListName)).setText(surah.name);
      ((TextView) row.findViewById(R.id.surahListInfo))
          .setText(surah.place + " · " + surah.count + " ayat");
      ((TextView) row.findViewById(R.id.surahListArabic))
          .setText(surah.arabic.replace("سُورَةُ ", ""));
      row.setContentDescription("Baca surah " + surah.name + ", " + surah.count + " ayat");
      row.setOnClickListener(view -> openSurah(index, false));
      surahList.addView(row);
    }
    findViewById(R.id.quranEmpty)
        .setVisibility(surahList.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    applyNightMode(nightMode);
  }

  private void openSurah(int index, boolean restoreBookmark) {
    selectedIndex = index;
    pendingVerse = restoreBookmark ? preferences.getInt("last_ayah", 1) : 1;
    pendingVerse = Math.max(1, Math.min(chapters.get(index).count, pendingVerse));
    surahSelector.setSelection(index);
    showReader();
    renderSurah(true);
    InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (keyboard != null)
      keyboard.hideSoftInputFromWindow(findViewById(R.id.quranSearch).getWindowToken(), 0);
  }

  private void showReader() {
    readingOpen = true;
    findViewById(R.id.quranTitle).setVisibility(View.GONE);
    findViewById(R.id.quranHeaderSearch).setVisibility(View.GONE);
    findViewById(R.id.quranCatalogButton).setVisibility(View.VISIBLE);
    findViewById(R.id.quranReadingOptions).setVisibility(View.VISIBLE);
    surahSelector.setVisibility(View.VISIBLE);
    findViewById(R.id.quranPageRoot).findViewWithTag("zikraDock").setVisibility(View.GONE);
    findViewById(R.id.quranScroll).setPadding(dp(20), 0, dp(20), dp(24));
    findViewById(R.id.quranCatalog).setVisibility(View.GONE);
    findViewById(R.id.quranReader).setVisibility(View.VISIBLE);
    ((ScrollView) findViewById(R.id.quranScroll)).scrollTo(0, 0);
  }

  private void showCatalog() {
    readingOpen = false;
    repository.cancel();
    loading = false;
    clearBookmarkLayout();
    findViewById(R.id.quranTitle).setVisibility(View.VISIBLE);
    findViewById(R.id.quranHeaderSearch).setVisibility(View.VISIBLE);
    findViewById(R.id.quranCatalogButton).setVisibility(View.GONE);
    findViewById(R.id.quranReadingOptions).setVisibility(View.GONE);
    surahSelector.setVisibility(View.GONE);
    findViewById(R.id.quranPageRoot).findViewWithTag("zikraDock").setVisibility(View.VISIBLE);
    findViewById(R.id.quranScroll).setPadding(dp(20), 0, dp(20), dp(88));
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
    QuranChapters.Chapter surah = chapters.get(selectedIndex);
    pendingVerse = Math.max(1, Math.min(surah.count, pendingVerse));
    currentPage = QuranPage.pageFor(pendingVerse);
    ((TextView) findViewById(R.id.surahArabicTitle)).setText(surah.arabic);
    ((TextView) findViewById(R.id.surahMetadata))
        .setText(surah.name + "\n" + surah.meaning + "\n" + surah.count + " ayat · " + surah.place);
    findViewById(R.id.surahBismillah)
        .setVisibility(
            surah.id == 1 || surah.id == 9 || currentPage > 1 ? View.GONE : View.VISIBLE);
    ayahContainer.removeAllViews();
    loadedPage = null;
    loading = true;
    findViewById(R.id.quranLoading).setVisibility(View.VISIBLE);
    findViewById(R.id.quranRetry).setVisibility(View.GONE);
    findViewById(R.id.quranPagination).setVisibility(View.GONE);
    ((TextView) findViewById(R.id.quranLoadStatus)).setText("Memuat " + surah.name + "…");
    repository.load(
        surah,
        currentPage,
        new QuranRepository.Result() {
          @Override
          public void ready(QuranPage page, String source) {
            if (!readingOpen || isFinishing() || isDestroyed()) return;
            loading = false;
            loadedPage = page;
            findViewById(R.id.quranLoading).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.quranLoadStatus))
                .setText(
                    source
                        + " · Ayat "
                        + page.verses.get(0).number
                        + "–"
                        + page.verses.get(page.verses.size() - 1).number
                        + (QuranPage.builtIn(surah.id) == null
                            ? "\nArab Utsmani · Terjemahan Kemenag RI"
                            : ""));
            for (QuranPage.Verse verse : page.verses) {
              View row =
                  getLayoutInflater().inflate(R.layout.reading_item_ayah, ayahContainer, false);
              row.setTag(verse.key);
              ((TextView) row.findViewById(R.id.ayahNumber)).setText(String.valueOf(verse.number));
              ((TextView) row.findViewById(R.id.ayahArabic)).setText(verse.arabic);
              String translation = verse.translation.replaceAll("(?is)<sup[^>]*>.*?</sup>", "");
              ((TextView) row.findViewById(R.id.ayahTranslation))
                  .setText(
                      android.text.Html.fromHtml(
                              translation, android.text.Html.FROM_HTML_MODE_LEGACY)
                          .toString()
                          .trim());
              row.findViewById(R.id.ayahBookmark)
                  .setOnClickListener(
                      v -> {
                        saveLastRead(surah, verse.number);
                        refreshBookmarkLabels();
                        toast(surah.name + " ayat " + verse.number + " disimpan");
                      });
              row.findViewById(R.id.ayahMore)
                  .setOnClickListener(v -> showVerseMenu(v, surah, verse, row));
              ayahContainer.addView(row);
            }
            saveLastRead(surah, pendingVerse);
            refreshBookmarkLabels();
            applyNightMode(nightMode);
            ((TextView) findViewById(R.id.quranPageLabel))
                .setText("Bagian " + page.page + " dari " + page.pages);
            findViewById(R.id.quranPagination)
                .setVisibility(page.pages > 1 ? View.VISIBLE : View.GONE);
            findViewById(R.id.quranPreviousPage).setEnabled(page.page > 1);
            findViewById(R.id.quranNextPage).setEnabled(page.page < page.pages);
            View target = ayahContainer.findViewWithTag(surah.id + ":" + pendingVerse);
            if (target != null && pendingVerse > 1) scrollToBookmark(target);
            else
              ((ScrollView) findViewById(R.id.quranScroll))
                  .post(() -> ((ScrollView) findViewById(R.id.quranScroll)).scrollTo(0, 0));
          }

          @Override
          public void failed(String message) {
            loading = false;
            findViewById(R.id.quranLoading).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.quranLoadStatus)).setText(message);
            findViewById(R.id.quranRetry).setVisibility(View.VISIBLE);
          }
        });
  }

  private void saveLastRead(QuranChapters.Chapter surah, int verse) {
    preferences
        .edit()
        .putInt("last_chapter", surah.id)
        .putString("last_surah", surah.name)
        .putInt("last_ayah", verse)
        .putString("last_verse_key", surah.id + ":" + verse)
        .apply();
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
    if (repository != null) repository.close();
    super.onDestroy();
  }

  private void refreshBookmarkLabels() {
    String lastSurah = preferences.getString("last_surah", "Al-Fatihah");
    int lastAyah = preferences.getInt("last_ayah", 1);
    ((TextView) findViewById(R.id.lastReadLabel)).setText(lastSurah + "  ·  Ayat " + lastAyah);
    for (int i = 0; i < ayahContainer.getChildCount(); i++) {
      ImageButton button = ayahContainer.getChildAt(i).findViewById(R.id.ayahBookmark);
      boolean selected =
          ayahContainer
              .getChildAt(i)
              .getTag()
              .equals(preferences.getInt("last_chapter", 1) + ":" + lastAyah);
      button.setSelected(selected);
      button.setImageResource(
          selected ? R.drawable.reading_ic_bookmark_filled : R.drawable.reading_ic_bookmark);
      button.setContentDescription(
          (selected ? "Bacaan terakhir: " : "Tandai terakhir dibaca: ")
              + ayahContainer.getChildAt(i).getTag());
      ((TextView) ayahContainer.getChildAt(i).findViewById(R.id.ayahLastReadStatus))
          .setText(selected ? "Bacaan terakhir" : "");
    }
  }

  private void applyNightMode(boolean enabled) {
    nightMode = enabled;
    int foreground = Color.parseColor(enabled ? "#EEF6F2" : "#15332F");
    int secondary = Color.parseColor(enabled ? "#B4C8BE" : "#586F62");
    int background = Color.parseColor(enabled ? "#10211F" : "#FFFFFF");
    int cardColor = Color.parseColor(enabled ? "#1D3530" : "#FFFFFF");
    findViewById(R.id.quranRoot).setBackgroundColor(background);
    ((TextView) findViewById(R.id.quranTitle)).setTextColor(foreground);
    ((TextView) findViewById(R.id.quranSurahHeading)).setTextColor(foreground);
    ((TextView) findViewById(R.id.lastReadLabel)).setTextColor(foreground);
    ((TextView) findViewById(R.id.quranSearch)).setTextColor(foreground);
    ((TextView) findViewById(R.id.quranResumeHeading)).setTextColor(secondary);
    ((TextView) findViewById(R.id.surahBismillah)).setTextColor(foreground);
    findViewById(R.id.quranHeaderDivider).setBackgroundColor(enabled ? 0xFF29413D : 0xFFE9EDE8);
    for (int id :
        new int[] {R.id.quranHeaderSearch, R.id.quranCatalogButton, R.id.quranReadingOptions})
      ((ImageButton) findViewById(id))
          .setImageTintList(android.content.res.ColorStateList.valueOf(foreground));
    findViewById(R.id.quranSearchCard).setBackground(card(cardColor));
    findViewById(R.id.quranResume)
        .setBackground(card(Color.parseColor(enabled ? "#233D33" : "#EDF3EA")));
    nightSwitch.setTextColor(foreground);
    ((android.widget.ImageButton) findViewById(R.id.toolbarBack)).setColorFilter(foreground);
    for (int id : secondaryLabels) ((TextView) findViewById(id)).setTextColor(secondary);
    for (int i = 0; i < surahList.getChildCount(); i++) {
      View row = surahList.getChildAt(i);
      row.setBackgroundColor(background);
      row.findViewById(R.id.surahListDivider).setBackgroundColor(enabled ? 0xFF29413D : 0xFFEBEEE9);
      ((TextView) row.findViewById(R.id.surahListNumber))
          .setTextColor(enabled ? 0xFFD0AC6B : 0xFF7F6739);
      ((TextView) row.findViewById(R.id.surahListName)).setTextColor(foreground);
      ((TextView) row.findViewById(R.id.surahListInfo)).setTextColor(secondary);
      ((TextView) row.findViewById(R.id.surahListArabic))
          .setTextColor(enabled ? Color.parseColor("#B4C8BE") : Color.parseColor("#597E6B"));
    }
    for (int i = 0; i < ayahContainer.getChildCount(); i++) {
      View ayah = ayahContainer.getChildAt(i);
      ayah.setBackgroundColor(background);
      ayah.findViewById(R.id.ayahDivider).setBackgroundColor(enabled ? 0xFF29413D : 0xFFE3E8E2);
      int gold = enabled ? 0xFFD0AC6B : 0xFF92713E;
      ((TextView) ayah.findViewById(R.id.ayahNumber)).setTextColor(gold);
      ((TextView) ayah.findViewById(R.id.ayahLastReadStatus)).setTextColor(gold);
      ImageButton bookmark = ayah.findViewById(R.id.ayahBookmark);
      bookmark.setImageTintList(
          android.content.res.ColorStateList.valueOf(bookmark.isSelected() ? gold : secondary));
      ((ImageButton) ayah.findViewById(R.id.ayahMore))
          .setImageTintList(android.content.res.ColorStateList.valueOf(secondary));
      ((TextView) ayah.findViewById(R.id.ayahArabic)).setTextColor(foreground);
      ((TextView) ayah.findViewById(R.id.ayahTranslation)).setTextColor(secondary);
    }
    ((TextView) findViewById(R.id.quranLoadStatus)).setTextColor(secondary);
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
    drawable.setCornerRadius(dp(8));
    return drawable;
  }

  private static String normalize(String value) {
    return value
        .trim()
        .toLowerCase(Locale.ROOT)
        .replace(" ", "")
        .replace("’", "")
        .replace("'", "")
        .replace("-", "");
  }

  private void toggleSearch() {
    View field = findViewById(R.id.quranSearchCard);
    EditText input = findViewById(R.id.quranSearch);
    boolean open = field.getVisibility() != View.VISIBLE;
    field.setVisibility(open ? View.VISIBLE : View.GONE);
    ImageButton button = findViewById(R.id.quranHeaderSearch);
    button.setImageResource(open ? R.drawable.reading_ic_close : R.drawable.reading_ic_search);
    button.setContentDescription(open ? "Tutup pencarian" : "Cari surah");
    InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (open) {
      input.requestFocus();
      input.post(
          () -> {
            if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
          });
    } else {
      input.setText("");
      if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }
  }

  private void showReadingOptions() {
    int selected = manualOverride ? (nightMode ? 1 : 0) : 2;
    new android.app.AlertDialog.Builder(this)
        .setTitle("Tampilan bacaan")
        .setSingleChoiceItems(
            new String[] {"Terang", "Malam", "Otomatis sesuai cahaya"},
            selected,
            (dialog, which) -> {
              preferences
                  .edit()
                  .putString("reading_theme", which == 0 ? "light" : which == 1 ? "dark" : "auto")
                  .apply();
              manualOverride = which != 2;
              applyingSensor = true;
              nightSwitch.setChecked(which == 1);
              applyingSensor = false;
              if (which == 2) applyAmbientLight();
              updateSensorLabel();
              dialog.dismiss();
            })
        .setNegativeButton("Tutup", null)
        .show();
  }

  private void showVerseMenu(
      View anchor, QuranChapters.Chapter chapter, QuranPage.Verse verse, View row) {
    android.widget.PopupMenu menu = new android.widget.PopupMenu(this, anchor);
    menu.getMenu().add(0, 1, 0, "Salin ayat");
    menu.getMenu().add(0, 2, 1, "Bagikan ayat");
    menu.setOnMenuItemClickListener(
        item -> {
          String text =
              verse.arabic
                  + "\n\n"
                  + ((TextView) row.findViewById(R.id.ayahTranslation)).getText()
                  + "\n\n"
                  + chapter.name
                  + " "
                  + verse.key
                  + " · Quran.com";
          if (item.getItemId() == 1) {
            android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
              clipboard.setPrimaryClip(
                  android.content.ClipData.newPlainText(chapter.name + " " + verse.key, text));
              toast("Ayat disalin");
            }
          } else {
            android.content.Intent share =
                new android.content.Intent(android.content.Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(android.content.Intent.EXTRA_TEXT, text);
            try {
              startActivity(android.content.Intent.createChooser(share, "Bagikan ayat"));
            } catch (android.content.ActivityNotFoundException e) {
              toast("Aplikasi berbagi belum tersedia.");
            }
          }
          return true;
        });
    menu.show();
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
    if (suspended && readingOpen) {
      suspended = false;
      renderSurah(true);
    }
    if (lightSensor != null
        && !sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
      lightSensor = null;
      updateSensorLabel();
    }
  }

  @Override
  protected void onPause() {
    if (sensorManager != null) sensorManager.unregisterListener(this);
    suspended = loading;
    repository.cancel();
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putInt("chapter", selectedIndex + 1);
    state.putInt(
        "verse",
        preferences.getInt("last_chapter", 1) == selectedIndex + 1
            ? preferences.getInt("last_ayah", pendingVerse)
            : pendingVerse);
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
