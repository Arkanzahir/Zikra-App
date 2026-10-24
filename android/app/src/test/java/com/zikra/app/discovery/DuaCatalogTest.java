package com.zikra.app.discovery;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class DuaCatalogTest {
  private JSONObject row(int id, String reference) throws Exception {
    return new JSONObject().put("id", id).put("nama", "Fixture doa").put("grup", "Fixture")
        .put("ar", "عِلْمًا").put("tr", "fixture").put("idn", "Arti fixture")
        .put("tentang", reference).put("tag", new JSONArray().put("Belajar"));
  }
  @Test public void excludesUnreferencedRowsAndPreservesSourceIdentity() throws Exception {
    DuaCatalog value = DuaCatalog.parse(new JSONArray().put(row(9, "Rujukan fixture"))
        .put(row(14, "")).toString());
    assertEquals(2, value.sourceCount); assertEquals(1, value.excludedWithoutReference);
    assertEquals(1, value.entries.size()); assertEquals("equran-doa:9", value.entries.get(0).id);
    assertTrue(value.entries.get(0).search.contains("علما"));
    assertTrue(value.entries.get(0).search.contains("belajar"));
    assertEquals("عِلْمًا", value.entries.get(0).arabic);
  }
  @Test public void excludesAnEntryWhoseTranslationIsMissing() throws Exception {
    DuaCatalog value = DuaCatalog.parse(new JSONArray().put(row(1, "ref"))
        .put(row(2, "ref").put("idn", "")).toString());
    assertEquals(1, value.entries.size()); assertEquals(1, value.excludedWithoutText);
  }
  @Test public void rejectsDuplicatesAndWrongIdTypes() throws Exception {
    reject(new JSONArray().put(row(1, "ref")).put(row(1, "ref")).toString());
    reject(new JSONArray().put(row(1, "ref").put("id", "1")).toString());
    reject(new JSONArray().put(row(1, "ref").put("id", 1.5)).toString());
  }
  @Test public void rejectsPartialAndTrailingPayloadInsteadOfReplacingGoodCache() throws Exception {
    reject("[{\"id\":1}]");
    reject(new JSONArray().put(row(1, "ref")).toString() + " ignored");
    reject(new JSONArray().put(row(1, "")).toString());
  }
  private void reject(String text) {
    try { DuaCatalog.parse(text); fail("Malformed source accepted"); } catch (JSONException expected) { }
  }
}
