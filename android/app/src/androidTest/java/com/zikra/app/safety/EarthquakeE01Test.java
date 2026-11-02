package com.zikra.app.safety;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.*;
import com.zikra.app.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import org.json.*;

@SuppressWarnings("deprecation")
public final class EarthquakeE01Test extends InstrumentationTestCase {
  private EarthquakeActivity page;
  private Context context;
  private void ui(Runnable action) { getInstrumentation().runOnMainSync(action); }
  private void await(java.util.concurrent.Callable<Boolean> condition) throws Exception {
    long until=SystemClock.elapsedRealtime()+30000;
    while(SystemClock.elapsedRealtime()<until){if(condition.call())return;SystemClock.sleep(100);}
    fail("E01 condition timed out");
  }
  private String text(int id) {
    AtomicReference<String> value=new AtomicReference<>();
    ui(()->value.set(((TextView)page.findViewById(id)).getText().toString()));return value.get();
  }
  private void invoke(String name,Class<?> type,Object value) throws Exception {
    Method m=EarthquakeActivity.class.getDeclaredMethod(name,type);m.setAccessible(true);
    ui(()->{try{m.invoke(page,value);}catch(Exception e){throw new AssertionError(e);}});
  }
  private String fixture(String area,String magnitude,String coordinate,int secondsAgo) throws Exception {
    return new JSONObject().put("Infogempa",new JSONObject().put("gempa",new JSONObject()
        .put("DateTime",Instant.now().minusSeconds(secondsAgo).toString()).put("Wilayah",area)
        .put("Magnitude",magnitude).put("Kedalaman","10 km").put("Coordinates",coordinate)
        .put("Shakemap","20261030130345.mmi.jpg"))).toString();
  }
  private void launch(EarthquakeReport report) throws Exception {
    context=getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(),"e01-emulator-authorized").isFile());
    Intent intent=new Intent(context,EarthquakeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if(report!=null)intent.putExtra("quake_report",report.snapshot());
    page=(EarthquakeActivity)getInstrumentation().startActivitySync(intent);
    Field f=EarthquakeActivity.class.getDeclaredField("bmkg");f.setAccessible(true);
    ui(()->{try{((BmkgClient)f.get(page)).close();}catch(Exception e){throw new AssertionError(e);}});
  }

  public void testCacheFreshOfflineExpiredAndLateCancellation() throws Exception {
    EarthquakeReport seed=EarthquakeReport.network(fixture("Sorong · DATA UJI","3.7","-.72,131.38",600),Instant.now());
    launch(seed);
    android.content.SharedPreferences cache=context.getSharedPreferences("bmkg_earthquake_v1",0);
    cache.edit().clear().commit();AtomicInteger requests=new AtomicInteger();
    OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{requests.incrementAndGet();return new Response.Builder()
        .request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Fixture")
        .body(ResponseBody.create(MediaType.parse("application/json"),fixtureUnchecked(seed))).build();}).build();
    EarthquakeClient client=new EarthquakeClient(page,http);
    AtomicReference<EarthquakeReport> got=new AtomicReference<>();CountDownLatch first=new CountDownLatch(1);
    client.load(new BmkgClient.Result<EarthquakeReport>(){public void success(EarthquakeReport q){got.set(q);first.countDown();}public void failure(String e){fail(e);}});
    assertTrue(first.await(10,TimeUnit.SECONDS));assertFalse(got.get().stored);assertEquals(1,requests.get());
    CountDownLatch fresh=new CountDownLatch(1);
    client.load(new BmkgClient.Result<EarthquakeReport>(){public void success(EarthquakeReport q){got.set(q);fresh.countDown();}public void failure(String e){fail(e);}});
    assertTrue(fresh.await(10,TimeUnit.SECONDS));assertTrue(got.get().stored);assertEquals(1,requests.get());client.close();
    Instant oldAt=Instant.now().minusSeconds(120);
    EarthquakeReport old=EarthquakeReport.network(fixture("Sorong · DATA UJI","3.7","-.72,131.38",600),oldAt);
    cache.edit().putString("snapshot",old.snapshot()).commit();
    OkHttpClient denied=new OkHttpClient.Builder().addInterceptor(chain->{throw new IOException("Synthetic offline");}).build();
    EarthquakeClient offline=new EarthquakeClient(page,denied);CountDownLatch both=new CountDownLatch(2);
    offline.load(new BmkgClient.Result<EarthquakeReport>(){public void success(EarthquakeReport q){got.set(q);both.countDown();}public void failure(String e){both.countDown();}});
    assertTrue(both.await(10,TimeUnit.SECONDS));assertEquals(oldAt.toEpochMilli(),got.get().fetchedAt.toEpochMilli());
    offline.close();AtomicInteger successes=new AtomicInteger();
    cache.edit().putString("snapshot",new JSONObject(old.snapshot()).put("fetchedAt",Instant.now().minusSeconds(86401).toEpochMilli()).toString()).commit();
    EarthquakeClient expired=new EarthquakeClient(page,denied);CountDownLatch failed=new CountDownLatch(1);
    expired.load(new BmkgClient.Result<EarthquakeReport>(){public void success(EarthquakeReport q){successes.incrementAndGet();}public void failure(String e){failed.countDown();}});
    assertTrue(failed.await(10,TimeUnit.SECONDS));assertEquals(0,successes.get());expired.close();
    cache.edit().clear().commit();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
    OkHttpClient held=new OkHttpClient.Builder().addInterceptor(chain->{entered.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){throw new IOException(e);}throw new IOException("Cancelled fixture");}).build();
    EarthquakeClient cancelled=new EarthquakeClient(page,held);AtomicInteger callbacks=new AtomicInteger();
    cancelled.load(new BmkgClient.Result<EarthquakeReport>(){public void success(EarthquakeReport q){callbacks.incrementAndGet();}public void failure(String e){callbacks.incrementAndGet();}});
    assertTrue(entered.await(10,TimeUnit.SECONDS));cancelled.reset();release.countDown();SystemClock.sleep(500);
    assertEquals(0,callbacks.get());cancelled.close();
  }

  private String fixtureUnchecked(EarthquakeReport report) throws IOException {
    try{return new JSONObject(report.snapshot()).getString("report");}catch(Exception e){throw new IOException(e);}
  }

  public void testSelectedReportDoesNotBecomeLatestAndFailurePreservesItsPin() throws Exception {
    EarthquakeReport selected=EarthquakeReport.network(fixture("Sorong · DATA UJI","3.7","-.72,131.38",600),Instant.now());
    context=getInstrumentation().getTargetContext();
    EarthquakeReport other=EarthquakeReport.network(fixture("Laut Jawa · DATA UJI","5.2","-6.1,112.3",300),Instant.now());
    context.getSharedPreferences("bmkg_earthquake_v1",0).edit().putString("snapshot",other.snapshot()).commit();
    launch(selected);assertEquals("3.7",text(R.id.quakeMagnitude));assertTrue(text(R.id.quakeArea).contains("Sorong"));
    assertTrue(text(R.id.quakeCoordinates).contains("131,380"));assertTrue(text(R.id.quakeStatus).contains("tersimpan"));
    String time=text(R.id.quakeTime),coordinate=text(R.id.quakeCoordinates);
    invoke("showFailure",String.class,"Synthetic offline");assertEquals(time,text(R.id.quakeTime));assertEquals(coordinate,text(R.id.quakeCoordinates));
    assertTrue(text(R.id.quakeStatus).contains("Pembaruan gagal"));
    ui(()->((TextView)page.findViewById(R.id.quakeTitle)).setText("E01 · DATA UJI"));
    UiEvidence.capture(getInstrumentation(),"E01-selected-report");
    invoke("show",EarthquakeReport.class,other);assertEquals("5.2",text(R.id.quakeMagnitude));assertTrue(text(R.id.quakeArea).contains("Laut Jawa"));
    assertTrue(text(R.id.quakeCoordinates).contains("112,300"));
  }

  public void testShakemapFailedReplacementAndUnsafeOriginNeverLeaveOldImage() throws Exception {
    launch(EarthquakeReport.network(fixture("DATA UJI","3.7","-.72,131.38",600),Instant.now()));
    Bitmap bitmap=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.YELLOW);
    ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);bitmap.recycle();byte[] bytes=out.toByteArray();
    AtomicInteger calls=new AtomicInteger();
    OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{calls.incrementAndGet();boolean bad=chain.request().url().encodedPath().contains("fail");
      return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(bad?503:200).message("Fixture")
          .body(ResponseBody.create(MediaType.parse("image/jpeg"),bad?new byte[0]:bytes)).build();}).build();
    ImageView image=page.findViewById(R.id.quakeShakingImage);TextView status=page.findViewById(R.id.quakeShakingStatus);
    AtomicReference<ShakemapLoader> ref=new AtomicReference<>();ui(()->ref.set(new ShakemapLoader(page,image,status,http)));ShakemapLoader loader=ref.get();
    ui(()->loader.load("https://static.bmkg.go.id/good.jpg"));await(()->image.getDrawable()!=null);
    ui(()->loader.load("https://static.bmkg.go.id/fail.jpg"));await(()->status.getText().toString().contains("gagal")||status.getText().toString().contains("belum"));
    assertNull(image.getDrawable());ui(()->loader.load("https://static.bmkg.go.id/good.jpg"));await(()->image.getDrawable()!=null);assertEquals(3,calls.get());
    ui(()->loader.load("https://evil.test/good.jpg"));assertNull(image.getDrawable());assertEquals(View.VISIBLE,status.getVisibility());assertEquals(3,calls.get());ui(loader::close);
  }

  public void testTileTimeoutKeepsSourcePinAndReachableDetails() throws Exception {
    launch(EarthquakeReport.network(fixture("Sorong · DATA UJI","3.7","-.72,131.38",600),Instant.now()));
    EarthquakeMapView map=page.findViewById(R.id.quakeMap);
    Field tiles=EarthquakeMapView.class.getDeclaredField("tiles");tiles.setAccessible(true);
    Field actual=EarthquakeMapView.class.getDeclaredField("map");actual.setAccessible(true);
    Field notice=EarthquakeMapView.class.getDeclaredField("notice");notice.setAccessible(true);
    Method offline=EarthquakeMapView.class.getDeclaredMethod("offline");offline.setAccessible(true);
    ui(()->{try{tiles.setBoolean(map,false);offline.invoke(map);map.pause();}catch(Exception e){throw new AssertionError(e);}});
    assertTrue(map.hasEpicenter());assertEquals(0f,((View)actual.get(map)).getAlpha(),.001f);
    assertEquals(context.getString(R.string.protector_map_fallback),((TextView)notice.get(map)).getText().toString());
    ui(()->{((TextView)page.findViewById(R.id.quakeTitle)).setText("E01 · DATA UJI");((ScrollView)page.findViewById(R.id.quakeSheet)).fullScroll(View.FOCUS_DOWN);});
    getInstrumentation().waitForIdleSync();android.graphics.Rect rectangle=new android.graphics.Rect();
    assertTrue(page.findViewById(R.id.quakeOpenMap).getGlobalVisibleRect(rectangle));
    UiEvidence.capture(getInstrumentation(),"E01-tile-timeout");
  }

  public void testOfficialBmkgReportAndRedirectedShakemap() throws Exception {
    launch(null);
    context.getSharedPreferences("bmkg_earthquake_v1",0).edit().clear().commit();
    EarthquakeClient live=new EarthquakeClient(page);AtomicReference<EarthquakeReport> got=new AtomicReference<>();
    AtomicReference<String> error=new AtomicReference<>();CountDownLatch done=new CountDownLatch(1);
    live.load(new BmkgClient.Result<EarthquakeReport>(){public void success(EarthquakeReport q){got.set(q);done.countDown();}public void failure(String e){error.set(e);done.countDown();}});
    assertTrue(done.await(30,TimeUnit.SECONDS));assertNotNull("BMKG network: "+error.get(),got.get());
    assertFalse(got.get().stored);invoke("show",EarthquakeReport.class,got.get());
    assertEquals(got.get().quake.magnitude,text(R.id.quakeMagnitude));assertEquals(got.get().quake.area,text(R.id.quakeArea));
    new File(context.getExternalFilesDir(null),"validation").mkdirs();
    java.nio.file.Files.write(new File(context.getExternalFilesDir(null),"validation/E01-live.json").toPath(),got.get().snapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    UiEvidence.capture(getInstrumentation(),"E01-live-report");
    ui(()->page.findViewById(R.id.quakeTabShaking).performClick());
    ImageView image=page.findViewById(R.id.quakeShakingImage);await(()->image.getDrawable()!=null);
    UiEvidence.capture(getInstrumentation(),"E01-live-shakemap");live.close();
  }

  @Override protected void tearDown() throws Exception {
    if(page!=null)ui(page::finish);
    super.tearDown();
  }
}
