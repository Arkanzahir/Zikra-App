package com.zikra.app.location;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Optional foreground lookup; failure never invalidates a coordinate or changes its time zone. */
public final class DevicePlaceName {
  private DevicePlaceName() {}

  public static LocationPoint resolve(Context context, LocationPoint point) {
    if (!"device".equals(point.source) || !Geocoder.isPresent()) return point;
    ExecutorService legacy = null;
    try {
      Geocoder geocoder =
          new Geocoder(context.getApplicationContext(), Locale.forLanguageTag("id-ID"));
      CompletableFuture<List<Address>> result = new CompletableFuture<>();
      if (Build.VERSION.SDK_INT >= 33) {
        geocoder.getFromLocation(
            point.latitude,
            point.longitude,
            1,
            new Geocoder.GeocodeListener() {
              @Override
              public void onGeocode(List<Address> addresses) {
                result.complete(addresses);
              }

              @Override
              public void onError(String message) {
                result.complete(java.util.Collections.emptyList());
              }
            });
      } else {
        legacy = Executors.newSingleThreadExecutor();
        legacy.execute(
            () -> {
              try {
                result.complete(geocoder.getFromLocation(point.latitude, point.longitude, 1));
              } catch (Exception unavailable) {
                result.complete(java.util.Collections.emptyList());
              }
            });
      }
      List<Address> addresses = result.get(4, TimeUnit.SECONDS);
      if (addresses == null || addresses.isEmpty()) return point;
      Address address = addresses.get(0);
      if (!address.hasLatitude()
          || !address.hasLongitude()
          || !Double.isFinite(address.getLatitude())
          || !Double.isFinite(address.getLongitude())
          || point.distanceTo(address.getLatitude(), address.getLongitude()) > 10000) return point;
      String label =
          PlaceNames.label(
              address.getSubLocality(),
              address.getLocality(),
              address.getSubAdminArea(),
              address.getAdminArea());
      return new LocationPoint(
          point.latitude,
          point.longitude,
          point.zone.getId(),
          label,
          point.source,
          point.selectedAt,
          point.accuracy);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return point;
    } catch (Exception unavailable) {
      return point;
    } finally {
      if (legacy != null) legacy.shutdownNow();
    }
  }
}
