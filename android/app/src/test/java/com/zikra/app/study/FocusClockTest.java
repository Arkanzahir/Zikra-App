package com.zikra.app.study;

import static org.junit.Assert.*;

import org.junit.Test;

public class FocusClockTest {
  @Test
  public void elapsedCountdownAndPauseResume() {
    FocusClock c = new FocusClock();
    c.start(100, 1);
    c.update(60100, 1);
    assertEquals(24 * 60000L, c.remaining);
    c.pause(60100, 1);
    c.update(999999, 1);
    assertEquals(24 * 60000L, c.remaining);
    c.start(1000000, 1);
    c.update(1060000, 1);
    assertEquals(23 * 60000L, c.remaining);
  }

  @Test
  public void completionRequiresExplicitNextPhase() {
    FocusClock c = new FocusClock();
    c.start(0, 1);
    c.update(1500000, 1);
    assertTrue(c.finished);
    assertFalse(c.running);
    assertFalse(c.breakPhase);
    c.update(9000000, 1);
    assertFalse(c.breakPhase);
    c.start(9000000, 1);
    assertTrue(c.breakPhase);
    assertEquals(5 * 60000L, c.remaining);
  }

  @Test
  public void rebootPausesWithoutUsingAnotherBootClock() {
    FocusClock c = new FocusClock();
    c.start(10000, 1);
    c.update(70000, 1);
    c.update(200, 2);
    assertFalse(c.running);
    assertFalse(c.finished);
    assertEquals(24 * 60000L, c.remaining);
  }

  @Test
  public void resetInvalidatesSessionAndPreservesTaskSelection() {
    FocusClock c = new FocusClock();
    c.start(10, 1);
    long old = c.session;
    c.reset(50, 10, 8);
    assertTrue(c.session > old);
    assertEquals(8, c.taskId);
    assertEquals(50 * 60000L, c.remaining);
    assertFalse(c.running);
  }

  @Test
  public void breakFinishesWithoutAutomaticFocus() {
    FocusClock c = new FocusClock();
    c.start(0, 1);
    c.update(1500000, 1);
    c.start(1500000, 1);
    c.update(1800000, 1);
    assertTrue(c.finished);
    assertTrue(c.breakPhase);
    assertFalse(c.running);
    c.start(1800000, 1);
    assertFalse(c.breakPhase);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidDurationRejected() {
    new FocusClock().reset(0, 5, -1);
  }
}
