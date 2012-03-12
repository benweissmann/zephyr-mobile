package com.benweissmann.zmobile;

import com.benweissmann.zmobile.service.ZephyrServiceBridge;
import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.BinderCallback;
import com.benweissmann.zmobile.service.callbacks.StringCallback;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ZephyrMobileClientActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Log.i("ZephyrMobileClientActivity", "starting to get binder");
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder) {
                
                final TextView t = (TextView) findViewById(R.id.main_textView1);
                binder.testMethod(new StringCallback() {
                    public void run(final String s) {
                        Log.i("ZephyrMobileClientActivity", "got callback with: " + s);
                        ZephyrMobileClientActivity.this.runOnUiThread(new Runnable() {

                            public void run() {
                                t.append(s);
                            }
                            
                        });
                    }

                    public void onError(Throwable e) {
                        Log.e("ZephyrMobileClientActivity", "got error callback", e);
                    }
                });
            }
        });
    }
}