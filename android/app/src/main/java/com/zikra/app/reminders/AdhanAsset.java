package com.zikra.app.reminders;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import java.io.IOException;

public final class AdhanAsset {
  private AdhanAsset() {}
  public static AssetFileDescriptor open(Context context,int index)throws IOException {
    if (!com.zikra.app.PrayerSchedule.isPrayer(index)) throw new IOException("Invalid prayer");
    if (LocalAdhanAudio.available(context, index)) {
      java.io.File audio = LocalAdhanAudio.file(context, index);
      android.os.ParcelFileDescriptor descriptor=android.os.ParcelFileDescriptor.open(audio,
          android.os.ParcelFileDescriptor.MODE_READ_ONLY);
      return new AssetFileDescriptor(descriptor, 0, descriptor.getStatSize());
    }
    if (com.zikra.app.BuildConfig.PUBLIC_BETA_SCOPE) {
      if (index == 0) throw new IOException("No bundled Fajr recording");
      return context.getAssets().openFd("bundled-adhan/regular.mp3");
    }
    return context.getAssets().openFd(index==0?"adhan/fajr.mp3":"adhan/selected.mp3");
  }
  public static boolean available(Context context,int index) {
    try(AssetFileDescriptor file=open(context,index)){return file.getLength()>0&&file.getLength()<=10000000;}
    catch(IOException unavailable){return false;}
  }
}
