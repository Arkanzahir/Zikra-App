package com.zikra.app.reading.mushaf;

import com.zikra.app.audio.Recitation;
import java.util.*;

/** Index only complete single-edition packages; network pagination is not mushaf pagination. */
public final class MushafIndex {
  private final List<MushafPage> pages;
  private final Map<Integer, List<Integer>> versePages = new HashMap<>();

  public MushafIndex(List<MushafPage> source) {
    if (source == null || source.isEmpty() || source.get(0) == null
        || source.size() != source.get(0).pageCount)
      throw new IllegalArgumentException("Incomplete mushaf package");
    MushafPage first = source.get(0);
    int previousIndex = 0, previousVerse = 0, previousPosition = 0;
    for (int i = 0; i < source.size(); i++) {
      MushafPage page = source.get(i);
      if (page == null || page.number != i + 1 || page.pageCount != source.size()
          || !first.edition.equals(page.edition) || !first.revision.equals(page.revision)
          || first.lineCount != page.lineCount)
        throw new IllegalArgumentException("Mixed editions, revisions or page order");
      for (MushafPage.Line line : page.lines)
        for (MushafPage.Word word : line.words) {
          int verse = Recitation.globalVerse(word.chapter, word.verse);
          if (previousIndex > 0 && (word.index != previousIndex + 1
              || verse < previousVerse || verse > previousVerse + 1
              || (verse == previousVerse && word.position != previousPosition + 1)
              || (verse != previousVerse && word.position != 1)))
            throw new IllegalArgumentException("Word gap or duplicate between pages");
          List<Integer> locations = versePages.computeIfAbsent(verse, key -> new ArrayList<>());
          if (locations.isEmpty() || locations.get(locations.size() - 1) != page.number)
            locations.add(page.number);
          previousIndex = word.index;
          previousVerse = verse;
          previousPosition = word.position;
        }
    }
    pages = Collections.unmodifiableList(new ArrayList<>(source));
  }

  public int pageFor(int chapter, int verse, int preferredPage) {
    List<Integer> locations = versePages.get(Recitation.globalVerse(chapter, verse));
    if (locations == null) throw new IllegalArgumentException("Ayah missing from package");
    return locations.contains(preferredPage) ? preferredPage : locations.get(0);
  }

  public MushafPage page(int number) {
    if (number < 1 || number > pages.size()) throw new IllegalArgumentException("Invalid page");
    return pages.get(number - 1);
  }
}
