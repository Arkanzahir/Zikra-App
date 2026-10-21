package com.zikra.app.audio;

import static org.junit.Assert.*;

import com.zikra.app.reading.mushaf.WordTimeline;
import org.junit.Test;

public final class TimedRecitationTest {
  private static final String HASH =
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static final String WORDS =
      "{\"verse\":{\"verse_key\":\"1:1\",\"words\":["
          + "{\"position\":1,\"char_type_name\":\"word\"},"
          + "{\"position\":2,\"char_type_name\":\"word\"},"
          + "{\"position\":3,\"char_type_name\":\"end\"}]}}";

  private TimedRecitation parse(String segments) throws Exception {
    return new TimedRecitation(
        1,
        1,
        "{\"audio_files\":[{\"verse_key\":\"1:1\","
            + "\"url\":\"Alafasy/mp3/001001.mp3\",\"duration\":2,\"segments\":"
            + segments
            + "}]}",
        WORDS);
  }

  @Test
  public void matchesExplicitRecitationIdsAndAudioOrigins() {
    assertEquals(7, TimedRecitation.id("ar.alafasy"));
    assertEquals(6, TimedRecitation.id("ar.husary"));
    assertEquals(9, TimedRecitation.id("ar.minshawi"));
    assertEquals(
        "https://verses.quran.foundation/Alafasy/mp3/001001.mp3",
        TimedRecitation.audioUrl("Alafasy/mp3/001001.mp3"));
    assertEquals(
        "https://mirrors.quranicaudio.com/everyayah/a.mp3",
        TimedRecitation.audioUrl("//mirrors.quranicaudio.com/everyayah/a.mp3"));
  }

  @Test
  public void excludesMedallionAndUsesNativePositionAndRecordingHash() throws Exception {
    TimedRecitation data = parse("[[0,1,100,500],[1,2,700,1600]]");
    assertEquals(2, data.wordCount);
    WordTimeline line = data.timeline(HASH, 2100);
    assertNotNull(line);
    assertEquals(1, line.at(1, 1, HASH, 300, false, true).word);
    assertEquals(0, line.at(1, 1, HASH, 600, false, true).word);
    assertEquals(2, line.at(1, 1, HASH, 1100, false, true).word);
    assertEquals(0, line.at(1, 1, HASH.replace('a', 'b'), 1100, false, true).word);
  }

  @Test
  public void incompleteRepeatedOverlappingAndUnknownOffsetsDisableWords() throws Exception {
    for (String rows :
        new String[] {
          "null",
          "[]",
          "[[0,1,100,500]]",
          "[[0,1,100,800],[1,2,700,1600]]",
          "[[0,1,100,500],[1,1,700,1600]]",
          "[[1,100,500],[2,700,1600]]"
        }) assertNull(parse(rows).timeline(HASH, 2000));
  }

  @Test
  public void durationMismatchDoesNotRescaleOffsets() throws Exception {
    TimedRecitation data = parse("[[0,1,100,500],[1,2,700,1600]]");
    assertNull(data.timeline(HASH, 8000));
    assertNull(data.timeline(HASH, 1200));
  }

  @Test(expected = org.json.JSONException.class)
  public void rejectsWrongAyah() throws Exception {
    new TimedRecitation(1, 2, "{\"audio_files\":[{\"verse_key\":\"1:1\"}]}", WORDS);
  }

  @Test(expected = org.json.JSONException.class)
  public void rejectsSkippedWordPosition() throws Exception {
    new TimedRecitation(
        1,
        1,
        "{\"audio_files\":[{\"verse_key\":\"1:1\",\"url\":\"Alafasy/a.mp3\",\"duration\":2}]}",
        WORDS.replace("\"position\":2", "\"position\":4"));
  }

  @Test
  public void rejectsUnrecognizedAudioOrigins() {
    for (String value :
        new String[] {
          "http://verses.quran.foundation/a.mp3",
          "https://example.com/a.mp3",
          "https://verses.quran.foundation.evil.test/a.mp3"
        }) {
      try {
        TimedRecitation.audioUrl(value);
        fail(value);
      } catch (IllegalArgumentException expected) {
      }
    }
  }
}
