package com.zikra.app.account;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zikra.app.BaseActivity;
import com.zikra.app.BuildConfig;
import com.zikra.app.R;
import com.zikra.app.ui.FeatureHeader;
import com.zikra.app.ui.RefinedUi;
import java.lang.ref.WeakReference;

/** Account authentication only. Email verification never grants age/guardian eligibility. */
public final class AccountActivity extends BaseActivity {
  private FirebaseAuth auth;
  private EditText email, password;
  private CheckBox consent;
  private TextView status;
  private boolean busy;
  private long nextEmailAt;
  private android.app.DatePickerDialog signupAgePicker;
  private final FirebaseAuth.AuthStateListener authState = ignored -> render();

  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    if (!BuildConfig.ACCOUNT_SIGN_IN_ENABLED) { finish(); return; }
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    setContentView(R.layout.activity_account);
    FeatureHeader header = findViewById(R.id.accountHeader);
    header.bind(0, 0, 0, 0, getString(R.string.account_title), null, 0, null);
    header.back().setOnClickListener(v -> finish());
    setupScreen(getString(R.string.account_title));
    RefinedUi.chrome(this, header, false);
    email = findViewById(R.id.accountEmail);
    password = findViewById(R.id.accountPassword);
    consent = findViewById(R.id.accountConsent);
    status = findViewById(R.id.accountStatus);
    try { auth = AccountAuth.get(this); }
    catch (IllegalStateException error) { toast(getString(R.string.account_closed)); finish(); return; }
    findViewById(R.id.accountSignIn).setOnClickListener(v -> authenticate(false));
    findViewById(R.id.accountCreate).setOnClickListener(v -> authenticate(true));
    findViewById(R.id.accountReset).setOnClickListener(v -> resetPassword());
    findViewById(R.id.accountVerify).setOnClickListener(v -> verifyEmail());
    findViewById(R.id.accountRefresh).setOnClickListener(v -> {
      FirebaseUser user = auth.getCurrentUser();
      if (!busy && user != null) complete(user.reload(), 0);
    });
    findViewById(R.id.accountSignOut).setOnClickListener(v -> {
      if (busy) return;
      AccountSession.invalidate();auth.signOut(); password.setText(""); consent.setChecked(false); render();
    });
    findViewById(R.id.accountAdultEntry).setOnClickListener(v -> startActivity(new android.content.Intent(this,AdultAccountActivity.class)));
    findViewById(R.id.accountReauthenticate).setOnClickListener(v -> reauthenticate());
    render();
  }

  private void reauthenticate(){
    FirebaseUser user=auth.getCurrentUser();if(busy||user==null||user.getEmail()==null)return;
    EditText secret=new EditText(this);secret.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    secret.setSaveEnabled(false);secret.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
    new android.app.AlertDialog.Builder(this).setTitle(R.string.adult_reauthenticate).setView(secret)
        .setNegativeButton(R.string.contextual_cancel,(d,w)->secret.setText(""))
        .setPositiveButton(R.string.account_signin,(d,w)->{
          String value=secret.getText().toString();secret.setText("");AccountSession.invalidate();
          complete(user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(),value)),0);
        }).setOnDismissListener(d->secret.setText("")).show();
  }

  @Override protected void onStart() {
    super.onStart(); if (auth != null) auth.addAuthStateListener(authState);
  }
  @Override protected void onStop() {
    if (auth != null) auth.removeAuthStateListener(authState);
    if (password != null) password.setText("");
    super.onStop();
  }
  @Override protected void onDestroy(){if(signupAgePicker!=null)signupAgePicker.dismiss();super.onDestroy();}
  @Override protected boolean usesShakeShortcut() { return false; }

  private boolean validEmail() {
    return email.length() <= 254 && Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches();
  }
  private boolean accepted() {
    if (consent.isChecked()) return true;
    status.setText(R.string.account_check_consent); return false;
  }
  private void authenticate(boolean create) {
    if (busy || !accepted()) return;
    if (!validEmail() || password.length() < (create ? 12 : 1)) {
      status.setText(R.string.account_check_input); return;
    }
    if(create&&BuildConfig.ADULT_ACCOUNTS_ENABLED){
      java.time.LocalDate today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta"));
      android.app.DatePickerDialog picker=new android.app.DatePickerDialog(this,(v,y,m,d)->{
        if(!AdultAgePolicy.adult(java.time.LocalDate.of(y,m+1,d),today)){status.setText(R.string.adult_under18);return;}
        performAuthentication(true);
      },today.getYear(),today.getMonthValue()-1,today.getDayOfMonth());
      signupAgePicker=picker;picker.setTitle(R.string.adult_signup_age);picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();return;
    }
    performAuthentication(create);
  }
  private void performAuthentication(boolean create){
    if(busy||!accepted()||!validEmail()||password.length()<(create?12:1)){status.setText(R.string.account_check_input);return;}
    AccountSession.invalidate();
    String address = email.getText().toString().trim();
    String secret = password.getText().toString();
    password.setText("");
    complete(create ? auth.createUserWithEmailAndPassword(address, secret)
        : auth.signInWithEmailAndPassword(address, secret), 0);
  }
  private boolean emailAllowed() {
    if (SystemClock.elapsedRealtime() >= nextEmailAt) return true;
    status.setText(R.string.account_wait_email); return false;
  }
  private void resetPassword() {
    if (busy || !accepted() || !emailAllowed()) return;
    if (!validEmail()) { status.setText(R.string.account_check_input); return; }
    nextEmailAt = SystemClock.elapsedRealtime() + 60000;
    complete(auth.sendPasswordResetEmail(email.getText().toString().trim()), R.string.account_email_sent);
  }
  private void verifyEmail() {
    FirebaseUser user = auth.getCurrentUser();
    if (busy || user == null || user.isEmailVerified() || !emailAllowed()) return;
    nextEmailAt = SystemClock.elapsedRealtime() + 60000;
    complete(user.sendEmailVerification(), R.string.account_email_sent);
  }
  private void complete(Task<?> task, int successMessage) {
    busy = true; render();
    WeakReference<AccountActivity> reference = new WeakReference<>(this);
    task.addOnCompleteListener(result -> {
      AccountActivity screen = reference.get();
      if (screen == null || screen.isDestroyed() || screen.isFinishing()) return;
      screen.busy = false; screen.render();
      if (!result.isSuccessful()) {
        Exception error = result.getException();
        screen.status.setText(error instanceof FirebaseTooManyRequestsException ? R.string.account_rate_error
            : error instanceof FirebaseNetworkException ? R.string.account_network_error : R.string.account_error);
      } else if (successMessage != 0) screen.status.setText(successMessage);
    });
  }
  private void render() {
    if (auth == null || status == null) return;
    FirebaseUser user = auth.getCurrentUser();
    boolean signedIn = user != null, verified = signedIn && user.isEmailVerified();
    TextView identity = findViewById(R.id.accountIdentity);
    identity.setVisibility(signedIn ? View.VISIBLE : View.GONE);
    identity.setText(signedIn ? getString(R.string.account_identity, user.getEmail()) : "");
    findViewById(R.id.accountForm).setVisibility(signedIn ? View.GONE : View.VISIBLE);
    findViewById(R.id.accountVerify).setVisibility(signedIn && !verified ? View.VISIBLE : View.GONE);
    findViewById(R.id.accountRefresh).setVisibility(signedIn && !verified ? View.VISIBLE : View.GONE);
    findViewById(R.id.accountSignOut).setVisibility(signedIn ? View.VISIBLE : View.GONE);
    findViewById(R.id.accountAdultEntry).setVisibility(verified&&BuildConfig.ADULT_ACCOUNTS_ENABLED?View.VISIBLE:View.GONE);
    findViewById(R.id.accountReauthenticate).setVisibility(signedIn?View.VISIBLE:View.GONE);
    for (int id : new int[] {R.id.accountSignIn, R.id.accountCreate, R.id.accountReset,
        R.id.accountVerify, R.id.accountRefresh, R.id.accountSignOut, R.id.accountAdultEntry, R.id.accountReauthenticate}) findViewById(id).setEnabled(!busy);
    status.setText(busy ? R.string.account_busy : verified ? R.string.account_verified
        : signedIn ? R.string.account_signed_in : R.string.account_signed_out);
  }
}
