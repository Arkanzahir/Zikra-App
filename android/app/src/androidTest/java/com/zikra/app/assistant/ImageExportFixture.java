package com.zikra.app.assistant;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** Synthetic save destination in the separately installed, disposable test helper only. */
public final class ImageExportFixture extends ContentProvider {
  private File file(Uri uri){if(!"/synthetic.png".equals(uri.getPath()))throw new SecurityException();return new File(getContext().getCacheDir(),"image-c09-export.png");}
  @Override public boolean onCreate(){return true;}
  @Override public String getType(Uri uri){file(uri);return "image/png";}
  @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.parseMode(mode));}
  @Override public Cursor query(Uri uri,String[] p,String s,String[] a,String o){return null;}
  @Override public Uri insert(Uri uri,ContentValues value){throw new UnsupportedOperationException();}
  @Override public int update(Uri uri,ContentValues value,String where,String[] args){throw new UnsupportedOperationException();}
  @Override public int delete(Uri uri,String where,String[] args){return file(uri).delete()?1:0;}
}
