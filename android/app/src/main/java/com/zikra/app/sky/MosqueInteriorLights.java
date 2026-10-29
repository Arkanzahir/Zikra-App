package com.zikra.app.sky;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/** Light stays within the recesses of the approved 960x640 mosque illustration. */
final class MosqueInteriorLights {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path openings=new Path();
  private final Shader interior=new RadialGradient(0,0,1,
      new int[]{0xC0FFE2A4,0x68FFCA79,0x12FFB653,0x00FFB653},
      new float[]{0,.5f,.9f,1},Shader.TileMode.CLAMP);
  private final Shader reflection=new RadialGradient(0,0,1,
      new int[]{0x66FFD081,0x00FFBD5A},null,Shader.TileMode.CLAMP);
  private static final float[] CENTRES={450,509,570,662,746,806,866};
  private static final float[][] OPENINGS={
      {450,38,425,497},{509,42,426,497},{570,45,426,498},{662,61,409,499},
      {746,44,427,499},{806,44,428,499},{866,44,428,499},{926,39,430,476}};

  MosqueInteriorLights() {
    for(float[] opening:OPENINGS)arch(opening[0],opening[1],opening[2],opening[3]);
  }

  private void arch(float centre,float width,float top,float bottom) {
    float half=width/2,shoulder=top+21;
    openings.moveTo(centre-half,bottom);openings.lineTo(centre-half,shoulder);
    openings.quadTo(centre-half,top+12,centre,top);
    openings.quadTo(centre+half,top+12,centre+half,shoulder);
    openings.lineTo(centre+half,bottom);openings.close();
  }

  void draw(Canvas canvas,float strength) {
    if(strength<=0)return;
    canvas.save();canvas.clipPath(openings);
    paint.setShader(interior);paint.setAlpha(Math.round(255*strength));
    for(float[] opening:OPENINGS){
      canvas.save();canvas.translate(opening[0]+opening[1]*.12f,opening[2]+(opening[3]-opening[2])*.38f);
      canvas.scale(opening[1]*.68f,(opening[3]-opening[2])*.82f);
      canvas.drawCircle(0,0,1,paint);canvas.restore();
    }
    canvas.restore();
    paint.setShader(reflection);paint.setAlpha(Math.round(160*strength));
    for(float centre:CENTRES){
      canvas.save();canvas.translate(centre,509);canvas.scale(20,12);
      canvas.drawCircle(0,0,1,paint);canvas.restore();
    }
    paint.setShader(null);
  }
}
