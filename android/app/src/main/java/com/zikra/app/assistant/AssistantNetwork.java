package com.zikra.app.assistant;

import android.content.Context;
import android.net.*;
import android.os.Handler;
import android.os.Looper;

/** Network changes update status only; they never send a queued message. */
public final class AssistantNetwork implements AutoCloseable {
  private final ConnectivityManager manager;
  private final Handler main=new Handler(Looper.getMainLooper());
  private final Runnable changed;
  private boolean registered;
  private final ConnectivityManager.NetworkCallback callback=new ConnectivityManager.NetworkCallback(){
    @Override public void onAvailable(Network network){main.post(changed);}
    @Override public void onLost(Network network){main.post(changed);}
    @Override public void onCapabilitiesChanged(Network network,NetworkCapabilities caps){main.post(changed);}
  };
  public AssistantNetwork(Context context,Runnable changed){
    manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);this.changed=changed;
  }
  public boolean validated(){
    if(manager==null)return false;
    NetworkCapabilities caps=manager.getNetworkCapabilities(manager.getActiveNetwork());
    return caps!=null&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        &&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
  }
  public void start(){
    if(manager!=null&&!registered)try{manager.registerDefaultNetworkCallback(callback);registered=true;}catch(SecurityException ignored){}
  }
  @Override public void close(){
    if(registered){try{manager.unregisterNetworkCallback(callback);}catch(IllegalArgumentException ignored){}registered=false;}
    main.removeCallbacks(changed);
  }
}
