package com.zikra.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.zikra.app.R;
import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.core.MarkwonTheme;
import okhttp3.HttpUrl;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;

/** Native text spans only. No HTML execution, WebView, image loader or automatic network access. */
public final class AssistantMarkdown {
  private final Markwon renderer;

  public AssistantMarkdown(Context context) {
    renderer =
        Markwon.builder(context)
            .usePlugin(
                new AbstractMarkwonPlugin() {
                  @Override
                  public void configureTheme(MarkwonTheme.Builder theme) {
                    theme
                        .linkColor(context.getColor(R.color.ai_refresh_accent))
                        .isLinkUnderlined(true)
                        .blockMargin(AssistantUi.dp(context, 20))
                        .bulletWidth(AssistantUi.dp(context, 4))
                        .headingTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
                        .headingTextSizeMultipliers(new float[] {1.4f, 1.25f, 1.125f, 1f, 1f, 1f})
                        .headingBreakHeight(0)
                        .codeTypeface(Typeface.MONOSPACE)
                        .codeTextSize(sp(context, 14))
                        .codeTextColor(context.getColor(R.color.ai_refresh_ink))
                        .codeBackgroundColor(context.getColor(R.color.ai_refresh_mint))
                        .codeBlockTextColor(context.getColor(R.color.ai_refresh_ink))
                        .codeBlockBackgroundColor(context.getColor(R.color.ai_refresh_mint));
                  }

                  @Override
                  public void configureConfiguration(MarkwonConfiguration.Builder builder) {
                    builder.linkResolver(
                        (view, destination) -> {
                          String safe = safeLink(destination);
                          if (safe == null) {
                            Toast.makeText(
                                    view.getContext(),
                                    R.string.ai_reply_link_blocked,
                                    Toast.LENGTH_SHORT)
                                .show();
                            return;
                          }
                          try {
                            view.getContext()
                                .startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(safe))
                                        .addCategory(Intent.CATEGORY_BROWSABLE));
                          } catch (ActivityNotFoundException | SecurityException unavailable) {
                            Toast.makeText(
                                    view.getContext(),
                                    R.string.ai_reply_link_unavailable,
                                    Toast.LENGTH_SHORT)
                                .show();
                          }
                        });
                  }

                  @Override
                  public void configureVisitor(MarkwonVisitor.Builder visitor) {
                    visitor.on(Image.class, (render, image) -> render.visitChildren(image));
                    visitor.on(
                        HtmlInline.class,
                        (render, html) -> render.builder().append(html.getLiteral()));
                    visitor.on(
                        HtmlBlock.class,
                        (render, html) -> render.builder().append(html.getLiteral()));
                  }
                })
            .build();
  }

  public static void style(TextView view) {
    view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    view.setTextSize(
        android.util.TypedValue.COMPLEX_UNIT_PX,
        view.getResources().getDimension(R.dimen.ai_reply_body));
    view.setLetterSpacing(0);
    view.setLineSpacing(0, 1.35f);
    if (android.os.Build.VERSION.SDK_INT >= 29)
      view.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
    view.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
    view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    view.setTextColor(view.getContext().getColor(R.color.ai_refresh_ink));
    view.setTextIsSelectable(true);
  }

  public void bind(TextView view, String source) {
    style(view);
    renderer.setMarkdown(view, source);
  }

  public static String safeLink(String destination) {
    if (destination == null || destination.length() > 2048) return null;
    for (int i = 0; i < destination.length(); i++) if (destination.charAt(i) <= 0x20) return null;
    HttpUrl url = HttpUrl.parse(destination);
    return url != null && url.isHttps() && url.username().isEmpty() && url.password().isEmpty()
        ? url.toString()
        : null;
  }

  private static int sp(Context context, int value) {
    return Math.round(
        android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            value,
            context.getResources().getDisplayMetrics()));
  }
}
