package com.zikra.app.audio;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.media.session.*;
import android.os.*;
import com.zikra.app.QuranActivity;
import com.zikra.app.R;
import com.zikra.app.reading.QuranChapters;
import com.zikra.app.reading.mushaf.WordTimeline;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** One native player and media session survive Activity changes. No automatic playback on boot. */
public final class QuranAudioService extends Service {
  public static final String PLAY = "play",
      RESUME = "resume",
      PAUSE = "pause",
      STOP = "stop",
      NEXT = "next",
      PREVIOUS = "previous",
      SEEK = "seek";
  private static final String CHANNEL = "quran_recitation";
  private static final int NOTIFICATION = 7301;

  public interface Listener {
    void changed(State state);
  }

  public static final class State {
    public final int chapter, verse, first, last, repeat, repeats, position, duration, word;
    public final float wordFraction;
    public final boolean playing, loading, loop, requested, wordSync, wordTimingAvailable;
    public final String reciter, status;
    public final List<String> words;

    State(QuranAudioService s) {
      RecitationQueue q = s.queue;
      chapter = q == null ? 0 : q.chapter;
      verse = q == null ? 0 : q.verse;
      first = q == null ? 0 : q.first;
      last = q == null ? 0 : q.last;
      repeat = q == null ? 1 : q.repetition;
      repeats = q == null ? 1 : q.repeats;
      loop = q != null && q.loop;
      requested = s.wantPlay;
      reciter = s.reciter;
      status = s.status;
      loading = s.fetching || (s.player != null && (!s.prepared || s.buffering || s.seeking));
      playing = s.isPlaying();
      int at = 0, length = 0;
      try {
        if (s.prepared && s.player != null) {
          at = s.player.getCurrentPosition();
          length = s.player.getDuration();
        }
      } catch (IllegalStateException pendingErrorCallback) {
        // Native decoder errors may precede the main-thread OnError callback.
      }
      position = at;
      duration = length;
      wordSync = s.wordSync;
      wordTimingAvailable = s.timeline != null;
      words = s.timedWords;
      WordTimeline.Highlight highlight =
          s.timeline == null
              ? null
              : s.timeline.at(chapter, verse, s.recordingSha256, at, loading, s.prepared);
      word = highlight == null ? 0 : highlight.word;
      wordFraction = highlight == null ? 0 : highlight.fraction;
    }
  }

  public final class LocalBinder extends Binder {
    public QuranAudioService service() {
      return QuranAudioService.this;
    }
  }

  private final Handler main = new Handler(Looper.getMainLooper());
  private final List<Listener> listeners = new ArrayList<>();
  private final IBinder binder = new LocalBinder();
  private MediaPlayer player;
  private MediaSession session;
  private AudioManager audio;
  private AudioFocusRequest focus;
  private RecitationQueue queue;
  private String reciter = Recitation.IDS[0], status = "Pilih ayat untuk mulai mendengarkan";
  private boolean prepared, wantPlay, resumeOnFocus, finished;
  private boolean wordSync, fetching, buffering, seeking;
  private volatile boolean destroyed;
  private volatile int audioGeneration;
  private WordTimeline timeline;
  private List<String> timedWords = java.util.Collections.emptyList();
  private String recordingSha256;
  private TimedAudioRepository timedAudio;
  private final ExecutorService audioWorker = Executors.newSingleThreadExecutor();
  private final BroadcastReceiver noisy =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          pause(true);
        }
      };
  private final Runnable prepareTimeout =
      () -> error("Audio belum termuat. Periksa koneksi atau unduh terlebih dahulu.");
  private final Runnable tick =
      new Runnable() {
        @Override
        public void run() {
          if (prepared && wantPlay && player != null) {
            publish(false);
            main.postDelayed(this, 500);
          }
        }
      };
  private final AudioManager.OnAudioFocusChangeListener focusChanges =
      change -> {
        if (change == AudioManager.AUDIOFOCUS_GAIN) {
          if (resumeOnFocus) {
            resumeOnFocus = false;
            resume();
          }
        } else if (change == AudioManager.AUDIOFOCUS_LOSS) {
          pause(true);
        } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
          boolean wasPlaying = wantPlay;
          pause(false);
          resumeOnFocus = wasPlaying;
        }
      };

  public static void send(Context c, String action) {
    send(c, new Intent().setAction(action));
  }

  public static void send(Context c, Intent command) {
    command.setClass(c, QuranAudioService.class);
    c.startForegroundService(command);
  }

  public static Intent play(
      int chapter, int first, int last, int verse, String qari, int repeats, boolean loop) {
    return new Intent()
        .setAction(PLAY)
        .putExtra("chapter", chapter)
        .putExtra("first", first)
        .putExtra("last", last)
        .putExtra("verse", verse)
        .putExtra("reciter", qari)
        .putExtra("repeats", repeats)
        .putExtra("loop", loop);
  }

  public State state() {
    return new State(this);
  }

  public void addListener(Listener l) {
    listeners.add(l);
    l.changed(state());
  }

  public void removeListener(Listener l) {
    listeners.remove(l);
  }

  @Override
  public IBinder onBind(Intent i) {
    return binder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    timedAudio = new TimedAudioRepository(this);
    audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    focus =
        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes())
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(focusChanges, main)
            .build();
    getSystemService(NotificationManager.class)
        .createNotificationChannel(
            new NotificationChannel(CHANNEL, "Murottal Quran", NotificationManager.IMPORTANCE_LOW));
    session = new MediaSession(this, "ZikraMurottal");
    session.setCallback(
        new MediaSession.Callback() {
          @Override
          public void onPlay() {
            resume();
          }

          @Override
          public void onPause() {
            pause(true);
          }

          @Override
          public void onStop() {
            stopPlayback();
          }

          @Override
          public void onSkipToNext() {
            skip(1);
          }

          @Override
          public void onSkipToPrevious() {
            skip(-1);
          }

          @Override
          public void onSeekTo(long ms) {
            seek(ms);
          }
        },
        main);
    IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    if (Build.VERSION.SDK_INT >= 33) registerReceiver(noisy, filter, RECEIVER_NOT_EXPORTED);
    else registerReceiver(noisy, filter);
  }

  private AudioAttributes attributes() {
    return new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    foreground();
    if (intent == null) {
      stopPlayback();
      return START_NOT_STICKY;
    }
    String action = intent.getAction();
    if (PLAY.equals(action)) {
      try {
        RecitationQueue next =
            new RecitationQueue(
                intent.getIntExtra("chapter", 0),
                intent.getIntExtra("first", 1),
                intent.getIntExtra("last", 0),
                intent.getIntExtra("verse", 1),
                intent.getIntExtra("repeats", 1),
                intent.getBooleanExtra("loop", false));
        String qari = intent.getStringExtra("reciter");
        Recitation.reciterIndex(qari);
        queue = next;
        reciter = qari;
        wordSync = intent.getBooleanExtra("word_sync", false);
        finished = false;
        prepare();
      } catch (IllegalArgumentException invalid) {
        error("Pilihan audio tidak valid.");
      }
    } else if (RESUME.equals(action)) resume();
    else if (PAUSE.equals(action)) pause(true);
    else if (NEXT.equals(action)) skip(1);
    else if (PREVIOUS.equals(action)) skip(-1);
    else if (SEEK.equals(action)) seek(intent.getIntExtra("position", 0));
    else stopPlayback();
    return START_NOT_STICKY;
  }

  private void foreground() {
    if (Build.VERSION.SDK_INT >= 29)
      startForeground(
          NOTIFICATION, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    else startForeground(NOTIFICATION, notification());
  }

  private void prepare() {
    releasePlayer();
    wantPlay = true;
    resumeOnFocus = false;
    finished = false;
    status = "Memuat audio ayat " + queue.verse + "…";
    foreground();
    session.setActive(true);
    if (wordSync) {
      fetching = true;
      main.postDelayed(prepareTimeout, 45000);
      int token = audioGeneration, chapter = queue.chapter, verse = queue.verse;
      String qari = reciter;
      publish(true);
      audioWorker.execute(
          () -> {
            if (destroyed || token != audioGeneration) return;
            TimedAudioRepository.Clip clip = null;
            try {
              clip = timedAudio.load(qari, chapter, verse);
            } catch (Exception unavailable) {
              // Existing downloads/stream remain playable, with ayah highlighting only.
            }
            TimedAudioRepository.Clip found = clip;
            main.post(
                () -> {
                  if (destroyed || token != audioGeneration) return;
                  fetching = false;
                  preparePlayer(found);
                });
          });
    } else preparePlayer(null);
  }

  private void preparePlayer(TimedAudioRepository.Clip clip) {
    MediaPlayer next = new MediaPlayer();
    player = next;
    next.setAudioAttributes(attributes());
    next.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
    next.setOnPreparedListener(
        p -> {
          if (player != p) return;
          main.removeCallbacks(prepareTimeout);
          prepared = true;
          if (clip != null) {
            recordingSha256 = clip.sha256;
            timeline = clip.data.timeline(clip.sha256, p.getDuration());
            if (timeline != null) timedWords = clip.data.words;
          }
          if (wantPlay) resume();
          else {
            status = "Dijeda";
            publish(true);
          }
        });
    next.setOnInfoListener(
        (p, what, extra) -> {
          if (player == p
              && (what == MediaPlayer.MEDIA_INFO_BUFFERING_START
                  || what == MediaPlayer.MEDIA_INFO_BUFFERING_END)) {
            buffering = what == MediaPlayer.MEDIA_INFO_BUFFERING_START;
            publish(false);
          }
          return false;
        });
    next.setOnSeekCompleteListener(
        p -> {
          if (player != p) return;
          seeking = false;
          publish(false);
        });
    next.setOnCompletionListener(
        p -> {
          if (player != p) return;
          if (queue.complete()) prepare();
          else {
            releasePlayer();
            wantPlay = false;
            finished = true;
            status = "Rentang selesai · progres hafalan ditandai sendiri di Diary Hifz";
            abandonFocus();
            publish(true);
            stopForeground(STOP_FOREGROUND_DETACH);
          }
        });
    next.setOnErrorListener(
        (p, what, extra) -> {
          if (player == p)
            error("Audio tidak dapat diputar. Coba lagi atau periksa unduhan dan koneksi.");
          return true;
        });
    try {
      File file = AudioFiles.file(this, reciter, queue.chapter, queue.verse);
      next.setDataSource(
          clip != null
              ? clip.file.getAbsolutePath()
              : AudioFiles.available(this, reciter, queue.chapter, queue.verse)
                  ? file.getAbsolutePath()
                  : Recitation.url(reciter, queue.chapter, queue.verse));
      next.prepareAsync();
      main.postDelayed(prepareTimeout, 45000);
      publish(true);
    } catch (Exception unavailable) {
      error("Audio belum tersedia. Periksa koneksi atau unduh surah ini.");
    }
  }

  private void resume() {
    if (queue == null) {
      stopPlayback();
      return;
    }
    foreground();
    if (finished) {
      queue.verse = queue.first;
      queue.repetition = 1;
      prepare();
      return;
    }
    if (fetching) {
      wantPlay = true;
      publish(true);
      return;
    }
    if (player == null) {
      prepare();
      return;
    }
    wantPlay = true;
    if (!prepared) {
      publish(true);
      return;
    }
    if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      wantPlay = false;
      status = "Audio sedang dipakai aplikasi lain. Ketuk putar untuk mencoba lagi.";
      publish(true);
      stopForeground(STOP_FOREGROUND_DETACH);
      return;
    }
    try {
      player.start();
    } catch (IllegalStateException invalidPlayer) {
      error("Audio terhenti. Ketuk Putar untuk memuat ulang.");
      return;
    }
    status = "Memutar ayat " + queue.verse;
    session.setActive(true);
    main.removeCallbacks(tick);
    main.post(tick);
    publish(true);
  }

  private void pause(boolean abandon) {
    if (queue == null) return;
    wantPlay = false;
    if (isPlaying()) {
      try {
        player.pause();
      } catch (IllegalStateException pendingErrorCallback) {
        error("Audio terhenti. Ketuk Putar untuk memuat ulang.");
        return;
      }
    }
    if (abandon) {
      resumeOnFocus = false;
      abandonFocus();
    }
    main.removeCallbacks(tick);
    status = "Dijeda";
    publish(true);
    // Keep transient interruptions in foreground so Android 15 can resume in background.
    if (abandon) stopForeground(STOP_FOREGROUND_DETACH);
  }

  private void skip(int direction) {
    if (queue != null && queue.skip(direction)) prepare();
    else {
      publish(true);
      if (!wantPlay) stopForeground(STOP_FOREGROUND_DETACH);
    }
  }

  private void seek(long ms) {
    try {
      if (prepared && player != null) {
        seeking = true;
        player.seekTo((int) Math.max(0, Math.min(player.getDuration(), ms)));
      }
    } catch (IllegalStateException pendingErrorCallback) {
      error("Posisi audio belum dapat diubah. Ketuk Putar untuk memuat ulang.");
      return;
    }
    publish(true);
    if (!wantPlay) stopForeground(STOP_FOREGROUND_DETACH);
  }

  private void error(String message) {
    releasePlayer();
    wantPlay = false;
    resumeOnFocus = false;
    status = message;
    abandonFocus();
    publish(true);
    stopForeground(STOP_FOREGROUND_DETACH);
  }

  private void abandonFocus() {
    if (audio != null) audio.abandonAudioFocusRequest(focus);
  }

  private void releasePlayer() {
    audioGeneration++;
    fetching = buffering = seeking = false;
    timeline = null;
    timedWords = java.util.Collections.emptyList();
    recordingSha256 = null;
    if (timedAudio != null) timedAudio.cancel();
    main.removeCallbacks(tick);
    main.removeCallbacks(prepareTimeout);
    prepared = false;
    if (player != null) {
      player.release();
      player = null;
    }
  }

  private void stopPlayback() {
    releasePlayer();
    wantPlay = false;
    resumeOnFocus = false;
    queue = null;
    status = "Pemutaran dihentikan";
    abandonFocus();
    session.setActive(false);
    publish(false);
    stopForeground(STOP_FOREGROUND_REMOVE);
    stopSelf();
  }

  private String title() {
    return queue == null
        ? "Murottal Zikra"
        : QuranChapters.all(this).get(queue.chapter - 1).name + " · Ayat " + queue.verse;
  }

  private void publish(boolean notify) {
    State state = state();
    session.setMetadata(
        new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title())
            .putString(
                MediaMetadata.METADATA_KEY_ARTIST,
                Recitation.NAMES[Recitation.reciterIndex(reciter)])
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.duration)
            .build());
    long actions =
        PlaybackState.ACTION_PLAY
            | PlaybackState.ACTION_PAUSE
            | PlaybackState.ACTION_PLAY_PAUSE
            | PlaybackState.ACTION_STOP
            | PlaybackState.ACTION_SEEK_TO;
    if (queue != null && queue.verse < queue.last) actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
    if (queue != null && queue.verse > queue.first)
      actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
    session.setPlaybackState(
        new PlaybackState.Builder()
            .setActions(actions)
            .setState(
                state.loading
                    ? PlaybackState.STATE_BUFFERING
                    : state.playing
                        ? PlaybackState.STATE_PLAYING
                        : queue == null ? PlaybackState.STATE_STOPPED : PlaybackState.STATE_PAUSED,
                state.position,
                state.playing ? 1 : 0)
            .build());
    if (notify) getSystemService(NotificationManager.class).notify(NOTIFICATION, notification());
    for (Listener l : new ArrayList<>(listeners)) l.changed(state);
  }

  private PendingIntent control(String action, int code) {
    return PendingIntent.getForegroundService(
        this,
        code,
        new Intent(this, QuranAudioService.class).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  private Notification notification() {
    boolean playing = isPlaying();
    Intent open = new Intent(this, QuranActivity.class);
    if (queue != null)
      open.putExtra(QuranActivity.EXTRA_TARGET_CHAPTER, queue.chapter)
          .putExtra(QuranActivity.EXTRA_TARGET_VERSE, queue.verse);
    return new Notification.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_book)
        .setContentTitle(title())
        .setContentText(status)
        .setOnlyAlertOnce(true)
        .setColor(0xFF15332F)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 7301, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
        .addAction(R.drawable.audio_previous, "Ayat sebelumnya", control(PREVIOUS, 1))
        .addAction(
            playing ? R.drawable.audio_pause : R.drawable.audio_play,
            playing ? "Jeda" : "Putar",
            control(playing ? PAUSE : RESUME, 2))
        .addAction(R.drawable.audio_next, "Ayat berikutnya", control(NEXT, 3))
        .addAction(R.drawable.audio_stop, "Hentikan", control(STOP, 4))
        .setDeleteIntent(control(STOP, 4))
        .setStyle(
            new Notification.MediaStyle()
                .setMediaSession(session.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2))
        .build();
  }

  private boolean isPlaying() {
    try {
      return prepared && player != null && player.isPlaying();
    } catch (IllegalStateException pendingErrorCallback) {
      return false;
    }
  }

  @Override
  public void onDestroy() {
    destroyed = true;
    releasePlayer();
    audioWorker.execute(timedAudio::close);
    audioWorker.shutdown();
    abandonFocus();
    unregisterReceiver(noisy);
    listeners.clear();
    getSystemService(NotificationManager.class).cancel(NOTIFICATION);
    session.release();
    super.onDestroy();
  }
}
