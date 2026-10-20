package com.zikra.app.reading.mushaf;

import com.zikra.app.audio.Recitation;
import java.util.*;

/** Position lookup only: no clock, inferred word durations or mapping by reciter name. */
public final class WordTimeline {
  public static final class Segment {
    public final int word;
    public final long startMs, endMs;

    public Segment(int word, long startMs, long endMs) {
      if (word < 1 || startMs < 0 || endMs <= startMs)
        throw new IllegalArgumentException("Invalid word timing");
      this.word = word;
      this.startMs = startMs;
      this.endMs = endMs;
    }
  }

  public static final class Highlight {
    public final int word;
    public final float fraction;
    private Highlight(int word, float fraction) { this.word = word; this.fraction = fraction; }
  }

  private static final Highlight NONE = new Highlight(0, 0);
  public final int chapter, verse, wordCount;
  public final long durationMs;
  public final String recordingSha256, sourceRevision;
  public final List<Segment> segments;

  public WordTimeline(int chapter, int verse, int wordCount, long durationMs,
      String recordingSha256, String sourceRevision, List<Segment> segments) {
    Recitation.globalVerse(chapter, verse);
    if (wordCount < 1 || durationMs < 1 || recordingSha256 == null
        || !recordingSha256.matches("[a-fA-F0-9]{64}")
        || sourceRevision == null || sourceRevision.trim().isEmpty()
        || segments == null || segments.size() != wordCount)
      throw new IllegalArgumentException("Incomplete recording/timing identity");
    long previousEnd = 0;
    int expectedWord = 1;
    for (Segment segment : segments) {
      if (segment == null || segment.word != expectedWord++
          || segment.startMs < previousEnd || segment.endMs > durationMs)
        throw new IllegalArgumentException("Timing order, coverage or duration mismatch");
      previousEnd = segment.endMs;
    }
    this.chapter = chapter;
    this.verse = verse;
    this.wordCount = wordCount;
    this.durationMs = durationMs;
    this.recordingSha256 = recordingSha256.toLowerCase(Locale.ROOT);
    this.sourceRevision = sourceRevision;
    this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
  }

  public Highlight at(int playingChapter, int playingVerse, String verifiedRecordingSha256,
      long playerPositionMs, boolean buffering, boolean enabled) {
    if (!enabled || buffering || chapter != playingChapter || verse != playingVerse
        || verifiedRecordingSha256 == null
        || !recordingSha256.equalsIgnoreCase(verifiedRecordingSha256)
        || playerPositionMs < 0 || playerPositionMs >= durationMs) return NONE;
    int low = 0, high = segments.size() - 1;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      Segment segment = segments.get(middle);
      if (playerPositionMs < segment.startMs) high = middle - 1;
      else if (playerPositionMs >= segment.endMs) low = middle + 1;
      else return new Highlight(segment.word,
          (float) (playerPositionMs - segment.startMs) / (segment.endMs - segment.startMs));
    }
    return NONE;
  }
}
