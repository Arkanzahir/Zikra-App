package com.zikra.app.migration;

import android.content.*;
import android.test.InstrumentationTestCase;
import com.zikra.app.assistant.ConversationStore;
import com.zikra.app.data.ZikraDbHelper;
import java.io.*;
import java.util.Arrays;
import org.json.*;

/** Three separately invoked steps around an emulator-only uninstall and disposable signer change. */
@SuppressWarnings("deprecation")
public final class MigrationPortabilityTest extends InstrumentationTestCase {
  private android.content.Context context;
  private android.app.Activity foreground;
  private static final String PASSWORD = "Synthetic-portability-password-1234";
  protected void setUp() throws Exception {
    super.setUp(); context = getInstrumentation().getTargetContext();
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    assertTrue(new File(context.getFilesDir(), "local-migration-emulator-authorized").isFile());
    foreground = getInstrumentation().startActivitySync(new Intent(context, com.zikra.app.CalendarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  }
  protected void tearDown() throws Exception {
    try { if (foreground != null) getInstrumentation().runOnMainSync(foreground::finish); }
    finally { super.tearDown(); }
  }
  public void testPreparePortableEncryptedFixture() throws Exception {
    try (ZikraDbHelper db = new ZikraDbHelper(context)) { db.addTask("Portable synthetic task", ZikraDbHelper.CATEGORY_STUDY); }
    try (ConversationStore chats = new ConversationStore(context)) {
      chats.save(chats.create().id, new JSONArray().put(new JSONObject().put("role", "user").put("text", "Portable synthetic message")), "Portable draft");
    }
    context.getSharedPreferences("zikra_preferences", 0).edit().putString("profile_display_name", "Portable synthetic user")
        .putString("assistant_basic_consent_cloudflare_endpoint_v2", "SYNTHETIC-CONSENT-EXCLUDED").commit();
    context.getSharedPreferences("assistant_gateway", 0).edit().putString("token", "SYNTHETIC-TOKEN-EXCLUDED").commit();
    char[] password = PASSWORD.toCharArray(); File folder = new File(context.getCacheDir(), "zikra-export-" + java.util.UUID.randomUUID());
    try {
      File encrypted = LocalMigration.export(context, password, folder);
      java.nio.file.Files.copy(encrypted.toPath(), new File(context.getFilesDir(), "qa-portable.zikrabackup").toPath());
    } finally { Arrays.fill(password, '\0'); if (folder.exists()) LocalMigration.discardExport(context, folder); }
  }
  public void testPrepareRestoreAfterSignerChange() throws Exception {
    assertTrue(MigrationFreshness.empty(context)); char[] password = PASSWORD.toCharArray();
    try (InputStream input = new FileInputStream(new File(context.getFilesDir(), "qa-portable.zikrabackup"));
         LocalMigration.Preview preview = LocalMigration.inspect(context, input, password)) {
      assertTrue(preview.canRestore()); assertTrue(preview.records >= 3); LocalMigration.requestRestore(context, preview);
    } finally { Arrays.fill(password, '\0'); }
    assertTrue(LocalMigration.awaitingRestart(context));
  }
  public void testVerifyRestoredDataAfterSecondFreshProcess() throws Exception {
    assertFalse(LocalMigration.blocked()); assertFalse(LocalMigration.awaitingRestart(context));
    assertEquals("restored", context.getSharedPreferences("local_migration_status", 0).getString("result", ""));
    assertEquals("Portable synthetic user", context.getSharedPreferences("zikra_preferences", 0).getString("profile_display_name", ""));
    assertFalse(context.getSharedPreferences("zikra_preferences", 0).getBoolean("reminder_enabled", true));
    assertFalse(context.getSharedPreferences("zikra_preferences", 0).contains("assistant_basic_consent_cloudflare_endpoint_v2"));
    assertFalse(context.getSharedPreferences("assistant_gateway", 0).contains("token"));
    try (ZikraDbHelper db = new ZikraDbHelper(context)) { assertEquals("Portable synthetic task", db.getTasks().get(0).title); }
    try (ConversationStore chats = new ConversationStore(context)) {
      assertEquals(1, chats.list("").size()); ConversationStore.Chat chat = chats.get(chats.list("").get(0).id);
      assertEquals("Portable draft", chat.draft); assertEquals("Portable synthetic message", chat.messages.getJSONObject(0).getString("text"));
    }
    android.app.Activity backup = getInstrumentation().startActivitySync(new Intent(context, BackupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      getInstrumentation().runOnMainSync(() -> backup.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE));
      getInstrumentation().waitForIdleSync(); android.os.SystemClock.sleep(150);
      android.graphics.Bitmap image = getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(image);
      try (OutputStream output = new FileOutputStream(new File(context.getFilesDir(), "qa-backup-screen.png"))) {
        assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
      } finally { image.recycle(); }
    } finally { getInstrumentation().runOnMainSync(backup::finish); }
  }
}
