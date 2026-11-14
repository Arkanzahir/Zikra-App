package com.zikra.app.assistant;

import java.io.File;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public final class AssistantTempFilesTest {
  @Rule public final TemporaryFolder folder = new TemporaryFolder();
  private final long now = 1_800_000_000_000L;
  private File entry(String name, long time) throws Exception {
    File file = folder.newFile(name);
    assertTrue(file.setLastModified(time));
    return file;
  }
  @Test public void normalCloseRemovesRendererCopy() throws Exception {
    File file;
    try (AssistantTempFiles.Pdf pdf = AssistantTempFiles.createPdf(folder.getRoot())) {
      file = pdf.file; assertTrue(file.isFile());
    }
    assertFalse(file.exists());
  }
  @Test public void onlyAbandonedOwnedCopiesExpire() throws Exception {
    File stale = entry("assistant-pdf-123.pdf", now - AssistantTempFiles.MAX_AGE_MS - 1);
    File negative = entry("assistant-pdf--456.pdf", stale.lastModified());
    File recent = entry("assistant-pdf-789.pdf", now - 100);
    File boundary = entry("assistant-pdf-100.pdf", now - AssistantTempFiles.MAX_AGE_MS);
    File future = entry("assistant-pdf-101.pdf", now + 100);
    File foreign = entry("my-original.pdf", stale.lastModified());
    File malformed = entry("assistant-pdf-original.pdf", stale.lastModified());
    AssistantTempFiles.pruneStalePdfs(folder.getRoot(), now);
    assertFalse(stale.exists()); assertFalse(negative.exists());
    for (File file : new File[]{recent, boundary, future, foreign, malformed}) assertTrue(file.exists());
  }
  @Test public void activeRendererIsProtectedEvenWithOldTimestamp() throws Exception {
    try (AssistantTempFiles.Pdf pdf = AssistantTempFiles.createPdf(folder.getRoot())) {
      assertTrue(pdf.file.setLastModified(now - AssistantTempFiles.MAX_AGE_MS - 1));
      AssistantTempFiles.pruneStalePdfs(folder.getRoot(), now);
      assertTrue(pdf.file.exists());
    }
  }
  @Test public void matchingDirectoryAndItsContentsAreNeverTraversed() throws Exception {
    File directory = folder.newFolder("assistant-pdf-123.pdf");
    File child = new File(directory, "assistant-pdf-456.pdf");
    Files.write(child.toPath(), new byte[]{1});
    assertTrue(directory.setLastModified(1)); assertTrue(child.setLastModified(1));
    AssistantTempFiles.pruneStalePdfs(folder.getRoot(), now);
    assertTrue(child.exists());
  }
  @Test public void missingCacheAndInvalidClockAreHarmless() throws Exception {
    File stale = entry("assistant-pdf-1.pdf", 1);
    AssistantTempFiles.pruneStalePdfs(null, now);
    AssistantTempFiles.pruneStalePdfs(new File(folder.getRoot(), "missing"), now);
    AssistantTempFiles.pruneStalePdfs(folder.getRoot(), 0);
    assertTrue(stale.exists());
  }
}
