package com.zikra.app.assistant;

import android.graphics.pdf.PdfDocument;
import android.test.InstrumentationTestCase;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;

/** No provider/network calls; synthetic PDFs in a disposable emulator only. */
@SuppressWarnings("deprecation")
public final class AssistantTempFilesNativeTest extends InstrumentationTestCase {
  @Override protected void setUp() throws Exception {
    super.setUp();
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
  }
  private byte[] pdf(int pages) throws Exception {
    PdfDocument document = new PdfDocument();
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      for (int i = 0; i < pages; i++) {
        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(120, 180, i + 1).create());
        page.getCanvas().drawColor(android.graphics.Color.WHITE);
        document.finishPage(page);
      }
      document.writeTo(output); return output.toByteArray();
    } finally { document.close(); }
  }
  private int rendererCopies(File cache) {
    String[] entries = cache.list((dir, name) -> name.matches("assistant-pdf--?[0-9]+\\.pdf"));
    assertNotNull(entries); return entries.length;
  }
  public void testPdfSuccessAndRejectionLeaveNoRawCopy() throws Exception {
    android.content.Context context = getInstrumentation().getTargetContext();
    int initial = rendererCopies(context.getCacheDir());
    Method decode = AssistantAttachments.class.getDeclaredMethod("decode", String.class, String.class, byte[].class);
    decode.setAccessible(true);
    try (AssistantAttachments attachments = new AssistantAttachments(context)) {
      AssistantAttachments.Draft draft = (AssistantAttachments.Draft) decode.invoke(attachments,
          "synthetic.pdf", "application/pdf", pdf(2));
      assertEquals(2, draft.pages); assertEquals(2, draft.imageCount());
      assertEquals(initial, rendererCopies(context.getCacheDir()));
      try { decode.invoke(attachments, "oversize.pdf", "application/pdf", pdf(4)); fail("Four pages accepted"); }
      catch (InvocationTargetException expected) {
        assertTrue(expected.getCause() instanceof java.io.IOException);
      }
      assertEquals(initial, rendererCopies(context.getCacheDir()));
    }
  }
  public void testSymlinkCannotDeleteOriginalDocument() throws Exception {
    File root = new File(getInstrumentation().getTargetContext().getCacheDir(), "temp-cleanup-native");
    assertTrue(root.mkdir());
    File original = new File(root, "original.pdf"), link = new File(root, "assistant-pdf-123.pdf");
    try {
      Files.write(original.toPath(), new byte[]{1, 2, 3});
      assertTrue(original.setLastModified(1));
      Files.createSymbolicLink(link.toPath(), original.toPath());
      AssistantTempFiles.pruneStalePdfs(root, System.currentTimeMillis());
      assertTrue(Files.isSymbolicLink(link.toPath()));
      assertEquals(3, Files.readAllBytes(original.toPath()).length);
    } finally {
      Files.deleteIfExists(link.toPath()); Files.deleteIfExists(original.toPath());
      assertTrue(root.delete());
    }
  }
}
