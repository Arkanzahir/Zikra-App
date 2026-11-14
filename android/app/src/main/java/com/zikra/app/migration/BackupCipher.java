package com.zikra.app.migration;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Portable password encryption. Plaintext is trusted only after the GCM tag verifies. */
public final class BackupCipher {
  public static final long MAX_PAYLOAD = 96L * 1024 * 1024;
  public static final int MIN_PASSWORD = 12, MAX_PASSWORD = 128;
  private static final int ITERATIONS = 600000, SALT = 16, NONCE = 12, TAG = 16;
  private static final int CHUNK = 65536;
  private static final byte[] MAGIC = "ZIKRAB01".getBytes(StandardCharsets.US_ASCII);
  private static final int HEADER = 8 + 4 + SALT + NONCE + 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private BackupCipher() {}

  private static byte[] key(byte[] header, char[] password) throws GeneralSecurityException, IOException {
    if (password == null || password.length < MIN_PASSWORD || password.length > MAX_PASSWORD)
      throw new IOException("Backup password length is invalid");
    ByteBuffer data = ByteBuffer.wrap(header); byte[] magic = new byte[MAGIC.length]; data.get(magic);
    if (!Arrays.equals(magic, MAGIC) || data.getInt() != ITERATIONS) throw new IOException("Unsupported backup header");
    byte[] salt = new byte[SALT]; data.get(salt);
    PBEKeySpec specification = new PBEKeySpec(password, salt, ITERATIONS, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).getEncoded();
    } finally { specification.clearPassword(); }
  }

  private static Cipher chunkCipher(int mode, byte[] key, byte[] header, int index, int length) throws GeneralSecurityException {
    byte[] nonce = Arrays.copyOfRange(header, 8 + 4 + SALT, 8 + 4 + SALT + NONCE);
    ByteBuffer nonceBytes = ByteBuffer.wrap(nonce); nonceBytes.putInt(8, nonceBytes.getInt(8) ^ index);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
    cipher.updateAAD(header); cipher.updateAAD(ByteBuffer.allocate(8).putInt(index).putInt(length).array());
    return cipher;
  }

  /** Caller owns the destination stream and removes an incomplete external document on failure. */
  public static void encrypt(File payload, OutputStream destination, char[] password) throws IOException {
    long length = payload.length();
    if (!payload.isFile() || length < 1 || length > MAX_PAYLOAD) throw new IOException("Backup payload size is invalid");
    byte[] salt = new byte[SALT], nonce = new byte[NONCE]; RANDOM.nextBytes(salt); RANDOM.nextBytes(nonce);
    byte[] header = ByteBuffer.allocate(HEADER).put(MAGIC).putInt(ITERATIONS).put(salt).put(nonce).putLong(length).array();
    byte[] key = null;
    try {
      key = key(header, password); destination.write(header);
      try (DataInputStream input = new DataInputStream(new FileInputStream(payload))) {
        byte[] buffer = new byte[CHUNK]; long remaining = length; int index = 0;
        while (remaining > 0) {
          int count = (int) Math.min(CHUNK, remaining); input.readFully(buffer, 0, count);
          destination.write(chunkCipher(Cipher.ENCRYPT_MODE, key, header, index++, count).doFinal(buffer, 0, count));
          remaining -= count;
        }
        if (input.read() != -1) throw new IOException("Backup payload changed");
        Arrays.fill(buffer, (byte) 0);
      }
      destination.flush();
    } catch (GeneralSecurityException failed) { throw new IOException("Backup encryption failed", failed); }
    finally { if (key != null) Arrays.fill(key, (byte) 0); }
  }

  /** Creates a new private staging file; never overwrites a pre-existing file or exposes unauthenticated output. */
  public static void decrypt(InputStream source, File stagedPayload, char[] password) throws IOException {
    DataInputStream input = new DataInputStream(source); byte[] header = new byte[HEADER]; input.readFully(header);
    long length = ByteBuffer.wrap(header).getLong(HEADER - 8);
    if (length < 1 || length > MAX_PAYLOAD) throw new IOException("Backup payload size is invalid");
    boolean created = false, complete = false;
    byte[] key = null;
    try {
      key = key(header, password);
      try (OutputStream output = Files.newOutputStream(stagedPayload.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
        created = true; long remaining = length; int index = 0;
        byte[] buffer = new byte[CHUNK + TAG];
        while (remaining > 0) {
          int count = (int) Math.min(CHUNK, remaining); input.readFully(buffer, 0, count + TAG);
          byte[] plain = chunkCipher(Cipher.DECRYPT_MODE, key, header, index++, count).doFinal(buffer, 0, count + TAG);
          if (plain.length != count) throw new IOException("Invalid backup chunk");
          output.write(plain); Arrays.fill(plain, (byte) 0); remaining -= count;
        }
        if (input.read() != -1) throw new IOException("Invalid backup length");
      }
      try (FileOutputStream sync = new FileOutputStream(stagedPayload, true)) { sync.getFD().sync(); }
      complete = true;
    } catch (GeneralSecurityException failed) { throw new IOException("Wrong password or damaged backup", failed); }
    finally {
      if (key != null) Arrays.fill(key, (byte) 0);
      if (created && !complete) Files.deleteIfExists(stagedPayload.toPath());
    }
  }
}
