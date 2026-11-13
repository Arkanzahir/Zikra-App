package com.zikra.app.audio;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.test.InstrumentationTestCase;
import com.zikra.app.CalendarActivity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;

/** Representative provider contract, not human verification of recitation or redistribution rights. */
@SuppressWarnings("deprecation")
public final class LiveRecitationContractTest extends InstrumentationTestCase {
  public void testSupportedRecitersUseLiveRecordingsAndDecodedTiming() throws Exception {
    assertTrue(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("generic"));
    android.content.Context context = getInstrumentation().getTargetContext();
    Activity page = getInstrumentation().startActivitySync(new Intent(context, CalendarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
    long focusDeadline = android.os.SystemClock.uptimeMillis() + 5000;
    while (!page.hasWindowFocus() && android.os.SystemClock.uptimeMillis() < focusDeadline) android.os.SystemClock.sleep(50);
    assertTrue(page.hasWindowFocus());
    File folder = new File(context.getCacheDir(), "recitation-contract-" + UUID.randomUUID()); assertTrue(folder.mkdir());
    OkHttpClient http = new OkHttpClient.Builder().callTimeout(40, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
    JSONArray evidence = new JSONArray();
    try (TimedAudioRepository repository = new TimedAudioRepository(folder, http)) {
      JSONArray catalog;
      try (Response response = http.newCall(new Request.Builder()
          .url("https://api.quran.com/api/v4/resources/recitations?language=en").build()).execute()) {
        assertEquals(200, response.code()); assertNotNull(response.body());
        response.body().source().request(1048577);
        assertTrue(response.body().source().buffer().size() <= 1048576);
        catalog = new JSONObject(response.body().string()).getJSONArray("recitations");
      }
      String[] names = {"afas", "husar", "minshaw"};
      for (int index = 0; index < Recitation.IDS.length; index++) {
        String qari = Recitation.IDS[index]; int id = TimedRecitation.id(qari);
        String name = null;
        for (int row = 0; row < catalog.length(); row++)
          if (catalog.getJSONObject(row).getInt("id") == id) name = catalog.getJSONObject(row).getString("reciter_name");
        assertNotNull("Explicit recitation ID must exist", name);
        assertTrue("Provider identity must match configured reciter", name.toLowerCase(Locale.ROOT).contains(names[index]));
        TimedAudioRepository.Clip clip = repository.load(qari, 1, 1);
        assertEquals(1, clip.data.chapter); assertEquals(1, clip.data.verse);
        assertTrue(clip.file.length() > 128);
        MediaMetadataRetriever decoder = new MediaMetadataRetriever();
        long duration;
        try {
          decoder.setDataSource(clip.file.getAbsolutePath());
          assertTrue(decoder.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE).startsWith("audio/"));
          duration = Long.parseLong(decoder.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
          assertTrue(duration > 0);
        } finally { decoder.release(); }
        boolean timing = clip.data.timeline(clip.sha256, duration) != null;
        evidence.put(new JSONObject().put("reciter", qari).put("resourceId", id)
            .put("providerName", name).put("chapter", 1).put("verse", 1)
            .put("bytes", clip.file.length()).put("decodedDurationMs", duration)
            .put("reportedDurationMs", clip.data.reportedDurationMs).put("timingValidated", timing));
        assertSame("Same active clip should not be downloaded twice", clip, repository.load(qari, 1, 1));
      }
    } finally {
      http.dispatcher().cancelAll();
      getInstrumentation().runOnMainSync(page::finish);
      File output = new File(context.getExternalFilesDir(null), "validation"); output.mkdirs();
      try (FileOutputStream stream = new FileOutputStream(new File(output, "live-reciters.json"))) {
        stream.write(evidence.toString(2).getBytes(StandardCharsets.UTF_8));
      }
      File[] remaining = folder.listFiles();
      if (remaining != null) for (File file : remaining) file.delete();
      folder.delete();
    }
    assertEquals(3, evidence.length());
  }
}
