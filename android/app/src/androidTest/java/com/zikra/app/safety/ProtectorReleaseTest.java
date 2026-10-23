package com.zikra.app.safety;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.TextView;
import com.zikra.app.*;
import java.lang.reflect.Field;

/** Actual APK proof: live responses may fail; fixtures are explicitly labelled separately. */
@SuppressWarnings("deprecation")
public final class ProtectorReleaseTest extends InstrumentationTestCase {
  private Activity page;

  private void launch(Class<? extends Activity> type) {
    page =
        getInstrumentation()
            .startActivitySync(
                new Intent(getInstrumentation().getTargetContext(), type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    getInstrumentation().waitForIdleSync();
  }

  private String text(int id) {
    String[] result = {""};
    getInstrumentation()
        .runOnMainSync(() -> result[0] = ((TextView) page.findViewById(id)).getText().toString());
    return result[0];
  }

  private void waitForData(int id) {
    long until = SystemClock.uptimeMillis() + 30000;
    while (text(id).startsWith("Memuat") && SystemClock.uptimeMillis() < until)
      SystemClock.sleep(250);
    SystemClock.sleep(3000);
  }

  public void testActualProtectorAndRainMotion() throws Exception {
    launch(ProtectorActivity.class);
    waitForData(R.id.protectorWeatherDescription);
    waitForData(R.id.protectorQuakeArea);
    UiEvidence.capture(getInstrumentation(), "ProtectorLive");
    Field client = ProtectorActivity.class.getDeclaredField("bmkg");
    client.setAccessible(true);
    ProtectorWeatherView scene = page.findViewById(R.id.protectorWeatherScene);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                ((BmkgClient) client.get(page)).close();
              } catch (Exception e) {
                throw new AssertionError(e);
              }
              ((TextView) page.findViewById(R.id.protectorTitle)).setText("Protector · DATA UJI");
              ((TextView) page.findViewById(R.id.protectorWeatherDescription))
                  .setText("Hujan lebat · DATA UJI");
              ((TextView) page.findViewById(R.id.protectorWeatherTime))
                  .setText("Simulasi visual, bukan cuaca aktual");
              scene.setWeather("Hujan lebat", java.time.ZoneId.of("Asia/Jakarta"));
            });
    SystemClock.sleep(400);
    UiEvidence.capture(getInstrumentation(), "ProtectorRainFixture");
    Field elapsed = ProtectorWeatherView.class.getDeclaredField("elapsed");
    elapsed.setAccessible(true);
    float[] at = {0, 0};
    getInstrumentation()
        .runOnMainSync(
            () -> {
              scene.setActive(false);
              try {
                at[0] = elapsed.getFloat(scene);
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
    SystemClock.sleep(300);
    getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                at[1] = elapsed.getFloat(scene);
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
    assertEquals("No animation clock advances after pause", at[0], at[1]);
  }

  public void testActualEarthquakeAndShakemapTabs() {
    launch(EarthquakeActivity.class);
    waitForData(R.id.quakeArea);
    UiEvidence.capture(getInstrumentation(), "EarthquakeLive");
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.quakeTabShaking).performClick());
    long until = SystemClock.uptimeMillis() + 25000;
    while (text(R.id.quakeShakingStatus).startsWith("Memuat") && SystemClock.uptimeMillis() < until)
      SystemClock.sleep(250);
    assertEquals(View.VISIBLE, page.findViewById(R.id.quakeShakingPanel).getVisibility());
    UiEvidence.capture(getInstrumentation(), "ShakemapLive");
    getInstrumentation()
        .runOnMainSync(() -> page.findViewById(R.id.quakeTabEpicenter).performClick());
    assertEquals(View.GONE, page.findViewById(R.id.quakeShakingPanel).getVisibility());
  }

  @Override
  protected void tearDown() throws Exception {
    if (page != null) getInstrumentation().runOnMainSync(page::finish);
    getInstrumentation().waitForIdleSync();
    super.tearDown();
  }
}
