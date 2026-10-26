package com.zikra.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zikra.app.assistant.ConversationStore;
import com.zikra.app.assistant.AssistantNetwork;
import com.zikra.app.assistant.AssistantAttachments;
import com.zikra.app.assistant.AssistantCameraProvider;
import com.zikra.app.assistant.GeneratedImages;
import com.zikra.app.assistant.ImageGenerationClient;
import com.zikra.app.assistant.OfflineAssistant;
import com.zikra.app.assistant.AssistantSelection;
import com.zikra.app.assistant.TaskProposal;
import com.zikra.app.assistant.OnlineAssistantClient;
import com.zikra.app.data.ZikraDbHelper;
import com.zikra.app.ui.AssistantUi;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Native local companion with a bounded on-device conversation and Android voice input. */
public final class AiActivity extends BaseActivity {
  private static final int VOICE_REQUEST = 4001;
  private static final int MAX_MESSAGES = ConversationStore.MAX_MESSAGES;
  private static final int MAX_STORED_TEXT = 8000;
  private com.zikra.app.assistant.AssistantConnection connection;
  private com.zikra.app.assistant.OnlineAssistantClient onlineClient;
  private boolean waiting;
  private AlertDialog modeDialog;
  private SharedPreferences preferences;
  private EditText input;
  private LinearLayout thread;
  private ScrollView scroll;
  private ConversationStore conversations;
  private AssistantNetwork network;
  private String chatId, routingMode;
  private int visibleMessages = 40;
  private FrameLayout historyOverlay;
  private OnlineAssistantClient.Failure gatewayFailure;
  private AssistantAttachments attachmentReader;
  private AssistantAttachments.Draft attachment;
  private boolean importingAttachment;
  private android.net.Uri cameraUri;
  private String cameraChat;
  private static final int PICK_PHOTO=4101,PICK_DOCUMENT=4102,CAPTURE_PHOTO=4103;
  private JSONArray messages = new JSONArray();
  private AlertDialog clearDialog;
  private AlertDialog voiceDialog;
  private FrameLayout pageRoot;
  private android.view.ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
  private int largestPageHeight;
  private boolean keyboardVisible;
  private AlertDialog selectionDialog, taskDialog, sourceDialog;
  private JSONObject pendingVerse;
  private ImageGenerationClient imageClient;
  private GeneratedImages generatedImages;
  private boolean imageAvailable,imageMode,generatingImage;
  private long imageCapabilityAt;
  private String imageCapabilityEndpoint="";
  private String[] pendingImageExport;
  private static final int EXPORT_IMAGE=4201;


  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_ai);
    setupScreen(getString(R.string.ai_refresh_title));
    findViewById(R.id.toolbarBack).setOnClickListener(v->showHistory());
    findViewById(R.id.aiClear).setOnClickListener(v->newConversation());
    getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    getWindow().setStatusBarColor(getColor(R.color.ai_refresh_surface));
    boolean dark=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
    getWindow().setNavigationBarColor(getColor(R.color.ai_refresh_surface));
    getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    preferences=getSharedPreferences("zikra_preferences",MODE_PRIVATE);
    connection=new com.zikra.app.assistant.AssistantConnection(this);
    routingMode=preferences.getString("assistant_mode_v2",connection.configured()&&!connection.online()?"offline":"auto");
    onlineClient=new OnlineAssistantClient();
    imageClient=new ImageGenerationClient();generatedImages=new GeneratedImages(this);
    conversations=new ConversationStore(this);
    network=new AssistantNetwork(this,()->{
      if(isFinishing()||isDestroyed())return;
      gatewayFailure=null;updateMode();
      refreshImageCapability();
      if(waiting&&!network.validated()){boolean image=generatingImage;cancelImageWork();onlineClient.cancel();if(image)imageFailure("image_network");else showOnlineFailure(OnlineAssistantClient.Failure.NETWORK);}
    });
    pageRoot=findViewById(R.id.aiRoot);input=findViewById(R.id.aiInput);
    input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(OfflineAssistant.MAX_INPUT_LENGTH)});
    thread=findViewById(R.id.aiChatThread);scroll=findViewById(R.id.aiScroll);
    restoreHistory();
    imageMode=preferences.getBoolean("assistant_image_draft:"+chatId,false);
    attachmentReader=new AssistantAttachments(this);restoreAttachment();
    if(state!=null){String uri=state.getString("camera_uri");if(uri!=null){cameraUri=android.net.Uri.parse(uri);cameraChat=state.getString("camera_chat");}}
    if(state!=null&&chatId.equals(state.getString("chat_id",chatId)))input.setText(state.getString("draft",input.getText().toString()));
    findViewById(R.id.aiMode).setOnClickListener(v->chooseMode());
    findViewById(R.id.aiRetry).setOnClickListener(v->{JSONObject last=messages.optJSONObject(messages.length()-1);if(last!=null&&last.optBoolean("image_generation"))showImageConsent();else requestOnline();});
    findViewById(R.id.aiSend).setOnClickListener(v->{if(waiting)cancelOnline();else sendMessage();});
    findViewById(R.id.aiVoice).setOnClickListener(v->beginVoice());
    findViewById(R.id.aiAttach).setOnClickListener(v->showAttachmentPicker());
    findViewById(R.id.aiTools).setOnClickListener(v->showAttachmentPicker());
    findViewById(R.id.aiTopicAttachment).setOnClickListener(v->showAttachmentPicker());
    input.addTextChangedListener(new android.text.TextWatcher(){
      public void beforeTextChanged(CharSequence text,int start,int count,int after){}
      public void onTextChanged(CharSequence text,int start,int before,int count){updateSendAvailability();}
      public void afterTextChanged(android.text.Editable text){}
    });
    findViewById(R.id.aiTopicImage).setOnClickListener(v->enterImageMode());
    findViewById(R.id.aiImageMode).setOnClickListener(v->{if(!waiting){imageMode=false;persistConversation();updateImageMode();}});
    if(state!=null){String c=state.getString("image_export_chat"),i=state.getString("image_export_id"),m=state.getString("image_export_mime");if(c!=null&&i!=null&&m!=null)pendingImageExport=new String[]{c,i,m};}
    configureVoiceAvailability();
    findViewById(R.id.aiChatTile).setOnClickListener(v->focusComposer());
    findViewById(R.id.aiTopicStudy).setOnClickListener(v->composeTopic("Bantu susun rencana belajar untuk hari ini"));
    findViewById(R.id.aiTopicHifz).setOnClickListener(v->composeTopic("Bantu susun target hafalan Al-Quran"));
    renderConversation();
    if(android.os.Build.VERSION.SDK_INT>=30){pageRoot.setOnApplyWindowInsetsListener((view,insets)->{refreshKeyboardLayout(insets);return insets;});pageRoot.requestApplyInsets();}
    keyboardLayoutListener=()->refreshKeyboardLayout(pageRoot.getRootWindowInsets());
    pageRoot.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    int chapter=getIntent().getIntExtra("tadabbur_chapter",0),verse=getIntent().getIntExtra("tadabbur_verse",0);
    if(chapter>=1&&chapter<=114&&verse>=1&&verse<=286){
      imageMode=false;updateImageMode();
      try{pendingVerse=new JSONObject().put("chapter",chapter).put("number",verse);}catch(JSONException ignored){}
      if(state==null)input.setText("Jelaskan makna surah "+chapter+" ayat "+verse+" berdasarkan sumber tafsir.");
    }
  }

  private void refreshImageCapability(){
    if(imageClient==null||network==null)return;
    if(!effectiveOnline()){imageAvailable=false;imageCapabilityAt=0;imageCapabilityEndpoint="";updateImageMode();return;}
    long now=android.os.SystemClock.elapsedRealtime();String endpoint=connection.endpoint();
    if(endpoint.equals(imageCapabilityEndpoint)&&now-imageCapabilityAt<60000)return;
    imageCapabilityAt=now;imageCapabilityEndpoint=endpoint;
    imageClient.capabilities(connection,ready->{if(isDestroyed()||!endpoint.equals(connection.endpoint()))return;imageAvailable=ready&&effectiveOnline();updateImageMode();});
  }
  private void updateImageMode(){
    if(findViewById(R.id.aiTopicImage)==null)return;
    findViewById(R.id.aiTopicImage).setVisibility(View.VISIBLE);
    findViewById(R.id.aiTopicImage).setEnabled(imageAvailable);
    findViewById(R.id.aiTopicImage).setAlpha(imageAvailable?1f:.45f);
    findViewById(R.id.aiTopicHifz).setVisibility(View.GONE);
    findViewById(R.id.aiImageMode).setVisibility(imageMode&&!keyboardVisible?View.VISIBLE:View.GONE);
    if(input!=null)input.setHint(getString(imageMode?R.string.ai_images_input:
        keyboardVisible||getResources().getConfiguration().fontScale>1.3f?R.string.assistant_short_input:R.string.ai_balanced_input));
  }
  private void enterImageMode(){
    if(waiting)return;
    if(!imageAvailable){toast(getString(R.string.ai_images_unavailable));refreshImageCapability();return;}
    if(attachment!=null||importingAttachment){toast(getString(R.string.ai_images_no_attachment));return;}
    imageMode=true;pendingVerse=null;getIntent().removeExtra("tadabbur_chapter");getIntent().removeExtra("tadabbur_verse");
    updateImageMode();persistConversation();focusComposer();
  }
  private void cancelImageWork(){if(imageClient!=null)imageClient.cancel();if(generatedImages!=null)generatedImages.cancel();generatingImage=false;}
  private void sendImageMessage(){
    if(!effectiveOnline()){toast(getString(R.string.ai_images_online));return;}
    if(!imageAvailable){toast(getString(R.string.ai_images_unavailable));return;}
    if(attachment!=null||importingAttachment){toast(getString(R.string.ai_images_no_attachment));return;}
    String prompt=input.getText().toString().trim();if(prompt.length()<3){input.setError(getString(R.string.ai_images_prompt));return;}
    if(messages.length()>MAX_MESSAGES-2||messages.toString().length()+30000>ConversationStore.MAX_CHAT_CHARS){toast(getString(R.string.ai_images_full));return;}
    if(!generatedImages.canAccept()){toast(getString(R.string.ai_images_full));return;}
    addToHistory("user",prompt,"online");try{messages.getJSONObject(messages.length()-1).put("image_generation",true);}catch(JSONException ignored){}
    input.setText("");persistConversation();renderConversation();showImageConsent();
  }
  private void showImageConsent(){
    if(waiting||selectionDialog!=null&&selectionDialog.isShowing())return;
    JSONObject row=messages.optJSONObject(messages.length()-1);if(row==null||!"user".equals(row.optString("role"))||!row.optBoolean("image_generation"))return;
    if(!effectiveOnline()){imageFailure("image_network");return;}
    if(!generatedImages.canAccept()){toast(getString(R.string.ai_images_full));return;}
    String prompt=row.optString("text");TextView review=new TextView(this);review.setText(getString(R.string.ai_images_consent)+"\n\n"+prompt);review.setTextSize(16);review.setPadding(dp(20),dp(12),dp(20),dp(16));ScrollView content=new ScrollView(this);content.addView(review);
    selectionDialog=new AlertDialog.Builder(this).setTitle(R.string.ai_images_review).setView(content).setPositiveButton(R.string.ai_images_start,(dialog,which)->generateImage(prompt)).setNegativeButton(R.string.ai_images_cancel,(dialog,which)->showCancelled()).create();
    selectionDialog.setOnCancelListener(dialog->showCancelled());selectionDialog.show();
  }
  private void generateImage(String prompt){
    if(waiting)return;if(!effectiveOnline()){imageFailure("image_network");return;}
    final String expected=chatId;generatingImage=true;setWaiting(true);findViewById(R.id.aiRetry).setVisibility(View.GONE);
    ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);input.clearFocus();
    ((TextView)findViewById(R.id.aiRequestStatus)).setText(R.string.ai_images_working);
    final String attemptId=java.util.UUID.randomUUID().toString();try{JSONObject current=messages.optJSONObject(messages.length()-1);if(current!=null)current.put("image_request_id",attemptId);}catch(JSONException ignored){}persistConversation();
    try{imageClient.generate(connection,prompt,attemptId,new ImageGenerationClient.Result(){
      public void complete(GeneratedImages.Data data){if(!expected.equals(chatId)||isDestroyed())return;
        ((TextView)findViewById(R.id.aiRequestStatus)).setText(R.string.ai_images_saving);
        generatedImages.save(expected,data,new GeneratedImages.Result(){
          public void saved(String id,String mime){if(!expected.equals(chatId)||isDestroyed())return;generatingImage=false;setWaiting(false);
            addToHistory("assistant",getString(R.string.ai_images_caption),"online");try{messages.getJSONObject(messages.length()-1).put("image_id",id).put("image_mime",mime).put("image_width",data.width).put("image_height",data.height);}catch(JSONException ignored){}
            persistConversation();renderConversation();scroll.post(()->scroll.smoothScrollTo(0,thread.getBottom()));}
          public void failed(){imageFailure("image_storage");}
        });
      }
      public void failed(String error){if(expected.equals(chatId))imageFailure(error);}
    });}catch(Exception unavailable){imageFailure("image_provider_unavailable");}
  }
  private void imageFailure(String error){
    if(isDestroyed()||isFinishing())return;generatingImage=false;setWaiting(false);int id;
    switch(error){case "image_user_quota":id=R.string.ai_images_user_quota;break;case "image_global_quota":case "image_provider_quota":id=R.string.ai_images_global_quota;break;case "image_timeout":id=R.string.ai_images_timeout;break;case "image_network":id=R.string.ai_images_network;break;case "image_storage":id=R.string.ai_images_full;break;default:id=R.string.ai_images_failed;}
    ((TextView)findViewById(R.id.aiRequestStatus)).setText(id);findViewById(R.id.aiRequestStatus).setVisibility(View.VISIBLE);findViewById(R.id.aiRetry).setVisibility(View.VISIBLE);
  }
  private void imageInChat(LinearLayout card,JSONObject item){
    LinearLayout.LayoutParams cardSize=(LinearLayout.LayoutParams)card.getLayoutParams();cardSize.width=LinearLayout.LayoutParams.MATCH_PARENT;card.setLayoutParams(cardSize);
    String id=item.optString("image_id"),mime=item.optString("image_mime"),chat=chatId;
    android.graphics.Bitmap thumbnail=generatedImages.thumbnail(chat,id,mime);
    if(thumbnail==null){TextView missing=new TextView(this);missing.setText(R.string.ai_images_missing);card.addView(missing);return;}
    android.widget.ImageView image=new android.widget.ImageView(this);image.setImageBitmap(thumbnail);image.setAdjustViewBounds(true);image.setContentDescription(getString(R.string.ai_images_caption));image.setFocusable(true);image.setOnClickListener(v->previewGeneratedImage(chat,id,mime));
    LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-1,-2);size.topMargin=dp(12);card.addView(image,size);
    card.addView(historyButton(getString(R.string.ai_images_save),()->saveGeneratedImage(chat,id,mime)));
    card.addView(historyButton(getString(R.string.ai_images_share),()->shareGeneratedImage(chat,id,mime)));
  }
  private void previewGeneratedImage(String chat,String id,String mime){
    android.graphics.Bitmap bitmap=generatedImages.preview(chat,id,mime);if(bitmap==null){toast(getString(R.string.ai_images_missing));return;}
    android.widget.ImageView image=new android.widget.ImageView(this);image.setImageBitmap(bitmap);image.setAdjustViewBounds(true);ScrollView content=new ScrollView(this);content.addView(image);
    sourceDialog=new AlertDialog.Builder(this).setTitle(R.string.ai_images_caption).setView(content).setPositiveButton(R.string.ai_images_close,null).create();sourceDialog.setOnDismissListener(dialog->bitmap.recycle());sourceDialog.show();
  }
  private void saveGeneratedImage(String chat,String id,String mime){
    try{GeneratedImages.imageFile(this,chat,id,mime);pendingImageExport=new String[]{chat,id,mime};
      Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_TITLE,"Zikra-"+id.substring(0,8)+(mime.equals("image/png")?".png":".jpg"));startActivityForResult(save,EXPORT_IMAGE);
    }catch(RuntimeException unavailable){pendingImageExport=null;toast(getString(R.string.ai_images_picker_unavailable));}
  }
  private void shareGeneratedImage(String chat,String id,String mime){
    try{android.net.Uri uri=generatedImages.shareUri(chat,id,mime);Intent share=new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(android.content.ClipData.newRawUri(getString(R.string.ai_images_caption),uri));startActivity(Intent.createChooser(share,getString(R.string.ai_images_share)));}
    catch(RuntimeException unavailable){toast(getString(R.string.ai_images_picker_unavailable));}
  }


  private void configureVoiceAvailability() {
    boolean available=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(getPackageManager())!=null;
    findViewById(R.id.aiVoice).setVisibility(available?View.VISIBLE:View.GONE);
  }



  private void refreshKeyboardLayout(android.view.WindowInsets insets) {
    largestPageHeight = Math.max(largestPageHeight, pageRoot.getHeight());
    boolean resized = largestPageHeight - pageRoot.getHeight() > dp(150);
    boolean imeVisible =
        android.os.Build.VERSION.SDK_INT >= 30
            && insets != null
            && insets.isVisible(android.view.WindowInsets.Type.ime());
    // Legacy fitting windows can consume IME insets; the resized viewport is the fallback.
    updateKeyboardLayout(imeVisible || resized);
  }

  private void updateKeyboardLayout(boolean visible) {
    if (keyboardVisible == visible) return;
    keyboardVisible = visible;
    findViewById(R.id.aiWelcome).setVisibility(!visible&&messages.length()==0?View.VISIBLE:View.GONE);
    findViewById(R.id.aiVoiceStatus).setVisibility(visible?View.GONE:View.VISIBLE);
    input.setMaxLines(visible?3:4);
    updateMode();
    updateImageMode();
    if(attachment!=null)renderAttachment();
    if (visible) scroll.post(() -> scroll.smoothScrollTo(0, thread.getBottom()));
  }


  private void restoreHistory() {
    ConversationStore.Chat chat=conversations.get(preferences.getString("assistant_active_chat",""));
    if(chat==null){java.util.List<ConversationStore.Chat> all=conversations.list("");chat=all.isEmpty()?conversations.create():conversations.get(all.get(0).id);}
    chatId=chat.id;messages=chat.messages;input.setText(chat.draft);
    preferences.edit().putString("assistant_active_chat",chatId).apply();
  }

  private void persistConversation() {
    if(conversations==null||chatId==null||input==null)return;
    preferences.edit().putBoolean("assistant_image_draft:"+chatId,imageMode).apply();
    try{conversations.save(chatId,messages,input.getText().toString());}
    catch(RuntimeException error){toast("Percakapan belum tersimpan. Periksa ruang perangkat sebelum keluar.");}
  }

  private boolean effectiveOnline() {
    return !"offline".equals(routingMode)&&connection.configured()&&network.validated();
  }

  private void restoreAttachment() {
    if(attachmentReader==null)return;attachment=null;importingAttachment=true;renderAttachment();String expected=chatId;
    attachmentReader.restore(chatId,new AssistantAttachments.Result(){
      public void loaded(AssistantAttachments.Draft draft){if(!expected.equals(chatId)||isDestroyed())return;importingAttachment=false;attachment=draft;renderAttachment();}
      public void failed(String message){if(!expected.equals(chatId)||isDestroyed())return;importingAttachment=false;attachment=null;renderAttachment();toast(message);}
    });
  }

  private void renderAttachment() {
    LinearLayout holder=findViewById(R.id.aiAttachmentDraft);holder.removeAllViews();
    holder.setVisibility(attachment!=null||importingAttachment?View.VISIBLE:View.GONE);
    updateSendAvailability();
    if(importingAttachment){TextView progress=new TextView(this);progress.setText("Menyiapkan lampiran di perangkat…");progress.setTextSize(13);progress.setPadding(dp(12),dp(10),dp(12),dp(10));holder.addView(progress);return;}
    if(attachment==null)return;
    LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(4),0,dp(4));row.setBackground(AssistantUi.surface(this,R.color.ai_refresh_surface,16,false));
    android.graphics.Bitmap thumb=attachment.thumbnail();if(thumb!=null){android.widget.ImageView photo=new android.widget.ImageView(this);photo.setImageBitmap(thumb);photo.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);photo.setContentDescription("Preview lampiran "+attachment.name);row.addView(photo,new LinearLayout.LayoutParams(dp(44),dp(44)));}
    TextView label=new TextView(this);label.setText(attachment.summary()+"\nBelum dikirim");label.setTextSize(12);label.setMaxLines(keyboardVisible?1:3);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setPadding(dp(10),0,dp(6),0);label.setTextColor(getColor(R.color.ai_refresh_ink));
    label.setMinHeight(dp(48));label.setGravity(Gravity.CENTER_VERTICAL);label.setFocusable(true);label.setClickable(true);label.setContentDescription("Lihat lampiran "+attachment.name);label.setId(R.id.aiAttachmentPreview);label.setOnClickListener(v->previewAttachment(attachment));
    row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
    android.widget.ImageButton remove=AssistantUi.icon(this,R.drawable.reading_ic_close,"Hapus lampiran "+attachment.name,()->{attachmentReader.clearDraft(chatId,true);attachment=null;renderAttachment();});remove.setEnabled(!waiting);
    row.addView(remove,new LinearLayout.LayoutParams(dp(48),dp(48)));holder.addView(row);
  }

  private void previewAttachment(AssistantAttachments.Draft draft){
    if(draft==null)return;
    LinearLayout pages=new LinearLayout(this);pages.setOrientation(LinearLayout.VERTICAL);pages.setPadding(dp(16),dp(8),dp(16),dp(16));
    java.util.ArrayList<android.graphics.Bitmap> bitmaps=new java.util.ArrayList<>();
    if(draft.imageCount()>0)for(int i=0;i<draft.imageCount();i++){
      TextView number=new TextView(this);number.setText(draft.kind.equals("pdf")?"Halaman "+(i+1):"Foto yang akan dikirim");number.setPadding(0,dp(8),0,dp(8));pages.addView(number);
      android.graphics.Bitmap bitmap=draft.preview(i);if(bitmap==null)continue;bitmaps.add(bitmap);
      android.widget.ImageView image=new android.widget.ImageView(this);image.setImageBitmap(bitmap);image.setAdjustViewBounds(true);image.setContentDescription(draft.name+" halaman "+(i+1));pages.addView(image,new LinearLayout.LayoutParams(-1,-2));
    }else{TextView text=new TextView(this);text.setText(draft.text);text.setTextSize(16);text.setTextColor(getColor(R.color.ai_refresh_ink));text.setTextIsSelectable(true);pages.addView(text);}
    TextView note=new TextView(this);note.setText("Periksa keterbacaan sebelum mengirim. Foto dan PDF diperkecil; tabel, tulisan kecil dan hasil scan bisa keliru dibaca AI.");note.setPadding(0,dp(16),0,0);pages.addView(note);
    ScrollView content=new ScrollView(this);content.addView(pages);
    sourceDialog=new AlertDialog.Builder(this).setTitle(draft.name).setView(content).setPositiveButton("Tutup",null).create();
    sourceDialog.setOnDismissListener(dialog->{for(android.graphics.Bitmap bitmap:bitmaps)bitmap.recycle();});sourceDialog.show();
  }

  private void showAttachmentPicker() {
    if(waiting||importingAttachment){toast("Tunggu proses selesai atau batalkan permintaan dahulu.");return;}
    clearDialog=AssistantUi.attachments(this,imageAvailable,this::pickAttachment,this::enterImageMode);
    AssistantUi.bottomSheet(clearDialog,this);
  }

  private void pickAttachment(int action) {
    imageMode=false;updateImageMode();persistConversation();
    try{
      if(action==0){
        Intent camera=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if(camera.resolveActivity(getPackageManager())==null){toast("Aplikasi kamera belum tersedia. Pilih Galeri.");return;}
        cameraUri=AssistantCameraProvider.create(this);cameraChat=chatId;
        camera.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,cameraUri);camera.setClipData(android.content.ClipData.newRawUri("Foto Zikra",cameraUri));
        camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);startActivityForResult(camera,CAPTURE_PHOTO);
      }else{
        Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(action==1?"image/*":"*/*");
        picker.putExtra(Intent.EXTRA_MIME_TYPES,action==1?new String[]{"image/jpeg","image/png","image/webp"}:new String[]{"application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","text/plain"});
        startActivityForResult(picker,action==1?PICK_PHOTO:PICK_DOCUMENT);
      }
    }catch(ActivityNotFoundException|SecurityException unavailable){if(cameraUri!=null){AssistantCameraProvider.discard(this,cameraUri);cameraUri=null;}toast("Pemilih berkas belum tersedia di perangkat ini.");}
  }

  private void importAttachment(android.net.Uri uri) {
    if(uri==null)return;String expected=chatId;importingAttachment=true;renderAttachment();
    attachmentReader.load(chatId,uri,new AssistantAttachments.Result(){
      public void loaded(AssistantAttachments.Draft draft){if(!expected.equals(chatId)||isDestroyed())return;attachment=draft;importingAttachment=false;renderAttachment();}
      public void failed(String message){if(!expected.equals(chatId)||isDestroyed())return;importingAttachment=false;renderAttachment();toast(message);}
    });
  }

  private TextView historyButton(String label,Runnable action) {
    TextView button=new TextView(this);button.setText(label);button.setTextSize(14);
    button.setTextColor(getColor(R.color.ai_refresh_ink));button.setMinHeight(dp(48));
    button.setGravity(Gravity.CENTER_VERTICAL);button.setPadding(dp(12),dp(8),dp(12),dp(8));
    button.setClickable(true);button.setFocusable(true);button.setOnClickListener(v->action.run());return button;
  }

  private void closeHistory() {
    if(historyOverlay!=null){pageRoot.removeView(historyOverlay);historyOverlay=null;}
    findViewById(R.id.aiPage).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
  }

  private void selectConversation(String id) {
    persistConversation();onlineClient.cancel();cancelImageWork();setWaiting(false);
    if(selectionDialog!=null)selectionDialog.dismiss();
    ConversationStore.Chat chat=conversations.get(id);if(chat==null)return;
    chatId=chat.id;messages=chat.messages;input.setText(chat.draft);visibleMessages=40;
    imageMode=preferences.getBoolean("assistant_image_draft:"+chatId,false);updateImageMode();
    pendingVerse=null;gatewayFailure=null;preferences.edit().putString("assistant_active_chat",chatId).apply();
    getIntent().removeExtra("tadabbur_chapter");getIntent().removeExtra("tadabbur_verse");
    findViewById(R.id.aiRetry).setVisibility(View.GONE);closeHistory();renderConversation();
    restoreAttachment();
    scroll.post(()->scroll.scrollTo(0,0));
  }

  private void newConversation() {
    if(messages.length()==0&&input.length()==0&&attachment==null){imageMode=false;updateImageMode();persistConversation();closeHistory();return;}
    persistConversation();
    try{ConversationStore.Chat chat=conversations.create();selectConversation(chat.id);}
    catch(IllegalStateException limit){toast(limit.getMessage());}
  }

  private void showHistory() {
    if(historyOverlay!=null)return;persistConversation();
    ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);
    input.clearFocus();
    FrameLayout overlay=new FrameLayout(this);historyOverlay=overlay;overlay.setId(R.id.aiHistoryOverlay);
    overlay.setBackgroundColor(0x66000000);overlay.setOnClickListener(v->closeHistory());
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(14),dp(12),dp(14),dp(20));panel.setBackground(AssistantUi.surface(this,R.color.ai_refresh_surface,24,false));panel.setClickable(true);
    FrameLayout.LayoutParams panelSize=new FrameLayout.LayoutParams(Math.min(dp(360),getResources().getDisplayMetrics().widthPixels*9/10),-1,Gravity.START);
    overlay.addView(panel,panelSize);
    LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
    TextView title=new TextView(this);title.setText(R.string.ai_refresh_title);title.setTextSize(20);title.setTextColor(getColor(R.color.ai_refresh_ink));title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setPadding(dp(10),0,0,0);if(android.os.Build.VERSION.SDK_INT>=28)title.setAccessibilityHeading(true);
    heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));heading.addView(AssistantUi.icon(this,R.drawable.reading_ic_close,getString(R.string.ai_balanced_close),this::closeHistory));panel.addView(heading);
    EditText search=new EditText(this);search.setId(R.id.aiHistorySearch);search.setHint(R.string.ai_refresh_search);search.setSingleLine(true);search.setTextSize(16);search.setTextColor(getColor(R.color.ai_refresh_ink));search.setHintTextColor(getColor(R.color.ai_refresh_muted));search.setPadding(dp(12),dp(10),dp(12),dp(10));search.setBackground(AssistantUi.surface(this,R.color.ai_refresh_paper,15,false));search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.reading_ic_search,0,0,0);search.setCompoundDrawablePadding(dp(8));search.setMinHeight(dp(48));search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    LinearLayout.LayoutParams searchSize=new LinearLayout.LayoutParams(-1,-2);searchSize.topMargin=dp(12);searchSize.bottomMargin=dp(12);panel.addView(search,searchSize);
    TextView newChat=historyButton(getString(R.string.ai_balanced_new_chat),this::newConversation);newChat.setBackground(AssistantUi.surface(this,R.color.ai_refresh_paper,14,true));newChat.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ai_refresh_plus,0,0,0);newChat.setCompoundDrawablePadding(dp(10));panel.addView(newChat,new LinearLayout.LayoutParams(-1,-2));
    ScrollView listScroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setId(R.id.aiHistoryList);list.setOrientation(LinearLayout.VERTICAL);listScroll.addView(list);
    panel.addView(listScroll,new LinearLayout.LayoutParams(-1,0,1));
    panel.addView(historyButton(getString(R.string.ai_balanced_settings),()->{closeHistory();chooseMode();}));
    TextView local=new TextView(this);local.setText(R.string.ai_balanced_local_storage);local.setTextSize(12);local.setTextColor(getColor(R.color.ai_refresh_muted));local.setPadding(dp(12),dp(8),0,0);panel.addView(local);
    Runnable update=()->{
      list.removeAllViews();java.time.LocalDate today=java.time.LocalDate.now();String group="";
      for(ConversationStore.Chat chat:conversations.list(search.getText().toString())) {
        java.time.LocalDate day=java.time.Instant.ofEpochMilli(chat.updated).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        String next=day.equals(today)?"Hari ini":day.equals(today.minusDays(1))?"Kemarin":day.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy",java.util.Locale.forLanguageTag("id-ID")));
        if(!next.equals(group)){TextView date=new TextView(this);date.setText(next);date.setTextSize(12);date.setTextColor(getColor(R.color.ai_refresh_muted));date.setPadding(dp(12),dp(18),0,dp(6));list.addView(date);group=next;}
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        if(chat.id.equals(chatId)){GradientDrawable bg=new GradientDrawable();bg.setColor(getColor(R.color.ai_refresh_mint));bg.setCornerRadius(dp(12));row.setBackground(bg);}
        TextView item=historyButton(chat.title,()->selectConversation(chat.id));item.setMaxLines(1);item.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        TextView more=historyButton("⋮",()->showChatActions(chat.id,chat.title));more.setContentDescription("Pilihan percakapan "+chat.title);more.setGravity(Gravity.CENTER);
        row.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));list.addView(row,new LinearLayout.LayoutParams(-1,-2));
      }
      if(list.getChildCount()==0){TextView empty=new TextView(this);empty.setText("Belum ada percakapan yang cocok.");empty.setTextSize(14);empty.setPadding(dp(12),dp(20),dp(12),0);list.addView(empty);}
    };
    search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence text,int start,int count,int after){}public void onTextChanged(CharSequence text,int start,int before,int count){update.run();}public void afterTextChanged(android.text.Editable text){}});
    findViewById(R.id.aiPage).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    panel.setFocusableInTouchMode(true);pageRoot.addView(overlay,new FrameLayout.LayoutParams(-1,-1));panel.requestFocus();update.run();
  }

  private void showChatActions(String id,String title) {
    clearDialog=new AlertDialog.Builder(this).setTitle(title).setItems(new String[]{"Ubah judul","Hapus percakapan"},(dialog,index)->{
      if(index==0){EditText field=new EditText(this);field.setText(title);field.setSelectAllOnFocus(true);field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        clearDialog=new AlertDialog.Builder(this).setTitle("Ubah judul").setView(field).setPositiveButton("Simpan",(d,w)->{try{conversations.rename(id,field.getText().toString());closeHistory();showHistory();}catch(IllegalArgumentException invalid){toast("Judul tidak boleh kosong.");}}).setNegativeButton("Batal",null).show();
      }else{clearDialog=new AlertDialog.Builder(this).setTitle("Hapus percakapan ini?").setMessage("Hanya percakapan ini yang dihapus dari perangkat.").setPositiveButton("Hapus",(d,w)->{
        boolean current=id.equals(chatId);if(current){onlineClient.cancel();cancelImageWork();setWaiting(false);}
        conversations.delete(id);attachmentReader.deleteConversation(id);generatedImages.deleteChat(id);preferences.edit().remove("assistant_image_draft:"+id).apply();closeHistory();
        if(current){if(selectionDialog!=null)selectionDialog.dismiss();chatId=null;pendingVerse=null;gatewayFailure=null;visibleMessages=40;
          getIntent().removeExtra("tadabbur_chapter");getIntent().removeExtra("tadabbur_verse");findViewById(R.id.aiRetry).setVisibility(View.GONE);
          restoreHistory();imageMode=preferences.getBoolean("assistant_image_draft:"+chatId,false);renderConversation();restoreAttachment();}showHistory();
      }).setNegativeButton("Batal",null).show();}
    }).show();
  }

  @Override public void onBackPressed() {if(historyOverlay!=null){closeHistory();return;}super.onBackPressed();}

  private void addToHistory(String role, String text) {
    addToHistory(role, text, connection != null && effectiveOnline() ? "online" : "offline");
  }

  private void addToHistory(String role, String text, String mode) {
    try {
      JSONObject item = new JSONObject();
      item.put("role", role);
      item.put("text", text);
      item.put("mode", mode);
      if ("user".equals(role)) item.put("request_id", java.util.UUID.randomUUID().toString());
      messages.put(item);
      ConversationStore.validate(messages);
    } catch (JSONException ignored) {
      /* All fields are validated strings. */
    }
  }


  private void renderConversation() {
    thread.removeAllViews();
    findViewById(R.id.aiWelcome).setVisibility(!keyboardVisible&&messages.length()==0?View.VISIBLE:View.GONE);
    int first=Math.max(0,messages.length()-visibleMessages);
    if(first>0){TextView earlier=historyButton("Lihat pesan sebelumnya",()->{visibleMessages+=40;renderConversation();});thread.addView(earlier);}
    for(int i=first;i<messages.length();i++){
      JSONObject item=messages.optJSONObject(i);if(item==null)continue;
      bubble(item.optString("role"),item.optString("text"),item.optString("mode","offline"));
      if("user".equals(item.optString("role"))&&!item.optString("attachment_name").isEmpty()){
        LinearLayout card=(LinearLayout)((LinearLayout)thread.getChildAt(thread.getChildCount()-1)).getChildAt(0);
        TextView file=new TextView(this);file.setText("Lampiran: "+item.optString("attachment_name"));file.setTextSize(12);file.setPadding(0,dp(8),0,0);file.setTextColor(getColor(R.color.ai_refresh_muted));card.addView(file);
      }
      if("assistant".equals(item.optString("role")))appendAssistantExtras(item);
    }
    updateMode();
    updateImageMode();
  }

  private void bubble(String role, String message) {
    bubble(role, message, "offline");
  }

  private void bubble(String role, String message, String mode) {
    boolean user = "user".equals(role);
    LinearLayout row = new LinearLayout(this);
    row.setGravity(user ? Gravity.END : Gravity.START);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
    rowParams.bottomMargin = dp(24);
    row.setLayoutParams(rowParams);
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(user?dp(15):0, user?dp(12):0, user?dp(15):0, user?dp(12):0);
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(user?-2:-1, -2);
    if (user) cardParams.leftMargin = dp(32);
    card.setLayoutParams(cardParams);
    GradientDrawable background = new GradientDrawable();
    background.setColor(getColor(user ? R.color.ai_refresh_mint : R.color.ai_refresh_paper));
    background.setCornerRadii(
        new float[] {
          dp(19),
          dp(19),
          dp(19),
          dp(19),
          user ? dp(5) : dp(19),
          user ? dp(5) : dp(19),
          user ? dp(19) : dp(5),
          user ? dp(19) : dp(5)
        });
    if(user)card.setBackground(background);
    if (!user) {
      TextView label = new TextView(this);
      label.setText(
          "online".equals(mode)
              ? R.string.concept_ai_cloud_label
              : R.string.concept_ai_local_label);
      label.setTextSize(12);
      label.setTextColor(getColor(R.color.ai_refresh_accent));
      label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
      label.setPadding(0, 0, 0, dp(7));
      card.addView(label);
    }
    TextView body = new TextView(this);
    body.setText(message);
    body.setTextSize(16);
    body.setLineSpacing(dp(4), 1f);
    body.setTextColor(getColor(R.color.ai_refresh_ink));
    body.setTextIsSelectable(true);
    card.addView(body);
    if(!user)card.addView(AssistantUi.icon(this,R.drawable.ai_balanced_copy,getString(R.string.ai_balanced_copy),()->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("Zikra AI",message));toast("Teks disalin");}));
    row.addView(card);
    thread.addView(row);
  }

  private void sendMessage() {
    if (waiting) return;
    if(imageMode){sendImageMessage();return;}
    if(importingAttachment){toast("Lampiran masih disiapkan. Belum ada berkas yang dikirim.");return;}
    if(attachment!=null&&!effectiveOnline()){toast("Foto dan dokumen memerlukan internet serta layanan online. Lampiran tetap draf.");return;}
    findViewById(R.id.aiRequestStatus).setVisibility(View.GONE);
    findViewById(R.id.aiRetry).setVisibility(View.GONE);
    String message = input.getText().toString().trim();
    if(message.isEmpty()&&attachment!=null)message="Bantu pahami lampiran ini.";
    if (message.isEmpty()) {
      input.setError("Tulis pesanmu dahulu");
      input.requestFocus();
      return;
    }
    if (message.length() > OfflineAssistant.MAX_INPUT_LENGTH) {
      input.setError("Maksimal 500 karakter");
      return;
    }
    if(messages.length()>MAX_MESSAGES-2||messages.toString().length()+30000>ConversationStore.MAX_CHAT_CHARS){toast("Percakapan sudah panjang. Buat chat baru; riwayat ini tetap tersimpan.");return;}
    if("online".equals(routingMode)&&!effectiveOnline()){toast(connection.configured()?"Internet belum tersedia. Pesan tetap draf.":"Hubungkan layanan melalui label mode terlebih dahulu.");return;}
    if (pendingVerse != null && !effectiveOnline()) {
      toast("Tadabbur bersumber memerlukan mode online. Aktifkan melalui label mode.");
      return;
    }
    if (effectiveOnline()) {
      addToHistory("user", message);
      if(attachment!=null)try{messages.getJSONObject(messages.length()-1).put("attachment_id",attachment.id).put("attachment_name",attachment.name);}catch(JSONException ignored){}
      input.setText("");
      persistConversation();
      renderConversation();
      requestOnline();
      return;
    }
    int completed;
    int total;
    try (ZikraDbHelper database = new ZikraDbHelper(this)) {
      ZikraDbHelper.Progress progress = database.getProgress(ZikraDbHelper.CATEGORY_STUDY);
      completed = progress.completed;
      total = progress.total;
    }
    String reply =
        OfflineAssistant.reply(
            message, completed, total, preferences.getString("last_surah", "Al-Fatihah"));
    addToHistory("user", message);
    addToHistory("assistant", reply);
    input.setText("");
    persistConversation();
    renderConversation();
    scroll.post(
        () -> {
          if (!isFinishing()) scroll.smoothScrollTo(0, thread.getBottom());
        });
  }


  private void updateMode() {
    if(connection==null||network==null)return;
    boolean online=effectiveOnline();String prefix="auto".equals(routingMode)?"Otomatis · ":"";
    String status=online?"Online":"Offline";
    if(gatewayFailure!=null&&network.validated()&&online)status="Layanan bermasalah";
    ((TextView)findViewById(R.id.aiMode)).setText(prefix+status+" ⌄");
    int note;
    switch(network.state()){
      case CAPTIVE_PORTAL: note=R.string.assistant_network_login;break;
      case UNVALIDATED: note=R.string.assistant_network_unvalidated;break;
      case OFFLINE: note=R.string.assistant_network_offline;break;
      default: note=!connection.configured()?R.string.assistant_gateway_unconfigured:gatewayFailure!=null?R.string.assistant_gateway_retry:R.string.assistant_network_available;
    }
    ((TextView)findViewById(R.id.aiNetworkNote)).setText(note);
    findViewById(R.id.aiNetworkNote).setVisibility(!keyboardVisible&&gatewayFailure!=null?View.VISIBLE:View.GONE);
    findViewById(R.id.aiOfflineNotice).setVisibility(!online&&!keyboardVisible?View.VISIBLE:View.GONE);
  }


  private void chooseMode() {
    modeDialog=new AlertDialog.Builder(this).setTitle("Mode Zikra AI")
      .setItems(new String[]{"Otomatis","Online","Offline · panduan lokal","Pengaturan koneksi"},(dialog,index)->{
        if(index==3){configureGateway();return;}
        boolean pending=waiting;
        onlineClient.cancel();cancelImageWork();setWaiting(false);gatewayFailure=null;
        if(pending)showCancelled();
        routingMode=new String[]{"auto","online","offline"}[index];
        preferences.edit().putString("assistant_mode_v2",routingMode).apply();
        if(index==2)connection.offline();else if(connection.configured())connection.activate();else configureGateway();
        updateMode();imageCapabilityAt=0;refreshImageCapability();
      }).show();
  }

  private void configureGateway() {
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(dp(24), dp(8), dp(24), dp(8));
    TextView note = new TextView(this);
    note.setText(R.string.concept_ai_backend_note);
    note.setTextSize(14);
    form.addView(note);
    EditText endpoint = new EditText(this);
    endpoint.setSingleLine(true);
    endpoint.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    endpoint.setHint(R.string.concept_ai_endpoint);
    endpoint.setText(connection.endpoint());
    form.addView(endpoint);
    EditText access = new EditText(this);
    access.setSingleLine(true);
    access.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    access.setHint(R.string.concept_ai_access);
    form.addView(access);
    modeDialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.concept_ai_backend)
            .setView(form)
            .setPositiveButton(R.string.concept_ai_connect, null)
            .setNegativeButton(R.string.concept_ai_cancel, null)
            .create();
    modeDialog.setOnShowListener(
        d ->
            modeDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        connection.save(
                            endpoint.getText().toString().trim(),
                            access.getText().toString().trim());
                        boolean pending=waiting;
                        onlineClient.cancel();cancelImageWork();setWaiting(false);
                        if(selectionDialog!=null&&selectionDialog.isShowing())selectionDialog.dismiss();
                        if(pending)showCancelled();
                        if("offline".equals(routingMode))routingMode="auto";
                        preferences.edit().putString("assistant_mode_v2",routingMode).apply();gatewayFailure=null;
                        updateMode();
                        imageCapabilityAt=0;refreshImageCapability();
                        modeDialog.dismiss();
                      } catch (Exception invalid) {
                        endpoint.setError(getString(R.string.concept_ai_backend_invalid));
                      }
                    }));
    modeDialog.show();
  }

  private void setWaiting(boolean value) {
    waiting = value;
    android.widget.ImageButton send = findViewById(R.id.aiSend);
    send.setImageResource(value ? android.R.drawable.ic_menu_close_clear_cancel : R.drawable.ai_send);
    send.setContentDescription(value ? getString(R.string.assistant_cancel_send) : "Kirim pesan");
    TextView status = findViewById(R.id.aiRequestStatus);
    status.setText(value ? R.string.concept_ai_pending : R.string.concept_ai_failed);
    status.setVisibility(value ? View.VISIBLE : View.GONE);
    findViewById(R.id.aiAttach).setEnabled(!value);findViewById(R.id.aiTools).setEnabled(!value);renderAttachment();
  }

  private void updateSendAvailability() {
    if(input==null)return;
    boolean available=waiting||!importingAttachment&&(input.length()>0||attachment!=null);
    View send=findViewById(R.id.aiSend);send.setEnabled(available);send.setAlpha(available?1f:.35f);
  }

  private void requestOnline() {
    if (waiting
        || !effectiveOnline()
        || messages.length() == 0
        || !"user".equals(messages.optJSONObject(messages.length() - 1).optString("role"))) return;
    showSelection();
  }

  private void showSelection() {
    if (selectionDialog != null && selectionDialog.isShowing()) return;
    ScrollView container = new ScrollView(this);
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), dp(16));
    container.addView(form);
    TextView note = new TextView(this); note.setText(R.string.assistant_send_note); form.addView(note);
    int[] labels = { R.string.assistant_select_name, R.string.assistant_select_tasks,
      R.string.assistant_select_prayer, R.string.assistant_select_history, R.string.assistant_select_proposals };
    android.widget.CheckBox[] choices = new android.widget.CheckBox[labels.length];
    for (int i = 0; i < labels.length; i++) {
      choices[i] = new android.widget.CheckBox(this); choices[i].setText(labels[i]);
      choices[i].setMinHeight(dp(48)); form.addView(choices[i]);
    }
    if (pendingVerse != null) {
      TextView sourceNote = new TextView(this); sourceNote.setText(R.string.assistant_tafsir_note); form.addView(sourceNote);
    }
    TextView preview = new TextView(this); preview.setTextIsSelectable(true); preview.setPadding(0, dp(12), 0, 0); form.addView(preview);
    final JSONObject[] snapshot = new JSONObject[1];
    Runnable update = () -> {
      try {
        snapshot[0] = AssistantSelection.build(this, messages, choices[0].isChecked(), choices[1].isChecked(),
            choices[2].isChecked(), choices[3].isChecked(), choices[4].isChecked(), pendingVerse);
        if(attachment!=null)snapshot[0].put("attachments",new JSONArray().put(attachment.payload()));
        if(snapshot[0].toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>2*1024*1024)throw new JSONException("Attachment budget");
        preview.setText(AssistantSelection.preview(snapshot[0])+(attachment==null?"":"\n\nLampiran yang akan dikirim: "+attachment.summary()+"\nFoto/PDF disiapkan sebagai gambar; DOCX/TXT sebagai teks. Data dikirim melalui layanan Zikra ke model pembaca."));
      } catch (Exception unavailable) {
        snapshot[0] = null; preview.setText("Data pilihan belum dapat dibaca. Batalkan dan coba lagi.");
      }
    };
    for (android.widget.CheckBox choice : choices) choice.setOnCheckedChangeListener((button, checked) -> update.run());
    update.run();
    selectionDialog = new AlertDialog.Builder(this).setTitle(R.string.assistant_review_send).setView(container)
        .setPositiveButton(R.string.assistant_send_selected, null)
        .setNegativeButton("Batal", (dialog, which) -> showCancelled()).create();
    selectionDialog.setOnCancelListener(dialog -> showCancelled());
    selectionDialog.setOnShowListener(dialog -> selectionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
      if (snapshot[0] == null) return;
      JSONObject selected = snapshot[0]; selectionDialog.dismiss(); sendOnline(selected);
    }));
    selectionDialog.show();
  }

  private void sendOnline(JSONObject request) {
    final String requestChat=chatId;gatewayFailure=null;updateMode();
    if (waiting) return;
    if(!effectiveOnline()){showOnlineFailure(OnlineAssistantClient.Failure.NETWORK);return;}
    setWaiting(true);
    findViewById(R.id.aiRetry).setVisibility(View.GONE);
    try {
      onlineClient.send(
          connection,
          request,
          new com.zikra.app.assistant.OnlineAssistantClient.Result() {
            @Override
            public void complete(String reply) {
              complete(reply, null, null);
            }

            @Override
            public void complete(String reply, JSONObject proposal, JSONObject source) {
              if (isFinishing() || isDestroyed() || !requestChat.equals(chatId)) return;
              setWaiting(false);
              addToHistory("assistant", reply, "online");
              JSONObject row = messages.optJSONObject(messages.length() - 1);
              try {
                if (proposal != null) row.put("proposal", proposal);
                if (source != null) row.put("source", source);
              } catch (JSONException ignored) { }
              pendingVerse = null;
              if(request.has("attachments")){attachmentReader.clearDraft(chatId,false);attachment=null;renderAttachment();}
              getIntent().removeExtra("tadabbur_chapter"); getIntent().removeExtra("tadabbur_verse");
              persistConversation();
              renderConversation();
              scroll.post(() -> scroll.smoothScrollTo(0, thread.getBottom()));
            }

            @Override
            public void failed() {
              showOnlineFailure();
            }

            @Override public void failed(OnlineAssistantClient.Failure failure) { showOnlineFailure(failure); }
          });
    } catch (Exception unavailable) {
      showOnlineFailure();
    }
  }

  private void showOnlineFailure() {
    showOnlineFailure(OnlineAssistantClient.Failure.SERVER);
  }

  private void showOnlineFailure(OnlineAssistantClient.Failure failure) {
    if (isFinishing() || isDestroyed()) return;
    setWaiting(false);
    gatewayFailure=failure;updateMode();
    int message;
    switch (failure) {
      case UNAUTHORIZED: message = R.string.assistant_auth_failed; break;
      case QUOTA: message = R.string.assistant_quota_failed; break;
      case TIMEOUT: message = R.string.assistant_timeout_failed; break;
      case NETWORK: message = R.string.assistant_network_failed; break;
      case SOURCE: message = R.string.assistant_source_failed; break;
      case ATTACHMENT: message = R.string.ai_refresh_attachment_failed; break;
      case INVALID_RESPONSE: message = R.string.assistant_response_failed; break;
      default: message = R.string.assistant_server_failed;
    }
    ((TextView) findViewById(R.id.aiRequestStatus)).setText(message);
    findViewById(R.id.aiRequestStatus).setVisibility(View.VISIBLE);
    findViewById(R.id.aiRetry).setVisibility(View.VISIBLE);
    revealRequestStatus();
  }

  private void cancelOnline() {
    onlineClient.cancel();cancelImageWork();setWaiting(false);showCancelled();
  }

  private void showCancelled() {
    TextView status = findViewById(R.id.aiRequestStatus);
    status.setText(R.string.assistant_cancelled); status.setVisibility(View.VISIBLE);
    findViewById(R.id.aiRetry).setVisibility(View.VISIBLE);
    revealRequestStatus();
  }

  private void revealRequestStatus() {
    scroll.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener(){
      @Override public void onGlobalLayout(){
        if(scroll.getViewTreeObserver().isAlive())scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if(!isFinishing()&&!isDestroyed())scroll.smoothScrollTo(0,scroll.getChildAt(0).getBottom());
      }
    });
    scroll.requestLayout();
  }

  private void appendAssistantExtras(JSONObject item) {
    LinearLayout card = (LinearLayout) ((LinearLayout) thread.getChildAt(thread.getChildCount() - 1)).getChildAt(0);
    if(item.has("image_id"))imageInChat(card,item);
    JSONObject proposal = item.optJSONObject("proposal");
    if (proposal != null) {
      boolean applied;
      try (ZikraDbHelper db = new ZikraDbHelper(this)) { applied = db.assistantProposalApplied(proposal.optString("id")); }
      TextView review = new TextView(this);
      review.setText(applied ? R.string.assistant_task_saved : R.string.assistant_review_task);
      review.setTextColor(getColor(R.color.pine)); review.setMinHeight(dp(48)); review.setGravity(Gravity.CENTER_VERTICAL);
      if (!applied) { review.setOnClickListener(view -> reviewTask(proposal)); review.setFocusable(true); }
      card.addView(review);
    }
    JSONObject source = item.optJSONObject("source");
    if (source != null) {
      TextView note = new TextView(this); note.setText(R.string.assistant_tafsir_note);
      note.setTextSize(12); note.setPadding(0, dp(8), 0, dp(4)); card.addView(note);
      TextView openSource = new TextView(this); openSource.setText(R.string.assistant_tafsir_original);
      openSource.setTextColor(getColor(R.color.pine)); openSource.setMinHeight(dp(48)); openSource.setGravity(Gravity.CENTER_VERTICAL);
      openSource.setFocusable(true); openSource.setOnClickListener(view -> showSource(source)); card.addView(openSource);
    }
  }

  private void reviewTask(JSONObject value) {
    final TaskProposal proposal;
    try { proposal = TaskProposal.fromJson(value); }
    catch (Exception invalid) { toast("Usulan tugas tidak valid."); return; }
    LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(24), dp(8), dp(24), dp(12));
    TextView note = new TextView(this); note.setText(getString(R.string.assistant_task_unsaved)
        + ("edit_task".equals(proposal.kind) ? "\n\nMengubah: " + proposal.expectedTitle : "")); form.addView(note);
    EditText title = new EditText(this); title.setHint(R.string.assistant_task_title); title.setText(proposal.title);
    title.setFilters(new InputFilter[] {new InputFilter.LengthFilter(200)}); form.addView(title);
    long[] due = {proposal.dueAt};
    TextView dueText = new TextView(this); dueText.setPadding(0, dp(12), 0, dp(8)); form.addView(dueText);
    Runnable renderDue = () -> dueText.setText(due[0] == 0 ? getString(R.string.assistant_task_no_due)
        : java.time.Instant.ofEpochMilli(due[0]).atZone(com.zikra.app.location.LocationStore.current(this).zone)
          .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy HH:mm z", new java.util.Locale("id", "ID"))));
    android.widget.Button editDue = new android.widget.Button(this); editDue.setText(R.string.assistant_task_due); form.addView(editDue);
    editDue.setOnClickListener(view -> {
      java.time.ZoneId zone = com.zikra.app.location.LocationStore.current(this).zone;
      java.time.ZonedDateTime selected = (due[0] == 0 ? java.time.Instant.now() : java.time.Instant.ofEpochMilli(due[0])).atZone(zone);
      new android.app.DatePickerDialog(this, (picker, year, month, day) ->
          new android.app.TimePickerDialog(this, (clock, hour, minute) -> {
            due[0] = java.time.LocalDateTime.of(year, month + 1, day, hour, minute).atZone(zone).toInstant().toEpochMilli(); renderDue.run();
          }, selected.getHour(), selected.getMinute(), true).show(), selected.getYear(), selected.getMonthValue() - 1, selected.getDayOfMonth()).show();
    });
    android.widget.Button clearDue = new android.widget.Button(this); clearDue.setText(R.string.assistant_task_no_due);
    clearDue.setOnClickListener(view -> { due[0] = 0; renderDue.run(); }); form.addView(clearDue); renderDue.run();
    ScrollView scrollForm = new ScrollView(this); scrollForm.addView(form);
    taskDialog = new AlertDialog.Builder(this).setTitle(R.string.assistant_review_task).setView(scrollForm)
        .setPositiveButton(R.string.assistant_save_task, null).setNegativeButton("Batal", null).create();
    taskDialog.setOnShowListener(dialog -> taskDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
      try (ZikraDbHelper db = new ZikraDbHelper(this)) {
        db.applyReviewedProposal(proposal.reviewed(title.getText().toString(), due[0]));
        com.zikra.app.study.StudyReminders.sync(this);
        taskDialog.dismiss(); renderConversation(); toast(getString(R.string.assistant_task_saved));
      } catch (IllegalArgumentException | IllegalStateException invalid) { title.setError(invalid.getMessage()); }
    }));
    taskDialog.show();
  }

  private void showSource(JSONObject source) {
    if (OnlineAssistantClient.verifiedSource(source, source) == null) return;
    TextView text = new TextView(this); text.setTextIsSelectable(true); text.setPadding(dp(20), dp(8), dp(20), dp(16));
    text.setText(getString(R.string.assistant_tafsir_note) + "\n\n" + source.optString("text")
        + (source.optBoolean("truncated") ? "\n\nPetikan dipotong; buka sumber untuk teks lengkap." : "") + "\n\n" + source.optString("url"));
    ScrollView container = new ScrollView(this); container.addView(text);
    sourceDialog = new AlertDialog.Builder(this).setTitle("Tafsir · " + source.optInt("chapter") + ":" + source.optInt("number"))
        .setView(container).setNegativeButton("Tutup", null).setPositiveButton(R.string.assistant_tafsir_open, (dialog, which) -> {
          try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(source.optString("url")))); }
          catch (ActivityNotFoundException unavailable) { toast("Browser belum tersedia."); }
        }).show();
  }

  private void composeTopic(String text) {
    input.setText(text);
    sendMessage();
  }

  private void focusComposer() {
    input.requestFocus();
    scroll.post(
        () -> {
          if (isFinishing()) return;
          InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
          if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
  }

  private void beginVoice() {
    voiceDialog = new AlertDialog.Builder(this).setTitle(R.string.assistant_voice_start)
        .setMessage(R.string.assistant_voice_consent).setPositiveButton(R.string.assistant_voice_start, (dialog, which) -> startVoice())
        .setNegativeButton("Batal", null).show();
  }

  private void startVoice() {
    ((TextView) findViewById(R.id.aiVoiceStatus))
        .setText("Layanan Android mengubah suaramu menjadi teks. Periksa sebelum mengirim.");
    Intent recognize = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    recognize.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    recognize.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
    recognize.putExtra(RecognizerIntent.EXTRA_PROMPT, "Apa yang ingin kamu kerjakan hari ini?");
    recognize.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    try {
      startActivityForResult(recognize, VOICE_REQUEST);
    } catch (ActivityNotFoundException | SecurityException unavailable) {
      ((TextView) findViewById(R.id.aiVoiceStatus))
          .setText("Input suara belum tersedia di perangkat ini");
      voiceDialog =
          new AlertDialog.Builder(this)
              .setTitle("Gunakan pesan tertulis")
              .setMessage(
                  "Layanan pengenal suara Android belum tersedia. Kamu tetap bisa menuliskan target"
                      + " di kolom pesan.")
              .setPositiveButton("Tulis pesan", (dialog, which) -> focusComposer())
              .setNegativeButton("Tutup", null)
              .show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(requestCode==EXPORT_IMAGE){String[] selected=pendingImageExport;pendingImageExport=null;if(resultCode==RESULT_OK&&data!=null&&selected!=null)generatedImages.copyTo(selected[0],selected[1],selected[2],data.getData(),()->toast(getString(R.string.ai_images_saved)),()->toast(getString(R.string.ai_images_export_failed)));return;}
    if(requestCode==CAPTURE_PHOTO){android.net.Uri uri=cameraUri;cameraUri=null;if(resultCode==RESULT_OK&&uri!=null&&chatId.equals(cameraChat))importAttachment(uri);else AssistantCameraProvider.discard(this,uri);return;}
    if(requestCode==PICK_PHOTO||requestCode==PICK_DOCUMENT){if(resultCode==RESULT_OK&&data!=null)importAttachment(data.getData());return;}
    if (requestCode != VOICE_REQUEST || isFinishing()) return;
    ArrayList<?> results = null;
    try { results = data == null ? null : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS); }
    catch (RuntimeException invalid) { }
    if (resultCode != RESULT_OK
        || results == null
        || results.isEmpty()
        || !(results.get(0) instanceof String)
        || ((String) results.get(0)).trim().isEmpty()) {
      ((TextView) findViewById(R.id.aiVoiceStatus))
          .setText("Belum ada pesan suara. Coba lagi atau ketik pesan.");
      return;
    }
    String raw = (String) results.get(0);
    String result = com.zikra.app.assistant.VoiceDraft.normalize(raw);
    if (raw.trim().length() > OfflineAssistant.MAX_INPUT_LENGTH) {
      toast("Hasil suara dibatasi 500 karakter. Periksa sebelum dikirim.");
    }
    input.setText(result);
    input.setSelection(input.length());
    ((TextView) findViewById(R.id.aiVoiceStatus)).setText("Pesan siap. Periksa lalu tekan kirim.");
    scroll.post(() -> scroll.smoothScrollTo(0, thread.getBottom()));
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("draft", input.getText().toString());
    state.putString("chat_id",chatId);persistConversation();
    if(pendingImageExport!=null){state.putString("image_export_chat",pendingImageExport[0]);state.putString("image_export_id",pendingImageExport[1]);state.putString("image_export_mime",pendingImageExport[2]);}
    if(cameraUri!=null){state.putString("camera_uri",cameraUri.toString());state.putString("camera_chat",cameraChat);}
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if(network!=null){network.start();updateMode();refreshImageCapability();}
  }

  @Override
  protected void onPause() {
    if (onlineClient != null) onlineClient.cancel();
    cancelImageWork();
    if (waiting) { setWaiting(false); showCancelled(); }
    persistConversation();
    if(network!=null)network.close();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (pageRoot != null) {
      pageRoot.setOnApplyWindowInsetsListener(null);
      if (keyboardLayoutListener != null && pageRoot.getViewTreeObserver().isAlive()) {
        pageRoot.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
      }
    }
    if (clearDialog != null) clearDialog.dismiss();
    if (voiceDialog != null) voiceDialog.dismiss();
    if (modeDialog != null) modeDialog.dismiss();
    if (selectionDialog != null) selectionDialog.dismiss();
    if (taskDialog != null) taskDialog.dismiss();
    if (sourceDialog != null) sourceDialog.dismiss();
    if (onlineClient != null) onlineClient.close();
    if(imageClient!=null)imageClient.close();if(generatedImages!=null)generatedImages.close();
    if(network!=null)network.close();
    if(conversations!=null)conversations.close();
    if(attachmentReader!=null)attachmentReader.close();
    super.onDestroy();
  }
}
