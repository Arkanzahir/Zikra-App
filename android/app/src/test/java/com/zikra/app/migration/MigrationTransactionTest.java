package com.zikra.app.migration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public final class MigrationTransactionTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();
  private static final class InterruptedProcess extends Error {}
  private void write(File file, String value) throws Exception { file.getParentFile().mkdirs(); Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8)); }
  private String read(File file) throws Exception { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
  private static final class Fixture {
    File root, live;
    Map<String, File> staged = new LinkedHashMap<>(), targets = new LinkedHashMap<>();
    MigrationTransaction transaction() throws Exception { return new MigrationTransaction(root, staged, targets); }
  }
  private Fixture fixture() throws Exception {
    Fixture f = new Fixture(); File top = temporary.newFolder(); f.root = new File(top, "stage"); f.live = new File(top, "live");
    assertTrue(f.root.mkdir()); assertTrue(f.live.mkdir()); write(new File(f.root, ".owner"), MigrationStageContext.OWNER);
    for (String key : new String[]{"database", "preferences"}) {
      File staged = new File(f.root, "next/" + key), target = new File(f.live, key);
      write(staged, "new-" + key); write(target, "old-" + key); f.staged.put(key, staged); f.targets.put(key, target);
    }
    return f;
  }
  @Test public void completeReplacementRetainsOriginalsUntilCallerCleanup() throws Exception {
    Fixture f = fixture(); MigrationTransaction tx = f.transaction(); tx.prepare(); tx.apply();
    for (String key : f.targets.keySet()) {
      assertEquals("new-" + key, read(f.targets.get(key)));
      assertEquals("old-" + key, read(new File(f.root, "originals/" + key)));
    }
    assertEquals("committed", tx.phase()); tx.apply();
    try { tx.rollback(); fail(); } catch (IOException expected) { }
  }
  @Test public void aNewInstanceResumesAfterEveryIndividualMove() throws Exception {
    for (int interruptedAt = 1; interruptedAt <= 4; interruptedAt++) {
      Fixture f = fixture(); MigrationTransaction tx = f.transaction(); tx.prepare(); final int stop = interruptedAt; int[] count = {0};
      try { tx.apply(key -> { if (++count[0] == stop) throw new InterruptedProcess(); }); fail(); } catch (InterruptedProcess expected) { }
      MigrationTransaction resumed = f.transaction(); resumed.apply(); assertEquals("committed", resumed.phase());
      for (String key : f.targets.keySet()) assertEquals("new-" + key, read(f.targets.get(key)));
    }
  }
  @Test public void invalidCandidateCanRollBackAlreadyMovedFilesWithoutChangingSourceData() throws Exception {
    Fixture f = fixture(); MigrationTransaction tx = f.transaction(); tx.prepare();
    try { tx.apply(key -> { throw new InterruptedProcess(); }); fail(); } catch (InterruptedProcess expected) { }
    write(f.staged.get("preferences"), "corrupt");
    try { f.transaction().apply(); fail(); } catch (IOException expected) { }
    f.transaction().rollback();
    for (String key : f.targets.keySet()) assertEquals("old-" + key, read(f.targets.get(key)));
    assertEquals("rolled_back", f.transaction().phase());
  }
  @Test public void unexpectedNewLiveDataIsNeverDeletedToForceRollback() throws Exception {
    Fixture f = fixture(); MigrationTransaction tx = f.transaction(); tx.prepare();
    try { tx.apply(key -> { throw new InterruptedProcess(); }); fail(); } catch (InterruptedProcess expected) { }
    write(f.targets.get("database"), "another-new-user-record");
    try { f.transaction().apply(); fail(); } catch (IOException expected) { }
    try { f.transaction().rollback(); fail(); } catch (IOException expected) { }
    assertEquals("another-new-user-record", read(f.targets.get("database")));
    assertEquals("old-database", read(new File(f.root, "originals/database")));
  }
  @Test public void absentAndRemovedSidecarsAreRestoredCorrectlyOnRollback() throws Exception {
    Fixture f = fixture(); Files.delete(f.staged.get("database").toPath()); Files.delete(f.targets.get("preferences").toPath());
    MigrationTransaction tx = f.transaction(); tx.prepare();
    try { tx.apply(key -> { if (key.equals("preferences")) throw new InterruptedProcess(); }); fail(); } catch (InterruptedProcess expected) { }
    tx.rollback(); assertEquals("old-database", read(f.targets.get("database"))); assertFalse(f.targets.get("preferences").exists());
  }
  @Test public void unknownOwnerAliasedTargetsAndStagesAsDestinationsAreRefused() throws Exception {
    Fixture f = fixture(); write(new File(f.root, ".owner"), "not-owned");
    try { f.transaction(); fail(); } catch (IOException expected) { }
    write(new File(f.root, ".owner"), MigrationStageContext.OWNER);
    f.targets.put("preferences", f.targets.get("database"));
    try { f.transaction(); fail(); } catch (IOException expected) { }
    f.targets.put("preferences", new File(f.root, "forbidden"));
    try { f.transaction(); fail(); } catch (IOException expected) { }
  }
}
