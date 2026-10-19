package com.zikra.app.audio;

/** Hafs verse numbering used by the Islamic Network audio CDN (6,236 ayahs). */
public final class Recitation {
  private Recitation() {}

  private static final int[] COUNTS = {
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
    112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
    54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
    14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
    29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
    11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
  };
  public static final String[] IDS = {"ar.alafasy", "ar.husary", "ar.minshawi"};
  public static final String[] NAMES = {
    "Mishary Rashid Alafasy", "Mahmoud Khalil Al-Husary", "Mohamed Siddiq Al-Minshawi"
  };

  public static int count(int chapter) {
    if (chapter < 1 || chapter > 114) throw new IllegalArgumentException("Surah tidak valid");
    return COUNTS[chapter - 1];
  }

  public static int reciterIndex(String id) {
    for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
    throw new IllegalArgumentException("Qari tidak tersedia");
  }

  public static int globalVerse(int chapter, int verse) {
    if (verse < 1 || verse > count(chapter)) throw new IllegalArgumentException("Ayat tidak valid");
    int number = verse;
    for (int i = 0; i < chapter - 1; i++) number += COUNTS[i];
    return number;
  }

  public static String url(String reciter, int chapter, int verse) {
    reciterIndex(reciter);
    return "https://cdn.islamic.network/quran/audio/128/"
        + reciter
        + "/"
        + globalVerse(chapter, verse)
        + ".mp3";
  }
}
