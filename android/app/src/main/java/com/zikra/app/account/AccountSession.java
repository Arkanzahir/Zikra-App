package com.zikra.app.account;

import android.os.Looper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zikra.app.BuildConfig;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.*;

/** Fresh account token stays in memory and is sent only to the first-party service. */
public final class AccountSession {
  private static final AtomicLong REVISION=new AtomicLong();
  private AccountSession() {}
  public static void invalidate(){REVISION.incrementAndGet();}
  public static String binding(){
    if(!BuildConfig.ADULT_ACCOUNTS_ENABLED)return "legacy";
    try{FirebaseUser user=auth().getCurrentUser();return REVISION.get()+":"+(user==null?"signed-out":user.getUid());}
    catch(IllegalStateException unavailable){return REVISION.get()+":unavailable";}
  }
  public static boolean held(){
    FirebaseUser user=auth().getCurrentUser();if(user==null)return false;
    return auth().getApp().getApplicationContext().getSharedPreferences("zikra_adult_ai_privacy",0).getBoolean(holdKey(user),false);
  }
  private static String holdKey(FirebaseUser user){
    try{
      String value=auth().getApp().getOptions().getProjectId()+":"+user.getUid();
      byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder key=new StringBuilder("withdrawn:");for(byte b:bytes)key.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return key.toString();
    }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
  }
  public static void hold(){setHold(true);}
  static void releaseHold(){setHold(false);}
  private static void setHold(boolean blocked){
    invalidate();FirebaseUser user=auth().getCurrentUser();if(user==null)return;
    auth().getApp().getApplicationContext().getSharedPreferences("zikra_adult_ai_privacy",0).edit().putBoolean(holdKey(user),blocked).commit();
  }
  static FirebaseAuth auth(){return AccountAuth.current();}
  public enum GatewayRequest { INSTANCE }

  public static String serviceBase() {
    return BuildConfig.DEBUG && BuildConfig.ACCOUNT_AUTH_EMULATOR
        ? "http://10.0.2.2:9098" : "https://zikra-ai.vercel.app";
  }

  public static boolean hasVerifiedUser() {
    try {
      FirebaseUser user = auth().getCurrentUser();
      return user != null && user.isEmailVerified();
    } catch (IllegalStateException unavailable) {
      return false;
    }
  }

  public static boolean trusted(HttpUrl url){
    if(url==null||!url.username().isEmpty()||!url.password().isEmpty()||url.query()!=null||url.fragment()!=null)return false;
    if(BuildConfig.DEBUG&&BuildConfig.ACCOUNT_AUTH_EMULATOR)
      return url.scheme().equals("http")&&url.host().equals("10.0.2.2")&&url.port()==9098;
    return url.isHttps()&&url.host().equals("zikra-ai.vercel.app")&&url.port()==443;
  }
  public static final class Required extends IOException { Required(){super("account_sign_in_required");} }
  static String token()throws IOException {
    if(Looper.myLooper()==Looper.getMainLooper())throw new IOException("Account request requires worker");
    FirebaseUser user=auth().getCurrentUser();
    if(user==null||!user.isEmailVerified())throw new Required();
    try{
      String token=Tasks.await(user.getIdToken(false),12,TimeUnit.SECONDS).getToken();
      FirebaseUser current=auth().getCurrentUser();
      if(token==null||current==null||!current.getUid().equals(user.getUid())||!current.isEmailVerified())throw new Required();
      return token;
    }catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException("Account interrupted");}
    catch(java.util.concurrent.ExecutionException|java.util.concurrent.TimeoutException error){throw new IOException("Account token unavailable");}
  }
  public static Interceptor interceptor(){return chain->{
    Request request=chain.request();String path=request.url().encodedPath();
    if(!BuildConfig.ADULT_ACCOUNTS_ENABLED||!(path.equals("/v1/assistant")||path.equals("/v1/images")||path.equals("/v1/answer-reports")
        ||(path.equals("/v1/capabilities") && request.tag(GatewayRequest.class)!=null)))return chain.proceed(request);
    if(!trusted(request.url()))throw new IOException("Untrusted account service");
    if(held())throw new Required();
    long revision=REVISION.get();String token=token();
    FirebaseUser user=auth().getCurrentUser();String uid=user==null?null:user.getUid();
    if(revision!=REVISION.get()||uid==null)throw new IOException("Account changed");
    Response response=chain.proceed(request.newBuilder().header("X-Zikra-Account-Token",token).build());
    FirebaseUser current=auth().getCurrentUser();
    if(revision!=REVISION.get()||current==null||!uid.equals(current.getUid())||held()){response.close();throw new IOException("Account changed");}
    return response;
  };}
}
