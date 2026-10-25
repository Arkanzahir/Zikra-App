package com.zikra.app.assistant;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Explicit URI grants expose only a random temporary camera capture, never arbitrary app files. */
public final class AssistantCameraProvider extends ContentProvider {
  private File file(Uri uri) {
    if(!"capture".equals(uri.getPathSegments().size()==2?uri.getPathSegments().get(0):""))throw new SecurityException("Invalid capture URI");
    String name=uri.getLastPathSegment();
    if(name==null||!name.matches("[0-9a-f-]{36}\\.jpg"))throw new SecurityException("Invalid capture name");
    File directory=new File(getContext().getCacheDir(),"assistant_camera");
    if(!directory.exists()&&!directory.mkdirs())throw new IllegalStateException("Capture storage unavailable");
    return new File(directory,name);
  }
  public static Uri create(Context context) {
    return new Uri.Builder().scheme("content").authority(context.getPackageName()+".assistant.camera")
        .appendPath("capture").appendPath(java.util.UUID.randomUUID()+".jpg").build();
  }
  public static void discard(Context context,Uri uri) {
    if(uri==null||!(context.getPackageName()+".assistant.camera").equals(uri.getAuthority()))return;
    String name=uri.getLastPathSegment();
    if(name!=null&&name.matches("[0-9a-f-]{36}\\.jpg"))new File(new File(context.getCacheDir(),"assistant_camera"),name).delete();
    context.revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  }
  @Override public boolean onCreate(){return true;}
  @Override public String getType(Uri uri){file(uri);return "image/jpeg";}
  @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {
    if(!java.util.Arrays.asList("r","w","wt","rw","rwt").contains(mode))throw new FileNotFoundException("Unsupported capture access");
    return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.parseMode(mode));
  }
  @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order) {
    File file=file(uri);MatrixCursor cursor=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});
    cursor.addRow(new Object[]{"Foto kamera.jpg",file.exists()?file.length():0});return cursor;
  }
  @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
  @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
  @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
