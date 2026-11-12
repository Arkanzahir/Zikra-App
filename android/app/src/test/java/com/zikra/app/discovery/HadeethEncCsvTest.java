package com.zikra.app.discovery;

import static org.junit.Assert.*;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;

public class HadeethEncCsvTest {
  private static final String METADATA = "# Language: Indonesia\r\n# Source: https://hadeethenc.com/id\r\n"
      + "# Last update: 2026-05-10 17:47:10 (v1.22.0)\r\n# Check for updates: " + HadeethEncCsv.UPDATE_URL
      + "\r\n# PLEASE DON'T REMOVE THIS IMPORTANT INFORMATION!";
  private static final String HEADER = "id,title_ar,title,hadith_text_ar,hadith_text,explanation_ar,explanation,benefits_ar,benefits,grade_ar,takhrij_ar,grade,takhrij,lang,link\r\n";

  private static String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
  private static String row(int id) {
    String[] cells = {"" + id, "عنوان تجريبي", "Data uji, bukan hadis", "نص تجريبي", "Teks uji \"kutip\"\r\nbaris kedua",
        "", "Uraian uji", "", "1- Faedah uji\r\n2- Baris kedua", "", "", "[Label uji]", "[Atribusi uji]", "id", HadeethEncCsv.sourceUrl(id)};
    StringBuilder value = new StringBuilder();
    for (int i = 0; i < cells.length; i++) { if (i > 0) value.append(','); value.append(quote(cells[i])); }
    return value.append("\r\n").toString();
  }
  private static String preamble() { return "\ufeff" + quote(METADATA) + ",,,,,,,,,,,,,,\r\n" + HEADER; }
  private static String fixture() {
    StringBuilder csv = new StringBuilder(preamble());
    for (int i = 0; i < HadeethEncCsv.COUNT; i++) csv.append(row(i == HadeethEncCsv.COUNT - 1 ? 66541 : 1751 + i));
    return csv.toString();
  }
  private static void rejected(String csv) throws Exception {
    try { HadeethEncCsv.parse(new StringReader(csv), record -> {}); fail("Invalid source accepted"); }
    catch (java.io.IOException expected) { }
  }

  @Test public void quotedMultilineBomRoundTripAndDistinctIdentity() throws Exception {
    List<HadeethEncCsv.Record> records = new ArrayList<>();
    HadeethEncCsv.parse(new StringReader(fixture()), records::add);
    assertEquals(2260, records.size());
    HadeethEncCsv.Record first = records.get(0);
    assertEquals("hadeethenc:id:1751", first.id());
    assertEquals("Data uji, bukan hadis", first.cell(2));
    assertEquals("Teks uji \"kutip\"\r\nbaris kedua", first.cell(4));
    assertEquals(METADATA, first.metadata);
    HadeethEncCsv.Record copy = HadeethEncCsv.Record.fromJson(first.json());
    for (int i = 0; i < 15; i++) assertEquals(first.cell(i), copy.cell(i));
    assertEquals("hadeethenc", HadithCollection.bookForId(first.id()));
    assertEquals("bukhari", HadithCollection.bookForId("tafsirq-v1:bukhari:1751"));
    assertEquals(first.id(), HadithCollection.encyclopedia(first).id);
  }
  @Test public void wrongVersionForeignLinkLanguageAndDuplicateIdsAreRejected() throws Exception {
    String good = fixture();
    rejected(good.replace("v1.22.0", "v1.21.0"));
    rejected(good.replace("# Source: https://hadeethenc.com/id", "# Source: https://hadeethenc.com/id.evil.example"));
    rejected(good.replace(HadeethEncCsv.sourceUrl(1751), "https://evil.example/1751"));
    rejected(good.replace("\"id\",\"https://", "\"en\",\"https://"));
    rejected(preamble() + row(1751) + row(1751));
  }
  @Test public void truncationMalformedQuotesAndOversizedFieldsAreRejected() throws Exception {
    rejected(preamble() + row(1751));
    rejected(preamble() + "\"1751");
    rejected(preamble() + "\"1751\"unexpected,");
    rejected(preamble() + row(1751).replace("Data uji, bukan hadis", "x".repeat(200001)));
    rejected(preamble() + row(1751).replace("Data uji, bukan hadis", "bad\0text"));
  }
  @Test public void sinkCancellationStopsWithoutSilentlyCompleting() throws Exception {
    int[] seen = {0};
    try {
      HadeethEncCsv.parse(new StringReader(fixture()), record -> {
        if (++seen[0] == 4) throw new InterruptedException("cancelled fixture");
      });
      fail("Cancellation ignored");
    } catch (InterruptedException expected) { assertEquals(4, seen[0]); }
  }
  @Test public void auditedOfficialExportMatchesPinnedHashAndPreservesEveryRecord() throws Exception {
    String selected = System.getenv("ZIKRA_HADEETHENC_FIXTURE");
    Assume.assumeTrue("Official audit snapshot supplied explicitly", selected != null);
    Path path = Path.of(selected); assertEquals(HadeethEncCsv.BYTES, Files.size(path));
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (java.io.InputStream input = Files.newInputStream(path)) {
      byte[] buffer = new byte[8192]; int count;
      while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
    }
    StringBuilder hash = new StringBuilder();
    for (byte value : digest.digest()) hash.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
    assertEquals(HadeethEncCsv.SHA256, hash.toString());
    int[] count = {0};
    try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      HadeethEncCsv.parse(reader, record -> {
        HadeethEncCsv.Record decoded = HadeethEncCsv.Record.fromJson(record.json());
        assertEquals(record.metadata, decoded.metadata);
        for (int i = 0; i < 15; i++) assertEquals(record.cell(i), decoded.cell(i));
        count[0]++;
      });
    }
    assertEquals(HadeethEncCsv.COUNT, count[0]);
  }
}
