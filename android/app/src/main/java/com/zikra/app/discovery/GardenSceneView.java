package com.zikra.app.discovery;

import android.content.Context;
import android.graphics.*;
import android.hardware.*;
import android.os.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.zikra.app.R;
import com.zikra.app.ui.AmbientMotionPolicy;
import java.util.concurrent.*;

/**
 * Legacy view name retained for XML compatibility. Plays authored character poses once, then rests.
 */
public final class GardenSceneView extends View implements SensorEventListener {
  private static final int[] POSTERS = {
    R.drawable.onboard_study_end, R.drawable.onboard_prayer_end, R.drawable.onboard_quran_end
  };
  private static final int[] MOTIONS = {
    R.drawable.onboard_study_motion,
    R.drawable.onboard_prayer_motion,
    R.drawable.onboard_quran_motion
  };
  private static final int[] CIRCLES = {0xFFE6CAA0, 0xFFC0D8CA, 0xFFDBD2BE};
  private static final ExecutorService DECODER = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Rect source = new Rect();
  private final RectF target = new RectF();
  private final SensorManager sensors;
  private final Sensor gravity;
  private final AmbientMotionPolicy policy;
  private Bitmap poster, sheet;
  private Future<?> decode;
  private boolean running, registered, loading, playing, played;
  private int story, generation;
  private long started;
  private float aimX, aimY, offsetX, offsetY, downX, downY;

  public GardenSceneView(Context context, AttributeSet attrs) {
    super(context, attrs);
    sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    gravity = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);
    policy = new AmbientMotionPolicy(context, this::syncMotion);
    setClickable(true);
  }

  public void setStory(int value) {
    value = Math.max(0, Math.min(2, value));
    if (story == value && (poster != null || loading)) return;
    cancelDecode();
    story = value;
    poster = sheet = null;
    played = playing = false;
    aimX = aimY = offsetX = offsetY = 0;
    syncMotion();
  }

  public void setRunning(boolean value) {
    running = value;
    if (value) policy.start();
    else {
      stopPlayback();
      cancelDecode();
      policy.stop();
    }
    syncMotion();
  }

  public boolean isMotionRunning() {
    return playing && canAnimate();
  }

  private boolean canAnimate() {
    return running && isShown() && hasWindowFocus() && policy.enabled();
  }

  private void cancelDecode() {
    generation++;
    if (decode != null) decode.cancel(false);
    decode = null;
    loading = false;
  }

  private void stopPlayback() {
    if (playing) played = true;
    playing = false;
    sheet = null;
  }

  private void syncMotion() {
    boolean active = canAnimate();
    if (active && gravity != null && !registered)
      registered = sensors.registerListener(this, gravity, SensorManager.SENSOR_DELAY_UI);
    if (!active && registered) {
      sensors.unregisterListener(this);
      registered = false;
    }
    if (!active) {
      stopPlayback();
      aimX = aimY = offsetX = offsetY = 0;
    }
    if (running
        && isAttachedToWindow()
        && isShown()
        && !loading
        && (poster == null || (active && !played && sheet == null))) load(active && !played);
    invalidate();
  }

  private void load(boolean motion) {
    loading = true;
    final int token = ++generation, selected = story;
    final Bitmap existing = poster;
    final android.content.res.Resources resources = getResources();
    decode =
        DECODER.submit(
            () -> {
              Bitmap still =
                  existing == null
                      ? BitmapFactory.decodeResource(resources, POSTERS[selected])
                      : existing;
              Bitmap atlas =
                  motion ? BitmapFactory.decodeResource(resources, MOTIONS[selected]) : null;
              main.post(
                  () -> {
                    if (token != generation || !isAttachedToWindow() || !running) return;
                    loading = false;
                    poster = still;
                    if (atlas != null && canAnimate() && !played) {
                      sheet = atlas;
                      started = SystemClock.uptimeMillis();
                      playing = true;
                    }
                    syncMotion();
                  });
            });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    syncMotion();
  }

  @Override
  protected void onDetachedFromWindow() {
    running = false;
    cancelDecode();
    stopPlayback();
    poster = null;
    policy.stop();
    super.onDetachedFromWindow();
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    if (policy != null) syncMotion();
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (policy != null) syncMotion();
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (!canAnimate()) return;
    float nextX = Math.max(-1, Math.min(1, -event.values[0] / 5));
    float nextY = Math.max(-1, Math.min(1, event.values[1] / 6));
    if (Math.abs(nextX - aimX) > .02f || Math.abs(nextY - aimY) > .02f) {
      aimX = nextX;
      aimY = nextY;
      invalidate();
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
      downX = e.getX();
      downY = e.getY();
    }
    if (e.getActionMasked() == MotionEvent.ACTION_UP
        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      aimX = aimY = 0;
      if (e.getActionMasked() == MotionEvent.ACTION_UP
          && Math.hypot(e.getX() - downX, e.getY() - downY)
              < 20 * getResources().getDisplayMetrics().density) performClick();
    } else if (canAnimate()) {
      aimX = (e.getX() / Math.max(1, getWidth()) - .5f) * 2;
      aimY = (e.getY() / Math.max(1, getHeight()) - .5f) * 2;
    }
    invalidate();
    return true;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    if (canAnimate() && !playing && !loading) {
      played = false;
      syncMotion();
    }
    return true;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    boolean active = canAnimate();
    if (active) {
      offsetX += (aimX - offsetX) * .2f;
      offsetY += (aimY - offsetY) * .2f;
      if (Math.abs(aimX - offsetX) < .005f) offsetX = aimX;
      if (Math.abs(aimY - offsetY) < .005f) offsetY = aimY;
    }
    float unit = getWidth() / 360f;
    paint.setColor(CIRCLES[story]);
    canvas.drawCircle(
        getWidth() * .5f + offsetX * unit * 3,
        getHeight() * .48f + offsetY * unit * 2,
        Math.min(getWidth() * .45f, getHeight() * .48f),
        paint);
    float width = Math.min(getWidth() * 1.07f, getHeight() * 4f / 3f);
    float height = width * .75f;
    float x = (getWidth() - width) / 2 + offsetX * unit * 9;
    float y = (getHeight() - height) / 2 + offsetY * unit * 5;
    target.set(x, y, x + width, y + height);
    paint.setColor(Color.WHITE);
    int frame = playing ? (int) ((SystemClock.uptimeMillis() - started) * 24 / 1000) : 48;
    if (playing && (!active || frame >= 48)) stopPlayback();
    if (playing && sheet != null) {
      int col = frame % 8, row = frame / 8;
      source.set(col * 320, row * 240, (col + 1) * 320, (row + 1) * 240);
      canvas.drawBitmap(sheet, source, target, paint);
    } else if (poster != null) canvas.drawBitmap(poster, null, target, paint);
    if (playing
        || (active && (Math.abs(aimX - offsetX) > .005f || Math.abs(aimY - offsetY) > .005f)))
      postInvalidateDelayed(42);
  }
}
