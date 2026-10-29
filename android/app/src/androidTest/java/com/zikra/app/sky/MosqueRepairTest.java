package com.zikra.app.sky;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.*;
import com.zikra.app.*;
import com.zikra.app.location.*;
import com.zikra.app.mosque.*;
import java.io.*;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;

/** Only the isolated project emulator, with public location and a verified backup. */
public final class MosqueRepairTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private final Map<String,Map<String,?>> saved=new HashMap<>();
  @Override protected void setUp() throws Exception {
    super.setUp();context=getInstrumentation().getTargetContext();
    assertTrue(android.os.Build.FINGERPRINT.contains("generic")||android.os.Build.MODEL.contains("sdk"));
    assertTrue(new File(context.getFilesDir(),"mosque-repair-emulator-authorized").isFile());
    for(String name:new String[]{"zikra_preferences",LocationStore.PREFS,"zikra_mosques_osm"})saved.put(name,new HashMap<>(context.getSharedPreferences(name,0).getAll()));
    context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true).putBoolean("reminder_enabled",false).putBoolean("shake_enabled",false).commit();
    LocationPoint p=LocationPoint.INITIAL;
    context.getSharedPreferences(LocationStore.PREFS,0).edit().clear().putLong("latitude",Double.doubleToLongBits(p.latitude))
        .putLong("longitude",Double.doubleToLongBits(p.longitude)).putString("zone",p.zone.getId())
        .putString("label","Sukolilo, Surabaya · acuan publik").putString("source","manual").putLong("selected_at",0).putFloat("accuracy",-1).commit();
  }
  private void main(Runnable action){
    java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
    getInstrumentation().runOnMainSync(()->{try{action.run();}catch(Throwable e){error.set(e);}});
    getInstrumentation().waitForIdleSync();if(error.get()!=null)throw new AssertionError(error.get());
  }
  private static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
  private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
  private void until(String label,java.util.function.BooleanSupplier condition,long seconds){
    long end=SystemClock.uptimeMillis()+seconds*1000;
    while(SystemClock.uptimeMillis()<end){getInstrumentation().waitForIdleSync();if(condition.getAsBoolean())return;SystemClock.sleep(100);}fail(label);
  }
  private void save(String name,Bitmap image)throws Exception{
    File folder=new File(context.getExternalFilesDir(null),"mosque-repair");assertTrue(folder.isDirectory()||folder.mkdirs());
    try(FileOutputStream stream=new FileOutputStream(new File(folder,name+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,stream));}
  }
  private void screenshot(String name)throws Exception{getInstrumentation().waitForIdleSync();SystemClock.sleep(400);Bitmap image=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);try{save(name,image);}finally{image.recycle();}}

  public void testInteriorClipDayOffAndHomeNightRender() throws Exception {
    Bitmap base=Bitmap.createBitmap(960,640,Bitmap.Config.ARGB_8888);base.eraseColor(0xFF172524);
    Bitmap dark=base.copy(Bitmap.Config.ARGB_8888,true),lit=base.copy(Bitmap.Config.ARGB_8888,true),half=base.copy(Bitmap.Config.ARGB_8888,true);
    final MosqueInteriorLights[] lights=new MosqueInteriorLights[1];
    main(()->{lights[0]=new MosqueInteriorLights(context);lights[0].draw(new Canvas(dark),0);lights[0].draw(new Canvas(lit),1);lights[0].draw(new Canvas(half),.5f);});
    Bitmap band=(Bitmap)field(lights[0],"nightBand");assertNotNull("Approved doorway texture decoded",band);
    assertEquals(703,band.getWidth());assertEquals(180,band.getHeight());
    assertTrue("Only the cropped band is retained",band.getAllocationByteCount()<600000);
    assertTrue("No interior light during day",base.sameAs(dark));
    assertTrue("Warm light within main arch",Color.red(lit.getPixel(662,460))-Color.red(base.getPixel(662,460))>100);
    assertTrue("Twilight blends the doorway gradually",Color.red(half.getPixel(662,460))>Color.red(base.getPixel(662,460))
        &&Color.red(half.getPixel(662,460))<Color.red(lit.getPixel(662,460)));
    for(int y=0;y<390;y+=30)for(int x=0;x<960;x+=30)
      assertEquals("Reference sky is never drawn",base.getPixel(x,y),lit.getPixel(x,y));
    assertEquals("Stone pillar stays outside interior clip",base.getPixel(617,460),lit.getPixel(617,460));
    assertEquals("Occluded right opening stays unpainted below foliage",base.getPixel(926,490),lit.getPixel(926,490));
    base.recycle();dark.recycle();lit.recycle();half.recycle();
    for(int hour:new int[]{12,21}){
      final HomeSkyView[] sky=new HomeSkyView[1];Bitmap image=Bitmap.createBitmap(1080,1100,Bitmap.Config.ARGB_8888);
      main(()->{sky[0]=new HomeSkyView(context,null);sky[0].setSky(SkyDay.estimate(LocalDate.of(2026,10,29),LocationPoint.INITIAL),LocalTime.of(hour,0));sky[0].measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1100,View.MeasureSpec.EXACTLY));sky[0].layout(0,0,1080,1100);sky[0].draw(new Canvas(image));});
      save("home-"+hour,image);image.recycle();
    }
  }

  public void testLivePlacesListTilesAndStaleFallback() throws Exception {
    context.getSharedPreferences("zikra_mosques_osm",0).edit().clear().commit();
    page=getInstrumentation().startActivitySync(new Intent(context,NearbyMosquesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    main(()->page.findViewById(R.id.mosquesRefresh).performClick());
    until("Live search completed",()->page.findViewById(R.id.mosquesProgress).getVisibility()!=View.VISIBLE,65);
    LinearLayout list=page.findViewById(R.id.mosquesResults);assertTrue("Real places available",list.getChildCount()>0);
    assertTrue(((TextView)page.findViewById(R.id.mosquesUpdated)).getText().toString().startsWith("Diperbarui"));screenshot("live-list");
    main(()->((RadioGroup)page.findViewById(R.id.mosquesViewMode)).check(R.id.mosquesMapToggle));
    until("Real map tiles downloaded",()->{try{return (Boolean)field(page,"mapHasTiles");}catch(Exception e){return false;}},35);
    until("Loading notice cleared",()->page.findViewById(R.id.mosquesMapNotice).getVisibility()==View.GONE,5);
    org.osmdroid.views.MapView map=(org.osmdroid.views.MapView)field(page,"mapView");assertTrue(map.getOverlays().size()>1);screenshot("live-map");
    main(()->((RadioGroup)page.findViewById(R.id.mosquesViewMode)).check(R.id.mosquesListToggle));
    context.getSharedPreferences("zikra_mosques_osm",0).edit().putLong("fetched_at",System.currentTimeMillis()-2*3600000L).putLong("retry_until",System.currentTimeMillis()+60000).commit();
    set(page,"nextAllowedSearch",0L);main(()->page.findViewById(R.id.mosquesRefresh).performClick());
    until("Stale fallback completed",()->page.findViewById(R.id.mosquesProgress).getVisibility()!=View.VISIBLE,5);
    assertTrue(((TextView)page.findViewById(R.id.mosquesStatus)).getText().toString().startsWith("Menampilkan hasil tersimpan"));
    assertTrue(((LinearLayout)page.findViewById(R.id.mosquesResults)).getChildCount()>0);screenshot("stale-list");
    SharedPreferences cache=context.getSharedPreferences("zikra_mosques_osm",0);
    try(MosqueSearch search=new MosqueSearch(context)){
      LocationPoint origin=LocationStore.current(context);assertTrue(search.find(origin,true).stale);
      LocationPoint other=new LocationPoint(-6.1754,106.8272,"Asia/Jakarta","Jakarta · acuan publik","manual",0,-1);
      try{search.find(other,true);fail("Other location must not inherit cached places");}catch(MosqueSearch.RateLimited expected){}
      cache.edit().putLong("fetched_at",System.currentTimeMillis()-8*24*3600000L).commit();
      try{search.find(origin,true);fail("Expired places must not be presented");}catch(MosqueSearch.RateLimited expected){}
    }
  }
  @SuppressWarnings("unchecked") @Override protected void tearDown()throws Exception{
    if(page!=null&&!page.isDestroyed())main(page::finish);
    for(Map.Entry<String,Map<String,?>> group:saved.entrySet()){
      SharedPreferences.Editor e=context.getSharedPreferences(group.getKey(),0).edit().clear();
      for(Map.Entry<String,?> item:group.getValue().entrySet()){
        Object v=item.getValue();String k=item.getKey();
        if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Long)e.putLong(k,(Long)v);
        else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
        else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));
      }e.commit();
    }super.tearDown();
  }
}
