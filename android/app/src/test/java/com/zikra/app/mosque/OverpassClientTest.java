package com.zikra.app.mosque;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.OkHttpClient;
import org.junit.Test;

public final class OverpassClientTest {
  private static final String JSON="{\"elements\":[]}";
  private static final class Server implements AutoCloseable {
    final ServerSocket server=new ServerSocket();
    final AtomicInteger calls=new AtomicInteger();
    final Thread listener;
    volatile String method,body;
    Server(int status,String response) throws IOException {
      server.bind(new InetSocketAddress("127.0.0.1",0));
      listener=new Thread(()->{
        while(!server.isClosed())try(Socket socket=server.accept()){
          socket.setSoTimeout(3000);calls.incrementAndGet();
          BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
          String request=input.readLine();method=request.split(" ")[0];int length=0;
          for(String line;(line=input.readLine())!=null&&!line.isEmpty();)
            if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:"))length=Integer.parseInt(line.substring(15).trim());
          StringBuilder content=new StringBuilder();while(content.length()<length){int c=input.read();if(c<0)break;content.append((char)c);}body=content.toString();
          byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
          socket.getOutputStream().write(("HTTP/1.1 "+status+" Response\r\nContent-Length: "+bytes.length+"\r\nRetry-After: 120\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
          socket.getOutputStream().write(bytes);socket.getOutputStream().flush();
        }catch(IOException disconnected){/* A bounded reader may deliberately close early. */}
      },"overpass-test-server");listener.start();
    }
    String url(){return "http://127.0.0.1:"+server.getLocalPort()+"/";}
    @Override public void close() throws Exception {server.close();listener.join(1000);}
  }
  private OverpassClient client(Server... servers){
    return new OverpassClient(new OkHttpClient.Builder().callTimeout(3,TimeUnit.SECONDS).build(),
        java.util.Arrays.stream(servers).map(Server::url).toArray(String[]::new));
  }
  @Test public void sendsBoundedFormPost() throws Exception {
    try(Server server=new Server(200,JSON);OverpassClient client=client(server)){
      assertEquals(JSON,client.fetch("[out:json];node(1);out;"));assertEquals("POST",server.method);
      assertEquals("data=[out:json];node(1);out;",java.net.URLDecoder.decode(server.body,"UTF-8"));
    }
  }
  @Test public void unavailableAndMalformedEndpointsFallBack() throws Exception {
    try(Server down=new Server(503,"busy");Server html=new Server(200,"<html>unavailable</html>");
        Server good=new Server(200,JSON);OverpassClient client=client(down,html,good)){
      assertEquals(JSON,client.fetch("test"));assertEquals(1,down.calls.get());assertEquals(1,html.calls.get());assertEquals(1,good.calls.get());
    }
  }
  @Test public void serverThrottleStopsAllFailover() throws Exception {
    for(int code:new int[]{429,406})try(Server limited=new Server(code,"wait");Server next=new Server(200,JSON);OverpassClient client=client(limited,next)){
      try{client.fetch("test");fail("Throttle expected");}catch(OverpassClient.RateLimited e){assertEquals(120000,e.retryAfterMs);}
      assertEquals(0,next.calls.get());
    }
  }
  @Test public void oversizedBodyIsRejected() throws Exception {
    try(Server server=new Server(200,"x".repeat(1_048_577));OverpassClient client=client(server)){
      try{client.fetch("test");fail("Bound expected");}catch(IOException e){assertTrue(e.getMessage().contains("too large"));}
    }
  }
  @Test public void closedTransportCannotSendNewLocation() throws Exception {
    try(Server server=new Server(200,JSON);OverpassClient client=client(server)){
      client.close();try{client.fetch("test");fail("Closed expected");}catch(java.io.InterruptedIOException expected){assertEquals(0,server.calls.get());}
    }
  }
}
