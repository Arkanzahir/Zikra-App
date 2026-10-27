package com.zikra.app;

import android.content.Context;
import android.test.InstrumentationTestCase;
import com.zikra.app.audio.Recitation;
import com.zikra.app.reading.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.*;

/** Host-supplied public snapshots; never part of the APK or user download package. */
@SuppressWarnings("deprecation")
public final class QuranCorpusAuditTest extends InstrumentationTestCase {
  public void testAll114SurahsPassProductionParserAnd6236GlobalAudioKeys() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    File folder = new File(context.getExternalFilesDir(null), "validation/d03-corpus");
    assertTrue("Explicit host audit snapshots required", folder.isDirectory());
    int total = 0, pages = 0;
    for (QuranChapters.Chapter chapter : QuranChapters.all(context)) {
      assertEquals(chapter.count, Recitation.count(chapter.id));
      for (int page = 1; page <= (chapter.count + 49) / 50; page++) {
        String json =
            new String(
                Files.readAllBytes(
                    new File(folder, "chapter-" + chapter.id + "-page-" + page + ".json").toPath()),
                StandardCharsets.UTF_8);
        QuranPage parsed = QuranPage.parse(json, chapter, page);
        for (QuranPage.Verse verse : parsed.verses) {
          total++;
          assertEquals(total, Recitation.globalVerse(chapter.id, verse.number));
          for (String reciter : Recitation.IDS)
            assertTrue(
                Recitation.url(reciter, chapter.id, verse.number).endsWith("/" + total + ".mp3"));
        }
        pages++;
      }
    }
    assertEquals(6236, total);
    assertTrue(pages >= 114);
  }
}
