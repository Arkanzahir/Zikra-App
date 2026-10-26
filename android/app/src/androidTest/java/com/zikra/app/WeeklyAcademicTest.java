package com.zikra.app;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.zikra.app.data.*;
import com.zikra.app.location.*;
import com.zikra.app.study.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;

/** Synthetic data only. Host must isolate and later restore emulator data and permissions. */
@SuppressWarnings("deprecation")
public final class WeeklyAcademicTest extends InstrumentationTestCase {
  private Context context;
  private Activity page;
  private ZikraDbHelper db;
  private WeeklyClassStore store;
  @Override protected void setUp()throws Exception {
    super.setUp();context=getInstrumentation().getTargetContext();
    assertEquals("Emulator only","1",shell("getprop ro.kernel.qemu").trim());
    assertTrue("Host backup and fixture isolation required",new File(context.getFilesDir(),"study-d04-authorized").isFile());
    db=new ZikraDbHelper(context);store=new WeeklyClassStore(context);
    for(Task task:db.getTasks())db.deleteTask(task.id);
    for(WeeklyClass item:store.all())store.delete(item.id,item.revision);
    StudyReminders.sync(context);
    context.getSharedPreferences("zikra_preferences",0).edit().putBoolean("onboarding_complete",true).putBoolean("shake_enabled",false).commit();
  }
  @Override protected void tearDown()throws Exception {
    try {
      if(page!=null)ui(page::finish);
      for(Task task:db.getTasks())db.deleteTask(task.id);
      for(WeeklyClass item:store.all())store.delete(item.id,item.revision);
      StudyReminders.sync(context);db.close();store.close();
    }finally{super.tearDown();}
  }
  private String shell(String command)throws Exception{try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
  private void ui(Runnable action){getInstrumentation().runOnMainSync(action);getInstrumentation().waitForIdleSync();}
  private Activity launch(Class<? extends Activity> type){page=getInstrumentation().startActivitySync(new Intent(context,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));getInstrumentation().waitForIdleSync();return page;}
  private Object field(String name)throws Exception{Field f=page.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(page);}
  private void invoke(String name){try{Method m=page.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(page);}catch(Exception failure){throw new RuntimeException(failure);}}
  private void recreate()throws Exception{android.app.Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(page.getClass().getName(),null,false);try{ui(page::recreate);page=getInstrumentation().waitForMonitorWithTimeout(monitor,15000);assertNotNull(page);getInstrumentation().waitForIdleSync();}finally{getInstrumentation().removeMonitor(monitor);}}
  private void waitFor(Callable<Boolean> condition,int seconds)throws Exception{long end=SystemClock.elapsedRealtime()+seconds*1000L;while(SystemClock.elapsedRealtime()<end){if(condition.call())return;SystemClock.sleep(150);}fail("Native state did not complete");}
  private WeeklyClass lesson(long id,long revision,int day,int start,int end,int lead){return new WeeklyClass(id,revision,"[UJI] Matematika","A1","Pengajar uji",day,start,end,lead);}
  private Set<String> alarms(String pref){return context.getSharedPreferences(pref,0).getStringSet("ids",Collections.emptySet());}
  private boolean notice(String tag){for(android.service.notification.StatusBarNotification n:context.getSystemService(NotificationManager.class).getActiveNotifications())if(tag.equals(n.getTag()))return true;return false;}
  private String classTag(WeeklyClass item){return "class:"+item.id+"|"+item.revision+"|"+LocationStore.current(context).zone.getId();}
  private Task task(long id){for(Task t:db.getTasks())if(t.id==id)return t;throw new AssertionError("Missing task");}
  private void shot(String name){UiEvidence.capture(getInstrumentation(),"D04-"+name+"-"+(context.getResources().getConfiguration().fontScale>1.9?"200":"100"));}

  public void testV4MigrationPreservesTasksHifzAndReceipts()throws Exception {
    String name="d04-v4-migration.db";context.deleteDatabase(name);
    try(SQLiteDatabase old=context.openOrCreateDatabase(name,0,null)){
      old.execSQL("CREATE TABLE checklist_days(task_date TEXT PRIMARY KEY NOT NULL)");
      old.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,task_date TEXT NOT NULL,title TEXT NOT NULL,category TEXT NOT NULL,completed INTEGER NOT NULL DEFAULT 0,hifz_chapter INTEGER,hifz_name TEXT,hifz_count INTEGER,hifz_first INTEGER,hifz_last INTEGER,due_at INTEGER NOT NULL DEFAULT 0,study_reminder INTEGER NOT NULL DEFAULT 0)");
      old.execSQL("CREATE TABLE assistant_actions(action_id TEXT PRIMARY KEY NOT NULL,task_id INTEGER NOT NULL)");
      old.execSQL("INSERT INTO tasks VALUES(81,'2026-10-26','Tugas lama','Study',0,null,null,null,null,null,1800000000000,1)");
      old.execSQL("INSERT INTO tasks VALUES(82,'2026-10-26','Hafalan lama','Hifz',1,1,'Al-Fatihah',7,2,5,0,0)");
      old.execSQL("INSERT INTO assistant_actions VALUES('preserved-receipt',81)");old.setVersion(4);
    }
    try(ZikraDbHelper upgraded=new ZikraDbHelper(context,name,Clock.systemUTC())){
      assertEquals(5,upgraded.getReadableDatabase().getVersion());List<Task> tasks=upgraded.getTasks();assertEquals(2,tasks.size());
      Task study=tasks.get(0),hifz=tasks.get(1);assertEquals(81,study.id);assertEquals(1800000000000L,study.dueAt);assertTrue(study.reminder);assertEquals("",study.subject);assertEquals(1,study.priority);assertFalse(study.advanceReminder);
      assertTrue(hifz.completed);assertEquals(2,hifz.hifz.firstVerse);assertEquals(5,hifz.hifz.lastVerse);assertTrue(upgraded.assistantProposalApplied("preserved-receipt"));
    }finally{context.deleteDatabase(name);}
  }

  public void testClassStorageConflictStaleDeleteAndBoundAreAtomic(){
    long id=store.save(lesson(0,0,1,480,540,15));WeeklyClass first=store.find(id);
    try{store.save(lesson(0,0,1,500,550,30));fail("Overlap accepted");}catch(WeeklyClassStore.Conflict expected){}
    assertEquals(1,store.all().size());assertEquals(1,store.find(id).revision);
    store.save(new WeeklyClass(id,first.revision,"[UJI] Fisika","B2","",1,600,660,30));
    try{store.save(first);fail("Stale update accepted");}catch(WeeklyClassStore.Stale expected){}
    assertFalse(store.delete(id,first.revision));assertEquals("[UJI] Fisika",store.find(id).title);
    assertTrue(store.delete(id,store.find(id).revision));assertTrue(store.all().isEmpty());
    for(int i=0;i<WeeklyClassStore.MAX_CLASSES;i++)store.save(lesson(0,0,1,i*10,i*10+5,0));
    try{store.save(lesson(0,0,2,480,540,0));fail("Unbounded class storage");}catch(WeeklyClassStore.Full expected){}
    assertEquals(64,store.all().size());
  }

  public void testTaskMetadataSortingAndCalendarSelectionSurviveReopen()throws Exception {
    ZoneId zone=LocationStore.current(context).zone;LocalDate today=LocationStore.today(context);
    long due=today.plusDays(1).atTime(18,0).atZone(zone).toInstant().toEpochMilli();
    long low=db.addAcademicTask("[UJI] Rendah","Fisika",0,due,true,true);
    long high=db.addAcademicTask("[UJI] Tinggi","Matematika",2,due,true,true);
    long later=db.addAcademicTask("[UJI] Lusa","Kimia",2,due+86400000L,false,false);
    assertEquals("Matematika",task(high).subject);assertEquals(2,task(high).priority);assertTrue(task(high).advanceReminder);
    List<Task> ordered=db.getTasks();ordered.sort(TaskOrder.ACADEMIC);assertEquals(high,ordered.get(0).id);assertEquals(low,ordered.get(1).id);
    launch(StudyActivity.class);ui(()->page.findViewById(R.id.study_date).performClick());
    DatePickerDialog date=(DatePickerDialog)field("agendaDateDialog");LocalDate chosen=today.plusDays(1);
    ui(()->{date.updateDate(chosen.getYear(),chosen.getMonthValue()-1,chosen.getDayOfMonth());date.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
    assertEquals(chosen,field("selectedAgendaDay"));recreate();assertEquals(chosen,field("selectedAgendaDay"));shot("calendar-priority");
    assertTrue(db.updateAcademicTask(low,"[UJI] Diubah","Biologi",1,0,false,false));assertEquals("Biologi",task(low).subject);assertFalse(task(low).advanceReminder);
    db.setCompleted(high,true);assertTrue(task(high).completed);db.deleteTask(later);assertEquals(2,db.getTasks().size());
  }

  public void testWeeklyEditorSaveCancelConflictAndDraftRecreation()throws Exception {
    launch(WeeklyClassesActivity.class);ui(()->page.findViewById(R.id.weeklyAdd).performClick());
    AlertDialog editor=(AlertDialog)field("editor");
    AlertDialog initial=editor;
    ui(()->{((EditText)initial.findViewById(R.id.weeklyTitle)).setText("[UJI] Kelas tersimpan");((EditText)initial.findViewById(R.id.weeklyRoom)).setText("Ruang A");((Spinner)initial.findViewById(R.id.weeklyLead)).setSelection(1);});
    recreate();editor=(AlertDialog)field("editor");assertNotNull(editor);assertEquals("[UJI] Kelas tersimpan",((EditText)editor.findViewById(R.id.weeklyTitle)).getText().toString());
    AlertDialog save=editor;ui(()->save.getButton(AlertDialog.BUTTON_POSITIVE).performClick());assertEquals(1,store.all().size());WeeklyClass item=store.all().get(0);assertEquals("Ruang A",item.room);assertEquals(15,item.reminderMinutes);shot("weekly-list");
    ui(()->page.findViewById(R.id.weeklyAdd).performClick());AlertDialog conflict=(AlertDialog)field("editor");
    ui(()->{((EditText)conflict.findViewById(R.id.weeklyTitle)).setText("[UJI] Bentrok");conflict.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
    assertTrue(conflict.isShowing());assertEquals(1,store.all().size());assertTrue(((TextView)conflict.findViewById(R.id.weeklyError)).length()>0);shot("weekly-conflict");ui(()->conflict.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
    ui(()->{ListView list=page.findViewById(R.id.weeklyList);list.performItemClick(list.getChildAt(0),0,item.id);});
    AlertDialog edit=(AlertDialog)field("editor");ui(()->edit.findViewById(R.id.weeklyDelete).performClick());AlertDialog delete=(AlertDialog)field("deleteDialog");AlertDialog decline=delete;ui(()->decline.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());assertEquals(1,store.all().size());
    ui(()->edit.findViewById(R.id.weeklyDelete).performClick());delete=(AlertDialog)field("deleteDialog");AlertDialog confirm=delete;ui(()->confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick());assertTrue(store.all().isEmpty());assertTrue(alarms("study_class_alarms").isEmpty());
  }

  public void testHomeSummaryUsesOnlyTodayAndNeverChangesProgress()throws Exception {
    int day=LocationStore.today(context).getDayOfWeek().getValue();
    store.save(new WeeklyClass(0,0,"[UJI] Kelas hari ini","A1","",day,480,540,0));
    store.save(new WeeklyClass(0,0,"[UJI] Kelas besok","A2","",day%7+1,480,540,0));
    int before=db.getProgress(ZikraDbHelper.CATEGORY_STUDY).completed;
    launch(HomeActivity.class);LinearLayout summary=page.findViewById(R.id.homeClasses);
    assertTrue(text(summary).contains("[UJI] Kelas hari ini"));assertFalse(text(summary).contains("[UJI] Kelas besok"));assertTrue(text(summary).contains("08.00"));
    assertEquals(before,db.getProgress(ZikraDbHelper.CATEGORY_STUDY).completed);shot("home-summary");
  }
  private String text(View view){String value=view instanceof TextView?((TextView)view).getText().toString():"";if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)value+=" "+text(((ViewGroup)view).getChildAt(i));return value;}

  public void testTaskReminderStagesDeduplicateAndCompletionCancels()throws Exception {
    assertTrue("Notification permission required",StudyReminders.permitted(context));
    ZoneId zone=LocationStore.current(context).zone;long now=System.currentTimeMillis();
    long due=now+86400000L-5000;long id=db.addAcademicTask("[UJI] Besok","Matematika",2,due,true,true);
    Intent h1=new Intent().putExtra("task",id).putExtra("due",due).putExtra("stage",1).putExtra("zone",zone.getId());
    StudyReminders.deliver(context,h1);waitFor(()->notice("study:"+id+":1"),10);
    long posted=context.getSharedPreferences("study_alarm_ids",0).getLong("delivered:"+id+":1",0);StudyReminders.deliver(context,h1);assertEquals(posted,context.getSharedPreferences("study_alarm_ids",0).getLong("delivered:"+id+":1",0));
    db.setCompleted(id,true);StudyReminders.sync(context);waitFor(()->!notice("study:"+id+":1"),10);StudyReminders.deliver(context,h1);assertFalse(notice("study:"+id+":1"));
    long today=ZonedDateTime.now(zone).toLocalDate().atTime(23,59,59).atZone(zone).toInstant().toEpochMilli();
    if(today-now>10000&&today-now<3600000){/* This boundary is covered by pure clock tests. */}
    else {long hDue=now+1800000;long h=db.addAcademicTask("[UJI] Hari ini","Fisika",1,hDue,true,true);StudyReminders.deliver(context,new Intent().putExtra("task",h).putExtra("due",hDue).putExtra("stage",2).putExtra("zone",zone.getId()));waitFor(()->notice("study:"+h+":2"),10);db.deleteTask(h);StudyReminders.sync(context);waitFor(()->!notice("study:"+h+":2"),10);}
  }

  public void testActualOfflineClassLeadsAndDueAlarmReachReceivers()throws Exception {
    assertTrue(StudyReminders.permitted(context));assertTrue("Exact permission fixture needed",StudyAlarmPolicy.exact(context));
    assertFalse("No validated network for the actual local alarm case",new com.zikra.app.assistant.AssistantNetwork(context,()->{}).validated());
    ZoneId zone=LocationStore.current(context).zone;ZonedDateTime now=ZonedDateTime.now(zone);
    ZonedDateTime first=now.plusMinutes(16).withSecond(0).withNano(0),second=first.plusMinutes(15);
    if(first.toLocalDate().equals(second.toLocalDate())&&second.getHour()*60+second.getMinute()+5<=1439){
      long a=store.save(lesson(0,0,first.getDayOfWeek().getValue(),first.getHour()*60+first.getMinute(),first.getHour()*60+first.getMinute()+5,15));
      long b=store.save(lesson(0,0,second.getDayOfWeek().getValue(),second.getHour()*60+second.getMinute(),second.getHour()*60+second.getMinute()+5,30));
      WeeklyClass c15=store.find(a),c30=store.find(b);ClassReminders.sync(context);assertEquals(2,alarms("study_class_alarms").size());
      long task=db.addAcademicTask("[UJI] Alarm tenggat","",1,System.currentTimeMillis()+4000,true,false);StudyReminders.sync(context);
      waitFor(()->notice("study:"+task),20);waitFor(()->notice(classTag(c15))&&notice(classTag(c30)),80);
      assertEquals(0,db.getProgress(ZikraDbHelper.CATEGORY_STUDY).completed);
      JSONObjectReport.write(context,"D04-actual-alarms.json","actualAlarmManagerDelivery",true);
    }else fail("Choose a test time with both classes on the same day");
  }

  public void testClassAlarmRevisionZoneAndDisabledGuards()throws Exception {
    assertTrue(StudyReminders.permitted(context));ZoneId zone=LocationStore.current(context).zone;
    ZonedDateTime start=ZonedDateTime.now(zone).plusMinutes(16).withSecond(0).withNano(0);int minute=start.getHour()*60+start.getMinute();
    long id=store.save(lesson(0,0,start.getDayOfWeek().getValue(),minute,minute+1,30));WeeklyClass old=store.find(id);
    String signature=id+"|"+old.revision+"|"+zone.getId();long millis=start.toInstant().toEpochMilli();
    Intent intent=new Intent().putExtra("signature",signature).putExtra("start",millis).putExtra("trigger",millis-1800000);
    ClassReminders.deliver(context,intent);waitFor(()->notice(classTag(old)),10);
    long delivered=context.getSharedPreferences("study_class_alarms",0).getLong("delivered:"+signature,0);ClassReminders.deliver(context,intent);assertEquals(delivered,context.getSharedPreferences("study_class_alarms",0).getLong("delivered:"+signature,0));
    store.save(new WeeklyClass(id,old.revision,"[UJI] Diubah","","",old.weekday,old.startMinute,old.endMinute,0));ClassReminders.sync(context);waitFor(()->!notice(classTag(old)),10);
    ClassReminders.deliver(context,intent);assertFalse(notice(classTag(old)));assertTrue(alarms("study_class_alarms").isEmpty());
    WeeklyClass disabled=store.find(id);store.save(new WeeklyClass(id,disabled.revision,"[UJI] Zona","","",old.weekday,old.startMinute,old.endMinute,30));
    Intent wrongZone=new Intent().putExtra("signature",id+"|"+store.find(id).revision+"|UTC").putExtra("start",millis).putExtra("trigger",millis-1800000);
    ClassReminders.deliver(context,wrongZone);assertFalse(notice(classTag(store.find(id))));
  }

  public void testAlarmBoundKeepsAllTasksAndPromotesTheNextOne(){
    assertTrue(StudyReminders.permitted(context));List<Long> ids=new ArrayList<>();long due=System.currentTimeMillis()+3600000;
    for(int i=0;i<130;i++)ids.add(db.addAcademicTask("[UJI] Tugas "+i,"",1,due+i*60000,false,false));
    for(int i=0;i<ids.size();i++)db.updateAcademicTask(ids.get(i),"[UJI] Tugas "+i,due+i*60000,true);
    StudyReminders.sync(context);assertEquals(130,db.getTasks().size());assertEquals(128,alarms("study_alarm_ids").size());assertTrue(context.getSharedPreferences("study_alarm_ids",0).getBoolean("limited",false));assertFalse(alarms("study_alarm_ids").contains(ids.get(128)+":0"));
    db.setCompleted(ids.get(0),true);StudyReminders.sync(context);assertTrue(alarms("study_alarm_ids").contains(ids.get(128)+":0"));assertEquals(130,db.getTasks().size());
  }

  public void testDeniedNotificationKeepsRecordsWithoutScheduling()throws Exception {
    assertFalse("Host revokes notification permission before this isolated case",StudyReminders.permitted(context));
    int day=LocationStore.today(context).getDayOfWeek().getValue();long id=store.save(lesson(0,0,day,480,540,15));
    long task=db.addAcademicTask("[UJI] Izin ditolak","Matematika",1,System.currentTimeMillis()+3600000,true,true);
    StudyReminders.sync(context);assertTrue(alarms("study_class_alarms").isEmpty());assertTrue(alarms("study_alarm_ids").isEmpty());assertNotNull(store.find(id));assertEquals("[UJI] Izin ditolak",task(task).title);
    launch(WeeklyClassesActivity.class);assertTrue(((TextView)page.findViewById(R.id.weeklyHint)).getText().toString().contains("belum diizinkan"));shot("permission-denied");
  }
  private static final class JSONObjectReport {
    static void write(Context context,String name,String field,boolean value)throws Exception{
      File dir=new File(context.getExternalFilesDir(null),"validation");if(!dir.exists())assertTrue(dir.mkdirs());
      try(FileOutputStream out=new FileOutputStream(new File(dir,name))){out.write(new org.json.JSONObject().put(field,value).put("physicalPhone",false).put("networkRequired",false).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
  }
}
