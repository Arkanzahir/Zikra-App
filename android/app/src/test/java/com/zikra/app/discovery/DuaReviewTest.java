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
    assertEquals(DuaReview.NONE, DuaReview.forSource(999, "a", "b", "c", "d"));
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
      if (entry.review == DuaReview.TITLE_TYPO || entry.review == DuaReview.TITLE_SCOPE) {
        assertNotEquals(entry.title, entry.review.displayTitle(entry.title));
        assertEquals(entry.title, DuaReview.SOURCE_CHANGED.displayTitle(entry.title));
        assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(entry.number,
            entry.title + "changed", entry.sourceArabic, entry.meaning, entry.reference));
      }
      if (entry.review == DuaReview.ARABIC_INSTRUCTIONS) {
        assertFalse(entry.arabic.matches("(?s).*[A-Za-z].*"));
        assertTrue(entry.sourceArabic.endsWith(entry.arabic));
        assertTrue(entry.sourceArabic.contains("\n"));
        assertEquals(DuaReview.SOURCE_CHANGED, DuaReview.forSource(entry.number,
            entry.title, entry.sourceArabic + "changed", entry.meaning, entry.reference));
      } else if (entry.review == DuaReview.WITR_LAYOUT) {
        assertFalse(entry.arabic.contains("("));
        assertFalse(entry.arabic.contains(")"));
        assertEquals(entry.sourceArabic.replace("(", "").replace(")", "").trim(), entry.arabic);
      } else if (entry.number != 6) assertEquals(entry.sourceArabic, entry.arabic);
    }
    assertEquals(14, corrected);
  }
}
