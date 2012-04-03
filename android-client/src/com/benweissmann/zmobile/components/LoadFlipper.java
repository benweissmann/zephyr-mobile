package com.benweissmann.zmobile.components;

import android.app.Activity;
import android.widget.ViewFlipper;

import com.benweissmann.zmobile.R;

public class LoadFlipper {
    private static final int VIEW_FLIPPER_LOADING_INDEX = 0;
    private static final int VIEW_FLIPPER_CONTENT_INDEX = 1;
    
    public static void flipToLoader(Activity activity) {
        flipTo(VIEW_FLIPPER_LOADING_INDEX, activity);
    }
    
    public static void flipToContent(Activity activity) {
        flipTo(VIEW_FLIPPER_CONTENT_INDEX, activity);
    }
    
    private static void flipTo(final int index, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                ViewFlipper flipper = (ViewFlipper) activity.findViewById(R.id.list_flipper);
                flipper.setDisplayedChild(index);
            }
        });
    }
}
