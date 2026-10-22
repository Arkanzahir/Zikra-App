package com.zikra.app.reading;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class ArabicWordMapTest {
  @Test
  public void matchesSourceSequenceWithDiacritics() {
    String verse = "قُلْ هُوَ ٱللَّهُ أَحَدٌ";
    ArabicWordMap map = ArabicWordMap.match(verse, List.of("قُلْ", "هُوَ", "ٱللَّهُ", "أَحَدٌ"));
    assertNotNull(map);
    assertEquals(4, map.starts.length);
    assertEquals("هُوَ", verse.substring(map.starts[1], map.ends[1]));
  }

  @Test
  public void matchingCountDoesNotAllowWrongWords() {
    assertNull(ArabicWordMap.match("قل هو الله أحد", List.of("قل", "هو", "الله", "الصمد")));
  }

  @Test
  public void ignoresStandaloneWaqfAndAyahMarkers() {
    ArabicWordMap map =
        ArabicWordMap.match("ٱللَّهُ ۖ ٱلصَّمَدُ ۝٢", List.of("ٱللَّهُ", "ٱلصَّمَدُ"));
    assertNotNull(map);
    assertEquals(2, map.starts.length);
  }

  @Test
  public void missingOrAdditionalSourceWordsRejectKaraoke() {
    assertNull(ArabicWordMap.match("الله الصمد", List.of("الله")));
    assertNull(ArabicWordMap.match("الله", List.of("الله", "الصمد")));
    assertNull(ArabicWordMap.match("الله", List.of("")));
    assertNull(ArabicWordMap.match("الله", List.of()));
  }

  @Test
  public void alifWaslaAndTatweelCanMapWithoutChangingDisplayText() {
    String text = "اللّـٰه الصَّمَد";
    ArabicWordMap map = ArabicWordMap.match(text, List.of("ٱللَّهُ", "ٱلصَّمَدُ"));
    assertNotNull(map);
    assertEquals("اللّـٰه", text.substring(map.starts[0], map.ends[0]));
  }

  @Test
  public void daggerAlifMatchesFullAlifWithoutChangingOffsetsOrDroppingLetters() {
    String verse = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ";
    ArabicWordMap map =
        ArabicWordMap.match(verse, List.of("ٱلْحَمْدُ", "لِلَّهِ", "رَبِّ", "ٱلْعَـٰلَمِينَ"));
    assertNotNull(map);
    assertEquals("الْعَالَمِينَ", verse.substring(map.starts[3], map.ends[3]));
    assertNull(ArabicWordMap.match("العالمين", List.of("العلمين")));
    assertNull(ArabicWordMap.match("العالمين", List.of("الظَّـٰلَمِينَ")));
  }
}
