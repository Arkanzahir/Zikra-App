package com.zikra.app;

import static org.junit.Assert.*;
import com.zikra.app.location.LocationPoint;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.json.*;
import org.junit.Test;

public final class PrayerReferenceTest {
  @Test public void matchesSeparateAstralImplementationAcrossCitiesSeasonsAndLeapDay()throws Exception {
    try(InputStream input=getClass().getResourceAsStream("/prayer-d01-astral.json")){
      assertNotNull(input);JSONObject root=new JSONObject(new String(input.readAllBytes(),StandardCharsets.UTF_8));
      JSONArray rows=root.getJSONArray("rows");assertEquals(45,rows.length());
      for(int index=0;index<rows.length();index++){
        JSONObject row=rows.getJSONObject(index);LocationPoint point=new LocationPoint(row.getDouble("latitude"),row.getDouble("longitude"),row.getString("zone"),row.getString("city"),"manual",0,-1);
        LocalDate date=LocalDate.parse(row.getString("date"));PrayerSchedule.Day actual=PrayerSchedule.forDate(date,point);
        for(int event=0;event<6;event++){
          String label=point.label+" "+date+" "+PrayerSchedule.NAMES[event];JSONArray expected=row.getJSONArray("times");
          if(expected.isNull(event)){assertNull(label,actual.at(event));continue;}
          assertNotNull(label,actual.at(event));long difference=Math.abs(Duration.between(LocalTime.parse(expected.getString(event)),actual.at(event)).toMinutes());
          assertTrue(label+": "+actual.time(event)+" vs "+expected.getString(event),Math.min(difference,1440-difference)<=root.getInt("toleranceMinutes"));
        }
      }
    }
  }
}
