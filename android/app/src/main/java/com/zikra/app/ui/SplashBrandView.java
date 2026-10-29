package com.zikra.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RadialGradient;
import android.graphics.Shader;
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
  public static final long DURATION_MS = 800;
  private final FrameLayout mark;
  private final TextView word;
  private final View crescent, left, right, spark, halo;
  private final AccentView accent;
  private final PathInterpolator ease = new PathInterpolator(.22f,1,.36f,1);
  private float systemOffsetY;

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
        new int[]{0xFFD5E7DE,0x00D5E7DE});
    glow.setShape(GradientDrawable.OVAL);glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
    glow.setGradientRadius(dp(130));halo.setBackground(glow);
    mark.addView(halo,new LayoutParams(dp(260),dp(260),Gravity.CENTER));
    crescent=part(R.drawable.splash_crescent);left=part(R.drawable.splash_bookleft);
    right=part(R.drawable.splash_bookright);
    accent=new AccentView(context);mark.addView(accent,new LayoutParams(-1,-1));
    spark=part(R.drawable.splash_spark);
    crescent.setPivotX(dp(112)*32/64f);crescent.setPivotY(dp(112)*27/64f);
    left.setPivotX(dp(112)*31/64f);left.setPivotY(dp(112)*55/64f);
    right.setPivotX(dp(112)*33/64f);right.setPivotY(dp(112)*55/64f);
    spark.setPivotX(dp(112)*38.3f/64);spark.setPivotY(dp(112)*26.3f/64);
    word=new TextView(context);word.setText("zikra");word.setTextColor(context.getColor(R.color.white));
    word.setTextSize(TypedValue.COMPLEX_UNIT_DIP,44);
    word.setLetterSpacing(-.02f);
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
    setProgress(0);
  }

  private float tween(float time,float start,float end,float from,float to) {
    if(time<=start)return from;
    if(time>=end)return to;
    return from+(to-from)*ease.getInterpolation((time-start)/(end-start));
  }

  private float pulse(float time,float peak,float end,float initial,float middle,float finish) {
    return time<=peak?tween(time,0,peak,initial,middle):tween(time,peak,end,middle,finish);
  }

  public void setProgress(float progress) {
    float time=Math.max(0,Math.min(1,progress))*DURATION_MS;
    float shift=tween(time,420,780,0,1),reveal=tween(time,500,800,0,1);
    mark.setTranslationX((word.getMeasuredWidth()+dp(8))/2f*(1-shift));
    mark.setTranslationY(systemOffsetY*(1-shift));
    word.setAlpha(reveal);word.setTranslationX(-dp(8)*(1-reveal));
    crescent.setAlpha(pulse(time,90,340,1,.73f,1));
    float moonAngle=pulse(time,100,380,0,-4,0);crescent.setRotation(moonAngle);
    float bookScale=pulse(time,90,360,1,.78f,1);
    float bookAngle=pulse(time,90,380,0,11,0);
    float bookY=pulse(time,90,380,0,-1.8f,0)*getResources().getDisplayMetrics().density*1.75f;
    left.setScaleX(bookScale);right.setScaleX(bookScale);
    left.setRotation(bookAngle);right.setRotation(-bookAngle);
    left.setTranslationY(bookY);right.setTranslationY(bookY);
    spark.setAlpha(pulse(time,120,340,1,.65f,1));
    float scale=time<=340?pulse(time,120,340,1,.70f,1.12f):tween(time,340,520,1.12f,1);
    spark.setScaleX(scale);spark.setScaleY(scale);
    halo.setAlpha(pulse(time,340,800,.015f,.035f,.015f));
    float traceAlpha=time<=100?tween(time,0,100,0,.8f)
        :time<=360?tween(time,100,360,.8f,.65f):tween(time,360,480,.65f,0);
    float glint=time<=340?tween(time,180,340,0,.27f):tween(time,340,600,.27f,0);
    accent.update(tween(time,60,360,0,1),traceAlpha,moonAngle,glint);
  }

  /** A cached outline of the original crescent; never a second emblem. */
  private static final class AccentView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path trace=new Path();
    private final PathMeasure measure;
    private final Shader glintShader=new RadialGradient(38.3f,26.3f,12,
        new int[]{0xFFFFF4D4,0x00FFF4D4},null,Shader.TileMode.CLAMP);
    private float traceAlpha,moonAngle,glint;

    AccentView(Context context) {
      super(context);
      Path crescent=new Path();
      crescent.moveTo(40.5f,7.6f);
      crescent.cubicTo(27.3f,3.3f,14,12.7f,14,26.9f);
      crescent.cubicTo(14,35.7f,18.2f,41.8f,24.5f,45.3f);
      crescent.cubicTo(27.9f,46,30.3f,47.9f,32,50.4f);
      crescent.cubicTo(33.9f,47.9f,36.6f,46,40.3f,45.3f);
      crescent.cubicTo(47.4f,41.6f,51.9f,34.7f,52,26.8f);
      crescent.cubicTo(49.3f,36.7f,41.4f,42.1f,32.6f,39.7f);
      crescent.cubicTo(23.7f,37.3f,19.3f,28.8f,21.8f,20.2f);
      crescent.cubicTo(24.1f,12.3f,31.8f,7.4f,40.5f,7.6f);
      crescent.close();measure=new PathMeasure(crescent,true);
      paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(.65f);
    }

    void update(float progress,float alpha,float angle,float glow) {
      trace.reset();measure.getSegment(0,Math.min(160*progress,measure.getLength()),trace,true);
      traceAlpha=alpha;moonAngle=angle;glint=glow;invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/64f,getHeight()/64f);
      if(traceAlpha>0){
        canvas.save();canvas.rotate(moonAngle,32,27);
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.WHITE);
        paint.setAlpha(Math.round(255*traceAlpha));canvas.drawPath(trace,paint);canvas.restore();
      }
      if(glint>0){
        paint.setStyle(Paint.Style.FILL);paint.setShader(glintShader);paint.setAlpha(Math.round(255*glint));
        canvas.drawCircle(38.3f,26.3f,12,paint);
      }
      canvas.restore();
    }
  }
}
