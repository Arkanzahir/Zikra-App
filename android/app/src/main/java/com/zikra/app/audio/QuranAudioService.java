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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    public final int chapter, verse, first, last, repeat, repeats, position, duration;
    public final boolean playing, loading, loop, requested;
    public final String reciter, status;

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
      loading = s.player != null && !s.prepared;
      playing = s.prepared && s.player != null && s.player.isPlaying();
      position = s.prepared ? s.player.getCurrentPosition() : 0;
      duration = s.prepared ? s.player.getDuration() : 0;
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
    MediaPlayer next = new MediaPlayer();
    player = next;
    next.setAudioAttributes(attributes());
    next.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
    next.setOnPreparedListener(
        p -> {
          if (player != p) return;
          main.removeCallbacks(prepareTimeout);
          prepared = true;
          if (wantPlay) resume();
          else {
            status = "Dijeda";
            publish(true);
          }
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
          AudioFiles.available(this, reciter, queue.chapter, queue.verse)
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
    player.start();
    status = "Memutar ayat " + queue.verse;
    session.setActive(true);
    main.removeCallbacks(tick);
    main.post(tick);
    publish(true);
  }

  private void pause(boolean abandon) {
    wantPlay = false;
    if (prepared && player != null && player.isPlaying()) player.pause();
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
    if (prepared && player != null)
      player.seekTo((int) Math.max(0, Math.min(player.getDuration(), ms)));
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
    boolean playing = prepared && player != null && player.isPlaying();
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

  @Override
  public void onDestroy() {
    releasePlayer();
    abandonFocus();
    unregisterReceiver(noisy);
    listeners.clear();
    getSystemService(NotificationManager.class).cancel(NOTIFICATION);
    session.release();
    super.onDestroy();
  }
}
