package com.zikra.app.audio;

/** Completion advances by real media completion events, never by estimated verse durations. */
public final class RecitationQueue {
  public final int chapter, first, last, repeats;
  public final boolean loop;
  public int verse, repetition = 1;

  public RecitationQueue(int chapter, int first, int last, int verse, int repeats, boolean loop) {
    if (first < 1
        || last > Recitation.count(chapter)
        || first > last
        || verse < first
        || verse > last
        || repeats < 1
        || repeats > 10) throw new IllegalArgumentException("Rentang atau pengulangan tidak valid");
    this.chapter = chapter;
    this.first = first;
    this.last = last;
    this.verse = verse;
    this.repeats = repeats;
    this.loop = loop;
  }

  public boolean complete() {
    if (repetition < repeats) {
      repetition++;
      return true;
    }
    repetition = 1;
    if (verse < last) {
      verse++;
      return true;
    }
    if (loop) {
      verse = first;
      return true;
    }
    return false;
  }

  public boolean skip(int direction) {
    int next = verse + direction;
    if (next < first || next > last) return false;
    verse = next;
    repetition = 1;
    return true;
  }
}
