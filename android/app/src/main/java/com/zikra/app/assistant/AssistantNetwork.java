package com.zikra.app.assistant;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

/** Network changes update status only; they never send a queued message. */
public final class AssistantNetwork implements AutoCloseable {
  public enum State { OFFLINE, UNVALIDATED, CAPTIVE_PORTAL, ONLINE }

  private final ConnectivityManager manager;
  private final Handler main=new Handler(Looper.getMainLooper());
  private final Runnable changed;
  private boolean registered;
  private Network currentNetwork;
  private State currentState = State.OFFLINE;
  private final ConnectivityManager.NetworkCallback callback=new ConnectivityManager.NetworkCallback(){
    @Override public void onAvailable(Network network){if(registered)currentNetwork=network;}
    @Override public void onLost(Network network){
      if(registered&&network.equals(currentNetwork)){currentNetwork=null;publish(State.OFFLINE);}
    }
    @Override public void onCapabilitiesChanged(Network network,NetworkCapabilities caps){
      if(registered&&network.equals(currentNetwork))publish(classify(caps));
    }
  };
  public AssistantNetwork(Context context,Runnable changed){
    manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);this.changed=changed;
  }
  static State classify(NetworkCapabilities caps){
    if(caps==null)return State.OFFLINE;
    if(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL))return State.CAPTIVE_PORTAL;
    if(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        &&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))return State.ONLINE;
    return State.UNVALIDATED;
  }
  public State state(){
    if(registered)return currentState;
    if(manager==null)return State.OFFLINE;
    try{return classify(manager.getNetworkCapabilities(manager.getActiveNetwork()));}
    catch(SecurityException unavailable){return State.OFFLINE;}
  }
  public boolean validated(){return state()==State.ONLINE;}
  private void publish(State state){
    if(currentState!=state){currentState=state;changed.run();}
  }
  public void start(){
    if(manager!=null&&!registered)try{
      currentState=state();currentNetwork=manager.getActiveNetwork();
      manager.registerDefaultNetworkCallback(callback,main);registered=true;
    }catch(SecurityException ignored){}
  }
  @Override public void close(){
    if(registered){try{manager.unregisterNetworkCallback(callback);}catch(IllegalArgumentException ignored){}registered=false;}
    currentNetwork=null;
  }
}
