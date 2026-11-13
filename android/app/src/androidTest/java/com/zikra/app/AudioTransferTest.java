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

  public void testUnexpectedPartialResponseNeverPublishesCompletedAudio() throws Exception {
    for (int status : new int[]{206, 200}) {
      OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain ->
          new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
              .code(status).message("Synthetic partial representation")
              .header("Content-Range", "bytes 0-449/9000")
              .body(ResponseBody.create(MediaType.parse("audio/wav"), wave())).build()).build();
      try {
        transfer(http, () -> {});
        fail("Partial audio must not be marked complete even if its prefix decodes");
      } catch (IOException expected) { }
      assertFalse(target.exists());
      assertFalse(new File(target + ".part").exists());
    }
  }

  public void testTruncationAndInterruptedBodyDoNotPublishAndCanRetry() throws Exception {
    for (boolean interrupted : new boolean[]{false, true}) {
      OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
        byte[] bytes = wave();
        okio.Source stream = new okio.Buffer().write(bytes);
        if (interrupted) stream = new okio.ForwardingSource(stream) {
          private long read;
          @Override public long read(okio.Buffer sink, long count) throws IOException {
            if (read >= 256) throw new IOException("Synthetic body interrupted");
            long size = super.read(sink, Math.min(count, 256));
            if (size > 0) read += size;
            return size;
          }
        };
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(200).message("Synthetic truncation")
            .body(ResponseBody.create(MediaType.parse("audio/wav"), bytes.length + 1024, okio.Okio.buffer(stream))).build();
      }).build();
      try { transfer(http, () -> {}); fail("Incomplete transfer must fail"); }
      catch (IOException expected) { }
      assertFalse(target.exists());
      assertFalse(new File(target + ".part").exists());
    }
    transfer(client(wave(), 200, new AtomicInteger()), () -> {});
    assertTrue(AudioTransfer.decodable(target));
  }

  public void testValidCompletedAudioSurvivesCancelledRetryWithoutNetwork() throws Exception {
    transfer(client(wave(), 200, new AtomicInteger()), () -> {});
    byte[] before = Files.readAllBytes(target.toPath());
    AtomicInteger calls = new AtomicInteger();
    OkHttpClient unavailable = client(new byte[512], 503, calls);
    try { transfer(unavailable, () -> { throw new IOException("Synthetic cancellation"); }); fail("Cancelled request must stop"); }
    catch (IOException expected) { }
    transfer(unavailable, () -> {});
    assertEquals(0, calls.get());
    assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(target.toPath())));
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
