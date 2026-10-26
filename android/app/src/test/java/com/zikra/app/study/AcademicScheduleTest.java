package com.zikra.app.study;

import com.zikra.app.data.Task;
import com.zikra.app.data.WeeklyClass;
import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AcademicScheduleTest {
  private static final ZoneId JAKARTA=ZoneId.of("Asia/Jakarta");
  private static long at(String date){return LocalDateTime.parse(date).atZone(JAKARTA).toInstant().toEpochMilli();}
  private static WeeklyClass lesson(int day,int start,int end,int lead){return new WeeklyClass(1,1,"Matematika","A1","",day,start,end,lead);}

  @Test public void nextWeeklyReminderUsesCityClockAndRollsOnlyAfterTheLeadPasses(){
    WeeklyClass item=lesson(1,8*60,9*60,15);
    assertEquals(at("2026-10-26T07:45"),item.nextReminder(at("2026-10-26T07:44"),JAKARTA));
    assertEquals(at("2026-11-02T07:45"),item.nextReminder(at("2026-10-26T07:46"),JAKARTA));
    assertEquals(at("2026-11-02T07:45"),item.nextReminder(at("2026-10-26T07:45"),JAKARTA));
    assertEquals(0,lesson(1,480,540,0).nextReminder(at("2026-10-25T12:00"),JAKARTA));
    assertEquals(at("2026-10-26T07:30"),lesson(1,480,540,30).nextReminder(at("2026-10-25T12:00"),JAKARTA));
  }
  @Test public void midnightLeadAndZoneChangesKeepTheClassDay(){
    WeeklyClass item=lesson(1,10,60,30);
    assertEquals(at("2026-10-25T23:40"),item.nextReminder(at("2026-10-25T23:00"),JAKARTA));
    ZoneId jayapura=ZoneId.of("Asia/Jayapura");
    assertEquals(2*3600000L,item.nextReminder(at("2026-10-25T12:00"),JAKARTA)-item.nextReminder(at("2026-10-25T12:00"),jayapura));
  }
  @Test public void overlapsRejectOnlyIntersectingTimesOnTheSameDay(){
    WeeklyClass item=lesson(1,480,540,0);
    assertTrue(item.overlaps(lesson(1,500,550,0)));
    assertFalse(item.overlaps(lesson(1,540,600,0)));
    assertFalse(item.overlaps(lesson(2,480,540,0)));
  }
  @Test public void rejectsInvalidDaysRangesLeadAndControlCharacters(){
    Runnable[] invalid={()->lesson(0,480,540,15),()->lesson(8,480,540,15),()->lesson(1,540,480,15),()->lesson(1,480,480,15),()->lesson(1,480,1440,15),()->lesson(1,-1,540,15),()->lesson(1,480,540,10),()->new WeeklyClass(0,0," ","","",1,1,2,0),()->new WeeklyClass(0,0,"A\nB","","",1,1,2,0),()->WeeklyClass.label("x".repeat(101),100,false)};
    for(Runnable action:invalid){try{action.run();fail("Invalid class accepted");}catch(IllegalArgumentException expected){}}
  }
  @Test public void deadlineStagesUseLocalDayAndNeverAlertBeforeTheirTrigger(){
    long due=at("2026-10-28T16:00");
    assertEquals(due,DeadlinePlan.at(due,0,JAKARTA));
    assertEquals(at("2026-10-27T16:00"),DeadlinePlan.at(due,1,JAKARTA));
    assertEquals(at("2026-10-28T15:00"),DeadlinePlan.at(due,2,JAKARTA));
    assertFalse(DeadlinePlan.deliverable(due,1,JAKARTA,at("2026-10-27T15:59")));
    assertTrue(DeadlinePlan.deliverable(due,1,JAKARTA,at("2026-10-27T16:01")));
    assertFalse(DeadlinePlan.deliverable(due,1,JAKARTA,at("2026-10-28T00:00")));
    assertFalse(DeadlinePlan.deliverable(due,2,JAKARTA,due));
    assertTrue(DeadlinePlan.deliverable(due,0,JAKARTA,due));
    assertFalse(DeadlinePlan.deliverable(due,0,JAKARTA,due+86400000L));
  }
  @Test public void earlyDeadlineUsesMidnightAndDstDayIsNotAssumedToBe24Hours(){
    assertEquals(at("2026-10-28T00:00"),DeadlinePlan.at(at("2026-10-28T00:30"),2,JAKARTA));
    long midnight=at("2026-10-28T00:00");assertFalse(DeadlinePlan.deliverable(midnight,2,JAKARTA,midnight));
    ZoneId berlin=ZoneId.of("Europe/Berlin");long due=ZonedDateTime.of(2026,3,29,12,0,0,0,berlin).toInstant().toEpochMilli();
    assertEquals(23*3600000L,due-DeadlinePlan.at(due,1,berlin));
  }
  @Test public void orderingPrioritizesDeadlineThenPriorityWithStableIds(){
    List<Task> tasks=new ArrayList<>(Arrays.asList(task(4,0,2),task(3,2000,0),task(2,1000,0),task(1,1000,2)));
    tasks.sort(TaskOrder.ACADEMIC);assertEquals(Arrays.asList(1L,2L,3L,4L),Arrays.asList(tasks.get(0).id,tasks.get(1).id,tasks.get(2).id,tasks.get(3).id));
  }
  private Task task(long id,long due,int priority){return new Task(id,"Tugas","Study",false,null,due,false,"Fisika",priority,false);}
}
