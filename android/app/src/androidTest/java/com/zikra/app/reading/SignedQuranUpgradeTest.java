package com.zikra.app.reading;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.test.InstrumentationTestCase;

/** Verifies reader data across a signed package update on a fresh emulator. */
@SuppressWarnings("deprecation")
public final class SignedQuranUpgradeTest extends InstrumentationTestCase {
  private SharedPreferences readerPreferences(int expectedVersionCode) throws Exception {
    assertEquals("Use the emulator assigned to this test", "ranchu", Build.HARDWARE);
    Context target = getInstrumentation().getTargetContext();
    assertEquals(0, target.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
    assertEquals(expectedVersionCode,
        target.getPackageManager().getPackageInfo(target.getPackageName(), 0).versionCode);
    return target.getSharedPreferences("zikra_preferences", Context.MODE_PRIVATE);
  }

  public void testSeedReaderBeforeSignedUpgrade() throws Exception {
    SharedPreferences reader = readerPreferences(91);
    assertFalse("Root the migration test in a fresh AVD", reader.contains("last_verse_key"));
    assertTrue(reader.edit().putInt("last_chapter", 1).putInt("last_ayah", 2)
        .putString("last_verse_key", "1:2").putBoolean("reader_word_sync", true).commit());
  }

  public void testReaderDataSurvivesSignedUpgrade() throws Exception {
    SharedPreferences reader = readerPreferences(92);
    assertEquals(1, reader.getInt("last_chapter", -1));
    assertEquals(2, reader.getInt("last_ayah", -1));
    assertEquals("1:2", reader.getString("last_verse_key", ""));
    assertTrue(reader.getBoolean("reader_word_sync", false));
  }
}
