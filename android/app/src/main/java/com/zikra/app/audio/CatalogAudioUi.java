package com.zikra.app.audio;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import com.zikra.app.R;
import com.zikra.app.reading.QuranChapters;
import java.util.List;
import java.util.Locale;

/** Mirrors the existing service queue; catalog selection must never replace audible audio. */
final class CatalogAudioUi {
  interface Actions {
    void command(String command);

    void open(int chapter, int verse);
  }

  private final Activity activity;
  private final View panel;
  private final Actions actions;
  private final List<QuranChapters.Chapter> chapters;
  private QuranAudioService.State state;
  private boolean visible;

  CatalogAudioUi(Activity activity, Actions actions) {
    this.activity = activity;
    this.actions = actions;
    panel = activity.findViewById(R.id.quranCatalogAudio);
    chapters = QuranChapters.all(activity);
    if (activity.getResources().getConfiguration().fontScale >= 1.5f) {
      LinearLayout row = activity.findViewById(R.id.catalogAudioRow);
      row.setOrientation(LinearLayout.VERTICAL);
      View open = activity.findViewById(R.id.catalogAudioOpen);
      open.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
      ((LinearLayout.LayoutParams)
                  activity.findViewById(R.id.catalogAudioControls).getLayoutParams())
              .gravity =
          android.view.Gravity.END;
    }
    activity
        .findViewById(R.id.catalogAudioOpen)
        .setOnClickListener(
            v -> {
              if (valid()) actions.open(state.chapter, state.verse);
            });
    activity
        .findViewById(R.id.catalogAudioPlay)
        .setOnClickListener(
            v -> {
              if (valid())
                actions.command(
                    state.playing || (state.loading && state.requested)
                        ? QuranAudioService.PAUSE
                        : QuranAudioService.RESUME);
            });
    activity
        .findViewById(R.id.catalogAudioPrevious)
        .setOnClickListener(
            v -> {
              if (valid()) actions.command(QuranAudioService.PREVIOUS);
            });
    activity
        .findViewById(R.id.catalogAudioNext)
        .setOnClickListener(
            v -> {
              if (valid()) actions.command(QuranAudioService.NEXT);
            });
  }

  private boolean valid() {
    return state != null && state.chapter >= 1 && state.chapter <= 114;
  }

  void update(QuranAudioService.State next, boolean reading) {
    state = next;
    boolean show = !reading && valid();
    if (show != visible) {
      visible = show;
      panel.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    if (!reading) {
      View scroll = activity.findViewById(R.id.quranScroll);
      scroll.setPadding(scroll.getPaddingLeft(), 0, scroll.getPaddingRight(), dp(show ? 16 : 88));
    }
    if (!show) return;
    String name = chapters.get(state.chapter - 1).name;
    text(
        R.id.catalogAudioTitle,
        activity.getString(R.string.catalog_audio_title, name, state.verse));
    activity
        .findViewById(R.id.catalogAudioOpen)
        .setContentDescription(activity.getString(R.string.catalog_audio_open, name, state.verse));
    boolean playing = state.playing || (state.loading && state.requested);
    String reciter = Recitation.NAMES[Recitation.reciterIndex(state.reciter)];
    text(
        R.id.catalogAudioReciter,
        state.loading || (!state.playing && !state.status.equals("Dijeda"))
            ? reciter + " · " + state.status
            : reciter);
    ImageButton play = activity.findViewById(R.id.catalogAudioPlay);
    play.setImageResource(playing ? R.drawable.audio_pause : R.drawable.audio_play);
    play.setContentDescription(
        activity.getString(playing ? R.string.catalog_audio_pause : R.string.audio_play));
    activity.findViewById(R.id.catalogAudioPrevious).setEnabled(state.verse > state.first);
    activity.findViewById(R.id.catalogAudioNext).setEnabled(state.verse < state.last);
    ProgressBar progress = activity.findViewById(R.id.catalogAudioProgress);
    progress.setMax(Math.max(1, state.duration));
    progress.setProgress(state.position);
    text(
        R.id.catalogAudioTime,
        activity.getString(
            R.string.catalog_audio_time, time(state.position), time(state.duration)));
  }

  private String time(int ms) {
    return String.format(Locale.ROOT, "%d:%02d", ms / 60000, ms / 1000 % 60);
  }

  private int dp(int size) {
    return Math.round(size * activity.getResources().getDisplayMetrics().density);
  }

  private void text(int id, String value) {
    TextView text = activity.findViewById(id);
    if (!value.contentEquals(text.getText())) text.setText(value);
  }

  void theme(boolean night) {
    int fg = night ? 0xFFEEF6F2 : 0xFF15332F;
    int muted = night ? 0xFFB4C8BE : 0xFF586C63;
    panel.setBackgroundColor(night ? 0xFF10211F : activity.getColor(R.color.concept_ivory));
    GradientDrawable card = new GradientDrawable();
    card.setColor(night ? 0xFF1D3530 : 0xFFFFFCF6);
    card.setCornerRadius(dp(12));
    card.setStroke(dp(1), night ? 0xFF345149 : 0xFFE9E4D8);
    activity.findViewById(R.id.catalogAudioCard).setBackground(card);
    ((TextView) activity.findViewById(R.id.catalogAudioTitle)).setTextColor(fg);
    for (int id : new int[] {R.id.catalogAudioReciter, R.id.catalogAudioTime})
      ((TextView) activity.findViewById(id)).setTextColor(muted);
    for (int id : new int[] {R.id.catalogAudioPrevious, R.id.catalogAudioNext})
      ((ImageButton) activity.findViewById(id)).setImageTintList(ColorStateList.valueOf(fg));
    ((ProgressBar) activity.findViewById(R.id.catalogAudioProgress))
        .setProgressTintList(ColorStateList.valueOf(night ? 0xFFACCCB7 : 0xFF15332F));
  }
}
