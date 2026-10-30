package com.zikra.app.assistant;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.*;
import org.json.*;

/** Bounded, opt-in private gateway request; no provider SDK, secrets, or calls in draw frames. */
public final class OnlineAssistantClient implements AutoCloseable {
  public interface Result {
    void complete(String reply);

    default void complete(String reply, JSONObject proposal, JSONObject source) { complete(reply); }

    void failed();

    default void failed(Failure failure) { failed(); }
  }

  public enum Failure { UNAUTHORIZED, QUOTA, BUSY, TIMEOUT, NETWORK, SERVER, INVALID_RESPONSE, SOURCE, ATTACHMENT }

  public static Failure failure(int status, String error) {
    if ("source_unavailable".equals(error)) return Failure.SOURCE;
    if(status==413||"vision_unavailable".equals(error)||"invalid_attachment".equals(error)||"unsupported_attachment".equals(error))return Failure.ATTACHMENT;
    if (status == 401 || status == 403) return Failure.UNAUTHORIZED;
    if ((status == 429 || status == 503) && "provider_busy".equals(error)) return Failure.BUSY;
    if (status == 429) return Failure.QUOTA;
    if (status == 408 || status == 504) return Failure.TIMEOUT;
    return status >= 500 ? Failure.SERVER : Failure.INVALID_RESPONSE;
  }

  private final OkHttpClient http;

  public OnlineAssistantClient() {
    this(new OkHttpClient.Builder()
          .callTimeout(45, TimeUnit.SECONDS)
          .followRedirects(false)
          .followSslRedirects(false)
          .retryOnConnectionFailure(false)
          .build());
  }

  OnlineAssistantClient(OkHttpClient http) { this.http = http; }
  private final Handler main = new Handler(Looper.getMainLooper());
  private Call active;
  private int generation;
  private final AtomicBoolean closed = new AtomicBoolean();

  public static JSONArray context(JSONArray stored) throws JSONException {
    ArrayList<JSONObject> chosen = new ArrayList<>();
    int size = 0;
    for (int i = stored.length() - 1; i >= 0 && chosen.size() < 24; i--) {
      JSONObject row = stored.optJSONObject(i);
      if (row == null) continue;
      String role = row.optString("role"), text = row.optString("text");
      if (!(role.equals("user") || role.equals("assistant")) || text.isEmpty()) continue;
      if (text.length() > 2000 || size + text.length() > 16000) break;
      chosen.add(new JSONObject().put("role", role).put("content", text));
      size += text.length();
    }
    Collections.reverse(chosen);
    return new JSONArray(chosen);
  }

  public void send(AssistantConnection connection, JSONArray stored, Result result)
      throws Exception {
    send(connection, new JSONObject().put("messages", context(stored)), result);
  }

  public void send(AssistantConnection connection, JSONObject body, Result result) throws Exception {
    if (closed.get()) throw new IOException("Assistant client is closed");
    cancel();
    int token = generation;
    String code = connection.accessCode(), endpoint = connection.endpoint();
    if (!AssistantConnection.valid(endpoint, code)) throw new IOException("Invalid gateway");
    active =
        http.newCall(
            new Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer " + code)
                .header("X-Zikra-Attachments",body.has("attachments")?"1":"0")
                .post(
                    RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"), body.toString()))
                .build());
    active.enqueue(
        new Callback() {
          @Override
          public void onFailure(Call call, IOException error) {
            main.post(
                () -> {
                  if (token == generation) result.failed(error instanceof java.io.InterruptedIOException
                      ? Failure.TIMEOUT : Failure.NETWORK);
                });
          }

          @Override
          public void onResponse(Call call, Response response) {
            String answer = null;
            JSONObject proposal = null, source = null;
            Failure failure = Failure.INVALID_RESPONSE;
            try (response) {
              failure = failure(response.code(), "");
              if (response.body() != null) {
                response.body().source().request(65537);
                if (response.body().source().buffer().size() <= 65536) {
                  JSONObject data = new JSONObject(response.body().string());
                  failure = failure(response.code(), data.optString("error"));
                  if (response.isSuccessful() && "featherless".equals(data.optString("provider"))) {
                    Object raw = data.opt("reply");
                    String value = raw instanceof String ? ((String) raw).trim() : "";
                    if (!value.isEmpty() && value.length() <= 8000) {
                      answer = value;
                      proposal = authorizedProposal(data.optJSONObject("proposal"), body);
                      source = verifiedSource(data.optJSONObject("source"), body.optJSONObject("verse"));
                      if (body.has("verse") && source == null) { answer = null; failure = Failure.SOURCE; }
                    }
                  }
                }
              }
            } catch (IOException error) {
              failure = error instanceof java.io.InterruptedIOException ? Failure.TIMEOUT : Failure.NETWORK;
            } catch (JSONException invalid) {
              // Preserve the HTTP error when a gateway error page is not JSON.
            }
            String finalAnswer = answer;
            JSONObject finalProposal = proposal, finalSource = source;
            Failure finalFailure = failure;
            main.post(
                () -> {
                  if (token == generation) {
                    if (finalAnswer != null) result.complete(finalAnswer, finalProposal, finalSource);
                    else result.failed(finalFailure);
                  }
                });
          }
        });
  }

  public static JSONObject authorizedProposal(JSONObject proposal, JSONObject request) {
    JSONObject features = request.optJSONObject("features"), context = request.optJSONObject("context");
    if (proposal == null || features == null || !features.optBoolean("taskProposals", false)) return null;
    try {
      TaskProposal parsed = TaskProposal.fromJson(proposal);
      if (!parsed.id.equals(request.optString("requestId"))) return null;
      if ("add_task".equals(parsed.kind)) return proposal;
      JSONArray tasks = context == null ? null : context.optJSONArray("tasks");
      if (tasks == null) return null;
      for (int i = 0; i < tasks.length(); i++) {
        JSONObject task = tasks.getJSONObject(i);
        if (task.optString("id").equals(Long.toString(parsed.taskId)) && !task.getBoolean("completed")
            && task.getString("title").equals(parsed.expectedTitle) && task.getLong("dueAt") == parsed.expectedDueAt)
          return proposal;
      }
    } catch (JSONException | IllegalArgumentException invalid) { }
    return null;
  }

  public static JSONObject verifiedSource(JSONObject source, JSONObject verse) {
    if (source == null || verse == null) return null;
    int chapter = verse.optInt("chapter"), number = verse.optInt("number");
    if (chapter < 1 || chapter > 114 || number < 1 || number > 286
        || source.optInt("chapter") != chapter || source.optInt("number") != number
        || !("https://equran.id/tafsir/" + chapter).equals(source.optString("url"))
        || source.optString("text").trim().isEmpty() || source.optString("text").length() > 6000) return null;
    return source;
  }

  public void cancel() {
    generation++;
    if (active != null) active.cancel();
    active = null;
  }

  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) return;
    cancel();
    main.removeCallbacksAndMessages(null);
    // Closing an idle TLS socket can write close_notify; never do that on the UI thread.
    Thread cleanup = new Thread(() -> {
      try { http.connectionPool().evictAll(); }
      finally { http.dispatcher().executorService().shutdown(); }
    }, "Zikra-AI-cleanup");
    cleanup.setDaemon(true);
    cleanup.start();
  }
}
