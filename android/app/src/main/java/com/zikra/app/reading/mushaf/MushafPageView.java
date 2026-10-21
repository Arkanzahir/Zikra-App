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
    ink.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    ink.setTextSize(w * .041f);
    ink.setColor(color(R.color.mushaf_border, R.color.mushaf_night_border));
    String panelTitle =
        QuranChapters.all(getContext())
            .get(document.page.lines.get(0).words.get(0).chapter - 1)
            .arabic;
    Paint.FontMetrics titleMetrics = ink.getFontMetrics();
    canvas.drawText(
        panelTitle,
        (w - ink.measureText(panelTitle)) / 2,
        w * .145f - (titleMetrics.ascent + titleMetrics.descent) / 2,
        ink);
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
      float cursor = right;
      RectF activeBounds = null;
      for (MushafPage.Word word : line.words) {
        float width = canvasFont.metrics.width(word.glyph, size);
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
        float width = canvasFont.metrics.width(word.glyph, size);
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
              color(R.color.mushaf_gold, R.color.mushaf_night_gold),
              ink.getColor());
        } else canvasFont.draw(canvas, word.glyph, right, baseline, ink);
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
        if (header > 1) ornament(canvas, w, titleBaseline, ink.measureText(title), size);
        if (header > 1)
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
    Path plaque = new Path();
    float left = width / 2 - half, right = width / 2 + half;
    plaque.moveTo(left, center);
    plaque.cubicTo(
        left + height,
        center - height * .3f,
        left,
        center - height,
        left + height * 1.4f,
        center - height);
    plaque.lineTo(right - height * 1.4f, center - height);
    plaque.cubicTo(right, center - height, right - height, center - height * .3f, right, center);
    plaque.cubicTo(
        right - height,
        center + height * .3f,
        right,
        center + height,
        right - height * 1.4f,
        center + height);
    plaque.lineTo(left + height * 1.4f, center + height);
    plaque.cubicTo(left, center + height, left + height, center + height * .3f, left, center);
    canvas.drawPath(plaque, frame);
    for (int direction : new int[] {-1, 1}) {
      float edge = width / 2 + direction * half;
      ornaments.finial(
          canvas,
          edge + direction * size * .06f,
          center,
          size * .72f,
          direction,
          color(R.color.mushaf_gold, R.color.mushaf_night_gold));
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
