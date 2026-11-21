package com.zikra.app.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.zikra.app.BuildConfig;
import com.zikra.app.account.AccountSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import okhttp3.HttpUrl;
import okhttp3.Request;

/** Private gateway credentials are separate from upstream provider credentials. */
public final class AssistantConnection {
  private static final String KEY = "zikra-assistant-gateway-v1";
  private final SharedPreferences prefs, consent;

  public AssistantConnection(Context context) {
    prefs = context.getSharedPreferences("assistant_gateway", 0);
    consent = context.getSharedPreferences("zikra_preferences", 0);
  }

  private String credentialIdentity() { return prefs.getString("iv", "") + ":" + prefs.getString("access", "")
      + (com.zikra.app.BuildConfig.ADULT_ACCOUNTS_ENABLED ? ":"+com.zikra.app.account.AccountSession.binding() : ""); }
  public AssistantConsent.Permit permission() { return configured() ? AssistantConsent.capture(consent, endpoint(), credentialIdentity()) : null; }
  public boolean permits(AssistantConsent.Permit permit) {
    return configured() && !"offline".equals(consent.getString("assistant_mode_v2", "auto")) && permit != null && permit.matches(consent, endpoint(), credentialIdentity());
  }

  /** Image confirmation is per prompt; it does not silently grant basic chat permission. */
  public static final class ServicePermit {
    private final String endpoint, credential, revision;
    private ServicePermit(String endpoint, String credential, String revision) {
      this.endpoint = endpoint; this.credential = credential; this.revision = revision;
    }
  }
  public ServicePermit servicePermission() {
    return configured() ? new ServicePermit(endpoint(), credentialIdentity(), AssistantConsent.revision(consent)) : null;
  }
  public boolean permitsImage(ServicePermit permit) { return permitsService(permit); }
  public boolean permitsService(ServicePermit permit) {
    return configured() && !"offline".equals(consent.getString("assistant_mode_v2", "auto")) && permit != null
        && endpoint().equals(permit.endpoint) && credentialIdentity().equals(permit.credential)
        && AssistantConsent.revision(consent).equals(permit.revision);
  }

  public boolean hasInstallation() {
    return !prefs.getString("endpoint", "").isEmpty() && prefs.contains("access") && prefs.contains("iv");
  }

  public boolean usesAccountGateway() {
    return BuildConfig.ADULT_ACCOUNTS_ENABLED && !hasInstallation();
  }

  public String endpoint() {
    if (!usesAccountGateway()) return prefs.getString("endpoint", "");
    return AccountSession.serviceBase() + "/v1/assistant";
  }

  public boolean online() {
    return configured() && prefs.getBoolean("online", usesAccountGateway());
  }

  public boolean configured() {
    return hasInstallation() || (usesAccountGateway() && AccountSession.hasVerifiedUser());
  }

  Request.Builder request(String route) throws Exception {
    HttpUrl url = HttpUrl.parse(endpoint());
    if (usesAccountGateway()) {
      if (!AccountSession.trusted(url) || !configured()) throw new IOException("Account gateway unavailable");
      return new Request.Builder().url(url.newBuilder().encodedPath(route).build())
          .tag(AccountSession.GatewayRequest.class, AccountSession.GatewayRequest.INSTANCE);
    }
    String code = accessCode();
    if (!valid(endpoint(), code)) throw new IOException("Invalid gateway");
    return new Request.Builder().url(url.newBuilder().encodedPath(route).build())
        .header("Authorization", "Bearer " + code);
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
        && !url.host().equals("api.cloudflare.com")
        && accessCode != null
        && accessCode.length() >= 32
        && accessCode.length() <= 256
        && accessCode.matches("[A-Za-z0-9._~+/=\\-]+")
        && !accessCode.startsWith("sk-") && !accessCode.startsWith("AIza") && !accessCode.startsWith("cfat_");
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
