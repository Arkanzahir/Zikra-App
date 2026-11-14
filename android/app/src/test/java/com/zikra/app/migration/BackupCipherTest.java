package com.zikra.app.migration;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public final class BackupCipherTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();
  private static final char[] PASSWORD = "contoh-khusus-uji-123".toCharArray();
  private byte[] encrypted() throws Exception {
    File plain = temporary.newFile(); Files.write(plain.toPath(), "Synthetic profile, tasks and chat only".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream(); BackupCipher.encrypt(plain, output, PASSWORD); return output.toByteArray();
  }
  private File target() { return new File(temporary.getRoot(), "restore-" + java.util.UUID.randomUUID()); }
  private void rejected(byte[] encrypted, char[] password) throws Exception {
    File file = target();
    try { BackupCipher.decrypt(new ByteArrayInputStream(encrypted), file, password); fail("Invalid encrypted backup must not publish"); }
    catch (IOException expected) { assertFalse(file.exists()); }
  }
  @Test public void roundTripUsesFreshSaltAndNonceWithoutPlaintextInArchive() throws Exception {
    byte[] first = encrypted(), second = encrypted(); assertFalse(Arrays.equals(first, second));
    assertFalse(new String(first, StandardCharsets.ISO_8859_1).contains("Synthetic profile"));
    File file = target(); BackupCipher.decrypt(new ByteArrayInputStream(first), file, PASSWORD);
    assertEquals("Synthetic profile, tasks and chat only", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
  }
  @Test public void wrongPasswordAndTamperingNeverLeaveStagingPlaintext() throws Exception {
    byte[] bytes = encrypted(); rejected(bytes, "kata-sandi-yang-salah".toCharArray());
    for (int index : new int[]{0, 8, 12, 28, 40, bytes.length - 1}) {
      byte[] changed = bytes.clone(); changed[index] ^= 1; rejected(changed, PASSWORD);
    }
  }
  @Test public void truncatedTrailingAndOversizedInputsAreRejected() throws Exception {
    byte[] bytes = encrypted(); rejected(Arrays.copyOf(bytes, bytes.length - 1), PASSWORD);
    rejected(Arrays.copyOf(bytes, 20), PASSWORD);
    rejected(Arrays.copyOf(bytes, bytes.length + 1), PASSWORD);
    byte[] oversized = bytes.clone(); ByteBuffer.wrap(oversized).putLong(40, BackupCipher.MAX_PAYLOAD + 1); rejected(oversized, PASSWORD);
  }
  @Test public void existingOutputAndBadPasswordPolicyNeverOverwriteUserFiles() throws Exception {
    byte[] bytes = encrypted(); File file = temporary.newFile(); Files.write(file.toPath(), new byte[]{1, 2, 3});
    try { BackupCipher.decrypt(new ByteArrayInputStream(bytes), file, PASSWORD); fail(); } catch (IOException expected) { }
    assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(file.toPath()));
    rejected(bytes, new char[0]); rejected(bytes, new char[129]);
  }
  @Test public void authenticatedChunkOrderAndCompletenessAreBoundToTheHeader() throws Exception {
    File input = temporary.newFile(); byte[] plain = new byte[150000]; new java.util.Random(42).nextBytes(plain);
    Files.write(input.toPath(), plain); ByteArrayOutputStream encoded = new ByteArrayOutputStream();
    BackupCipher.encrypt(input, encoded, PASSWORD); byte[] bytes = encoded.toByteArray();
    File decoded = target(); BackupCipher.decrypt(new ByteArrayInputStream(bytes), decoded, PASSWORD);
    assertArrayEquals(plain, Files.readAllBytes(decoded.toPath()));
    byte[] swapped = bytes.clone(); int first = 48, next = 48 + 65536 + 16;
    System.arraycopy(bytes, first, swapped, next, 65536 + 16);
    System.arraycopy(bytes, next, swapped, first, 65536 + 16); rejected(swapped, PASSWORD);
    rejected(Arrays.copyOf(bytes, next), PASSWORD);
  }
}
