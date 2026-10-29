package com.zikra.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import com.zikra.app.reminders.*;
import android.app.AlertDialog;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** Daily native timetable with per-prayer Android notification reminders. */
public final class PrayerActivity extends BaseActivity {
  private LocationPoint location = LocationPoint.INITIAL;
  private ZoneId localZone = location.zone;
  private static final Locale ID = Locale.forLanguageTag("id-ID");
  private static final int[] PRAYER_INDEXES = {0, 2, 3, 4, 5};
  private static final String[] PRAYER_KEYS = {"subuh", "dzuhur", "ashar", "maghrib", "isya"};
  private final Handler clock = new Handler(Looper.getMainLooper());
  private final Runnable updateClock =
      new Runnable() {
        @Override
        public void run() {
          LocalDate today = LocalDate.now(localZone);
          if (!today.equals(lastClockDate)) {
            lastClockDate = today;
            selectedDate = today;
            renderDates();
            renderRows();
          }
          renderCountdown();
          PrayerSchedule.Day source = PrayerSchedule.forDate(selectedDate, location);
          if (source != lastTimetableSource) {
            lastTimetableSource = source;
            renderRows();
            renderWeek();
            updateScheduleSource();
          }
          updateAudioUi();
          clock.postDelayed(this, 1000);
        }
      };
  private SharedPreferences preferences;
  private LinearLayout dates, rows, weekSummary;
  private TextView todayTab, weekTab;
  private LocalDate selectedDate;
  private LocalDate lastClockDate;
  private boolean weekMode;
  private int previousNext = -1;
  private boolean applyingMaster;
  private PrayerSchedule.Day lastTimetableSource;

  private void updateScheduleSource() {
    TextView method = findViewById(R.id.prayerMethod);
    method.setText(com.zikra.app.prayer.PrayerTimetables.get(this).caption(selectedDate, location));
    method.setMinHeight(dp(48));
    method.setFocusable(true);
    method.setOnClickListener(v -> com.zikra.app.prayer.PrayerSourceDialog.show(this));
  }
  private AlertDialog modeDialog, previewDialog;
  private int editingPrayer=-1, draftMode, pendingPreview=-1;
  private static final int REQUEST_NOTIFICATIONS = 401;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_prayer);
    location = LocationStore.current(this);
    localZone = location.zone;
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.sharedFeatureHeader))
        .bind(
            R.id.toolbarBack,
            R.id.detailTitle,
            R.id.featureSubtitle,
            R.id.prayerOpenQibla,
            "Waktu Sholat",
            location.caption(),
            R.drawable.ic_compass,
            "Buka arah kiblat");
    setupScreen("Waktu Sholat");
    getWindow().setStatusBarColor(getColor(R.color.ivory));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    selectedDate = LocalDate.now(localZone);
    lastClockDate = selectedDate;
    if (state != null) {
      try {
        selectedDate = LocalDate.parse(state.getString("selected_date", selectedDate.toString()));
      } catch (java.time.DateTimeException ignored) {
      }
      weekMode = state.getBoolean("week_mode", false);
    }
    dates = findViewById(R.id.prayerWeekDates);
    rows = findViewById(R.id.prayerRows);
    weekSummary = findViewById(R.id.prayerWeekSummary);
    todayTab = findViewById(R.id.prayerTodayTab);
    weekTab = findViewById(R.id.prayerWeekTab);

    todayTab.setOnClickListener(
        view -> {
          selectedDate = LocalDate.now(localZone);
          selectMode(false);
        });
    weekTab.setOnClickListener(view -> selectMode(true));
    ((com.zikra.app.ui.FeatureHeader) findViewById(R.id.sharedFeatureHeader))
        .action()
        .setOnClickListener(view -> open(QiblaActivity.class));
    findViewById(R.id.prayerLocation).setOnClickListener(v -> open(LocationActivity.class));
    findViewById(R.id.prayerChecklist)
        .setOnClickListener(
            view ->
                startActivity(
                    new Intent(this, StudyActivity.class)
                        .putExtra("category", com.zikra.app.data.ZikraDbHelper.CATEGORY_IBADAH)));
    Switch master = findViewById(R.id.prayerReminderEnabled);
    master.setOnCheckedChangeListener(
        (view, enabled) -> {
          if (applyingMaster) return;
          if (enabled
              && Build.VERSION.SDK_INT >= 33
              && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                  != PackageManager.PERMISSION_GRANTED) {
            applyingMaster = true;
            master.setChecked(false);
            applyingMaster = false;
            requestPermissions(
                new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
          }
          preferences.edit().putBoolean("reminder_enabled", enabled).apply();
          if(!enabled)AdhanService.stop(this);
          PrayerReminders.sync(this);
          renderRows();
          updateReminderUi();
        });
    findViewById(R.id.prayerAlarmSettings).setOnClickListener(view -> openReminderSettings());
    findViewById(R.id.prayerBannerSettings).setOnClickListener(view -> openBannerSettings());
    Switch pause=findViewById(R.id.prayerPauseFocus), quiet=findViewById(R.id.prayerQuietClasses);
    pause.setChecked(preferences.getBoolean(PrayerSettings.PAUSE_FOCUS,false));
    quiet.setChecked(preferences.getBoolean(PrayerSettings.QUIET_CLASSES,false));
    pause.setOnCheckedChangeListener((v,on)->preferences.edit().putBoolean(PrayerSettings.PAUSE_FOCUS,on).apply());
    quiet.setOnCheckedChangeListener((v,on)->{preferences.edit().putBoolean(PrayerSettings.QUIET_CLASSES,on).apply();AdhanService.stopIfInvalid();});
    findViewById(R.id.prayerAudioPreview).setOnClickListener(v->previewDialog=new AlertDialog.Builder(this)
        .setTitle(R.string.adhan_preview).setItems(R.array.adhan_preview_choices,(dialog,choice)->preview(choice==1?0:2)).show());
    findViewById(R.id.prayerAudioStop).setOnClickListener(v->{AdhanService.stop(this);updateAudioUi();});
    selectMode(weekMode);
    if(state!=null&&state.getInt("editing_prayer",-1)>=0){draftMode=state.getInt("draft_mode");showMode(state.getInt("editing_prayer"),true);}
    NavDock.attach(this, (FrameLayout) findViewById(R.id.prayerRoot), "prayer");
  }

  @Override
  protected void onResume() {
    super.onResume();
    LocationPoint selected = LocationStore.current(this);
    if (!selected.key().equals(location.key())) selectedDate = LocalDate.now(selected.zone);
    location = selected;
    localZone = location.zone;
    ((TextView) findViewById(R.id.featureSubtitle)).setText(location.caption());
    updateScheduleSource();
    com.zikra.app.prayer.PrayerTimetables.get(this).refresh(false, () -> {
      if (!isFinishing() && !isDestroyed()) {
        renderRows();
        renderWeek();
        updateScheduleSource();
      }
    });
    PrayerReminders.sync(this);
    updateReminderUi();
    renderDates();
    renderWeek();
    renderRows();
    clock.removeCallbacks(updateClock);
    clock.post(updateClock);
  }

  @Override
  protected void onPause() {
    clock.removeCallbacks(updateClock);
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("selected_date", selectedDate.toString());
    state.putBoolean("week_mode", weekMode);
    if(modeDialog!=null&&modeDialog.isShowing()){state.putInt("editing_prayer",editingPrayer);state.putInt("draft_mode",draftMode);}
    super.onSaveInstanceState(state);
  }

  private void selectMode(boolean week) {
    weekMode = week;
    todayTab.setBackgroundResource(week ? 0 : R.drawable.prayer_bg_green);
    weekTab.setBackgroundResource(week ? R.drawable.prayer_bg_green : 0);
    todayTab.setTextColor(getColor(week ? R.color.muted : R.color.white));
    weekTab.setTextColor(getColor(week ? R.color.white : R.color.muted));
    rows.setVisibility(week ? View.GONE : View.VISIBLE);
    weekSummary.setVisibility(week ? View.VISIBLE : View.GONE);
    renderDates();
    renderRows();
    renderWeek();
    renderCountdown();
  }

  private void renderDates() {
    dates.removeAllViews();
    LocalDate weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    for (int i = 0; i < 7; i++) {
      LocalDate day = weekStart.plusDays(i);
      LinearLayout item =
          (LinearLayout) getLayoutInflater().inflate(R.layout.prayer_day, dates, false);
      item.getLayoutParams().width =
          dp(getResources().getConfiguration().fontScale > 1.3f ? 64 : 52);
      TextView name = item.findViewById(R.id.prayerDayName);
      TextView number = item.findViewById(R.id.prayerDayNumber);
      name.setText(day.format(DateTimeFormatter.ofPattern("EEE", ID)).toUpperCase(ID));
      number.setText(Integer.toString(day.getDayOfMonth()));
      boolean selected = day.equals(selectedDate);
      item.setBackgroundResource(selected ? R.drawable.prayer_bg_green : 0);
      name.setTextColor(getColor(selected ? R.color.white : R.color.muted));
      number.setTextColor(getColor(selected ? R.color.white : R.color.text));
      item.setContentDescription(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)));
      item.setOnClickListener(
          view -> {
            selectedDate = day;
            renderDates();
            renderRows();
            renderCountdown();
          });
      dates.addView(item);
    }
    dates.post(
        () -> {
          if (dates.getParent() instanceof android.widget.HorizontalScrollView) {
            int selected = selectedDate.getDayOfWeek().getValue() - 1;
            View child = dates.getChildAt(selected);
            if (child != null)
              ((android.widget.HorizontalScrollView) dates.getParent())
                  .smoothScrollTo(
                      Math.max(
                          0,
                          child.getLeft()
                              - (dates.getResources().getDisplayMetrics().widthPixels
                                      - child.getWidth())
                                  / 2),
                      0);
          }
        });
    String caption = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID));
    try {
      caption +=
          " · "
              + DateTimeFormatter.ofPattern("d MMMM yyyy", ID).format(HijrahDate.from(selectedDate))
              + " H";
    } catch (java.time.DateTimeException ignored) {
      /* Gregorian caption is still useful outside the Hijrah calendar range. */
    }
    ((TextView) findViewById(R.id.prayerDateCaption)).setText(caption);
    ((TextView) findViewById(R.id.prayerListTitle))
        .setText(
            weekMode
                ? "Jadwal pekan ini"
                : selectedDate.equals(LocalDate.now(localZone))
                    ? "Jadwal hari ini"
                    : selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)));
  }

  private void renderRows() {
    rows.removeAllViews();
    ZonedDateTime now = ZonedDateTime.now(localZone);
    PrayerSchedule.NextPrayer upcoming = PrayerSchedule.nextAfter(now, location);
    int next = upcoming == null ? -1 : upcoming.index;
    PrayerSchedule.Day timetable = PrayerSchedule.forDate(selectedDate, location);
    ((TextView) findViewById(R.id.prayerSunrise)).setText(timetable.time(1));
    ((TextView) findViewById(R.id.prayerSunset)).setText(timetable.time(4));
    boolean today = selectedDate.equals(LocalDate.now(localZone));
    boolean master = preferences.getBoolean("reminder_enabled", false);
    for (int item = 0; item < PRAYER_INDEXES.length; item++) {
      final String key = "reminder_" + PRAYER_KEYS[item];
      final int index = PRAYER_INDEXES[item];
      LinearLayout row =
          (LinearLayout) getLayoutInflater().inflate(R.layout.prayer_item, rows, false);
      if(getResources().getConfiguration().fontScale>=1.5f){
        TextView time=row.findViewById(R.id.prayerRowTime);
        ((android.view.ViewGroup)time.getParent()).removeView(time);
        LinearLayout column=(LinearLayout)row.findViewById(R.id.prayerRowName).getParent();
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(4);
        column.addView(time,1,params);
      }
      row.setBackgroundResource(
          today
                  && upcoming != null
                  && upcoming.time.toLocalDate().equals(selectedDate)
                  && next == index
              ? R.drawable.prayer_bg_active
              : R.drawable.prayer_bg_row);
      ((TextView) row.findViewById(R.id.prayerRowName)).setText(PrayerSchedule.NAMES[index]);
      ((TextView) row.findViewById(R.id.prayerRowTime)).setText(timetable.time(index));
      ((ImageView) row.findViewById(R.id.prayerRowIcon))
          .setImageResource(index == 0 || index == 5 ? R.drawable.ic_moon : R.drawable.ic_sun);
      ImageButton bell = row.findViewById(R.id.prayerRowBell);
      PrayerMode mode=PrayerSettings.mode(this,index);
      String label=getResources().getStringArray(R.array.prayer_mode_choices)[mode.ordinal()];
      ((TextView)row.findViewById(R.id.prayerRowMode)).setText(label);
      bell.setAlpha(master&&mode!=PrayerMode.OFF?1f:.3f);bell.setSelected(mode!=PrayerMode.OFF);
      bell.setImageResource(mode==PrayerMode.ADHAN?R.drawable.reading_audio_speaker:R.drawable.ic_bell);
      bell.setContentDescription(getString(R.string.adhan_mode_title,PrayerSchedule.NAMES[index])+" · "+label);
      bell.setOnClickListener(view->showMode(index,false));row.setOnClickListener(view->showMode(index,false));
      rows.addView(row);
    }
    previousNext = next;
  }

  private void showMode(int index,boolean restoring) {
    if(!PrayerSchedule.isPrayer(index))return;
    editingPrayer=index;if(!restoring)draftMode=PrayerSettings.mode(this,index).ordinal();
    modeDialog=new AlertDialog.Builder(this).setTitle(getString(R.string.adhan_mode_title,PrayerSchedule.NAMES[index]))
        .setSingleChoiceItems(R.array.prayer_mode_choices,draftMode,(dialog,choice)->draftMode=choice)
        .setNegativeButton(R.string.weekly_cancel,null).setPositiveButton(R.string.weekly_save,null).create();
    modeDialog.setOnShowListener(dialog->modeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
      PrayerMode selected=PrayerMode.values()[draftMode];
      if(selected==PrayerMode.ADHAN&&!AdhanAsset.available(this,index)){toast(getString(R.string.adhan_audio_missing));return;}
      PrayerSettings.setMode(this,index,selected);modeDialog.dismiss();editingPrayer=-1;
      PrayerReminders.sync(this);renderRows();updateReminderUi();
    }));modeDialog.show();
  }
  private void preview(int index) {
    if(!AdhanAsset.available(this,index)){toast(getString(R.string.adhan_audio_missing));return;}
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
      pendingPreview=index;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},402);return;
    }
    if(!AdhanService.preview(this,index))toast(getString(R.string.adhan_unavailable));
  }
  private void updateAudioUi() {
    AdhanService.State state=AdhanService.state();boolean present=AdhanAsset.available(this,0)&&AdhanAsset.available(this,2);
    findViewById(R.id.prayerAudioStop).setVisibility(state.active?View.VISIBLE:View.GONE);
    findViewById(R.id.prayerAudioPreview).setEnabled(present&&!state.active);
    String message=!state.status.isEmpty()?state.status:getString(present?R.string.adhan_audio_label:R.string.adhan_audio_missing);
    TextView label=findViewById(R.id.prayerAudioStatus);if(!message.contentEquals(label.getText()))label.setText(message);
    ((TextView)findViewById(R.id.prayerBannerHint)).setText(
        !PrayerReminders.canNotify(this)?R.string.adhan_permission_needed:
        PrayerReminders.channelAllowed(this,AdhanNotification.CHANNEL)?R.string.adhan_banner_hint:R.string.adhan_banner_blocked);
  }
  private void openBannerSettings(){
    PrayerReminders.createChannel(this);
    Intent intent=new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,AdhanNotification.CHANNEL);
    try{startActivity(intent);}catch(ActivityNotFoundException unavailable){toast(getString(R.string.adhan_settings_unavailable));}
  }
  @Override protected void onDestroy(){
    if(modeDialog!=null)modeDialog.dismiss();if(previewDialog!=null)previewDialog.dismiss();super.onDestroy();
  }

  private void renderCountdown() {
    ZonedDateTime now = ZonedDateTime.now(localZone);
    boolean today = selectedDate.equals(now.toLocalDate());
    PrayerSchedule.NextPrayer next = PrayerSchedule.nextAfter(now, location);
    PrayerSchedule.Day selected = PrayerSchedule.forDate(selectedDate, location);
    if (next == null) {
      ((TextView) findViewById(R.id.prayerNextLabel)).setText("Perhitungan belum tersedia");
      ((TextView) findViewById(R.id.prayerNextName))
          .setText("Cocokkan jadwal dengan masjid setempat");
      ((TextView) findViewById(R.id.prayerCountdown)).setText("—");
      return;
    }
    if (today && previousNext != next.index) renderRows();
    ((TextView) findViewById(R.id.prayerNextLabel))
        .setText(
            today
                ? "Sholat selanjutnya"
                    + (next.time.toLocalDate().isAfter(now.toLocalDate()) ? " · besok" : "")
                : selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)));
    ((TextView) findViewById(R.id.prayerNextName))
        .setText(
            today
                ? PrayerSchedule.NAMES[next.index]
                    + " · "
                    + next.time.format(DateTimeFormatter.ofPattern("HH:mm"))
                : PrayerSchedule.NAMES[0] + " · " + selected.time(0));
    TextView countdown = findViewById(R.id.prayerCountdown);
    if (!today) {
      countdown.setText(location.zoneLabel(java.time.Instant.now()));
      countdown.setTextSize(21);
      return;
    }
    long seconds = Duration.between(now, next.time).getSeconds();
    countdown.setText(
        String.format(ID, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60));
    countdown.setTextSize(27);
  }

  private void updateReminderUi() {
    applyingMaster = true;
    ((Switch) findViewById(R.id.prayerReminderEnabled)).setChecked(PrayerReminders.enabled(this));
    applyingMaster = false;
    boolean allowed = PrayerReminders.canNotify(this);
    ((TextView) findViewById(R.id.prayerReminderStatus))
        .setText(
            !PrayerReminders.enabled(this)
                ? "Aktifkan untuk menerima notifikasi waktu sholat."
                : !allowed
                    ? "Notifikasi belum diizinkan di pengaturan perangkat."
                    : PrayerReminders.exactAllowed(this)
                        ? "Pengingat aktif sesuai jadwal."
                        : "Pengingat aktif; ketepatan waktu mengikuti pengaturan perangkat.");
    ((TextView) findViewById(R.id.prayerAlarmSettings))
        .setText(
            !allowed
                ? "Atur izin notifikasi"
                : !PrayerReminders.exactAllowed(this)
                    ? "Atur ketepatan pengingat"
                    : "Pengaturan notifikasi");
  }

  private void openReminderSettings() {
    Intent intent;
    if (PrayerReminders.canNotify(this)
        && !PrayerReminders.exactAllowed(this)
        && Build.VERSION.SDK_INT >= 31) {
      intent =
          new Intent(
              Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
              Uri.parse("package:" + getPackageName()));
    } else {
      intent =
          new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
              .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
    }
    try {
      startActivity(intent);
    } catch (ActivityNotFoundException unavailable) {
      toast("Pengaturan tidak tersedia pada perangkat ini.");
    }
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(request, permissions, grants);
    if(request==402){int index=pendingPreview;pendingPreview=-1;
      if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED&&PrayerSchedule.isPrayer(index))preview(index);
      else toast(getString(R.string.adhan_permission_needed));return;
    }
    if (request == REQUEST_NOTIFICATIONS) {
      preferences
          .edit()
          .putBoolean(
              "reminder_enabled",
              grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED)
          .apply();
      PrayerReminders.sync(this);
      updateReminderUi();
      renderRows();
    }
  }

  private void renderWeek() {
    weekSummary.removeAllViews();
    LocalDate monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    for (int day = 0; day < 7; day++) {
      LocalDate date = monday.plusDays(day);
      PrayerSchedule.Day timetable = PrayerSchedule.forDate(date, location);
      TextView item = new TextView(this);
      item.setText(
          date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", ID))
              + "\nSubuh  "
              + timetable.time(0)
              + "    Dzuhur  "
              + timetable.time(2)
              + "    Ashar  "
              + timetable.time(3)
              + "\nMaghrib  "
              + timetable.time(4)
              + "    Isya  "
              + timetable.time(5));
      item.setTextSize(14);
      item.setTextColor(getColor(R.color.muted));
      item.setLineSpacing(dp(5), 1);
      item.setPadding(dp(14), dp(11), dp(14), dp(11));
      item.setBackgroundResource(
          date.equals(LocalDate.now(localZone))
              ? R.drawable.prayer_bg_active
              : R.drawable.prayer_bg_row);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
      params.bottomMargin = dp(7);
      weekSummary.addView(item, params);
    }
  }
}
