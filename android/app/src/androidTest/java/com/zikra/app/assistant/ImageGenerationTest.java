package com.zikra.app.assistant;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;
import okhttp3.*;

/** Synthetic fixtures require emulator backup/isolation. Live method requires a separate private token. */
@SuppressWarnings("deprecation")
public final class ImageGenerationTest extends InstrumentationTestCase {
  private Context context;private AiActivity activity;
  private String liveFile="image-c09-live.json";
  @Override protected void setUp()throws Exception{
    super.setUp();context=getInstrumentation().getTargetContext();
    try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))){assertEquals("1",new String(in.readAllBytes(),StandardCharsets.UTF_8).trim());}
    assertTrue("Host must back up and isolate app data",new File(context.getFilesDir(),"image-c09-test-authorized").isFile());
    context.deleteDatabase("assistant_chats.db");context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true).putString("assistant_mode_v2","auto").remove("assistant_active_chat").remove("assistant_history_v1").putString("profile_display_name","PRIVATE-NEVER-IN-IMAGE-REQUEST").commit();
    new AssistantConnection(context).save("https://example.test/v1/assistant","synthetic-gateway-code-32-characters-images");
  }
  @Override protected void tearDown()throws Exception{close();context.deleteDatabase("assistant_chats.db");super.tearDown();}
  private void ui(Runnable task){getInstrumentation().runOnMainSync(task);getInstrumentation().waitForIdleSync();}
  private Object get(String key)throws Exception{Field f=AiActivity.class.getDeclaredField(key);f.setAccessible(true);return f.get(activity);}
  private void set(String key,Object value)throws Exception{Field f=AiActivity.class.getDeclaredField(key);f.setAccessible(true);f.set(activity,value);}
  private void call(String key,Class<?>[] types,Object... args){try{Method m=AiActivity.class.getDeclaredMethod(key,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception e){throw new RuntimeException(e);}}
  private void call(String key){call(key,new Class<?>[]{});}
  private void launch()throws Exception{launch(true);}
  private void launch(boolean online)throws Exception{if(online)waitFor(()->new AssistantNetwork(context,()->{}).validated());activity=(AiActivity)getInstrumentation().startActivitySync(new Intent(context,AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));getInstrumentation().waitForIdleSync();for(int i=0;i<300&&!activity.hasWindowFocus();i++)SystemClock.sleep(100);assertTrue("Activity window must be foreground",activity.hasWindowFocus());SystemClock.sleep(700);}
  private void close(){if(activity!=null){ui(activity::finish);activity=null;}}
  private JSONArray rows()throws Exception{return (JSONArray)get("messages");}
  private void waitFor(Callable<Boolean> condition)throws Exception{for(int i=0;i<120;i++){if(condition.call())return;SystemClock.sleep(100);}fail("Expected image state not reached");}
  private JSONObject picture()throws Exception{Bitmap b=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);b.eraseColor(Color.rgb(80,160,120));ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,out);b.recycle();return new JSONObject().put("provider","cloudflare").put("model","flux-1-schnell").put("generated",true).put("mime","image/png").put("width",128).put("height",128).put("data",android.util.Base64.encodeToString(out.toByteArray(),2));}
  private void install(OkHttpClient http)throws Exception{((ImageGenerationClient)get("imageClient")).close();set("imageClient",new ImageGenerationClient(http));set("imageAvailable",true);ui(()->call("updateImageMode"));}
  private void compose(String prompt){ui(()->{call("enterImageMode");((EditText)activity.findViewById(R.id.aiInput)).setText(prompt);activity.findViewById(R.id.aiSend).performClick();});}
  private void approve()throws Exception{AlertDialog d=(AlertDialog)get("selectionDialog");assertTrue(d.isShowing());ui(()->d.getButton(AlertDialog.BUTTON_POSITIVE).performClick());}
  private void shot(String name){UiEvidence.capture(getInstrumentation(),"ImageC09-"+name+"-"+(context.getResources().getConfiguration().fontScale>1.9?"200":"100"));}

  public void testBoundedImageDecodePrivateStorageAndReadOnlyShareUri()throws Exception{
    JSONObject payload=picture();GeneratedImages.Data data=GeneratedImages.Data.parse(payload);
    for(String key:new String[]{"provider","mime","data"}){JSONObject bad=new JSONObject(payload.toString()).put(key,"https://example.test/private");try{GeneratedImages.Data.parse(bad);fail("Unsafe generated image accepted");}catch(Exception expected){}}
    String chat=UUID.randomUUID().toString();CountDownLatch saved=new CountDownLatch(1);AtomicReference<String> id=new AtomicReference<>();
    try(GeneratedImages store=new GeneratedImages(context)){
      store.save(chat,data,new GeneratedImages.Result(){public void saved(String image,String mime){id.set(image);saved.countDown();}public void failed(){saved.countDown();}});assertTrue(saved.await(5,TimeUnit.SECONDS));assertNotNull(id.get());
      Uri uri=store.shareUri(chat,id.get(),data.mime);try(InputStream in=context.getContentResolver().openInputStream(uri)){assertTrue(java.util.Arrays.equals(data.bytes,in.readAllBytes()));}
      try{context.getContentResolver().openOutputStream(uri);fail("Share URI writable");}catch(FileNotFoundException expected){}
      try{GeneratedImages.imageFile(context,"../secret",id.get(),data.mime);fail("Traversal accepted");}catch(IllegalArgumentException expected){}
    }
    File interrupted=new File(GeneratedImages.imageFile(context,chat,id.get(),data.mime).getPath()+".new");try(FileOutputStream out=new FileOutputStream(interrupted)){out.write(data.bytes);}
    try(GeneratedImages reopened=new GeneratedImages(context)){Bitmap thumb=reopened.thumbnail(chat,id.get(),data.mime);assertNotNull(thumb);thumb.recycle();reopened.deleteChat(chat);}
    waitFor(()->!GeneratedImages.imageFile(context,chat,id.get(),data.mime).exists());
    waitFor(()->!interrupted.exists());
  }

  public void testConsentPromptOnlyImageHistoryAndSaveShareIntents()throws Exception{
    launch();AtomicInteger calls=new AtomicInteger();AtomicReference<JSONObject> posted=new AtomicReference<>();String response=picture().toString();
    install(new OkHttpClient.Builder().addInterceptor(chain->{if(chain.request().method().equals("GET"))return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic").body(ResponseBody.create(MediaType.parse("application/json"),"{\"imageGeneration\":true}")).build();calls.incrementAndGet();okio.Buffer buffer=new okio.Buffer();chain.request().body().writeTo(buffer);try{posted.set(new JSONObject(buffer.readUtf8()));}catch(JSONException e){throw new IOException(e);}return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic").body(ResponseBody.create(MediaType.parse("application/json"),response)).build();}).build());
    compose("Ilustrasi taman hijau untuk belajar");assertEquals(0,calls.get());shot("consent");approve();waitFor(()->rows().length()==2&&!(Boolean)get("waiting"));assertEquals(1,calls.get());
    assertEquals(2,posted.get().length());assertTrue(posted.get().has("prompt"));assertTrue(posted.get().has("requestId"));assertFalse(posted.get().toString().contains("PRIVATE-NEVER"));
    JSONObject generated=rows().getJSONObject(1);assertTrue(generated.has("image_id"));assertFalse(generated.has("data"));assertFalse(rows().toString().contains("base64"));
    String chat=(String)get("chatId"),id=generated.getString("image_id"),mime=generated.getString("image_mime");shot("result");
    Uri destination=Uri.parse("content://com.zikra.app.test.imageexport/synthetic.png");context.getContentResolver().delete(destination,null,null);
    IntentFilter saveFilter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);saveFilter.addCategory(Intent.CATEGORY_OPENABLE);saveFilter.addDataType("image/png");
    Instrumentation.ActivityMonitor save=getInstrumentation().addMonitor(saveFilter,new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(destination)),true);
    ui(()->call("saveGeneratedImage",new Class<?>[]{String.class,String.class,String.class},chat,id,mime));assertEquals(1,save.getHits());getInstrumentation().removeMonitor(save);
    waitFor(()->{try(InputStream in=context.getContentResolver().openInputStream(destination)){return Arrays.equals(in.readAllBytes(),GeneratedImages.Data.parse(picture()).bytes);}catch(Exception e){return false;}});context.getContentResolver().delete(destination,null,null);
    Instrumentation.ActivityMonitor share=getInstrumentation().addMonitor(new IntentFilter(Intent.ACTION_CHOOSER),new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);
    ui(()->call("shareGeneratedImage",new Class<?>[]{String.class,String.class,String.class},chat,id,mime));assertEquals(1,share.getHits());getInstrumentation().removeMonitor(share);
    close();launch();assertEquals(2,rows().length());assertEquals(id,rows().getJSONObject(1).getString("image_id"));assertTrue(GeneratedImages.imageFile(context,chat,id,mime).isFile());shot("restored");
  }

  public void testQuotaCancelAndNewChatSuppressLateImage()throws Exception{
    launch();AtomicInteger calls=new AtomicInteger();CountDownLatch began=new CountDownLatch(1),release=new CountDownLatch(1);String response=picture().toString();
    install(new OkHttpClient.Builder().addInterceptor(chain->{if(chain.request().method().equals("GET"))return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic").body(ResponseBody.create(MediaType.parse("application/json"),"{\"imageGeneration\":true}")).build();int n=calls.incrementAndGet();if(n==1){began.countDown();try{release.await(6,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(n==1?200:429).message("Synthetic").body(ResponseBody.create(MediaType.parse("application/json"),n==1?response:"{\"error\":\"image_user_quota\"}")).build();}).build());
    compose("Ilustrasi uji pembatalan");approve();assertTrue(began.await(3,TimeUnit.SECONDS));ui(()->activity.findViewById(R.id.aiSend).performClick());ui(()->activity.findViewById(R.id.aiClear).performClick());release.countDown();SystemClock.sleep(400);assertEquals(0,rows().length());
    ui(()->{try{set("imageAvailable",true);}catch(Exception e){throw new RuntimeException(e);}call("enterImageMode");});compose("Ilustrasi uji kuota");approve();waitFor(()->!(Boolean)get("waiting"));
    assertEquals(1,rows().length());assertEquals(context.getString(R.string.ai_images_user_quota),((TextView)activity.findViewById(R.id.aiRequestStatus)).getText().toString());assertEquals(2,calls.get());shot("quota");
    ui(()->{try{set("routingMode","offline");}catch(Exception e){throw new RuntimeException(e);}call("refreshImageCapability");});assertFalse((Boolean)get("imageAvailable"));
    ui(()->{try{set("routingMode","auto");}catch(Exception e){throw new RuntimeException(e);}call("refreshImageCapability");});waitFor(()->(Boolean)get("imageAvailable"));assertEquals("Recovery never auto-generates",2,calls.get());
  }

  public void testImageComposerAtLargeTextHasWholeSendAndAttachmentTargets()throws Exception{
    launch(false);set("imageAvailable",true);ui(()->call("enterImageMode"));
    EditText editor=activity.findViewById(R.id.aiInput);int[] point=new int[2];ui(()->editor.getLocationOnScreen(point));
    long time=SystemClock.uptimeMillis();float x=point[0]+editor.getWidth()/2f,y=point[1]+Math.min(editor.getHeight()/2f,32*context.getResources().getDisplayMetrics().density);
    android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,android.view.MotionEvent.ACTION_DOWN,x,y,0),up=android.view.MotionEvent.obtain(time,time+60,android.view.MotionEvent.ACTION_UP,x,y,0);
    down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
    getInstrumentation().getUiAutomation().injectInputEvent(down,true);getInstrumentation().getUiAutomation().injectInputEvent(up,true);down.recycle();up.recycle();
    ui(()->((android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editor,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));
    waitFor(()->activity.getWindow().getDecorView().getRootWindowInsets()!=null&&activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()));SystemClock.sleep(600);shot("composer");
    assertEquals("Welcome yields space to the keyboard",View.GONE,activity.findViewById(R.id.aiWelcome).getVisibility());
    for(int id:new int[]{R.id.aiSend,R.id.aiAttach}){View v=activity.findViewById(id);Rect rect=new Rect();assertTrue(v.getGlobalVisibleRect(rect));assertEquals(v.getHeight(),rect.height());}
  }

  public void testLiveCloudflareImageThroughHostedGateway()throws Exception{
    JSONObject cfg=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),liveFile).toPath()),StandardCharsets.UTF_8));assertEquals("https://zikra-ai.vercel.app/v1/assistant",cfg.getString("endpoint"));new AssistantConnection(context).save(cfg.getString("endpoint"),cfg.getString("token"));
    launch();waitFor(()->(Boolean)get("imageAvailable"));shot("online-ready");compose("A calm watercolor garden with green hills and a small pond, morning sunlight, no text, no people.");approve();long end=SystemClock.elapsedRealtime()+55000;while((Boolean)get("waiting")&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(150);
    assertEquals("Actual gateway image; status="+((TextView)activity.findViewById(R.id.aiRequestStatus)).getText(),2,rows().length());JSONObject image=rows().getJSONObject(1);assertTrue(image.has("image_id"));assertTrue(GeneratedImages.imageFile(context,(String)get("chatId"),image.getString("image_id"),image.getString("image_mime")).length()>1000);shot("live-result");
    File dir=new File(context.getExternalFilesDir(null),"validation");try(FileOutputStream out=new FileOutputStream(new File(dir,"ImageC09-live.json"))){out.write(new JSONObject().put("actualVercelCloudflare",true).put("interceptorUsed",false).put("width",image.getInt("image_width")).put("height",image.getInt("image_height")).put("localImageStored",true).toString(2).getBytes(StandardCharsets.UTF_8));}
  }
}
