package com.zikra.app.reading.mushaf;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;
import com.zikra.app.R;
import com.zikra.app.reading.*;
import java.util.*;

/** QCF page glyphs laid out right-to-left by source line; never line-wrap Arabic. */
public final class MushafPageView extends View {
  public interface Actions {
    void touched();

    void toggleChrome();

    void verse(int chapter, int verse);
  }

  private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final List<Hit> hits = new ArrayList<>();
  private final GestureDetector gestures;
  private MushafDocument document;
  private Typeface font;
  private QcfCanvasFont canvasFont;
  private Actions actions;
  private boolean night;
  private int activeChapter, activeVerse;

  private static final class Hit {
    final RectF bounds;
    final MushafPage.Word word;

    Hit(RectF bounds, MushafPage.Word word) {
      this.bounds = bounds;
      this.word = word;
    }
  }

  public MushafPageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setFocusable(true);
    setClickable(true);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    gestures =
        new GestureDetector(
            context,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDown(MotionEvent event) {
                if (actions != null) actions.touched();
                return true;
              }

              @Override
              public boolean onSingleTapConfirmed(MotionEvent event) {
                return performClick();
              }

              @Override
              public void onLongPress(MotionEvent event) {
                if (actions == null) return;
                for (Hit hit : hits)
                  if (hit.bounds.contains(event.getX(), event.getY())) {
                    actions.verse(hit.word.chapter, hit.word.verse);
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    break;
                  }
              }
            });
  }

  public void bind(MushafDocument document, Typeface font, Actions actions) {
    this.document = document;
    this.font = font;
    this.actions = actions;
    try {
      canvasFont =
          new QcfCanvasFont(
              new java.io.File(
                  getContext().getCacheDir(), "qcf-v2-fonts/" + document.page.fontAsset));
      for (MushafPage.Line line : document.page.lines)
        for (MushafPage.Word word : line.words)
          if (canvasFont.metrics.width(word.glyph, 100) <= 0)
            throw new IllegalArgumentException("Invalid glyph width");
    } catch (java.io.IOException error) {
      throw new IllegalStateException("QCF font could not load", error);
    }
    setContentDescription(document.accessibleText);
    invalidate();
  }

  public void theme(boolean night) {
    this.night = night;
    invalidate();
  }

  public void active(int chapter, int verse) {
    activeChapter = chapter;
    activeVerse = verse;
    invalidate();
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    int width = MeasureSpec.getSize(widthSpec);
    if (getLayoutParams() != null && getLayoutParams().width > 0) width = getLayoutParams().width;
    if (width == 0) width = getResources().getDisplayMetrics().widthPixels;
    setMeasuredDimension(width, Math.round(width * 1.48f));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawColor(color(R.color.mushaf_paper, R.color.mushaf_night_paper));
    float w = getWidth(), margin = w * .085f, top = w * .075f, rowHeight = w * .086f;
    frame.setColor(color(R.color.mushaf_border, R.color.mushaf_night_border));
    frame.setStyle(Paint.Style.STROKE);
    frame.setStrokeWidth(Math.max(1, w / 500));
    canvas.drawRect(margin * .45f, top * .35f, w - margin * .45f, getHeight() - top * .35f, frame);
    canvas.drawRect(margin * .58f, top * .48f, w - margin * .58f, getHeight() - top * .48f, frame);
    float inset = margin * .58f, arm = w * .035f;
    for (int x : new int[] {-1, 1}) {
      for (int y : new int[] {-1, 1}) {
        canvas.save();
        canvas.translate(x < 0 ? inset : w - inset, y < 0 ? top * .48f : getHeight() - top * .48f);
        canvas.scale(-x, -y);
        Path corner = new Path();
        corner.moveTo(0, arm * 2);
        corner.lineTo(arm, arm * 2);
        corner.quadTo(arm * 2, arm * 2, arm * 2, arm);
        corner.lineTo(arm * 2, 0);
        canvas.drawPath(corner, frame);
        diamond(canvas, arm * .8f, arm * .8f, arm * .23f);
        canvas.restore();
      }
    }
    hits.clear();
    if (document == null || canvasFont == null) return;
    // Opening pages retain their source lines, with generous spacing like a frontispiece.
    if (document.page.number <= 2) {
      top = w * .14f;
      rowHeight = w * .145f;
    }
    ink.setTypeface(font);
    ink.setTextSize(100);
    float longest = 1;
    for (MushafPage.Line line : document.page.lines) {
      float length = 0;
      for (MushafPage.Word word : line.words) length += canvasFont.metrics.width(word.glyph, 100);
      longest = Math.max(longest, length + Math.max(0, line.words.size() - 1) * 8);
    }
    float size = Math.min(w * .085f, (w - margin * 2) / longest * 100);
    for (MushafPage.Line line : document.page.lines) {
      ink.setTypeface(font);
      ink.setTextSize(size);
      float natural = 0;
      for (MushafPage.Word word : line.words) natural += canvasFont.metrics.width(word.glyph, size);
      float gap =
          line.centered || line.words.size() == 1
              ? size * .08f
              : Math.max(0, (w - margin * 2 - natural) / (line.words.size() - 1));
      float length = natural + gap * (line.words.size() - 1);
      float right = line.centered ? (w + length) / 2 : w - margin;
      float baseline = top + (line.number - 1) * rowHeight + rowHeight * .72f;
      for (MushafPage.Word word : line.words) {
        float width = canvasFont.metrics.width(word.glyph, size);
        RectF box = new RectF(right - width, baseline - size, right, baseline + size * .35f);
        if (word.chapter == activeChapter && word.verse == activeVerse) {
          frame.setStyle(Paint.Style.FILL);
          frame.setColor(night ? 0xFF314C40 : 0xFFE3ECD9);
          canvas.drawRoundRect(box, size * .1f, size * .1f, frame);
        }
        ink.setColor(color(R.color.mushaf_ink, R.color.mushaf_night_ink));
        canvasFont.draw(canvas, word.glyph, right, baseline, ink);
        hits.add(new Hit(box, word));
        right -= width + gap;
      }
      MushafPage.Word first = line.words.get(0);
      if (first.verse == 1 && first.position == 1 && line.number >= 2) {
        int header = line.number - (first.chapter == 1 || first.chapter == 9 ? 1 : 2);
        ink.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        ink.setTextSize(size * .62f);
        ink.setColor(night ? 0xFFE0D4AA : 0xFF596C4D);
        String title = QuranChapters.all(getContext()).get(first.chapter - 1).arabic;
        float titleBaseline = top + (header - 1) * rowHeight + rowHeight * .7f;
        ornament(canvas, w, titleBaseline, ink.measureText(title), size);
        canvas.drawText(title, (w - ink.measureText(title)) / 2, titleBaseline, ink);
        if (first.chapter != 1 && first.chapter != 9 && line.number >= 3) {
          // Existing verified built-in Al-Fatihah 1:1; no text is copied from a mockup.
          String basmalah = QuranPage.builtIn(1).verses.get(0).arabic;
          ink.setTextSize(size * .59f);
          canvas.drawText(
              basmalah,
              (w - ink.measureText(basmalah)) / 2,
              top + (line.number - 2) * rowHeight + rowHeight * .7f,
              ink);
        }
      }
    }
  }

  private int color(int day, int dark) {
    return getContext().getColor(night ? dark : day);
  }

  private void diamond(Canvas canvas, float x, float y, float radius) {
    Path path = new Path();
    path.moveTo(x, y - radius);
    path.lineTo(x + radius, y);
    path.lineTo(x, y + radius);
    path.lineTo(x - radius, y);
    path.close();
    canvas.drawPath(path, frame);
  }

  private void ornament(Canvas canvas, float width, float baseline, float textWidth, float size) {
    frame.setStyle(Paint.Style.STROKE);
    frame.setColor(color(R.color.mushaf_gold, R.color.mushaf_night_gold));
    frame.setStrokeWidth(Math.max(1, width / 900));
    float half = Math.max(textWidth / 2 + size * .65f, width * .19f);
    float center = baseline - size * .22f, height = size * .44f;
    RectF plaque = new RectF(width / 2 - half, center - height, width / 2 + half, center + height);
    canvas.drawRoundRect(plaque, height, height, frame);
    for (int direction : new int[] {-1, 1}) {
      float edge = width / 2 + direction * half;
      diamond(canvas, edge + direction * size * .24f, center, size * .16f);
      canvas.drawLine(
          edge + direction * size * .43f, center, edge + direction * size * .78f, center, frame);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return gestures.onTouchEvent(event) || super.onTouchEvent(event);
  }

  @Override
  public boolean performClick() {
    super.performClick();
    if (actions != null) actions.toggleChrome();
    return true;
  }
}
