package com.zikra.app.audio;

import static org.junit.Assert.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import okio.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public final class TimedAudioRepositoryTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void cancellationBeforeWorkerStartsRejectsQueuedTicket() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    OkHttpClient http =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  calls.incrementAndGet();
                  return response(chain.request(), () -> {});
                })
            .build();
    try (TimedAudioRepository repository = new TimedAudioRepository(temporary.newFolder(), http)) {
      long ticket = repository.generation();
      repository.cancel();
      try {
        repository.load(Recitation.IDS[0], 1, 1, ticket);
        fail("Queued stale work must not start network requests");
      } catch (IOException expected) {
      }
      assertEquals(0, calls.get());
      assertTrue(repository.load(Recitation.IDS[0], 1, 1).file.isFile());
    }
  }

  private Response response(Request request, Runnable onClose) {
    String url = request.url().toString();
    String key =
        url.contains("by_ayah/")
            ? url.split("by_ayah/")[1].split("\\?")[0]
            : url.contains("by_key/") ? url.split("by_key/")[1].split("\\?")[0] : "1:1";
    String body =
        url.contains("by_ayah/")
            ? "{\"audio_files\":[{\"verse_key\":\""
                + key
                + "\",\"url\":\"Alafasy/mp3/001001.mp3\",\"duration\":2,\"segments\":[[0,1,0,1000]]}]}"
            : url.contains("by_key/")
                ? "{\"verse\":{\"verse_key\":\""
                    + key
                    + "\",\"words\":[{\"position\":1,\"char_type_name\":\"word\"}]}}"
                : "synthetic audio bytes; not a recording";
    BufferedSource source =
        Okio.buffer(
            new ForwardingSource(new Buffer().writeUtf8(body)) {
              @Override
              public void close() throws IOException {
                super.close();
                onClose.run();
              }
            });
    return new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("Fixture")
        .body(ResponseBody.create(null, -1, source))
        .build();
  }

  @Test
  public void cancelBetweenMetadataCallsPreventsNextRequestAndAllowsRetry() throws Exception {
    AtomicReference<TimedAudioRepository> reference = new AtomicReference<>();
    AtomicInteger calls = new AtomicInteger();
    AtomicBoolean cancel = new AtomicBoolean(true);
    OkHttpClient http =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  calls.incrementAndGet();
                  return response(
                      chain.request(),
                      () -> {
                        if (cancel.getAndSet(false)) reference.get().cancel();
                      });
                })
            .build();
    File folder = temporary.newFolder();
    try (TimedAudioRepository repository = new TimedAudioRepository(folder, http)) {
      reference.set(repository);
      try {
        repository.load(Recitation.IDS[0], 1, 1);
        fail("Cancelled generation must not fetch words or audio");
      } catch (IOException expected) {
      }
      assertEquals(1, calls.get());
      assertEquals(0, folder.list().length);
      assertTrue(repository.load(Recitation.IDS[0], 1, 1).file.isFile());
      assertEquals(4, calls.get());
    }
    assertEquals(0, folder.list().length);
  }

  @Test
  public void closeDoesNotWaitForStalledNetworkAndCannotPublishLateClip() throws Exception {
    CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
    OkHttpClient http =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  entered.countDown();
                  try {
                    if (!release.await(5, TimeUnit.SECONDS))
                      throw new IOException("Fixture timeout");
                  } catch (InterruptedException error) {
                    throw new IOException(error);
                  }
                  return response(chain.request(), () -> {});
                })
            .build();
    File folder = temporary.newFolder();
    TimedAudioRepository repository = new TimedAudioRepository(folder, http);
    ExecutorService worker = Executors.newSingleThreadExecutor();
    Future<?> load =
        worker.submit(
            () -> {
              try {
                repository.load(Recitation.IDS[0], 1, 1);
                throw new AssertionError("Closed load published");
              } catch (IOException expected) {
              } catch (Exception error) {
                throw new AssertionError(error);
              }
            });
    try {
      assertTrue(entered.await(3, TimeUnit.SECONDS));
      Future<?> close = CompletableFuture.runAsync(repository::close);
      close.get(1, TimeUnit.SECONDS);
      release.countDown();
      load.get(3, TimeUnit.SECONDS);
      assertEquals(0, folder.list().length);
      try {
        repository.load(Recitation.IDS[0], 1, 1);
        fail("Closed repository accepted another load");
      } catch (IOException expected) {
      }
    } finally {
      release.countDown();
      repository.close();
      worker.shutdownNow();
    }
  }

  @Test
  public void cancelAfterAudioReadCannotWriteStaleCache() throws Exception {
    AtomicReference<TimedAudioRepository> reference = new AtomicReference<>();
    File folder = temporary.newFolder();
    OkHttpClient http =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain ->
                    response(
                        chain.request(),
                        () -> {
                          if (chain.request().url().host().equals("verses.quran.foundation"))
                            reference.get().cancel();
                        }))
            .build();
    try (TimedAudioRepository repository = new TimedAudioRepository(folder, http)) {
      reference.set(repository);
      try {
        repository.load(Recitation.IDS[0], 1, 1);
        fail("Late audio must not enter cache");
      } catch (IOException expected) {
      }
      assertEquals(0, folder.list().length);
    }
  }

  @Test
  public void cacheStaysBoundedAndInvalidIdentityNeverUsesNetwork() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    File folder = temporary.newFolder();
    OkHttpClient http =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  calls.incrementAndGet();
                  return response(chain.request(), () -> {});
                })
            .build();
    try (TimedAudioRepository repository = new TimedAudioRepository(folder, http)) {
      try {
        repository.load(Recitation.IDS[0], 114, 7);
        fail();
      } catch (IllegalArgumentException expected) {
      }
      assertEquals(0, calls.get());
      TimedAudioRepository.Clip first = repository.load(Recitation.IDS[0], 1, 1);
      assertSame(first, repository.load(Recitation.IDS[0], 1, 1));
      assertEquals(3, calls.get());
      for (int verse = 2; verse <= 4; verse++) repository.load(Recitation.IDS[0], 1, verse);
      assertFalse(first.file.exists());
      assertEquals(3, folder.list().length);
    }
    assertEquals(0, folder.list().length);
  }
}
