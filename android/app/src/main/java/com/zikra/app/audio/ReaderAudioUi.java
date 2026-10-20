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
  private boolean loop;
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

  private TextView label(LinearLayout parent, String title) {
    TextView v = new TextView(activity);
    v.setText(title);
    v.setTextSize(14);
    v.setPadding(0, dp(14), 0, dp(6));
    parent.addView(v);
    return v;
  }

  private Spinner spinner(LinearLayout parent, int id, String[] values, int selected) {
    Spinner view = new Spinner(activity);
    view.setId(id);
    view.setMinimumHeight(dp(48));
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, values);
    view.setAdapter(adapter);
    view.setSelection(selected);
    parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
    return view;
  }

  private Switch toggle(LinearLayout parent, int id, String title, boolean checked) {
    Switch v = new Switch(activity);
    v.setId(id);
    v.setText(title);
    v.setTextSize(14);
    v.setMinHeight(dp(52));
    v.setChecked(checked);
    parent.addView(v);
    return v;
  }

  private EditText number(LinearLayout row, int id, int value, String description) {
    EditText v = new EditText(activity);
    v.setId(id);
    v.setText(String.valueOf(value));
    v.setTextSize(16);
    v.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    v.setSingleLine();
    v.setMinHeight(dp(48));
    v.setContentDescription(description);
    v.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(3)});
    row.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
    return v;
  }

  public void options() {
    if (options != null) options.dismiss();
    ScrollView scroll = new ScrollView(activity);
    LinearLayout body = new LinearLayout(activity);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(24), 0, dp(24), dp(16));
    scroll.addView(body);
    label(body, "Qari");
    Spinner reciters =
        spinner(body, R.id.quranAudioReciter, Recitation.NAMES, Recitation.reciterIndex(qari));
    label(body, "Rentang ayat · " + QuranChapters.all(activity).get(chapter - 1).name);
    LinearLayout range = new LinearLayout(activity);
    body.addView(range);
    EditText start = number(range, R.id.quranAudioFirst, first, "Ayat awal");
    TextView dash = new TextView(activity);
    dash.setText(" — ");
    range.addView(dash);
    EditText end = number(range, R.id.quranAudioLast, last, "Ayat akhir");
    label(body, "Pengulangan setiap ayat");
    String[] counts = new String[10];
    for (int i = 0; i < 10; i++) counts[i] = (i + 1) + " kali";
    Spinner repeat = spinner(body, R.id.quranAudioRepeats, counts, repeats - 1);
    Switch loopChoice = toggle(body, R.id.quranAudioLoop, "Ulang rentang sampai dihentikan", loop);
    Switch followChoice =
        toggle(
            body, R.id.quranAudioFollow, "Ikuti ayat yang sedang diputar", follow && !touchPaused);
    label(body, "Gulir baca otomatis · terpisah dari murottal");
    TextView speedLabel = label(body, "Kecepatan " + scrollSpeedName());
    SeekBar speedBar = new SeekBar(activity);
    speedBar.setId(R.id.quranAutoSpeed);
    speedBar.setMax(55);
    speedBar.setProgress(speed - 5);
    speedBar.setMinimumHeight(dp(48));
    speedBar.setContentDescription("Kecepatan gulir baca");
    body.addView(speedBar, new LinearLayout.LayoutParams(-1, dp(48)));
    speedBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar bar, int progress, boolean user) {
            speed = 5 + progress;
            speedLabel.setText("Kecepatan " + scrollSpeedName());
            prefs.edit().putInt("scroll_speed", speed).apply();
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    Button autoButton = new Button(activity);
    autoButton.setId(R.id.quranAutoScroll);
    autoButton.setAllCaps(false);
    autoButton.setMinHeight(dp(48));
    autoButton.setText(auto ? "Hentikan gulir baca" : "Mulai gulir baca");
    body.addView(autoButton);
    autoButton.setOnClickListener(
        v -> {
          if (auto) stopScroll();
          else {
            follow = false;
            touchPaused = false;
            auto = true;
            awaitingPage = false;
            prefs.edit().putBoolean("follow", false).apply();
            lastFrame = SystemClock.uptimeMillis();
            main.removeCallbacks(scrollFrame);
            main.post(scrollFrame);
          }
          options.dismiss();
          update();
        });
    TextView source =
        label(
            body,
            "Rekaman qari: Al Quran Cloud / Islamic Network. Menyentuh bacaan menjeda gulir."
                + " Progres hafalan tidak ditandai otomatis.");
    source.setTextSize(12);
    source.setMinHeight(dp(48));
    source.setOnClickListener(
        v -> {
          try {
            activity.startActivity(
                new Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://alquran.cloud/terms-and-conditions")));
          } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "Browser belum tersedia.", Toast.LENGTH_SHORT).show();
          }
        });
    options =
        new AlertDialog.Builder(activity)
            .setTitle("Audio dan gulir Quran")
            .setView(scroll)
            .setNegativeButton("Tutup", null)
            .setPositiveButton("Terapkan", null)
            .create();
    options.setOnShowListener(
        d ->
            options
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      int from, to;
                      try {
                        from = Integer.parseInt(start.getText().toString());
                        to = Integer.parseInt(end.getText().toString());
                      } catch (NumberFormatException invalid) {
                        start.setError("Isi nomor ayat");
                        return;
                      }
                      if (from < 1 || to < from || to > Recitation.count(chapter)) {
                        end.setError("Gunakan rentang 1–" + Recitation.count(chapter));
                        return;
                      }
                      first = from;
                      last = to;
                      qari = Recitation.IDS[reciters.getSelectedItemPosition()];
                      repeats = repeat.getSelectedItemPosition() + 1;
                      loop = loopChoice.isChecked();
                      follow = followChoice.isChecked();
                      touchPaused = false;
                      lastFollowed = 0;
                      prefs
                          .edit()
                          .putString("reciter", qari)
                          .putInt("repeats", repeats)
                          .putBoolean("loop", loop)
                          .putBoolean("follow", follow)
                          .apply();
                      if (follow) stopScroll();
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
                    }));
    options.show();
  }

  private String scrollSpeedName() {
    return speed < 15 ? "lambat" : speed < 35 ? "sedang" : "cepat";
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
    int foreground = night ? 0xFFEEF6F2 : 0xFF15332F, secondary = night ? 0xFFB4C8BE : 0xFF586C63;
    activity.findViewById(R.id.quranAudioPanel).setBackgroundColor(night ? 0xFF1D3530 : 0xFFEFF2EA);
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
  }
}
