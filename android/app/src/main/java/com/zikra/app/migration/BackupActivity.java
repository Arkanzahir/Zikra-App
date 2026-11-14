package com.zikra.app.migration;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.*;
import com.zikra.app.BaseActivity;
import com.zikra.app.R;
import com.zikra.app.ui.RefinedUi;
import java.io.*;
import java.util.*;

/** User-selected documents only; no storage grant, password persistence or automatic cloud upload. */
public final class BackupActivity extends BaseActivity {
  private static final int EXPORT = 310, IMPORT = 311;
  private TextView status;
  private Button export, restore;
  private AlertDialog dialog;
  private volatile boolean busy;
  private volatile LocalMigration.Preview preview;

  @Override protected boolean usesShakeShortcut() { return false; }
  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(getColor(R.color.refined_background));
    com.zikra.app.ui.FeatureHeader header = new com.zikra.app.ui.FeatureHeader(this, null);
    header.bind(R.id.toolbarBack, R.id.detailTitle, R.id.featureSubtitle, 0, getString(R.string.migration_title), null, 0, null);
    root.addView(header);
    ScrollView scroll = new ScrollView(this); LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
    int pad = RefinedUi.dp(this, 24); body.setPadding(pad, pad, pad, pad); scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    TextView explanation = RefinedUi.text(this, getString(R.string.migration_explanation), 16, R.color.refined_ink, false);
    explanation.setLineSpacing(RefinedUi.dp(this, 3), 1); body.addView(explanation);
    export = new Button(this); export.setText(R.string.migration_export); styleButton(export, true); body.addView(export);
    restore = new Button(this); restore.setText(R.string.migration_import); styleButton(restore, false); body.addView(restore);
    status = RefinedUi.text(this, getString(R.string.migration_safety), 14, R.color.refined_muted, false);
    status.setLineSpacing(RefinedUi.dp(this, 3), 1); status.setPadding(0, pad, 0, pad); body.addView(status);
    setContentView(root); setupScreen(getString(R.string.migration_title)); RefinedUi.chrome(this, header, false);
    header.back().setOnClickListener(v -> onBackPressed());
    export.setOnClickListener(v -> picker(true)); restore.setOnClickListener(v -> picker(false));
  }
  private void styleButton(Button button, boolean primary) {
    int gap = RefinedUi.dp(this, 16); button.setAllCaps(false); button.setTextSize(16);
    button.setMinHeight(RefinedUi.dp(this, 56)); button.setPadding(gap, gap / 2, gap, gap / 2);
    button.setTextColor(getColor(primary ? R.color.refined_on_primary : R.color.refined_ink));
    button.setBackground(RefinedUi.surface(this, primary ? R.color.refined_primary : R.color.refined_background, !primary, 16));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = gap;
    button.setLayoutParams(params);
  }
  private void picker(boolean saving) {
    if (busy) return;
    Intent intent = new Intent(saving ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream");
    if (saving) intent.putExtra(Intent.EXTRA_TITLE, "Zikra-" + java.time.LocalDate.now() + ".zikrabackup");
    else intent.setType("*/*");
    try { startActivityForResult(intent, saving ? EXPORT : IMPORT); }
    catch (android.content.ActivityNotFoundException unavailable) { status.setText(R.string.migration_picker_missing); }
  }
  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null || busy) return;
    if (request == EXPORT || request == IMPORT) password(data.getData(), request == EXPORT);
  }
  private EditText passwordInput() {
    EditText input = new EditText(this); input.setSingleLine(true);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    input.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
    input.setSaveEnabled(false); input.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
    input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(BackupCipher.MAX_PASSWORD)});
    return input;
  }
  private void password(Uri uri, boolean saving) {
    LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL); int pad = RefinedUi.dp(this, 24); fields.setPadding(pad, 0, pad, 0);
    EditText first = passwordInput(), confirm = passwordInput(); first.setHint(R.string.migration_password);
    confirm.setHint(R.string.migration_password_repeat); fields.addView(first); if (saving) fields.addView(confirm);
    dialog = new AlertDialog.Builder(this).setTitle(saving ? R.string.migration_export : R.string.migration_import)
        .setMessage(R.string.migration_password_notice).setView(fields)
        .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.migration_continue, null).create();
    dialog.setOnDismissListener(d -> { first.getText().clear(); confirm.getText().clear(); });
    dialog.setOnShowListener(d -> {
      dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
        char[] secret = new char[first.length()]; first.getText().getChars(0, first.length(), secret, 0);
        boolean matches = !saving || android.text.TextUtils.equals(first.getText(), confirm.getText());
        if (secret.length < BackupCipher.MIN_PASSWORD || !matches) {
          Arrays.fill(secret, '\0'); first.setError(getString(R.string.migration_password_error)); return;
        }
        dialog.dismiss(); execute(uri, saving, secret);
      });
    }); dialog.show();
  }
  private void busy(boolean value) {
    busy = value; export.setEnabled(!value); restore.setEnabled(!value);
  }
  private void execute(Uri uri, boolean saving, char[] secret) {
    busy(true); status.setText(R.string.migration_working);
    android.content.Context context = getApplicationContext();
    new Thread(() -> {
      File folder = new File(context.getCacheDir(), "zikra-export-" + UUID.randomUUID());
      LocalMigration.Preview checked = null;
      try {
        if (saving) {
          File encrypted = LocalMigration.export(context, secret, folder);
          try (InputStream input = new FileInputStream(encrypted); OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) throw new IOException("Document unavailable");
            byte[] buffer = new byte[65536]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
          }
          try (InputStream input = getContentResolver().openInputStream(uri);
               LocalMigration.Preview verified = LocalMigration.inspect(context, input, secret)) { }
          runOnUiThread(() -> { if (!isDestroyed()) { busy(false); status.setText(R.string.migration_export_success); } });
        } else {
          try (InputStream input = getContentResolver().openInputStream(uri)) { checked = LocalMigration.inspect(context, input, secret); }
          LocalMigration.Preview ready = checked; checked = null;
          runOnUiThread(() -> { if (isDestroyed()) close(ready); else { busy(false); preview = ready; showPreview(); } });
        }
      } catch (Exception failure) {
        runOnUiThread(() -> { if (!isDestroyed()) { busy(false); status.setText(R.string.migration_failed); } });
      } finally {
        Arrays.fill(secret, '\0'); close(checked);
        if (folder.exists()) try { LocalMigration.discardExport(context, folder); } catch (Exception ignored) { }
      }
    }, "zikra-local-backup").start();
  }
  private void showPreview() {
    LocalMigration.Preview ready = preview;
    String counts = getString(R.string.migration_counts, ready.records, ready.mediaFiles);
    dialog = new AlertDialog.Builder(this).setTitle(R.string.migration_validated)
        .setMessage(counts + "\n\n" + getString(ready.canRestore() ? R.string.migration_apply_notice : R.string.migration_verify_only))
        .setNegativeButton(R.string.migration_done, null)
        .setPositiveButton(ready.canRestore() ? R.string.migration_apply : R.string.migration_done, (d, which) -> {
          if (ready.canRestore()) confirmRestore(ready); else discardPreview();
        }).create();
    dialog.setOnDismissListener(d -> { if (!busy) discardPreview(); });
    // The positive action is overridden so dismissal cannot discard the stage while it is scheduled.
    dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
      if (!ready.canRestore()) { dialog.dismiss(); return; }
      busy(true); dialog.dismiss(); confirmRestore(ready);
    })); dialog.show();
  }
  private void confirmRestore(LocalMigration.Preview ready) {
    new Thread(() -> {
      try {
        LocalMigration.requestRestore(getApplicationContext(), ready);
        runOnUiThread(() -> {
          if (isDestroyed()) return;
          status.setText(R.string.migration_restart_notice);
          export.setText(R.string.migration_close); export.setEnabled(true);
          export.setOnClickListener(v -> { finishAffinity(); android.os.Process.killProcess(android.os.Process.myPid()); });
        });
      } catch (Exception failure) {
        runOnUiThread(() -> { if (!isDestroyed()) { busy(false); status.setText(R.string.migration_failed); } });
      } finally { close(ready); preview = null; }
    }, "zikra-local-prepare").start();
  }
  private static void close(LocalMigration.Preview value) { if (value != null) try { value.close(); } catch (Exception ignored) { } }
  private void discardPreview() { LocalMigration.Preview value = preview; preview = null; close(value); }
  @Override public void onBackPressed() { if (!busy) super.onBackPressed(); }
  @Override protected void onDestroy() {
    if (dialog != null) dialog.dismiss(); if (!busy) discardPreview(); super.onDestroy();
  }
}
