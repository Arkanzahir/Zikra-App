package com.zikra.app.assistant;

import static org.junit.Assert.*;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class AssistantProviderConsentTest {
  @Test
  public void legacyPermissionCannotAuthorizeNewProviderAndRevocationKeepsLegacyData() {
    Map<String, String> values = new HashMap<>();
    String endpoint = "https://example.test/v1/assistant";
    values.put("assistant_basic_consent_endpoint_v1", endpoint);
    SharedPreferences.Editor edit =
        (SharedPreferences.Editor)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {SharedPreferences.Editor.class},
                (proxy, method, args) -> {
                  switch (method.getName()) {
                    case "putString": values.put((String) args[0], (String) args[1]); return proxy;
                    case "remove": values.remove(args[0]); return proxy;
                    case "apply": return null;
                    default: throw new UnsupportedOperationException(method.getName());
                  }
                });
    SharedPreferences prefs =
        (SharedPreferences)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {SharedPreferences.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("getString")) return values.getOrDefault(args[0], (String) args[1]);
                  if (method.getName().equals("edit")) return edit;
                  throw new UnsupportedOperationException(method.getName());
                });
    assertFalse(AssistantConsent.granted(prefs, endpoint));
    AssistantConsent.accept(prefs, endpoint);
    assertTrue(AssistantConsent.granted(prefs, endpoint));
    assertFalse(AssistantConsent.granted(prefs, "https://different.test/v1/assistant"));
    AssistantConsent.revoke(prefs);
    assertFalse(AssistantConsent.granted(prefs, endpoint));
    assertEquals(endpoint, values.get("assistant_basic_consent_endpoint_v1"));
  }
}
