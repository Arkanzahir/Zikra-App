package com.zikra.app;

import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.KeyEvent;
import android.view.View;
import com.zikra.app.ui.SplashBrandView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

/** Explicit opt-in after a private backup; never seeds or edits user preferences. */
public final class SplashPolishTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;

  @Override protected void setUp() throws Exception {
    super.setUp();context=getInstrumentation().getTargetContext();
    assertTrue(new File(context.getFilesDir(),"ui07-backup-verified").isFile());
    assertEquals("1.9.25",context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName);
  }

  private void main(Runnable action){getInstrumentation().runOnMainSync(action);getInstrumentation().waitForIdleSync();}
  private static Object field(Object object,String name) throws Exception {
    Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);
  }
  private void resetIntro() throws Exception {
    Field shown=SplashActivity.class.getDeclaredField("introShown");shown.setAccessible(true);shown.setBoolean(null,false);
  }
  private Activity launch(boolean reset) throws Exception {
    if(reset)resetIntro();
    ActivityMonitor home=getInstrumentation().addMonitor(HomeActivity.class.getName(),null,false);
    try{
      main(()->context.startActivity(new Intent(context,SplashActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)));
      page=home.waitForActivityWithTimeout(8000);assertNotNull(page);
      getInstrumentation().waitForIdleSync();SystemClock.sleep(200);
      assertEquals("One Home destination",1,home.getHits());return page;
    }finally{getInstrumentation().removeMonitor(home);}
  }

  public void testApprovedMotionRendersOriginalEndpointsAndDistinctPhases() throws Exception {
    final SplashBrandView[] view=new SplashBrandView[1];
    main(()->{
      view[0]=new SplashBrandView(context,null);view[0].setBackgroundColor(context.getColor(R.color.pine));
      view[0].measure(View.MeasureSpec.makeMeasureSpec(780,View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(1688,View.MeasureSpec.EXACTLY));view[0].layout(0,0,780,1688);
    });
    File dir=new File(context.getExternalFilesDir(null),"ui07");assertTrue(dir.isDirectory()||dir.mkdirs());
    Bitmap previous=null;
    try{
      for(int time:new int[]{0,90,160,340,520,800}){
        Bitmap image=Bitmap.createBitmap(780,1688,Bitmap.Config.ARGB_8888);
        main(()->{view[0].setProgress(time/800f);view[0].draw(new Canvas(image));});
        View book=(View)field(view[0],"left"),star=(View)field(view[0],"spark"),word=(View)field(view[0],"word");
        if(time==0||time==800){
          assertEquals(1f,book.getScaleX(),.0001f);assertEquals(0f,book.getRotation(),.0001f);
          assertEquals(1f,star.getScaleX(),.0001f);assertEquals(time==0?0f:1f,word.getAlpha(),.0001f);
        }
        if(time==90){assertEquals(.78f,book.getScaleX(),.0001f);assertEquals(11f,book.getRotation(),.0001f);}
        if(previous!=null){assertFalse("Each approved phase changes actual Canvas pixels",image.sameAs(previous));previous.recycle();}
        try(FileOutputStream stream=new FileOutputStream(new File(dir,"phase-"+time+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,stream));}
        previous=image;
      }
    }finally{if(previous!=null)previous.recycle();}
  }

  public void testColdAndWarmNavigateOnce() throws Exception {launch(true);launch(false);}

  public void testDisabledMotionSkipsAnimator() throws Exception {
    ActivityMonitor splash=getInstrumentation().addMonitor(SplashActivity.class.getName(),null,false);
    try{launch(true);assertNotNull(splash.getLastActivity());assertFalse((Boolean)field(splash.getLastActivity(),"started"));}
    finally{getInstrumentation().removeMonitor(splash);}
  }

  public void testPausedIntroDoesNotNavigateInBackground() throws Exception {
    resetIntro();ActivityMonitor splash=getInstrumentation().addMonitor(SplashActivity.class.getName(),null,false);
    ActivityMonitor home=getInstrumentation().addMonitor(HomeActivity.class.getName(),null,false);
    try{
      main(()->context.startActivity(new Intent(context,SplashActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)));
      Activity intro=splash.waitForActivityWithTimeout(3000);assertNotNull(intro);
      getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);SystemClock.sleep(1000);
      assertFalse((Boolean)field(intro,"resumed"));assertEquals(0,home.getHits());
      main(()->context.startActivity(new Intent(context,SplashActivity.class)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
      page=home.waitForActivityWithTimeout(8000);assertNotNull(page);assertEquals(1,home.getHits());
    }finally{getInstrumentation().removeMonitor(splash);getInstrumentation().removeMonitor(home);}
  }

  @Override protected void tearDown() throws Exception {
    if(page!=null&&!page.isDestroyed())main(page::finish);super.tearDown();
  }
}
