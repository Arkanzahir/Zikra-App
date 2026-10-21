package com.zikra.app.assistant;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;

/** Bounded, opt-in private gateway request; no provider SDK, secrets, or calls in draw frames. */
public final class OnlineAssistantClient implements AutoCloseable {
  public interface Result {
    void complete(String reply);

    void failed();
  }

  private final OkHttpClient http =
      new OkHttpClient.Builder()
          .callTimeout(45, TimeUnit.SECONDS)
          .followRedirects(false)
          .followSslRedirects(false)
          .retryOnConnectionFailure(false)
          .build();
  private final Handler main = new Handler(Looper.getMainLooper());
  private Call active;
  private int generation;

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
    cancel();
    int token = generation;
    String code = connection.accessCode(), endpoint = connection.endpoint();
    if (!AssistantConnection.valid(endpoint, code)) throw new IOException("Invalid gateway");
    JSONObject body = new JSONObject().put("messages", context(stored));
    active =
        http.newCall(
            new Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer " + code)
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
                  if (token == generation) result.failed();
                });
          }

          @Override
          public void onResponse(Call call, Response response) {
            String answer = null;
            try (response) {
              if (response.isSuccessful() && response.body() != null) {
                response.body().source().request(65537);
                if (response.body().source().buffer().size() <= 65536) {
                  String value = new JSONObject(response.body().string()).getString("reply").trim();
                  if (!value.isEmpty() && value.length() <= 8000) answer = value;
                }
              }
            } catch (IOException | JSONException ignored) {
            }
            String finalAnswer = answer;
            main.post(
                () -> {
                  if (token == generation) {
                    if (finalAnswer != null) result.complete(finalAnswer);
                    else result.failed();
                  }
                });
          }
        });
  }

  public void cancel() {
    generation++;
    if (active != null) active.cancel();
    active = null;
  }

  @Override
  public void close() {
    cancel();
    main.removeCallbacksAndMessages(null);
    http.connectionPool().evictAll();
    http.dispatcher().executorService().shutdown();
  }
}
