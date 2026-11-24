package com.zikra.app.audio;

import static org.junit.Assert.*;

import org.junit.Test;

public final class PlaybackProgressTest {
  @Test public void stalledPlaybackIsDetectedAtStartAndMidVerse() {
    PlaybackProgress progress = new PlaybackProgress();
    assertFalse(progress.stalled(500, 0));
    assertFalse(progress.stalled(15_499, 0));
    assertTrue(progress.stalled(15_500, 0));

    progress.reset();
    assertFalse(progress.stalled(30_000, 400));
    assertFalse(progress.stalled(44_999, 3_000));
    assertFalse(progress.stalled(59_998, 3_000));
    assertTrue(progress.stalled(59_999, 3_000));
  }

  @Test public void advancingPositionRestartsTheBufferingAllowance() {
    PlaybackProgress progress = new PlaybackProgress();
    for (int i = 0; i < 10; i++) {
      assertFalse(progress.stalled(i * 14_000L, i * 100));
    }
  }

  @Test public void pausedTimeAndSeekDoNotCountAgainstANewAttempt() {
    PlaybackProgress progress = new PlaybackProgress();
    assertFalse(progress.stalled(100, 3_000));
    progress.reset();
    assertFalse(progress.stalled(40_000, 3_000));
    assertFalse(progress.stalled(44_000, 1_000));
    assertFalse(progress.stalled(58_999, 1_000));
    assertTrue(progress.stalled(59_000, 1_000));
  }
}
