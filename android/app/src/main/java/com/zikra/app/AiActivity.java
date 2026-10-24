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
import com.zikra.app.assistant.AssistantSelection;
import com.zikra.app.assistant.TaskProposal;
import com.zikra.app.assistant.OnlineAssistantClient;
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
  private static final int MAX_STORED_TEXT = 8000;
  private com.zikra.app.assistant.AssistantConnection connection;
  private com.zikra.app.assistant.OnlineAssistantClient onlineClient;
  private boolean waiting;
  private AlertDialog modeDialog;
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
  private AlertDialog selectionDialog, taskDialog, sourceDialog;
  private JSONObject pendingVerse;

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
    getWindow().setStatusBarColor(getColor(R.color.studio_lavender));
    ((FeatureHeader) findViewById(R.id.utilitiesHeader))
        .setSurfaceColor(getColor(R.color.studio_lavender));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    preferences = getSharedPreferences("zikra_preferences", MODE_PRIVATE);
    connection = new com.zikra.app.assistant.AssistantConnection(this);
    onlineClient = new com.zikra.app.assistant.OnlineAssistantClient();
    findViewById(R.id.aiMode).setOnClickListener(v -> chooseMode());
    findViewById(R.id.aiRetry).setOnClickListener(v -> requestOnline());
    updateMode();
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
    findViewById(R.id.aiSend).setOnClickListener(v -> { if (waiting) cancelOnline(); else sendMessage(); });
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
    int chapter = getIntent().getIntExtra("tadabbur_chapter", 0);
    int verse = getIntent().getIntExtra("tadabbur_verse", 0);
    if (chapter >= 1 && chapter <= 114 && verse >= 1 && verse <= 286) {
      try { pendingVerse = new JSONObject().put("chapter", chapter).put("number", verse); }
      catch (JSONException ignored) { }
      if (state == null) input.setText("Jelaskan makna surah " + chapter + " ayat " + verse + " berdasarkan sumber tafsir.");
    }
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
        addToHistory(role, text, item.optString("mode", "offline"));
        JSONObject restored = messages.optJSONObject(messages.length() - 1);
        String requestId = item.optString("request_id");
        if (requestId.matches("[0-9a-f-]{36}")) restored.put("request_id", requestId);
        try {
          JSONObject proposal = item.optJSONObject("proposal");
          if (proposal != null) { TaskProposal.fromJson(proposal); restored.put("proposal", proposal); }
        } catch (IllegalArgumentException | JSONException invalid) { }
        JSONObject source = item.optJSONObject("source");
        if (source != null && OnlineAssistantClient.verifiedSource(source, source) != null) restored.put("source", source);
      }
    } catch (JSONException ignored) {
      messages = new JSONArray();
    }
  }

  private void addToHistory(String role, String text) {
    addToHistory(role, text, connection != null && connection.online() ? "online" : "offline");
  }

  private void addToHistory(String role, String text, String mode) {
    try {
      JSONObject item = new JSONObject();
      item.put("role", role);
      item.put("text", text);
      item.put("mode", mode);
      if ("user".equals(role)) item.put("request_id", java.util.UUID.randomUUID().toString());
      messages.put(item);
      while (messages.length() > MAX_MESSAGES) messages.remove(0);
      while (messages.length() > 1 && messages.toString().length() > 48000) messages.remove(0);
    } catch (JSONException ignored) {
      /* All fields are validated strings. */
    }
  }

  private void renderConversation() {
    thread.removeAllViews();
    if (messages.length() == 0) {
      bubble("assistant", getString(R.string.concept_ai_welcome));
      return;
    }
    for (int i = 0; i < messages.length(); i++) {
      JSONObject item = messages.optJSONObject(i);
      if (item != null) {
        bubble(item.optString("role"), item.optString("text"), item.optString("mode", "offline"));
        if ("assistant".equals(item.optString("role"))) appendAssistantExtras(item);
      }
    }
  }

  private void bubble(String role, String message) {
    bubble(role, message, "offline");
  }

  private void bubble(String role, String message, String mode) {
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
    background.setColor(getColor(user ? R.color.concept_lavender : R.color.white));
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
      label.setText(
          "online".equals(mode)
              ? R.string.concept_ai_cloud_label
              : R.string.concept_ai_local_label);
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
    body.setTextColor(getColor(R.color.pine));
    body.setTextIsSelectable(true);
    card.addView(body);
    row.addView(card);
    thread.addView(row);
  }

  private void sendMessage() {
    if (waiting) return;
    findViewById(R.id.aiRequestStatus).setVisibility(View.GONE);
    findViewById(R.id.aiRetry).setVisibility(View.GONE);
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
    if (pendingVerse != null && !connection.online()) {
      toast("Tadabbur bersumber memerlukan mode online. Aktifkan melalui label mode.");
      return;
    }
    if (connection.online()) {
      addToHistory("user", message);
      preferences.edit().putString(HISTORY_KEY, messages.toString()).apply();
      input.setText("");
      renderConversation();
      requestOnline();
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

  private void updateMode() {
    boolean online = connection.online();
    ((TextView) findViewById(R.id.aiMode))
        .setText(online ? R.string.concept_ai_mode_online : R.string.concept_ai_mode_offline);
    ((TextView) findViewById(R.id.aiPrivacyNote))
        .setText(online ? R.string.concept_ai_network_note : R.string.concept_ai_local_note);
    ((TextView) findViewById(R.id.featureSubtitle))
        .setText(online ? R.string.concept_ai_mode_online : R.string.concept_ai_mode_offline);
  }

  private void chooseMode() {
    modeDialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.concept_ai_choose_mode)
            .setItems(
                new String[] {
                  getString(R.string.concept_ai_offline), getString(R.string.concept_ai_online)
                },
                (d, index) -> {
                  onlineClient.cancel();
                  setWaiting(false);
                  findViewById(R.id.aiRetry).setVisibility(View.GONE);
                  if (index == 0) {
                    connection.offline();
                    updateMode();
                  } else configureGateway();
                })
            .show();
  }

  private void configureGateway() {
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(dp(24), dp(8), dp(24), dp(8));
    TextView note = new TextView(this);
    note.setText(R.string.concept_ai_backend_note);
    note.setTextSize(14);
    form.addView(note);
    EditText endpoint = new EditText(this);
    endpoint.setSingleLine(true);
    endpoint.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    endpoint.setHint(R.string.concept_ai_endpoint);
    endpoint.setText(connection.endpoint());
    form.addView(endpoint);
    EditText access = new EditText(this);
    access.setSingleLine(true);
    access.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    access.setHint(R.string.concept_ai_access);
    form.addView(access);
    modeDialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.concept_ai_backend)
            .setView(form)
            .setPositiveButton(R.string.concept_ai_connect, null)
            .setNegativeButton(R.string.concept_ai_cancel, null)
            .create();
    modeDialog.setOnShowListener(
        d ->
            modeDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        connection.save(
                            endpoint.getText().toString().trim(),
                            access.getText().toString().trim());
                        updateMode();
                        modeDialog.dismiss();
                      } catch (Exception invalid) {
                        endpoint.setError(getString(R.string.concept_ai_backend_invalid));
                      }
                    }));
    modeDialog.show();
  }

  private void setWaiting(boolean value) {
    waiting = value;
    android.widget.ImageButton send = findViewById(R.id.aiSend);
    send.setImageResource(value ? android.R.drawable.ic_menu_close_clear_cancel : R.drawable.ai_send);
    send.setContentDescription(value ? getString(R.string.assistant_cancel_send) : "Kirim pesan");
    TextView status = findViewById(R.id.aiRequestStatus);
    status.setText(value ? R.string.concept_ai_pending : R.string.concept_ai_failed);
    status.setVisibility(value ? View.VISIBLE : View.GONE);
  }

  private void requestOnline() {
    if (waiting
        || !connection.online()
        || messages.length() == 0
        || !"user".equals(messages.optJSONObject(messages.length() - 1).optString("role"))) return;
    showSelection();
  }

  private void showSelection() {
    if (selectionDialog != null && selectionDialog.isShowing()) return;
    ScrollView container = new ScrollView(this);
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), dp(16));
    container.addView(form);
    TextView note = new TextView(this); note.setText(R.string.assistant_send_note); form.addView(note);
    int[] labels = { R.string.assistant_select_name, R.string.assistant_select_tasks,
      R.string.assistant_select_prayer, R.string.assistant_select_history, R.string.assistant_select_proposals };
    android.widget.CheckBox[] choices = new android.widget.CheckBox[labels.length];
    for (int i = 0; i < labels.length; i++) {
      choices[i] = new android.widget.CheckBox(this); choices[i].setText(labels[i]);
      choices[i].setMinHeight(dp(48)); form.addView(choices[i]);
    }
    if (pendingVerse != null) {
      TextView sourceNote = new TextView(this); sourceNote.setText(R.string.assistant_tafsir_note); form.addView(sourceNote);
    }
    TextView preview = new TextView(this); preview.setTextIsSelectable(true); preview.setPadding(0, dp(12), 0, 0); form.addView(preview);
    final JSONObject[] snapshot = new JSONObject[1];
    Runnable update = () -> {
      try {
        snapshot[0] = AssistantSelection.build(this, messages, choices[0].isChecked(), choices[1].isChecked(),
            choices[2].isChecked(), choices[3].isChecked(), choices[4].isChecked(), pendingVerse);
        preview.setText(AssistantSelection.preview(snapshot[0]));
      } catch (Exception unavailable) {
        snapshot[0] = null; preview.setText("Data pilihan belum dapat dibaca. Batalkan dan coba lagi.");
      }
    };
    for (android.widget.CheckBox choice : choices) choice.setOnCheckedChangeListener((button, checked) -> update.run());
    update.run();
    selectionDialog = new AlertDialog.Builder(this).setTitle(R.string.assistant_review_send).setView(container)
        .setPositiveButton(R.string.assistant_send_selected, null)
        .setNegativeButton("Batal", (dialog, which) -> showCancelled()).create();
    selectionDialog.setOnCancelListener(dialog -> showCancelled());
    selectionDialog.setOnShowListener(dialog -> selectionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
      if (snapshot[0] == null) return;
      JSONObject selected = snapshot[0]; selectionDialog.dismiss(); sendOnline(selected);
    }));
    selectionDialog.show();
  }

  private void sendOnline(JSONObject request) {
    if (waiting || !connection.online()) return;
    setWaiting(true);
    findViewById(R.id.aiRetry).setVisibility(View.GONE);
    try {
      onlineClient.send(
          connection,
          request,
          new com.zikra.app.assistant.OnlineAssistantClient.Result() {
            @Override
            public void complete(String reply) {
              complete(reply, null, null);
            }

            @Override
            public void complete(String reply, JSONObject proposal, JSONObject source) {
              if (isFinishing() || isDestroyed()) return;
              setWaiting(false);
              addToHistory("assistant", reply, "online");
              JSONObject row = messages.optJSONObject(messages.length() - 1);
              try {
                if (proposal != null) row.put("proposal", proposal);
                if (source != null) row.put("source", source);
              } catch (JSONException ignored) { }
              pendingVerse = null;
              getIntent().removeExtra("tadabbur_chapter"); getIntent().removeExtra("tadabbur_verse");
              preferences.edit().putString(HISTORY_KEY, messages.toString()).apply();
              renderConversation();
              scroll.post(() -> scroll.smoothScrollTo(0, thread.getBottom()));
            }

            @Override
            public void failed() {
              showOnlineFailure();
            }

            @Override public void failed(OnlineAssistantClient.Failure failure) { showOnlineFailure(failure); }
          });
    } catch (Exception unavailable) {
      showOnlineFailure();
    }
  }

  private void showOnlineFailure() {
    showOnlineFailure(OnlineAssistantClient.Failure.SERVER);
  }

  private void showOnlineFailure(OnlineAssistantClient.Failure failure) {
    if (isFinishing() || isDestroyed()) return;
    setWaiting(false);
    int message;
    switch (failure) {
      case UNAUTHORIZED: message = R.string.assistant_auth_failed; break;
      case QUOTA: message = R.string.assistant_quota_failed; break;
      case TIMEOUT: message = R.string.assistant_timeout_failed; break;
      case NETWORK: message = R.string.assistant_network_failed; break;
      case SOURCE: message = R.string.assistant_source_failed; break;
      case INVALID_RESPONSE: message = R.string.assistant_response_failed; break;
      default: message = R.string.assistant_server_failed;
    }
    ((TextView) findViewById(R.id.aiRequestStatus)).setText(message);
    findViewById(R.id.aiRequestStatus).setVisibility(View.VISIBLE);
    findViewById(R.id.aiRetry).setVisibility(View.VISIBLE);
  }

  private void cancelOnline() {
    onlineClient.cancel(); setWaiting(false); showCancelled();
  }

  private void showCancelled() {
    TextView status = findViewById(R.id.aiRequestStatus);
    status.setText(R.string.assistant_cancelled); status.setVisibility(View.VISIBLE);
    findViewById(R.id.aiRetry).setVisibility(View.VISIBLE);
  }

  private void appendAssistantExtras(JSONObject item) {
    LinearLayout card = (LinearLayout) ((LinearLayout) thread.getChildAt(thread.getChildCount() - 1)).getChildAt(0);
    JSONObject proposal = item.optJSONObject("proposal");
    if (proposal != null) {
      boolean applied;
      try (ZikraDbHelper db = new ZikraDbHelper(this)) { applied = db.assistantProposalApplied(proposal.optString("id")); }
      TextView review = new TextView(this);
      review.setText(applied ? R.string.assistant_task_saved : R.string.assistant_review_task);
      review.setTextColor(getColor(R.color.pine)); review.setMinHeight(dp(48)); review.setGravity(Gravity.CENTER_VERTICAL);
      if (!applied) { review.setOnClickListener(view -> reviewTask(proposal)); review.setFocusable(true); }
      card.addView(review);
    }
    JSONObject source = item.optJSONObject("source");
    if (source != null) {
      TextView note = new TextView(this); note.setText(R.string.assistant_tafsir_note);
      note.setTextSize(12); note.setPadding(0, dp(8), 0, dp(4)); card.addView(note);
      TextView openSource = new TextView(this); openSource.setText(R.string.assistant_tafsir_original);
      openSource.setTextColor(getColor(R.color.pine)); openSource.setMinHeight(dp(48)); openSource.setGravity(Gravity.CENTER_VERTICAL);
      openSource.setFocusable(true); openSource.setOnClickListener(view -> showSource(source)); card.addView(openSource);
    }
  }

  private void reviewTask(JSONObject value) {
    final TaskProposal proposal;
    try { proposal = TaskProposal.fromJson(value); }
    catch (Exception invalid) { toast("Usulan tugas tidak valid."); return; }
    LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(24), dp(8), dp(24), dp(12));
    TextView note = new TextView(this); note.setText(getString(R.string.assistant_task_unsaved)
        + ("edit_task".equals(proposal.kind) ? "\n\nMengubah: " + proposal.expectedTitle : "")); form.addView(note);
    EditText title = new EditText(this); title.setHint(R.string.assistant_task_title); title.setText(proposal.title);
    title.setFilters(new InputFilter[] {new InputFilter.LengthFilter(200)}); form.addView(title);
    long[] due = {proposal.dueAt};
    TextView dueText = new TextView(this); dueText.setPadding(0, dp(12), 0, dp(8)); form.addView(dueText);
    Runnable renderDue = () -> dueText.setText(due[0] == 0 ? getString(R.string.assistant_task_no_due)
        : java.time.Instant.ofEpochMilli(due[0]).atZone(com.zikra.app.location.LocationStore.current(this).zone)
          .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy HH:mm z", new java.util.Locale("id", "ID"))));
    android.widget.Button editDue = new android.widget.Button(this); editDue.setText(R.string.assistant_task_due); form.addView(editDue);
    editDue.setOnClickListener(view -> {
      java.time.ZoneId zone = com.zikra.app.location.LocationStore.current(this).zone;
      java.time.ZonedDateTime selected = (due[0] == 0 ? java.time.Instant.now() : java.time.Instant.ofEpochMilli(due[0])).atZone(zone);
      new android.app.DatePickerDialog(this, (picker, year, month, day) ->
          new android.app.TimePickerDialog(this, (clock, hour, minute) -> {
            due[0] = java.time.LocalDateTime.of(year, month + 1, day, hour, minute).atZone(zone).toInstant().toEpochMilli(); renderDue.run();
          }, selected.getHour(), selected.getMinute(), true).show(), selected.getYear(), selected.getMonthValue() - 1, selected.getDayOfMonth()).show();
    });
    android.widget.Button clearDue = new android.widget.Button(this); clearDue.setText(R.string.assistant_task_no_due);
    clearDue.setOnClickListener(view -> { due[0] = 0; renderDue.run(); }); form.addView(clearDue); renderDue.run();
    ScrollView scrollForm = new ScrollView(this); scrollForm.addView(form);
    taskDialog = new AlertDialog.Builder(this).setTitle(R.string.assistant_review_task).setView(scrollForm)
        .setPositiveButton(R.string.assistant_save_task, null).setNegativeButton("Batal", null).create();
    taskDialog.setOnShowListener(dialog -> taskDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
      try (ZikraDbHelper db = new ZikraDbHelper(this)) {
        db.applyReviewedProposal(proposal.reviewed(title.getText().toString(), due[0]));
        com.zikra.app.study.StudyReminders.sync(this);
        taskDialog.dismiss(); renderConversation(); toast(getString(R.string.assistant_task_saved));
      } catch (IllegalArgumentException | IllegalStateException invalid) { title.setError(invalid.getMessage()); }
    }));
    taskDialog.show();
  }

  private void showSource(JSONObject source) {
    if (OnlineAssistantClient.verifiedSource(source, source) == null) return;
    TextView text = new TextView(this); text.setTextIsSelectable(true); text.setPadding(dp(20), dp(8), dp(20), dp(16));
    text.setText(getString(R.string.assistant_tafsir_note) + "\n\n" + source.optString("text")
        + (source.optBoolean("truncated") ? "\n\nPetikan dipotong; buka sumber untuk teks lengkap." : "") + "\n\n" + source.optString("url"));
    ScrollView container = new ScrollView(this); container.addView(text);
    sourceDialog = new AlertDialog.Builder(this).setTitle("Tafsir · " + source.optInt("chapter") + ":" + source.optInt("number"))
        .setView(container).setNegativeButton("Tutup", null).setPositiveButton(R.string.assistant_tafsir_open, (dialog, which) -> {
          try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(source.optString("url")))); }
          catch (ActivityNotFoundException unavailable) { toast("Browser belum tersedia."); }
        }).show();
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
    voiceDialog = new AlertDialog.Builder(this).setTitle(R.string.assistant_voice_start)
        .setMessage(R.string.assistant_voice_consent).setPositiveButton(R.string.assistant_voice_start, (dialog, which) -> startVoice())
        .setNegativeButton("Batal", null).show();
  }

  private void startVoice() {
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
    ArrayList<?> results = null;
    try { results = data == null ? null : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS); }
    catch (RuntimeException invalid) { }
    if (resultCode != RESULT_OK
        || results == null
        || results.isEmpty()
        || !(results.get(0) instanceof String)
        || ((String) results.get(0)).trim().isEmpty()) {
      ((TextView) findViewById(R.id.aiVoiceStatus))
          .setText("Belum ada pesan suara. Coba lagi atau ketik pesan.");
      return;
    }
    String raw = (String) results.get(0);
    String result = com.zikra.app.assistant.VoiceDraft.normalize(raw);
    if (raw.trim().length() > OfflineAssistant.MAX_INPUT_LENGTH) {
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
                  onlineClient.cancel();
                  setWaiting(false);
                  findViewById(R.id.aiRetry).setVisibility(View.GONE);
                  messages = new JSONArray();
                  pendingVerse = null;
                  getIntent().removeExtra("tadabbur_chapter"); getIntent().removeExtra("tadabbur_verse");
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
  protected void onResume() {
    super.onResume();
    if (orb != null) orb.setActive(true);
  }

  @Override
  protected void onPause() {
    if (onlineClient != null) onlineClient.cancel();
    if (waiting) { setWaiting(false); showCancelled(); }
    if (orb != null) orb.setActive(false);
    super.onPause();
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
    if (modeDialog != null) modeDialog.dismiss();
    if (selectionDialog != null) selectionDialog.dismiss();
    if (taskDialog != null) taskDialog.dismiss();
    if (sourceDialog != null) sourceDialog.dismiss();
    if (onlineClient != null) onlineClient.close();
    super.onDestroy();
  }
}
