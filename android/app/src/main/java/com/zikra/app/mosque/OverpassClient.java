package com.zikra.app.mosque;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Bounded POST transport. A throttle stops the entire lookup, including failover. */
final class OverpassClient implements AutoCloseable {
  static final String[] ENDPOINTS={
      "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
      "https://overpass.private.coffee/api/interpreter",
      "https://overpass-api.de/api/interpreter"};
  private final OkHttpClient http;
  private final String[] endpoints;
  private volatile int generation;
  private volatile boolean closed;

  OverpassClient() {
    this(new OkHttpClient.Builder().connectTimeout(8,TimeUnit.SECONDS)
        .readTimeout(26,TimeUnit.SECONDS).callTimeout(32,TimeUnit.SECONDS).build(),ENDPOINTS);
  }
  OverpassClient(OkHttpClient http,String[] endpoints){this.http=http;this.endpoints=endpoints.clone();}
  static final class RateLimited extends IOException {
    final long retryAfterMs;
    RateLimited(long delay){super("Places rate limited");retryAfterMs=delay;}
  }
  static long retryMillis(String header) {
    long seconds=60;
    if(header!=null)try{seconds=Long.parseLong(header);}
    catch(NumberFormatException invalid){
      try{seconds=java.time.Duration.between(java.time.Instant.now(),java.time.ZonedDateTime
          .parse(header,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds();}
      catch(RuntimeException ignored){/* Keep the conservative default. */}
    }
    return Math.max(60,Math.min(31_536_000L,seconds))*1000;
  }
  String fetch(String query) throws IOException {
    int token=generation;long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(50);
    IOException last=null;
    for(String endpoint:endpoints){
      check(token);if(end-System.nanoTime()<=0)break;
      Request request=new Request.Builder().url(endpoint)
          .header("User-Agent","Zikra-Android/1.0 (+https://github.com/Arkanzahir/Zikra-App)")
          .header("Accept","application/json").post(new FormBody.Builder().add("data",query).build()).build();
      okhttp3.Call call=http.newCall(request);call.timeout().deadlineNanoTime(end);
      try(Response response=call.execute()){
        check(token);
        if(response.code()==429||response.code()==406)throw new RateLimited(retryMillis(response.header("Retry-After")));
        if(!response.isSuccessful()||response.body()==null)throw new IOException("Places HTTP "+response.code());
        response.body().source().request(1_048_577);
        if(response.body().source().buffer().size()>1_048_576)throw new IOException("Places response too large");
        String json=response.body().string();
        try{
          org.json.JSONObject root=new org.json.JSONObject(json);
          if(root.has("error")||root.has("remark")||root.optJSONArray("elements")==null)
            throw new IOException("Incomplete places response");
        }catch(org.json.JSONException invalid){throw new IOException("Invalid places response",invalid);}
        check(token);return json;
      }catch(RateLimited limited){throw limited;}
      catch(IOException unavailable){check(token);last=unavailable;}
    }
    throw last==null?new IOException("Places unavailable"):last;
  }
  private void check(int token) throws InterruptedIOException {
    if(closed||token!=generation||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Places lookup cancelled");
  }
  void cancel(){generation++;http.dispatcher().cancelAll();}
  @Override public void close(){closed=true;cancel();http.connectionPool().evictAll();http.dispatcher().executorService().shutdown();}
}
