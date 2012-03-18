package com.benweissmann.zmobile;

import com.benweissmann.zmobile.service.ZephyrServiceBridge;
import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.BinderCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrCallback;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.ZephyrClass;
import com.benweissmann.zmobile.service.objects.Zephyrgram;
import com.benweissmann.zmobile.service.objects.ZephyrgramResultSet;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ZephyrMobileClientActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        getClasses();
        moreZephyrs();
    }
    
    private void getClasses() {
        Log.i("ZephyrMobileClientActivity", "starting to get binder");
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder) {
                binder.fetchClasses(new ZephyrCallback<ZephyrClass[]>() {
                    public void run(final ZephyrClass[] s) {
                        ZephyrMobileClientActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                TextView t = (TextView) findViewById(R.id.main_textView1);
                                for(ZephyrClass cls : s) {
                                    t.append(String.format("%s: %s\n", cls.getName(), cls.getUnreadCount()));
                                }
                                t.append("\n");
                            }
                        });
                    }

                    public void onError(Throwable e) {
                        Log.e("ZephyrMobileClientActivity", "got error callback in ZephyrMobileClientActivity", e);
                    }
                });
            }
        });
    }
    
    private void moreZephyrs() {
        Log.i("ZephyrMobileClientActivity", "starting to get binder");
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder) {
                binder.fetchZephyrgrams((new Query()), new ZephyrCallback<ZephyrgramResultSet>() {
                    public void run(final ZephyrgramResultSet s) {
                        ZephyrMobileClientActivity.this.updateUI(s);
                    }

                    public void onError(Throwable e) {
                        Log.e("ZephyrMobileClientActivity", "got error callback in ZephyrMobileClientActivity", e);
                    }
                });
            }
        });
    }
    
    private void moreZephyrs(final ZephyrgramResultSet prevResults) {
        Log.i("ZephyrMobileClientActivity", "starting to get binder");
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder) {
                Log.i("ZephyrMobileClientActivity", "fetching new page");
                binder.fetchNextPage(prevResults, new ZephyrCallback<ZephyrgramResultSet>() {
                    public void run(final ZephyrgramResultSet s) {
                        Log.i("ZephyrMobileClientActivity", "updating UI");
                        ZephyrMobileClientActivity.this.updateUI(s);
                    }

                    public void onError(Throwable e) {
                        Log.e("ZephyrMobileClientActivity", "got error callback in ZephyrMobileClientActivity", e);
                    }
                });
            }
        });
    }
    
    private void updateUI(final ZephyrgramResultSet s) {
        ZephyrMobileClientActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Log.i("ZephyrMobileClientActivity", "updating with " + s.getPageLength() + " results");
                
                TextView t = (TextView) findViewById(R.id.main_textView1);
                
                for(Zephyrgram zephyr : s) {
                    t.append(String.format("%s / %s / %s \n%s\n", zephyr.getCls(), zephyr.getInstance(), zephyr.getSender(), zephyr.getBody()));
                }
                
                final Button button = (Button) findViewById(R.id.button1);
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if(s.hasNextPage()) {
                            moreZephyrs(s);
                        }
                        else {
                            Log.i("ZephyrMobileClientActivity", "no more zephyrs");
                        }
                    }
                });
            }
        });
    }
}