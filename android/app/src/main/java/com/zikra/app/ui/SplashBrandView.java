package com.zikra.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.R;

/** The approved emblem remains vector artwork; the wordmark remains native text. */
public final class SplashBrandView extends FrameLayout {
  private final FrameLayout mark;
  private final TextView word;
  private final View crescent, left, right, spark, halo;
  private final PathInterpolator ease = new PathInterpolator(0,0,.58f,1);
  private float systemOffsetY;
  private boolean systemOrigin;

  public SplashBrandView(Context context, AttributeSet attrs) {
    super(context,attrs);
    setClipChildren(false);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    LinearLayout lockup=new LinearLayout(context);
    lockup.setOrientation(LinearLayout.HORIZONTAL);lockup.setGravity(Gravity.CENTER_VERTICAL);
    lockup.setClipChildren(false);
    addView(lockup,new LayoutParams(-2,-2,Gravity.CENTER));
    mark=new FrameLayout(context);mark.setClipChildren(false);
    lockup.addView(mark,new LinearLayout.LayoutParams(dp(112),dp(112)));
    halo=new View(context);
    GradientDrawable glow=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
        new int[]{0x20FFFFFF,0x00FFFFFF});
    glow.setShape(GradientDrawable.OVAL);glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
    glow.setGradientRadius(dp(90));halo.setBackground(glow);halo.setScaleX(1.8f);halo.setScaleY(1.8f);
    mark.addView(halo,new LayoutParams(-1,-1));
    crescent=part(R.drawable.splash_crescent);left=part(R.drawable.splash_bookleft);
    right=part(R.drawable.splash_bookright);spark=part(R.drawable.splash_spark);
    left.setPivotX(dp(112)*21/64f);left.setPivotY(dp(112)*54/64f);
    right.setPivotX(dp(112)*43/64f);right.setPivotY(dp(112)*54/64f);
    spark.setPivotX(dp(112)*38.3f/64);spark.setPivotY(dp(112)*26.3f/64);
    word=new TextView(context);word.setText("zikra");word.setTextColor(context.getColor(R.color.white));
    word.setTextSize(TypedValue.COMPLEX_UNIT_DIP,44);
    Typeface font=context.getResources().getFont(R.font.plus_jakarta_sans);
    word.setTypeface(android.os.Build.VERSION.SDK_INT>=28?Typeface.create(font,600,false):Typeface.create(font,Typeface.BOLD));
    word.setIncludeFontPadding(false);
    LinearLayout.LayoutParams text=new LinearLayout.LayoutParams(-2,-2);text.leftMargin=dp(8);
    lockup.addView(word,text);
    setProgress(0);
  }

  private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }

  private View part(int resource) {
    ImageView view=new ImageView(getContext());view.setImageResource(resource);
    mark.addView(view,new LayoutParams(-1,-1));return view;
  }

  public void fromSystemIcon(View icon) {
    if(icon==null)return;
    int[] system=new int[2],app=new int[2];icon.getLocationOnScreen(system);mark.getLocationOnScreen(app);
    systemOffsetY=system[1]+icon.getHeight()/2f-app[1]-mark.getHeight()/2f;
    systemOrigin=true;setProgress(0);
  }

  private float track(float progress,float start,float end) {
    return ease.getInterpolation(Math.max(0,Math.min(1,(progress-start)/(end-start))));
  }

  public void setProgress(float progress) {
    float shift=track(progress,.18f,.74f),reveal=track(progress,.43f,.86f);
    mark.setTranslationX((word.getMeasuredWidth()+dp(8))/2f*(1-shift));
    mark.setTranslationY(systemOffsetY*(1-shift));
    word.setAlpha(reveal);word.setTranslationX(-dp(10)*(1-reveal));
    crescent.setAlpha(systemOrigin?1:.65f+.35f*track(progress,0,.34f));
    crescent.setRotation(systemOrigin?0:-5*(1-track(progress,0,.45f)));
    left.setAlpha(systemOrigin?1:.35f+.65f*track(progress,0,.32f));right.setAlpha(left.getAlpha());
    left.setRotation(systemOrigin?0:6*(1-track(progress,0,.4f)));right.setRotation(-left.getRotation());
    float bright=track(progress,.23f,.52f);
    spark.setAlpha(systemOrigin?1:.18f+.82f*bright);
    float scale=systemOrigin?1:.65f+.43f*bright-.08f*track(progress,.52f,.77f);
    spark.setScaleX(scale);spark.setScaleY(scale);
    halo.setAlpha(.04f*track(progress,0,.62f));
  }
}
