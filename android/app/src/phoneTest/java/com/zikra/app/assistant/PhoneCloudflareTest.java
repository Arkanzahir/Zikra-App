package com.zikra.app.assistant;

import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.os.*;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

/** Explicit physical-device probe. Only generated fixtures and an isolated test identity leave the phone. */
@SuppressWarnings("deprecation")
public final class PhoneCloudflareTest extends InstrumentationTestCase {
  public void testSyntheticChatPhotoPdfAndDocumentViaHostedCloudflare() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    File marker = new File(context.getFilesDir(), "cloudflare-phone-backup-verified");
    assertTrue("Host backup required", marker.isFile());
    assertEquals(64, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
    try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(
        getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))) {
      assertFalse("Physical only", "1".equals(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim()));
    }
    JSONObject config = new JSONObject(new String(Files.readAllBytes(new File(context.getFilesDir(), "cloudflare-phone-live-private.json").toPath()), StandardCharsets.UTF_8));
    assertTrue(config.getBoolean("syntheticOnlyAuthorized"));
    assertEquals("https://zikra-ai.vercel.app/v1/assistant", config.getString("endpoint"));
    android.app.Activity foreground = getInstrumentation().startActivitySync(new Intent(context, com.zikra.app.HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    SharedPreferences gateway = context.getSharedPreferences("assistant_gateway", 0);
    Map<String, ?> before = new HashMap<>(gateway.getAll());
    assertTrue(before.values().stream().allMatch(v -> v instanceof String || v instanceof Boolean));
    AssistantConnection connection = new AssistantConnection(context);
    connection.save(config.getString("endpoint"), config.getString("token"));
    Map<String, ?> installed = new HashMap<>(gateway.getAll());
    JSONArray results = new JSONArray();
    AtomicReference<String> networkCause = new AtomicReference<>();
    okhttp3.OkHttpClient http = new okhttp3.OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .eventListener(new okhttp3.EventListener() {
          @Override public void callFailed(okhttp3.Call call, IOException failure) {
            networkCause.set(failure.getClass().getSimpleName() + ":" + (failure.getCause() == null ? "none" : failure.getCause().getClass().getSimpleName()));
          }
        }).build();
    try (OnlineAssistantClient client = new OnlineAssistantClient(http);
        AssistantAttachments reader = new AssistantAttachments(context)) {
      for (String kind : new String[] {"text", "photo", "pdf", "document"}) {
        JSONObject body = new JSONObject().put("messages", new JSONArray().put(new JSONObject()
            .put("role", "user").put("content", kind.equals("text") ? "Balas persis: ZIKRA-CF-PHONE"
                : "Hitung penjumlahan pada lampiran. Jawab dengan persamaan dan hasilnya.")));
        String expected = "ZIKRA-CF-PHONE";
        if (!kind.equals("text")) {
          byte[] bytes; String name, mime;
          Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(Color.BLACK); paint.setTextSize(38);
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          if (kind.equals("pdf")) {
            PdfDocument pdf = new PdfDocument();
            try {
              for (int page = 1; page <= 3; page++) {
                PdfDocument.Page sheet = pdf.startPage(new PdfDocument.PageInfo.Builder(500, 300, page).create());
                sheet.getCanvas().drawText("28 + 34 = ?", 25, 100, paint); pdf.finishPage(sheet);
              }
              pdf.writeTo(out);
            } finally { pdf.close(); }
            name = "Zikra-CF-Soal.pdf"; mime = "application/pdf"; expected = "62";
          } else if (kind.equals("photo")) {
            Bitmap bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap); canvas.drawColor(Color.WHITE);
            canvas.drawText("17 + 26 = ?", 25, 100, paint);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); bitmap.recycle();
            name = "Zikra-CF-Soal.png"; mime = "image/png"; expected = "43";
          } else {
            out.write("Soal latihan: 12 + 8 = ?".getBytes(StandardCharsets.UTF_8));
            name = "Zikra-CF-Soal.txt"; mime = "text/plain"; expected = "20";
          }
          bytes = out.toByteArray();
          Method decode = AssistantAttachments.class.getDeclaredMethod("decode", String.class, String.class, byte[].class);
          decode.setAccessible(true);
          AssistantAttachments.Draft draft = (AssistantAttachments.Draft) decode.invoke(reader, name, mime, bytes);
          body.put("attachments", new JSONArray().put(draft.payload()));
        }
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> answer = new AtomicReference<>();
        AtomicReference<OnlineAssistantClient.Failure> failure = new AtomicReference<>();
        long start = SystemClock.elapsedRealtime();
        client.send(connection, body, new OnlineAssistantClient.Result() {
          @Override public void complete(String value) { answer.set(value); done.countDown(); }
          @Override public void failed() { done.countDown(); }
          @Override public void failed(OnlineAssistantClient.Failure value) { failure.set(value); done.countDown(); }
        });
        boolean completed = done.await(55, TimeUnit.SECONDS);
        String reply = answer.get();
        boolean passed = completed && failure.get() == null && reply != null
            && (kind.equals("text") ? reply.trim().equals(expected) : reply.contains(expected));
        results.put(new JSONObject().put("kind", kind).put("passed", passed)
            .put("networkCause", networkCause.get() == null ? JSONObject.NULL : networkCause.get())
            .put("elapsedMs", SystemClock.elapsedRealtime() - start)
            .put("reply", reply == null ? JSONObject.NULL : reply)
            .put("failure", failure.get() == null ? JSONObject.NULL : failure.get().name()));
        Files.write(new File(context.getFilesDir(), "cloudflare-phone-result.json").toPath(),
            new JSONObject().put("physicalPhone", true).put("actualHostedProvider", true)
                .put("syntheticOnly", true).put("interceptorUsed", false).put("results", results).toString(2).getBytes(StandardCharsets.UTF_8));
      }
    } finally {
      getInstrumentation().runOnMainSync(foreground::finish);
      assertEquals("Concurrent gateway change; do not overwrite user choice", installed, gateway.getAll());
      SharedPreferences.Editor editor = gateway.edit().clear();
      for (Map.Entry<String, ?> row : before.entrySet()) {
        if (row.getValue() instanceof String) editor.putString(row.getKey(), (String) row.getValue());
        else editor.putBoolean(row.getKey(), (Boolean) row.getValue());
      }
      assertTrue(editor.commit()); assertEquals(before, gateway.getAll());
    }
    for (int i = 0; i < results.length(); i++) assertTrue(results.getJSONObject(i).toString(), results.getJSONObject(i).getBoolean("passed"));
  }
}
