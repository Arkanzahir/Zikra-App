package com.zikra.app.ui;

import android.content.Context;
import android.graphics.*;
import com.zikra.app.R;
import java.lang.ref.WeakReference;

/**
 * Painted animation layers: room stays fixed, forearm/wrist and drinking arm have their own pivots.
 */
final class StudyScenePainter {
  private static final float W = 960, H = 640;
  private static WeakReference<Assets> cached = new WeakReference<>(null);
  private final Assets assets;
  private final Paint bitmap = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF room = new RectF(0, 0, W, H);
  private final RectF body = new RectF(230, 125, 690, 585);
  private final RectF writingArm = new RectF(270, 490, 470, 570);
  private final RectF drinkingArm = new RectF(238, 304, 586, 565);
  private final RectF clipRect = new RectF();
  private final Path rounded = new Path(),
      book = new Path(),
      support = new Path(),
      stroke = new Path();
  private final int pine;
  private Shader thumbnailFade;
  private int fadeWidth;
  private final ColorFilter thumbnailTone =
      new PorterDuffColorFilter(0xFF9EAB88, PorterDuff.Mode.MULTIPLY);

  private static final class Assets {
    final Bitmap room, poses, writing, drinking;
    final Rect focus, rest;

    Assets(Context context) {
      room = decode(context, R.drawable.study_room_v2);
      poses = decode(context, R.drawable.study_student_poses_v2);
      writing = decode(context, R.drawable.study_writing_arm_v2);
      drinking = decode(context, R.drawable.study_drinking_arm_v2);
      int half = poses.getWidth() / 2;
      focus = new Rect(0, 0, half, poses.getHeight());
      rest = new Rect(half, 0, poses.getWidth(), poses.getHeight());
    }

    private static Bitmap decode(Context context, int resource) {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inScaled = false;
      options.inSampleSize = 2;
      return BitmapFactory.decodeResource(context.getResources(), resource, options);
    }
  }

  StudyScenePainter(Context context) {
    synchronized (StudyScenePainter.class) {
      Assets current = cached.get();
      if (current == null) {
        current = new Assets(context);
        cached = new WeakReference<>(current);
      }
      assets = current;
    }
    pine = context.getColor(R.color.concept_pine);
    ink.setStrokeCap(Paint.Cap.ROUND);
    ink.setStrokeJoin(Paint.Join.ROUND);
    book.moveTo(335, 519);
    book.lineTo(475, 515);
    book.lineTo(597, 519);
    book.lineTo(653, 592);
    book.lineTo(475, 599);
    book.lineTo(287, 592);
    book.close();
    support.moveTo(455, 552);
    support.cubicTo(440, 536, 456, 514, 480, 507);
    support.lineTo(600, 511);
    support.quadTo(655, 518, 655, 550);
    support.lineTo(620, 561);
    support.close();
  }

  void draw(Canvas canvas, int width, int height, float seconds, boolean resting, boolean preview) {
    if (width <= 0 || height <= 0) return;
    int outside = canvas.save();
    rounded.reset();
    clipRect.set(0, 0, width, height);
    rounded.addRoundRect(
        clipRect,
        Math.min(width, height) * .055f,
        Math.min(width, height) * .055f,
        Path.Direction.CW);
    canvas.clipPath(rounded);
    canvas.drawColor(pine);
    float scale = preview ? height / H : Math.max(width / W, height / H);
    canvas.translate(
        preview ? width - W * scale : (width - W * scale) / 2, (height - H * scale) / 2);
    canvas.scale(scale, scale);
    bitmap.setColorFilter(preview ? thumbnailTone : null);
    canvas.drawBitmap(assets.room, null, room, bitmap);
    bitmap.setColorFilter(null);
    Rect pose = resting ? assets.rest : assets.focus;
    int figure = canvas.save();
    float breath = (float) Math.sin(seconds * 1.6f) * .75f;
    canvas.translate(0, breath);
    canvas.drawBitmap(assets.poses, pose, body, bitmap);
    canvas.restoreToCount(figure);
    // The original notebook is a foreground plane, so the torso never paints over its pages.
    int paper = canvas.save();
    canvas.clipPath(book);
    canvas.drawBitmap(assets.room, null, room, bitmap);
    canvas.restoreToCount(paper);
    int hand = canvas.save();
    canvas.clipPath(support);
    canvas.drawBitmap(assets.poses, pose, body, bitmap);
    canvas.restoreToCount(hand);
    if (resting) drink(canvas, seconds);
    else write(canvas, seconds);
    canvas.restoreToCount(outside);
    if (preview) {
      if (thumbnailFade == null || fadeWidth != width) {
        fadeWidth = width;
        thumbnailFade =
            new LinearGradient(
                0,
                0,
                width * .72f,
                0,
                new int[] {pine, pine, 0x00153D34},
                new float[] {0, .55f, 1},
                Shader.TileMode.CLAMP);
      }
      ink.setStyle(Paint.Style.FILL);
      ink.setShader(thumbnailFade);
      canvas.drawRoundRect(0, 0, width, height, height * .055f, height * .055f, ink);
      ink.setShader(null);
    }
  }

  private void write(Canvas canvas, float time) {
    // Tiny pen strokes plus a slower traverse across a line; every position comes from scene time.
    float traverse = (float) Math.sin(time * .8f) * 5;
    float letters = (float) Math.sin(time * 13f) * 1.7f;
    float elbow = (float) Math.sin(time * 2.1f) * 1.1f;
    ink.setStyle(Paint.Style.STROKE);
    ink.setStrokeWidth(.9f);
    ink.setColor(0x90746B54);
    for (int line = 0; line < 3; line++) {
      stroke.reset();
      stroke.moveTo(389, 551 + line * 7);
      float end = 423 + 10 * (float) Math.sin(time * .4f + line);
      for (float x = 392; x < end; x += 3)
        stroke.lineTo(x, 551 + line * 7 + (float) Math.sin(x * 1.2f) * .6f);
      canvas.drawPath(stroke, ink);
    }
    int arm = canvas.save();
    canvas.rotate(elbow, 300, 520);
    canvas.translate(traverse, letters * .35f);
    // The forearm and hand share the same painting but rotate at separate elbow/wrist joints.
    int forearm = canvas.save();
    canvas.clipRect(250, 475, 400, 595);
    canvas.drawBitmap(assets.writing, null, writingArm, bitmap);
    canvas.restoreToCount(forearm);
    int wrist = canvas.save();
    canvas.rotate(letters, 398, 540);
    canvas.clipRect(396, 475, 500, 595);
    canvas.drawBitmap(assets.writing, null, writingArm, bitmap);
    canvas.restoreToCount(wrist);
    canvas.restoreToCount(arm);
  }

  private static float smooth(float value) {
    float t = Math.max(0, Math.min(1, value));
    return t * t * (3 - 2 * t);
  }

  private void drink(Canvas canvas, float time) {
    float phase = time % 8;
    float lift =
        phase < 1.5f
            ? 0
            : phase < 3
                ? smooth((phase - 1.5f) / 1.5f)
                : phase < 4.2f ? 1 : phase < 5.8f ? 1 - smooth((phase - 4.2f) / 1.6f) : 0;
    float sip = phase > 3 && phase < 4.2f ? (float) Math.sin((phase - 3) / 1.2f * Math.PI) : 0;
    int arm = canvas.save();
    // Lower cup -> elbow lifts to lips -> gentle sip tilt -> return to rest, over eight seconds.
    canvas.rotate(25 - 36 * lift - 2 * sip, 300, 520);
    canvas.drawBitmap(assets.drinking, null, drinkingArm, bitmap);
    ink.setStyle(Paint.Style.STROKE);
    ink.setStrokeWidth(1.4f);
    ink.setColor(0x90FFF9EB);
    for (int i = 0; i < 2; i++) {
      float rise = (time * 9 + i * 14) % 28;
      stroke.reset();
      stroke.moveTo(525 + i * 13, 326 - rise);
      stroke.cubicTo(518 + i * 13, 316 - rise, 535 + i * 13, 310 - rise, 527 + i * 13, 298 - rise);
      canvas.drawPath(stroke, ink);
    }
    canvas.restoreToCount(arm);
  }
}
