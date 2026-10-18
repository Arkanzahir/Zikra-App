package com.zikra.app.ui;

import android.content.*;
import android.database.ContentObserver;
import android.os.*;
import android.provider.Settings;

/** Observe motion/power changes once per foreground lifetime, not with Binder calls per frame. */
public final class AmbientMotionPolicy {
  private final Context context;
  private final Runnable changed;
  private boolean listening, enabled;
  private final ContentObserver observer =
      new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
          refresh();
        }
      };
  private final BroadcastReceiver receiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          refresh();
        }
      };

  public AmbientMotionPolicy(Context context, Runnable changed) {
    this.context = context;
    this.changed = changed;
  }

  public boolean enabled() {
    return listening && enabled;
  }

  public void start() {
    if (!listening) {
      listening = true;
      context
          .getContentResolver()
          .registerContentObserver(
              Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer);
      IntentFilter filter = new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
      if (Build.VERSION.SDK_INT >= 33)
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
      else context.registerReceiver(receiver, filter);
    }
    refresh();
  }

  private void refresh() {
    PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    enabled =
        Settings.Global.getFloat(
                    context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                > 0f
            && (power == null || !power.isPowerSaveMode());
    changed.run();
  }

  public void stop() {
    if (listening) {
      context.unregisterReceiver(receiver);
      context.getContentResolver().unregisterContentObserver(observer);
    }
    listening = false;
    enabled = false;
    changed.run();
  }
}
