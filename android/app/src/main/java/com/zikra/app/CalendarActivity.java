package com.zikra.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.discovery.DiscoveryUi;
import com.zikra.app.discovery.HijriCalendarModel;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Locale;

public final class CalendarActivity extends BaseActivity {
  private static final Locale ID = Locale.forLanguageTag("id-ID");
  private HijrahDate month, selected;
  private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Jakarta"));

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_calendar);
    setupScreen("Kalender Hijriah");
    getWindow().setStatusBarColor(0xFFF8F9F4);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    selected = HijrahDate.from(today);
    if (state != null) {
      try {
        selected = HijrahDate.from(LocalDate.parse(state.getString("selected", today.toString())));
      } catch (java.time.DateTimeException ignored) {
      }
    }
    month = HijriCalendarModel.first(selected);
    findViewById(R.id.calendarPrev).setOnClickListener(v -> shift(-1));
    findViewById(R.id.calendarNext).setOnClickListener(v -> shift(1));
    findViewById(R.id.calendarToday)
        .setOnClickListener(
            v -> {
              selected = HijrahDate.from(today);
              month = HijriCalendarModel.first(selected);
              render();
            });
    LinearLayout weekdays = findViewById(R.id.calendarWeekdays);
    for (String day : new String[] {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"}) {
      TextView label = DiscoveryUi.text(this, day, 11, "#88988D", false);
      label.setGravity(Gravity.CENTER);
      weekdays.addView(label, new LinearLayout.LayoutParams(0, -1, 1));
    }
    NavDock.attach(this, (FrameLayout) findViewById(R.id.calendarRoot), "calendar");
    render();
  }

  private void shift(int amount) {
    if (!HijriCalendarModel.canMove(month, amount)) return;
    int day = selected.get(ChronoField.DAY_OF_MONTH);
    month = HijriCalendarModel.move(month, amount);
    selected = month.with(ChronoField.DAY_OF_MONTH, Math.min(day, month.lengthOfMonth()));
    render();
  }

  private void render() {
    ((TextView) findViewById(R.id.calendarMonth))
        .setText(
            HijriCalendarModel.MONTHS[month.get(ChronoField.MONTH_OF_YEAR) - 1]
                + " "
                + month.get(ChronoField.YEAR)
                + " H");
    DateTimeFormatter greg = DateTimeFormatter.ofPattern("d MMM yyyy", ID);
    ((TextView) findViewById(R.id.calendarGregorian))
        .setText(
            LocalDate.from(month).format(greg)
                + " – "
                + LocalDate.from(month.with(ChronoField.DAY_OF_MONTH, month.lengthOfMonth()))
                    .format(greg));
    findViewById(R.id.calendarPrev).setEnabled(HijriCalendarModel.canMove(month, -1));
    findViewById(R.id.calendarNext).setEnabled(HijriCalendarModel.canMove(month, 1));
    GridLayout grid = findViewById(R.id.calendarGrid);
    grid.removeAllViews();
    int offset = HijriCalendarModel.offset(month);
    int cells = ((offset + month.lengthOfMonth() + 6) / 7) * 7;
    for (int cell = 0; cell < cells; cell++) {
      final int day = cell - offset + 1;
      TextView view = DiscoveryUi.text(this, "", 13, "#315849", false);
      view.setGravity(Gravity.CENTER);
      GridLayout.LayoutParams p =
          new GridLayout.LayoutParams(GridLayout.spec(cell / 7), GridLayout.spec(cell % 7, 1f));
      p.width = 0;
      p.height = dp(42);
      p.setMargins(dp(1), dp(3), dp(1), dp(3));
      grid.addView(view, p);
      if (day < 1 || day > month.lengthOfMonth()) continue;
      HijrahDate date = month.with(ChronoField.DAY_OF_MONTH, day);
      view.setText(Integer.toString(day));
      boolean active = date.equals(selected), isToday = LocalDate.from(date).equals(today);
      view.setBackground(
          DiscoveryUi.background(this, active ? "#57927E" : isToday ? "#E3F1E8" : "#FFFFFF", 24));
      view.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#315849"));
      view.setContentDescription(
          HijriCalendarModel.label(date) + ", " + LocalDate.from(date).format(greg));
      view.setClickable(true);
      view.setFocusable(true);
      view.setOnClickListener(
          v -> {
            selected = date;
            render();
          });
    }
    ((TextView) findViewById(R.id.calendarSelection))
        .setText(
            HijriCalendarModel.label(selected)
                + "\n"
                + LocalDate.from(selected)
                    .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)));
    LinearLayout events = findViewById(R.id.calendarEvents);
    events.removeAllViews();
    for (HijriCalendarModel.Event event :
        HijriCalendarModel.upcoming(LocalDate.from(selected), 4)) {
      LinearLayout card = new LinearLayout(this);
      card.setOrientation(LinearLayout.VERTICAL);
      card.setPadding(dp(16), dp(16), dp(16), dp(16));
      card.setBackgroundResource(R.drawable.calendar_card);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
      p.bottomMargin = dp(12);
      events.addView(card, p);
      card.addView(DiscoveryUi.text(this, event.title, 15, "#315849", true));
      TextView dates =
          DiscoveryUi.text(
              this,
              HijriCalendarModel.label(event.date) + " · " + event.gregorian.format(greg),
              11,
              "#839487",
              false);
      dates.setPadding(0, dp(8), 0, 0);
      card.addView(dates);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("selected", LocalDate.from(selected).toString());
    super.onSaveInstanceState(state);
  }
}
