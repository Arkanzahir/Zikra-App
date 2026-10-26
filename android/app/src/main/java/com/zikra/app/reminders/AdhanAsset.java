package com.zikra.app.reminders;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import java.io.IOException;

public final class AdhanAsset {
  private AdhanAsset() {}
  public static AssetFileDescriptor open(Context context,int index)throws IOException {
    return context.getAssets().openFd(index==0?"adhan/fajr.mp3":"adhan/selected.mp3");
  }
  public static boolean available(Context context,int index) {
    try(AssetFileDescriptor file=open(context,index)){return file.getLength()>0&&file.getLength()<=10000000;}
    catch(IOException unavailable){return false;}
  }
}
