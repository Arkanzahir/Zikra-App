package com.zikra.app.account;

import android.content.Intent;
import android.os.Build;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.zikra.app.BuildConfig;
import com.zikra.app.R;
import java.io.File;

/** Isolated emulator only. No credentials, signup, email or real user accounts. */
@SuppressWarnings("deprecation")
public final class AccountScreenNativeTest extends InstrumentationTestCase {
  private AccountActivity page;
  @Override protected void setUp() throws Exception {
    super.setUp();
    String model = Build.MODEL.toLowerCase(java.util.Locale.ROOT);
    assertTrue(model.contains("sdk") || model.contains("ranchu")
        || Build.FINGERPRINT.toLowerCase(java.util.Locale.ROOT).contains("generic"));
    assertTrue(BuildConfig.ACCOUNT_SIGN_IN_ENABLED);
    assertTrue(new File(getInstrumentation().getTargetContext().getFilesDir(), "account-emulator-authorized").isFile());
    page = (AccountActivity) getInstrumentation().startActivitySync(new Intent(
        getInstrumentation().getTargetContext(), AccountActivity.class).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    getInstrumentation().waitForIdleSync();
  }
  @Override protected void tearDown() throws Exception {
    try { if (page != null) getInstrumentation().runOnMainSync(page::finish); }
    finally { super.tearDown(); }
  }
  public void testSignInHasItsOwnRowAboveSecondaryActions() {
    getInstrumentation().runOnMainSync(() -> {
      View signIn = page.findViewById(R.id.accountSignIn);
      View create = page.findViewById(R.id.accountCreate);
      View reset = page.findViewById(R.id.accountReset);
      View secondaryRow = (View) create.getParent();

      assertSame(secondaryRow, reset.getParent());
      assertTrue(secondaryRow.getTop() >= signIn.getBottom());
      assertTrue(create.getRight() <= reset.getLeft());
      int minimumTapHeight = Math.round(48 * page.getResources().getDisplayMetrics().density);
      for (View action : new View[] {signIn, create, reset}) {
        assertTrue(action.getHeight() >= minimumTapHeight);
      }
    });
  }

  public void testInitialScreenRequiresConsentAndDoesNotSaveSecrets() throws Throwable {
    getInstrumentation().runOnMainSync(() -> {
      assertTrue((page.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE) != 0);
      EditText password = page.findViewById(R.id.accountPassword);
      assertFalse(password.isSaveEnabled());
      assertFalse(((CheckBox)page.findViewById(R.id.accountConsent)).isChecked());
      assertEquals(View.VISIBLE,page.findViewById(R.id.accountForm).getVisibility());
      page.findViewById(R.id.accountCreate).performClick();
      assertEquals(page.getString(R.string.account_check_consent),
          ((TextView)page.findViewById(R.id.accountStatus)).getText().toString());
    });
  }
  public void testInvalidInputRejectedBeforeFirebaseRequest() throws Throwable {
    getInstrumentation().runOnMainSync(() -> {
      ((CheckBox)page.findViewById(R.id.accountConsent)).setChecked(true);
      ((EditText)page.findViewById(R.id.accountEmail)).setText("invalid");
      ((EditText)page.findViewById(R.id.accountPassword)).setText("synthetic-only");
      page.findViewById(R.id.accountCreate).performClick();
      assertEquals(page.getString(R.string.account_check_input),
          ((TextView)page.findViewById(R.id.accountStatus)).getText().toString());
    });
  }
}
