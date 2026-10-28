package com.zikra.app.prayer;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import com.zikra.app.PrayerSchedule;
import com.zikra.app.R;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.reminders.PrayerReminders;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Public city tables shared by UI and alarms. Cache reads never request a network connection. */
public final class PrayerTimetables implements PrayerSchedule.Timetable {
  public static final String PREFS = "prayer_timetable_source";
  private static final long DAY = 24 * 60 * 60 * 1000L;
  private static PrayerTimetables instance;
  private final Context context;
  private final SharedPreferences prefs;
  private final List<PrayerRegion> regions;
  private final ConcurrentHashMap<String, PrayerMonth> months = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Long> attempts = new ConcurrentHashMap<>();
  private final AtomicBoolean busy = new AtomicBoolean();
  private final Object refreshLock = new Object();
  private final java.util.List<Runnable> callbacks = new java.util.ArrayList<>();
  private boolean refreshAgain;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private volatile PrayerApi active;

  private PrayerTimetables(Context context) {
    this.context = context.getApplicationContext();
    prefs = this.context.getSharedPreferences(PREFS, 0);
    try (java.io.InputStream in = this.context.getAssets().open("prayer/regions-myquran-v3.json")) {
      java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int count;
      while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
      regions = PrayerRegion.parse(bytes.toString("UTF-8"));
    } catch (Exception unavailable) {
      throw new IllegalStateException("Invalid bundled prayer regions", unavailable);
    }
  }

  public static synchronized PrayerTimetables get(Context context) {
    if (instance == null) {
      instance = new PrayerTimetables(context);
      PrayerSchedule.setTimetable(instance);
    }
    return instance;
  }

  public List<PrayerRegion> regions() { return regions; }

  public PrayerRegion region(LocationPoint point) {
    if (!PrayerRegion.indonesianPoint(point)) return null;
    String id = prefs.getString("region:" + point.key(), "");
    for (PrayerRegion item : regions) if (item.id.equals(id)) return item;
    return PrayerRegion.automatic(regions, point);
  }

  public void select(LocationPoint point, PrayerRegion region) {
    if (!regions.contains(region)) throw new IllegalArgumentException("Unknown prayer region");
    prefs.edit().putString("region:" + point.key(), region.id).commit();
    PrayerReminders.sync(context);
  }

  private File file(PrayerRegion region, YearMonth month) {
    return new File(new File(context.getFilesDir(), "prayer-timetables"), region.id + "-" + month + ".json");
  }

  @Override public PrayerSchedule.Day find(LocalDate date, LocationPoint point) {
    PrayerRegion region = region(point);
    if (region == null) return null;
    PrayerMonth month = cached(region, YearMonth.from(date));
    return month != null && month.supports(point) ? month.day(date) : null;
  }

  private PrayerMonth cached(PrayerRegion region, YearMonth month) {
    String key = region.id + "-" + month;
    PrayerMonth loaded = months.get(key);
    if (loaded != null) return loaded;
    File file = file(region, month);
    if (!file.isFile() || file.length() > 131072) return null;
    try {
      loaded = PrayerMonth.parse(new String(new AtomicFile(file).readFully(), StandardCharsets.UTF_8), region, month);
      months.put(key, loaded);
      return loaded;
    } catch (Exception invalid) { return null; }
  }

  public String caption(LocalDate date, LocationPoint point) {
    return context.getString(find(date, point) != null ? R.string.prayer_source_cached : R.string.prayer_source_local);
  }

  public String description(LocalDate date, LocationPoint point) {
    PrayerRegion region = region(point);
    if (find(date, point) == null) return context.getString(R.string.prayer_source_fallback_detail,
        region == null ? context.getString(R.string.prayer_source_choose_region) : region.name);
    YearMonth month = YearMonth.from(date);
    String stamp = java.time.Instant.ofEpochMilli(file(region, month).lastModified()).atZone(point.zone)
        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH.mm", java.util.Locale.forLanguageTag("id-ID")));
    return context.getString(R.string.prayer_source_cached_detail, region.name,
        month.atDay(1).toString(), month.atEndOfMonth().toString(), stamp);
  }

  public void refresh(boolean force, Runnable done) {
    LocationPoint point = LocationStore.current(context);
    PrayerRegion region = region(point);
    if (region == null) {
      if (done != null) main.post(done);
      synchronized (refreshLock) {
        if (!busy.get()) {
          for (Runnable callback : callbacks) main.post(callback);
          callbacks.clear();
        }
      }
      return;
    }
    synchronized (refreshLock) {
      if (done != null) callbacks.add(done);
      if (!busy.compareAndSet(false, true)) {
        refreshAgain |= force;
        return;
      }
    }
    worker.execute(() -> {
      try (PrayerApi api = new PrayerApi()) {
        active = api;
        YearMonth current = YearMonth.from(LocalDate.now(point.zone));
        for (int offset = 0; offset < 2; offset++) {
          YearMonth period = current.plusMonths(offset);
          String key = region.id + "-" + period;
          long now = System.currentTimeMillis(), modified = file(region, period).lastModified();
          if (!force && cached(region, period) != null && now >= modified && now - modified < DAY) continue;
          Long last = attempts.get(key);
          if (!force && last != null && now >= last && now - last < 15 * 60 * 1000L) continue;
          attempts.put(key, now);
          try {
            String text = api.month(region, period);
            PrayerMonth validated = PrayerMonth.parse(text, region, period);
            if (!validated.supports(point)) throw new IOException("Time zone mismatch");
            AtomicFile target = new AtomicFile(file(region, period));
            FileOutputStream output = null;
            try {
              output = target.startWrite();
              output.write(text.getBytes(StandardCharsets.UTF_8));
              target.finishWrite(output);
            } catch (IOException failure) {
              if (output != null) target.failWrite(output);
              throw failure;
            }
            months.put(key, validated);
          } catch (Exception unavailable) {
            // Retain complete cached months; absent dates use a clearly labelled local fallback.
          }
        }
        prune();
        if (point.key().equals(LocationStore.current(context).key())) PrayerReminders.sync(context);
      } finally {
        active = null;
        boolean repeat;
        java.util.List<Runnable> finished = new java.util.ArrayList<>();
        synchronized (refreshLock) {
          busy.set(false);
          repeat = refreshAgain;
          refreshAgain = false;
          if (!repeat) {
            finished.addAll(callbacks);
            callbacks.clear();
          }
        }
        if (repeat) refresh(true, null);
        else for (Runnable callback : finished) main.post(callback);
        LocationPoint latest = LocationStore.current(context);
        PrayerRegion selected = region(latest);
        if (selected != null && (!latest.key().equals(point.key()) || !selected.id.equals(region.id)))
          refresh(false, null);
      }
    });
  }

  private void prune() {
    File[] files = new File(context.getFilesDir(), "prayer-timetables").listFiles(
        f -> f.isFile() && f.getName().matches("[a-f0-9]{32}-[0-9]{4}-[0-9]{2}\\.json"));
    if (files == null || files.length <= 48) return;
    java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified));
    for (int i = 0; i < files.length - 48; i++)
      if (files[i].delete()) months.remove(files[i].getName().replace(".json", ""));
  }

  public void cancel() { PrayerApi api = active; if (api != null) api.close(); }

  public static void scheduleRefresh(Context context) {
    JobScheduler scheduler = context.getSystemService(JobScheduler.class);
    if (scheduler.getPendingJob(7411) == null) scheduler.schedule(new JobInfo.Builder(7411,
        new ComponentName(context, PrayerRefreshJob.class)).setPersisted(true)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(12 * 60 * 60 * 1000L).build());
  }
}
