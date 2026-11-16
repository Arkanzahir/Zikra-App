package com.zikra.app.account;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.zikra.app.*;
import com.zikra.app.ui.*;
import java.time.LocalDate;
import org.json.JSONObject;

/** Explicit18+ declaration, verified-email session and owner-controlled withdrawal. */
public final class AdultAccountActivity extends BaseActivity {
  private final AdultAccountClient client=new AdultAccountClient();
  private TextView status,date;
  private CheckBox consent;
  private Button confirm,refresh,withdraw;
  private LocalDate born;
  private String policy="",serverStatus="";
  private boolean busy;
  private DatePickerDialog birthPicker;
  private AlertDialog withdrawalDialog;
  @Override protected void onCreate(Bundle saved){
    super.onCreate(saved);
    if(!BuildConfig.ACCOUNT_SIGN_IN_ENABLED||!BuildConfig.ADULT_ACCOUNTS_ENABLED){finish();return;}
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(getColor(R.color.refined_background));
    FeatureHeader header=new FeatureHeader(this,null);header.bind(0,0,0,0,getString(R.string.adult_title),null,0,null);
    header.back().setOnClickListener(v->finish());root.addView(header);
    ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);int pad=dp(24);body.setPadding(pad,pad,pad,pad);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    body.addView(text(R.string.adult_heading,26,true));body.addView(text(R.string.adult_intro,15,false));
    status=text(R.string.adult_busy,15,false);status.setId(R.id.adultStatus);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(status);
    date=text(R.string.adult_date,16,false);date.setId(R.id.adultBirthDate);date.setMinHeight(dp(56));date.setGravity(android.view.Gravity.CENTER_VERTICAL);date.setFocusable(true);date.setClickable(true);date.setSaveEnabled(false);
    date.setBackground(RefinedUi.surface(this,R.color.refined_surface,true,16));date.setPadding(dp(16),dp(12),dp(16),dp(12));body.addView(date);
    date.setOnClickListener(v->chooseDate());
    consent=new CheckBox(this);consent.setId(R.id.adultConsent);consent.setText(R.string.adult_consent);consent.setTextSize(14);consent.setTextColor(getColor(R.color.refined_ink));consent.setMinHeight(dp(64));consent.setSaveEnabled(false);body.addView(consent);
    confirm=button(body,R.id.adultConfirm,R.string.adult_confirm,true);confirm.setOnClickListener(v->declare());
    refresh=button(body,R.id.adultRefresh,R.string.adult_refresh,false);refresh.setOnClickListener(v->request("status",null));
    Button signin=button(body,R.id.adultSignIn,R.string.adult_signin,false);signin.setOnClickListener(v->startActivity(new Intent(this,AccountActivity.class)));
    withdraw=button(body,R.id.adultWithdraw,R.string.adult_withdraw,false);withdraw.setOnClickListener(v->{withdrawalDialog=new AlertDialog.Builder(this)
        .setTitle(R.string.adult_withdraw).setMessage(R.string.adult_withdraw_note)
        .setNegativeButton(R.string.contextual_cancel,null).setPositiveButton(R.string.adult_withdraw,(d,w)->{
          AccountSession.hold();request("withdraw",new JSONObject());
        }).create();withdrawalDialog.show();});
    body.addView(text(R.string.adult_privacy,13,false));
    setContentView(root);setupScreen(getString(R.string.adult_title));RefinedUi.chrome(this,header,false);
    request("status",null);
  }
  private TextView text(int string,int size,boolean bold){
    TextView view=RefinedUi.text(this,getString(string),size,bold?R.color.refined_ink:R.color.refined_muted,bold);
    view.setLineSpacing(dp(4),1);view.setPadding(0,0,0,dp(16));return view;
  }
  private Button button(LinearLayout body,int id,int label,boolean primary){
    Button button=new Button(this);button.setId(id);button.setText(label);button.setAllCaps(false);button.setTextSize(15);button.setMinHeight(dp(56));button.setPadding(dp(16),dp(12),dp(16),dp(12));
    button.setTextColor(getColor(primary?R.color.refined_on_primary:R.color.refined_ink));button.setBackground(RefinedUi.surface(this,primary?R.color.refined_primary:R.color.refined_surface,!primary,16));
    LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);body.addView(button,params);return button;
  }
  private void chooseDate(){
    if(busy)return;LocalDate selected=born==null?LocalDate.now():born;
    DatePickerDialog picker=new DatePickerDialog(this,(v,y,m,d)->{
      born=LocalDate.of(y,m+1,d);date.setText(born.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy",java.util.Locale.forLanguageTag("id-ID"))));
    },selected.getYear(),selected.getMonthValue()-1,selected.getDayOfMonth());
    birthPicker=picker;picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();
  }
  private void declare(){
    if(busy||policy.isEmpty()||born==null||!consent.isChecked()){status.setText(R.string.adult_check);return;}
    if(!AdultAgePolicy.adult(born,LocalDate.now(java.time.ZoneId.of("Asia/Jakarta")))){status.setText(R.string.adult_under18);return;}
    try{
      JSONObject body=new JSONObject().put("birthDate",born.toString()).put("accepted",true).put("policyVersion",policy);
      AccountSession.invalidate();request(serverStatus.equals("not_registered")?"declare":"renew",body);
    }catch(org.json.JSONException invalid){status.setText(R.string.adult_check);}
  }
  private void request(String action,JSONObject body){
    if(busy)return;busy=true;render();status.setText(R.string.adult_busy);
    client.send(action,body,new AdultAccountClient.Result(){
      public void complete(JSONObject result){
        busy=false;policy=result.optString("policyVersion");serverStatus=result.optString("status");
        if(serverStatus.equals("adult_confirmed")){
          if(action.equals("declare")||action.equals("renew"))AccountSession.releaseHold();
          else if(AccountSession.held())serverStatus="withdraw_pending";
        }
        born=null;date.setText(R.string.adult_date);consent.setChecked(false);
        status.setText(message(serverStatus));render();
      }
      public void failed(String code){busy=false;status.setText(code.equals("under18_blocked")?R.string.adult_under18:code.equals("unauthorized")?R.string.adult_reauth:R.string.adult_unavailable);render();}
    });
  }
  private int message(String state){
    switch(state){
      case "adult_confirmed":return R.string.adult_ready;
      case "under18_blocked":return R.string.adult_under18;
      case "withdrawn":return R.string.adult_withdrawn;
      case "withdraw_pending":return R.string.adult_withdraw_pending;
      case "confirmation_expired":case "policy_changed":return R.string.adult_expired;
      default:return R.string.adult_needed;
    }
  }
  private void render(){
    boolean blocked=serverStatus.equals("under18_blocked"),active=serverStatus.equals("adult_confirmed"),pending=serverStatus.equals("withdraw_pending");
    date.setVisibility(active||blocked||pending?View.GONE:View.VISIBLE);consent.setVisibility(active||blocked||pending?View.GONE:View.VISIBLE);confirm.setVisibility(active||blocked||pending?View.GONE:View.VISIBLE);
    confirm.setEnabled(!busy&&!policy.isEmpty());date.setEnabled(!busy);consent.setEnabled(!busy);refresh.setEnabled(!busy);
    withdraw.setVisibility(active||pending?View.VISIBLE:View.GONE);withdraw.setEnabled(!busy);
  }
  @Override protected boolean usesShakeShortcut(){return false;}
  @Override protected void onResume(){super.onResume();if(status!=null&&!busy)request("status",null);}
  @Override protected void onStop(){client.cancel();busy=false;born=null;if(date!=null)date.setText(R.string.adult_date);if(consent!=null)consent.setChecked(false);super.onStop();}
  @Override protected void onDestroy(){client.close();if(birthPicker!=null)birthPicker.dismiss();if(withdrawalDialog!=null)withdrawalDialog.dismiss();super.onDestroy();}
}
