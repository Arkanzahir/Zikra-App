package com.zikra.app.assistant;

import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.json.*;
import org.xmlpull.v1.XmlPullParser;

/** One reviewed attachment per message. Original files/EXIF are never uploaded. */
public final class AssistantAttachments implements AutoCloseable {
  public static final int MAX_FILE_BYTES=8_000_000,MAX_TEXT=12000,MAX_LOCAL_BYTES=25_000_000;
  private final Context context;
  private final File base;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler main=new Handler(Looper.getMainLooper());
  private volatile int generation;
  private volatile boolean closed;
  public interface Result {void loaded(Draft draft);void failed(String message);}
  public static final class Draft {
    public final String id,name,kind,mime,text;
    public final int pages;
    public final long originalBytes;
    private final ArrayList<byte[]> images;
    Draft(String id,String name,String kind,String mime,String text,int pages,long bytes,ArrayList<byte[]> images){this.id=id;this.name=name;this.kind=kind;this.mime=mime;this.text=text;this.pages=pages;this.originalBytes=bytes;this.images=images;}
    public JSONObject payload() throws JSONException {
      JSONObject value=new JSONObject().put("name",name).put("kind",kind).put("mime",mime).put("pages",pages);
      if(!text.isEmpty())value.put("text",text);
      if(!images.isEmpty()){
        JSONArray pictures=new JSONArray();for(byte[] bytes:images)pictures.put(new JSONObject().put("mime","image/jpeg").put("data",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)));
        value.put("images",pictures);
      }
      return value;
    }
    public String summary(){return name+" · "+(kind.equals("pdf")?pages+" halaman":kind.equals("photo")?"foto":text.length()+" karakter");}
    public Bitmap thumbnail(){if(images.isEmpty())return null;BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=16;byte[] data=images.get(0);return BitmapFactory.decodeByteArray(data,0,data.length,opts);}
    public int imageCount(){return images.size();}
    public Bitmap preview(int page){if(page<0||page>=images.size())return null;byte[] data=images.get(page);return BitmapFactory.decodeByteArray(data,0,data.length);}
  }
  public AssistantAttachments(Context context){this.context=context.getApplicationContext();base=new File(context.getFilesDir(),"assistant_attachments");if(!base.exists())base.mkdirs();}
  private File chat(String id){if(id==null||!id.matches("(?:legacy-local|[0-9a-f-]{36})"))throw new IllegalArgumentException("Invalid conversation");return new File(base,id);}
  private File pack(String chat,String id){if(id==null||!id.matches("[0-9a-f-]{36}"))throw new IllegalArgumentException("Invalid attachment");return new File(chat(chat),id);}
  private static byte[] bounded(InputStream input,int maximum)throws IOException {
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
    while((n=input.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>maximum)throw new IOException("Berkas terlalu besar. Batas 8 MB.");out.write(buffer,0,n);}return out.toByteArray();
  }
  private static String safeName(String name){String clean=name==null?"Lampiran":name.replaceAll("[\\p{Cntrl}/\\\\]","_").trim();return clean.isEmpty()?"Lampiran":clean.substring(0,Math.min(100,clean.length()));}
  private String[] metadata(Uri uri)throws IOException {
    if(!"content".equals(uri.getScheme()))throw new IOException("Pilih berkas melalui pemilih Android.");
    String name="Lampiran";long size=-1;
    try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
      if(c!=null&&c.moveToFirst()){int n=c.getColumnIndex(OpenableColumns.DISPLAY_NAME),s=c.getColumnIndex(OpenableColumns.SIZE);if(n>=0)name=c.getString(n);if(s>=0&&!c.isNull(s))size=c.getLong(s);}
    }catch(RuntimeException invalid){throw new IOException("Berkas tidak dapat dibuka.");}
    if(size>MAX_FILE_BYTES)throw new IOException("Berkas terlalu besar. Batas 8 MB.");
    return new String[]{safeName(name),context.getContentResolver().getType(uri)};
  }
  public void load(String chatId,Uri uri,Result result){
    int token=++generation;
    worker.execute(()->{try{
      String[] meta=metadata(uri);byte[] bytes;
      try(InputStream in=context.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Berkas tidak ditemukan.");bytes=bounded(in,MAX_FILE_BYTES);}
      Draft draft=decode(meta[0],meta[1],bytes);
      if(closed||token!=generation)return;
      store(chatId,draft);main.post(()->{if(!closed&&token==generation)result.loaded(draft);});
    }catch(Exception e){String message=e instanceof IOException?e.getMessage():"Berkas rusak atau belum didukung.";main.post(()->{if(!closed&&token==generation)result.failed(message);});}
    finally{if((context.getPackageName()+".assistant.camera").equals(uri.getAuthority()))AssistantCameraProvider.discard(context,uri);}});
  }
  public void restore(String chatId,Result result){
    int token=++generation;
    worker.execute(()->{try{
      File pointer=new File(chat(chatId),"draft.json");Draft draft=null;
      if(pointer.isFile()){JSONObject p=new JSONObject(new String(read(pointer,1024),StandardCharsets.UTF_8));draft=readPack(chatId,p.getString("id"));}
      Draft value=draft;main.post(()->{if(!closed&&token==generation)result.loaded(value);});
    }catch(Exception invalid){main.post(()->{if(!closed&&token==generation)result.failed("Draf lampiran tidak dapat dipulihkan. Pilih ulang berkasnya.");});}});
  }
  private Draft decode(String name,String declared,byte[] bytes)throws Exception {
    if(bytes.length==0)throw new IOException("Berkas kosong.");
    String lower=name.toLowerCase(Locale.ROOT);ArrayList<byte[]> images=new ArrayList<>();String kind,mime,text="";int pages=1;
    boolean jpeg=bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255;
    boolean png=bytes.length>8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
    boolean webp=bytes.length>12&&new String(bytes,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP");
    if(jpeg||png||webp){
      mime=jpeg?"image/jpeg":png?"image/png":"image/webp";
      if(declared!=null&&!declared.equals(mime)&&!declared.equals("application/octet-stream"))throw new IOException("Jenis berkas tidak cocok dengan isinya.");
      kind="photo";images.add(normalizeImage(bytes));
    }else if(lower.endsWith(".pdf")&&bytes.length>5&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")){
      if(declared!=null&&!declared.equals("application/pdf")&&!declared.equals("application/octet-stream"))throw new IOException("Jenis PDF tidak cocok.");
      kind="pdf";mime="application/pdf";File temp=File.createTempFile("assistant-pdf-",".pdf",context.getCacheDir());
      try{try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);}
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(temp,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){
          pages=renderer.getPageCount();if(pages<1||pages>3)throw new IOException("PDF didukung sampai 3 halaman. Pilih bagian yang ingin dibahas.");
          for(int i=0;i<pages;i++){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();try(PdfRenderer.Page page=renderer.openPage(i)){
            float scale=Math.min(1100f/Math.max(1,page.getWidth()),1300f/Math.max(1,page.getHeight()));
            int w=Math.max(1,Math.round(page.getWidth()*scale)),h=Math.max(1,Math.round(page.getHeight()*scale));
            Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);image.eraseColor(Color.WHITE);
            try{page.render(image,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);images.add(compress(image));}finally{image.recycle();}
          }}
        }
      }finally{temp.delete();}
    }else if(lower.endsWith(".docx")&&bytes.length>4&&bytes[0]==80&&bytes[1]==75){
      if(declared!=null&&!declared.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")&&!declared.equals("application/zip")&&!declared.equals("application/octet-stream"))throw new IOException("Jenis DOCX tidak cocok.");
      kind="docx";mime="application/vnd.openxmlformats-officedocument.wordprocessingml.document";text=docx(bytes);
    }else if(lower.endsWith(".txt")&&(declared==null||declared.equals("text/plain")||declared.equals("application/octet-stream"))){
      kind="text";mime="text/plain";
      if(bytes.length>64000)throw new IOException("Teks terlalu panjang. Maksimal 12.000 karakter.");
      try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString().replace("\uFEFF","").trim();}
      catch(CharacterCodingException invalid){throw new IOException("Gunakan TXT dengan pengodean UTF-8.");}
      if(text.indexOf('\0')>=0||text.length()>MAX_TEXT)throw new IOException("Gunakan TXT UTF-8, maksimal 12.000 karakter.");
    }else throw new IOException("Pilih foto JPG/PNG/WebP, PDF, DOCX atau TXT. Format ini belum didukung.");
    if(images.isEmpty()&&text.trim().isEmpty())throw new IOException("Tidak ada teks yang terbaca. Dokumen scan dapat dipilih sebagai PDF.");
    return new Draft(UUID.randomUUID().toString(),safeName(name),kind,mime,text,pages,bytes.length,images);
  }
  private static String docx(byte[] bytes)throws Exception {
    byte[] document=null;int total=0,entries=0;
    try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;
      while((entry=zip.getNextEntry())!=null){if(++entries>200||entry.getName().contains("..")||entry.getName().startsWith("/")||entry.getName().contains("\\"))throw new IOException("Arsip DOCX tidak valid.");
        byte[] value=bounded(zip,2_000_000);total+=value.length;if(total>4_000_000)throw new IOException("Isi DOCX terlalu besar.");
        if(entry.getName().equals("word/document.xml")){if(document!=null)throw new IOException("DOCX tidak valid.");document=value;}
      }
    }
    if(document==null)throw new IOException("Pilih DOCX yang berisi teks.");
    String xml=new String(document,StandardCharsets.UTF_8);if(xml.contains("<!DOCTYPE")||xml.contains("<!ENTITY"))throw new IOException("Isi DOCX tidak didukung.");
    XmlPullParser parser=Xml.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);parser.setInput(new StringReader(xml));
    StringBuilder out=new StringBuilder();boolean text=false;int depth=0;
    for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.next()){
      if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Struktur DOCX terlalu dalam.");text="t".equals(parser.getName());}
      else if(event==XmlPullParser.TEXT&&text){out.append(parser.getText());if(out.length()>MAX_TEXT)throw new IOException("Teks DOCX maksimal 12.000 karakter.");}
      else if(event==XmlPullParser.END_TAG){if("p".equals(parser.getName()))out.append('\n');text=false;depth--;}
    }
    String value=out.toString().trim();if(value.length()>MAX_TEXT)throw new IOException("Teks DOCX maksimal 12.000 karakter.");return value;
  }
  private static byte[] normalizeImage(byte[] bytes)throws Exception {
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
    if(bounds.outWidth<1||bounds.outHeight<1||(long)bounds.outWidth*bounds.outHeight>40_000_000L)throw new IOException("Dimensi foto tidak didukung.");
    BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/opts.inSampleSize>1600)opts.inSampleSize*=2;
    Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(bitmap==null)throw new IOException("Foto tidak dapat dibaca.");
    try{
      Matrix transform=new Matrix();try{
        int orientation=new ExifInterface(new ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
        if(orientation==ExifInterface.ORIENTATION_ROTATE_90)transform.postRotate(90);else if(orientation==ExifInterface.ORIENTATION_ROTATE_180)transform.postRotate(180);else if(orientation==ExifInterface.ORIENTATION_ROTATE_270)transform.postRotate(270);
        else if(orientation==ExifInterface.ORIENTATION_FLIP_HORIZONTAL)transform.postScale(-1,1);else if(orientation==ExifInterface.ORIENTATION_FLIP_VERTICAL)transform.postScale(1,-1);
        else if(orientation==ExifInterface.ORIENTATION_TRANSPOSE){transform.postRotate(90);transform.postScale(-1,1);}else if(orientation==ExifInterface.ORIENTATION_TRANSVERSE){transform.postRotate(270);transform.postScale(-1,1);}
      }catch(IOException ignored){}
      float scale=Math.min(1f,1200f/Math.max(bitmap.getWidth(),bitmap.getHeight()));transform.postScale(scale,scale);
      Bitmap normalized=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),transform,true);
      try{Bitmap opaque=Bitmap.createBitmap(normalized.getWidth(),normalized.getHeight(),Bitmap.Config.ARGB_8888);try{Canvas canvas=new Canvas(opaque);canvas.drawColor(Color.WHITE);canvas.drawBitmap(normalized,0,0,null);return compress(opaque);}finally{opaque.recycle();}}
      finally{if(normalized!=bitmap)normalized.recycle();}
    }finally{bitmap.recycle();}
  }
  private static byte[] compress(Bitmap image)throws IOException {
    for(int quality:new int[]{86,72,58,45}){ByteArrayOutputStream out=new ByteArrayOutputStream();if(!image.compress(Bitmap.CompressFormat.JPEG,quality,out))throw new IOException("Foto gagal disiapkan.");if(out.size()<=360000)return out.toByteArray();}
    throw new IOException("Foto terlalu padat. Pilih gambar yang lebih sederhana.");
  }
  private static byte[] read(File file,int limit)throws IOException{try(InputStream in=new FileInputStream(file)){return bounded(in,limit);}}
  private long size(File directory){long total=0;File[] entries=directory.listFiles();if(entries!=null)for(File file:entries)total+=file.isDirectory()?size(file):file.length();return total;}
  private void store(String chatId,Draft draft)throws Exception {
    long needed=draft.text.getBytes(StandardCharsets.UTF_8).length;for(byte[] data:draft.images)needed+=data.length;
    if(size(base)+needed>MAX_LOCAL_BYTES)throw new IOException("Penyimpanan lampiran penuh (25 MB). Hapus percakapan atau lampiran lama.");
    File oldPointer=new File(chat(chatId),"draft.json");String oldId=null;
    if(oldPointer.isFile())try{oldId=new JSONObject(new String(read(oldPointer,1024),StandardCharsets.UTF_8)).optString("id");}catch(Exception ignored){}
    File directory=pack(chatId,draft.id);if(!directory.mkdirs())throw new IOException("Lampiran belum tersimpan.");
    JSONObject meta=new JSONObject().put("id",draft.id).put("name",draft.name).put("kind",draft.kind).put("mime",draft.mime).put("pages",draft.pages).put("bytes",draft.originalBytes).put("images",draft.images.size());
    try{for(int i=0;i<draft.images.size();i++)try(FileOutputStream out=new FileOutputStream(new File(directory,"image-"+i+".jpg"))){out.write(draft.images.get(i));}
      try(FileOutputStream out=new FileOutputStream(new File(directory,"text.txt"))){out.write(draft.text.getBytes(StandardCharsets.UTF_8));}
      try(FileOutputStream out=new FileOutputStream(new File(directory,"meta.json"))){out.write(meta.toString().getBytes(StandardCharsets.UTF_8));}
      AtomicFile pointer=new AtomicFile(new File(chat(chatId),"draft.json"));FileOutputStream out=pointer.startWrite();try{out.write(new JSONObject().put("id",draft.id).toString().getBytes(StandardCharsets.UTF_8));pointer.finishWrite(out);}catch(Exception error){pointer.failWrite(out);throw error;}
      if(oldId!=null&&!oldId.equals(draft.id)&&oldId.matches("[0-9a-f-]{36}"))erase(pack(chatId,oldId));
    }catch(Exception error){erase(directory);throw error;}
  }
  private Draft readPack(String chatId,String id)throws Exception {
    File directory=pack(chatId,id);JSONObject meta=new JSONObject(new String(read(new File(directory,"meta.json"),2048),StandardCharsets.UTF_8));
    int count=meta.getInt("images");if(count<0||count>3)throw new IOException();ArrayList<byte[]> images=new ArrayList<>();for(int i=0;i<count;i++)images.add(read(new File(directory,"image-"+i+".jpg"),360000));
    return new Draft(id,safeName(meta.getString("name")),meta.getString("kind"),meta.getString("mime"),new String(read(new File(directory,"text.txt"),MAX_TEXT*4),StandardCharsets.UTF_8),meta.getInt("pages"),meta.getLong("bytes"),images);
  }
  public void clearDraft(String chatId,boolean discard) {
    cancel();worker.execute(()->{try{File pointer=new File(chat(chatId),"draft.json");if(discard&&pointer.isFile()){JSONObject p=new JSONObject(new String(read(pointer,1024),StandardCharsets.UTF_8));erase(pack(chatId,p.getString("id")));}pointer.delete();}catch(Exception ignored){}});
  }
  public void deleteConversation(String chatId){worker.execute(()->{File directory=chat(chatId);File[] files=directory.listFiles();if(files!=null)for(File file:files){if(file.isDirectory()&&file.getName().matches("[0-9a-f-]{36}"))erase(file);else if(file.getName().equals("draft.json")||file.getName().equals("draft.json.bak"))file.delete();}directory.delete();});}
  private static void erase(File directory){for(String name:new String[]{"image-0.jpg","image-1.jpg","image-2.jpg","text.txt","meta.json"})new File(directory,name).delete();directory.delete();}
  public void cancel(){generation++;}
  @Override public void close(){closed=true;generation++;worker.shutdown();main.removeCallbacksAndMessages(null);}
}
