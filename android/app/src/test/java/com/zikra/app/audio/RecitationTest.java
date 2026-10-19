package com.zikra.app.audio;

import static org.junit.Assert.*;

import org.junit.Test;

public class RecitationTest {
  @Test
  public void knownVerseKeysMapToPublishedGlobalNumbering() {
    assertEquals(1, Recitation.globalVerse(1, 1));
    assertEquals(7, Recitation.globalVerse(1, 7));
    assertEquals(8, Recitation.globalVerse(2, 1));
    assertEquals(262, Recitation.globalVerse(2, 255));
    assertEquals(6236, Recitation.globalVerse(114, 6));
    assertEquals(
        "https://cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3",
        Recitation.url("ar.alafasy", 2, 255));
  }

  @Test
  public void allAyahsHaveDistinctContinuousIdentifiers() {
    int expected = 0;
    for (int chapter = 1; chapter <= 114; chapter++)
      for (int verse = 1; verse <= Recitation.count(chapter); verse++)
        assertEquals(++expected, Recitation.globalVerse(chapter, verse));
    assertEquals(6236, expected);
  }

  @Test
  public void rangeRepeatsEachVerseBeforeAdvancingAndStopsAtEnd() {
    RecitationQueue q = new RecitationQueue(67, 1, 5, 1, 3, false);
    for (int verse = 1; verse <= 5; verse++)
      for (int repeat = 1; repeat <= 3; repeat++) {
        assertEquals(verse, q.verse);
        assertEquals(repeat, q.repetition);
        assertEquals(!(verse == 5 && repeat == 3), q.complete());
      }
    assertEquals(5, q.verse);
  }

  @Test
  public void explicitRangeLoopReturnsToFirstAyah() {
    RecitationQueue q = new RecitationQueue(67, 1, 5, 5, 1, true);
    assertTrue(q.complete());
    assertEquals(1, q.verse);
  }

  @Test
  public void skipsCannotEscapeHifzRangeAndResetRepetition() {
    RecitationQueue q = new RecitationQueue(67, 2, 4, 2, 3, false);
    assertFalse(q.skip(-1));
    q.complete();
    assertEquals(2, q.repetition);
    assertTrue(q.skip(1));
    assertEquals(3, q.verse);
    assertEquals(1, q.repetition);
    q.skip(1);
    assertFalse(q.skip(1));
    assertEquals(4, q.verse);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidAyahCannotResolveToDifferentSurahAudio() {
    Recitation.globalVerse(1, 8);
  }

  @Test(expected = IllegalArgumentException.class)
  public void reciterMustBeAllowlisted() {
    Recitation.url("../../other", 1, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void rangeMustMatchRealSurahCount() {
    new RecitationQueue(1, 1, 8, 1, 1, false);
  }
}
