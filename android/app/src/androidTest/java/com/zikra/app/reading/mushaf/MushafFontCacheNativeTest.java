package com.zikra.app.reading.mushaf;

import android.test.InstrumentationTestCase;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;
import java.util.Arrays;

@SuppressWarnings("deprecation")
public final class MushafFontCacheNativeTest extends InstrumentationTestCase {
  public void testValidatedPublishOfflineRetentionAndFailureRollback() throws Exception {
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    File files = getInstrumentation().getTargetContext().getFilesDir();
    byte[] source = Files.readAllBytes(new File(files, "final-fixtures/p1.ttf").toPath());
    File root = new File(files, "font-test-" + UUID.randomUUID()); assertTrue(root.mkdir());
    try {
      File font = new File(root, "p1.ttf");
      MushafFontCache.load(font, () -> source, QcfGlyphMetrics::new);
      assertTrue(Arrays.equals(source, Files.readAllBytes(font.toPath())));
      assertFalse(new BasmalahRenderer(font).inkBounds().isEmpty());
      MushafFontCache.load(font, () -> { throw new AssertionError("Fresh cache must not fetch"); }, QcfGlyphMetrics::new);
      assertTrue(font.setLastModified(System.currentTimeMillis() - 8L * 86400000));
      long oldTime = font.lastModified();
      MushafFontCache.load(font, () -> { throw new IOException("offline"); }, QcfGlyphMetrics::new);
      assertEquals(oldTime, font.lastModified());
      boolean invalidRejected = false;
      try { MushafFontCache.load(font, () -> new byte[]{1, 2, 3}, QcfGlyphMetrics::new); }
      catch (IOException expected) { invalidRejected = true; }
      assertTrue(invalidRejected); assertEquals(oldTime, font.lastModified());
      boolean denied = false;
      try { MushafFontCache.load(font, () -> { throw new MushafFontCache.AccessDenied(); }, QcfGlyphMetrics::new); }
      catch (MushafFontCache.AccessDenied expected) { denied = true; }
      assertTrue(denied);
      File blocked = new File(root, "p1.ttf.new"); assertTrue(blocked.mkdir());
      Files.write(new File(blocked, "owned-blocker").toPath(), new byte[]{1});
      MushafFontCache.load(font, () -> source, QcfGlyphMetrics::new);
      assertEquals(oldTime, font.lastModified());
      assertTrue(Arrays.equals(source, Files.readAllBytes(font.toPath())));
      for (File file : root.listFiles()) assertFalse(file.getName().endsWith(".pending"));
    } finally { erase(root); }
  }
  private void erase(File file) throws IOException {
    if (file.isDirectory()) for (File child : file.listFiles()) erase(child);
    if (!file.delete()) throw new IOException("Owned test cleanup failed");
  }
}
