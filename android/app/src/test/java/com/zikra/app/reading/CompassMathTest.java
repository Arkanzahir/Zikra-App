package com.zikra.app.reading;

import static org.junit.Assert.*;

import org.junit.Test;

public class CompassMathTest {
  @Test
  public void turnsAcrossNorthUseShortestDirection() {
    assertEquals(2f, CompassMath.delta(1f, 359f), 0.001f);
    assertEquals(-2f, CompassMath.delta(359f, 1f), 0.001f);
    assertEquals(-66f, CompassMath.delta(294f, 0f), 0.001f);
  }

  @Test
  public void smoothingDoesNotSpinAcrossNorth() {
    assertEquals(0f, CompassMath.smooth(359f, 1f, 0.5f), 0.001f);
  }

  @Test
  public void alignmentUsesCircularDistanceAndTolerance() {
    assertTrue(CompassMath.aligned(359f, 1f, 3f));
    assertTrue(CompassMath.aligned(299f, 294f, 5f));
    assertFalse(CompassMath.aligned(300f, 294f, 5f));
  }

  @Test
  public void normalizesNegativeAndMultipleRevolutions() {
    assertEquals(294f, CompassMath.normalize(-66f), 0.001f);
    assertEquals(14f, CompassMath.normalize(734f), 0.001f);
  }
}
