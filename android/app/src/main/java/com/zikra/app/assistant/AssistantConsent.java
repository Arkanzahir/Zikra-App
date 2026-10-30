package com.zikra.app.assistant;

import android.content.SharedPreferences;

/** Basic online permission is bound to the reviewed service, never to extra personal context. */
public final class AssistantConsent {
  private static final String ENDPOINT = "assistant_basic_consent_endpoint_v1";
  private AssistantConsent() {}
  public static boolean granted(SharedPreferences prefs, String endpoint) {
    return endpoint != null && !endpoint.isEmpty() && endpoint.equals(prefs.getString(ENDPOINT, ""));
  }
  public static void accept(SharedPreferences prefs, String endpoint) {
    if (endpoint == null || endpoint.isEmpty()) throw new IllegalArgumentException("Missing service");
    prefs.edit().putString(ENDPOINT, endpoint).apply();
  }
  public static void revoke(SharedPreferences prefs) { prefs.edit().remove(ENDPOINT).apply(); }
}
