package com.zikra.app.assistant;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.widget.EditText;
import com.zikra.app.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Explicit live run only, private short-lived gateway fixture installed after host backup. */
@SuppressWarnings("deprecation")
public final class AiVisionHostedTest extends InstrumentationTestCase {
  private android.content.Context context;private AiActivity activity;
  private Object get(String key)throws Exception{Field f=AiActivity.class.getDeclaredField(key);f.setAccessible(true);return f.get(activity);}
  private void set(String key,Object value)throws Exception{Field f=AiActivity.class.getDeclaredField(key);f.setAccessible(true);f.set(activity,value);}
  private void ui(Runnable r){getInstrumentation().runOnMainSync(r);getInstrumentation().waitForIdleSync();}
  private void invoke(String name)throws Exception{Method m=AiActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}
  public void testActualPhotoAndScannedPdfThroughVercel()throws Exception{
    context=getInstrumentation().getTargetContext();
    try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand("getprop ro.kernel.qemu"))){assertEquals("1",new String(in.readAllBytes(),StandardCharsets.UTF_8).trim());}
    assertTrue(new File(context.getFilesDir(),"ai-refresh-test-authorized").isFile());
    JSONObject config=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"ai-vision-live.json").toPath()),StandardCharsets.UTF_8));
    assertEquals("https://zikra-ai.vercel.app/v1/assistant",config.getString("endpoint"));
    context.deleteDatabase("assistant_chats.db");context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true).putString("assistant_mode_v2","auto").remove("assistant_history_v1").remove("assistant_active_chat").commit();
    new AssistantConnection(context).save(config.getString("endpoint"),config.getString("token"));
    JSONArray results=new JSONArray();
    try{
      activity=(AiActivity)getInstrumentation().startActivitySync(new Intent(context,AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));SystemClock.sleep(700);
      assertTrue(new AssistantNetwork(context,()->{}).validated());
      for(boolean scan:new boolean[]{false,true}){
        if(scan)ui(()->{try{invoke("newConversation");}catch(Exception e){throw new RuntimeException(e);}});
        String code=scan?"ZK-8531":"ZK-7429",equation=scan?"28 + 34 = ?":"17 + 26 = ?",answer=scan?"62":"43";
        ByteArrayOutputStream out=new ByteArrayOutputStream();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(38);
        if(scan){PdfDocument pdf=new PdfDocument();PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(500,300,1).create());page.getCanvas().drawText(code,25,80,paint);page.getCanvas().drawText(equation,25,170,paint);pdf.finishPage(page);pdf.writeTo(out);pdf.close();}
        else{Bitmap b=Bitmap.createBitmap(500,300,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);canvas.drawText(code,25,80,paint);canvas.drawText(equation,25,170,paint);b.compress(Bitmap.CompressFormat.PNG,100,out);b.recycle();}
        Method decode=AssistantAttachments.class.getDeclaredMethod("decode",String.class,String.class,byte[].class);decode.setAccessible(true);
        AssistantAttachments.Draft draft=(AssistantAttachments.Draft)decode.invoke(get("attachmentReader"),scan?"scan-sintetis.pdf":"foto-sintetis.png",scan?"application/pdf":"image/png",out.toByteArray());
        ui(()->{try{set("attachment",draft);set("importingAttachment",false);invoke("renderAttachment");}catch(Exception e){throw new RuntimeException(e);}((EditText)activity.findViewById(R.id.aiInput)).setText("Hitung soal pada lampiran. Jawab dengan persamaan dan hasilnya.");AssistantReviewFixture.click(activity,R.id.aiSend);});
        AlertDialog consent=(AlertDialog)get("selectionDialog");assertTrue(consent.isShowing());ui(()->consent.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        long deadline=SystemClock.elapsedRealtime()+55000;while((Boolean)get("waiting")&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(150);
        JSONArray rows=(JSONArray)get("messages");
        if(rows.length()==1){ui(()->AssistantReviewFixture.click(activity,R.id.aiRetry));final AlertDialog retry=(AlertDialog)get("selectionDialog");ui(()->retry.getButton(AlertDialog.BUTTON_POSITIVE).performClick());deadline=SystemClock.elapsedRealtime()+55000;while((Boolean)get("waiting")&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(150);rows=(JSONArray)get("messages");}
        UiEvidence.capture(getInstrumentation(),scan?"AI195-live-scan-attempt":"AI195-live-photo-attempt");
        assertEquals("Actual hosted reply; status="+((android.widget.TextView)activity.findViewById(R.id.aiRequestStatus)).getText(),2,rows.length());String reply=rows.getJSONObject(1).getString("text");assertTrue("Visual arithmetic solved: "+reply,reply.contains(answer));assertEquals("online",rows.getJSONObject(1).getString("mode"));assertNull(get("attachment"));
        results.put(new JSONObject().put("kind",scan?"scanned-pdf":"photo").put("arithmeticCorrect",true).put("numbersOnlyInsideImage",true).put("actualVercel",true).put("interceptorUsed",false));
        UiEvidence.capture(getInstrumentation(),scan?"AI195-live-scan":"AI195-live-photo");
      }
    }finally{if(activity!=null)ui(activity::finish);}
    File dir=new File(context.getExternalFilesDir(null),"validation");try(FileOutputStream out=new FileOutputStream(new File(dir,"AI195-live.json"))){out.write(new JSONObject().put("scope","Synthetic photo/scanned PDF, APK to actual Vercel to provider; emulator, no physical camera claim").put("results",results).toString(2).getBytes(StandardCharsets.UTF_8));}
  }
}
