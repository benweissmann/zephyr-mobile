package com.benweissmann.zmobile.components;

import java.util.List;

import com.benweissmann.zmobile.ClassListActivity;
import com.benweissmann.zmobile.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ListHeader {
    public static class Breadcrumb {
        private String label;
        private Class<? extends Activity> activity;
        
        public Breadcrumb(String label, Class<? extends Activity> activity) {
            this.label = label;
            this.activity = activity;
        }
        
        public String getLabel() {
            return label;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public Class<? extends Activity> getActivity() {
            return activity;
        }
        
        public void setActivity(Class<? extends Activity> activity) {
            this.activity = activity;
        }

        public boolean hasActivity() {
            return this.activity != null;
        }
    }
    
    public static void populate(final Activity activity, List<Breadcrumb> breadcrumbs) {
        LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout v = (LinearLayout) activity.findViewById(R.id.list_header );
        
        v.findViewById(R.id.breadcrumb_home)
         .setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 goToBreadcrumbActivity(activity, ClassListActivity.class);
             }
         });
        
        for (final Breadcrumb breadcrumb : breadcrumbs) {
            View breadcrumbView = vi.inflate(R.layout.breadcrumb, null);
            TextView label = (TextView) breadcrumbView.findViewById(R.id.breadcrumb_label);
            label.setText(breadcrumb.getLabel());
            
            if(breadcrumb.hasActivity()) {
                label.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        goToBreadcrumbActivity(activity, breadcrumb.getActivity());
                    }
                });
            }
            
            v.addView(breadcrumbView);
        }
    }
    
    private static void goToBreadcrumbActivity(Activity currentActivity,
                                               Class<? extends Activity> newActivity) {
        Intent intent = new Intent(currentActivity, newActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        currentActivity.startActivityForResult(intent, 0);
    }
}
