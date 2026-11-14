package com.zikra.app.assistant;

import android.content.SharedPreferences;

/** Basic online permission is bound to the reviewed service, never to extra personal context. */
public final class AssistantConsent {
  private static final String ENDPOINT = "assistant_basic_consent_cloudflare_endpoint_v2";
  private static final String REVISION = "assistant_basic_consent_revision_v1";
  private AssistantConsent() {}
  public static boolean granted(SharedPreferences prefs, String endpoint) {
    return endpoint != null && !endpoint.isEmpty() && endpoint.equals(prefs.getString(ENDPOINT, ""));
  }
  public static void accept(SharedPreferences prefs, String endpoint) {
    if (endpoint == null || endpoint.isEmpty()) throw new IllegalArgumentException("Missing service");
    prefs.edit().putString(ENDPOINT, endpoint).putString(REVISION, java.util.UUID.randomUUID().toString()).apply();
  }
  public static void revoke(SharedPreferences prefs) { prefs.edit().remove(ENDPOINT).putString(REVISION, java.util.UUID.randomUUID().toString()).apply(); }
  /** A reviewed request cannot acquire a later grant or a different service credential. */
  static String revision(SharedPreferences prefs) { return prefs.getString(REVISION, "legacy-v2"); }
  public static final class Permit {
    private final String endpoint, revision, credential;
    private Permit(String endpoint, String revision, String credential) {
      this.endpoint = endpoint; this.revision = revision; this.credential = credential;
    }
    public boolean matches(SharedPreferences prefs, String currentEndpoint, String currentCredential) {
      return granted(prefs, currentEndpoint) && endpoint.equals(currentEndpoint)
          && revision.equals(revision(prefs)) && credential.equals(currentCredential);
    }
  }
  public static Permit capture(SharedPreferences prefs, String endpoint, String credential) {
    if (!granted(prefs, endpoint) || credential == null || credential.isEmpty()) return null;
    return new Permit(endpoint, revision(prefs), credential);
  }
}
