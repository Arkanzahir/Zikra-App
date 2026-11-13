package com.zikra.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;
import com.zikra.app.assistant.ConversationStore;
import com.zikra.app.data.*;
import java.io.File;
import org.json.*;

/** Run seed on 1.9.41, install -r the current candidate, then verify the same disposable data. */
@SuppressWarnings("deprecation")
public final class BetaUpgradeDataTest extends InstrumentationTestCase {
  private Context context;
  @Override protected void setUp() throws Exception {
    super.setUp(); context = getInstrumentation().getTargetContext();
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    assertTrue(new File(context.getFilesDir(), "upgrade-emulator-authorized").isFile());
  }
  public void testSeedOnPreviousCandidate() throws Exception {
    assertEquals("1.9.41", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
    SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
    assertFalse(prefs.contains("upgrade_chat"));
    try (ZikraDbHelper database = new ZikraDbHelper(context); ConversationStore chats = new ConversationStore(context)) {
      long task = database.addTask("[Uji upgrade] Belajar", ZikraDbHelper.CATEGORY_STUDY);
      long hifz = database.addHifzTask("[Uji upgrade] Hafalan", new HifzTarget(1, "Al-Fatihah", 7, 1, 3));
      ConversationStore.Chat chat = chats.create();
      chats.save(chat.id, new JSONArray().put(new JSONObject().put("role", "user").put("text", "Pesan sintetis sebelum upgrade")), "Draf belum dikirim");
      prefs.edit().putLong("upgrade_task", task).putLong("upgrade_hifz", hifz).putString("upgrade_chat", chat.id)
          .putString("profile_display_name", "Penguji upgrade").putBoolean("onboarding_complete", true)
          .putInt("last_chapter", 2).putInt("last_ayah", 12).putString("last_surah", "Al-Baqarah")
          .putString("last_verse_key", "2:12").commit();
      context.getSharedPreferences("zikra_home_appearance", 0).edit().putString("theme", "dark").commit();
    }
  }
  public void testVerifyAfterInPlaceUpdate() throws Exception {
    assertEquals("1.9.47", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
    SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
    assertTrue(prefs.contains("upgrade_chat"));
    assertEquals("Penguji upgrade", prefs.getString("profile_display_name", ""));
    assertEquals("2:12", prefs.getString("last_verse_key", ""));
    assertEquals(12, prefs.getInt("last_ayah", 0));
    assertEquals("dark", context.getSharedPreferences("zikra_home_appearance", 0).getString("theme", ""));
    try (ZikraDbHelper database = new ZikraDbHelper(context); ConversationStore chats = new ConversationStore(context)) {
      boolean taskFound = false, hifzFound = false;
      for (Task task : database.getTasks()) {
        if (task.id == prefs.getLong("upgrade_task", -1)) { assertEquals("[Uji upgrade] Belajar", task.title); taskFound = true; }
        if (task.id == prefs.getLong("upgrade_hifz", -1)) {
          assertEquals("[Uji upgrade] Hafalan", task.title); assertNotNull(task.hifz);
          assertEquals(1, task.hifz.firstVerse); assertEquals(3, task.hifz.lastVerse); hifzFound = true;
        }
      }
      assertTrue(taskFound); assertTrue(hifzFound);
      ConversationStore.Chat chat = chats.get(prefs.getString("upgrade_chat", ""));
      assertNotNull(chat); assertEquals(1, chat.messages.length());
      assertEquals("Pesan sintetis sebelum upgrade", chat.messages.getJSONObject(0).getString("text"));
      assertEquals("Draf belum dikirim", chat.draft);
    }
  }
}
