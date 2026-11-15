package com.zikra.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import com.zikra.app.discovery.HadithStore;
import com.zikra.app.reminders.*;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Disposable emulator only. Uses a separate database and restores preferences. */
public final class PublicBetaScopeTest extends InstrumentationTestCase {
  public void testPublicContentIsFilteredWithoutDeletingStoredRows() throws Exception {
    assertTrue(BuildConfig.PUBLIC_BETA_SCOPE);
    assertEquals("ranchu", android.os.Build.HARDWARE);
    Context context = getInstrumentation().getTargetContext();
    String database = "public-scope-test.db";
    context.deleteDatabase(database);
    try (HadithStore store = new HadithStore(context, database)) {
      CountDownLatch query = new CountDownLatch(1);
      AtomicReference<HadithStore.Page> page = new AtomicReference<>();
      store.query("", "Semua", Collections.emptySet(), 0, new HadithStore.Result<HadithStore.Page>() {
        public void show(HadithStore.Page value) { page.set(value); query.countDown(); }
        public void failed() { query.countDown(); }
      });
      assertTrue(query.await(5, TimeUnit.SECONDS));
      assertNotNull(page.get());
      assertEquals(0, page.get().total);
      try (SQLiteDatabase db = context.openOrCreateDatabase(database, 0, null);
          android.database.Cursor rows = db.rawQuery("SELECT COUNT(*) FROM entries", null)) {
        rows.moveToFirst(); assertTrue("Old entries retained", rows.getInt(0) > 0);
      }
      CountDownLatch read = new CountDownLatch(1);
      AtomicReference<Object> value = new AtomicReference<>();
      store.get("intentions", new HadithStore.Result<com.zikra.app.discovery.HadithCollection.Entry>() {
        public void show(com.zikra.app.discovery.HadithCollection.Entry entry) { value.set(entry); read.countDown(); }
        public void failed() { value.set("error"); read.countDown(); }
      });
      assertTrue(read.await(5, TimeUnit.SECONDS)); assertNull(value.get());
      CountDownLatch download = new CountDownLatch(1);
      AtomicReference<String> result = new AtomicReference<>();
      store.download("bukhari", new HadithStore.Download() {
        public void progress(int percent) { }
        public void finished() { result.set("unexpected"); download.countDown(); }
        public void failed(String code) { result.set(code); download.countDown(); }
      });
      assertTrue(download.await(5, TimeUnit.SECONDS)); assertEquals("scope", result.get());
    } finally { context.deleteDatabase(database); }
  }

  public void testPublicHadithScreenHidesDeferredCollections() throws Exception {
    assertTrue(BuildConfig.PUBLIC_BETA_SCOPE);
    assertEquals("ranchu", android.os.Build.HARDWARE);
    Context context = getInstrumentation().getTargetContext();
    android.app.Activity activity = getInstrumentation().startActivitySync(
        new android.content.Intent(context, HadithActivity.class)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      getInstrumentation().waitForIdleSync();
      assertFalse(activity.findViewById(R.id.hadithBooks).isShown());
      assertFalse(activity.findViewById(R.id.hadithHero).isShown());
      UiEvidence.capture(getInstrumentation(), "public-hadith");
    } finally { getInstrumentation().runOnMainSync(activity::finish); }
  }

  public void testPublicNotificationFallbackKeepsStoredAudioPreference() {
    assertTrue(BuildConfig.PUBLIC_BETA_SCOPE);
    assertEquals("ranchu", android.os.Build.HARDWARE);
    Context context = getInstrumentation().getTargetContext();
    android.content.SharedPreferences prefs = PrayerSettings.prefs(context);
    String previous = prefs.getString("prayer_mode_subuh", null);
    boolean enabled = prefs.getBoolean("reminder_subuh", true);
    try {
      prefs.edit().putString("prayer_mode_subuh", "ADHAN").putBoolean("reminder_subuh", true).commit();
      assertEquals(PrayerMode.NOTIFICATION, PrayerSettings.mode(context, 0));
      assertEquals("ADHAN", prefs.getString("prayer_mode_subuh", null));
      assertFalse(AdhanAsset.available(context, 0));
    } finally {
      android.content.SharedPreferences.Editor edit = prefs.edit().putBoolean("reminder_subuh", enabled);
      if (previous == null) edit.remove("prayer_mode_subuh"); else edit.putString("prayer_mode_subuh", previous);
      edit.commit();
    }
  }
}
