package com.zikra.app.assistant;

import android.os.*;
import java.io.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;

/** Separate image route; no conversation, profile or attachment data is sent. */
public final class ImageGenerationClient implements AutoCloseable {
  public interface Result{void complete(GeneratedImages.Data data);void failed(String error);}
  public interface Capability{void available(boolean value);}
  private final OkHttpClient http;private final Handler main=new Handler(Looper.getMainLooper());
  private int generation,capabilityGeneration;private Call active,check;private boolean closed;
  public ImageGenerationClient(){this(new OkHttpClient.Builder().callTimeout(45,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build());}
  ImageGenerationClient(OkHttpClient client){http=client;}
  private Request.Builder request(AssistantConnection connection,String route)throws Exception{
    String token=connection.accessCode();if(!AssistantConnection.valid(connection.endpoint(),token))throw new IOException();
    HttpUrl url=HttpUrl.parse(connection.endpoint());return new Request.Builder().url(url.newBuilder().encodedPath(route).build()).header("Authorization","Bearer "+token);
  }
  public void capabilities(AssistantConnection connection,Capability callback){int token=++capabilityGeneration;if(check!=null)check.cancel();if(closed)return;
    try{check=http.newCall(request(connection,"/v1/capabilities").get().build());check.enqueue(new Callback(){public void onFailure(Call c,IOException e){deliver(false);}public void onResponse(Call c,Response r){boolean ready=false;try(r){if(r.isSuccessful()&&r.body()!=null){r.body().source().request(4097);if(r.body().source().buffer().size()<=4096)ready=new JSONObject(r.body().string()).optBoolean("imageGeneration");}}catch(Exception ignored){}deliver(ready);}private void deliver(boolean value){main.post(()->{if(!closed&&token==capabilityGeneration)callback.available(value);});}});}catch(Exception e){main.post(()->{if(!closed&&token==capabilityGeneration)callback.available(false);});}
  }
  public void generate(AssistantConnection connection,String prompt,String requestId,Result result)throws Exception{
    if(closed)throw new IOException();cancel();int token=generation;
    JSONObject body=new JSONObject().put("prompt",prompt).put("requestId",requestId);
    active=http.newCall(request(connection,"/v1/images").post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),body.toString())).build());
    active.enqueue(new Callback(){
      public void onFailure(Call c,IOException e){fail(e instanceof java.io.InterruptedIOException?"image_timeout":"image_network");}
      public void onResponse(Call c,Response r){GeneratedImages.Data image=null;String error="invalid_image_response";
        try(r){if(r.body()!=null){int limit=r.isSuccessful()?2_800_000:4096;r.body().source().request(limit+1L);if(r.body().source().buffer().size()<=limit){JSONObject value=new JSONObject(r.body().string());if(r.isSuccessful())image=GeneratedImages.Data.parse(value);else error=value.optString("error","image_provider_unavailable");}}}catch(Exception ignored){}
        if(image==null){fail(error);return;}GeneratedImages.Data data=image;main.post(()->{if(!closed&&token==generation)result.complete(data);});
      }
      private void fail(String error){main.post(()->{if(!closed&&token==generation)result.failed(error);});}
    });
  }
  public void cancel(){generation++;if(active!=null)active.cancel();active=null;}
  @Override public void close(){closed=true;cancel();capabilityGeneration++;if(check!=null)check.cancel();Thread cleanup=new Thread(()->{try{http.connectionPool().evictAll();}finally{http.dispatcher().executorService().shutdown();}},"Zikra-image-cleanup");cleanup.setDaemon(true);cleanup.start();}
}
