package com.zikra.app.assistant;

import static org.junit.Assert.*;

import org.junit.Test;

public final class OfflineAssistantTest {
  @Test
  public void hifzGuidanceNamesTheRealDestination() {
    String reply = OfflineAssistant.reply("Bantu target hafalan", 0, 0, "Al-Mulk");
    assertTrue(reply.contains("Diary Hifz"));
    assertFalse(reply.contains("kategori Hifz pada Study Hub"));
    assertTrue(reply.contains("Al-Mulk"));
  }

  @Test
  public void greetingsDoNotInventAUserIdentity() {
    String reply = OfflineAssistant.reply("halo", 0, 0, "Al-Fatihah");
    assertFalse(reply.contains("Arkan"));
    assertTrue(reply.contains("offline"));
  }

  @Test
  public void localRepliesStateTheirLimitsAndDoNotChangeChecklist() {
    String reply = OfflineAssistant.reply("lihat progres checklist", 2, 8, "Al-Fatihah");
    assertTrue(reply.contains("2 dari 8"));
    assertTrue(reply.contains("tidak otomatis mengubah checklist"));
    assertTrue(
        OfflineAssistant.reply("jelaskan persamaan kuadrat", 0, 0, "Al-Fatihah")
            .contains("belum bisa menjawab soal khusus"));
  }

  @Test
  public void emptyPlannerIsNotReportedAsCompletedWork() {
    String reply = OfflineAssistant.reply("lihat progres", 0, 0, "Al-Fatihah");
    assertTrue(reply.contains("Belum ada tugas akademik"));
    assertFalse(reply.contains("Semua target hari ini sudah selesai"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyMessageIsRejected() {
    OfflineAssistant.reply("  ", 0, 0, "Al-Fatihah");
  }

  @Test(expected = IllegalArgumentException.class)
  public void oversizedMessageIsRejected() {
    OfflineAssistant.reply(new String(new char[501]).replace('\0', 'a'), 0, 0, "Al-Fatihah");
  }
}
