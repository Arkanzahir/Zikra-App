package com.zikra.app;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.discovery.HijriCalendarModel;
import com.zikra.app.location.LocationStore;
import com.zikra.app.ui.FeatureHeader;
import com.zikra.app.ui.RefinedUi;
import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Locale;

public final class CalendarActivity extends BaseActivity {
  private static final Locale ID = Locale.forLanguageTag("id-ID");
  private HijrahDate month, selected;
  private LocalDate today;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    today = LocationStore.today(this);
    setContentView(R.layout.activity_calendar);
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(
            R.id.toolbarBack,
            R.id.detailTitle,
            R.id.featureSubtitle,
            R.id.calendarToday,
            "Kalender Hijriah",
            getString(R.string.refined_calendar_estimate),
            R.drawable.ai_balanced_calendar,
            "Kembali ke hari ini");
    setupScreen("Kalender Hijriah");
    RefinedUi.chrome(this, findViewById(R.id.utilitiesHeader), false);
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
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .action()
        .setOnClickListener(
            v -> {
              selected = HijrahDate.from(today);
              month = HijriCalendarModel.first(selected);
              render();
            });
    LinearLayout weekdays = findViewById(R.id.calendarWeekdays);
    for (String day : getResources().getStringArray(R.array.refined_weekdays_short)) {
      TextView label = RefinedUi.text(this, day, 13, R.color.refined_muted, false);
      label.setAutoSizeTextTypeUniformWithConfiguration(
          10, 13, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
      label.setGravity(Gravity.CENTER);
      weekdays.addView(label, new LinearLayout.LayoutParams(0, dp(36), 1));
    }

    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    refreshToday(LocationStore.today(this));
  }

  /** Refreshes the real day without discarding a date the user deliberately browsed. */
  void refreshToday(LocalDate current) {
    if (current.equals(today) || selected == null) return;
    boolean followingToday = LocalDate.from(selected).equals(today);
    today = current;
    if (followingToday) {
      selected = HijrahDate.from(today);
      month = HijriCalendarModel.first(selected);
    }
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
                + "\u00a0H");
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
      TextView view = RefinedUi.text(this, "", 15, R.color.refined_ink, false);
      view.setGravity(Gravity.CENTER);
      GridLayout.LayoutParams p =
          new GridLayout.LayoutParams(GridLayout.spec(cell / 7), GridLayout.spec(cell % 7, 1f));
      p.width = 0;
      p.height = dp(56 * Math.max(1, getResources().getConfiguration().fontScale));
      p.setMargins(dp(1), dp(3), dp(1), dp(3));
      grid.addView(view, p);
      if (day < 1 || day > month.lengthOfMonth()) continue;
      HijrahDate date = month.with(ChronoField.DAY_OF_MONTH, day);
      boolean active = date.equals(selected), isToday = LocalDate.from(date).equals(today);
      String number = Integer.toString(day);
      String gregorianDay = Integer.toString(LocalDate.from(date).getDayOfMonth());
      android.text.SpannableString label =
          new android.text.SpannableString(number + "\n" + gregorianDay);
      label.setSpan(
          new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, number.length(), 0);
      label.setSpan(
          new android.text.style.RelativeSizeSpan(.72f), number.length() + 1, label.length(), 0);
      view.setText(label);
      view.setAutoSizeTextTypeUniformWithConfiguration(
          11, 15, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
      android.graphics.drawable.GradientDrawable background =
          RefinedUi.surface(
              this, active ? R.color.refined_primary : R.color.refined_surface, false, 14);
      if (isToday && !active) background.setStroke(dp(2), getColor(R.color.refined_ink));
      view.setBackground(background);
      view.setTextColor(getColor(active ? R.color.refined_on_primary : R.color.refined_ink));
      view.setContentDescription(
          HijriCalendarModel.label(date) + ", " + LocalDate.from(date).format(greg));
      view.setSelected(active);
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
      card.setBackgroundResource(R.drawable.refined_card);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
      p.bottomMargin = dp(12);
      events.addView(card, p);
      card.addView(RefinedUi.text(this, event.title, 15, R.color.refined_ink, true));
      TextView dates =
          RefinedUi.text(
              this,
              HijriCalendarModel.label(event.date) + " · " + event.gregorian.format(greg),
              14,
              R.color.refined_muted,
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
