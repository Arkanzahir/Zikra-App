package com.zikra.app.data;

/** A user-chosen range, not Quran verse text or an API catalog cache. */
public final class HifzTarget {
  public final int chapterId, versesCount, firstVerse, lastVerse;
  public final String chapterName;

  public HifzTarget(int id, String name, int count, int first, int last) {
    if (id < 1
        || id > 114
        || name == null
        || name.trim().isEmpty()
        || name.length() > 100
        || count < 1
        || count > 286
        || first < 1
        || last < first
        || last > count) throw new IllegalArgumentException("Surah atau rentang ayat tidak valid");
    chapterId = id;
    chapterName = name.trim();
    versesCount = count;
    firstVerse = first;
    lastVerse = last;
  }

  public int count() {
    return lastVerse - firstVerse + 1;
  }
}
