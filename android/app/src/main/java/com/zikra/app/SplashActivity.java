package com.zikra.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import com.zikra.app.ui.AmbientMotionPolicy;
import com.zikra.app.ui.SplashBrandView;

/** One short cold-launch transition; no wait for network and no second logo behind the OS splash. */
public final class SplashActivity extends Activity {
  private static boolean introShown;
  private SplashBrandView brand;
  private AmbientMotionPolicy motion;
  private ValueAnimator animator;
  private ViewTreeObserver.OnPreDrawListener firstDraw;
  private boolean resumed, entered, skip, systemReady, started;

  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    if(!isTaskRoot()){finish();return;}
    skip=state!=null || introShown;
    setContentView(R.layout.activity_splash);
    getWindow().getDecorView().setSystemUiVisibility(0);
    brand=findViewById(R.id.splashBrand);
    motion=new AmbientMotionPolicy(this,()->{
      if(resumed && !motion.enabled()) enterHome();
    });
    systemReady=android.os.Build.VERSION.SDK_INT<31;
    if(android.os.Build.VERSION.SDK_INT>=31) Api31.exit(this);
  }

  @Override protected void onResume() {
    super.onResume();resumed=true;
    if(brand==null)return;
    motion.start();
    if(skip || !motion.enabled()){enterHome();return;}
    if(systemReady) prepare();
  }

  private void prepare() {
    if(started || entered || !resumed || firstDraw!=null)return;
    firstDraw=()->{
      removeFirstDraw();
      if(resumed && !entered) animateBrand();
      return true;
    };
    brand.getViewTreeObserver().addOnPreDrawListener(firstDraw);brand.invalidate();
  }

  private void animateBrand() {
    if(started)return;started=true;introShown=true;
    animator=ValueAnimator.ofFloat(0,1);animator.setDuration(400);
    animator.setInterpolator(new LinearInterpolator());
    animator.addUpdateListener(value->brand.setProgress((float)value.getAnimatedValue()));
    animator.addListener(new AnimatorListenerAdapter(){
      private boolean cancelled;
      @Override public void onAnimationCancel(Animator value){cancelled=true;}
      @Override public void onAnimationEnd(Animator value){if(!cancelled && resumed)enterHome();}
    });
    animator.start();
  }

  private void enterHome() {
    if(entered || !resumed || isFinishing())return;
    entered=true;introShown=true;removeFirstDraw();cancelAnimation();
    if(brand!=null)brand.setProgress(1);
    startActivity(new Intent(this,HomeActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
    finish();overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
  }

  private void cancelAnimation(){if(animator!=null){animator.cancel();animator=null;}}
  private void removeFirstDraw(){
    if(firstDraw!=null && brand!=null && brand.getViewTreeObserver().isAlive())
      brand.getViewTreeObserver().removeOnPreDrawListener(firstDraw);
    firstDraw=null;
  }

  @Override protected void onPause(){
    resumed=false;skip=true;removeFirstDraw();cancelAnimation();
    if(motion!=null)motion.stop();super.onPause();
  }
  @Override public void onWindowFocusChanged(boolean focused){
    super.onWindowFocusChanged(focused);
    if(!focused && animator!=null && animator.isRunning()){skip=true;cancelAnimation();}
    if(focused && resumed && skip && brand!=null)enterHome();
  }
  @Override protected void onDestroy(){removeFirstDraw();cancelAnimation();super.onDestroy();}
  @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("intro_seen",true);super.onSaveInstanceState(state);}

  @android.annotation.TargetApi(31)
  private static final class Api31 {
    static void exit(SplashActivity page){
      page.getSplashScreen().setOnExitAnimationListener(cover->{
        page.systemReady=true;
        if(page.brand!=null)page.brand.fromSystemIcon(cover.getIconView());
        cover.remove();
        if(page.resumed){
          if(page.skip || !page.motion.enabled())page.enterHome();else page.prepare();
        }
      });
    }
  }
}
