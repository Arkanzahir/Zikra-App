package com.zikra.app.assistant;

import com.zikra.app.AiActivity;
import java.lang.reflect.Field;

/** Older context/retry suites explicitly choose the optional review path. */
public final class AssistantReviewFixture {
  private AssistantReviewFixture() {}
  public static void click(AiActivity activity,int viewId) {
    try {
      Field waiting=AiActivity.class.getDeclaredField("waiting");waiting.setAccessible(true);
      if(!waiting.getBoolean(activity)) {
        String endpoint=new AssistantConnection(activity).endpoint();
        if(!endpoint.isEmpty())AssistantConsent.accept(activity.getSharedPreferences("zikra_preferences",0),endpoint);
        Field review=AiActivity.class.getDeclaredField("reviewNextSend");review.setAccessible(true);review.setBoolean(activity,true);
      }
      activity.findViewById(viewId).performClick();
    }catch(ReflectiveOperationException error){throw new AssertionError(error);}
  }
}
