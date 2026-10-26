package com.zikra.app.study;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zikra.app.R;
import com.zikra.app.WeeklyClassesActivity;
import com.zikra.app.data.WeeklyClass;
import com.zikra.app.data.WeeklyClassStore;
import com.zikra.app.location.LocationStore;
import java.util.ArrayList;
import java.util.List;

/** Home reads the same local records and city day as the weekly editor. */
public final class ClassSummary {
  private ClassSummary() {}
  public static void bind(Context context, LinearLayout container) {
    container.removeAllViews();
    List<WeeklyClass> today = new ArrayList<>();
    int weekday = LocationStore.today(context).getDayOfWeek().getValue();
    try (WeeklyClassStore store = new WeeklyClassStore(context)) {
      for (WeeklyClass item : store.all()) if (item.weekday == weekday) today.add(item);
    } catch (android.database.sqlite.SQLiteException unavailable) {
      container.addView(text(context,context.getString(R.string.weekly_read_failed),14,false));
      return;
    }
    if (today.isEmpty()) {
      container.addView(text(context,context.getString(R.string.weekly_empty),14,false));return;
    }
    for (int i=0;i<Math.min(3,today.size());i++) {
      WeeklyClass item=today.get(i);
      LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.VERTICAL);
      row.setPadding(dp(context,14),dp(context,8),dp(context,14),dp(context,8));
      TextView title=text(context,item.title,15,true);title.setPadding(0,0,0,dp(context,5));
      row.addView(title);
      TextView detail=text(context,WeeklyClassesActivity.clock(item.startMinute)+"–"
          +WeeklyClassesActivity.clock(item.endMinute)+(item.room.isEmpty()?"":" · "+item.room),13,false);
      detail.setPadding(0,0,0,0);row.addView(detail);container.addView(row);
      if(i<Math.min(3,today.size())-1){View line=new View(context);line.setBackgroundColor(context.getColor(R.color.concept_line));container.addView(line,new LinearLayout.LayoutParams(-1,dp(context,1)));}
    }
    if(today.size()>3)container.addView(text(context,context.getString(R.string.weekly_more,today.size()-3),13,false));
  }
  private static TextView text(Context context,String value,int sp,boolean title) {
    TextView text=new TextView(context);text.setText(value);text.setTextSize(sp);
    text.setTextColor(context.getColor(title?R.color.pine:R.color.muted));
    if(title)text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
    text.setPadding(dp(context,14),dp(context,12),dp(context,14),dp(context,12));return text;
  }
  private static int dp(Context context,int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
}
