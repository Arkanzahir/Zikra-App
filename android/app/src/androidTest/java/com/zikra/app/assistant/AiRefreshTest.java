package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.*;
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
import java.util.zip.*;
import okhttp3.*;
import org.json.*;

/** Requires an emulator and host-side backup/isolation. All uploaded material is synthetic. */
@SuppressWarnings("deprecation")
public final class AiRefreshTest extends InstrumentationTestCase {
  private Context context;
  private AiActivity activity;
  private static final String CODE="synthetic-access-code-32-characters-only";
  @Override protected void setUp() throws Exception {
    super.setUp();context=getInstrumentation().getTargetContext();
    assertEquals("Emulator only","1",shell("getprop ro.kernel.qemu").trim());
    assertTrue("Host backup/isolation required",new File(context.getFilesDir(),"ai-refresh-test-authorized").isFile());
    context.deleteDatabase("assistant_chats.db");
    context.getSharedPreferences("zikra_preferences",0).edit().remove("assistant_history_v1").remove("assistant_active_chat").putBoolean("onboarding_complete",true).putString("assistant_mode_v2","auto").putString("profile_display_name","PRIVATE-NOT-SELECTED").commit();
    new AssistantConnection(context).save("https://example.test/v1/assistant",CODE);
  }
  @Override protected void tearDown() throws Exception {
    close();context.deleteDatabase("assistant_chats.db");super.tearDown();
  }
  private String shell(String command)throws Exception{try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
  private void ui(Runnable action){getInstrumentation().runOnMainSync(action);getInstrumentation().waitForIdleSync();}
  private void close(){if(activity!=null){ui(activity::finish);activity=null;}}
  private void launch(){activity=(AiActivity)getInstrumentation().startActivitySync(new Intent(context,AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));getInstrumentation().waitForIdleSync();SystemClock.sleep(600);}
  private Object field(String name)throws Exception{Field f=AiActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
  private void set(String name,Object value)throws Exception{Field f=AiActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
  private void call(String name,Class<?>[] types,Object... args){try{Method m=AiActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception e){throw new RuntimeException(e);}}
  private void call(String name){call(name,new Class<?>[]{});}
  private JSONArray rows()throws Exception{return (JSONArray)field("messages");}
  private void shot(String name){UiEvidence.capture(getInstrumentation(),"AI195-"+name+"-"+(context.getResources().getConfiguration().fontScale>1.9?"200":"100"));}
  private void waitUntil(Callable<Boolean> condition)throws Exception{for(int i=0;i<120;i++){if(condition.call())return;SystemClock.sleep(100);}fail("Condition did not complete");}
  private AssistantAttachments.Draft decode(AssistantAttachments reader,String name,String mime,byte[] bytes)throws Exception{Method m=AssistantAttachments.class.getDeclaredMethod("decode",String.class,String.class,byte[].class);m.setAccessible(true);try{return (AssistantAttachments.Draft)m.invoke(reader,name,mime,bytes);}catch(InvocationTargetException e){throw (Exception)e.getCause();}}
  private byte[] image(){Bitmap b=Bitmap.createBitmap(500,250,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);Paint p=new Paint();p.setColor(Color.BLACK);p.setTextSize(38);c.drawText("ZK-7429",25,70,p);c.drawText("17 + 26 = ?",25,150,p);ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,out);b.recycle();return out.toByteArray();}
  private byte[] pdf(int count)throws Exception{PdfDocument pdf=new PdfDocument();for(int i=0;i<count;i++){PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(400,500,i+1).create());Paint p=new Paint();p.setTextSize(28);page.getCanvas().drawText("17 + 26 = ?",25,90,p);pdf.finishPage(page);}ByteArrayOutputStream out=new ByteArrayOutputStream();pdf.writeTo(out);pdf.close();return out.toByteArray();}
  private byte[] zip(String path,String content)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry(path));z.write(content.getBytes(StandardCharsets.UTF_8));z.closeEntry();}return out.toByteArray();}

  public void testLegacyMigrationAndConversationIsolationAndLimits()throws Exception{
    String legacy=new JSONArray().put(new JSONObject().put("role","user").put("text","Lama 100%_ujian").put("request_id","retained-id"))
      .put(new JSONObject().put("role","assistant").put("text","Jawaban lama").put("mode","online").put("source",new JSONObject().put("chapter",1))).toString();
    context.getSharedPreferences("zikra_preferences",0).edit().putString("assistant_history_v1",legacy).commit();
    String second;
    try(ConversationStore store=new ConversationStore(context)){
      assertEquals(1,store.list("").size());assertEquals(legacy,store.get("legacy-local").messages.toString());
      second=store.create().id;store.save(second,new JSONArray().put(new JSONObject().put("role","user").put("text","Chat kedua")),"Draf kedua");
      store.rename(second,"Ujian Biologi");assertEquals(1,store.list("Biologi").size());assertEquals(1,store.list("100%_").size());
      JSONArray invalid=new JSONArray().put(new JSONObject().put("role","user").put("text",42));
      try{store.save(second,invalid,"");fail("Invalid storage accepted");}catch(IllegalArgumentException expected){}
      assertEquals("Draf kedua",store.get(second).draft);assertEquals(legacy,context.getSharedPreferences("zikra_preferences",0).getString("assistant_history_v1",""));
    }
    try(ConversationStore store=new ConversationStore(context)){
      assertEquals(2,store.list("").size());assertEquals("Ujian Biologi",store.get(second).title);
      store.delete(second);assertNotNull(store.get("legacy-local"));store.delete("legacy-local");
      assertFalse(context.getSharedPreferences("zikra_preferences",0).contains("assistant_history_v1"));
      for(int i=0;i<40;i++)store.create();try{store.create();fail("Silent chat eviction");}catch(IllegalStateException expected){}assertEquals(40,store.list("").size());
    }
  }

  public void testParsersPhotoPdfDocxTextAndHostileFiles()throws Exception{
    try(AssistantAttachments reader=new AssistantAttachments(context)){
      AssistantAttachments.Draft photo=decode(reader,"uji.png","image/png",image());
      JSONObject normalized=photo.payload().getJSONArray("images").getJSONObject(0);assertEquals("image/jpeg",normalized.getString("mime"));
      byte[] bytes=android.util.Base64.decode(normalized.getString("data"),0);assertTrue(bytes.length<=360000);assertEquals(255,bytes[0]&255);
      assertEquals(3,decode(reader,"scan.pdf","application/pdf",pdf(3)).imageCount());
      assertEquals("Tugas sintetis",decode(reader,"uji.txt","text/plain","Tugas sintetis".getBytes(StandardCharsets.UTF_8)).text);
      String xml="<w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body><w:p><w:r><w:t>Dokumen sintetis</w:t></w:r></w:p></w:body></w:document>";
      assertEquals("Dokumen sintetis",decode(reader,"uji.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",zip("word/document.xml",xml)).text);
      Object[][] invalid={{"uji.png","application/pdf",image()},{"scan.pdf","application/pdf",pdf(4)},{"rusak.pdf","application/pdf","%PDF-broken".getBytes(StandardCharsets.UTF_8)},
        {"uji.txt","text/plain",new byte[]{(byte)0xFF}},{"uji.docx","application/zip",zip("../word/document.xml",xml)},
        {"uji.docx","application/zip",zip("word/document.xml","<!DOCTYPE x [<!ENTITY y SYSTEM 'file:///data/secret'>]><x>&y;</x>")},
        {"uji.docx","application/zip",zip("word/document.xml","x".repeat(2_000_001))},{"uji.txt","text/plain","x".repeat(12001).getBytes(StandardCharsets.UTF_8)}};
      for(Object[] item:invalid)try{decode(reader,(String)item[0],(String)item[1],(byte[])item[2]);fail("Invalid file accepted: "+item[0]);}catch(IOException|org.xmlpull.v1.XmlPullParserException|SecurityException expected){}
    }
  }

  public void testCameraProviderImportRestoreAndDelete()throws Exception{
    String chat=UUID.randomUUID().toString();Uri uri=AssistantCameraProvider.create(context);
    Bitmap b=Bitmap.createBitmap(20,30,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);
    try(OutputStream out=context.getContentResolver().openOutputStream(uri)){b.compress(Bitmap.CompressFormat.JPEG,80,out);}b.recycle();
    CountDownLatch loaded=new CountDownLatch(1);AtomicReference<AssistantAttachments.Draft> draft=new AtomicReference<>();AtomicReference<String> error=new AtomicReference<>();
    try(AssistantAttachments reader=new AssistantAttachments(context)){
      reader.load(chat,uri,new AssistantAttachments.Result(){public void loaded(AssistantAttachments.Draft d){draft.set(d);loaded.countDown();}public void failed(String e){error.set(e);loaded.countDown();}});
      assertTrue(loaded.await(8,TimeUnit.SECONDS));assertNull(error.get());assertNotNull(draft.get());
      CountDownLatch restored=new CountDownLatch(1);reader.restore(chat,new AssistantAttachments.Result(){public void loaded(AssistantAttachments.Draft d){assertEquals(draft.get().id,d.id);restored.countDown();}public void failed(String e){error.set(e);restored.countDown();}});
      assertTrue(restored.await(5,TimeUnit.SECONDS));assertNull(error.get());reader.deleteConversation(chat);
    }
    waitUntil(()->!new File(context.getFilesDir(),"assistant_attachments/"+chat).exists());
    try{context.getContentResolver().openInputStream(uri);fail("Temporary camera file retained");}catch(FileNotFoundException expected){}
  }

  public void testNativeChatHistorySearchAndDraftSurviveReopen()throws Exception{
    new AssistantConnection(context).offline();context.getSharedPreferences("zikra_preferences",0).edit().putString("assistant_mode_v2","offline").commit();
    launch();shot("welcome");
    ui(()->{((EditText)activity.findViewById(R.id.aiInput)).setText("Bantu rencana belajar");activity.findViewById(R.id.aiSend).performClick();});
    assertEquals(2,rows().length());String original=(String)field("chatId");shot("chat");
    ui(()->{((EditText)activity.findViewById(R.id.aiInput)).setText("Draf pertama");activity.findViewById(R.id.aiClear).performClick();});
    assertFalse(original.equals(field("chatId")));assertEquals(0,rows().length());
    ui(()->{((EditText)activity.findViewById(R.id.aiInput)).setText("Draf kedua");activity.findViewById(R.id.toolbarBack).performClick();});
    assertNotNull(activity.findViewById(R.id.aiHistoryOverlay));shot("history");
    ui(()->((EditText)activity.findViewById(R.id.aiHistorySearch)).setText("rencana belajar"));
    assertEquals(2,((LinearLayout)activity.findViewById(R.id.aiHistoryList)).getChildCount());
    ui(()->call("selectConversation",new Class<?>[]{String.class},original));
    assertEquals("Draf pertama",((EditText)activity.findViewById(R.id.aiInput)).getText().toString());close();launch();
    assertEquals(original,field("chatId"));assertEquals(2,rows().length());assertEquals("Draf pertama",((EditText)activity.findViewById(R.id.aiInput)).getText().toString());
    ui(()->call("showChatActions",new Class<?>[]{String.class,String.class},original,"Judul lama"));
    AlertDialog actions=(AlertDialog)field("clearDialog");final AlertDialog renameMenu=actions;ui(()->renameMenu.getListView().performItemClick(renameMenu.getListView().getChildAt(0),0,0));
    AlertDialog rename=(AlertDialog)field("clearDialog");EditText title=findEdit(rename.getWindow().getDecorView());assertNotNull(title);
    ui(()->{title.setText("Materi Biologi");rename.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
    ConversationStore store=(ConversationStore)field("conversations");assertEquals("Materi Biologi",store.get(original).title);shot("history-renamed");
    ui(()->call("showChatActions",new Class<?>[]{String.class,String.class},original,"Materi Biologi"));
    actions=(AlertDialog)field("clearDialog");final AlertDialog menu=actions;ui(()->menu.getListView().performItemClick(menu.getListView().getChildAt(1),1,1));
    AlertDialog confirm=(AlertDialog)field("clearDialog");assertEquals(2,store.list("").size());ui(()->confirm.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());assertNotNull(store.get(original));
    ui(()->call("showChatActions",new Class<?>[]{String.class,String.class},original,"Materi Biologi"));
    final AlertDialog menu2=(AlertDialog)field("clearDialog");ui(()->menu2.getListView().performItemClick(menu2.getListView().getChildAt(1),1,1));
    final AlertDialog delete=(AlertDialog)field("clearDialog");ui(()->delete.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
    assertNull(store.get(original));assertEquals(1,store.list("").size());assertEquals("Draf kedua",((EditText)activity.findViewById(R.id.aiInput)).getText().toString());
  }
  private EditText findEdit(View view){if(view instanceof EditText)return (EditText)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){EditText found=findEdit(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}

  public void testBalancedPanelsKeepDraftAndCancelWithoutSend()throws Exception{
    new AssistantConnection(context).offline();context.getSharedPreferences("zikra_preferences",0).edit().putString("assistant_mode_v2","offline").commit();launch();
    if(Build.VERSION.SDK_INT>=30){
      android.graphics.Insets bars=activity.getWindow().getDecorView().getRootWindowInsets().getInsets(WindowInsets.Type.systemBars());
      android.graphics.Rect header=new android.graphics.Rect(),composer=new android.graphics.Rect();
      assertTrue(activity.findViewById(R.id.utilitiesHeader).getGlobalVisibleRect(header));
      assertTrue(activity.findViewById(R.id.aiComposer).getGlobalVisibleRect(composer));
      assertTrue("Header below system status bar",header.top>=bars.top);
      assertTrue("Composer above system navigation",composer.bottom<=activity.getWindow().getDecorView().getHeight()-bars.bottom);
    }
    assertFalse(activity.findViewById(R.id.aiSend).isEnabled());
    ui(()->((EditText)activity.findViewById(R.id.aiInput)).setText("Draf sintetis tetap di perangkat"));
    ui(()->activity.findViewById(R.id.aiAttach).performClick());
    AlertDialog attachments=(AlertDialog)field("clearDialog");assertTrue(attachments.isShowing());
    assertEquals(0,rows().length());UiEvidence.capture(getInstrumentation(),"AI198-attachments");
    ui(attachments::cancel);assertEquals("Draf sintetis tetap di perangkat",((EditText)activity.findViewById(R.id.aiInput)).getText().toString());
    ui(()->activity.findViewById(R.id.toolbarBack).performClick());
    assertNotNull(activity.findViewById(R.id.aiHistoryOverlay));UiEvidence.capture(getInstrumentation(),"AI198-history");
    ui(()->call("closeHistory"));assertEquals(0,rows().length());
    assertEquals("Draf sintetis tetap di perangkat",((EditText)activity.findViewById(R.id.aiInput)).getText().toString());
    ui(()->((EditText)activity.findViewById(R.id.aiInput)).setText(""));UiEvidence.capture(getInstrumentation(),"AI198-welcome");
  }

  public void testConsentAttachmentIsolationAndClearOnlyAfterSuccess()throws Exception{
    assertTrue("Validated internet needed",new AssistantNetwork(context,()->{}).validated());launch();
    AtomicInteger calls=new AtomicInteger();AtomicReference<JSONObject> captured=new AtomicReference<>();
    OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{calls.incrementAndGet();okio.Buffer body=new okio.Buffer();chain.request().body().writeTo(body);try{captured.set(new JSONObject(body.readUtf8()));}catch(JSONException e){throw new IOException(e);}assertEquals("1",chain.request().header("X-Zikra-Attachments"));
      return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic").body(ResponseBody.create(MediaType.parse("application/json"),"{\"reply\":\"Jawaban uji lampiran\",\"provider\":\"featherless\"}")).build();}).build();
    ((OnlineAssistantClient)field("onlineClient")).close();set("onlineClient",new OnlineAssistantClient(http));
    AssistantAttachments.Draft draft=decode((AssistantAttachments)field("attachmentReader"),"kode-uji.png","image/png",image());
    ui(()->{try{set("attachment",draft);set("importingAttachment",false);}catch(Exception e){throw new RuntimeException(e);}call("renderAttachment");});
    shot("attachment");assertEquals(0,calls.get());
    ui(()->activity.findViewById(R.id.aiAttachmentPreview).performClick());shot("preview");ui(()->{try{((AlertDialog)field("sourceDialog")).dismiss();}catch(Exception e){throw new RuntimeException(e);}});
    ui(()->{((EditText)activity.findViewById(R.id.aiInput)).setText("Baca kode uji");activity.findViewById(R.id.aiSend).performClick();});
    AlertDialog dialog=(AlertDialog)field("selectionDialog");assertTrue(dialog.isShowing());assertEquals(0,calls.get());
    ArrayList<CheckBox> choices=new ArrayList<>();checks(dialog.getWindow().getDecorView(),choices);assertEquals(5,choices.size());for(CheckBox choice:choices)assertFalse(choice.isChecked());
    shot("consent");ui(()->dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitUntil(()->calls.get()==1&&rows().length()==2);
    assertEquals(0,captured.get().getJSONObject("context").length());assertEquals(1,captured.get().getJSONArray("messages").length());assertFalse(captured.get().toString().contains("PRIVATE-NOT-SELECTED"));
    assertEquals(1,captured.get().getJSONArray("attachments").length());assertNull(field("attachment"));
    assertEquals("",((EditText)activity.findViewById(R.id.aiInput)).getText().toString());
  }
  private void checks(View view,List<CheckBox> result){if(view instanceof CheckBox)result.add((CheckBox)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)checks(((ViewGroup)view).getChildAt(i),result);}

  public void testAutoNetworkChangeNeverUploadsDraft()throws Exception{
    boolean wifi=shell("settings get global wifi_on").trim().equals("1"),data=shell("settings get global mobile_data").trim().equals("1");
    try{
      launch();AtomicInteger requests=new AtomicInteger();((OnlineAssistantClient)field("onlineClient")).close();set("onlineClient",new OnlineAssistantClient(new OkHttpClient.Builder().addInterceptor(chain->{requests.incrementAndGet();throw new IOException("No request expected");}).build()));
      ui(()->((EditText)activity.findViewById(R.id.aiInput)).setText("Tidak boleh terkirim otomatis"));
      shell("svc wifi disable");shell("svc data disable");waitUntil(()->!new AssistantNetwork(context,()->{}).validated());
      waitUntil(()->((TextView)activity.findViewById(R.id.aiMode)).getText().toString().contains("Offline"));assertEquals(0,requests.get());
      shot("offline");shell("svc wifi enable");waitUntil(()->new AssistantNetwork(context,()->{}).validated());
      waitUntil(()->((TextView)activity.findViewById(R.id.aiMode)).getText().toString().contains("Online"));assertEquals(0,requests.get());assertEquals(0,rows().length());
      assertEquals("Tidak boleh terkirim otomatis",((EditText)activity.findViewById(R.id.aiInput)).getText().toString());
    }finally{shell("svc wifi "+(wifi?"enable":"disable"));shell("svc data "+(data?"enable":"disable"));}
  }

  public void testLargeTextComposerAndKeyboardRemainReachable()throws Exception{
    new AssistantConnection(context).offline();context.getSharedPreferences("zikra_preferences",0).edit().putString("assistant_mode_v2","offline").commit();launch();
    android.graphics.Rect composer=new android.graphics.Rect();assertTrue(activity.findViewById(R.id.aiComposer).getGlobalVisibleRect(composer));assertTrue(composer.height()>40);
    ui(()->{EditText input=activity.findViewById(R.id.aiInput);input.requestFocus();((android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
    SystemClock.sleep(900);shot("keyboard");android.graphics.Rect send=new android.graphics.Rect();assertTrue(activity.findViewById(R.id.aiSend).getGlobalVisibleRect(send));
    assertEquals("Entire send target visible",activity.findViewById(R.id.aiSend).getHeight(),send.height());
    android.graphics.Rect attach=new android.graphics.Rect();assertTrue(activity.findViewById(R.id.aiAttach).getGlobalVisibleRect(attach));assertEquals("Entire attachment target visible",activity.findViewById(R.id.aiAttach).getHeight(),attach.height());
    android.view.WindowInsets insets=activity.getWindow().getDecorView().getRootWindowInsets();int keyboard=insets.getInsets(WindowInsets.Type.ime()).bottom;
    assertTrue("Send above keyboard",send.bottom<=activity.getWindow().getDecorView().getHeight()-keyboard+3);
    assertTrue("Input remains usable",activity.findViewById(R.id.aiInput).getWidth()>70);
  }
}
