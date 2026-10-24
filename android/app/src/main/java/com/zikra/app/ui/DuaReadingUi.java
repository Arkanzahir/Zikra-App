package com.zikra.app.ui;

import android.view.View;
import android.widget.TextView;
import com.zikra.app.R;

/** Shared reading hierarchy for bundled prayers and the optional catalogue. */
public final class DuaReadingUi {
  private DuaReadingUi() { }

  public static void bind(View card, String arabic, String latin, String meaning,
      String reference, String sourceDetails) {
    ((TextView) card.findViewById(R.id.duaArabic)).setText(arabic);
    ((TextView) card.findViewById(R.id.duaLatin)).setText(latin);
    card.findViewById(R.id.duaLatin).setVisibility(latin.isEmpty() ? View.GONE : View.VISIBLE);
    card.findViewById(R.id.duaLatinLabel).setVisibility(latin.isEmpty() ? View.GONE : View.VISIBLE);
    ((TextView) card.findViewById(R.id.duaTranslation)).setText(meaning);
    ((TextView) card.findViewById(R.id.duaSource)).setText(reference);
    TextView details = card.findViewById(R.id.duaSourceDetails);
    details.setText(sourceDetails); details.setVisibility(View.GONE);
    TextView toggle = card.findViewById(R.id.duaSourceToggle);
    toggle.setText(R.string.dua_source_show);
    toggle.setOnClickListener(v -> {
      boolean show = details.getVisibility() != View.VISIBLE;
      details.setVisibility(show ? View.VISIBLE : View.GONE);
      toggle.setText(show ? R.string.dua_source_hide : R.string.dua_source_show);
      toggle.setSelected(show);
    });
  }
}
