package com.zikra.app.reading.mushaf;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** Synthetic identifiers/tokens test validation only; no fixture is a Quran edition or recording. */
public class MushafFoundationTest {
  private static final String HASH = String.join("", Collections.nCopies(64, "a"));

  private MushafPage.Word word(int index, int verse, int position) {
    return new MushafPage.Word(index, 1, verse, position, "TEST-GLYPH");
  }

  private MushafPage.Line line(int number, MushafPage.Word... words) {
    return new MushafPage.Line(number, MushafPage.Kind.AYAH, 1, false, Arrays.asList(words));
  }

  private MushafPage page(MushafPage.Line... lines) {
    return new MushafPage("SYNTHETIC-TEST", "test-1", "TEST-FONT-NOT-BUNDLED",
        2, 3, 15, Arrays.asList(lines));
  }

  private WordTimeline timeline() {
    return new WordTimeline(1, 1, 2, 2000, HASH, "synthetic-timing-1", Arrays.asList(
        new WordTimeline.Segment(1, 100, 500), new WordTimeline.Segment(2, 700, 1500)));
  }

  @Test public void pageKeepsVerseAcrossLinesAndSeveralVersesOnOneLine() {
    MushafPage page = page(line(1, word(40, 1, 3), word(41, 1, 4)),
        line(2, word(42, 1, 5), word(43, 2, 1), word(44, 2, 2)));
    assertTrue(page.contains(1, 1));
    assertTrue(page.contains(1, 2));
    assertFalse(page.contains(1, 3));
    assertEquals(2, page.lines.size());
    assertEquals(3, page.lines.get(1).words.size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingWordsCannotSilentlyDisappear() {
    page(line(1, word(1, 1, 1), word(3, 1, 2)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void repeatedWordsCannotRenderTwice() {
    page(line(1, word(1, 1, 1)), line(2, word(1, 1, 1)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingVersePositionIsRejectedAcrossLineBreaks() {
    page(line(1, word(1, 1, 1)), line(2, word(2, 1, 3)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void editionLineBoundsCannotBeExceeded() {
    page(line(16, word(1, 1, 1)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void verseCannotJumpToAnotherSurahByInvalidNumber() {
    word(1, 8, 1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void callerCannotChangeValidatedPage() {
    page(line(1, word(1, 1, 1))).lines.clear();
  }

  @Test public void positionControlsSeekPauseAndRepeatWithoutElapsedClock() {
    WordTimeline timeline = timeline();
    assertEquals(1, timeline.at(1, 1, HASH, 300, false, true).word);
    assertEquals(.5f, timeline.at(1, 1, HASH, 300, false, true).fraction, .001f);
    assertEquals(2, timeline.at(1, 1, HASH, 1100, false, true).word);
    assertEquals(.5f, timeline.at(1, 1, HASH, 1100, false, true).fraction, .001f);
    assertEquals(.5f, timeline.at(1, 1, HASH, 1100, false, true).fraction, .001f);
    assertEquals(1, timeline.at(1, 1, HASH, 100, false, true).word);
    assertEquals(0f, timeline.at(1, 1, HASH, 100, false, true).fraction, .001f);
  }

  @Test public void gapsAndBoundariesDoNotInventWordTiming() {
    WordTimeline timeline = timeline();
    for (long position : new long[] {-1, 0, 99, 500, 600, 1500, 2000, Long.MAX_VALUE})
      assertEquals(0, timeline.at(1, 1, HASH, position, false, true).word);
    assertEquals(2, timeline.at(1, 1, HASH, 700, false, true).word);
  }

  @Test public void mismatchedRecordingAyahBufferingOrOptOutDisableHighlight() {
    WordTimeline timeline = timeline();
    assertEquals(0, timeline.at(1, 1, HASH.replace('a', 'b'), 300, false, true).word);
    assertEquals(0, timeline.at(1, 2, HASH, 300, false, true).word);
    assertEquals(0, timeline.at(2, 1, HASH, 300, false, true).word);
    assertEquals(0, timeline.at(1, 1, null, 300, false, true).word);
    assertEquals(0, timeline.at(1, 1, HASH, 300, true, true).word);
    assertEquals(0, timeline.at(1, 1, HASH, 300, false, false).word);
  }

  @Test(expected = IllegalArgumentException.class)
  public void overlappingTimingsAreRejected() {
    new WordTimeline(1, 1, 2, 2000, HASH, "test", Arrays.asList(
        new WordTimeline.Segment(1, 0, 1000), new WordTimeline.Segment(2, 900, 1500)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void incompleteWordCoverageIsRejected() {
    new WordTimeline(1, 1, 3, 2000, HASH, "test", timeline().segments);
  }

  @Test(expected = IllegalArgumentException.class)
  public void segmentsCannotExceedRecordingDuration() {
    new WordTimeline(1, 1, 2, 1000, HASH, "test", timeline().segments);
  }

  private MushafPage indexedPage(String edition, int number, MushafPage.Word... words) {
    return new MushafPage(edition, "test-1", "TEST-FONT-" + number,
        number, 2, 15, Arrays.asList(line(1, words)));
  }

  @Test public void modeSwitchKeepsPreferredPageWhenAyahSpansTwoPages() {
    MushafIndex index = new MushafIndex(Arrays.asList(
        indexedPage("TEST", 1, word(1, 1, 1)),
        indexedPage("TEST", 2, word(2, 1, 2), word(3, 2, 1))));
    assertEquals(2, index.pageFor(1, 1, 2));
    assertEquals(1, index.pageFor(1, 1, 0));
    assertEquals(2, index.pageFor(1, 2, 1));
    assertEquals(2, index.page(2).number);
  }

  @Test(expected = IllegalArgumentException.class)
  public void packagesCannotMixEditions() {
    new MushafIndex(Arrays.asList(indexedPage("TEST-A", 1, word(1, 1, 1)),
        indexedPage("TEST-B", 2, word(2, 1, 2))));
  }

  @Test(expected = IllegalArgumentException.class)
  public void pageBoundaryCannotDropWords() {
    new MushafIndex(Arrays.asList(indexedPage("TEST", 1, word(1, 1, 1)),
        indexedPage("TEST", 2, word(3, 1, 3))));
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingPackagePageIsRejected() {
    new MushafIndex(Arrays.asList(indexedPage("TEST", 1, word(1, 1, 1))));
  }
}
