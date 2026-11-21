package com.zikra.app.account;

import android.content.Intent;
import android.os.Build;
import android.test.InstrumentationTestCase;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zikra.app.BuildConfig;
import com.zikra.app.R;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/** Official Firebase Auth emulator only. No external email or production project. */
@SuppressWarnings("deprecation")
public final class AccountAuthEmulatorTest extends InstrumentationTestCase {
  private AccountActivity page;
  private FirebaseAuth auth;
  private FirebaseUser ownedUser;
  private String address, secret;
  @Override protected void setUp() throws Exception {
    super.setUp();
    assertTrue(BuildConfig.DEBUG && BuildConfig.ACCOUNT_SIGN_IN_ENABLED && BuildConfig.ACCOUNT_AUTH_EMULATOR);
    assertTrue(Build.MODEL.toLowerCase(java.util.Locale.ROOT).contains("sdk"));
    assertTrue(new File(getInstrumentation().getTargetContext().getFilesDir(), "account-emulator-authorized").isFile());
    auth = AccountAuth.get(getInstrumentation().getTargetContext());
    assertEquals("demo-zikra-accounts", auth.getApp().getOptions().getProjectId());
    assertNull(auth.getCurrentUser());
    address = "zikra-e2e-" + UUID.randomUUID() + "@example.test";
    secret = "Local-test-" + UUID.randomUUID();
    page = (AccountActivity)getInstrumentation().startActivitySync(new Intent(
        getInstrumentation().getTargetContext(), AccountActivity.class).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
  }
  @Override protected void tearDown() throws Exception {
    try {
      if (ownedUser == null && auth != null && auth.getCurrentUser() != null
          && address.equals(auth.getCurrentUser().getEmail())) ownedUser = auth.getCurrentUser();
      try { if (ownedUser != null) Tasks.await(ownedUser.delete(), 20, TimeUnit.SECONDS); }
      finally {
        if (auth != null) auth.signOut();
        if (page != null) getInstrumentation().runOnMainSync(page::finish);
      }
    } finally { secret = null; address = null; super.tearDown(); }
  }
  private void fill() {
    getInstrumentation().runOnMainSync(() -> {
      ((EditText)page.findViewById(R.id.accountEmail)).setText(address);
      ((EditText)page.findViewById(R.id.accountPassword)).setText(secret);
      ((CheckBox)page.findViewById(R.id.accountConsent)).setChecked(true);
    });
  }
  private void click(int id) { getInstrumentation().runOnMainSync(() -> page.findViewById(id).performClick()); }
  private void waitForText(int resource) throws Exception {
    long until = android.os.SystemClock.elapsedRealtime() + 30000;
    while (android.os.SystemClock.elapsedRealtime() < until) {
      getInstrumentation().waitForIdleSync();
      final boolean[] matched = { false };
      getInstrumentation().runOnMainSync(() -> matched[0] = page.getString(resource).contentEquals(
          ((TextView)page.findViewById(R.id.accountStatus)).getText()));
      if (matched[0]) return;
      Thread.sleep(100);
    }
    fail("Expected local authentication screen state was not reached");
  }
  private void waitForUser() throws Exception {
    long end=android.os.SystemClock.elapsedRealtime()+20000;
    while(android.os.SystemClock.elapsedRealtime()<end) {
      if(auth.getCurrentUser()!=null)return;
      Thread.sleep(100);
    }
    fail("Local sign-in did not return an authenticated user");
  }
  private String actionCode(String type) throws Exception {
    HttpURLConnection connection = (HttpURLConnection)new URL(
        "http://10.0.2.2:9099/emulator/v1/projects/demo-zikra-accounts/oobCodes").openConnection();
    connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
    try {
      assertEquals(200, connection.getResponseCode());
      java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
      try (java.io.InputStream input = connection.getInputStream()) {
        byte[] chunk = new byte[4096]; int read;
        while ((read = input.read(chunk)) != -1) {
          assertTrue(output.size() + read <= 65536); output.write(chunk,0,read);
        }
      }
      byte[] data = output.toByteArray();
      JSONArray codes = new JSONObject(new String(data, StandardCharsets.UTF_8)).getJSONArray("oobCodes");
      for (int i = 0; i < codes.length(); i++) {
        JSONObject item = codes.getJSONObject(i);
        if (address.equals(item.optString("email")) && type.equals(item.optString("requestType")))
          return item.getString("oobCode");
      }
      throw new AssertionError("No local action code for owned synthetic account");
    } finally { connection.disconnect(); }
  }
  public void testSignupVerificationSignoutAndLogin() throws Exception {
    fill(); click(R.id.accountCreate);
    if (BuildConfig.ADULT_ACCOUNTS_ENABLED) {
      java.lang.reflect.Field agePicker = AccountActivity.class.getDeclaredField("signupAgePicker");
      agePicker.setAccessible(true);
      android.app.DatePickerDialog picker = (android.app.DatePickerDialog) agePicker.get(page);
      assertNotNull(picker);
      java.time.LocalDate birth = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta")).minusYears(19);
      getInstrumentation().runOnMainSync(() -> {
        picker.getDatePicker().updateDate(birth.getYear(), birth.getMonthValue() - 1, birth.getDayOfMonth());
        picker.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
      });
    }
    waitForText(R.string.account_signed_in);
    ownedUser = auth.getCurrentUser(); assertNotNull(ownedUser); assertFalse(ownedUser.isEmailVerified());
    getInstrumentation().runOnMainSync(() -> assertTrue(((TextView)page.findViewById(R.id.accountIdentity))
        .getText().toString().contains(address)));
    click(R.id.accountVerify); waitForText(R.string.account_email_sent);
    Tasks.await(auth.applyActionCode(actionCode("VERIFY_EMAIL")), 20, TimeUnit.SECONDS);
    click(R.id.accountRefresh); waitForText(R.string.account_verified);
    assertTrue(auth.getCurrentUser().isEmailVerified());
    click(R.id.accountSignOut); waitForText(R.string.account_signed_out); assertNull(auth.getCurrentUser());
    fill(); click(R.id.accountSignIn); waitForText(R.string.account_verified);
    assertEquals(ownedUser.getUid(), auth.getCurrentUser().getUid());
  }
  public void testPasswordResetUsesLocalOutboxOnly() throws Exception {
    ownedUser = Tasks.await(auth.createUserWithEmailAndPassword(address,secret),20,TimeUnit.SECONDS).getUser();
    auth.signOut(); waitForText(R.string.account_signed_out);
    fill(); click(R.id.accountReset); waitForText(R.string.account_email_sent);
    StringBuilder longPassword = new StringBuilder("Changed-local-" + UUID.randomUUID());
    for (int n = 0; n < 200; n++) longPassword.append('x');
    String nextPassword = longPassword.toString();
    Tasks.await(auth.confirmPasswordReset(actionCode("PASSWORD_RESET"),nextPassword),20,TimeUnit.SECONDS);
    secret=nextPassword;fill();
    getInstrumentation().runOnMainSync(()->assertEquals(secret.length(),
        ((EditText)page.findViewById(R.id.accountPassword)).length()));
    click(R.id.accountSignIn);waitForUser();
    ownedUser=auth.getCurrentUser();
    assertNotNull(ownedUser);
    waitForText(ownedUser.isEmailVerified()?R.string.account_verified:R.string.account_signed_in);
  }
}
