package com.zikra.app;

import android.app.Instrumentation;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Captures the full physical display when an older emulator scales wm overrides. */
public final class UiEvidence {
  private UiEvidence() {}

  public static void capture(Instrumentation instrument, String name) {
    try {
      instrument.waitForIdleSync();
      instrument.getUiAutomation().waitForIdle(250, 5000);
      File dir = new File(instrument.getTargetContext().getExternalFilesDir(null), "validation");
      if (!dir.exists() && !dir.mkdirs()) return;
      // takeScreenshot() on API 28 crops scaled output to logical bounds after wm size changes.
      ParcelFileDescriptor descriptor =
          instrument.getUiAutomation().executeShellCommand("screencap -p");
      try (InputStream source = new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
          FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = source.read(buffer)) != -1) out.write(buffer, 0, count);
      }
    } catch (Exception e) {
      Bundle note = new Bundle();
      note.putString("screenshot_note", name + ": " + e.getClass().getSimpleName());
      instrument.sendStatus(0, note);
    }
  }
}
