package com.zikra.app.assistant;

import static org.junit.Assert.*;

import org.json.*;
import org.junit.Test;

public class AssistantGatewayTest {
  private static final String ACCESS = "private-test-code-32-characters-long";

  @Test
  public void onlyAcceptsPrivateHttpsGatewayRoute() {
    assertTrue(AssistantConnection.valid("https://example.test/v1/assistant", ACCESS));
    assertFalse(AssistantConnection.valid("http://example.test/v1/assistant", ACCESS));
    assertFalse(AssistantConnection.valid("https://api.featherless.ai/v1/assistant", ACCESS));
    assertFalse(AssistantConnection.valid("https://example.test/v1/assistant?key=secret", ACCESS));
    assertFalse(AssistantConnection.valid("https://user:secret@example.test/v1/assistant", ACCESS));
    assertFalse(AssistantConnection.valid("https://example.test/v1/assistant", "short"));
  }

  @Test
  public void onlySelectedConversationFieldsLeaveDevice() throws Exception {
    JSONArray stored =
        new JSONArray()
            .put(
                new JSONObject()
                    .put("role", "user")
                    .put("text", "Belajar 25 menit")
                    .put("mode", "offline")
                    .put("unrelated_profile", "private"));
    JSONObject sent = OnlineAssistantClient.context(stored).getJSONObject(0);
    assertEquals(2, sent.length());
    assertEquals("user", sent.getString("role"));
    assertEquals("Belajar 25 menit", sent.getString("content"));
  }

  @Test
  public void clipsHistoryToProviderBudget() throws Exception {
    JSONArray stored = new JSONArray();
    for (int i = 0; i < 24; i++)
      stored.put(
          new JSONObject()
              .put("role", i % 2 == 0 ? "assistant" : "user")
              .put("text", "x".repeat(2000)));
    assertEquals(8, OnlineAssistantClient.context(stored).length());
  }
}
