package com.zikra.app.assistant;

import java.io.IOException;
import okhttp3.*;
import org.json.JSONObject;

/** Only the explicitly reviewed excerpt and reason are transmitted. */
public final class AnswerReportClient {
  public interface Result { void complete(boolean received); }
  private static final OkHttpClient HTTP = AssistantHttp.create();

  public static Call send(AssistantConnection connection, AssistantConnection.ServicePermit permit,
      String reason, String excerpt, Result result) throws Exception {
    if (!connection.permitsService(permit)) throw new IOException();
    String endpoint = connection.endpoint();
    String token = connection.accessCode();
    if (!AssistantConnection.valid(endpoint, token)) throw new IOException();
    HttpUrl url = HttpUrl.parse(endpoint).newBuilder().encodedPath("/v1/answer-reports").build();
    JSONObject payload = new JSONObject().put("reason", reason).put("excerpt", excerpt);
    Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + token)
        .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload.toString())).build();
    Call call = HTTP.newBuilder().addInterceptor(chain -> {
      if (!connection.permitsService(permit)) throw new IOException();
      return chain.proceed(chain.request());
    }).build().newCall(request);
    call.enqueue(new Callback() {
      public void onFailure(Call ignored, IOException error) { result.complete(false); }
      public void onResponse(Call ignored, Response response) {
        boolean received = false;
        try (response) {
          ResponseBody body = response.body();
          if (response.code() == 201 && body != null && !body.source().request(1025))
            received = new JSONObject(body.string()).optBoolean("received", false);
        } catch (Exception invalid) { /* Treat an invalid receipt as unconfirmed. */ }
        result.complete(received);
      }
    });
    return call;
  }
}
