package com.zikra.app.mosque;

import android.content.Context;
import android.content.SharedPreferences;
import com.zikra.app.location.LocationPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Explicit OSM lookup; stale results retain their original location and date. */
public final class MosqueSearch implements AutoCloseable {
  public static final int RADIUS=3000;
  private static final long FRESH_MS=30*60_000L,STALE_MS=7*24*60*60_000L;
  private final Context context;
  private final OverpassClient transport=new OverpassClient();
  private volatile int generation;
  private volatile boolean closed;
  public MosqueSearch(Context context){this.context=context.getApplicationContext();}
  public static final class Result {
    public final List<MosquePlace> places;
    public final long fetchedAt;
    public final boolean cached,stale;
    public Result(List<MosquePlace> places,long at,boolean cached){this(places,at,cached,false);}
    private Result(List<MosquePlace> places,long at,boolean cached,boolean stale){
      this.places=Collections.unmodifiableList(new ArrayList<>(places));fetchedAt=at;this.cached=cached;this.stale=stale;
    }
  }
  public static final class RateLimited extends IOException {
    public final long retryAfterMs;
    private RateLimited(long delay){super("Places rate limited");retryAfterMs=delay;}
  }
  private Result cached(SharedPreferences cache,String key,LocationPoint origin,long maxAge,boolean stale){
    if(!key.equals(cache.getString("location","")))return null;
    long at=cache.getLong("fetched_at",0),age=System.currentTimeMillis()-at;
    if(age<0||age>=maxAge)return null;
    try{
      List<MosquePlace> places=MosquePlace.parse(cache.getString("json",""),"osm",origin,RADIUS);
      return stale&&places.isEmpty()?null:new Result(places,at,true,stale);
    }catch(Exception invalid){return null;}
  }
  public Result find(LocationPoint origin,boolean force) throws Exception {
    if(closed)throw new IOException("Places lookup closed");
    int token=generation;SharedPreferences cache=context.getSharedPreferences("zikra_mosques_osm",0);
    String key=origin.key()+":"+RADIUS;
    if(!force){Result fresh=cached(cache,key,origin,FRESH_MS,false);if(fresh!=null)return fresh;}
    try{
      long blocked=cache.getLong("retry_until",0)-System.currentTimeMillis();if(blocked>0)throw new RateLimited(blocked);
      String around="(around:"+RADIUS+","+origin.latitude+","+origin.longitude+")";
      String query="[out:json][timeout:15][maxsize:16777216];(nwr"+around
          +"[amenity=place_of_worship][religion=muslim];nwr"+around+"[building=mosque];);out center 100;";
      String json;
      try{json=transport.fetch(query);}
      catch(OverpassClient.RateLimited limited){
        cache.edit().putLong("retry_until",System.currentTimeMillis()+limited.retryAfterMs).apply();
        throw new RateLimited(limited.retryAfterMs);
      }
      List<MosquePlace> places=MosquePlace.parse(json,"osm",origin,RADIUS);
      check(token);long at=System.currentTimeMillis();
      cache.edit().putString("json",json).putString("location",key).putLong("fetched_at",at).remove("retry_until").apply();
      return new Result(places,at,false);
    }catch(Exception unavailable){
      check(token);Result previous=cached(cache,key,origin,STALE_MS,true);if(previous!=null)return previous;
      throw unavailable;
    }
  }
  private void check(int token) throws IOException {
    if(closed||token!=generation||Thread.currentThread().isInterrupted())throw new IOException("Places lookup cancelled");
  }
  public void cancel(){generation++;transport.cancel();}
  @Override public void close(){closed=true;generation++;transport.close();}
}
