package com.zikra.app.reading;

import static org.junit.Assert.*;
import org.junit.Test;

public final class CompassReadingTest {
  @Test public void waitingAndOldReadingsNeverClaimAlignment() {
    CompassReading state=new CompassReading();assertEquals(CompassReading.Status.WAITING,state.status(10000));
    state.accept(294,294,true,true,10000,10000);assertTrue(state.aligned(10000));
    assertEquals(CompassReading.Status.STALE,state.status(13001));assertFalse(state.aligned(13001));
    state.reset();assertFalse(state.aligned(13002));
  }
  @Test public void lowAccuracyCannotRecoverUntilANewReliableMeasurement() {
    CompassReading state=new CompassReading();state.accept(294,294,true,true,10000,10000);
    state.downgradeAccuracy();assertFalse(state.aligned(10001));assertEquals(CompassReading.Status.LOW_ACCURACY,state.status(10001));
    state.accept(294,294,false,true,10002,10002);assertFalse(state.aligned(10002));
    state.accept(294,294,true,true,10003,10003);assertTrue(state.aligned(10003));
  }
  @Test public void tiltedPhoneUsesStaticGuidance() {
    CompassReading state=new CompassReading();state.accept(294,294,true,false,10000,10000);
    assertEquals(CompassReading.Status.TILTED,state.status(10000));assertFalse(state.aligned(10000));
  }
  @Test public void futureStaleNonFiniteAndNonMonotonicSamplesAreRejected() {
    CompassReading state=new CompassReading();
    assertFalse(state.accept(0,0,true,true,10001,10000));assertEquals(CompassReading.Status.INVALID,state.status(10000));
    assertFalse(state.accept(0,0,true,true,6000,10000));
    assertFalse(state.accept(Float.NaN,0,true,true,10000,10000));
    assertFalse(state.accept(0,Float.POSITIVE_INFINITY,true,true,10000,10000));
    assertTrue(state.accept(90,90,true,true,10000,10000));
    assertFalse(state.accept(180,90,true,true,9999,10000));assertEquals(90,state.heading(),0);
  }
  @Test public void wraparoundAndHysteresisAreStable() {
    CompassReading state=new CompassReading();state.accept(359,1,true,true,10000,10000);
    assertTrue(state.aligned(10000));state.accept(1,1,true,true,10100,10100);
    assertEquals(359.4,state.heading(),0.001);assertTrue(state.aligned(10100));
    state.accept(20,1,true,true,10200,10200);assertTrue(state.aligned(10200));
    state.accept(40,1,true,true,10300,10300);assertFalse(state.aligned(10300));
  }
  @Test public void invalidQuaternionsCannotEnterTheMatrixCalculation() {
    assertTrue(CompassReading.validVector(new float[]{0,0,0}));
    assertTrue(CompassReading.validVector(new float[]{0,0,0,1,-1}));
    for(float[] vector:new float[][]{null,new float[]{0,1},new float[]{0,0,0,0},new float[]{Float.NaN,0,0},new float[]{0,Float.POSITIVE_INFINITY,0,1},new float[]{2,0,0}})
      assertFalse(CompassReading.validVector(vector));
  }
}
