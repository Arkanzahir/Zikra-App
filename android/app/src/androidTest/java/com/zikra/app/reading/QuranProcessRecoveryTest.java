package com.zikra.app.reading;

import android.content.*;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import org.json.JSONObject;

/** Harness intentionally kills the prepare method; only recovery is a completed test. */
@SuppressWarnings("deprecation")
public final class QuranProcessRecoveryTest extends InstrumentationTestCase {
  private Context target, isolated;
  private File root, fixtures;
  @Override protected void setUp() throws Exception {
    super.setUp();
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    target = getInstrumentation().getTargetContext();
    assertTrue(new File(target.getFilesDir(), "quran-failure-emulator-authorized").isFile());
    root = new File(target.getFilesDir(), "quran-process-owned");
    fixtures = new File(target.getFilesDir(), "quran-failure-fixtures/reader-pages");
    isolated = new ContextWrapper(target) { @Override public File getFilesDir() { return root; } };
  }
  private String page(int number) throws Exception {
    return QuranSyncStore.readBounded(new FileInputStream(new File(fixtures, "2-" + number + ".json")), 524288);
  }
  public void testPrepareThenWaitForProcessKill() throws Exception {
    assertFalse("Fresh owned sandbox required", root.exists()); assertTrue(root.mkdir());
    QuranChapters.Chapter chapter = QuranChapters.all(target).get(1);
    QuranSyncStore store = new QuranSyncStore(isolated);
    store.download(chapter, this::page, () -> {});
    File manifest = new File(root, "quran-content-sync-r33/chapter-2.json");
    Files.copy(manifest.toPath(), new File(root, "baseline-manifest.json").toPath());
    target.getSharedPreferences("zikra_preferences", 0).edit()
        .putInt("last_chapter", 2).putInt("last_ayah", 12).putString("last_verse_key", "2:12").commit();
    Files.write(new File(root, "private-user-file.txt").toPath(), "keep-user-data".getBytes(StandardCharsets.UTF_8));
    store.download(chapter, number -> {
      if (number == 2) {
        try (FileOutputStream marker = new FileOutputStream(new File(root, "kill-ready"))) {
          marker.write(Integer.toString(android.os.Process.myPid()).getBytes(StandardCharsets.UTF_8)); marker.getFD().sync();
        }
        new CountDownLatch(1).await(90, java.util.concurrent.TimeUnit.SECONDS);
        throw new IOException("Harness failed to kill process during staged write");
      }
      return page(number);
    }, () -> {});
    fail("Prepare must be interrupted by real process termination");
  }
  public void testRecoverCommittedDataAfterProcessKill() throws Exception {
    assertTrue(root.isDirectory());
    int previousPid = Integer.parseInt(new String(Files.readAllBytes(new File(root, "kill-ready").toPath()), StandardCharsets.UTF_8));
    assertTrue("A new process must perform recovery", previousPid != android.os.Process.myPid());
    File content = new File(root, "quran-content-sync-r33");
    String baseline = new String(Files.readAllBytes(new File(root, "baseline-manifest.json").toPath()), StandardCharsets.UTF_8);
    String committed = new JSONObject(baseline).getString("generation");
    File orphan = null;
    for (File folder : content.listFiles()) if (folder.isDirectory() && !folder.getName().equals(committed)) orphan = folder;
    assertNotNull("Killed process must leave a real incomplete generation", orphan);
    assertTrue(new File(orphan, "1.json").isFile()); assertFalse(new File(orphan, "2.json").exists());
    QuranSyncStore recovered = new QuranSyncStore(isolated);
    assertEquals(page(1), recovered.read(QuranChapters.all(target).get(1), 1));
    assertEquals(baseline, new String(Files.readAllBytes(new File(content, "chapter-2.json").toPath()), StandardCharsets.UTF_8));
    assertFalse("Only owned orphan should be removed", orphan.exists());
    assertEquals("keep-user-data", new String(Files.readAllBytes(new File(root, "private-user-file.txt").toPath()), StandardCharsets.UTF_8));
    assertEquals("2:12", target.getSharedPreferences("zikra_preferences", 0).getString("last_verse_key", ""));
    assertEquals(12, target.getSharedPreferences("zikra_preferences", 0).getInt("last_ayah", 0));
  }
}
