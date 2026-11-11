package com.zikra.app.assistant;

import android.content.*;
import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;
import android.test.InstrumentationTestCase;
import android.text.Spanned;
import android.view.*;
import android.widget.*;
import com.zikra.app.*;
import com.zikra.app.ui.AssistantMarkdown;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Offline presentation fixtures on an explicitly backed-up project emulator. */
@SuppressWarnings("deprecation")
public final class AssistantTypographyTest extends InstrumentationTestCase {
  private Context context;
  private AiActivity page;

  private void ui(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    try (InputStream in =
        new ParcelFileDescriptor.AutoCloseInputStream(
            getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))) {
      assertEquals(
          "Project emulator only",
          "1",
          new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
    }
    assertTrue(
        "Host backup required",
        new File(context.getFilesDir(), "ai-typography-backup-verified").isFile());
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) ui(page::finish);
    super.tearDown();
  }

  public void testActualReplyTypographyLight() throws Exception {
    renderFixture(false);
  }

  public void testActualReplyTypographyDark() throws Exception {
    renderFixture(true);
  }

  private void renderFixture(boolean requestedDark) throws Exception {
    context
        .getSharedPreferences(com.zikra.app.ui.AppAppearance.PREFS, 0)
        .edit()
        .putString(com.zikra.app.ui.AppAppearance.KEY, requestedDark ? "dark" : "light")
        .commit();
    context
        .getSharedPreferences("zikra_preferences", 0)
        .edit()
        .putString("assistant_mode_v2", "offline")
        .commit();
    new AssistantConnection(context).offline();
    page =
        (AiActivity)
            getInstrumentation()
                .startActivitySync(
                    new Intent(context, AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    Method bubble =
        AiActivity.class.getDeclaredMethod("bubble", String.class, String.class, String.class);
    bubble.setAccessible(true);
    String fixture =
        "## Rencana belajarmu\n\n"
            + "Mulai dari **satu target kecil**, lalu jaga ritmenya.\n\n"
            + "### Tiga langkah sederhana\n\n"
            + "1. Pilih satu materi.\n"
            + "2. Fokus selama 25 menit.\n"
            + "3. Istirahat sejenak.\n\n"
            + "**Catatan:** sedikit, tetapi konsisten.\n\n"
            + "_Satu langkah baik, setiap hari._";
    ui(
        () -> {
          ((EditText) page.findViewById(R.id.aiInput)).setText("");
          LinearLayout thread = page.findViewById(R.id.aiChatThread);
          thread.removeAllViews();
          page.findViewById(R.id.aiWelcome).setVisibility(View.GONE);
          try {
            bubble.invoke(page, "user", "Bantu susun cara belajar yang sederhana", "online");
            bubble.invoke(page, "assistant", fixture, "online");
          } catch (Exception error) {
            throw new RuntimeException(error);
          }
        });
    LinearLayout thread = page.findViewById(R.id.aiChatThread);
    LinearLayout card = (LinearLayout) ((LinearLayout) thread.getChildAt(1)).getChildAt(0);
    TextView answer = (TextView) card.getChildAt(1);
    assertFalse(answer.getText().toString().contains("**"));
    assertFalse(answer.getText().toString().contains("###"));
    assertTrue(answer.getText().toString().contains("Fokus selama 25 menit."));
    assertEquals(Typeface.create("sans-serif", Typeface.NORMAL), answer.getTypeface());
    assertTrue(answer.isTextSelectable());
    Spanned text = (Spanned) answer.getText();
    String spanNames =
        Arrays.toString(
            Arrays.stream(text.getSpans(0, text.length(), Object.class))
                .map(x -> x.getClass().getSimpleName())
                .toArray());
    assertTrue(spanNames, spanNames.contains("HeadingSpan"));
    assertTrue(spanNames, spanNames.contains("StrongEmphasisSpan"));
    assertTrue(spanNames, spanNames.contains("OrderedListItemSpan"));
    assertTrue(
        "Answer width remains within viewport",
        answer.getWidth() <= page.getResources().getDisplayMetrics().widthPixels);
    for (int i = 0; i < answer.getLayout().getLineCount(); i++) {
      assertTrue("Formatted line wraps inside its column", answer.getLayout().getLineWidth(i) <= answer.getWidth() + 1);
    }
    if (context.getResources().getConfiguration().fontScale > 1.9f) {
      ui(() -> ((ScrollView) page.findViewById(R.id.aiScroll)).scrollTo(0, thread.getTop() + thread.getChildAt(1).getTop()));
      android.graphics.Rect visible = new android.graphics.Rect();
      assertTrue("Large text remains reachable by scrolling", answer.getGlobalVisibleRect(visible));
      assertTrue("At least one full line remains visible", visible.height() >= answer.getLineHeight());
    }
    boolean dark = com.zikra.app.ui.AppAppearance.dark(page);
    assertEquals(requestedDark, dark);
    UiEvidence.capture(
        getInstrumentation(),
        "AITYP-"
            + (dark ? "dark" : "light")
            + "-"
            + (context.getResources().getConfiguration().fontScale > 1.9f ? "200" : "100"));
  }

  public void testCodeLiteralArabicAndUntrustedMarkupRemainNativeText() throws Exception {
    final TextView[] view = new TextView[1];
    String arabic = "السَّلَامُ عَلَيْكُمْ";
    ui(
        () -> {
          view[0] = new TextView(context);
          new AssistantMarkdown(context)
              .bind(
                  view[0],
                  "```text\n**literal**\n```\n\n"
                      + arabic
                      + "\n\n"
                      + "![Ilustrasi](https://example.test/private-image.png)\n\n"
                      + "<script>alert(1)</script>");
        });
    String rendered = view[0].getText().toString();
    assertTrue(rendered, rendered.contains("**literal**"));
    assertTrue(rendered, rendered.contains(arabic));
    assertTrue(rendered, rendered.contains("Ilustrasi"));
    assertTrue(rendered, rendered.contains("<script>"));
    Spanned spans = (Spanned) view[0].getText();
    for (Object span : spans.getSpans(0, spans.length(), Object.class))
      assertFalse(span.getClass().getName().contains("AsyncDrawable"));
  }
}
