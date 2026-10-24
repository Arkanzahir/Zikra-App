package com.zikra.app.discovery;

import static org.junit.Assert.*;
import org.junit.Test;

public final class HadithSearchTest {
  @Test public void arabicDiacriticsAndArabicIndicNumbersAreSearchable() {
    assertEquals("انما الاعمال", HadithSearch.normalize("إِنَّمَا الْأَعْمَالُ"));
    assertEquals("3001", HadithSearch.normalize("٣٠٠١"));
  }
  @Test public void searchWildcardsRemainLiteral() {
    assertEquals("%a\\%b\\_c\\\\d%", HadithSearch.literalLike("a%b_c\\d"));
    assertEquals("' or 1=1 --", HadithSearch.normalize("' OR 1=1 --"));
  }
  @Test public void stableIdentityDoesNotUseTheArrayPositionOrPackageRevision() {
    HadithCollection.Entry e = HadithCollection.sourced("bukhari", 3001, "x", "y");
    assertEquals("tafsirq-v1:bukhari:3001", e.id);
    assertFalse(e.excerpt);
    assertEquals("bukhari", HadithCollection.bookForId(e.id));
    assertNull(HadithCollection.bookForId("../../outside"));
    assertNull(HadithCollection.bookForId("tafsirq-v1:other:1"));
  }
}
