package com.zikra.app.reading.mushaf;

import static org.junit.Assert.*;

import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.junit.*;

public class MushafDocumentTest {
  private JSONObject word(int id, int position, int page, int line) throws JSONException {
    return new JSONObject()
        .put("id", id)
        .put("position", position)
        .put("page_number", page)
        .put("line_number", line)
        .put("code_v2", "TEST")
        .put("char_type_name", "word");
  }

  private JSONObject response() throws JSONException {
    return new JSONObject()
        .put(
            "pagination",
            new JSONObject()
                .put("current_page", 1)
                .put("total_pages", 1)
                .put("next_page", JSONObject.NULL)
                .put("total_records", 2))
        .put(
            "verses",
            new JSONArray()
                .put(
                    new JSONObject()
                        .put("verse_key", "1:1")
                        .put("juz_number", 1)
                        .put(
                            "words", new JSONArray().put(word(987, 1, 1, 2)).put(word(9, 2, 1, 2))))
                .put(
                    new JSONObject()
                        .put("verse_key", "1:2")
                        .put("juz_number", 1)
                        .put(
                            "words",
                            new JSONArray().put(word(300, 1, 1, 2)).put(word(7, 2, 1, 3)))));
  }

  @Test
  public void nonSequentialDatabaseIdsAndSharedLinesAreSupported() throws Exception {
    MushafDocument data = MushafDocument.parse(response().toString(), 1);
    assertEquals(2, data.page.lines.size());
    assertEquals(3, data.page.lines.get(0).words.size());
    assertEquals(2, data.page.lines.get(0).words.get(2).verse);
  }

  @Test(expected = JSONException.class)
  public void incompletePaginationRejected() throws Exception {
    JSONObject root = response();
    root.getJSONObject("pagination").put("next_page", 2);
    MushafDocument.parse(root.toString(), 1);
  }

  @Test
  public void verseMedallionUsesExplicitEndMetadataNotTheLastWordInALine() throws Exception {
    JSONObject root = response();
    root.getJSONArray("verses")
        .getJSONObject(0)
        .getJSONArray("words")
        .getJSONObject(1)
        .put("char_type_name", "end");
    MushafDocument data = MushafDocument.parse(root.toString(), 1);
    assertTrue(data.page.lines.get(0).words.get(1).verseEnd);
    assertEquals(1, data.page.lines.get(0).words.get(1).verse);
    assertFalse(data.page.lines.get(0).words.get(2).verseEnd);
    assertFalse(data.page.lines.get(1).words.get(0).verseEnd);
  }

  @Test(expected = JSONException.class)
  public void missingPositionRejected() throws Exception {
    JSONObject root = response();
    root.getJSONArray("verses")
        .getJSONObject(0)
        .getJSONArray("words")
        .getJSONObject(1)
        .put("position", 3);
    MushafDocument.parse(root.toString(), 1);
  }

  @Test(expected = JSONException.class)
  public void missingGlyphRejected() throws Exception {
    JSONObject root = response();
    root.getJSONArray("verses")
        .getJSONObject(0)
        .getJSONArray("words")
        .getJSONObject(0)
        .put("code_v2", "");
    MushafDocument.parse(root.toString(), 1);
  }

  @Test
  public void wordsOnAdjacentPageAreFilteredAndStartingPositionIsKept() throws Exception {
    JSONObject root = response();
    root.getJSONArray("verses")
        .getJSONObject(0)
        .getJSONArray("words")
        .getJSONObject(0)
        .put("page_number", 2);
    MushafDocument data = MushafDocument.parse(root.toString(), 1);
    assertEquals(2, data.page.lines.get(0).words.get(0).position);
  }

  @Test(expected = JSONException.class)
  public void reversedLinesRejected() throws Exception {
    JSONObject root = response();
    root.getJSONArray("verses")
        .getJSONObject(0)
        .getJSONArray("words")
        .getJSONObject(0)
        .put("line_number", 4);
    MushafDocument.parse(root.toString(), 1);
  }

  @Test
  public void actualHostSnapshotsParseWithoutBundlingContentInTests() throws Exception {
    String path = System.getenv("ZIKRA_MUSHAF_AUDIT_DIR");
    Assume.assumeTrue("Optional host HTTP snapshots required", path != null);
    int[] pages = {1, 2, 300, 562, 604};
    int[] counts = {36, 41, 140, 143, 73};
    for (int i = 0; i < pages.length; i++) {
      String text =
          new String(
              Files.readAllBytes(Paths.get(path, "page-" + pages[i] + ".json")),
              java.nio.charset.StandardCharsets.UTF_8);
      MushafDocument data = MushafDocument.parse(text, pages[i]);
      assertEquals(pages[i], data.page.number);
      int count = 0;
      for (MushafPage.Line line : data.page.lines) count += line.words.size();
      assertEquals(counts[i], count);
      if (pages[i] == 1) {
        StringJoiner basmalah = new StringJoiner(" ");
        for (MushafPage.Line line : data.page.lines)
          for (MushafPage.Word word : line.words)
            if (word.verse == 1 && !word.verseEnd) basmalah.add(word.glyph);
        assertEquals(BasmalahRenderer.QCF_BASMALAH, basmalah.toString());
      }
      if (pages[i] == 562) assertTrue(data.page.contains(67, 1));
      if (pages[i] == 604) assertTrue(data.page.contains(114, 6));
    }
  }
}
