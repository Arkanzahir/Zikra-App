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
  private final MushafOrnaments ornaments = new MushafOrnaments(getContext());
  private final List<Hit> hits = new ArrayList<>();
  private final GestureDetector gestures;
  private MushafDocument document;
  private Typeface font;
  private QcfCanvasFont canvasFont;
  private Actions actions;
  private boolean night;
  private float viewportRatio = 1.48f;
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
    setMeasuredDimension(width, Math.round(width * viewportRatio));
  }

  public void fitViewport(int width, int height) {
    if (width <= 0 || height <= 0) return;
    float next = Math.max(1.48f, (float) height / width);
    if (Math.abs(next - viewportRatio) > .001f) {
      viewportRatio = next;
      requestLayout();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawColor(color(R.color.mushaf_paper, R.color.mushaf_night_paper));
    float w = getWidth(),
        margin = w * .12f,
        top = w * .245f,
        rowHeight = (getHeight() - w * .50f) / 15;
    ornaments.border(canvas, w, getHeight(), night);
    hits.clear();
    if (document == null || canvasFont == null) return;
    // Opening pages retain their source lines, with generous spacing like a frontispiece.
    if (document.page.number <= 2) {
      top = w * .245f;
      int last = document.page.lines.get(document.page.lines.size() - 1).number;
      rowHeight = (getHeight() - top - w * .25f) / (last - .28f);
    }
    int firstChapter = document.page.lines.get(0).words.get(0).chapter;
    drawFittedLabel(
        canvas,
        surahHeading(firstChapter),
        new RectF(w * .285f, w * .106f, w * .725f, w * .188f),
        w * .067f);
    ink.setTypeface(font);
    ink.setTextSize(100);
    float longest = 1;
    for (MushafPage.Line line : document.page.lines) {
      float length = 0;
      for (MushafPage.Word word : line.words) length += wordWidth(word, 100);
      longest = Math.max(longest, length + Math.max(0, line.words.size() - 1) * 8);
    }
    float size = Math.min(rowHeight * .82f, Math.min(w * .085f, (w - margin * 2) / longest * 100));
    for (MushafPage.Line line : document.page.lines) {
      ink.setTypeface(font);
      ink.setTextSize(size);
      float natural = 0;
      for (MushafPage.Word word : line.words) natural += wordWidth(word, size);
      float gap =
          line.centered || line.words.size() == 1
              ? size * .08f
              : Math.max(0, (w - margin * 2 - natural) / (line.words.size() - 1));
      float length = natural + gap * (line.words.size() - 1);
      float right = line.centered ? (w + length) / 2 : w - margin;
      float baseline = top + (line.number - 1) * rowHeight + rowHeight * .72f;
      float cursor = right;
      RectF activeBounds = null;
      for (MushafPage.Word word : line.words) {
        float width = wordWidth(word, size);
        if (word.chapter == activeChapter && word.verse == activeVerse) {
          RectF box = new RectF(cursor - width, baseline - size, cursor, baseline + size * .35f);
          if (activeBounds == null) activeBounds = box;
          else activeBounds.union(box);
        }
        cursor -= width + gap;
      }
      if (activeBounds != null) {
        frame.setStyle(Paint.Style.FILL);
        frame.setColor(color(R.color.mushaf_selection, R.color.mushaf_night_selection));
        canvas.drawRoundRect(activeBounds, size * .1f, size * .1f, frame);
      }
      for (MushafPage.Word word : line.words) {
        float width = wordWidth(word, size);
        RectF box = new RectF(right - width, baseline - size, right, baseline + size * .35f);
        ink.setColor(color(R.color.mushaf_ink, R.color.mushaf_night_ink));
        if (word.verseEnd) {
          ornaments.medallion(
              canvas,
              right,
              baseline,
              width,
              size,
              word.verse,
              color(R.color.mushaf_border, R.color.mushaf_night_border),
              ink.getColor(),
              color(R.color.mushaf_medallion_fill, R.color.mushaf_night_medallion_fill));
        } else canvasFont.draw(canvas, word.glyph, right, baseline, ink);
        hits.add(new Hit(box, word));
        right -= width + gap;
      }
      MushafPage.Word first = line.words.get(0);
      if (first.verse == 1 && first.position == 1 && line.number >= 2) {
        int header = line.number - (first.chapter == 1 || first.chapter == 9 ? 1 : 2);
        if (header > 1) {
          float center = top + (header - 1) * rowHeight + rowHeight * .48f;
          float height = Math.min(rowHeight * .86f, w * .085f);
          RectF band = new RectF(w * .105f, center - height / 2, w * .895f, center + height / 2);
          ornaments.surahBand(canvas, band);
          RectF label =
              new RectF(
                  w * .295f, band.top + height * .10f, w * .705f, band.bottom - height * .10f);
          drawFittedLabel(canvas, surahHeading(first.chapter), label, w * .054f);
        }
        ink.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        ink.setColor(color(R.color.mushaf_ink, R.color.mushaf_night_ink));
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

  private float wordWidth(MushafPage.Word word, float size) {
    float original = canvasFont.metrics.width(word.glyph, size);
    if (!word.verseEnd) return original;
    float reserved = word.verse >= 100 ? .95f : word.verse >= 10 ? .85f : .76f;
    return Math.max(original, size * reserved);
  }

  private String surahHeading(int chapter) {
    return getContext()
        .getString(
            R.string.mushaf_surah_heading, QuranChapters.all(getContext()).get(chapter - 1).arabic);
  }

  private void drawFittedLabel(Canvas canvas, String title, RectF box, float maximum) {
    ink.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    ink.setColor(color(R.color.mushaf_ink, R.color.mushaf_night_ink));
    ink.setTextSize(maximum);
    Paint.FontMetrics metrics = ink.getFontMetrics();
    float scale =
        Math.min(
            1,
            Math.min(
                box.width() / Math.max(1, ink.measureText(title)),
                box.height() / (metrics.descent - metrics.ascent)));
    ink.setTextSize(maximum * scale);
    metrics = ink.getFontMetrics();
    ink.setTextAlign(Paint.Align.CENTER);
    canvas.save();
    canvas.clipRect(box);
    canvas.drawText(
        title, box.centerX(), box.centerY() - (metrics.ascent + metrics.descent) / 2, ink);
    canvas.restore();
    ink.setTextAlign(Paint.Align.LEFT);
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
