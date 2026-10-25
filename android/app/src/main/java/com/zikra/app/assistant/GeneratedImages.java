package com.zikra.app.assistant;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Bounded private image files. JSON history contains references, never base64. */
public final class GeneratedImages implements AutoCloseable {
  public static final int MAX_BYTES=2_000_000,MAX_LOCAL=32_000_000;
  private final Context context;private final File base;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler main=new Handler(Looper.getMainLooper());
  private volatile int generation;private volatile boolean closed;
  public interface Result{void saved(String id,String mime);void failed();}
  public static final class Data {
    public final String mime;public final int width,height;final byte[] bytes;
    private Data(String mime,int w,int h,byte[] bytes){this.mime=mime;width=w;height=h;this.bytes=bytes;}
    public static Data parse(JSONObject value)throws Exception{
      if(!"cloudflare".equals(value.optString("provider"))||!"flux-1-schnell".equals(value.optString("model"))||!value.optBoolean("generated"))throw new IOException();
      String mime=value.getString("mime"),raw=value.getString("data");
      if(!Arrays.asList("image/png","image/jpeg").contains(mime)||raw.length()>2_670_000||raw.length()%4!=0||!raw.matches("[A-Za-z0-9+/]+={0,2}"))throw new IOException();
      byte[] bytes=android.util.Base64.decode(raw,android.util.Base64.NO_WRAP);
      if(bytes.length<33||bytes.length>MAX_BYTES||!android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP).equals(raw))throw new IOException();
      BitmapFactory.Options info=new BitmapFactory.Options();info.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,info);
      if(!mime.equals(info.outMimeType)||info.outWidth<64||info.outHeight<64||info.outWidth>1536||info.outHeight>1536||(long)info.outWidth*info.outHeight>2_100_000L||value.getInt("width")!=info.outWidth||value.getInt("height")!=info.outHeight)throw new IOException();
      // Reject truncated/corrupt compressed data as well as forged dimensions.
      BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=4;Bitmap check=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(check==null)throw new IOException();check.recycle();
      return new Data(mime,info.outWidth,info.outHeight,bytes);
    }
  }
  public GeneratedImages(Context context){this.context=context.getApplicationContext();base=new File(context.getFilesDir(),"assistant_images");}
  private static void chatId(String id){if(id==null||!id.matches("(?:legacy-local|[0-9a-f-]{36})"))throw new IllegalArgumentException();}
  public static File imageFile(Context context,String chat,String id,String mime){chatId(chat);if(id==null||!id.matches("[0-9a-f-]{36}")||!Arrays.asList("image/png","image/jpeg").contains(mime))throw new IllegalArgumentException();return new File(new File(new File(context.getFilesDir(),"assistant_images"),chat),id+(mime.equals("image/png")?".png":".jpg"));}
  private long size(File dir){long n=0;File[] files=dir.listFiles();if(files!=null)for(File f:files)n+=f.isDirectory()?size(f):f.length();return n;}
  public boolean canAccept(){return size(base)+MAX_BYTES<=MAX_LOCAL;}
  public void save(String chat,Data data,Result result){int token=++generation;worker.execute(()->{
    File file=null;try{if(!canAccept())throw new IOException();String id=UUID.randomUUID().toString();file=imageFile(context,chat,id,data.mime);File parent=file.getParentFile();if(!parent.exists()&&!parent.mkdirs())throw new IOException();
      AtomicFile atom=new AtomicFile(file);FileOutputStream out=atom.startWrite();try{out.write(data.bytes);atom.finishWrite(out);}catch(Exception e){atom.failWrite(out);throw e;}
      File saved=file;main.post(()->{if(!closed&&token==generation)result.saved(id,data.mime);else saved.delete();});
    }catch(Exception e){if(file!=null)file.delete();main.post(()->{if(!closed&&token==generation)result.failed();});}
  });}
  public Bitmap thumbnail(String chat,String id,String mime){try{BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=4;return BitmapFactory.decodeFile(imageFile(context,chat,id,mime).getPath(),opts);}catch(RuntimeException invalid){return null;}}
  public Bitmap preview(String chat,String id,String mime){try{return BitmapFactory.decodeFile(imageFile(context,chat,id,mime).getPath());}catch(RuntimeException invalid){return null;}}
  public Uri shareUri(String chat,String id,String mime){File file=imageFile(context,chat,id,mime);return new Uri.Builder().scheme("content").authority(context.getPackageName()+".assistant.images").appendPath(chat).appendPath(file.getName()).build();}
  public void copyTo(String chat,String id,String mime,Uri destination,Runnable saved,Runnable failed){worker.execute(()->{try{if(destination==null||!"content".equals(destination.getScheme()))throw new IOException();
    try(InputStream in=new FileInputStream(imageFile(context,chat,id,mime));OutputStream out=context.getContentResolver().openOutputStream(destination,"wt")){if(out==null)throw new IOException();byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))!=-1){total+=n;if(total>MAX_BYTES)throw new IOException();out.write(buffer,0,n);}}
    main.post(()->{if(!closed)saved.run();});
  }catch(Exception e){main.post(()->{if(!closed)failed.run();});}});}
  public void deleteChat(String id){chatId(id);worker.execute(()->{File dir=new File(base,id);File[] files=dir.listFiles();if(files!=null)for(File f:files)if(f.getName().matches("[0-9a-f-]{36}\\.(?:jpg|png)(?:\\.(?:bak|new))?"))f.delete();dir.delete();});}
  public void cancel(){generation++;}
  @Override public void close(){closed=true;generation++;worker.shutdown();}
}
