package com.zikra.app.assistant;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.test.InstrumentationTestCase;
import android.view.accessibility.AccessibilityNodeInfo;
import com.zikra.app.AiActivity;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import org.json.JSONObject;

/** Real DocumentsUI grants on an explicitly backed-up phone; synthetic files only. */
@SuppressWarnings("deprecation")
public final class PhoneAttachmentPickerTest extends InstrumentationTestCase {
  private AiActivity page;
  private final AtomicInteger requests = new AtomicInteger();
  private static final String PREFIX = "Zikra-C07-20261110-";

  private void ui(Runnable action) {
    getInstrumentation().runOnMainSync(action);
    getInstrumentation().waitForIdleSync();
  }

  private Object field(String name) throws Exception {
    Field field = AiActivity.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(page);
  }

  private void waitFor(Callable<Boolean> condition) throws Exception {
    for (int i = 0; i < 200; i++) {
      if (condition.call()) return;
      SystemClock.sleep(100);
    }
    fail("Phone picker condition timed out");
  }

  private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String text) {
    if (node == null) return null;
    if (node.getText() != null && text.contentEquals(node.getText())) return node;
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo found = find(node.getChild(i), text);
      if (found != null) return found;
    }
    return null;
  }

  private void pick(int action, String file) throws Exception {
    Method method = AiActivity.class.getDeclaredMethod("pickAttachment", int.class);
    method.setAccessible(true);
    ui(
        () -> {
          try {
            method.invoke(page, action);
          } catch (Exception failure) {
            throw new RuntimeException(failure);
          }
        });
    waitFor(
        () -> {
          AccessibilityNodeInfo root =
              getInstrumentation().getUiAutomation().getRootInActiveWindow();
          return root != null && !"com.zikra.app".contentEquals(root.getPackageName());
        });
    if (file == null) {
      for (int i = 0; i < 8; i++) {
        AccessibilityNodeInfo root = getInstrumentation().getUiAutomation().getRootInActiveWindow();
        if (root != null && "com.zikra.app".contentEquals(root.getPackageName())) break;
        assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(1));
        SystemClock.sleep(350);
      }
    } else {
      waitFor(
          () -> find(getInstrumentation().getUiAutomation().getRootInActiveWindow(), file) != null);
      AccessibilityNodeInfo node =
          find(getInstrumentation().getUiAutomation().getRootInActiveWindow(), file);
      while (node != null && !node.isClickable()) node = node.getParent();
      assertNotNull("Fixture row must be clickable", node);
      assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));
      waitFor(
          () -> {
            AssistantAttachments.Draft draft = (AssistantAttachments.Draft) field("attachment");
            return draft != null
                && file.equals(draft.name)
                && !(Boolean) field("importingAttachment");
          });
    }
    waitFor(
        () -> {
          AccessibilityNodeInfo root =
              getInstrumentation().getUiAutomation().getRootInActiveWindow();
          return root != null && "com.zikra.app".contentEquals(root.getPackageName());
        });
  }

  public void testRealSelectionGrantsCancellationAndUnselectedDenial() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    assertFalse(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk"));
    assertTrue(new File(context.getFilesDir(), "phone-closeout-20261110-backup-verified").isFile());
    SharedPreferences prefs = context.getSharedPreferences("zikra_preferences", 0);
    String key = "assistant_active_chat";
    String original = prefs.getString(key, null);
    ConversationStore store = new ConversationStore(context);
    String chat = store.create().id;
    store.rename(chat, "UJI C07 picker 10 November");
    prefs.edit().putString(key, chat).commit();
    Instrumentation.ActivityMonitor monitor =
        new Instrumentation.ActivityMonitor() {
          @Override
          public Instrumentation.ActivityResult onStartActivity(Intent intent) {
            if (Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction())) {
              intent.putExtra(
                  DocumentsContract.EXTRA_INITIAL_URI,
                  DocumentsContract.buildDocumentUri(
                      "com.android.externalstorage.documents",
                      "primary:Download/Zikra-C07-20261110"));
            }
            return null;
          }
        };
    getInstrumentation().addMonitor(monitor);
    try {
      page =
          (AiActivity)
              getInstrumentation()
                  .startActivitySync(
                      new Intent(context, AiActivity.class)
                          .addFlags(
                              Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
      waitFor(() -> !(Boolean) field("importingAttachment"));
      ((OnlineAssistantClient) field("onlineClient")).close();
      Field client = AiActivity.class.getDeclaredField("onlineClient");
      client.setAccessible(true);
      client.set(
          page,
          new OnlineAssistantClient(
              new OkHttpClient.Builder()
                  .addInterceptor(
                      chain -> {
                        requests.incrementAndGet();
                        throw new IOException("Network blocked by physical picker test");
                      })
                  .build()));
      Uri unselected =
          DocumentsContract.buildDocumentUri(
              "com.android.externalstorage.documents",
              "primary:Download/Zikra-C07-20261110/" + PREFIX + "Unselected.txt");
      boolean denied = false;
      try (InputStream ignored = context.getContentResolver().openInputStream(unselected)) {
        // A provider may report a missing grant as SecurityException or FileNotFoundException.
      } catch (SecurityException | FileNotFoundException expected) {
        denied = true;
      }
      assertTrue("An unselected document must have no read grant", denied);
      pick(1, PREFIX + "Foto.png");
      assertEquals("photo", ((AssistantAttachments.Draft) field("attachment")).kind);
      String first = ((AssistantAttachments.Draft) field("attachment")).id;
      pick(1, null);
      assertEquals(first, ((AssistantAttachments.Draft) field("attachment")).id);
      pick(2, PREFIX + "Scan.pdf");
      assertEquals("pdf", ((AssistantAttachments.Draft) field("attachment")).kind);
      assertEquals(3, ((AssistantAttachments.Draft) field("attachment")).pages);
      pick(2, PREFIX + "Word.docx");
      assertTrue(((AssistantAttachments.Draft) field("attachment")).text.contains("ZK-4826"));
      assertEquals(0, requests.get());
      Files.write(
          new File(context.getFilesDir(), "phone-picker-result.json").toPath(),
          new JSONObject()
              .put("physicalPhone", true)
              .put("realDocumentsUiSelection", true)
              .put("initialDirectoryProvidedByHarness", true)
              .put("photo", true)
              .put("pdfPages", 3)
              .put("docxUtf16Table", true)
              .put("cancelPreservesDraft", true)
              .put("unselectedGrantDenied", denied)
              .put("assistantHttpRequests", requests.get())
              .put("liveProviderInference", false)
              .put("passed", true)
              .toString(2)
              .getBytes(StandardCharsets.UTF_8));
    } finally {
      getInstrumentation().removeMonitor(monitor);
      if (page != null) ui(page::finish);
      if (chat.equals(prefs.getString(key, null))) {
        SharedPreferences.Editor edit = prefs.edit();
        if (original == null) edit.remove(key);
        else edit.putString(key, original);
        edit.commit();
      }
      ConversationStore.Chat remaining = store.get(chat);
      if (remaining != null
          && remaining.messages.length() == 0
          && remaining.draft.isEmpty()
          && "UJI C07 picker 10 November".equals(remaining.title)) {
        try (AssistantAttachments attachments = new AssistantAttachments(context)) {
          attachments.deleteConversation(chat);
        }
        store.delete(chat);
      }
    }
  }
}
