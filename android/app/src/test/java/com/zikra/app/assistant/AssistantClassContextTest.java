package com.zikra.app.assistant;

import static org.junit.Assert.*;
import com.zikra.app.data.WeeklyClass;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;

public class AssistantClassContextTest {
  private WeeklyClass item(int day,int start,String title) {
    return new WeeklyClass(day*1000+start,1,title,"PRIVATE-ROOM","PRIVATE-TEACHER",day,start,start+30,15,3);
  }
  @Test public void nextSevenDatesWrapWeekAndYearInSelectedZoneWithoutPrivateMetadata() throws Exception {
    JSONObject snapshot=AssistantClassContext.snapshot(List.of(item(1,480,"Monday"),item(4,600,"Thursday")),
        LocalDate.of(2026,12,31),ZoneId.of("Asia/Makassar"));
    assertEquals("2027-01-06",snapshot.getString("throughDate"));
    assertEquals("Asia/Makassar",snapshot.getString("zone"));
    assertEquals("2026-12-31",snapshot.getJSONArray("items").getJSONObject(0).getString("date"));
    assertEquals("2027-01-04",snapshot.getJSONArray("items").getJSONObject(1).getString("date"));
    assertEquals("10:00",snapshot.getJSONArray("items").getJSONObject(0).getString("start"));
    String json=snapshot.toString();for(String hidden:List.of("PRIVATE","teacher","room","credits","reminder","revision","\"id\""))assertFalse(json.contains(hidden));
  }
  @Test public void capAndOmittedCountAreVisibleWithoutMutatingStoreOrderOrLeakingHiddenTitles() throws Exception {
    List<WeeklyClass> rows=new ArrayList<>();for(int i=0;i<12;i++)rows.add(item(4,100+i*40,"Class "+i));
    java.util.Collections.reverse(rows);
    JSONObject snapshot=AssistantClassContext.snapshot(rows,LocalDate.of(2026,10,29),ZoneId.of("Asia/Jakarta"));
    assertEquals(10,snapshot.getJSONArray("items").length());assertEquals(2,snapshot.getInt("omitted"));
    assertEquals("Class 11",rows.get(0).title);assertFalse(snapshot.toString().contains("Class 11"));
    String preview=AssistantSelection.preview(new JSONObject().put("context",new JSONObject().put("classes",snapshot)));
    assertTrue(preview.contains("2 kelas lain tidak disertakan"));assertFalse(preview.contains("Class 11"));
  }
  @Test public void emptyScheduleIsExplicitAndSnapshotDoesNotFollowLaterStoreEdits() throws Exception {
    List<WeeklyClass> rows=new ArrayList<>(List.of(item(4,480,"Original")));
    JSONObject snapshot=AssistantClassContext.snapshot(rows,LocalDate.of(2026,10,29),ZoneId.of("Asia/Jakarta"));
    rows.clear();rows.add(item(4,480,"Changed"));assertFalse(snapshot.toString().contains("Changed"));
    JSONObject empty=AssistantClassContext.snapshot(List.of(),LocalDate.of(2026,10,29),ZoneId.of("Asia/Jakarta"));
    assertEquals(0,empty.getJSONArray("items").length());assertEquals(0,empty.getInt("omitted"));
    assertTrue(AssistantSelection.preview(new JSONObject().put("context",new JSONObject().put("classes",empty))).contains("Belum ada kelas tersimpan"));
  }
}
