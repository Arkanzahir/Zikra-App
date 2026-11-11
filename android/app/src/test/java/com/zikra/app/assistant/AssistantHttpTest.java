package com.zikra.app.assistant;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import okhttp3.*;
import org.junit.Test;

public class AssistantHttpTest {
  @Test(timeout = 20000)
  public void delayedHeadersSurviveWhereOldDefaultReadTimeoutFails() throws Exception {
    ExecutorService workers = Executors.newFixedThreadPool(4);
    OkHttpClient old = new OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).build();
    OkHttpClient current = AssistantHttp.create();
    try (ServerSocket server = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))) {
      server.setSoTimeout(15000);
      Future<?> replies =
          workers.submit(
              () -> {
                for (int i = 0; i < 2; i++) {
                  try {
                    Socket socket = server.accept();
                    workers.submit(
                        () -> {
                          try (socket) {
                            socket.setSoTimeout(3000);
                            BufferedReader reader =
                                new BufferedReader(
                                    new InputStreamReader(
                                        socket.getInputStream(), StandardCharsets.US_ASCII));
                            String line;
                            while ((line = reader.readLine()) != null && !line.isEmpty()) {}
                            Thread.sleep(12000);
                            socket
                                .getOutputStream()
                                .write(
                                    "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nOK"
                                        .getBytes(StandardCharsets.US_ASCII));
                          } catch (Exception ignored) {
                            /* The old client deliberately disconnects first. */
                          }
                        });
                  } catch (Exception error) {
                    throw new RuntimeException(error);
                  }
                }
              });
      String url = "http://127.0.0.1:" + server.getLocalPort() + "/";
      Future<?> oldRequest =
          workers.submit(
              () -> {
                try (Response response =
                    old.newCall(new Request.Builder().url(url).build()).execute()) {
                  fail("Old default read timeout unexpectedly accepted delayed headers");
                } catch (SocketTimeoutException expected) {
                  assertEquals(10000, old.readTimeoutMillis());
                } catch (Exception error) {
                  throw new RuntimeException(error);
                }
              });
      try (Response response = current.newCall(new Request.Builder().url(url).build()).execute()) {
        assertEquals(200, response.code());
        assertEquals("OK", response.body().string());
      }
      oldRequest.get(2, TimeUnit.SECONDS);
      replies.get(2, TimeUnit.SECONDS);
    } finally {
      workers.shutdownNow();
      for (OkHttpClient client : new OkHttpClient[] {old, current}) {
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdownNow();
      }
    }
  }

  @Test
  public void timeoutsRemainBoundedAndNoRetryOrRedirectCanResendPrivateMessages() {
    OkHttpClient client = AssistantHttp.create();
    assertEquals(10000, client.connectTimeoutMillis());
    assertEquals(45000, client.readTimeoutMillis());
    assertEquals(20000, client.writeTimeoutMillis());
    assertEquals(60000, client.callTimeoutMillis());
    assertFalse(client.retryOnConnectionFailure());
    assertFalse(client.followRedirects());
    assertFalse(client.followSslRedirects());
  }
}
