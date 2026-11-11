package com.zikra.app.safety;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import okhttp3.*;
import org.junit.Test;

public class WeatherHttpTest {
  @Test(timeout = 8000)
  public void coordinateRequestDoesNotFollowRedirectToAnotherEndpoint() throws Exception {
    OkHttpClient client = WeatherHttp.create();
    ExecutorService worker = Executors.newSingleThreadExecutor();
    try (ServerSocket origin = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        ServerSocket target = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      origin.setSoTimeout(3000);
      target.setSoTimeout(300);
      Future<?> reply =
          worker.submit(
              () -> {
                try (Socket socket = origin.accept()) {
                  socket.setSoTimeout(2000);
                  BufferedReader input =
                      new BufferedReader(
                          new InputStreamReader(
                              socket.getInputStream(), StandardCharsets.US_ASCII));
                  while (!input.readLine().isEmpty()) {}
                  String response =
                      "HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:"
                          + target.getLocalPort()
                          + "/?latitude=0&longitude=0\r\n"
                          + "Content-Length: 0\r\n"
                          + "Connection: close\r\n\r\n";
                  socket.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
                } catch (Exception error) {
                  throw new RuntimeException(error);
                }
              });
      try (Response response =
          client
              .newCall(
                  new Request.Builder()
                      .url("http://127.0.0.1:" + origin.getLocalPort() + "/?latitude=0&longitude=0")
                      .build())
              .execute()) {
        assertEquals(302, response.code());
      }
      reply.get(3, TimeUnit.SECONDS);
      try (Socket ignored = target.accept()) {
        fail("Forecast request followed an unapproved redirect");
      } catch (SocketTimeoutException expected) {
      }
    } finally {
      worker.shutdownNow();
      client.connectionPool().evictAll();
      client.dispatcher().executorService().shutdownNow();
    }
  }
}
