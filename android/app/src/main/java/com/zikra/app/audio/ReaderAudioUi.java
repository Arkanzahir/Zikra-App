package com.zikra.app.audio;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.zikra.app.R;
import com.zikra.app.reading.QuranChapters;
import java.util.Locale;

/**
 * Activity UI observes the service; scrolling never drives media progress or marks Hifz complete.
 */
public final class ReaderAudioUi implements QuranAudioService.Listener {
  public interface Host {
    int chapter();

    int verse();

    int first();

    int last();

    void follow(int chapter, int verse);

    void highlight(int chapter, int verse);

    boolean nextPage();

    boolean ready();
  }

  private final Activity activity;
  private final Host host;
  private final SharedPreferences prefs;
  private final Handler main = new Handler(Looper.getMainLooper());
  private QuranAudioService service;
  private QuranAudioService.State state;
  private boolean bound, reading, seeking, follow, touchPaused, auto, awaitingPage, active;
  private Boolean showingPlaying;
  private int chapter, first, last, repeats, speed, lastFollowed;
  private String qari;
  private boolean loop, night;
  private long lastFrame;
  private float fractional;
  private AlertDialog options, downloads;
  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName n, IBinder binder) {
          service = ((QuranAudioService.LocalBinder) binder).service();
          service.addListener(ReaderAudioUi.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName n) {
          service = null;
          state = null;
          update();
        }
      };
  private final Runnable poll =
      new Runnable() {
        @Override
        public void run() {
          if (active) {
            update();
            main.postDelayed(this, 750);
          }
        }
      };
  private final Runnable scrollFrame =
      new Runnable() {
        @Override
        public void run() {
          if (!auto || !active || !reading) return;
          if (!host.ready()) {
            main.postDelayed(this, 100);
            return;
          }
          ScrollView scroll = activity.findViewById(R.id.quranScroll);
          long now = SystemClock.uptimeMillis();
          float density = activity.getResources().getDisplayMetrics().density;
          fractional += speed * density * Math.min(100, now - lastFrame) / 1000f;
          lastFrame = now;
          int pixels = (int) fractional;
          fractional -= pixels;
          int maximum =
              Math.max(
                  0,
                  scroll.getChildAt(0).getHeight()
                      - scroll.getHeight()
                      + scroll.getPaddingBottom());
          if (scroll.getScrollY() >= maximum - 2) {
            if (!awaitingPage) {
              awaitingPage = true;
              if (!host.nextPage()) {
                stopScroll();
                return;
              }
            }
          } else scroll.scrollBy(0, pixels);
          main.postDelayed(this, 32);
        }
      };

  // This touch listener only observes; ScrollView receives the event and handles accessible clicks.
  @android.annotation.SuppressLint("ClickableViewAccessibility")
  public ReaderAudioUi(Activity a, Host host) {
    activity = a;
    this.host = host;
    prefs = a.getSharedPreferences("quran_audio_options", 0);
    qari = prefs.getString("reciter", Recitation.IDS[0]);
    try {
      Recitation.reciterIndex(qari);
    } catch (IllegalArgumentException bad) {
      qari = Recitation.IDS[0];
    }
    repeats = Math.max(1, Math.min(10, prefs.getInt("repeats", 1)));
    loop = prefs.getBoolean("loop", false);
    follow = prefs.getBoolean("follow", true);
    speed = Math.max(5, Math.min(60, prefs.getInt("scroll_speed", 20)));
    a.findViewById(R.id.quranAudioTitle)
        .setOnClickListener(
            v -> {
              if (state != null && state.chapter > 0) host.follow(state.chapter, state.verse);
            });
    a.findViewById(R.id.quranAudioPlay).setOnClickListener(v -> playPause());
    a.findViewById(R.id.quranAudioPrevious)
        .setOnClickListener(v -> send(QuranAudioService.PREVIOUS));
    a.findViewById(R.id.quranAudioNext).setOnClickListener(v -> send(QuranAudioService.NEXT));
    a.findViewById(R.id.quranAudioOptions).setOnClickListener(v -> options());
    a.findViewById(R.id.quranAudioDownload).setOnClickListener(v -> downloads());
    ((SeekBar) a.findViewById(R.id.quranAudioSeek))
        .setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
              @Override
              public void onProgressChanged(SeekBar b, int p, boolean user) {}

              @Override
              public void onStartTrackingTouch(SeekBar b) {
                seeking = true;
              }

              @Override
              public void onStopTrackingTouch(SeekBar b) {
                seeking = false;
                if (state != null && state.chapter > 0)
                  send(
                      new Intent()
                          .setAction(QuranAudioService.SEEK)
                          .putExtra("position", b.getProgress()));
              }
            });
    a.findViewById(R.id.quranScroll)
        .setOnTouchListener(
            (v, event) -> {
              if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                stopScroll();
                touchPaused = true;
              }
              return false;
            });
  }

  public void start() {
    active = true;
    if (!bound)
      bound =
          activity.bindService(
              new Intent(activity, QuranAudioService.class), connection, Context.BIND_AUTO_CREATE);
    main.post(poll);
  }

  public void stop() {
    active = false;
    stopScroll();
    main.removeCallbacks(poll);
    if (service != null) service.removeListener(this);
    if (bound) {
      activity.unbindService(connection);
      bound = false;
    }
    service = null;
    if (options != null) options.dismiss();
    if (downloads != null) downloads.dismiss();
  }

  public void reader(boolean visible) {
    reading = visible;
    activity.findViewById(R.id.quranAudioPanel).setVisibility(visible ? View.VISIBLE : View.GONE);
    if (!visible) stopScroll();
    else changedChapter();
  }

  public void changedChapter() {
    if (chapter != host.chapter()) {
      stopScroll();
      chapter = host.chapter();
      first = host.first();
      last = host.last();
      lastFollowed = 0;
    }
    update();
  }

  public void pageReady() {
    awaitingPage = false;
    lastFrame = SystemClock.uptimeMillis();
    if (state != null && state.chapter == host.chapter()) {
      host.highlight(state.chapter, state.verse);
      if (follow && !touchPaused && state.verse == host.verse())
        host.follow(state.chapter, state.verse);
    }
  }

  @Override
  public void changed(QuranAudioService.State next) {
    state = next;
    if (reading && next.chapter == host.chapter()) {
      first = next.first;
      last = next.last;
      qari = next.reciter;
      repeats = next.repeats;
      loop = next.loop;
      host.highlight(next.chapter, next.verse);
      if (follow && !touchPaused && next.verse != lastFollowed) {
        lastFollowed = next.verse;
        host.follow(next.chapter, next.verse);
      }
    } else if (reading) host.highlight(0, 0);
    update();
  }

  private void send(String action) {
    send(new Intent().setAction(action));
  }

  private void send(Intent intent) {
    try {
      QuranAudioService.send(activity, intent);
    } catch (RuntimeException unavailable) {
      Toast.makeText(
              activity,
              "Pemutar belum bisa dibuka. Coba lagi dari halaman Quran.",
              Toast.LENGTH_LONG)
          .show();
    }
  }

  private void playPause() {
    stopScroll();
    touchPaused = false;
    if (state != null
        && state.chapter == host.chapter()
        && state.first == first
        && state.last == last
        && state.reciter.equals(qari)) {
      send(
          state.playing || (state.loading && state.requested)
              ? QuranAudioService.PAUSE
              : QuranAudioService.RESUME);
    } else {
      int verse = Math.max(first, Math.min(last, host.verse()));
      send(QuranAudioService.play(host.chapter(), first, last, verse, qari, repeats, loop));
    }
  }

  public void playVerse(int verse) {
    stopScroll();
    touchPaused = false;
    if (verse < first || verse > last) {
      first = 1;
      last = Recitation.count(host.chapter());
    }
    send(QuranAudioService.play(host.chapter(), first, last, verse, qari, repeats, loop));
  }

  private void update() {
    if (!reading || chapter < 1) return;
    boolean current = state != null && state.chapter == chapter;
    boolean playing = current && (state.playing || (state.loading && state.requested));
    String name = QuranChapters.all(activity).get(chapter - 1).name;
    String title = name + " · " + (current ? "Ayat " + state.verse : "Ayat " + first + "–" + last);
    text(R.id.quranAudioTitle, title);
    boolean otherChapter = state != null && state.chapter > 0 && state.chapter != chapter;
    TextView heading = activity.findViewById(R.id.quranAudioTitle);
    if (heading.isClickable() != otherChapter) heading.setClickable(otherChapter);
    if (heading.isFocusable() != otherChapter) heading.setFocusable(otherChapter);
    if (heading.getMinHeight() != (otherChapter ? dp(48) : 0))
      heading.setMinHeight(otherChapter ? dp(48) : 0);
    String status =
        current
            ? state.status + " · " + Recitation.NAMES[Recitation.reciterIndex(state.reciter)]
            : Recitation.NAMES[Recitation.reciterIndex(qari)]
                + " · ulang "
                + repeats
                + "× per ayat";
    if (current && state.repeats > 1) status += " · " + state.repeat + "/" + state.repeats;
    if (otherChapter)
      status =
          "Audio "
              + QuranChapters.all(activity).get(state.chapter - 1).name
              + " ayat "
              + state.verse
              + (state.playing ? " berjalan" : " dijeda")
              + ". Ketuk judul untuk kembali; Putar mengganti ke surah ini.";
    if (auto) status += " · Gulir baca · " + scrollSpeedName();
    if (touchPaused && follow) status += " · Ikuti ayat dijeda; aktifkan di pengaturan";
    SharedPreferences transfer = activity.getSharedPreferences(QuranDownloadService.PREFS, 0);
    if (QuranDownloadService.running) status += "\n" + transfer.getString("status", "Mengunduh…");
    else if (transfer.getInt("chapter", 0) == chapter
        && qari.equals(transfer.getString("reciter", ""))) {
      String result = transfer.getString("status", "");
      if (result.startsWith("Selesai")) status += " · Surah tersedia offline";
      else if (!result.isEmpty())
        status += " · Unduhan dijeda; buka kelola audio untuk melanjutkan";
    }
    text(R.id.quranAudioStatus, status);
    ImageButton play = activity.findViewById(R.id.quranAudioPlay);
    if (showingPlaying == null || showingPlaying != playing) {
      showingPlaying = playing;
      play.setImageResource(playing ? R.drawable.audio_pause : R.drawable.audio_play);
      play.setContentDescription(playing ? "Jeda murottal" : "Putar murottal");
    }
    activity.findViewById(R.id.quranAudioPrevious).setEnabled(current && state.verse > state.first);
    activity.findViewById(R.id.quranAudioNext).setEnabled(current && state.verse < state.last);
    SeekBar seek = activity.findViewById(R.id.quranAudioSeek);
    seek.setEnabled(current && state.duration > 0);
    if (!seeking) {
      int max = current ? state.duration : 0, progress = current ? state.position : 0;
      if (seek.getMax() != max) seek.setMax(max);
      if (seek.getProgress() != progress) seek.setProgress(progress);
    }
    text(
        R.id.quranAudioTime,
        time(current ? state.position : 0) + " / " + time(current ? state.duration : 0));
  }

  private String time(int ms) {
    return String.format(Locale.ROOT, "%d:%02d", ms / 60000, ms / 1000 % 60);
  }

  private void text(int id, String value) {
    TextView view = activity.findViewById(id);
    if (!value.contentEquals(view.getText())) view.setText(value);
  }

  private int dp(int value) {
    return Math.round(value * activity.getResources().getDisplayMetrics().density);
  }

  private void picker(Spinner view, String[] values, int selected) {
    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, values) {
          @Override
          public View getView(int position, View convert, ViewGroup parent) {
            TextView text = (TextView) super.getView(position, convert, parent);
            text.setTextSize(12);
            text.setTextColor(night ? 0xFFEEF6F2 : 0xFF15332F);
            text.setSingleLine(false);
            text.setMaxLines(2);
            text.setPadding(0, 0, 0, 0);
            return text;
          }
        };
    view.setAdapter(adapter);
    view.setSelection(selected);
  }

  public void options() {
    if (options != null) options.dismiss();
    View body = activity.getLayoutInflater().inflate(R.layout.quran_audio_options, null);
    Spinner reciters = body.findViewById(R.id.quranAudioReciter);
    picker(reciters, Recitation.NAMES, Recitation.reciterIndex(qari));
    Spinner repeat = body.findViewById(R.id.quranAudioRepeats);
    String[] counts = new String[10];
    for (int i = 0; i < counts.length; i++) counts[i] = (i + 1) + " kali";
    picker(repeat, counts, repeats - 1);
    EditText start = body.findViewById(R.id.quranAudioFirst);
    EditText end = body.findViewById(R.id.quranAudioLast);
    start.setText(String.valueOf(first));
    end.setText(String.valueOf(last));
    View rangeEditor = body.findViewById(R.id.quranRangeEditor);
    ((TextView) body.findViewById(R.id.quranRangeSummary)).setText(first + "–" + last);
    body.findViewById(R.id.quranRangeAction)
        .setOnClickListener(
            v -> {
              rangeEditor.setVisibility(
                  rangeEditor.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
              if (rangeEditor.getVisibility() == View.VISIBLE) start.requestFocus();
            });
    Switch loopChoice = body.findViewById(R.id.quranAudioLoop);
    loopChoice.setChecked(loop);
    Switch followChoice = body.findViewById(R.id.quranAudioFollow);
    followChoice.setChecked(follow && !touchPaused);
    Switch autoChoice = body.findViewById(R.id.quranAutoScroll);
    autoChoice.setChecked(auto);
    // Both modes drive the same viewport; choosing one turns the other off.
    autoChoice.setOnCheckedChangeListener(
        (button, checked) -> {
          if (checked) followChoice.setChecked(false);
        });
    followChoice.setOnCheckedChangeListener(
        (button, checked) -> {
          if (checked) autoChoice.setChecked(false);
        });
    TextView speedLabel = body.findViewById(R.id.quranAutoSpeedLabel);
    SeekBar speedBar = body.findViewById(R.id.quranAutoSpeed);
    speedBar.setProgress(speed - 5);
    speedLabel.setText("Kecepatan · " + scrollSpeedName(speed));
    speedBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar bar, int value, boolean user) {
            speedLabel.setText("Kecepatan · " + scrollSpeedName(value + 5));
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    body.findViewById(R.id.quranAudioSource)
        .setOnClickListener(
            v -> {
              try {
                activity.startActivity(
                    new Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://alquran.cloud/terms-and-conditions")));
              } catch (ActivityNotFoundException unavailable) {
                Toast.makeText(activity, "Browser belum tersedia.", Toast.LENGTH_SHORT).show();
              }
            });
    options =
        new AlertDialog.Builder(activity)
            .setView(body)
            .setNegativeButton("Tutup", null)
            .setPositiveButton("Terapkan", null)
            .create();
    View.OnClickListener apply =
        v -> {
          int from, to;
          try {
            from = Integer.parseInt(start.getText().toString());
            to = Integer.parseInt(end.getText().toString());
          } catch (NumberFormatException invalid) {
            rangeEditor.setVisibility(View.VISIBLE);
            start.setError("Isi nomor ayat");
            start.requestFocus();
            return;
          }
          if (from < 1 || to < from || to > Recitation.count(chapter)) {
            rangeEditor.setVisibility(View.VISIBLE);
            end.setError("Gunakan rentang 1–" + Recitation.count(chapter));
            end.requestFocus();
            return;
          }
          first = from;
          last = to;
          qari = Recitation.IDS[reciters.getSelectedItemPosition()];
          repeats = repeat.getSelectedItemPosition() + 1;
          loop = loopChoice.isChecked();
          follow = followChoice.isChecked();
          speed = speedBar.getProgress() + 5;
          touchPaused = false;
          lastFollowed = 0;
          prefs
              .edit()
              .putString("reciter", qari)
              .putInt("repeats", repeats)
              .putBoolean("loop", loop)
              .putBoolean("follow", follow)
              .putInt("scroll_speed", speed)
              .apply();
          stopScroll();
          if (autoChoice.isChecked()) {
            auto = true;
            lastFrame = SystemClock.uptimeMillis();
            main.post(scrollFrame);
          }
          if (state != null && state.chapter == chapter) {
            if (state.playing || state.requested)
              send(
                  QuranAudioService.play(
                      chapter,
                      first,
                      last,
                      Math.max(first, Math.min(last, state.verse)),
                      qari,
                      repeats,
                      loop));
            else send(QuranAudioService.STOP);
          }
          options.dismiss();
          update();
          if (v.getId() == R.id.quranAudioDownload) downloads();
        };
    body.findViewById(R.id.quranAudioDownload).setOnClickListener(apply);
    options.setOnShowListener(
        dialog -> {
          options.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(apply);
          styleSheet(options, body);
        });
    options.show();
  }

  private void sheetPalette(View view) {
    int foreground = night ? 0xFFEEF6F2 : 0xFF15332F;
    if (view instanceof TextView) ((TextView) view).setTextColor(foreground);
    if (view instanceof ImageView)
      ((ImageView) view).setImageTintList(ColorStateList.valueOf(foreground));
    if (view instanceof Spinner)
      ((Spinner) view)
          .setBackgroundTintList(ColorStateList.valueOf(night ? 0xFFACCCB7 : 0xFF586F62));
    if (view instanceof SeekBar) {
      SeekBar slider = (SeekBar) view;
      slider.setProgressTintList(ColorStateList.valueOf(night ? 0xFFACCCB7 : 0xFF15332F));
      slider.setThumbTintList(ColorStateList.valueOf(night ? 0xFFACCCB7 : 0xFF15332F));
      slider.setProgressBackgroundTintList(ColorStateList.valueOf(night ? 0xFF456055 : 0xFFD6E2DB));
    }
    if (!(view instanceof TextView)
        && !(view instanceof ViewGroup)
        && view.getLayoutParams() != null
        && view.getLayoutParams().height == dp(1))
      view.setBackgroundColor(night ? 0xFF345149 : 0xFFE9EDE8);
    if (view instanceof Switch) {
      ((Switch) view)
          .setThumbTintList(
              new ColorStateList(
                  new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                  new int[] {night ? 0xFFACCCB7 : 0xFF15332F, 0xFF8E9E96}));
    }
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
        sheetPalette(((ViewGroup) view).getChildAt(i));
  }

  private android.graphics.drawable.GradientDrawable sheetBackground() {
    android.graphics.drawable.GradientDrawable background =
        new android.graphics.drawable.GradientDrawable();
    background.setColor(night ? 0xFF1D3530 : 0xFFFFFFFF);
    float radius = dp(24);
    background.setCornerRadii(new float[] {radius, radius, radius, radius, 0, 0, 0, 0});
    return background;
  }

  private void styleSheet(AlertDialog dialog, View body) {
    sheetPalette(body);
    Window window = dialog.getWindow();
    if (window == null) return;
    window.setBackgroundDrawable(sheetBackground());
    window.getDecorView().setPadding(0, 0, 0, 0);
    window.setGravity(Gravity.BOTTOM);
    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    window.setDimAmount(.36f);
    window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
    window.setNavigationBarColor(night ? 0xFF1D3530 : 0xFFFFFFFF);
    window
        .getDecorView()
        .setSystemUiVisibility(
            night
                ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    android.os.PowerManager power =
        (android.os.PowerManager) activity.getSystemService(Context.POWER_SERVICE);
    boolean motion =
        android.animation.ValueAnimator.areAnimatorsEnabled()
            && (power == null || !power.isPowerSaveMode());
    window.setWindowAnimations(motion ? R.style.QuranSheetAnimation : 0);
    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    ScrollView scroll = body.findViewById(R.id.quranAudioSheetScroll);
    View contents = scroll.getChildAt(0);
    int width = activity.getResources().getDisplayMetrics().widthPixels - dp(48);
    contents.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    final int natural = contents.getMeasuredHeight();
    View decor = window.getDecorView();
    android.view.ViewTreeObserver.OnGlobalLayoutListener size =
        () -> {
          android.graphics.Rect visible = new android.graphics.Rect();
          decor.getWindowVisibleDisplayFrame(visible);
          int available =
              visible.height() > 0
                  ? visible.height()
                  : activity.getResources().getDisplayMetrics().heightPixels;
          int height = Math.min(natural, Math.max(dp(100), (int) (available * .88f) - dp(132)));
          if (scroll.getLayoutParams().height != height) {
            scroll.getLayoutParams().height = height;
            scroll.requestLayout();
          }
        };
    decor.getViewTreeObserver().addOnGlobalLayoutListener(size);
    dialog.setOnDismissListener(
        d -> {
          if (decor.getViewTreeObserver().isAlive())
            decor.getViewTreeObserver().removeOnGlobalLayoutListener(size);
        });
    size.onGlobalLayout();
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(night ? 0xFFACCCB7 : 0xFF15332F);
    Button apply = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    apply.setTextColor(night ? 0xFFACCCB7 : 0xFF15332F);
    apply.setMinHeight(dp(48));
  }

  private String scrollSpeedName(int value) {
    return value < 15 ? "Lambat" : value < 35 ? "Sedang" : "Cepat";
  }

  private String scrollSpeedName() {
    return scrollSpeedName(speed);
  }

  private void stopScroll() {
    auto = false;
    awaitingPage = false;
    fractional = 0;
    main.removeCallbacks(scrollFrame);
  }

  public void pause() {
    stopScroll();
  }

  private void downloads() {
    if (downloads != null) downloads.dismiss();
    int downloaded = AudioFiles.downloaded(activity, qari, chapter);
    String status =
        activity.getSharedPreferences(QuranDownloadService.PREFS, 0).getString("status", "");
    if (!QuranDownloadService.running
        && activity.getSharedPreferences(QuranDownloadService.PREFS, 0).getBoolean("busy", false))
      status = "Unduhan sebelumnya terhenti. Ketuk Unduh untuk melanjutkan.";
    downloads =
        new AlertDialog.Builder(activity)
            .setTitle("Audio offline · " + QuranChapters.all(activity).get(chapter - 1).name)
            .setMessage(
                Recitation.NAMES[Recitation.reciterIndex(qari)]
                    + "\n"
                    + downloaded
                    + "/"
                    + Recitation.count(chapter)
                    + " ayat tersimpan · "
                    + String.format(
                        Locale.forLanguageTag("id"),
                        "%.1f MB",
                        AudioFiles.bytes(activity, qari, chapter) / 1048576d)
                    + "\n\n"
                    + status
                    + "\n\n"
                    + "Unduhan mencakup teks dan rekaman surah ini. Menggunakan internet, termasuk"
                    + " data seluler. Unduhan yang terputus bisa dilanjutkan. Sumber: Al Quran"
                    + " Cloud / Islamic Network.")
            .setPositiveButton(
                QuranDownloadService.running ? "Lihat progres" : "Unduh / lanjutkan",
                (d, w) -> {
                  if (QuranDownloadService.running) return;
                  try {
                    activity.startForegroundService(
                        new Intent(activity, QuranDownloadService.class)
                            .setAction("download")
                            .putExtra("chapter", chapter)
                            .putExtra("reciter", qari));
                  } catch (RuntimeException e) {
                    Toast.makeText(activity, "Unduhan belum bisa dimulai.", Toast.LENGTH_LONG)
                        .show();
                  }
                })
            .setNeutralButton(
                QuranDownloadService.running ? "Jeda unduhan" : "Hapus audio",
                (d, w) -> {
                  if (QuranDownloadService.running)
                    activity.startForegroundService(
                        new Intent(activity, QuranDownloadService.class).setAction("cancel"));
                  else
                    new AlertDialog.Builder(activity)
                        .setTitle("Hapus audio qari ini?")
                        .setMessage(
                            "Rekaman surah ini dihapus dari perangkat. Teks bacaan, bookmark, dan"
                                + " target hafalan tetap tersimpan.")
                        .setNegativeButton("Batal", null)
                        .setPositiveButton(
                            "Hapus",
                            (dialog, which) -> {
                              if (state != null
                                  && state.chapter == chapter
                                  && state.reciter.equals(qari)) send(QuranAudioService.STOP);
                              try {
                                AudioFiles.delete(activity, qari, chapter);
                                Toast.makeText(activity, "Audio dihapus", Toast.LENGTH_SHORT)
                                    .show();
                              } catch (RuntimeException e) {
                                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                              }
                            })
                        .show();
                })
            .setNegativeButton("Tutup", null)
            .show();
  }

  public void theme(boolean night) {
    this.night = night;
    int foreground = night ? 0xFFEEF6F2 : 0xFF15332F, secondary = night ? 0xFFB4C8BE : 0xFF586C63;
    activity.findViewById(R.id.quranAudioPanel).setBackgroundColor(night ? 0xFF1D3530 : 0xFFFFFFFF);
    ((TextView) activity.findViewById(R.id.quranAudioTitle)).setTextColor(foreground);
    for (int id : new int[] {R.id.quranAudioStatus, R.id.quranAudioTime})
      ((TextView) activity.findViewById(id)).setTextColor(secondary);
    for (int id :
        new int[] {
          R.id.quranAudioPrevious,
          R.id.quranAudioNext,
          R.id.quranAudioOptions,
          R.id.quranAudioDownload
        })
      ((ImageButton) activity.findViewById(id))
          .setImageTintList(ColorStateList.valueOf(foreground));
    ((SeekBar) activity.findViewById(R.id.quranAudioSeek))
        .setProgressTintList(ColorStateList.valueOf(night ? 0xFFACCCB7 : 0xFF15332F));
    if (reading && state != null && state.chapter == chapter) host.highlight(chapter, state.verse);
    if (options != null && options.isShowing()) {
      sheetPalette(options.findViewById(R.id.quranAudioSheet));
      Window window = options.getWindow();
      if (window != null) {
        window.setBackgroundDrawable(sheetBackground());
        window.setNavigationBarColor(night ? 0xFF1D3530 : 0xFFFFFFFF);
        window
            .getDecorView()
            .setSystemUiVisibility(
                night
                    ? 0
                    : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
      }
      for (int button : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE})
        options.getButton(button).setTextColor(night ? 0xFFACCCB7 : 0xFF15332F);
    }
  }
}
