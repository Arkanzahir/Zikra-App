package com.zikra.app.study;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import com.zikra.app.R;

/** Local terminology preference; changing it never rewrites classes or task records. */
public final class EducationLevel {
  private EducationLevel() {}
  public static int selected(Context context) {
    String value=context.getSharedPreferences("zikra_preferences",0).getString("education_level","");
    String[] choices=context.getResources().getStringArray(R.array.study_levels);
    for(int i=0;i<choices.length;i++)if(choices[i].equals(value))return i;
    return -1;
  }
  public static boolean college(Context context) { return selected(context)==2; }
  public static boolean school(Context context) { int value=selected(context);return value==0||value==1; }
  public static String label(Context context) {
    int value=selected(context);
    return value<0?context.getString(R.string.study_level_choose):context.getResources().getStringArray(R.array.study_levels)[value];
  }
  public static void save(Context context,int choice) {
    String[] values=context.getResources().getStringArray(R.array.study_levels);
    if(choice<0||choice>=values.length)throw new IllegalArgumentException("Invalid education level");
    context.getSharedPreferences("zikra_preferences",0).edit().putString("education_level",values[choice]).apply();
  }
  public static AlertDialog choose(Activity owner,Runnable changed) {
    int[] draft={selected(owner)};
    AlertDialog dialog=new AlertDialog.Builder(owner).setTitle(R.string.study_level_choose)
        .setSingleChoiceItems(R.array.study_levels,draft[0],(view,choice)->draft[0]=choice)
        .setNegativeButton(R.string.weekly_cancel,null)
        .setPositiveButton(R.string.weekly_save,null).create();
    dialog.setOnShowListener(view->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
      if(draft[0]<0)return;
      save(owner,draft[0]);dialog.dismiss();changed.run();
    }));
    dialog.show();return dialog;
  }
  public static int courseLabel(Context context) {
    return college(context)?R.string.study_course_college:school(context)?R.string.study_course_school:R.string.weekly_course;
  }
  public static int teacherLabel(Context context) {
    return college(context)?R.string.study_teacher_college:school(context)?R.string.study_teacher_school:R.string.weekly_teacher;
  }
}
