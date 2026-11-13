package com.zikra.app.maps;

import android.content.Context;
import android.content.pm.PackageManager;
import java.io.File;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;

/** Shared policy: honor tile HTTP expiry; osmdroid supplies seven days when headers are absent. */
public final class OsmMapPolicy {
  private OsmMapPolicy() {}

  public static void configure(Context context) {
    String version = "unknown";
    try { version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; }
    catch (PackageManager.NameNotFoundException unavailable) { }
    IConfigurationProvider config = Configuration.getInstance();
    config.setUserAgentValue("Zikra-Android/" + version
        + " (+https://github.com/Arkanzahir/Zikra-App; contact: arkanzahir46@gmail.com)");
    // Keep old cache files intact, but do not reuse expiries extended by the previous policy.
    File base = new File(context.getCacheDir(), "osm-http-v2");
    base.mkdirs();
    config.setOsmdroidBasePath(base);
    config.setOsmdroidTileCache(new File(base, "tiles"));
    config.setTileDownloadThreads((short) 2);
    config.setCacheMapTileOvershoot((short) 0);
    config.setExpirationOverrideDuration(null);
    config.setExpirationExtendedDuration(0);
  }
}
