package com.zikra.app;

import android.content.Context;
import android.test.InstrumentationTestCase;
import com.zikra.app.audio.*;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;

/**
 * Exercises real decoder and transfer code with synthetic WAV HTTP bodies, never qari recordings.
 */
@SuppressWarnings("deprecation")
public final class AudioTransferTest extends InstrumentationTestCase {
  private Context context;
  private File target;
  private byte[] saved;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    target = AudioFiles.file(context, Recitation.IDS[0], 1, 1);
    saved = target.exists() ? Files.readAllBytes(target.toPath()) : null;
    if (target.exists()) assertTrue(target.delete());
  }

  private byte[] wave() {
    int size = 4410;
    ByteBuffer b = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN);
    b.put("RIFF".getBytes())
        .putInt(36 + size)
        .put("WAVEfmt ".getBytes())
        .putInt(16)
        .putShort((short) 1)
        .putShort((short) 1)
        .putInt(22050)
        .putInt(44100)
        .putShort((short) 2)
        .putShort((short) 16)
        .put("data".getBytes())
        .putInt(size);
    return b.array();
  }

  private OkHttpClient client(byte[] bytes, int code, AtomicInteger calls) {
    return new OkHttpClient.Builder()
        .addInterceptor(
            chain -> {
              calls.incrementAndGet();
              assertEquals(
                  Recitation.url(Recitation.IDS[0], 1, 1), chain.request().url().toString());
              return new Response.Builder()
                  .request(chain.request())
                  .protocol(Protocol.HTTP_1_1)
                  .code(code)
                  .message("Synthetic fixture")
                  .body(ResponseBody.create(MediaType.parse("audio/wav"), bytes))
                  .build();
            })
        .build();
  }

  private void transfer(OkHttpClient client, AudioTransfer.Check check) throws Exception {
    AudioTransfer.ensure(context, client, Recitation.IDS[0], 1, 1, check);
  }

  public void testAtomicTransferDecoderAndReuse() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    OkHttpClient client = client(wave(), 200, calls);
    transfer(client, () -> {});
    assertTrue(AudioTransfer.decodable(target));
    assertFalse(new File(target + ".part").exists());
    transfer(client, () -> {});
    assertEquals(1, calls.get());
  }

  public void testCorruptCompletedFileIsFetchedAgain() throws Exception {
    target.getParentFile().mkdirs();
    Files.write(target.toPath(), new byte[512]);
    assertTrue(AudioFiles.available(context, Recitation.IDS[0], 1, 1));
    AtomicInteger calls = new AtomicInteger();
    transfer(client(wave(), 200, calls), () -> {});
    assertEquals(1, calls.get());
    assertTrue(AudioTransfer.decodable(target));
  }

  public void testHttpErrorAndInvalidPayloadNeverBecomeAvailable() throws Exception {
    for (int code : new int[] {503, 200}) {
      try {
        transfer(client(new byte[512], code, new AtomicInteger()), () -> {});
        fail("Reject invalid transfer");
      } catch (IOException expected) {
      }
      assertFalse(target.exists());
      assertFalse(new File(target + ".part").exists());
    }
  }

  public void testCancellationRemovesPartialAndRetryCanFinish() throws Exception {
    AtomicInteger checks = new AtomicInteger();
    try {
      transfer(
          client(wave(), 200, new AtomicInteger()),
          () -> {
            if (checks.incrementAndGet() > 1) throw new IOException("Cancelled fixture");
          });
      fail("Cancel");
    } catch (IOException expected) {
    }
    assertFalse(target.exists());
    assertFalse(new File(target + ".part").exists());
    transfer(client(wave(), 200, new AtomicInteger()), () -> {});
    assertTrue(AudioTransfer.decodable(target));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (saved == null) target.delete();
      else {
        target.getParentFile().mkdirs();
        Files.write(target.toPath(), saved);
      }
      new File(target + ".part").delete();
    } finally {
      super.tearDown();
    }
  }
}
