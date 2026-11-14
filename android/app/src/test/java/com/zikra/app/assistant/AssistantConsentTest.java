package com.zikra.app.assistant;

import android.content.SharedPreferences;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AssistantConsentTest {
  private static final String ENDPOINT = "https://example.test/v1/assistant";
  @Test public void noGrantCannotAuthorizeARequest() {
    Memory prefs = new Memory(); assertNull(AssistantConsent.capture(prefs, ENDPOINT, "credential"));
  }
  @Test public void acceptedRequestRetainsItsReviewedBinding() {
    Memory prefs = new Memory(); AssistantConsent.accept(prefs, ENDPOINT);
    AssistantConsent.Permit permit = AssistantConsent.capture(prefs, ENDPOINT, "credential");
    assertNotNull(permit); assertTrue(permit.matches(prefs, ENDPOINT, "credential"));
    assertFalse(permit.matches(prefs, "https://other.test/v1/assistant", "credential"));
    assertFalse(permit.matches(prefs, ENDPOINT, "replacement credential"));
  }
  @Test public void revocationInvalidatesPreviouslyReviewedData() {
    Memory prefs = new Memory(); AssistantConsent.accept(prefs, ENDPOINT);
    AssistantConsent.Permit permit = AssistantConsent.capture(prefs, ENDPOINT, "credential");
    AssistantConsent.revoke(prefs); assertFalse(permit.matches(prefs, ENDPOINT, "credential"));
  }
  @Test public void reacceptingCannotResurrectAnOldPendingRequest() {
    Memory prefs = new Memory(); AssistantConsent.accept(prefs, ENDPOINT);
    AssistantConsent.Permit previous = AssistantConsent.capture(prefs, ENDPOINT, "credential");
    AssistantConsent.revoke(prefs); AssistantConsent.accept(prefs, ENDPOINT);
    assertFalse(previous.matches(prefs, ENDPOINT, "credential"));
    assertTrue(AssistantConsent.capture(prefs, ENDPOINT, "credential").matches(prefs, ENDPOINT, "credential"));
  }
  @Test public void validLegacyConsentDoesNotCauseAnotherForcedDialog() {
    Memory prefs = new Memory(); prefs.values.put("assistant_basic_consent_cloudflare_endpoint_v2", ENDPOINT);
    assertTrue(AssistantConsent.granted(prefs, ENDPOINT));
    AssistantConsent.Permit legacy = AssistantConsent.capture(prefs, ENDPOINT, "credential");
    assertTrue(legacy.matches(prefs, ENDPOINT, "credential"));
    AssistantConsent.revoke(prefs); AssistantConsent.accept(prefs, ENDPOINT);
    assertFalse(legacy.matches(prefs, ENDPOINT, "credential"));
  }
  @Test public void UnrelatedSettingsDoNotInvalidatePermission() {
    Memory prefs = new Memory(); AssistantConsent.accept(prefs, ENDPOINT);
    AssistantConsent.Permit permit = AssistantConsent.capture(prefs, ENDPOINT, "credential");
    prefs.edit().putString("profile_display_name", "Synthetic").apply();
    assertTrue(permit.matches(prefs, ENDPOINT, "credential"));
  }
  private static final class Memory implements SharedPreferences {
    final Map<String, Object> values = new HashMap<>();
    public Map<String, ?> getAll() { return new HashMap<>(values); }
    public String getString(String k, String d) { return (String) values.getOrDefault(k, d); }
    @SuppressWarnings("unchecked") public Set<String> getStringSet(String k, Set<String> d) { return (Set<String>) values.getOrDefault(k,d); }
    public int getInt(String k,int d) { return (Integer) values.getOrDefault(k,d); }
    public long getLong(String k,long d) { return (Long) values.getOrDefault(k,d); }
    public float getFloat(String k,float d) { return (Float) values.getOrDefault(k,d); }
    public boolean getBoolean(String k,boolean d) { return (Boolean) values.getOrDefault(k,d); }
    public boolean contains(String k) { return values.containsKey(k); }
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
    public Editor edit() { return new Editor() {
      final Map<String,Object> changes = new HashMap<>(); boolean clear;
      public Editor putString(String k,String v) { changes.put(k,v);return this; }
      public Editor putStringSet(String k,Set<String> v) { changes.put(k,v);return this; }
      public Editor putInt(String k,int v) { changes.put(k,v);return this; }
      public Editor putLong(String k,long v) { changes.put(k,v);return this; }
      public Editor putFloat(String k,float v) { changes.put(k,v);return this; }
      public Editor putBoolean(String k,boolean v) { changes.put(k,v);return this; }
      public Editor remove(String k) { changes.put(k,null);return this; }
      public Editor clear() { clear=true;return this; }
      public void apply() { commit(); }
      public boolean commit() { if(clear)values.clear();changes.forEach((k,v)->{if(v==null)values.remove(k);else values.put(k,v);});return true; }
    }; }
  }
}
