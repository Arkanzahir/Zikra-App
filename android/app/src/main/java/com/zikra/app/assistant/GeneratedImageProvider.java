package com.zikra.app.assistant;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only URI grants are issued only when the user chooses Share. */
public final class GeneratedImageProvider extends ContentProvider {
  private File file(Uri uri){if(uri.getPathSegments().size()!=2)throw new SecurityException();String name=uri.getLastPathSegment();if(name==null||!name.matches("[0-9a-f-]{36}\\.(?:jpg|png)"))throw new SecurityException();return GeneratedImages.imageFile(getContext(),uri.getPathSegments().get(0),name.substring(0,36),name.endsWith(".png")?"image/png":"image/jpeg");}
  @Override public boolean onCreate(){return true;}
  @Override public String getType(Uri uri){return file(uri).getName().endsWith(".png")?"image/png":"image/jpeg";}
  @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
  @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){File file=file(uri);MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});c.addRow(new Object[]{"Zikra-Ilustrasi."+(file.getName().endsWith(".png")?"png":"jpg"),file.length()});return c;}
  @Override public Uri insert(Uri uri,ContentValues value){throw new UnsupportedOperationException();}
  @Override public int update(Uri uri,ContentValues value,String where,String[] args){throw new UnsupportedOperationException();}
  @Override public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException();}
}
