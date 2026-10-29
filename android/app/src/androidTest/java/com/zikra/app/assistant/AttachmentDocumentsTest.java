package com.zikra.app.assistant;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.*;

/** Synthetic files only; requires the dedicated emulator and a host backup. */
@SuppressWarnings("deprecation")
public final class AttachmentDocumentsTest extends InstrumentationTestCase {
  private Context context;
  private static final String WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

  @Override protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))) {
      assertEquals("Emulator only", "1", new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
    }
    assertTrue("Host backup required", new File(context.getFilesDir(), "attachment-c07-authorized").isFile());
  }

  private byte[] zip(String xml, boolean utf16) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("word/document.xml"));
      zip.write(xml.getBytes(utf16 ? StandardCharsets.UTF_16 : StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return out.toByteArray();
  }

  private String document(String body) {
    return "<w:document xmlns:w='" + WORD + "'><w:body>" + body + "</w:body></w:document>";
  }

  public void testWordSeparatorsRunsEntitiesAndTables() throws Exception {
    String body = "<w:p><w:r><w:t>Belajar</w:t></w:r><w:r><w:t xml:space='preserve'> matematika &amp; fisika</w:t>"
        + "<w:tab/><w:t>Senin</w:t><w:br/><w:t>09.00</w:t><w:cr/><w:t>Selesai</w:t></w:r></w:p>"
        + "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>12</w:t></w:r></w:p></w:tc>"
        + "<w:tc><w:p><w:r><w:t>34</w:t></w:r></w:p></w:tc></w:tr>"
        + "<w:tr><w:tc><w:p><w:r><w:t>56</w:t></w:r></w:p></w:tc>"
        + "<w:tc><w:p><w:r><w:t>78</w:t></w:r></w:p></w:tc></w:tr></w:tbl>";
    assertEquals("Belajar matematika & fisika\tSenin\n09.00\nSelesai\n12\t34\n56\t78",
        AssistantDocx.read(zip(document(body), false), 12000));
  }

  public void testWordUtf16StrictNamespaceAndForeignText() throws Exception {
    String body = "<w:p><w:r><w:t>حفظ القرآن — Tugas</w:t></w:r>"
        + "<x:t xmlns:x='urn:foreign'>JANGAN BACA</x:t></w:p>";
    String xml = "<?xml version='1.0' encoding='UTF-16'?>" + document(body);
    assertEquals("حفظ القرآن — Tugas", AssistantDocx.read(zip(xml, true), 12000));
    assertEquals("حفظ القرآن — Tugas", AssistantDocx.read(zip(document(body)
        .replace(WORD, "http://purl.oclc.org/ooxml/wordprocessingml/main"), false), 12000));
  }

  public void testRejectDoctypeEntitiesNamespaceAndExpansion() throws Exception {
    String[] invalid = {
        "<!DOCTYPE w:document [<!ENTITY secret SYSTEM 'file:///data/secret'>]>"
            + document("<w:p><w:r><w:t>&secret;</w:t></w:r></w:p>"),
        "<!DOCTYPE w:document [<!ENTITY a 'EXPAND'>]>" + document("<w:p><w:r><w:t>&a;</w:t></w:r></w:p>"),
        "<document><p><t>fake Word</t></p></document>",
        document("<w:p><w:r><w:t>" + "x".repeat(12001) + "</w:t></w:r></w:p>"),
        document("<w:p>" + "<w:r>".repeat(65) + "<w:t>x</w:t>" + "</w:r>".repeat(65) + "</w:p>"),
        document("<w:p><w:r><w:t>&undefined;</w:t></w:r></w:p>")
    };
    for (String xml : invalid) for (boolean utf16 : new boolean[]{false, true}) {
      try { AssistantDocx.read(zip(xml, utf16), 12000); fail("Unsafe Word accepted"); }
      catch (IOException | org.xmlpull.v1.XmlPullParserException expected) { }
    }
    assertEquals(12000, AssistantDocx.read(zip(document("<w:p><w:r><w:t>"
        + "x".repeat(12000) + "</w:t></w:r></w:p>"), false), 12000).length());
  }

  private AssistantAttachments.Draft restore(AssistantAttachments reader, String chat) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<AssistantAttachments.Draft> draft = new AtomicReference<>();
    AtomicReference<String> error = new AtomicReference<>();
    reader.restore(chat, new AssistantAttachments.Result() {
      public void loaded(AssistantAttachments.Draft value) { draft.set(value); latch.countDown(); }
      public void failed(String message) { error.set(message); latch.countDown(); }
    });
    assertTrue(latch.await(8, TimeUnit.SECONDS)); assertNull(error.get()); return draft.get();
  }

  public void testFailedAndCancelledImportKeepExistingDraft() throws Exception {
    String chat = UUID.randomUUID().toString();
    try (AssistantAttachments reader = new AssistantAttachments(context)) {
      Method decode = AssistantAttachments.class.getDeclaredMethod("decode", String.class, String.class, byte[].class);
      decode.setAccessible(true);
      AssistantAttachments.Draft original = (AssistantAttachments.Draft) decode.invoke(reader,
          "tugas.txt", "text/plain", "Draf sintetis tetap".getBytes(StandardCharsets.UTF_8));
      Method store = AssistantAttachments.class.getDeclaredMethod("store", String.class, AssistantAttachments.Draft.class);
      store.setAccessible(true); store.invoke(reader, chat, original);
      Uri invalid = AssistantCameraProvider.create(context);
      try (OutputStream out = context.getContentResolver().openOutputStream(invalid)) { out.write(new byte[]{1,2,3}); }
      CountDownLatch failed = new CountDownLatch(1);
      reader.load(chat, invalid, new AssistantAttachments.Result() {
        public void loaded(AssistantAttachments.Draft value) { fail("Invalid image accepted"); }
        public void failed(String message) { failed.countDown(); }
      });
      assertTrue(failed.await(8, TimeUnit.SECONDS));
      assertEquals(original.id, restore(reader, chat).id);
      Field field = AssistantAttachments.class.getDeclaredField("worker"); field.setAccessible(true);
      ExecutorService worker = (ExecutorService) field.get(reader);
      CountDownLatch release = new CountDownLatch(1), blocked = new CountDownLatch(1);
      worker.execute(() -> { blocked.countDown(); try { release.await(8, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
      assertTrue(blocked.await(3, TimeUnit.SECONDS));
      Uri cancelled = AssistantCameraProvider.create(context);
      android.graphics.Bitmap image = android.graphics.Bitmap.createBitmap(20, 30,
          android.graphics.Bitmap.Config.ARGB_8888);
      image.eraseColor(android.graphics.Color.WHITE);
      try (OutputStream out = context.getContentResolver().openOutputStream(cancelled)) {
        assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out));
      } finally { image.recycle(); }
      try {
        reader.load(chat, cancelled, new AssistantAttachments.Result() {
          public void loaded(AssistantAttachments.Draft value) { fail("Cancelled callback"); }
          public void failed(String message) { fail("Cancelled callback"); }
        });
        reader.cancel();
      } finally { release.countDown(); }
      assertEquals(original.id, restore(reader, chat).id);
      for (Uri uri : new Uri[]{invalid, cancelled}) {
        try (InputStream ignored = context.getContentResolver().openInputStream(uri)) { fail("Camera temporary retained"); }
        catch (FileNotFoundException expected) { }
      }
      reader.deleteConversation(chat);
      worker.submit(() -> {}).get(5, TimeUnit.SECONDS);
      assertFalse(new File(context.getFilesDir(), "assistant_attachments/" + chat).exists());
    }
  }
}
