package com.zikra.app.discovery;

import static org.junit.Assert.*;
import java.nio.file.Files;
import org.junit.Assume;
import org.junit.Test;

public final class DuaReviewTest {
  @Test public void snapshotIdentityIncludesBoundariesAndEveryReviewedField() {
    assertNotEquals(DuaReview.fingerprint("a", "bc", "d", "e"), DuaReview.fingerprint("ab", "c", "d", "e"));
    assertNotEquals(DuaReview.fingerprint("a", "b", "c", "d"), DuaReview.fingerprint("a", "b", "c", "changed"));
    assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(18, "changed", "b", "c", "d"));
    assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(999, "a", "b", "c", "d"));
    assertEquals("fixture", DuaReview.SOURCE_CHANGED.displayArabic("fixture"));
  }
  @Test(expected = IllegalArgumentException.class) public void rejectsAnEmbeddedSnapshotSeparator() {
    DuaReview.fingerprint("a\0b", "c", "d", "e");
  }
  @Test public void auditedRealCatalogueAppliesOnlyTheReviewedCorrections() throws Exception {
    String fixture = System.getenv("ZIKRA_DOA_AUDIT_FILE"); Assume.assumeNotNull(fixture);
    DuaCatalog catalog = DuaCatalog.parse(new String(Files.readAllBytes(java.nio.file.Paths.get(fixture)), java.nio.charset.StandardCharsets.UTF_8));
    assertEquals(225, catalog.entries.size());
    int corrected = 0;
    for (DuaCatalog.Entry entry : catalog.entries) {
      if (entry.review != DuaReview.NONE) corrected++;
      if (entry.number == 6) {
        assertEquals(DuaReview.WAKE_UP, entry.review);
        assertNotEquals(entry.sourceArabic, entry.arabic);
        assertTrue(entry.arabic.endsWith("النُّشُورُ"));
      }
      if (entry.number == 18) {
        assertEquals(DuaReview.WIND, entry.review);
        assertTrue(entry.search.contains("angin"));
        assertFalse(entry.search.contains("halilintar"));
        assertFalse(entry.search.contains("petir"));
      }
      if (entry.number == 20) assertEquals(DuaReview.RAIN_NUMBERING, entry.review);
      if (entry.number == 96) assertEquals(DuaReview.GRADING_CONTEXT, entry.review);
      if (entry.number == 139) assertEquals(DuaReview.AFTER_MEAL_NUMBERING, entry.review);
      if (java.util.Arrays.asList(16, 55, 66, 70, 103, 121).contains(entry.number)) {
        assertNotEquals(entry.title, entry.review.displayTitle(entry.title));
        assertEquals(entry.title, DuaReview.SOURCE_CHANGED.displayTitle(entry.title));
        assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(entry.number,
            entry.title + "changed", entry.sourceArabic, entry.sourceMeaning, entry.reference));
      }
      if (java.util.Arrays.asList(57, 76, 77, 209).contains(entry.number)) {
        assertFalse(entry.arabic.matches("(?s).*[A-Za-z].*"));
        assertTrue(entry.sourceArabic.endsWith(entry.arabic));
        assertTrue(entry.sourceArabic.contains("\n"));
        assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(entry.number,
            entry.title, entry.sourceArabic + "changed", entry.sourceMeaning, entry.reference));
      } else if (entry.review == DuaReview.WITR_LAYOUT) {
        assertFalse(entry.arabic.contains("("));
        assertFalse(entry.arabic.contains(")"));
        assertEquals(entry.sourceArabic.replace("(", "").replace(")", "").trim(), entry.arabic);
      } else if (entry.number == 186) {
        assertEquals("لَبَّيْكَ، إِنَّ الْعَيْشَ عَيْشُ الْآخِرَةِ", entry.arabic);
        assertTrue(entry.search.contains(HadithSearch.normalize("لبيك")));
      } else if (entry.number == 188) {
        assertTrue(entry.arabic.contains("اَلأَحَدُ"));
        assertFalse(entry.arabic.contains("اَلأَ حَدُ"));
      } else if (entry.number == 111) {
        assertTrue(entry.arabic.contains("بِعِلْمِكَ الْغَيْبَ"));
        assertTrue(entry.arabic.contains("فِي الْغَيْبِ"));
        assertTrue(entry.latin.contains("bi'ilmikal ghoiba"));
        assertTrue(entry.latin.contains("fil ghoibi"));
        assertTrue(entry.sourceLatin.contains("bi'ilmikal ghoibi"));
      } else if (entry.number != 6) assertEquals(entry.sourceArabic, entry.arabic);
      if (entry.number == 154) {
        assertTrue(entry.sourceMeaning.contains("berlindung berlindung"));
        assertFalse(entry.meaning.contains("berlindung berlindung"));
      } else if (entry.number == 123) {
        assertTrue(entry.meaning.contains("kokohkanlah aku"));
        assertTrue(entry.search.contains("kokohkanlah aku"));
        assertTrue(entry.meaning.contains("aku menemui-Mu"));
        assertTrue(entry.sourceMeaning.contains("kokohkanlah kami"));
      } else assertEquals(entry.sourceMeaning, entry.meaning);
      {
        assertNotEquals(DuaReview.SOURCE_CHANGED, entry.review);
        assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(entry.number,
            entry.title + "changed", entry.sourceArabic, entry.sourceMeaning, entry.reference));
        assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(entry.number,
            entry.title, entry.sourceArabic + "changed", entry.sourceMeaning, entry.reference));
        assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(entry.number,
            entry.title, entry.sourceArabic, entry.sourceMeaning + "changed", entry.reference));
        assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(entry.number,
            entry.title, entry.sourceArabic, entry.sourceMeaning, entry.reference + "changed"));
        assertEquals(entry.sourceMeaning, DuaReview.SOURCE_CHANGED.displayMeaning(entry.sourceMeaning));
        assertEquals(entry.sourceLatin, DuaReview.SOURCE_CHANGED.displayLatin(entry.sourceLatin));
      }
      if (entry.number != 111) assertEquals(entry.sourceLatin, entry.latin);
      if (entry.number == 55) assertTrue(entry.search.contains("dzikir ketika masuk pasar"));
    }
    assertEquals(33, corrected);
  }
}
