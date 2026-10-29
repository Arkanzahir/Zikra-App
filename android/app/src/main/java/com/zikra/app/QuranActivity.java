package com.zikra.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import com.zikra.app.reading.*;
import java.util.List;

public class QuranActivity extends BaseActivity {
  public static final String EXTRA_TARGET_CHAPTER = "target_chapter";
  public static final String EXTRA_TARGET_VERSE = "target_verse";
  public static final String EXTRA_RESUME_READING = "resume_reading";
  public static final String EXTRA_RANGE_FIRST = "audio_first";
  public static final String EXTRA_RANGE_LAST = "audio_last";
  private com.zikra.app.audio.ReaderAudioUi audioUi;
  private TextView wordText;
  private List<String> wordSource;
  private String wordKey = "";
  private ArabicWordMap wordMap;
  private int lastWord = -1;
  private boolean lastWordNight;
  private int rangeChapter, rangeFirst, rangeLast;
  private SharedPreferences preferences;
  private LinearLayout ayahContainer;
  private Switch nightSwitch;
  private TextView lightStatus;
  private Spinner surahSelector;
  private boolean nightMode;
  private boolean manualOverride;
  private List<QuranChapters.Chapter> chapters;
  private QuranRepository repository;
  private QuranPage loadedPage;
  private int pendingVerse = 1, currentPage = 1;
  private boolean suspended, loading;
  private int selectedIndex;
  private boolean readingOpen;
  private int arabicSize, translationSize;
  private android.app.AlertDialog readingDialog, jumpDialog;
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
    arabicSize = Math.max(24, Math.min(44, preferences.getInt("reading_arabic_size", 32)));
    translationSize =
        Math.max(14, Math.min(24, preferences.getInt("reading_translation_size", 16)));
    ayahContainer = findViewById(R.id.ayahContainer);
    nightSwitch = findViewById(R.id.readingNightSwitch);
    lightStatus = findViewById(R.id.lightSensorStatus);
    surahSelector = findViewById(R.id.surahSelector);
    surahList = findViewById(R.id.quranSurahList);
    if (getResources().getConfiguration().fontScale >= 1.5f
        || getResources().getConfiguration().screenWidthDp < 360) {
      LinearLayout heading = findViewById(R.id.quranSurahHeader);
      heading.setOrientation(LinearLayout.VERTICAL);
      TextView metadata = findViewById(R.id.surahMetadata);
      metadata.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
      surahSelector.getLayoutParams().height = dp(48);
      TextView arabicHeading = findViewById(R.id.surahArabicTitle);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
      params.topMargin = dp(12);
      arabicHeading.setLayoutParams(params);
    }
    readingOpen =
        savedInstanceState != null
            ? savedInstanceState.getBoolean("reading_open")
            : getIntent().getBooleanExtra(EXTRA_RESUME_READING, false);
    nightMode = com.zikra.app.ui.AppAppearance.dark(this);
    manualOverride = !"system".equals(com.zikra.app.ui.AppAppearance.selected(this));
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
    findViewById(R.id.quranCatalogMushaf).setOnClickListener(v -> openMushaf());
    findViewById(R.id.quranBrowseJuz).setOnClickListener(v -> openMushaf(true));
    findViewById(R.id.quranBookmark)
        .setOnClickListener(
            v -> {
              selectedIndex =
                  Math.max(
                      0, Math.min(chapters.size() - 1, preferences.getInt("last_chapter", 1) - 1));
              openSurah(selectedIndex, true);
            });
    findViewById(R.id.quranReaderMushaf).setOnClickListener(v -> openMushaf());
    findViewById(R.id.quranReaderFont).setOnClickListener(v -> showReadingOptions());
    findViewById(R.id.quranReaderAudio).setOnClickListener(v -> audioUi.togglePanel());
    findViewById(R.id.quranCatalogDownloads)
        .setOnClickListener(
            v -> {
              showReader();
              if (loadedPage == null) renderSurah(true);
              audioUi.downloads();
            });
    Switch wordMode = findViewById(R.id.quranWordFollow);
    wordMode.setChecked(preferences.getBoolean("reader_word_sync", true));
    wordMode.setText(
        wordMode.isChecked() ? R.string.layout_word_follow : R.string.layout_ayah_follow);
    wordMode.setOnCheckedChangeListener(
        (view, enabled) -> {
          preferences.edit().putBoolean("reader_word_sync", enabled).apply();
          wordMode.setText(enabled ? R.string.layout_word_follow : R.string.layout_ayah_follow);
          clearWordHighlight();
          if (audioUi != null) audioUi.wordModeChanged();
        });
    findViewById(R.id.quranReaderAuto).setOnClickListener(v -> audioUi.toggleAutoScroll());
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
      labels[i] = chapters.get(i).name;
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
    // Global preferences are authoritative; restoring an old checkbox would undo the new theme.
    nightSwitch.setSaveEnabled(false);
    nightSwitch.setSaveFromParentEnabled(false);
    nightSwitch.setChecked(nightMode);
    nightSwitch.setOnCheckedChangeListener((button, checked) ->
        com.zikra.app.ui.AppAppearance.select(this, checked ? "dark" : "light"));
    nightSwitch.setOnLongClickListener(view -> {
      com.zikra.app.ui.AppAppearance.select(this, "system");
      return true;
    });
    rangeChapter = getIntent().getIntExtra(EXTRA_TARGET_CHAPTER, 0);
    if (rangeChapter < 1 || rangeChapter > 114) rangeChapter = selectedIndex + 1;
    int rangeCount = chapters.get(rangeChapter - 1).count;
    rangeFirst = getIntent().getIntExtra(EXTRA_RANGE_FIRST, 1);
    rangeLast = getIntent().getIntExtra(EXTRA_RANGE_LAST, rangeCount);
    if (rangeFirst < 1 || rangeFirst > rangeLast || rangeLast > rangeCount) {
      rangeFirst = 1;
      rangeLast = rangeCount;
    }
    audioUi =
        new com.zikra.app.audio.ReaderAudioUi(
            this,
            new com.zikra.app.audio.ReaderAudioUi.Host() {
              @Override
              public int chapter() {
                return selectedIndex + 1;
              }

              @Override
              public int verse() {
                return pendingVerse;
              }

              @Override
              public int first() {
                return chapter() == rangeChapter ? rangeFirst : 1;
              }

              @Override
              public int last() {
                return chapter() == rangeChapter ? rangeLast : chapters.get(selectedIndex).count;
              }

              @Override
              public void follow(int chapter, int verse) {
                followAudioVerse(chapter, verse);
              }

              @Override
              public void openAudio(int chapter, int verse) {
                if (chapter < 1 || chapter > chapters.size()) return;
                if (!readingOpen) {
                  selectedIndex = chapter - 1;
                  pendingVerse = verse;
                  surahSelector.setSelection(selectedIndex);
                  showReader();
                  renderSurah(true);
                } else followAudioVerse(chapter, verse);
              }

              @Override
              public void highlight(int chapter, int verse) {
                highlightAudio(chapter, verse);
              }

              @Override
              public boolean wordSync() {
                return preferences.getBoolean("reader_word_sync", true);
              }

              @Override
              public void wordMetadata(int chapter, int verse, List<String> words) {
                bindWordMetadata(chapter, verse, words);
              }

              @Override
              public void wordHighlight(
                  int chapter, int verse, int word, float fraction, boolean follow) {
                colorAudioWord(chapter, verse, word);
              }

              @Override
              public boolean ready() {
                return readingOpen && !loading && loadedPage != null;
              }

              @Override
              public boolean nextPage() {
                if (loadedPage == null || currentPage >= loadedPage.pages) return false;
                pendingVerse = currentPage * QuranPage.PAGE_SIZE + 1;
                renderSurah(true);
                return true;
              }
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
    surahList.removeAllViews();
    for (int i = 0; i < chapters.size(); i++) {
      final int index = i;
      QuranChapters.Chapter surah = chapters.get(i);
      if (!SurahSearch.matches(query, surah.id, surah.name, surah.meaning, surah.arabic)) continue;
      View row = getLayoutInflater().inflate(R.layout.reading_item_surah, surahList, false);
      ((TextView) row.findViewById(R.id.surahListNumber)).setText(String.valueOf(surah.id));
      ((TextView) row.findViewById(R.id.surahListName)).setText(surah.name);
      ((TextView) row.findViewById(R.id.surahListInfo))
          .setText(surah.place + " · " + surah.count + " ayat");
      ((TextView) row.findViewById(R.id.surahListArabic))
          .setText(surah.arabic.replace("سُورَةُ ", ""));
      if (getResources().getConfiguration().fontScale >= 1.5f) {
        LinearLayout names = row.findViewById(R.id.surahListNames);
        LinearLayout inner = row.findViewById(R.id.surahListContent);
        TextView arabic = row.findViewById(R.id.surahListArabic);
        inner.removeView(arabic);
        arabic.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        names.addView(arabic);
      }
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
    if (audioUi != null) audioUi.reader(true);
    findViewById(R.id.quranTitle).setVisibility(View.GONE);
    findViewById(R.id.quranReaderToolbar).setVisibility(View.VISIBLE);
    findViewById(R.id.quranHeaderSearch).setVisibility(View.GONE);
    findViewById(R.id.quranCatalogButton).setVisibility(View.GONE);
    findViewById(R.id.quranReadingOptions).setVisibility(View.VISIBLE);
    surahSelector.setVisibility(View.VISIBLE);
    NavDock.setVisible((FrameLayout) findViewById(R.id.quranPageRoot), true);
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
    findViewById(R.id.quranReaderToolbar).setVisibility(View.GONE);
    findViewById(R.id.quranHeaderSearch).setVisibility(View.VISIBLE);
    findViewById(R.id.quranCatalogButton).setVisibility(View.GONE);
    findViewById(R.id.quranReadingOptions).setVisibility(View.GONE);
    surahSelector.setVisibility(View.GONE);
    NavDock.setVisible((FrameLayout) findViewById(R.id.quranPageRoot), true);
    findViewById(R.id.quranScroll).setPadding(dp(20), 0, dp(20), dp(88));
    findViewById(R.id.quranCatalog).setVisibility(View.VISIBLE);
    findViewById(R.id.quranReader).setVisibility(View.GONE);
    refreshBookmarkLabels();
    if (audioUi != null) audioUi.reader(false);
    ((ScrollView) findViewById(R.id.quranScroll)).scrollTo(0, 0);
  }

  @Override
  public void onBackPressed() {
    if (readingOpen) showCatalog();
    else super.onBackPressed();
  }

  private void renderSurah(boolean restoreBookmark) {
    if (audioUi != null) audioUi.changedChapter();
    clearBookmarkLayout();
    QuranChapters.Chapter surah = chapters.get(selectedIndex);
    pendingVerse = Math.max(1, Math.min(surah.count, pendingVerse));
    currentPage = QuranPage.pageFor(pendingVerse);
    ((TextView) findViewById(R.id.surahArabicTitle)).setText("سُورَةُ " + surah.arabic);
    ((TextView) findViewById(R.id.surahMetadata))
        .setText(getString(R.string.layout_surah_meta, surah.id, surah.count));
    findViewById(R.id.quranSurahHeader)
        .setContentDescription(
            surah.name + ", " + surah.meaning + ", " + surah.place + ", " + surah.count + " ayat");
    findViewById(R.id.surahBismillah)
        .setVisibility(
            surah.id == 1 || surah.id == 9 || currentPage > 1 ? View.GONE : View.VISIBLE);
    ayahContainer.removeAllViews();
    loadedPage = null;
    loading = true;
    findViewById(R.id.quranLoading).setVisibility(View.VISIBLE);
    findViewById(R.id.quranLoadStatus).setVisibility(View.VISIBLE);
    findViewById(R.id.quranRetry).setVisibility(View.GONE);
    findViewById(R.id.quranPagination).setVisibility(View.GONE);
    findViewById(R.id.quranJumpToVerse).setOnClickListener(v -> showVerseJump());
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
            TextView sourceLabel = findViewById(R.id.quranLoadStatus);
            sourceLabel.setVisibility(View.GONE);
            sourceLabel.setMaxLines(1);
            sourceLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
              row.findViewById(R.id.ayahPlay)
                  .setContentDescription("Putar " + surah.name + " ayat " + verse.number);
              row.findViewById(R.id.ayahPlay)
                  .setOnClickListener(
                      v -> {
                        pendingVerse = verse.number;
                        audioUi.playVerse(verse.number);
                      });
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
                        pendingVerse = verse.number;
                        saveLastRead(surah, verse.number);
                        refreshBookmarkLabels();
                        toast(surah.name + " ayat " + verse.number + " disimpan");
                      });
              row.findViewById(R.id.ayahNumber)
                  .setOnClickListener(v -> showVerseMenu(v, surah, verse, row));
              row.findViewById(R.id.ayahNumber)
                  .setContentDescription(
                      getString(R.string.layout_verse_actions) + " " + verse.number);
              row.findViewById(R.id.ayahMore)
                  .setOnClickListener(
                      v -> showVerseMenu(row.findViewById(R.id.ayahNumber), surah, verse, row));
              row.setOnLongClickListener(
                  v -> {
                    showVerseMenu(row.findViewById(R.id.ayahNumber), surah, verse, row);
                    return true;
                  });
              ayahContainer.addView(row);
            }
            saveLastRead(surah, pendingVerse);
            refreshBookmarkLabels();
            applyReadingSizes();
            applyNightMode(nightMode);
            ((TextView) findViewById(R.id.quranPageLabel))
                .setText(
                    getString(
                        R.string.quran_verse_range,
                        page.verses.get(0).number,
                        page.verses.get(page.verses.size() - 1).number,
                        page.page,
                        page.pages));
            findViewById(R.id.quranPagination)
                .setVisibility(page.pages > 1 ? View.VISIBLE : View.GONE);
            findViewById(R.id.quranPreviousPage).setEnabled(page.page > 1);
            findViewById(R.id.quranNextPage).setEnabled(page.page < page.pages);
            View target = ayahContainer.findViewWithTag(surah.id + ":" + pendingVerse);
            if (target != null && pendingVerse > 1) scrollToBookmark(target);
            else
              ((ScrollView) findViewById(R.id.quranScroll))
                  .post(() -> ((ScrollView) findViewById(R.id.quranScroll)).scrollTo(0, 0));
            ayahContainer.post(
                () -> {
                  if (audioUi != null) audioUi.pageReady();
                });
          }

          @Override
          public void failed(String message) {
            loading = false;
            ((TextView) findViewById(R.id.quranLoadStatus)).setMaxLines(Integer.MAX_VALUE);
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

  private void followAudioVerse(int chapter, int verse) {
    if (!readingOpen
        || chapter < 1
        || chapter > 114
        || verse < 1
        || verse > chapters.get(chapter - 1).count) return;
    if (chapter != selectedIndex + 1) {
      selectedIndex = chapter - 1;
      pendingVerse = verse;
      surahSelector.setSelection(selectedIndex);
      showReader();
      renderSurah(true);
      return;
    }
    pendingVerse = verse;
    if (QuranPage.pageFor(verse) != currentPage) {
      renderSurah(true);
      return;
    }
    View row = ayahContainer.findViewWithTag(chapter + ":" + verse);
    if (row == null) return;
    row.post(
        () -> {
          if (!readingOpen || isFinishing() || isDestroyed()) return;
          ScrollView scroll = findViewById(R.id.quranScroll);
          int[] position = new int[2], viewport = new int[2];
          row.getLocationInWindow(position);
          scroll.getLocationInWindow(viewport);
          scroll.smoothScrollTo(
              0, Math.max(0, position[1] - viewport[1] + scroll.getScrollY() - dp(12)));
        });
  }

  private void highlightAudio(int chapter, int verse) {
    for (int i = 0; i < ayahContainer.getChildCount(); i++) {
      View row = ayahContainer.getChildAt(i);
      boolean active = (chapter + ":" + verse).equals(row.getTag());
      row.setBackgroundColor(
          active
              ? (nightMode ? 0xFF333C2B : getColor(R.color.concept_gold_wash))
              : (nightMode ? 0xFF10211F : getColor(R.color.concept_ivory)));
      boolean mapped = active && wordText == row.findViewById(R.id.ayahArabic) && wordMap != null;
      ((TextView) row.findViewById(R.id.ayahArabic))
          .setTextColor(
              active && !mapped
                  ? (nightMode ? 0xFFF0CB88 : getColor(R.color.concept_gold_ink))
                  : (nightMode ? 0xFFEEF6F2 : getColor(R.color.pine)));
      TextView label = row.findViewById(R.id.ayahAudioStatus);
      label.setText(
          active
              ? getString(mapped ? R.string.concept_word_active : R.string.concept_ayah_active)
              : "");
      label.setVisibility(
          active && !mapped && preferences.getBoolean("reader_word_sync", true)
              ? View.VISIBLE
              : View.GONE);
    }
  }

  private void clearWordHighlight() {
    if (wordText != null && wordText.getText() instanceof android.text.Spannable)
      for (android.text.style.ForegroundColorSpan span :
          ((android.text.Spannable) wordText.getText())
              .getSpans(0, wordText.length(), android.text.style.ForegroundColorSpan.class))
        ((android.text.Spannable) wordText.getText()).removeSpan(span);
    wordText = null;
    wordSource = null;
    wordMap = null;
    wordKey = "";
    lastWord = -1;
  }

  private void bindWordMetadata(int chapter, int verse, List<String> words) {
    String key = chapter + ":" + verse;
    if (wordSource == words
        && wordKey.equals(key)
        && wordText != null
        && wordText.isAttachedToWindow()) return;
    clearWordHighlight();
    if (!readingOpen || loadedPage == null || words.isEmpty()) return;
    View row = ayahContainer.findViewWithTag(key);
    if (row == null) return;
    wordText = row.findViewById(R.id.ayahArabic);
    wordSource = words;
    wordKey = key;
    wordMap = ArabicWordMap.match(wordText.getText().toString(), words);
    if (wordMap == null) {
      ((TextView) row.findViewById(R.id.ayahAudioStatus))
          .setText(R.string.concept_word_unavailable);
      return;
    }
    wordText.setText(wordText.getText(), TextView.BufferType.SPANNABLE);
    ((TextView) row.findViewById(R.id.ayahAudioStatus)).setText(R.string.concept_word_active);
    row.findViewById(R.id.ayahAudioStatus).setVisibility(View.GONE);
  }

  private void colorAudioWord(int chapter, int verse, int word) {
    if (wordText == null || !wordKey.equals(chapter + ":" + verse)) {
      if (chapter == 0) clearWordHighlight();
      return;
    }
    if (wordMap == null || (lastWord == word && lastWordNight == nightMode)) return;
    lastWord = word;
    lastWordNight = nightMode;
    android.text.Spannable text = (android.text.Spannable) wordText.getText();
    for (android.text.style.ForegroundColorSpan span :
        text.getSpans(0, text.length(), android.text.style.ForegroundColorSpan.class))
      text.removeSpan(span);
    wordText.setTextColor(nightMode ? 0xFFEEF6F2 : getColor(R.color.pine));
    if (word > 0 && word <= wordMap.starts.length)
      text.setSpan(
          new android.text.style.ForegroundColorSpan(
              nightMode ? 0xFFF0CB88 : getColor(R.color.concept_gold_ink)),
          wordMap.starts[word - 1],
          wordMap.ends[word - 1],
          android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (audioUi != null) audioUi.start();
  }

  @Override
  protected void onStop() {
    if (audioUi != null) audioUi.stop();
    super.onStop();
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
    if (readingDialog != null) readingDialog.dismiss();
    if (jumpDialog != null) jumpDialog.dismiss();
    if (repository != null) repository.close();
    super.onDestroy();
  }

  private void refreshBookmarkLabels() {
    String lastSurah = preferences.getString("last_surah", "Al-Fatihah");
    int lastAyah = preferences.getInt("last_ayah", 1);
    ((TextView) findViewById(R.id.lastReadLabel)).setText(lastSurah + "  ·  Ayat " + lastAyah);
    for (int i = 0; i < ayahContainer.getChildCount(); i++) {
      ImageButton button = ayahContainer.getChildAt(i).findViewById(R.id.ayahBookmark);
      if (button == null) continue;
      boolean selected =
          (preferences.getInt("last_chapter", 1) + ":" + lastAyah)
              .equals(ayahContainer.getChildAt(i).getTag());
      button.setSelected(selected);
      button.setImageResource(
          selected ? R.drawable.reading_ic_bookmark_filled : R.drawable.reading_ic_bookmark);
      button.setImageTintList(
          android.content.res.ColorStateList.valueOf(
              selected
                  ? (nightMode ? 0xFFD0AC6B : 0xFF806337)
                  : (nightMode ? 0xFFB4C8BE : 0xFF586F62)));
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
    int background = Color.parseColor(enabled ? "#10211F" : "#F7F6F0");
    int cardColor = Color.parseColor(enabled ? "#1D3530" : "#FFFFFF");
    findViewById(R.id.quranRoot).setBackgroundColor(background);
    ((TextView) findViewById(R.id.quranTitle)).setTextColor(foreground);
    ((TextView) findViewById(R.id.quranSurahHeading)).setTextColor(foreground);
    ((TextView) findViewById(R.id.lastReadLabel)).setTextColor(foreground);
    ((TextView) findViewById(R.id.quranSearch)).setTextColor(foreground);
    ((TextView) findViewById(R.id.quranResumeHeading)).setTextColor(secondary);
    ((TextView) findViewById(R.id.surahBismillah)).setTextColor(foreground);
    ((TextView) findViewById(R.id.surahArabicTitle)).setTextColor(foreground);
    ((TextView) findViewById(R.id.surahMetadata)).setTextColor(secondary);
    findViewById(R.id.quranReaderMovement).setBackgroundColor(background);
    ((TextView) findViewById(R.id.quranReaderFont)).setTextColor(foreground);
    for (int id : new int[] {R.id.quranReaderFont, R.id.quranReaderMushaf, R.id.quranReaderAudio}) {
      findViewById(id).setBackground(card(enabled ? 0xFF233D33 : 0xFFFFFCF6));
      findViewById(id).setStateListAnimator(null);
    }
    findViewById(R.id.quranHeaderDivider).setBackgroundColor(enabled ? 0xFF29413D : 0xFFE9EDE8);
    for (int id :
        new int[] {R.id.quranHeaderSearch, R.id.quranCatalogButton, R.id.quranReadingOptions})
      ((ImageButton) findViewById(id))
          .setImageTintList(android.content.res.ColorStateList.valueOf(foreground));
    findViewById(R.id.quranSearchCard).setBackground(card(cardColor));
    findViewById(R.id.quranResume)
        .setBackground(card(Color.parseColor(enabled ? "#233D33" : "#EDF3EA")));
    nightSwitch.setTextColor(foreground);
    ((TextView) findViewById(R.id.quranWordFollow)).setTextColor(foreground);
    for (int id : new int[] {R.id.quranReaderAuto, R.id.quranReaderMushaf, R.id.quranReaderAudio})
      ((TextView) findViewById(id)).setTextColor(foreground);
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
      int gold = enabled ? 0xFFD0AC6B : 0xFF806337;
      ((TextView) ayah.findViewById(R.id.ayahNumber)).setTextColor(gold);
      ((TextView) ayah.findViewById(R.id.ayahLastReadStatus)).setTextColor(gold);
      ImageButton bookmark = ayah.findViewById(R.id.ayahBookmark);
      bookmark.setImageTintList(
          android.content.res.ColorStateList.valueOf(bookmark.isSelected() ? gold : secondary));
      ((ImageButton) ayah.findViewById(R.id.ayahMore))
          .setImageTintList(android.content.res.ColorStateList.valueOf(secondary));
      ((ImageButton) ayah.findViewById(R.id.ayahPlay))
          .setImageTintList(android.content.res.ColorStateList.valueOf(foreground));
      ((TextView) ayah.findViewById(R.id.ayahAudioStatus)).setTextColor(foreground);
      ((TextView) ayah.findViewById(R.id.ayahArabic)).setTextColor(foreground);
      ((TextView) ayah.findViewById(R.id.ayahTranslation)).setTextColor(secondary);
    }
    ((TextView) findViewById(R.id.quranLoadStatus)).setTextColor(secondary);
    ((TextView) findViewById(R.id.quranPageLabel)).setTextColor(secondary);
    ((TextView) findViewById(R.id.quranJumpToVerse)).setTextColor(foreground);
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
    if (audioUi != null) audioUi.theme(enabled);
  }

  private GradientDrawable card(int color) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dp(8));
    return drawable;
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
    if (readingDialog != null) readingDialog.dismiss();
    View settings = getLayoutInflater().inflate(R.layout.quran_reading_settings, null);
    readingDialog =
        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.quran_reading_settings)
            .setView(settings)
            .setNegativeButton(R.string.quran_close, null)
            .create();
    RadioGroup themes = settings.findViewById(R.id.quranThemeChoices);
    themes.check(
        !manualOverride
            ? R.id.quranThemeAuto
            : nightMode ? R.id.quranThemeDark : R.id.quranThemeLight);
    themes.setOnCheckedChangeListener(
        (group, checked) -> {
          String choice =
              checked == R.id.quranThemeDark
                  ? "dark"
                  : checked == R.id.quranThemeAuto ? "system" : "light";
          readingDialog.dismiss();
          com.zikra.app.ui.AppAppearance.select(this, choice);
        });
    bindReadingSize(
        settings, R.id.quranArabicSize, R.id.quranArabicSizeValue, R.id.quranArabicPreview, true);
    bindReadingSize(
        settings,
        R.id.quranTranslationSize,
        R.id.quranTranslationSizeValue,
        R.id.quranTranslationPreview,
        false);
    settings
        .findViewById(R.id.quranSettingsJump)
        .setOnClickListener(
            v -> {
              readingDialog.dismiss();
              showVerseJump();
            });
    readingDialog.show();
    settings
        .findViewById(R.id.quranOpenMushaf)
        .setVisibility(android.os.Build.VERSION.SDK_INT >= 31 ? View.VISIBLE : View.GONE);
    settings.findViewById(R.id.quranOpenMushaf).setOnClickListener(v -> openMushaf());
  }

  private void openMushaf() {
    openMushaf(false);
  }

  private void openMushaf(boolean chooseJuz) {
    if (android.os.Build.VERSION.SDK_INT < 31) {
      toast(getString(R.string.design_mushaf_min_android));
      return;
    }
    int visibleVerse = pendingVerse;
    android.graphics.Rect visible = new android.graphics.Rect();
    for (int i = 0; readingOpen && i < ayahContainer.getChildCount(); i++) {
      View row = ayahContainer.getChildAt(i);
      if (row.getGlobalVisibleRect(visible) && row.getTag() instanceof String) {
        visibleVerse = Integer.parseInt(((String) row.getTag()).split(":")[1]);
        break;
      }
    }
    if (readingDialog != null) readingDialog.dismiss();
    startActivityForResult(
        new android.content.Intent(this, MushafActivity.class)
            .putExtra("browse_juz", chooseJuz)
            .putExtra(EXTRA_TARGET_CHAPTER, selectedIndex + 1)
            .putExtra(EXTRA_TARGET_VERSE, visibleVerse)
            .putExtra(EXTRA_RANGE_FIRST, selectedIndex + 1 == rangeChapter ? rangeFirst : 1)
            .putExtra(
                EXTRA_RANGE_LAST,
                selectedIndex + 1 == rangeChapter ? rangeLast : chapters.get(selectedIndex).count),
        MushafActivity.REQUEST);
  }

  @Override
  protected void onActivityResult(int request, int result, android.content.Intent data) {
    super.onActivityResult(request, result, data);
    if (request == MushafActivity.REQUEST && result == RESULT_OK && data != null) {
      int chapter = data.getIntExtra(EXTRA_TARGET_CHAPTER, 0);
      int verse = data.getIntExtra(EXTRA_TARGET_VERSE, 0);
      if (chapter >= 1
          && chapter <= 114
          && verse >= 1
          && verse <= chapters.get(chapter - 1).count) {
        selectedIndex = chapter - 1;
        pendingVerse = verse;
        readingOpen = true;
        suspended = false;
        surahSelector.setSelection(selectedIndex);
        showReader();
        renderSurah(true);
      }
    }
  }

  private void bindReadingSize(
      View root, int sliderId, int labelId, int previewId, boolean arabic) {
    SeekBar slider = root.findViewById(sliderId);
    TextView label = root.findViewById(labelId), preview = root.findViewById(previewId);
    int minimum = arabic ? 24 : 14;
    slider.setProgress((arabic ? arabicSize : translationSize) - minimum);
    Runnable update =
        () -> {
          int value = minimum + slider.getProgress();
          label.setText(getString(R.string.quran_size_value, value));
          slider.setContentDescription(
              getString(arabic ? R.string.quran_arabic_size : R.string.quran_translation_size)
                  + ", "
                  + getString(R.string.quran_size_value, value));
          preview.setTextSize(value);
        };
    update.run();
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            int value = minimum + progress;
            if (arabic) arabicSize = value;
            else translationSize = value;
            preferences
                .edit()
                .putInt(arabic ? "reading_arabic_size" : "reading_translation_size", value)
                .apply();
            update.run();
            applyReadingSizes();
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
  }

  private void applyReadingSizes() {
    ScrollView scroll = findViewById(R.id.quranScroll);
    View anchor = null;
    if (scroll.getScrollY() > 0) {
      int[] top = new int[2];
      scroll.getLocationInWindow(top);
      for (int i = 0; i < ayahContainer.getChildCount(); i++) {
        View row = ayahContainer.getChildAt(i);
        int[] position = new int[2];
        row.getLocationInWindow(position);
        if (row.getHeight() > 0 && position[1] + row.getHeight() > top[1] + dp(12)) {
          anchor = row;
          break;
        }
      }
    }
    for (int i = 0; i < ayahContainer.getChildCount(); i++) {
      View row = ayahContainer.getChildAt(i);
      ((TextView) row.findViewById(R.id.ayahArabic)).setTextSize(arabicSize);
      ((TextView) row.findViewById(R.id.ayahTranslation)).setTextSize(translationSize);
    }
    if (anchor != null) scrollToBookmark(anchor);
  }

  private void showVerseJump() {
    if (!readingOpen) return;
    if (jumpDialog != null) jumpDialog.dismiss();
    QuranChapters.Chapter chapter = chapters.get(selectedIndex);
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(24), dp(8), dp(24), 0);
    TextView range = new TextView(this);
    range.setText(getString(R.string.quran_jump_range, chapter.name, chapter.count));
    range.setTextSize(14);
    range.setTextColor(getColor(R.color.text));
    content.addView(range, new LinearLayout.LayoutParams(-1, -2));
    EditText verse = new EditText(this);
    verse.setId(R.id.quranJumpInput);
    verse.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    verse.setSingleLine(true);
    verse.setMinHeight(dp(48));
    verse.setHint(R.string.quran_jump_hint);
    verse.setContentDescription(getString(R.string.quran_jump_hint));
    verse.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(3)});
    verse.setText(String.valueOf(pendingVerse));
    verse.selectAll();
    content.addView(verse, new LinearLayout.LayoutParams(-1, -2));
    jumpDialog =
        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.quran_jump)
            .setView(content)
            .setNegativeButton(R.string.study_cancel, null)
            .setPositiveButton(R.string.quran_open_verse, null)
            .create();
    jumpDialog.setOnShowListener(
        ignored ->
            jumpDialog
                .getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      int number;
                      try {
                        number = Integer.parseInt(verse.getText().toString().trim());
                      } catch (NumberFormatException invalid) {
                        number = 0;
                      }
                      if (number < 1 || number > chapter.count) {
                        verse.setError(getString(R.string.quran_jump_invalid, chapter.count));
                        verse.requestFocus();
                        return;
                      }
                      pendingVerse = number;
                      jumpDialog.dismiss();
                      renderSurah(true);
                    }));
    jumpDialog.show();
  }

  private void showVerseMenu(
      View anchor, QuranChapters.Chapter chapter, QuranPage.Verse verse, View row) {
    android.widget.PopupMenu menu = new android.widget.PopupMenu(this, anchor);
    menu.getMenu().add(0, 3, 0, "Putar ayat " + verse.number);
    menu.getMenu().add(0, 4, 1, "Tandai terakhir dibaca");
    menu.getMenu().add(0, 1, 2, "Salin ayat");
    menu.getMenu().add(0, 2, 3, "Bagikan ayat");
    menu.getMenu().add(0, 5, 4, R.string.assistant_tafsir_action);
    menu.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == 5) {
            startActivity(new android.content.Intent(this, AiActivity.class)
                .putExtra("tadabbur_chapter", chapter.id).putExtra("tadabbur_verse", verse.number));
            return true;
          }
          if (item.getItemId() == 3) {
            row.findViewById(R.id.ayahPlay).performClick();
            return true;
          }
          if (item.getItemId() == 4) {
            row.findViewById(R.id.ayahBookmark).performClick();
            return true;
          }
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
    lightStatus.setText(R.string.app_appearance_shared);
  }

  @Override
  protected void onResume() {
    super.onResume();
    refreshBookmarkLabels();
    if (suspended && readingOpen) {
      suspended = false;
      renderSurah(true);
    }
  }

  @Override
  protected void onPause() {
    if (audioUi != null) audioUi.pause();
    suspended = loading;
    repository.cancel();
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putInt("chapter", selectedIndex + 1);
    state.putInt("verse", pendingVerse);
    state.putBoolean("night_mode", nightMode);
    state.putBoolean("manual_override", manualOverride);
    state.putBoolean("reading_open", readingOpen);
    super.onSaveInstanceState(state);
  }

}
