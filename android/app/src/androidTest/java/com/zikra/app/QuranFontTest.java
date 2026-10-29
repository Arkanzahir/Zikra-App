package com.zikra.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.reading.ArabicWordMap;
import com.zikra.app.reading.SurahData;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/** Backup-gated font-only checks. No audio, network fixtures, profile or DB edits. */
@SuppressWarnings("deprecation")
public final class QuranFontTest extends InstrumentationTestCase {
  private Context context;
  private QuranActivity page;
  private final Map<String,Object> bookmark=new HashMap<>();
  private boolean opened;
  private static final String[] KEYS={"last_chapter","last_surah","last_ayah","last_verse_key"};

  @Override protected void setUp()throws Exception{
    super.setUp();context=getInstrumentation().getTargetContext();
    assertTrue("Private host backup required",new File(context.getFilesDir(),"ui09-backup-verified").isFile());
    assertEquals("1.9.29",context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName);
    SharedPreferences prefs=context.getSharedPreferences("zikra_preferences",0);
    for(String key:KEYS)if(prefs.contains(key))bookmark.put(key,prefs.getAll().get(key));
  }
  private void main(Runnable action){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();getInstrumentation().runOnMainSync(()->{try{action.run();}catch(Throwable e){error.set(e);}});getInstrumentation().waitForIdleSync();if(error.get()!=null)throw new AssertionError(error.get());}
  private static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
  private void save(String name,Bitmap image)throws Exception{File folder=new File(context.getExternalFilesDir(null),"ui09");assertTrue(folder.isDirectory()||folder.mkdirs());try(FileOutputStream stream=new FileOutputStream(new File(folder,name))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,stream));}}

  public void testFontShapesBuiltinTextAtSizesAndThemesWithoutChangingWords()throws Exception{
    final int[] layouts={0};
    for(int night:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES})for(float scale:new float[]{1,2})for(int size:new int[]{24,32,44}){
      Configuration config=new Configuration(context.getResources().getConfiguration());config.fontScale=scale;
      config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|night;
      Context themed=new ContextThemeWrapper(context.createConfigurationContext(config),R.style.Theme_Zikra);
      Typeface expected=themed.getResources().getFont(R.font.amiri_quran);
      for(SurahData chapter:SurahData.ALL)for(int index=0;index<chapter.arabic.length;index++){
        final int verse=index;final View[] row={null};
        main(()->{
          row[0]=LayoutInflater.from(themed).inflate(R.layout.reading_item_ayah,null,false);
          TextView arabic=row[0].findViewById(R.id.ayahArabic);arabic.setText(chapter.arabic[verse]);arabic.setTextSize(size);
          ((TextView)row[0].findViewById(R.id.ayahNumber)).setText(Integer.toString(verse+1));
          ((TextView)row[0].findViewById(R.id.ayahTranslation)).setText(chapter.translations[verse]);
          int width=Math.round(360*themed.getResources().getDisplayMetrics().density);
          row[0].measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
          row[0].layout(0,0,width,row[0].getMeasuredHeight());
          assertEquals(expected,arabic.getTypeface());assertTrue(arabic.getIncludeFontPadding());
          assertEquals(chapter.arabic[verse],arabic.getText().toString());assertTrue(arabic.getLayout().getLineCount()>0);
          android.text.Layout layout=arabic.getLayout();int available=arabic.getWidth()-arabic.getCompoundPaddingLeft()-arabic.getCompoundPaddingRight();
          for(int line=0;line<layout.getLineCount();line++){
            assertTrue("No visible glyph clipping at "+chapter.number+":"+(verse+1)+" size="+size+" scale="+scale
                +" line="+line+" visible="+layout.getLineMax(line)+" available="+available,layout.getLineMax(line)<=available+2);
            String text=arabic.getText().subSequence(layout.getLineStart(line),layout.getLineEnd(line)).toString();
            Rect ink=new Rect();arabic.getPaint().getTextBounds(text,0,text.length(),ink);
            int baseline=arabic.getTotalPaddingTop()+layout.getLineBaseline(line);
            assertTrue("Top diacritics fit",baseline+ink.top>=0);
            assertTrue("Bottom diacritics fit",baseline+ink.bottom<=arabic.getHeight());
          }
          layouts[0]++;
        });
        if(chapter.number==1&&index==1&&scale==1&&size==32){
          Bitmap image=Bitmap.createBitmap(row[0].getWidth(),row[0].getHeight(),Bitmap.Config.ARGB_8888);
          main(()->{Canvas canvas=new Canvas(image);canvas.drawColor(themed.getColor(R.color.background));row[0].draw(canvas);});
          save(night==Configuration.UI_MODE_NIGHT_YES?"font-dark.png":"font-light.png",image);image.recycle();
        }
      }
    }
    String text=SurahData.ALL[0].arabic[1];ArabicWordMap map=ArabicWordMap.match(text,List.of("ٱلْحَمْدُ","لِلَّهِ","رَبِّ","ٱلْعَـٰلَمِينَ"));
    assertNotNull("Existing word mapping survives exact original text",map);assertEquals(4,map.starts.length);
    JSONObject proof=new JSONObject().put("nativeLayouts",layouts[0]).put("arabicSizes",new org.json.JSONArray(new int[]{24,32,44}))
        .put("configurationFontScales",new org.json.JSONArray(new int[]{1,2})).put("themes",new org.json.JSONArray(new String[]{"light","dark"}))
        .put("originalArabicAndTranslationUnchanged",true).put("wordMapStillMatches",true).put("systemFontSettingChanged",false).put("audioPlayed",false);
    File folder=new File(context.getExternalFilesDir(null),"ui09");try(FileOutputStream stream=new FileOutputStream(new File(folder,"font-shaping.json"))){stream.write(proof.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  }

  public void testActualReaderAndSettingsUseBundledFont()throws Exception{
    opened=true;page=(QuranActivity)getInstrumentation().startActivitySync(new Intent(context,QuranActivity.class)
        .putExtra(QuranActivity.EXTRA_TARGET_CHAPTER,1).putExtra(QuranActivity.EXTRA_TARGET_VERSE,1).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    long end=SystemClock.uptimeMillis()+15000;
    while(SystemClock.uptimeMillis()<end){getInstrumentation().waitForIdleSync();if(((LinearLayout)page.findViewById(R.id.ayahContainer)).getChildCount()==7)break;SystemClock.sleep(100);}
    LinearLayout rows=page.findViewById(R.id.ayahContainer);assertEquals(7,rows.getChildCount());
    Typeface font=page.getResources().getFont(R.font.amiri_quran);
    for(int i=0;i<7;i++){
      TextView arabic=rows.getChildAt(i).findViewById(R.id.ayahArabic);assertEquals(font,arabic.getTypeface());assertEquals(SurahData.ALL[0].arabic[i],arabic.getText().toString());
      assertFalse("Translation retains Latin UI font",font.equals(((TextView)rows.getChildAt(i).findViewById(R.id.ayahTranslation)).getTypeface()));
    }
    assertEquals(font,((TextView)page.findViewById(R.id.surahArabicTitle)).getTypeface());
    SystemClock.sleep(350);Bitmap screen=getInstrumentation().getUiAutomation().takeScreenshot();save("phone-reader.png",screen);screen.recycle();
    main(()->page.findViewById(R.id.quranReaderFont).performClick());
    AlertDialog dialog=(AlertDialog)field(page,"readingDialog");assertTrue(dialog.isShowing());
    assertEquals(font,((TextView)dialog.findViewById(R.id.quranArabicPreview)).getTypeface());
    SystemClock.sleep(250);screen=getInstrumentation().getUiAutomation().takeScreenshot();save("phone-settings.png",screen);screen.recycle();main(dialog::dismiss);
  }

  @Override protected void tearDown()throws Exception{
    if(page!=null)main(page::finish);
    if(opened){
      SharedPreferences prefs=context.getSharedPreferences("zikra_preferences",0);
      // Restore only the public test navigation, never overwrite a newer user bookmark.
      if(prefs.getInt("last_chapter",0)==1&&prefs.getInt("last_ayah",0)==1&&"1:1".equals(prefs.getString("last_verse_key",""))){
        SharedPreferences.Editor editor=prefs.edit();for(String key:KEYS){Object value=bookmark.get(key);if(value instanceof Integer)editor.putInt(key,(Integer)value);else if(value instanceof String)editor.putString(key,(String)value);else editor.remove(key);}assertTrue(editor.commit());
      }
    }
    super.tearDown();
  }
}
