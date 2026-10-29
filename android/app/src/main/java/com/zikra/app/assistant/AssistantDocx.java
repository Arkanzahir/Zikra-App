package com.zikra.app.assistant;

import android.util.Xml;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.xmlpull.v1.XmlPullParser;

/** Extracts bounded Word text locally, without opening links or embedded files. */
final class AssistantDocx {
  private static final String WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
  private static final String STRICT_WORD = "http://purl.oclc.org/ooxml/wordprocessingml/main";

  private AssistantDocx() {}

  static String read(byte[] bytes, int maximumText) throws Exception {
    byte[] document = null;
    int total = 0, entries = 0;
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (++entries > 200 || name.contains("..") || name.startsWith("/") || name.contains("\\"))
          throw new IOException("Arsip DOCX tidak valid.");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = zip.read(buffer)) != -1) {
          if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
          total += count;
          if (out.size() + count > 2_000_000 || total > 4_000_000)
            throw new IOException("Isi DOCX terlalu besar.");
          out.write(buffer, 0, count);
        }
        if (name.equals("word/document.xml")) {
          if (document != null) throw new IOException("DOCX tidak valid.");
          document = out.toByteArray();
        }
      }
    }
    if (document == null) throw new IOException("Pilih DOCX yang berisi teks.");

    XmlPullParser parser = Xml.newPullParser();
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
    parser.setInput(new ByteArrayInputStream(document), null);
    StringBuilder out = new StringBuilder();
    int textDepth = -1;
    boolean root = false;
    for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.nextToken()) {
      if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
      if (event == XmlPullParser.DOCDECL) throw new IOException("Isi DOCX tidak didukung.");
      if (event == XmlPullParser.START_TAG) {
        if (parser.getDepth() > 64) throw new IOException("Struktur DOCX terlalu dalam.");
        boolean word = wordNamespace(parser.getNamespace());
        String tag = parser.getName();
        if (parser.getDepth() == 1) {
          if (!word || !tag.equals("document")) throw new IOException("DOCX tidak valid.");
          root = true;
        }
        if (word) {
          if (tag.equals("t")) textDepth = parser.getDepth();
          else if (tag.equals("tab")) out.append('\t');
          else if (tag.equals("br") || tag.equals("cr")) out.append('\n');
        }
      } else if ((event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT
          || event == XmlPullParser.ENTITY_REF || event == XmlPullParser.IGNORABLE_WHITESPACE)
          && textDepth >= 0) {
        if (parser.getText() == null) throw new IOException("Isi DOCX tidak didukung.");
        out.append(parser.getText());
      } else if (event == XmlPullParser.END_TAG) {
        if (wordNamespace(parser.getNamespace())) {
          switch (parser.getName()) {
            case "p": out.append('\n'); break;
            case "tc": trimLast(out, '\n'); out.append('\t'); break;
            case "tr": trimLast(out, '\t'); out.append('\n'); break;
            default: break;
          }
        }
        if (parser.getDepth() == textDepth) textDepth = -1;
      }
      if (out.length() > maximumText + 1) throw new IOException("Teks DOCX maksimal 12.000 karakter.");
    }
    if (!root) throw new IOException("DOCX tidak valid.");
    String value = out.toString().trim();
    if (value.length() > maximumText) throw new IOException("Teks DOCX maksimal 12.000 karakter.");
    return value;
  }

  private static boolean wordNamespace(String namespace) {
    return WORD.equals(namespace) || STRICT_WORD.equals(namespace);
  }

  private static void trimLast(StringBuilder text, char separator) {
    if (text.length() > 0 && text.charAt(text.length() - 1) == separator)
      text.setLength(text.length() - 1);
  }
}
