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

  private static final class State {
    final List<ImageView> icons = new ArrayList<>();
    final List<TextView> labels = new ArrayList<>();
    final List<Boolean> active = new ArrayList<>();
  }

  public static void attach(BaseActivity activity, FrameLayout root, String selected) {
    View old = root.findViewWithTag("zikraDock");
    if (old != null) root.removeView(old);
    View dock = activity.getLayoutInflater().inflate(R.layout.view_nav_dock, root, false);
    dock.setTag("zikraDock");
    root.addView(dock, new FrameLayout.LayoutParams(-1, activity.dp(64), Gravity.BOTTOM));
    LinearLayout items = dock.findViewById(R.id.navItems);
    State state = new State();
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
    setNightMode(root, false);
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
    glyph.setImageResource(icon);
    TextView text = row.findViewById(R.id.navDestinationLabel);
    text.setText(label);
    text.setTextSize(10);
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
                          : Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        });
    items.addView(row, new LinearLayout.LayoutParams(0, page.dp(64), 1));
    state.icons.add(glyph);
    state.labels.add(text);
    state.active.add(key.equals(selected));
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
      state.labels.get(i).setTextColor(color);
    }
  }
}
