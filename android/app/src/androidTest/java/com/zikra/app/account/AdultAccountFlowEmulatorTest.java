package com.zikra.app.account;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zikra.app.*;
import com.zikra.app.assistant.AssistantConnection;
import com.zikra.app.assistant.AssistantConsent;
import com.zikra.app.assistant.OnlineAssistantClient;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import org.json.*;

/** Real Firebase SDK + official local emulator + real account HTTP/Redis Lua. No production users. */
@SuppressWarnings("deprecation")
public final class AdultAccountFlowEmulatorTest extends InstrumentationTestCase {
  private FirebaseAuth auth;private FirebaseUser owned;private Activity page;
  private String address,secret;
  protected void setUp()throws Exception {
    super.setUp();assertTrue(BuildConfig.DEBUG&&BuildConfig.ACCOUNT_AUTH_EMULATOR&&BuildConfig.ADULT_ACCOUNTS_ENABLED);
    assertEquals("ranchu",Build.HARDWARE);
    assertTrue(new File(getInstrumentation().getTargetContext().getFilesDir(),"account-emulator-authorized").isFile());
    android.accessibilityservice.AccessibilityServiceInfo info=getInstrumentation().getUiAutomation().getServiceInfo();
    info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        |android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
    getInstrumentation().getUiAutomation().setServiceInfo(info);
    auth=AccountAuth.get(getInstrumentation().getTargetContext());assertNull(auth.getCurrentUser());
    address="zikra-adult-"+UUID.randomUUID()+"@example.test";secret="Synthetic-account-"+UUID.randomUUID();
    owned=Tasks.await(auth.createUserWithEmailAndPassword(address,secret),20,TimeUnit.SECONDS).getUser();assertNotNull(owned);
  }
  private void verify()throws Exception {
    Tasks.await(owned.sendEmailVerification(),15,TimeUnit.SECONDS);
    HttpURLConnection connection=(HttpURLConnection)new URL("http://10.0.2.2:9099/emulator/v1/projects/demo-zikra-accounts/oobCodes").openConnection();
    connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
    String code=null;
    try{
      assertEquals(200,connection.getResponseCode());byte[] raw=connection.getInputStream().readNBytes(65537);assertTrue(raw.length<=65536);
      JSONArray codes=new JSONObject(new String(raw,java.nio.charset.StandardCharsets.UTF_8)).getJSONArray("oobCodes");
      for(int i=0;i<codes.length();i++){JSONObject item=codes.getJSONObject(i);if(address.equals(item.optString("email"))&&"VERIFY_EMAIL".equals(item.optString("requestType")))code=item.getString("oobCode");}
    }finally{connection.disconnect();}
    assertNotNull(code);Tasks.await(auth.applyActionCode(code),15,TimeUnit.SECONDS);Tasks.await(owned.reload(),15,TimeUnit.SECONDS);
    Tasks.await(owned.getIdToken(true),15,TimeUnit.SECONDS);assertTrue(auth.getCurrentUser().isEmailVerified());
  }
  private void open(Class<? extends Activity> type){
    if(page!=null)getInstrumentation().runOnMainSync(page::finish);
    page=getInstrumentation().startActivitySync(new Intent(getInstrumentation().getTargetContext(),type)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  }
  private void click(int id){getInstrumentation().runOnMainSync(()->page.findViewById(id).performClick());}
  private void waitText(int id,int resource)throws Exception {
    long until=SystemClock.elapsedRealtime()+25_000;
    while(SystemClock.elapsedRealtime()<until){boolean[] match={false};getInstrumentation().runOnMainSync(()->match[0]=page.getString(resource).contentEquals(((TextView)page.findViewById(id)).getText()));if(match[0])return;Thread.sleep(100);}
    fail("Expected account screen status not reached");
  }
  private Object field(String name)throws Exception {Field field=page.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(page);}
  private void date(String field,LocalDate date)throws Exception {
    DatePickerDialog picker=(DatePickerDialog)field(field);
    getInstrumentation().runOnMainSync(()->{picker.getDatePicker().updateDate(date.getYear(),date.getMonthValue()-1,date.getDayOfMonth());picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
  }
  private Response request(String path,JSONObject value)throws Exception {
    OkHttpClient http=new OkHttpClient.Builder().callTimeout(15,TimeUnit.SECONDS).addInterceptor(AccountSession.interceptor()).build();
    Request.Builder request=new Request.Builder().url("http://10.0.2.2:9098"+path).post(RequestBody.create(MediaType.parse("application/json"),value.toString()));
    if(path.equals("/v1/assistant"))request.header("X-Zikra-Provider","cloudflare");
    else request.header("Authorization","Bearer "+AccountSession.token());
    return http.newCall(request.build()).execute();
  }
  public void testVerifiedAdultConfirmsUsesAiAndWithdrawsWithoutGuardian()throws Exception {
    verify();open(AdultAccountActivity.class);waitText(R.id.adultStatus,R.string.adult_needed);
    click(R.id.adultBirthDate);date("birthPicker",LocalDate.now(java.time.ZoneId.of("Asia/Jakarta")).minusYears(19));
    getInstrumentation().runOnMainSync(()->((CheckBox)page.findViewById(R.id.adultConsent)).setChecked(true));click(R.id.adultConfirm);
    waitText(R.id.adultStatus,R.string.adult_ready);
    com.zikra.app.UiEvidence.capture(getInstrumentation(),"adult-account-confirmed");
    JSONObject body=new JSONObject().put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content","Synthetic fixture only")));
    var context = getInstrumentation().getTargetContext();
    var preferences = context.getSharedPreferences("zikra_preferences", 0);
    AssistantConnection connection = new AssistantConnection(context);
    int timeoutsBefore = timeoutCount();
    assertTrue(connection.usesAccountGateway());
    assertFalse(connection.hasInstallation());
    AssistantConsent.accept(preferences, connection.endpoint());
    try (OnlineAssistantClient client = new OnlineAssistantClient()) {
      assertEquals("Synthetic adult flow answer", sendChat(client, connection, "Synthetic success"));
      assertEquals(OnlineAssistantClient.Failure.TIMEOUT, sendChat(client, connection, "Synthetic edge timeout"));
      assertEquals("Synthetic adult flow answer", sendChat(client, connection, "Synthetic retry"));
    } finally {
      AssistantConsent.revoke(preferences);
    }
    assertEquals(timeoutsBefore + 1, timeoutCount());
    click(R.id.adultWithdraw);
    List<AccessibilityNodeInfo> buttons=Collections.emptyList();long dialogUntil=SystemClock.elapsedRealtime()+5000;
    while(buttons.isEmpty()&&SystemClock.elapsedRealtime()<dialogUntil){
      AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
      if(root!=null)buttons=root.findAccessibilityNodeInfosByViewId("android:id/button1");
      if(buttons.isEmpty())Thread.sleep(100);
    }
    assertFalse("Withdraw confirmation button not accessible",buttons.isEmpty());assertTrue(buttons.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK));
    waitText(R.id.adultStatus,R.string.adult_withdrawn);
    assertTrue(AccountSession.held());
    try(Response response=request("/v1/assistant",body)){fail("Local withdrawal must stop the request");}catch(AccountSession.Required expected){}
    Request raw=new Request.Builder().url("http://10.0.2.2:9098/v1/assistant")
        .header("X-Zikra-Account-Token",AccountSession.token()).header("X-Zikra-Provider","cloudflare")
        .post(RequestBody.create(MediaType.parse("application/json"),body.toString())).build();
    try(Response response=new OkHttpClient().newCall(raw).execute()){assertEquals(403,response.code());}
  }
  private int timeoutCount() throws Exception {
    try (Response response = new OkHttpClient().newCall(new Request.Builder()
        .url("http://10.0.2.2:9098/__qa").build()).execute()) {
      assertEquals(200, response.code());
      return new JSONObject(response.body().string()).getInt("timeouts");
    }
  }

  private Object sendChat(OnlineAssistantClient client, AssistantConnection connection, String prompt)
      throws Exception {
    JSONObject payload = new JSONObject().put("messages", new JSONArray()
        .put(new JSONObject().put("role", "user").put("content", prompt)));
    CountDownLatch done = new CountDownLatch(1);
    AtomicReference<Object> outcome = new AtomicReference<>();
    AtomicReference<Exception> error = new AtomicReference<>();
    getInstrumentation().runOnMainSync(() -> {
      try {
        client.send(connection, payload, new OnlineAssistantClient.Result() {
          @Override public void complete(String reply) { outcome.set(reply); done.countDown(); }
          @Override public void failed() { done.countDown(); }
          @Override public void failed(OnlineAssistantClient.Failure failure) {
            outcome.set(failure);
            done.countDown();
          }
        });
      } catch (Exception failure) {
        error.set(failure);
        done.countDown();
      }
    });
    assertTrue("Missing chat result", done.await(20, TimeUnit.SECONDS));
    if (error.get() != null) throw error.get();
    return outcome.get();
  }

  public void testUnder18ServerBlockCannotBeRenewedAsAdult()throws Exception {
    verify();JSONObject body=new JSONObject().put("birthDate","2010-01-01").put("accepted",true).put("policyVersion","adult-beta-2026-11-v1");
    try(Response response=request("/v1/adult-account/declare",body)){assertEquals(200,response.code());assertEquals("under18_blocked",new JSONObject(response.body().string()).getString("status"));}
    open(AdultAccountActivity.class);waitText(R.id.adultStatus,R.string.adult_under18);
    getInstrumentation().runOnMainSync(()->assertEquals(android.view.View.GONE,page.findViewById(R.id.adultConfirm).getVisibility()));
    body.put("birthDate","1990-01-01");try(Response response=request("/v1/adult-account/renew",body)){assertEquals(403,response.code());}
  }
  public void testSignupChecksAgeBeforeCreatingFirebaseAccount()throws Exception {
    Tasks.await(owned.delete(),15,TimeUnit.SECONDS);owned=null;auth.signOut();open(AccountActivity.class);
    getInstrumentation().runOnMainSync(()->{
      ((EditText)page.findViewById(R.id.accountEmail)).setText(address);((EditText)page.findViewById(R.id.accountPassword)).setText(secret);
      ((CheckBox)page.findViewById(R.id.accountConsent)).setChecked(true);
    });
    click(R.id.accountCreate);date("signupAgePicker",LocalDate.now().minusYears(16));
    waitText(R.id.accountStatus,R.string.adult_under18);assertNull(auth.getCurrentUser());
  }
  public void testBirthDateAndConsentAreNotRestoredAfterRecreation()throws Exception {
    verify();open(AdultAccountActivity.class);waitText(R.id.adultStatus,R.string.adult_needed);
    click(R.id.adultBirthDate);date("birthPicker",LocalDate.now().minusYears(19));
    getInstrumentation().runOnMainSync(()->((CheckBox)page.findViewById(R.id.adultConsent)).setChecked(true));
    Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(AdultAccountActivity.class.getName(),null,false);
    try{
      getInstrumentation().runOnMainSync(page::recreate);
      page=getInstrumentation().waitForMonitorWithTimeout(monitor,10_000);assertNotNull(page);
      waitText(R.id.adultStatus,R.string.adult_needed);
      getInstrumentation().runOnMainSync(()->{
        assertEquals(page.getString(R.string.adult_date),((TextView)page.findViewById(R.id.adultBirthDate)).getText());
        assertFalse(((CheckBox)page.findViewById(R.id.adultConsent)).isChecked());
      });
    }finally{getInstrumentation().removeMonitor(monitor);}
  }
  public void testAccountTokenCannotBeForwardedToOtherHostsOrQueryUrls(){
    assertFalse(AccountSession.trusted(HttpUrl.parse("https://zikra-ai.vercel.app/v1/assistant")));
    assertTrue(AccountSession.trusted(HttpUrl.parse("http://10.0.2.2:9098/v1/assistant")));
    for(String url:new String[]{"https://evil.example/v1/assistant","https://zikra-ai.vercel.app:444/v1/assistant","https://zikra-ai.vercel.app/v1/assistant?target=evil","https://user@zikra-ai.vercel.app/v1/assistant","http://10.0.2.2:9999/v1/assistant"})assertFalse(AccountSession.trusted(HttpUrl.parse(url)));
  }
  protected void tearDown()throws Exception {
    try{try{if(owned!=null)Tasks.await(owned.delete(),20,TimeUnit.SECONDS);}finally{if(auth!=null)auth.signOut();AccountSession.invalidate();if(page!=null)getInstrumentation().runOnMainSync(page::finish);}}
    finally{secret=null;address=null;super.tearDown();}
  }
}
