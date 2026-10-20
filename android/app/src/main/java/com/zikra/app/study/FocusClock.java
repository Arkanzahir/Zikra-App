package com.zikra.app.study;

/** Monotonic countdown; finishing never marks a task complete or starts the next phase. */
public final class FocusClock {
  public long remaining, end, taskId = -1, session;
  public int boot, focusMinutes = 25, breakMinutes = 5;
  public boolean running, finished, breakPhase;

  public FocusClock() {
    remaining = 25 * 60000L;
  }

  public void update(long now, int currentBoot) {
    if (!running) return;
    if (boot != currentBoot
        || now < 0
        || end - now > (breakPhase ? breakMinutes : focusMinutes) * 60000L) {
      running = false;
      end = 0;
      session++;
      return;
    }
    remaining = Math.max(0, end - now);
    if (remaining == 0) {
      running = false;
      finished = true;
    }
  }

  public void start(long now, int currentBoot) {
    update(now, currentBoot);
    if (running) return;
    if (finished) {
      breakPhase = !breakPhase;
      remaining = (breakPhase ? breakMinutes : focusMinutes) * 60000L;
      finished = false;
    }
    boot = currentBoot;
    end = now + remaining;
    running = true;
    session++;
  }

  public void pause(long now, int currentBoot) {
    update(now, currentBoot);
    running = false;
    session++;
  }

  public void reset(int focus, int pause, long task) {
    if (!((focus == 25 && pause == 5) || (focus == 50 && pause == 10)))
      throw new IllegalArgumentException("Duration");
    focusMinutes = focus;
    breakMinutes = pause;
    remaining = focus * 60000L;
    running = false;
    finished = false;
    breakPhase = false;
    end = 0;
    taskId = task;
    session++;
  }
}
