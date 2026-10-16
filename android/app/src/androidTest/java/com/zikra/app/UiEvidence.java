package com.zikra.app;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;

/** Captures actual emulator frames for design review; independent of test assertions. */
public final class UiEvidence {
  private UiEvidence() {}

  public static void capture(Instrumentation instrument, String name) {
    try {
      instrument.waitForIdleSync();
      Bitmap bitmap = instrument.getUiAutomation().takeScreenshot();
      if (bitmap == null) return;
      File dir = new File(instrument.getTargetContext().getExternalFilesDir(null), "validation");
      if (!dir.exists() && !dir.mkdirs()) return;
      try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
      }
      bitmap.recycle();
    } catch (Exception e) {
      Bundle note = new Bundle();
      note.putString("screenshot_note", name + ": " + e.getClass().getSimpleName());
      instrument.sendStatus(0, note);
    }
  }
}
