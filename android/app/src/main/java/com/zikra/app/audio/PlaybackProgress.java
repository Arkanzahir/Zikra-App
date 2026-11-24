package com.zikra.app.audio;

/** Tracks decoder position while the user has requested playback. */
final class PlaybackProgress {
  private static final long STALL_TIMEOUT_MS = 15_000;
  private int lastPosition = -1;
  private long lastChangeAt;

  boolean stalled(long now, int position) {
    if (position != lastPosition || lastPosition < 0) {
      lastPosition = position;
      lastChangeAt = now;
      return false;
    }
    return now - lastChangeAt >= STALL_TIMEOUT_MS;
  }

  void reset() {
    lastPosition = -1;
  }
}
