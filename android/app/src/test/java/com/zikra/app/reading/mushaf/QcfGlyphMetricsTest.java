package com.zikra.app.reading.mushaf;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import org.junit.*;

public class QcfGlyphMetricsTest {
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
