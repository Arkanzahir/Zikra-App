package com.zikra.app.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import okhttp3.HttpUrl;

/** User-configured private gateway. A Featherless provider key is never accepted here. */
public final class AssistantConnection {
  private static final String KEY = "zikra-assistant-gateway-v1";
  private final SharedPreferences prefs;

  public AssistantConnection(Context context) {
    prefs = context.getSharedPreferences("assistant_gateway", 0);
  }

  public String endpoint() {
    return prefs.getString("endpoint", "");
  }

  public boolean online() {
    return prefs.getBoolean("online", false) && !endpoint().isEmpty();
  }

  public boolean configured() {
    return !endpoint().isEmpty() && prefs.contains("access") && prefs.contains("iv");
  }

  public void activate() {
    if (configured()) prefs.edit().putBoolean("online", true).apply();
  }

  public void offline() {
    prefs.edit().putBoolean("online", false).apply();
  }

  public static boolean valid(String value, String accessCode) {
    HttpUrl url = HttpUrl.parse(value);
    return url != null
        && url.isHttps()
        && url.username().isEmpty()
        && url.password().isEmpty()
        && url.query() == null
        && url.fragment() == null
        && url.encodedPath().equals("/v1/assistant")
        && !url.host().equals("api.featherless.ai")
        && accessCode != null
        && accessCode.length() >= 32
        && accessCode.length() <= 256
        && accessCode.matches("[A-Za-z0-9._~+/=\\-]+")
        && !accessCode.startsWith("sk-") && !accessCode.startsWith("AIza");
  }

  private SecretKey key(boolean create) throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (store.containsAlias(KEY)) return (SecretKey) store.getKey(KEY, null);
    if (!create) throw new IllegalStateException("Missing gateway key");
    KeyGenerator generator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
    generator.init(
        new KeyGenParameterSpec.Builder(
                KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build());
    return generator.generateKey();
  }

  public void save(String endpoint, String accessCode) throws Exception {
    if (!valid(endpoint, accessCode)) throw new IllegalArgumentException("Invalid gateway");
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key(true));
    String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
    String encrypted =
        Base64.encodeToString(
            cipher.doFinal(accessCode.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    prefs
        .edit()
        .putString("endpoint", endpoint)
        .putString("iv", iv)
        .putString("access", encrypted)
        .putBoolean("online", true)
        .apply();
  }

  public String accessCode() throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE,
        key(false),
        new GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)));
    return new String(
        cipher.doFinal(Base64.decode(prefs.getString("access", ""), Base64.NO_WRAP)),
        StandardCharsets.UTF_8);
  }
}
