package com.zikra.app.reading.mushaf;

import com.zikra.app.audio.Recitation;
import java.util.*;

/** A page from one edition. This model never wraps verses into invented page lines. */
public final class MushafPage {
  public enum Kind {
    AYAH,
    SURAH_NAME,
    BASMALLAH
  }

  public static final class Word {
    public final int index, chapter, verse, position;
    public final String glyph;
    public final boolean verseEnd;

    public Word(int index, int chapter, int verse, int position, String glyph) {
      this(index, chapter, verse, position, glyph, false);
    }

    public Word(int index, int chapter, int verse, int position, String glyph, boolean verseEnd) {
      Recitation.globalVerse(chapter, verse);
      if (index < 1 || position < 1 || glyph == null || glyph.trim().isEmpty())
        throw new IllegalArgumentException("Invalid mushaf word");
      if (glyph.indexOf('\n') >= 0 || glyph.indexOf('\r') >= 0)
        throw new IllegalArgumentException("A glyph cannot invent a line break");
      this.index = index;
      this.chapter = chapter;
      this.verse = verse;
      this.position = position;
      this.glyph = glyph;
      this.verseEnd = verseEnd;
    }

    public String verseKey() {
      return chapter + ":" + verse;
    }
  }

  public static final class Line {
    public final int number, chapter;
    public final Kind kind;
    public final boolean centered;
    public final List<Word> words;

    public Line(int number, Kind kind, int chapter, boolean centered, List<Word> words) {
      if (number < 1 || kind == null || chapter < 1 || chapter > 114 || words == null)
        throw new IllegalArgumentException("Invalid page line");
      if ((kind == Kind.AYAH) == words.isEmpty())
        throw new IllegalArgumentException("Only ayah lines contain words");
      this.number = number;
      this.kind = kind;
      this.chapter = chapter;
      this.centered = centered;
      this.words = Collections.unmodifiableList(new ArrayList<>(words));
    }
  }

  public final String edition, revision, fontAsset;
  public final int number, pageCount, lineCount;
  public final List<Line> lines;

  public MushafPage(
      String edition,
      String revision,
      String fontAsset,
      int number,
      int pageCount,
      int lineCount,
      List<Line> lines) {
    if (blank(edition)
        || blank(revision)
        || blank(fontAsset)
        || pageCount < 1
        || number < 1
        || number > pageCount
        || lineCount < 1
        || lines == null
        || lines.isEmpty()
        || lines.size() > lineCount)
      throw new IllegalArgumentException("Incomplete edition/page metadata");
    int previousLine = 0, previousIndex = 0, previousVerse = 0, previousPosition = 0;
    boolean containsWords = false;
    for (Line line : lines) {
      if (line == null || line.number <= previousLine || line.number > lineCount)
        throw new IllegalArgumentException("Line order/bounds mismatch");
      previousLine = line.number;
      for (Word word : line.words) {
        if (word == null) throw new IllegalArgumentException("Missing word");
        int verse = Recitation.globalVerse(word.chapter, word.verse);
        if ((previousIndex > 0 && word.index != previousIndex + 1)
            || verse < previousVerse
            || (verse == previousVerse && word.position != previousPosition + 1)
            || (previousVerse > 0
                && verse != previousVerse
                && (verse != previousVerse + 1 || word.position != 1)))
          throw new IllegalArgumentException("Missing, duplicate or reordered word");
        previousIndex = word.index;
        previousVerse = verse;
        previousPosition = word.position;
        containsWords = true;
      }
    }
    if (!containsWords) throw new IllegalArgumentException("Page has no Quran words");
    this.edition = edition;
    this.revision = revision;
    this.fontAsset = fontAsset;
    this.number = number;
    this.pageCount = pageCount;
    this.lineCount = lineCount;
    this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
  }

  private static boolean blank(String value) {
    return value == null || value.trim().isEmpty();
  }

  public boolean contains(int chapter, int verse) {
    for (Line line : lines)
      for (Word word : line.words) if (word.chapter == chapter && word.verse == verse) return true;
    return false;
  }
}
