package com.zikra.app;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import com.zikra.app.audio.*;
import com.zikra.app.reading.*;
import com.zikra.app.reading.mushaf.*;
import java.util.*;

public final class MushafActivity extends BaseActivity implements MushafPageView.Actions {
  public static final int REQUEST = 604;
  private MushafRepository repository;
  private MushafDocument document;
  private MushafPageView pageView;
  private ReaderAudioUi audio;
  private SharedPreferences prefs;
  private int page = 1, chapter = 1, verse = 1, zoom = 100;
  private int retryPage, rangeChapter, rangeFirst, rangeLast;
  private int retryJuz;
  private boolean night, ready, chrome = true;
  private AlertDialog dialog;

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    if (android.os.Build.VERSION.SDK_INT < 31) {
      finish();
      return;
    }
    setContentView(R.layout.activity_mushaf);
    setupScreen(getString(R.string.mushaf_title));
    if (getResources().getConfiguration().fontScale >= 1.5f) {
      LinearLayout header = findViewById(R.id.mushafHeader);
      View back = findViewById(R.id.toolbarBack), title = findViewById(R.id.mushafTitle);
      View zoomButton = findViewById(R.id.mushafZoom),
          translation = findViewById(R.id.mushafTranslation);
      header.removeAllViews();
      header.setOrientation(LinearLayout.VERTICAL);
      LinearLayout heading = new LinearLayout(this), actions = new LinearLayout(this);
      heading.setGravity(Gravity.CENTER_VERTICAL);
      heading.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
      heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
      actions.addView(zoomButton, new LinearLayout.LayoutParams(0, -2, 1));
      actions.addView(translation, new LinearLayout.LayoutParams(0, -2, 1));
      header.addView(heading, new LinearLayout.LayoutParams(-1, -2));
      header.addView(actions, new LinearLayout.LayoutParams(-1, -2));
    }
    prefs = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    night =
        saved == null
            ? prefs.getBoolean(
                "mushaf_night", "dark".equals(prefs.getString("reading_theme", "light")))
            : saved.getBoolean("night");
    zoom = Math.max(100, Math.min(200, prefs.getInt("mushaf_zoom", 100)));
    chapter =
        saved == null
            ? getIntent().getIntExtra(QuranActivity.EXTRA_TARGET_CHAPTER, 1)
            : saved.getInt("chapter", 1);
    verse =
        saved == null
            ? getIntent().getIntExtra(QuranActivity.EXTRA_TARGET_VERSE, 1)
            : saved.getInt("verse", 1);
    try {
      Recitation.globalVerse(chapter, verse);
    } catch (IllegalArgumentException invalid) {
      chapter = 1;
      verse = 1;
    }
    rangeChapter = saved == null ? chapter : saved.getInt("range_chapter", chapter);
    rangeFirst =
        saved == null
            ? getIntent().getIntExtra(QuranActivity.EXTRA_RANGE_FIRST, 1)
            : saved.getInt("range_first", 1);
    rangeLast =
        saved == null
            ? getIntent().getIntExtra(QuranActivity.EXTRA_RANGE_LAST, Recitation.count(chapter))
            : saved.getInt("range_last", Recitation.count(rangeChapter));
    if (rangeFirst < 1 || rangeFirst > rangeLast || rangeLast > Recitation.count(rangeChapter)) {
      rangeFirst = 1;
      rangeLast = Recitation.count(rangeChapter);
    }
    page =
        Math.max(
            1,
            Math.min(
                604,
                saved == null ? prefs.getInt("last_mushaf_page", 1) : saved.getInt("page", 1)));
    repository = new MushafRepository(this);
    pageView = findViewById(R.id.mushafPage);
    findViewById(R.id.toolbarBack).setOnClickListener(v -> finish());
    findViewById(R.id.mushafTranslation).setOnClickListener(v -> finish());
    findViewById(R.id.mushafNext).setOnClickListener(v -> load(Math.min(604, page + 1)));
    findViewById(R.id.mushafPrevious).setOnClickListener(v -> load(Math.max(1, page - 1)));
    findViewById(R.id.mushafJump).setOnClickListener(v -> jump());
    findViewById(R.id.mushafRetry)
        .setOnClickListener(
            v -> {
              if (retryJuz > 0) locateJuz(retryJuz);
              else if (retryPage > 0) load(retryPage);
              else locate(chapter, verse);
            });
    findViewById(R.id.mushafZoom).setOnClickListener(v -> options());
    audio =
        new ReaderAudioUi(
            this,
            new ReaderAudioUi.Host() {
              public int chapter() {
                return chapter;
              }

              public int verse() {
                return verse;
              }

              public int first() {
                return chapter == rangeChapter ? rangeFirst : 1;
              }

              public int last() {
                return chapter == rangeChapter ? rangeLast : Recitation.count(chapter);
              }

              public boolean ready() {
                return ready;
              }

              public boolean nextPage() {
                if (!ready || page == 604) return false;
                load(page + 1);
                return true;
              }

              public void highlight(int c, int v) {
                pageView.active(c, v);
              }

              public void follow(int c, int v) {
                if (!ready) return;
                if (document.page.contains(c, v)) {
                  chapter = c;
                  verse = v;
                  remember();
                } else locate(c, v);
              }
            });
    audio.reader(true);
    palette();
    if (saved != null) load(page);
    else locate(chapter, verse);
  }

  private void busy() {
    ready = false;
    pageView.setVisibility(View.INVISIBLE);
    findViewById(R.id.mushafRetry).setVisibility(View.GONE);
    ((TextView) findViewById(R.id.mushafStatus)).setText(R.string.mushaf_loading);
    findViewById(R.id.mushafNext).setEnabled(false);
    findViewById(R.id.mushafPrevious).setEnabled(false);
  }

  private void failed() {
    if (isFinishing() || isDestroyed()) return;
    ready = false;
    ((TextView) findViewById(R.id.mushafStatus)).setText(R.string.mushaf_error);
    findViewById(R.id.mushafRetry).setVisibility(View.VISIBLE);
  }

  private void locate(int c, int v) {
    retryJuz = 0;
    retryPage = 0;
    busy();
    repository.locate(
        c,
        v,
        page,
        new MushafRepository.Located() {
          public void ready(int found) {
            chapter = c;
            verse = v;
            load(found);
          }

          public void failed() {
            MushafActivity.this.failed();
          }
        });
  }

  private void load(int requested) {
    retryJuz = 0;
    retryPage = requested;
    busy();
    repository.load(
        requested,
        new MushafRepository.Result() {
          public void ready(MushafDocument data, Typeface font) {
            if (isFinishing() || isDestroyed()) return;
            document = data;
            page = requested;
            if (!data.page.contains(chapter, verse)) {
              MushafPage.Word first = data.page.lines.get(0).words.get(0);
              chapter = first.chapter;
              verse = first.verse;
            }
            ready = true;
            try {
              pageView.bind(data, font, MushafActivity.this);
            } catch (RuntimeException invalidFont) {
              failed();
              return;
            }
            pageView.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.mushafMetadata))
                .setText(
                    QuranChapters.all(MushafActivity.this).get(chapter - 1).name
                        + " · Juz "
                        + data.juz);
            ((TextView) findViewById(R.id.mushafStatus)).setText(R.string.mushaf_attribution);
            ((Button) findViewById(R.id.mushafJump))
                .setText(getString(R.string.mushaf_page_label, page));
            findViewById(R.id.mushafNext).setEnabled(page < 604);
            findViewById(R.id.mushafPrevious).setEnabled(page > 1);
            ((ScrollView) findViewById(R.id.quranScroll)).scrollTo(0, 0);
            applyZoom();
            audio.changedChapter();
            audio.pageReady();
            remember();
          }

          public void failed() {
            MushafActivity.this.failed();
          }
        });
  }

  private void remember() {
    prefs
        .edit()
        .putInt("last_mushaf_page", page)
        .putInt("last_mushaf_chapter", chapter)
        .putInt("last_mushaf_verse", verse)
        .apply();
    prefs
        .edit()
        .putInt("last_chapter", chapter)
        .putInt("last_ayah", verse)
        .putString("last_surah", QuranChapters.all(this).get(chapter - 1).name)
        .apply();
    setResult(
        RESULT_OK,
        new Intent()
            .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, chapter)
            .putExtra(QuranActivity.EXTRA_TARGET_VERSE, verse));
  }

  private void applyZoom() {
    View container = findViewById(R.id.mushafHorizontal);
    container.post(
        () -> {
          if (isDestroyed() || container.getWidth() == 0) return;
          ViewGroup.LayoutParams params = pageView.getLayoutParams();
          params.width = container.getWidth() * zoom / 100;
          pageView.setLayoutParams(params);
          pageView.post(() -> ((HorizontalScrollView) container).fullScroll(View.FOCUS_RIGHT));
        });
  }

  @Override
  public void toggleChrome() {
    chrome = !chrome;
    for (int id : new int[] {R.id.mushafHeader, R.id.mushafMetadata, R.id.mushafNavigation})
      findViewById(id).setVisibility(chrome ? View.VISIBLE : View.GONE);
  }

  @Override
  public void touched() {
    if (audio != null) audio.userTouch();
  }

  @Override
  public void verse(int c, int v) {
    chapter = c;
    verse = v;
    remember();
    audio.changedChapter();
    if (dialog != null) dialog.dismiss();
    dialog =
        new AlertDialog.Builder(this)
            .setTitle(QuranChapters.all(this).get(c - 1).name + " · " + v)
            .setItems(
                new String[] {
                  getString(R.string.audio_play),
                  getString(R.string.mushaf_translation),
                  getString(R.string.mushaf_bookmark)
                },
                (d, action) -> {
                  if (action == 0) audio.playVerse(v);
                  else if (action == 1) finish();
                  else {
                    prefs
                        .edit()
                        .putInt("last_chapter", c)
                        .putInt("last_ayah", v)
                        .putString("last_surah", QuranChapters.all(this).get(c - 1).name)
                        .apply();
                    toast(getString(R.string.mushaf_bookmarked));
                  }
                })
            .show();
  }

  private void jump() {
    if (dialog != null) dialog.dismiss();
    dialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.mushaf_title)
            .setItems(
                new String[] {
                  getString(R.string.mushaf_jump_page),
                  getString(R.string.mushaf_jump_surah),
                  getString(R.string.mushaf_jump_juz)
                },
                (d, action) -> {
                  if (action == 0) jumpPage();
                  else if (action == 1) {
                    String[] names = new String[114];
                    for (int i = 0; i < 114; i++)
                      names[i] = (i + 1) + ". " + QuranChapters.all(this).get(i).name;
                    dialog =
                        new AlertDialog.Builder(this)
                            .setTitle(R.string.mushaf_jump_surah)
                            .setItems(names, (choice, index) -> locate(index + 1, 1))
                            .show();
                  } else {
                    String[] names = new String[30];
                    for (int i = 0; i < 30; i++)
                      names[i] = getString(R.string.mushaf_juz_label, i + 1);
                    dialog =
                        new AlertDialog.Builder(this)
                            .setTitle(R.string.mushaf_jump_juz)
                            .setItems(names, (choice, index) -> locateJuz(index + 1))
                            .show();
                  }
                })
            .show();
  }

  private void locateJuz(int target) {
    retryJuz = target;
    retryPage = 0;
    busy();
    repository.locateJuz(
        target,
        new MushafRepository.Located() {
          public void ready(int found) {
            load(found);
          }

          public void failed() {
            MushafActivity.this.failed();
          }
        });
  }

  private void jumpPage() {
    EditText input = new EditText(this);
    input.setInputType(InputType.TYPE_CLASS_NUMBER);
    input.setHint(R.string.mushaf_page_range);
    input.setPadding(dp(24), dp(12), dp(24), dp(12));
    dialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.mushaf_jump_page)
            .setView(input)
            .setNegativeButton(R.string.quran_close, null)
            .setPositiveButton(R.string.quran_jump, null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      int target = 0;
                      try {
                        target = Integer.parseInt(input.getText().toString().trim());
                      } catch (NumberFormatException ignored) {
                      }
                      if (target < 1 || target > 604)
                        input.setError(getString(R.string.mushaf_page_range));
                      else {
                        dialog.dismiss();
                        load(target);
                      }
                    }));
    dialog.show();
  }

  private void options() {
    if (dialog != null) dialog.dismiss();
    dialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.mushaf_options)
            .setItems(
                new String[] {
                  "100%",
                  "125%",
                  "150%",
                  "200%",
                  getString(R.string.mushaf_night),
                  getString(R.string.mushaf_choose_verse)
                },
                (d, choice) -> {
                  if (choice < 4) {
                    zoom = new int[] {100, 125, 150, 200}[choice];
                    prefs.edit().putInt("mushaf_zoom", zoom).apply();
                    applyZoom();
                  } else if (choice == 4) {
                    night = !night;
                    prefs.edit().putBoolean("mushaf_night", night).apply();
                    palette();
                  } else chooseVerse();
                })
            .show();
  }

  private void chooseVerse() {
    if (!ready) return;
    LinkedHashMap<String, MushafPage.Word> words = new LinkedHashMap<>();
    for (MushafPage.Line line : document.page.lines)
      for (MushafPage.Word word : line.words) words.putIfAbsent(word.verseKey(), word);
    List<MushafPage.Word> choices = new ArrayList<>(words.values());
    String[] labels = new String[choices.size()];
    for (int i = 0; i < labels.length; i++)
      labels[i] =
          QuranChapters.all(this).get(choices.get(i).chapter - 1).name
              + " · "
              + choices.get(i).verse;
    dialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.mushaf_choose_verse)
            .setItems(
                labels, (d, index) -> verse(choices.get(index).chapter, choices.get(index).verse))
            .show();
  }

  private void palette() {
    pageView.theme(night);
    int background = night ? 0xFF152A26 : getColor(R.color.mushaf_paper);
    findViewById(R.id.mushafRoot).setBackgroundColor(background);
    getWindow().setStatusBarColor(background);
    getWindow().setNavigationBarColor(background);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            night
                ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    ((TextView) findViewById(R.id.mushafMetadata))
        .setTextColor(night ? 0xFFD1DDCA : getColor(R.color.muted));
    ((TextView) findViewById(R.id.mushafStatus))
        .setTextColor(night ? 0xFFD1DDCA : getColor(R.color.muted));
    int foreground = night ? 0xFFEEF3E5 : getColor(R.color.pine);
    for (int id :
        new int[] {
          R.id.mushafTitle,
          R.id.mushafZoom,
          R.id.mushafTranslation,
          R.id.mushafPrevious,
          R.id.mushafNext,
          R.id.mushafJump,
          R.id.mushafRetry
        }) ((TextView) findViewById(id)).setTextColor(foreground);
    ((ImageButton) findViewById(R.id.toolbarBack))
        .setImageTintList(android.content.res.ColorStateList.valueOf(foreground));
    audio.theme(night);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (audio != null) audio.start();
  }

  @Override
  protected void onStop() {
    if (audio != null) audio.stop();
    super.onStop();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putInt("page", page);
    state.putInt("chapter", chapter);
    state.putInt("verse", verse);
    state.putBoolean("night", night);
    state.putInt("range_chapter", rangeChapter);
    state.putInt("range_first", rangeFirst);
    state.putInt("range_last", rangeLast);
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    if (dialog != null) dialog.dismiss();
    if (repository != null) repository.close();
    super.onDestroy();
  }
}
