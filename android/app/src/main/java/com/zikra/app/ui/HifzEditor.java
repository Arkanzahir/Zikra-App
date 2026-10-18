package com.zikra.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.zikra.app.R;
import com.zikra.app.data.HifzTarget;
import com.zikra.app.study.SurahCatalog.Chapter;
import java.util.ArrayList;
import java.util.List;

public final class HifzEditor {
  private final View root;
  private final Spinner picker;
  private final EditText first, last;
  private final List<Chapter> available = new ArrayList<>();
  private Chapter selected;

  public HifzEditor(View root, HifzTarget target) {
    this.root = root;
    picker = root.findViewById(R.id.hifz_surah);
    first = root.findViewById(R.id.hifz_first);
    last = root.findViewById(R.id.hifz_last);
    root.findViewById(R.id.hifz_editor_fields).setVisibility(View.VISIBLE);
    if (target != null) {
      selected = new Chapter(target.chapterId, target.chapterName, target.versesCount);
      first.setText(String.valueOf(target.firstVerse));
      last.setText(String.valueOf(target.lastVerse));
    }
    picker.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position > available.size()) return;
            Chapter next = position == 0 ? null : available.get(position - 1);
            if (next == null && selected == null) return;
            if (next != null && selected != null && next.id == selected.id) {
              selected = next;
              preview();
              return;
            }
            selected = next;
            first.setText(next == null ? "" : "1");
            last.setText(next == null ? "" : "1");
            preview();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    TextWatcher watcher =
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            preview();
          }

          @Override
          public void afterTextChanged(Editable e) {}
        };
    first.addTextChangedListener(watcher);
    last.addTextChangedListener(watcher);
    preview();
  }

  public void catalog(List<Chapter> chapters, boolean loading, Runnable reload) {
    available.clear();
    available.addAll(chapters);
    if (selected != null) {
      boolean found = false;
      for (Chapter chapter : available) if (chapter.id == selected.id) found = true;
      if (!found) available.add(selected);
    }
    List<String> names = new ArrayList<>();
    names.add(root.getContext().getString(R.string.hifz_choose));
    int index = 0;
    for (Chapter chapter : available) {
      names.add(chapter.id + ". " + chapter.name);
      if (selected != null && chapter.id == selected.id) index = names.size() - 1;
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(root.getContext(), R.layout.hifz_surah_selected, names);
    adapter.setDropDownViewResource(R.layout.hifz_surah_row);
    picker.setAdapter(adapter);
    picker.setSelection(index, false);
    ((TextView) root.findViewById(R.id.hifz_catalog_status))
        .setText(
            loading
                ? R.string.hifz_catalog_loading
                : chapters.size() == 114
                    ? R.string.hifz_catalog_ready
                    : R.string.hifz_catalog_offline);
    View retry = root.findViewById(R.id.hifz_catalog_reload);
    retry.setVisibility(chapters.size() == 114 ? View.GONE : View.VISIBLE);
    retry.setEnabled(!loading);
    retry.setOnClickListener(v -> reload.run());
  }

  private HifzTarget parse() {
    if (selected == null) return null;
    try {
      return new HifzTarget(
          selected.id,
          selected.name,
          selected.versesCount,
          Integer.parseInt(first.getText().toString()),
          Integer.parseInt(last.getText().toString()));
    } catch (IllegalArgumentException invalid) {
      return null;
    }
  }

  public HifzTarget target() {
    HifzTarget value = parse();
    if (value != null) return value;
    TextView error = root.findViewById(R.id.hifz_validation);
    error.setText(
        selected == null
            ? root.getContext().getString(R.string.hifz_required)
            : root.getContext().getString(R.string.hifz_invalid, selected.versesCount));
    error.setVisibility(View.VISIBLE);
    error.post(
        () ->
            error.requestRectangleOnScreen(
                new android.graphics.Rect(0, 0, error.getWidth(), error.getHeight()), false));
    return null;
  }

  private void preview() {
    HifzTarget value = parse();
    ((TextView) root.findViewById(R.id.hifz_range_preview))
        .setText(
            value == null
                ? root.getContext().getString(R.string.hifz_range_hint)
                : root.getContext()
                    .getString(
                        R.string.hifz_range, value.firstVerse, value.lastVerse, value.count()));
    root.findViewById(R.id.hifz_validation).setVisibility(View.GONE);
  }

  public void save(Bundle state) {
    if (selected != null) {
      state.putInt("hifz_id", selected.id);
      state.putInt("hifz_count", selected.versesCount);
      state.putString("hifz_name", selected.name);
    } else {
      state.remove("hifz_id");
      state.remove("hifz_count");
      state.remove("hifz_name");
    }
    state.putString("hifz_first", first.getText().toString());
    state.putString("hifz_last", last.getText().toString());
  }

  public void restore(Bundle state, List<Chapter> chapters, boolean loading, Runnable reload) {
    int id = state.getInt("hifz_id", 0);
    if (id > 0)
      try {
        selected = new Chapter(id, state.getString("hifz_name", ""), state.getInt("hifz_count"));
      } catch (IllegalArgumentException invalid) {
        selected = null;
      }
    catalog(chapters, loading, reload);
    first.setText(state.getString("hifz_first", ""));
    last.setText(state.getString("hifz_last", ""));
    preview();
  }
}
