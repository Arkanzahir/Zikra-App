package com.zikra.app.prayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import com.zikra.app.R;
import com.zikra.app.location.LocationPoint;
import com.zikra.app.location.LocationStore;
import java.time.LocalDate;

public final class PrayerSourceDialog {
  private PrayerSourceDialog() {}

  public static AlertDialog show(Activity activity) {
    PrayerTimetables store = PrayerTimetables.get(activity);
    LocationPoint point = LocationStore.current(activity);
    ScrollView scroll = new ScrollView(activity);
    scroll.setFillViewport(false);
    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    int padding = Math.round(20 * activity.getResources().getDisplayMetrics().density);
    content.setPadding(padding, padding / 2, padding, padding);
    TextView detail = new TextView(activity);
    detail.setTextSize(16);
    detail.setTextColor(activity.getColor(R.color.refined_ink));
    detail.setText(store.description(LocalDate.now(point.zone), point));
    content.addView(detail);
    scroll.addView(content);
    AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.prayer_source_title)
        .setView(scroll).create();
    action(activity, content, R.id.prayerSourceRefresh, R.string.prayer_source_update, () -> {
          Toast.makeText(activity, R.string.prayer_source_updating, Toast.LENGTH_SHORT).show();
          dialog.dismiss();
          store.refresh(true, () -> {
            if (!activity.isFinishing() && !activity.isDestroyed())
              Toast.makeText(activity, store.caption(LocalDate.now(point.zone), point), Toast.LENGTH_LONG).show();
          });
        });
    action(activity, content, R.id.prayerSourceRegions, R.string.prayer_source_region, () -> {
      dialog.dismiss();
      choose(activity);
    });
    action(activity, content, R.id.prayerSourceClose, R.string.prayer_source_close, dialog::dismiss);
    dialog.show();
    return dialog;
  }

  private static void action(Activity activity, LinearLayout content, int id, int label, Runnable click) {
    Button button = new Button(activity);
    button.setId(id);
    button.setText(label);
    button.setTextSize(16);
    button.setAllCaps(false);
    button.setTextColor(activity.getColor(R.color.refined_ink));
    button.setMinHeight(Math.round(48 * activity.getResources().getDisplayMetrics().density));
    LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
    layout.topMargin = Math.round(8 * activity.getResources().getDisplayMetrics().density);
    content.addView(button, layout);
    button.setOnClickListener(v -> click.run());
  }

  private static void choose(Activity activity) {
    PrayerTimetables store = PrayerTimetables.get(activity);
    LocationPoint point = LocationStore.current(activity);
    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    EditText query = new EditText(activity);
    query.setSingleLine(true);
    query.setHint(R.string.prayer_source_search);
    query.setContentDescription(activity.getString(R.string.prayer_source_search));
    content.addView(query);
    ListView list = new ListView(activity);
    ArrayAdapter<PrayerRegion> adapter = new ArrayAdapter<>(activity,
        android.R.layout.simple_list_item_1, new java.util.ArrayList<>(store.regions()));
    list.setAdapter(adapter);
    content.addView(list, new LinearLayout.LayoutParams(-1,
        Math.round(300 * activity.getResources().getDisplayMetrics().density)));
    AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.prayer_source_region)
        .setView(content).setNegativeButton(R.string.prayer_source_close, null).create();
    query.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        adapter.getFilter().filter(s);
      }
      @Override public void afterTextChanged(Editable s) {}
    });
    list.setOnItemClickListener((parent, view, position, id) -> {
      PrayerRegion selected = adapter.getItem(position);
      new AlertDialog.Builder(activity).setTitle(selected.name)
          .setMessage(R.string.prayer_source_region_confirm)
          .setPositiveButton(R.string.prayer_source_use, (d, w) -> {
            store.select(point, selected);
            store.refresh(true, null);
            dialog.dismiss();
          }).setNegativeButton(R.string.prayer_source_close, null).show();
    });
    dialog.show();
  }
}
