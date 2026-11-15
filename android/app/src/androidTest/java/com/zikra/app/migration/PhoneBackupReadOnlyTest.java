package com.zikra.app.migration;

import android.content.Context;
import android.test.InstrumentationTestCase;
import java.io.File;
import java.io.FileInputStream;

/** Explicit phone marker required. Exports into owned cache; never activates a restore. */
public final class PhoneBackupReadOnlyTest extends InstrumentationTestCase {
  public void testExportAndReadBackWithoutActivatingRestore() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(), "phone-backup-test-authorized").isFile());
    File folder = new File(context.getCacheDir(), "zikra-export-" + java.util.UUID.randomUUID());
    char[] password = java.util.UUID.randomUUID().toString().toCharArray();
    try {
      File encrypted = LocalMigration.export(context, password, folder);
      assertTrue(encrypted.length() > 100);
      try (FileInputStream input = new FileInputStream(encrypted);
          LocalMigration.Preview preview = LocalMigration.inspect(context, input, password)) {
        assertTrue(preview.records >= 0);
      }
    } catch (Exception failure) {
      StringBuilder trace = new StringBuilder(failure.getClass().getName());
      for (StackTraceElement frame : failure.getStackTrace())
        if (frame.getClassName().startsWith("com.zikra.app.migration"))
          trace.append(" ").append(frame.getClassName()).append(".").append(frame.getMethodName())
              .append(":").append(frame.getLineNumber());
      fail(trace.toString());
    } finally {
      java.util.Arrays.fill(password, '\0');
      if (folder.exists()) LocalMigration.discardExport(context, folder);
    }
  }
}
