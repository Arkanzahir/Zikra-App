package com.zikra.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Shared XML-based floating navigation for the native application and worship pages. */
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
    FrameLayout.LayoutParams placement =
        new FrameLayout.LayoutParams(-1, activity.dp(88), Gravity.BOTTOM);
    placement.setMargins(activity.dp(16), 0, activity.dp(16), activity.dp(24));
    root.addView(dock, placement);
    LinearLayout items = dock.findViewById(R.id.navItems);
    State state = new State();
    items.setTag(state);
    boolean faith = "qibla".equals(selected) || "prayer".equals(selected) || "dua".equals(selected);
    if (faith) {
      dock.findViewById(R.id.navAiFloating).setVisibility(View.GONE);
      add(activity, items, state, "Home", "home", R.drawable.ic_home, HomeActivity.class, selected);
      add(
          activity,
          items,
          state,
          "Waktu",
          "prayer",
          R.drawable.ic_sun,
          PrayerActivity.class,
          selected);
      add(
          activity,
          items,
          state,
          "Kiblat",
          "qibla",
          R.drawable.ic_compass,
          QiblaActivity.class,
          selected);
      add(activity, items, state, "Doa", "dua", R.drawable.ic_heart, DuaActivity.class, selected);
    } else {
      add(activity, items, state, "Home", "home", R.drawable.ic_home, HomeActivity.class, selected);
      add(
          activity,
          items,
          state,
          "Quran",
          "quran",
          R.drawable.ic_book,
          QuranActivity.class,
          selected);
      View gap = new View(activity);
      items.addView(gap, new LinearLayout.LayoutParams(0, 1, 1));
      add(
          activity,
          items,
          state,
          "Study",
          "study",
          R.drawable.ic_tasks,
          StudyActivity.class,
          selected);
      add(
          activity,
          items,
          state,
          "Profil",
          "profile",
          R.drawable.ic_user,
          ProfileActivity.class,
          selected);
      dock.findViewById(R.id.navAiFloating)
          .setOnClickListener(v -> navigate(activity, AiActivity.class));
    }
    setNightMode(root, false);
  }

  private static void add(
      BaseActivity activity,
      LinearLayout items,
      State state,
      String label,
      String key,
      int icon,
      Class<? extends Activity> target,
      String selected) {
    View item = activity.getLayoutInflater().inflate(R.layout.item_nav_destination, items, false);
    item.setContentDescription(label);
    ImageView glyph = item.findViewById(R.id.navDestinationIcon);
    glyph.setImageResource(icon);
    TextView caption = item.findViewById(R.id.navDestinationLabel);
    caption.setText(label);
    item.setOnClickListener(v -> navigate(activity, target));
    items.addView(item, new LinearLayout.LayoutParams(0, activity.dp(64), 1));
    state.icons.add(glyph);
    state.labels.add(caption);
    state.active.add(key.equals(selected));
  }

  private static void navigate(BaseActivity activity, Class<? extends Activity> target) {
    if (activity.getClass() == target) return;
    Intent intent = new Intent(activity, target);
    intent.addFlags(
        target == HomeActivity.class
            ? Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
            : Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    activity.startActivity(intent);
  }

  public static void setNightMode(FrameLayout root, boolean night) {
    View dock = root.findViewWithTag("zikraDock");
    if (dock == null) return;
    GradientDrawable surface = new GradientDrawable();
    surface.setColor(night ? 0xFF1D3530 : 0xFFFFFFFF);
    surface.setCornerRadius(999 * root.getResources().getDisplayMetrics().density);
    dock.findViewById(R.id.navSurface).setBackground(surface);
    State state = (State) dock.findViewById(R.id.navItems).getTag();
    for (int i = 0; i < state.icons.size(); i++) {
      int color = state.active.get(i) ? 0xFF10B981 : night ? 0xFFB4C8BE : 0xFF8A9BA9;
      state.icons.get(i).setColorFilter(color);
      state.labels.get(i).setTextColor(color);
    }
  }
}
