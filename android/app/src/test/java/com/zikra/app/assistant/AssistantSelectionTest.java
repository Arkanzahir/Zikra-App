package com.zikra.app.assistant;

import static org.junit.Assert.*;
import org.json.*;
import org.junit.Test;

public class AssistantSelectionTest {
  private static final String ID = "8da51639-4899-42f0-9dc4-66723b358667";
  @Test public void defaultsToCurrentMessageAndNeverUploadsOfflineHistoryOrMetadata() throws Exception {
    JSONArray rows = new JSONArray().put(new JSONObject().put("role", "user").put("text", "private offline").put("mode", "offline"))
        .put(new JSONObject().put("role", "assistant").put("text", "online answer").put("mode", "online"))
        .put(new JSONObject().put("role", "user").put("text", "current").put("mode", "online").put("proposal", "private metadata"));
    assertEquals(1, AssistantSelection.conversation(rows, false).length());
    JSONArray chosen = AssistantSelection.conversation(rows, true);
    assertEquals(2, chosen.length());
    assertFalse(chosen.toString().contains("private"));
    assertEquals(2, chosen.getJSONObject(1).length());
  }
  @Test public void proposalsRequireConsentRequestIdAndExactSelectedTaskSnapshot() throws Exception {
    JSONObject proposal = new JSONObject().put("id", ID).put("kind", "edit_task").put("title", "Revised")
        .put("dueAt", 0).put("taskId", "12").put("expectedTitle", "Original").put("expectedDueAt", 0);
    JSONObject request = new JSONObject();
    assertNull(OnlineAssistantClient.authorizedProposal(proposal, request));
    request.put("requestId", ID).put("features", new JSONObject().put("taskProposals", true))
        .put("context", new JSONObject().put("tasks", new JSONArray().put(new JSONObject().put("id", "12")
            .put("title", "Original").put("dueAt", 0).put("completed", false))));
    assertNotNull(OnlineAssistantClient.authorizedProposal(proposal, request));
    proposal.put("taskId", "13");
    assertNull(OnlineAssistantClient.authorizedProposal(proposal, request));
    proposal.put("taskId", "12").put("completed", true);
    assertNull(OnlineAssistantClient.authorizedProposal(proposal, request));
  }
  @Test public void errorKindsAndSourceProvenanceDoNotTurnIntoOfflineAnswers() throws Exception {
    assertEquals(OnlineAssistantClient.Failure.UNAUTHORIZED, OnlineAssistantClient.failure(401, ""));
    assertEquals(OnlineAssistantClient.Failure.QUOTA, OnlineAssistantClient.failure(429, ""));
    assertEquals(OnlineAssistantClient.Failure.TIMEOUT, OnlineAssistantClient.failure(504, ""));
    assertEquals(OnlineAssistantClient.Failure.SOURCE, OnlineAssistantClient.failure(503, "source_unavailable"));
    JSONObject verse = new JSONObject().put("chapter", 1).put("number", 2);
    JSONObject source = new JSONObject().put("chapter", 1).put("number", 2).put("text", "Synthetic source")
        .put("url", "https://equran.id/tafsir/1");
    assertNotNull(OnlineAssistantClient.verifiedSource(source, verse));
    source.put("url", "https://untrusted.test/tafsir/1");
    assertNull(OnlineAssistantClient.verifiedSource(source, verse));
  }
  @Test public void voiceIsBoundedDraftAndDoesNotSplitEmoji() {
    assertEquals("", VoiceDraft.normalize(null));
    assertEquals("hello", VoiceDraft.normalize("  hello  "));
    assertEquals(499, VoiceDraft.normalize("x".repeat(499) + "😀end").length());
    assertEquals(500, VoiceDraft.normalize("x".repeat(900)).length());
  }
  @Test public void selectedHistoryAlsoFitsUtf8RequestByteLimit() throws Exception {
    JSONArray messages = new JSONArray();
    for (int i = 0; i < 24; i++) messages.put(new JSONObject().put("role", "user").put("content", "ع".repeat(2000)));
    JSONObject request = AssistantSelection.bound(new JSONObject().put("messages", messages)
        .put("context", new JSONObject().put("name", "ع".repeat(80))));
    assertTrue(request.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 48 * 1024);
    assertTrue(messages.length() < 24);
    assertEquals("ع".repeat(2000), messages.getJSONObject(messages.length() - 1).getString("content"));
  }
}
