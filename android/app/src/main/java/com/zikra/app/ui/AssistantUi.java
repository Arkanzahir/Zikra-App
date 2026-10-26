package com.zikra.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zikra.app.R;
import java.util.function.IntConsumer;

/** Presentation only; attachment access and consent remain owned by the activity. */
public final class AssistantUi {
  private AssistantUi() {}

  public static int dp(Context context, float value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }

  public static GradientDrawable surface(Context context, int color, float radius, boolean border) {
    GradientDrawable background = new GradientDrawable();
    background.setColor(context.getColor(color));
    background.setCornerRadius(dp(context, radius));
    if (border) background.setStroke(dp(context, 1), context.getColor(R.color.ai_refresh_line));
    return background;
  }

  public static ImageButton icon(Context context, int drawable, String label, Runnable action) {
    ImageButton button = new ImageButton(context);
    button.setImageResource(drawable);
    button.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.ai_refresh_ink)));
    button.setContentDescription(label);
    button.setPadding(dp(context, 13), dp(context, 13), dp(context, 13), dp(context, 13));
    android.util.TypedValue value = new android.util.TypedValue();
    context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true);
    button.setBackgroundResource(value.resourceId);
    button.setOnClickListener(view -> action.run());
    button.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 48), dp(context, 48)));
    return button;
  }

  public static void bottomSheet(AlertDialog dialog, Context context) {
    dialog.show();
    Window window = dialog.getWindow();
    if (window == null) return;
    window.setBackgroundDrawableResource(android.R.color.transparent);
    window.setGravity(Gravity.BOTTOM);
    window.setLayout(-1, -2);
    window.getDecorView().setPadding(0, 0, 0, 0);
  }

  public static AlertDialog attachments(Activity activity, boolean images, IntConsumer pick, Runnable createImage) {
    Context context = activity;
    LinearLayout panel = new LinearLayout(context);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 24));
    GradientDrawable background = surface(context, R.color.ai_refresh_surface, 28, true);
    background.setCornerRadii(new float[]{dp(context,28),dp(context,28),dp(context,28),dp(context,28),0,0,0,0});
    panel.setBackground(background);
    View handle = new View(context);
    handle.setBackground(surface(context, R.color.ai_refresh_line, 2, false));
    LinearLayout.LayoutParams handleSize = new LinearLayout.LayoutParams(dp(context,32),dp(context,4));
    handleSize.gravity = Gravity.CENTER_HORIZONTAL; handleSize.bottomMargin = dp(context,10);
    panel.addView(handle,handleSize);
    AlertDialog dialog = new AlertDialog.Builder(context).create();
    LinearLayout heading = new LinearLayout(context); heading.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = text(context, R.string.ai_balanced_add_attachment, 19);
    if(android.os.Build.VERSION.SDK_INT>=28)title.setAccessibilityHeading(true);
    heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    heading.addView(icon(context,R.drawable.reading_ic_close,context.getString(R.string.ai_balanced_close),dialog::dismiss));
    panel.addView(heading);
    LinearLayout options = new LinearLayout(context);
    int[] labels = {R.string.ai_balanced_camera,R.string.ai_balanced_gallery,R.string.ai_balanced_document};
    int[] icons = {R.drawable.ai_balanced_camera,R.drawable.ai_balanced_image,R.drawable.ai_balanced_file};
    for(int i=0;i<labels.length;i++) {
      final int index=i;
      LinearLayout tile=new LinearLayout(context); tile.setOrientation(LinearLayout.VERTICAL);
      tile.setGravity(Gravity.CENTER); tile.setPadding(dp(context,4),dp(context,17),dp(context,4),dp(context,13));
      tile.setBackground(surface(context,R.color.ai_refresh_paper,18,false));
      tile.setClickable(true);tile.setFocusable(true);tile.setContentDescription(context.getString(labels[i]));
      tile.setOnClickListener(view->{dialog.dismiss();pick.accept(index);});
      ImageView image=new ImageView(context);image.setImageResource(icons[i]);
      image.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.ai_refresh_ink)));
      image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      tile.addView(image,new LinearLayout.LayoutParams(dp(context,26),dp(context,26)));
      TextView label=text(context,labels[i],13);label.setGravity(Gravity.CENTER);label.setPadding(0,dp(context,12),0,0);
      label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(label);
      tile.setMinimumHeight(dp(context,96));LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1);if(i>0)size.leftMargin=dp(context,8);
      options.addView(tile,size);
    }
    LinearLayout.LayoutParams optionSize=new LinearLayout.LayoutParams(-1,-2);optionSize.topMargin=dp(context,14);optionSize.bottomMargin=dp(context,12);panel.addView(options,optionSize);
    if(images) {
      TextView image=text(context,R.string.ai_images_create,15);image.setMinHeight(dp(context,56));image.setGravity(Gravity.CENTER_VERTICAL);
      image.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ai_balanced_image,0,0,0);image.setCompoundDrawablePadding(dp(context,12));
      image.setClickable(true);image.setFocusable(true);image.setOnClickListener(view->{dialog.dismiss();createImage.run();});panel.addView(image);
    }
    TextView details=text(context,R.string.ai_balanced_file_details,13);details.setTextColor(context.getColor(R.color.ai_refresh_muted));details.setPadding(0,0,0,dp(context,12));details.setVisibility(View.GONE);
    TextView limits=text(context,R.string.ai_balanced_file_limits,13);limits.setMinHeight(dp(context,48));limits.setGravity(Gravity.CENTER_VERTICAL);limits.setTextColor(context.getColor(R.color.ai_refresh_muted));
    limits.setClickable(true);limits.setFocusable(true);limits.setOnClickListener(view->details.setVisibility(details.getVisibility()==View.GONE?View.VISIBLE:View.GONE));
    panel.addView(limits);panel.addView(details);
    ScrollView scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.addView(panel);
    dialog.setView(scroll,0,0,0,0);return dialog;
  }

  private static TextView text(Context context,int resource,int sp) {
    TextView text=new TextView(context);text.setText(resource);text.setTextSize(sp);text.setTextColor(context.getColor(R.color.ai_refresh_ink));return text;
  }
}
