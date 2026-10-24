package com.zikra.app.discovery;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.junit.Test;

public class DuaSelectionTest {
  private String resource() throws Exception {
    return new String(Files.readAllBytes(Paths.get("src/main/res/raw/dua_selected.json")), StandardCharsets.UTF_8);
  }
  private DuaSelection.Entry named(List<DuaSelection.Entry> entries, String id) {
    return entries.stream().filter(e -> e.id.equals("daily:" + id)).findFirst().orElseThrow(AssertionError::new);
  }

  @Test public void bundledSelectionCoversRequiredContextsAndKeepsLegacyPositions() throws Exception {
    List<DuaSelection.Entry> entries = DuaSelection.parse(resource());
    assertEquals(14, entries.size());
    assertEquals("daily:study", entries.get(0).id); assertEquals("daily:rain", entries.get(1).id);
    assertEquals("daily:goodness", entries.get(2).id); assertEquals("daily:calamity", entries.get(3).id);
    Set<Integer> categories = new HashSet<>();
    for (DuaSelection.Entry entry : entries) {
      categories.add(entry.category); assertTrue(entry.sourceText().contains(entry.sourceUrl));
      assertFalse(entry.latin.trim().isEmpty());
      assertTrue(entry.arabic.codePoints().anyMatch(cp -> cp >= 0x0621 && cp <= 0x064A));
    }
    for (int category = 1; category < DuaSelection.CATEGORIES.size(); category++)
      assertTrue("Missing category " + category, categories.contains(category));
    assertTrue(named(entries, "study").matches("رب زدني", 2));
    assertFalse(named(entries, "study").matches("رب زدني", 8));
    assertTrue(named(entries, "after-meal").matches("3458", 4));
    assertTrue(named(entries, "wake").arabic.endsWith("النُّشُورُ"));
  }

  @Test public void morningEveningVariantsAndCompanionThunderAttributionStayDistinct() throws Exception {
    List<DuaSelection.Entry> entries = DuaSelection.parse(resource());
    assertTrue(named(entries, "morning").arabic.endsWith("النُّشُورُ"));
    assertTrue(named(entries, "evening").arabic.endsWith("الْمَصِيرُ"));
    assertTrue(named(entries, "evening").note.contains("variasi"));
    DuaSelection.Entry thunder = named(entries, "thunder");
    assertEquals("athar", thunder.kind); assertTrue(thunder.reference.contains("az-Zubair"));
    assertTrue(thunder.note.contains("bukan sabda Nabi"));
    assertEquals(1, entries.stream().filter(e -> e.matches("petir", 0)).count());
    assertEquals(2, entries.stream().filter(e -> e.matches("", 9)).count());
    assertEquals(0, entries.stream().filter(e -> e.matches("absent-doa-918zz", 0)).count());
  }

  @Test public void duplicatedIdentityAndMissingSourceCannotEnterBundledSelection() throws Exception {
    JSONObject root = new JSONObject(resource());
    root.getJSONArray("entries").getJSONObject(1).put("id", "daily:study");
    try { DuaSelection.parse(root.toString()); fail("Duplicate ID accepted"); }
    catch (org.json.JSONException expected) { }
    root = new JSONObject(resource());
    root.getJSONArray("entries").getJSONObject(0).put("sourceUrl", "");
    try { DuaSelection.parse(root.toString()); fail("Missing source accepted"); }
    catch (org.json.JSONException expected) { }
  }
}
