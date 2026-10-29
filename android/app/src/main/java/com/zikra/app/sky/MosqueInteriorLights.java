package com.zikra.app.sky;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import com.zikra.app.R;
import java.io.IOException;
import java.io.InputStream;

/** Registered doorway textures only; the approved scene's sky and outer silhouette stay native. */
final class MosqueInteriorLights {
  private static final Rect BAND=new Rect(540,970,1243,1150);
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  private final Bitmap nightBand;
  private final Opening[] openings={
      new Opening(450,38,425,497,497,568,1009,614,1110),
      new Opening(509,42,426,497,497,644,1009,694,1111),
      new Opening(570,45,426,498,498,724,1008,777,1111),
      new Opening(662,61,409,499,499,826,990,909,1113),
      new Opening(746,44,427,499,499,966,1009,1019,1112),
      new Opening(806,44,428,499,499,1039,1009,1091,1113),
      new Opening(866,44,428,499,499,1119,1010,1172,1113),
      new Opening(926,39,430,499,476,1194,1010,1243,1113)};
  private final Shader reflection=new RadialGradient(0,0,1,
      new int[]{0x38FFD28A,0x00FFC06A},null,Shader.TileMode.CLAMP);

  MosqueInteriorLights(Context context) {nightBand=decodeBand(context);}

  @SuppressWarnings("deprecation") private static Bitmap decodeBand(Context context) {
    BitmapRegionDecoder decoder=null;
    try(InputStream input=context.getResources().openRawResource(R.raw.home_mosque_recess_reference)){
      decoder=BitmapRegionDecoder.newInstance(input,false);
      if(decoder.getWidth()!=1243||decoder.getHeight()!=1266)return null;
      BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
      options.inPreferredConfig=Bitmap.Config.ARGB_8888;
      return decoder.decodeRegion(BAND,options);
    }catch(IOException|IllegalArgumentException invalid){
      return null;
    }finally{if(decoder!=null)decoder.recycle();}
  }

  private static final class Opening {
    final Path clip=new Path();
    final Rect source;
    final RectF destination;
    final float centre,bottom;
    Opening(float centre,float width,float top,float bottom,float visibleBottom,int left,int upper,int right,int lower){
      this.centre=centre;this.bottom=bottom;
      destination=new RectF(centre-width/2,top,centre+width/2,bottom);
      source=new Rect(left-BAND.left,upper-BAND.top,right-BAND.left,lower-BAND.top);
      float half=width/2,shoulder=top+21;
      clip.moveTo(centre-half,visibleBottom);clip.lineTo(centre-half,shoulder);
      clip.quadTo(centre-half,top+12,centre,top);
      clip.quadTo(centre+half,top+12,centre+half,shoulder);
      clip.lineTo(centre+half,visibleBottom);clip.close();
    }
  }

  void draw(Canvas canvas,float strength) {
    if(strength<=0||nightBand==null)return;
    paint.setShader(null);paint.setAlpha(Math.round(255*strength));
    for(Opening opening:openings){
      canvas.save();canvas.clipPath(opening.clip);
      canvas.drawBitmap(nightBand,opening.source,opening.destination,paint);canvas.restore();
    }
    paint.setShader(reflection);paint.setAlpha(Math.round(85*strength));
    for(int i=0;i<openings.length-1;i++){
      Opening opening=openings[i];canvas.save();canvas.translate(opening.centre,opening.bottom+7);
      canvas.scale(i==3?24:15,8);canvas.drawCircle(0,0,1,paint);canvas.restore();
    }
    paint.setShader(null);
  }
}
