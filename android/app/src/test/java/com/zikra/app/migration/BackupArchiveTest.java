package com.zikra.app.migration;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public final class BackupArchiveTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();
  private File target() { return new File(temporary.getRoot(), "backup-" + UUID.randomUUID()); }
  private File file(byte[] data) throws Exception { File file = temporary.newFile(); Files.write(file.toPath(), data); return file; }
  @Test public void personalSnapshotAndMediaRoundTripWithoutArbitraryFiles() throws Exception {
    String name = "media/assistant_images/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".png";
    Map<String, File> entries = new HashMap<>(); entries.put("snapshot.json", file("{}".getBytes())); entries.put(name, file(new byte[]{1, 2, 3}));
    File packed = target(), restored = target(); BackupArchive.pack(entries, packed); BackupArchive.unpack(packed, restored);
    assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(new File(restored, name).toPath()));
    assertTrue(new File(restored, "snapshot.json").isFile());
  }
  @Test public void pathsCredentialsAndUnknownEntriesAreNotAllowed() throws Exception {
    for (String name : new String[]{"../snapshot.json", "/snapshot.json", "snapshot.json/../../private", "databases/zikra.db",
        "shared_prefs/assistant_gateway.xml", ".env", "media/assistant_images/../../key", "media\\assistant_images\\key"}) {
      assertFalse(BackupArchive.allowed(name));
      File output = target(); Map<String, File> entries = new HashMap<>();
      entries.put("snapshot.json", file(new byte[]{1})); entries.put(name, file(new byte[]{2}));
      try { BackupArchive.pack(entries, output); fail(); } catch (IOException expected) { assertFalse(output.exists()); }
    }
  }
  @Test public void extractionRejectsZipSlipAndMissingSnapshotWithoutLeavingPartialFiles() throws Exception {
    for (String name : new String[]{"../escaped", "unknown", "media/assistant_images/x/file.png"}) {
      File archive = target();
      try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(archive))) {
        zip.putNextEntry(new ZipEntry(name)); zip.write(1); zip.closeEntry();
      }
      File restored = target();
      try { BackupArchive.unpack(archive, restored); fail(); } catch (IOException expected) { assertFalse(restored.exists()); }
    }
  }
  @Test public void expansionLimitIsMeasuredFromActualBytesAndExistingTargetsStayUntouched() throws Exception {
    File archive = target();
    try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(archive))) {
      zip.putNextEntry(new ZipEntry("snapshot.json")); byte[] zeros = new byte[65536];
      for (int i = 0; i <= BackupArchive.MAX_SINGLE / zeros.length; i++) zip.write(zeros);
      zip.closeEntry();
    }
    File restored = target();
    try { BackupArchive.unpack(archive, restored); fail(); } catch (IOException expected) { assertFalse(restored.exists()); }
    File existing = temporary.newFolder(); File retained = new File(existing, "user.txt"); Files.write(retained.toPath(), new byte[]{42});
    try { BackupArchive.unpack(archive, existing); fail(); } catch (IOException expected) { assertArrayEquals(new byte[]{42}, Files.readAllBytes(retained.toPath())); }
  }
}
