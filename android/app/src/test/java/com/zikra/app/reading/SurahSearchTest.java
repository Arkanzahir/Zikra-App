package com.zikra.app.reading;

import static org.junit.Assert.*;

import org.junit.Test;

public final class SurahSearchTest {
  @Test
  public void latinNamesIgnoreSpacingApostrophesAndCase() {
    assertTrue(SurahSearch.matches(" AL MULK ", 67, "Al-Mulk", "Kerajaan", "الْمُلْكِ"));
    assertTrue(SurahSearch.matches("Ali Imran", 3, "Ali 'Imran", "Keluarga Imran", "آل عمران"));
    assertTrue(SurahSearch.matches("kerajaan", 67, "Al-Mulk", "Kerajaan", "الْمُلْكِ"));
  }

  @Test
  public void arabicSearchIgnoresVocalizationTatweelAndAlefVariants() {
    assertTrue(
        SurahSearch.matches("البقرة", 2, "Al-Baqarah", "Sapi Betina", "سُورَةُ ٱلْبَقَرَةِ"));
    assertEquals(SurahSearch.normalize("الأنعام"), SurahSearch.normalize("ٱلْأَنْعَـامِ"));
    assertEquals(SurahSearch.normalize("الاعراف"), SurahSearch.normalize("الأَعْرَاف"));
  }

  @Test
  public void arabicDigitsAndFullChapterNumberMatchWithoutPartialIds() {
    assertTrue(SurahSearch.matches("٦٧", 67, "Al-Mulk", "Kerajaan", "الملك"));
    assertFalse(SurahSearch.matches("6", 67, "Al-Mulk", "Kerajaan", "الملك"));
    assertTrue(SurahSearch.matches("", 114, "An-Nas", "Manusia", "الناس"));
    assertFalse(SurahSearch.matches("xyz", 114, "An-Nas", "Manusia", "الناس"));
  }
}
