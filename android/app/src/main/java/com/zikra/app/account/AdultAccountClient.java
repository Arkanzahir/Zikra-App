package com.zikra.app.account;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.JSONObject;

/** Owner-scoped account requests; no client cached eligibility can grant server access. */
public final class AdultAccountClient implements AutoCloseable {
  public interface Result{void complete(JSONObject result);void failed(String code);}
  private final OkHttpClient http=new OkHttpClient.Builder().callTimeout(20,TimeUnit.SECONDS)
      .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
  private final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
  private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
  private volatile boolean closed;private volatile Call active;private volatile int generation;
  public void send(String action,JSONObject body,Result callback){
    if(closed)return;int run=++generation;String binding=AccountSession.binding();
    String base=AccountSession.serviceBase();
    boolean deletion = action.equals("delete");
    if(!java.util.Arrays.asList("status","declare","renew","withdraw","delete").contains(action))throw new IllegalArgumentException("Unknown account action");
    worker.execute(()->{
      JSONObject result=null;String error="account_unavailable";
      try{
        HttpUrl url=HttpUrl.parse(base+"/v1/adult-account"+(action.equals("status")?"":"/"+action));
        if(!AccountSession.trusted(url))throw new IOException();
        if(!binding.equals(AccountSession.binding()))throw new IOException("Account changed");
        Request.Builder request=new Request.Builder().url(url).header("Authorization","Bearer "+(deletion ? AccountSession.tokenForDeletion() : AccountSession.token()));
        if(!action.equals("status"))request.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),body.toString()));
        if(closed||run!=generation)return;
        Call call=http.newCall(request.build());active=call;
        if(closed||run!=generation||!binding.equals(AccountSession.binding())){call.cancel();return;}
        try(Response response=call.execute()){
          if(response.body()!=null){response.body().source().request(8193);
            if(response.body().source().buffer().size()<=8192){
              JSONObject data=new JSONObject(response.body().string());
              if(response.isSuccessful() && (deletion ? "deleted".equals(data.optString("status"))
                  : data.optInt("minimumAge")==18&&"self_declared".equals(data.optString("assurance"))))result=data;
              else{String code=data.optString("error");if(java.util.Arrays.asList("unauthorized","under18_blocked","account_exists","not_registered","policy_changed","rate_limited","invalid_input").contains(code))error=code;}
            }
          }
        }
      }catch(Exception failure){if(failure instanceof AccountSession.Required)error="unauthorized";}
      JSONObject value=result;String code=error;
      main.post(()->{if(!closed&&run==generation){if(!binding.equals(AccountSession.binding())){callback.failed("unauthorized");return;}if(value!=null)callback.complete(value);else callback.failed(code);}});
    });
  }
  public void cancel(){generation++;Call call=active;if(call!=null)call.cancel();active=null;}
  @Override public void close(){closed=true;generation++;Call call=active;if(call!=null)call.cancel();worker.shutdownNow();main.removeCallbacksAndMessages(null);http.connectionPool().evictAll();}
}
