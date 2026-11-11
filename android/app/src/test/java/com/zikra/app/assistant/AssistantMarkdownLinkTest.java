package com.zikra.app.assistant;

import static org.junit.Assert.*;

import com.zikra.app.ui.AssistantMarkdown;
import org.junit.Test;

public class AssistantMarkdownLinkTest {
  @Test
  public void linksCannotLaunchScriptsAppsFilesOrCredentialUrls() {
    for (String value :
        new String[] {
          "javascript:alert(1)",
          "data:text/html,test",
          "file:///sdcard/private.txt",
          "intent://example.test",
          "http://example.test",
          "https://user:secret@example.test",
          "https://example.test\n.evil.test",
          "https://example.test/" + "a".repeat(2048)
        }) {
      assertNull(AssistantMarkdown.safeLink(value));
    }
    assertNull(AssistantMarkdown.safeLink(null));
  }

  @Test
  public void validHttpsLinkPreservesPathQueryAndFragment() {
    assertEquals(
        "https://example.test/path?q=study#part",
        AssistantMarkdown.safeLink("https://example.test/path?q=study#part"));
  }
}
