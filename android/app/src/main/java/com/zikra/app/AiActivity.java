package com.zikra.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zikra.app.assistant.AssistantOrbView;
import com.zikra.app.assistant.OfflineAssistant;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.ui.FeatureHeader;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Native local companion with a bounded on-device conversation and Android voice input. */
public final class AiActivity extends BaseActivity {
  private static final int VOICE_REQUEST = 4001;
  private static final String HISTORY_KEY = "assistant_history_v1";
  private static final int MAX_MESSAGES = 24;
  private static final int MAX_STORED_TEXT = 1600;
  private SharedPreferences preferences;
  private EditText input;
  private LinearLayout thread;
  private ScrollView scroll;
  private AssistantOrbView orb;
  private JSONArray messages = new JSONArray();
  private AlertDialog clearDialog;
  private AlertDialog voiceDialog;
  private FrameLayout pageRoot;
  private android.view.ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
  private int largestPageHeight;
  private boolean keyboardVisible;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_ai);
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .bind(
            R.id.toolbarBack,
            R.id.detailTitle,
            R.id.featureSubtitle,
            R.id.aiClear,
            "Pendamping Zikra",
            "Panduan rutinitas · offline",
            R.drawable.ai_reset,
            "Hapus percakapan lokal");
    setupScreen("Pendamping Zikra");
    getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    getWindow().setStatusBarColor(Color.WHITE);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    adaptEntryActions();
    input = findViewById(R.id.aiInput);
    input.setFilters(
        new InputFilter[] {new InputFilter.LengthFilter(OfflineAssistant.MAX_INPUT_LENGTH)});
    thread = findViewById(R.id.aiChatThread);
    scroll = findViewById(R.id.aiScroll);
    orb = findViewById(R.id.aiOrb);
    restoreHistory();
    renderConversation();
    if (state != null) input.setText(state.getString("draft", ""));
    findViewById(R.id.aiSend).setOnClickListener(v -> sendMessage());
    findViewById(R.id.aiVoice).setOnClickListener(v -> beginVoice());
    configureVoiceAvailability();
    findViewById(R.id.aiChatTile).setOnClickListener(v -> focusComposer());
    findViewById(R.id.aiTopicStudy)
        .setOnClickListener(v -> composeTopic("Bantu susun rencana belajar untuk hari ini"));
    findViewById(R.id.aiTopicPrayer)
        .setOnClickListener(v -> composeTopic("Bantu menjaga rutinitas ibadah hari ini"));
    findViewById(R.id.aiTopicHifz)
        .setOnClickListener(v -> composeTopic("Bantu susun target hafalan Al-Quran"));
    findViewById(R.id.aiOpenStudy).setOnClickListener(v -> open(StudyActivity.class));
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .action()
        .setOnClickListener(v -> confirmClear());
    pageRoot = findViewById(R.id.aiRoot);
    NavDock.attach(this, pageRoot, "ai");
    if (android.os.Build.VERSION.SDK_INT >= 30) {
      pageRoot.setOnApplyWindowInsetsListener(
          (view, insets) -> {
            refreshKeyboardLayout(insets);
            return insets;
          });
      pageRoot.requestApplyInsets();
    }
    keyboardLayoutListener = () -> refreshKeyboardLayout(pageRoot.getRootWindowInsets());
    pageRoot.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
  }

  private void configureVoiceAvailability() {
    boolean available =
        new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(getPackageManager())
            != null;
    ((TextView) findViewById(R.id.aiVoiceTitle))
        .setText(available ? "Pakai suara" : "Ide rencana belajar");
    ((TextView) findViewById(R.id.aiVoiceCaption))
        .setText(
            available
                ? "Dikte Android menjadi teks."
                : "Suara Android belum tersedia. Mulai dengan panduan belajar.");
    ((TextView) findViewById(R.id.aiVoiceAction))
        .setText(available ? "Gunakan suara  →" : "Coba rencana  →");
    View tile = findViewById(R.id.aiVoiceTile);
    tile.setContentDescription(
        available
            ? "Ubah suara menjadi teks dengan layanan Android"
            : "Layanan suara belum tersedia. Buka panduan rencana belajar");
    tile.setOnClickListener(
        v -> {
          if (available) beginVoice();
          else composeTopic("Bantu susun rencana belajar untuk hari ini");
        });
    findViewById(R.id.aiVoice).setVisibility(available ? View.VISIBLE : View.GONE);
  }

  private void adaptEntryActions() {
    android.content.res.Configuration configuration = getResources().getConfiguration();
    if (configuration.fontScale < 1.3f && configuration.screenWidthDp >= 360) return;
    LinearLayout actions = findViewById(R.id.aiEntryActions);
    actions.setOrientation(LinearLayout.VERTICAL);
    for (int index = 0; index < actions.getChildCount(); index++) {
      View action = actions.getChildAt(index);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
      if (index > 0) params.topMargin = dp(12);
      action.setLayoutParams(params);
    }
  }

  private void refreshKeyboardLayout(android.view.WindowInsets insets) {
    largestPageHeight = Math.max(largestPageHeight, pageRoot.getHeight());
    boolean resized = largestPageHeight - pageRoot.getHeight() > dp(150);
    boolean imeVisible =
        android.os.Build.VERSION.SDK_INT >= 30
            && insets != null
            && insets.isVisible(android.view.WindowInsets.Type.ime());
    // Legacy fitting windows can consume IME insets; the resized viewport is the fallback.
    updateKeyboardLayout(imeVisible || resized);
  }

  private void updateKeyboardLayout(boolean visible) {
    if (keyboardVisible == visible) return;
    keyboardVisible = visible;
    findViewById(R.id.aiPage)
        .setPadding(
            0, 0, 0, visible ? 0 : getResources().getDimensionPixelSize(R.dimen.dock_reserved));
    NavDock.setVisible(pageRoot, !visible);
    if (visible) scroll.post(() -> scroll.smoothScrollTo(0, thread.getBottom()));
  }

  private void restoreHistory() {
    String stored = preferences.getString(HISTORY_KEY, "[]");
    // Refuse oversized or malformed state rather than allocating an unbounded thread.
    if (stored.length() > 50000) return;
    try {
      JSONArray previous = new JSONArray(stored);
      for (int i = Math.max(0, previous.length() - MAX_MESSAGES); i < previous.length(); i++) {
        JSONObject item = previous.optJSONObject(i);
        if (item == null) continue;
        String role = item.optString("role", "");
        String text = item.optString("text", "");
        if ((!"user".equals(role) && !"assistant".equals(role))
            || text.isEmpty()
            || text.length() > MAX_STORED_TEXT) continue;
        addToHistory(role, text);
      }
    } catch (JSONException ignored) {
      messages = new JSONArray();
    }
  }

  private void addToHistory(String role, String text) {
    try {
      JSONObject item = new JSONObject();
      item.put("role", role);
      item.put("text", text);
      messages.put(item);
      while (messages.length() > MAX_MESSAGES) messages.remove(0);
    } catch (JSONException ignored) {
      /* All fields are validated strings. */
    }
  }

  private void renderConversation() {
    thread.removeAllViews();
    if (messages.length() == 0) {
      bubble(
          "assistant",
          "Assalamualaikum.\n"
              + "Mari susun satu langkah baik hari ini. Mau mulai dari belajar, ibadah, atau"
              + " hafalan?");
      return;
    }
    for (int i = 0; i < messages.length(); i++) {
      JSONObject item = messages.optJSONObject(i);
      if (item != null) bubble(item.optString("role"), item.optString("text"));
    }
  }

  private void bubble(String role, String message) {
    boolean user = "user".equals(role);
    LinearLayout row = new LinearLayout(this);
    row.setGravity(user ? Gravity.END : Gravity.START);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
    rowParams.bottomMargin = dp(12);
    row.setLayoutParams(rowParams);
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(15), dp(13), dp(15), dp(14));
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-2, -2);
    if (user) cardParams.leftMargin = dp(32);
    else cardParams.rightMargin = dp(18);
    card.setLayoutParams(cardParams);
    GradientDrawable background = new GradientDrawable();
    background.setColor(Color.parseColor(user ? "#15332F" : "#FFFFFF"));
    background.setCornerRadii(
        new float[] {
          dp(19),
          dp(19),
          dp(19),
          dp(19),
          user ? dp(5) : dp(19),
          user ? dp(5) : dp(19),
          user ? dp(19) : dp(5),
          user ? dp(19) : dp(5)
        });
    card.setBackground(background);
    if (!user) {
      TextView label = new TextView(this);
      label.setText("✦  ZIKRA · OFFLINE");
      label.setTextSize(12);
      label.setTextColor(Color.parseColor("#52665F"));
      label.setTypeface(label.getTypeface(), Typeface.BOLD);
      label.setPadding(0, 0, 0, dp(7));
      card.addView(label);
    }
    TextView body = new TextView(this);
    body.setText(message);
    body.setTextSize(16);
    body.setLineSpacing(dp(4), 1f);
    body.setTextColor(Color.parseColor(user ? "#FFFFFF" : "#15332F"));
    body.setTextIsSelectable(true);
    card.addView(body);
    row.addView(card);
    thread.addView(row);
  }

  private void sendMessage() {
    String message = input.getText().toString().trim();
    if (message.isEmpty()) {
      input.setError("Tulis pesanmu dahulu");
      input.requestFocus();
      return;
    }
    if (message.length() > OfflineAssistant.MAX_INPUT_LENGTH) {
      input.setError("Maksimal 500 karakter");
      return;
    }
    int completed;
    int total;
    try (ZikraDbHelper database = new ZikraDbHelper(this)) {
      ZikraDbHelper.Progress progress = database.getProgress(ZikraDbHelper.CATEGORY_STUDY);
      completed = progress.completed;
      total = progress.total;
    }
    String reply =
        OfflineAssistant.reply(
            message, completed, total, preferences.getString("last_surah", "Al-Fatihah"));
    addToHistory("user", message);
    addToHistory("assistant", reply);
    preferences.edit().putString(HISTORY_KEY, messages.toString()).apply();
    input.setText("");
    renderConversation();
    scroll.post(
        () -> {
          if (!isFinishing()) scroll.smoothScrollTo(0, thread.getBottom());
        });
  }

  private void composeTopic(String text) {
    input.setText(text);
    sendMessage();
  }

  private void focusComposer() {
    input.requestFocus();
    scroll.post(
        () -> {
          if (isFinishing()) return;
          InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
          if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
  }

  private void beginVoice() {
    findViewById(R.id.aiVoicePanel).setVisibility(View.VISIBLE);
    orb.setListening(true);
    ((TextView) findViewById(R.id.aiVoiceStatus))
        .setText("Layanan Android mengubah suaramu menjadi teks. Periksa sebelum mengirim.");
    Intent recognize = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    recognize.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    recognize.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
    recognize.putExtra(RecognizerIntent.EXTRA_PROMPT, "Apa yang ingin kamu kerjakan hari ini?");
    recognize.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    try {
      startActivityForResult(recognize, VOICE_REQUEST);
    } catch (ActivityNotFoundException | SecurityException unavailable) {
      orb.setListening(false);
      ((TextView) findViewById(R.id.aiVoiceStatus))
          .setText("Input suara belum tersedia di perangkat ini");
      voiceDialog =
          new AlertDialog.Builder(this)
              .setTitle("Gunakan pesan tertulis")
              .setMessage(
                  "Layanan pengenal suara Android belum tersedia. Kamu tetap bisa menuliskan target"
                      + " di kolom pesan.")
              .setPositiveButton("Tulis pesan", (dialog, which) -> focusComposer())
              .setNegativeButton("Tutup", null)
              .show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != VOICE_REQUEST || isFinishing()) return;
    orb.setListening(false);
    ArrayList<String> results =
        data == null ? null : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    if (resultCode != RESULT_OK
        || results == null
        || results.isEmpty()
        || results.get(0) == null
        || results.get(0).trim().isEmpty()) {
      ((TextView) findViewById(R.id.aiVoiceStatus))
          .setText("Belum ada pesan suara. Coba lagi atau ketik pesan.");
      return;
    }
    String result = results.get(0).trim();
    if (result.length() > OfflineAssistant.MAX_INPUT_LENGTH) {
      result = result.substring(0, OfflineAssistant.MAX_INPUT_LENGTH);
      toast("Hasil suara dibatasi 500 karakter. Periksa sebelum dikirim.");
    }
    input.setText(result);
    input.setSelection(input.length());
    ((TextView) findViewById(R.id.aiVoiceStatus)).setText("Pesan siap. Periksa lalu tekan kirim.");
    scroll.post(() -> scroll.smoothScrollTo(0, thread.getBottom()));
  }

  private void confirmClear() {
    clearDialog =
        new AlertDialog.Builder(this)
            .setTitle("Mulai percakapan baru?")
            .setMessage("Percakapan lokal di perangkat ini akan dihapus.")
            .setPositiveButton(
                "Hapus",
                (dialog, which) -> {
                  messages = new JSONArray();
                  preferences.edit().remove(HISTORY_KEY).apply();
                  renderConversation();
                  toast("Percakapan dihapus");
                })
            .setNegativeButton("Batal", null)
            .show();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("draft", input.getText().toString());
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    if (pageRoot != null) {
      pageRoot.setOnApplyWindowInsetsListener(null);
      if (keyboardLayoutListener != null && pageRoot.getViewTreeObserver().isAlive()) {
        pageRoot.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
      }
    }
    if (clearDialog != null) clearDialog.dismiss();
    if (voiceDialog != null) voiceDialog.dismiss();
    super.onDestroy();
  }
}
