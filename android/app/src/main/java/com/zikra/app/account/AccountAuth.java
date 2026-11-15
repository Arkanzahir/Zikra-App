package com.zikra.app.account;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.zikra.app.BuildConfig;

final class AccountAuth {
  private static FirebaseAuth instance;
  private AccountAuth() {}
  static synchronized FirebaseAuth get(Context context) {
    if (instance != null) return instance;
    if (!BuildConfig.ACCOUNT_AUTH_EMULATOR) {
      instance = FirebaseAuth.getInstance(); instance.setLanguageCode("id"); return instance;
    }
    if (!BuildConfig.DEBUG) throw new IllegalStateException("Emulator requires debug");
    FirebaseOptions options = new FirebaseOptions.Builder()
        .setProjectId("demo-zikra-accounts")
        .setApplicationId("1:1234567890:android:0123456789abcdef")
        .setApiKey("AIzaSyntheticLocalEmulatorKeyOnly123456789")
        .build();
    FirebaseApp app = FirebaseApp.initializeApp(context.getApplicationContext(), options, "zikra-auth-local");
    FirebaseAuth auth = FirebaseAuth.getInstance(app);
    auth.useEmulator("10.0.2.2", 9099);
    auth.setLanguageCode("id");
    return instance = auth;
  }
}
