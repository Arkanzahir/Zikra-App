package com.zikra.app.ui;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** Native cloth binding: text stays crisp at every density, without large cover bitmaps. */
public final class BookCoverView extends View {
  private final Paint paint = new Paint(3);
  private final RectF rect = new RectF();
  private final Typeface titleFont = Typeface.create("serif", Typeface.BOLD);
  private final Typeface smallFont;
  private boolean muslim;

  public BookCoverView(Context context, AttributeSet attrs) {
    super(context, attrs);
    smallFont = context.getResources().getFont(com.zikra.app.R.font.plus_jakarta_sans);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  public void setMuslim(boolean value) {
    muslim = value;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    c.save();
    c.scale(getWidth() / 160f, getHeight() / 220f);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(0x18223c2e);
    rect.set(9, 9, 158, 216);
    c.drawRoundRect(rect, 5, 5, paint);
    paint.setColor(0xffcfc0a0);
    rect.set(12, 5, 155, 212);
    c.drawRoundRect(rect, 3, 3, paint);
    paint.setColor(0xfff2e4c8);
    rect.set(14, 5, 153, 209);
    c.drawRect(rect, paint);
    paint.setColor(0xffb7a78b);
    paint.setStrokeWidth(.45f);
    for (int y = 10; y < 210; y += 3) c.drawLine(148, y, 154, y, paint);
    paint.setColor(muslim ? 0xff244c3f : 0xff713d36);
    rect.set(3, 2, 150, 208);
    c.drawRoundRect(rect, 4, 4, paint);
    paint.setColor(0x3b102e22);
    c.drawRect(4, 3, 15, 207, paint);
    paint.setColor(0x30f5dfa7);
    c.drawRect(15, 4, 17, 206, paint);
    paint.setColor(0x0cffffff);
    paint.setStrokeWidth(.35f);
    for (int x = 19; x < 149; x += 3) c.drawLine(x, 4, x, 206, paint);
    paint.setColor(0x09ffffff);
    for (int y = 5; y < 207; y += 4) c.drawLine(18, y, 147, y, paint);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0xffc4a56b);
    paint.setStrokeWidth(.65f);
    rect.set(23, 10, 143, 199);
    c.drawRect(rect, paint);
    rect.set(27, 14, 139, 195);
    c.drawRect(rect, paint);
    for (int y : new int[] {18, 188}) {
      c.drawLine(6, y, 13, y, paint);
      c.drawLine(6, y + 3, 13, y + 3, paint);
    }
    c.drawLine(55, 70, 110, 70, paint);
    c.drawLine(55, 144, 110, 144, paint);
    c.drawLine(77, 66, 83, 62, paint);
    c.drawLine(83, 62, 89, 66, paint);
    c.drawLine(77, 66, 83, 70, paint);
    c.drawLine(83, 70, 89, 66, paint);
    paint.setStyle(Paint.Style.FILL);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(smallFont);
    paint.setTextSize(6.5f);
    paint.setColor(0xffddc88f);
    c.drawText("KOLEKSI HADIS", 83, 49, paint);
    paint.setTypeface(titleFont);
    paint.setTextSize(22);
    c.drawText("Shahih", 83, 103, paint);
    c.drawText(muslim ? "Muslim" : "Bukhari", 83, 128, paint);
    paint.setTypeface(smallFont);
    paint.setTextSize(6.1f);
    c.drawText(getResources().getString(com.zikra.app.R.string.hadith_refined_cover_caption), 83, 172, paint);
    c.restore();
  }
}
