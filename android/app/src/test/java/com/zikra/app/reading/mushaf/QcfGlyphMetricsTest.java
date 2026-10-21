package com.zikra.app.reading.mushaf;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import org.junit.*;

public class QcfGlyphMetricsTest {
  @Test
  public void threeDigitMedallionsKeepTheirSourceGlyphAdvance() throws Exception {
    String path = System.getenv("ZIKRA_MUSHAF_AUDIT_DIR");
    Assume.assumeTrue("Optional page 49 font/snapshot needed", path != null);
    MushafDocument page =
        MushafDocument.parse(
            new String(
                Files.readAllBytes(Paths.get(path, "page-49.json")),
                java.nio.charset.StandardCharsets.UTF_8),
            49);
    QcfGlyphMetrics font = new QcfGlyphMetrics(new File(path, "p49.ttf"));
    int medallions = 0;
    for (MushafPage.Line line : page.page.lines)
      for (MushafPage.Word word : line.words) {
        assertTrue(
            "Positive source advance at " + word.verseKey() + ":" + word.position,
            font.width(word.glyph, 100) > 0);
        if (word.verseEnd) {
          assertTrue(word.verse >= 283 && word.verse <= 286);
          medallions++;
        }
      }
    assertEquals(4, medallions);
  }

  @Test
  public void originalFontKeepsPositiveAdvancesForUnicodeCombiningCodes() throws Exception {
    String path = System.getenv("ZIKRA_MUSHAF_AUDIT_DIR");
    Assume.assumeTrue("Official font snapshot needed", path != null);
    QcfGlyphMetrics font = new QcfGlyphMetrics(new File(path, "p1.ttf"));
    for (int cp = 0xfc41; cp <= 0xfc64; cp++)
      assertTrue(
          "Every page 1 glyph has a real native advance", font.advance(font.glyph(cp), 100) > 0);
    assertTrue(
        "QCF FC5E must not become a zero-width combining mark", font.width("\ufc5e", 100) > 0);
  }

  @Test
  public void corruptFontCannotBecomeARenderablePage() throws Exception {
    File file = File.createTempFile("qcf-invalid-", ".ttf");
    try {
      for (int size : new int[] {0, 63, 128}) {
        Files.write(file.toPath(), new byte[size]);
        try {
          new QcfGlyphMetrics(file);
          fail("Reject corrupt font");
        } catch (IOException expected) {
        }
      }
    } finally {
      file.delete();
    }
  }
}
