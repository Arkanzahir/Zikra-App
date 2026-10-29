package com.zikra.app;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Shared, compact primary navigation, including Home and Study/Hifz. */
public final class NavDock {
  private NavDock() {}

  public static int surfaceColor(android.content.Context context) {
    return com.zikra.app.ui.AppAppearance.dark(context) ? 0xFF142822 : 0xFFFFFFFF;
  }

  private static final class State {
    final List<ImageView> icons = new ArrayList<>();
    final List<TextView> labels = new ArrayList<>();
    final List<Boolean> active = new ArrayList<>();
    final List<View> indicators = new ArrayList<>();
    View content;
    int extraInset;
    int appliedInset;
  }

  public static void attach(BaseActivity activity, FrameLayout root, String selected) {
    View old = root.findViewWithTag("zikraDock");
    if (old != null) {
      setVisible(root, false);
      root.removeView(old);
    }
    View dock = activity.getLayoutInflater().inflate(R.layout.view_nav_dock, root, false);
    dock.setTag("zikraDock");
    int baseHeight = 64;
    int height =
        activity.dp(
            baseHeight
                + 32
                    * Math.max(
                        0f,
                        Math.min(1f, activity.getResources().getConfiguration().fontScale - 1f)));
    root.addView(dock, new FrameLayout.LayoutParams(-1, height, Gravity.BOTTOM));
    LinearLayout items = dock.findViewById(R.id.navItems);
    State state = new State();
    state.content = root.getChildAt(0);
    state.extraInset = height - activity.dp(baseHeight);
    items.setTag(state);
    add(
        activity,
        items,
        state,
        "Home",
        "home",
        R.drawable.ic_home,
        HomeActivity.class,
        selected,
        R.id.dockHome);
    add(
        activity,
        items,
        state,
        "Quran",
        "quran",
        R.drawable.ic_book,
        QuranActivity.class,
        selected,
        R.id.dockQuran);
    add(
        activity,
        items,
        state,
        "Zikra AI",
        "ai",
        R.drawable.ic_spark,
        AiActivity.class,
        selected,
        R.id.dockAi);
    add(
        activity,
        items,
        state,
        "Study",
        "study",
        R.drawable.ic_tasks,
        StudyActivity.class,
        selected,
        R.id.dockStudy);
    add(
        activity,
        items,
        state,
        "Profil",
        "profile",
        R.drawable.ic_user,
        ProfileActivity.class,
        selected,
        R.id.dockProfile);
    setNightMode(root, com.zikra.app.ui.AppAppearance.dark(activity));
    setVisible(root, true);
  }

  /** Keep existing 64dp reservations and add only the extra space needed for large text. */
  public static void setVisible(FrameLayout root, boolean visible) {
    View dock = root.findViewWithTag("zikraDock");
    if (dock == null) return;
    State state = (State) dock.findViewById(R.id.navItems).getTag();
    int wanted = visible ? state.extraInset : 0;
    if (state.content != null && state.appliedInset != wanted) {
      View content = state.content;
      content.setPadding(
          content.getPaddingLeft(),
          content.getPaddingTop(),
          content.getPaddingRight(),
          Math.max(0, content.getPaddingBottom() + wanted - state.appliedInset));
      state.appliedInset = wanted;
    }
    dock.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  private static void add(
      BaseActivity page,
      LinearLayout items,
      State state,
      String label,
      String key,
      int icon,
      Class<? extends BaseActivity> target,
      String selected,
      int id) {
    View row = page.getLayoutInflater().inflate(R.layout.item_nav_destination, items, false);
    row.setId(id);
    row.setContentDescription(label);
    row.setSelected(key.equals(selected));
    ImageView glyph = row.findViewById(R.id.navDestinationIcon);
    glyph.setImageResource(com.zikra.app.ui.FeatureIcons.destination(key, icon));
    TextView text = row.findViewById(R.id.navDestinationLabel);
    boolean largeType = page.getResources().getConfiguration().fontScale >= 1.5f;
    text.setText(key.equals("ai") && largeType ? "Zikra\nAI" : label);
    text.setMaxLines(key.equals("ai") ? 2 : 1);
    // Keep whole destination names on narrow screens. Labels still enlarge with the system;
    // bound only this compact navigation text while the page content keeps the full font scale.
    text.setAutoSizeTextTypeUniformWithConfiguration(
        10, 12, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    text.setTypeface(
        page.getResources().getFont(R.font.plus_jakarta_sans),
        key.equals(selected) ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    View indicator = row.findViewById(R.id.navDestinationIndicator);
    indicator.setVisibility(View.GONE);
    glyph.setBackgroundResource(key.equals(selected) ? R.drawable.concept_mint_card : 0);
    row.setAccessibilityDelegate(
        new View.AccessibilityDelegate() {
          @Override
          public void onInitializeAccessibilityNodeInfo(
              View host, android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(android.widget.Button.class.getName());
            info.setSelected(host.isSelected());
          }
        });
    row.setOnClickListener(
        v -> {
          if (page.getClass() == target) {
            if (target == HomeActivity.class)
              ((android.widget.ScrollView) page.findViewById(R.id.homeScroll)).smoothScrollTo(0, 0);
            else if (target == StudyActivity.class && page.getIntent().hasExtra("category")) {
              page.startActivity(new Intent(page, StudyActivity.class));
              page.finish();
            }
            return;
          }
          page.startActivity(
              new Intent(page, target)
                  .addFlags(
                      target == HomeActivity.class
                          ? Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                          : target == StudyActivity.class
                              ? 0 // A primary Study destination must never reuse a scoped
                              // Hifz/Ibadah page.
                              : Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        });
    items.addView(row, new LinearLayout.LayoutParams(0, -1, 1));
    state.icons.add(glyph);
    state.labels.add(text);
    state.active.add(key.equals(selected));
    state.indicators.add(indicator);
  }

  public static void setNightMode(FrameLayout root, boolean night) {
    View dock = root.findViewWithTag("zikraDock");
    if (dock == null) return;
    dock.findViewById(R.id.navSurface).setBackgroundColor(night ? 0xFF142822 : 0xFFFFFFFF);
    dock.findViewById(R.id.navDivider).setBackgroundColor(night ? 0xFF29413D : 0xFFE5EBE7);
    State state = (State) dock.findViewById(R.id.navItems).getTag();
    for (int i = 0; i < state.icons.size(); i++) {
      int color =
          state.active.get(i)
              ? (night ? 0xFFF5F7F6 : 0xFF15332F)
              : (night ? 0xFFA2B5AD : 0xFF65766F);
      state.icons.get(i).setColorFilter(color);
      state
          .icons
          .get(i)
          .setBackgroundResource(
              state.active.get(i)
                  ? (night ? R.drawable.concept_pine_button : R.drawable.concept_mint_card)
                  : 0);
      state.labels.get(i).setTextColor(color);
      state.indicators.get(i).setBackgroundColor(color);
    }
  }
}
