package com.benweissmann.zmobile;

import com.benweissmann.zmobile.service.ZephyrServiceBridge;
import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.BinderCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrStatusCallback;
import com.benweissmann.zmobile.service.objects.Zephyrgram;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.Toast;

public class ComposeActivity extends TabActivity {
    private static final String CLASS_TAB_TAG = "class";
    private static final String PERSONAL_TAB_TAG = "personal";
    
    public static final String CLASS_EXTRA = "compose_prefill_class";
    public static final String INSTANCE_EXTRA = "compose_prefill_instance";
    public static final String PERSONAL_TO_EXTRA = "compose_prefill_to";
    public static final String BODY_EXTRA = "compose_body";
    public static final String SELECT_PERSONAL_EXTRA = "compose_select_personal";
    
    private static final String FEEDBACK_CLASS = "zmobile";
    private static final String FEEDBACK_INSTANCE = "feedback";
    
    public static void launchFeedback(Activity ctx) {
        Intent intent = new Intent(ctx, ComposeActivity.class);
        intent.putExtra(CLASS_EXTRA, FEEDBACK_CLASS);
        intent.putExtra(INSTANCE_EXTRA, FEEDBACK_INSTANCE);
        ctx.startActivityForResult(intent, 0);
    }
    
    private class ComposeTabContentFactory implements TabContentFactory {
        private String cls;
        private String instance;
        private String to;
        private String body;
        
        public ComposeTabContentFactory(String cls, String instance, String to,
                                        String body) {
            this.cls = cls;
            this.instance = instance;
            this.to = to;
            this.body = body;
        }
        
        public View createTabContent(String tag) {
            LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v;
            
            if(tag.equals(CLASS_TAB_TAG)) {
                v = vi.inflate(R.layout.compose_class, null);
                
                
                final EditText clsText = (EditText) v.findViewById(R.id.compose_class_class);
                final EditText instanceText = (EditText) v.findViewById(R.id.compose_class_instance);
                final EditText bodyText = (EditText) v.findViewById(R.id.compose_class_body);
                
                if(this.cls != null) { 
                    clsText.setText(this.cls);
                }
                
                if(this.instance != null) {
                    instanceText.setText(this.instance);
                }
                
                if(this.body != null) {
                    bodyText.setText(this.body);
                }
                
                final Button sendButton = (Button) v.findViewById(R.id.compose_class_send);
                sendButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        send(new Zephyrgram(clsText.getText().toString(),
                                            instanceText.getText().toString(),
                                            bodyText.getText().toString()));
                        
                        
                    }
                });
            }
            else if(tag.equals(PERSONAL_TAB_TAG)) {
                v = vi.inflate(R.layout.compose_personal, null);
                
                final EditText toText = (EditText) v.findViewById(R.id.compose_personal_to);
                final EditText bodyText = (EditText) v.findViewById(R.id.compose_personal_body);
                    
                if(this.to != null) {
                    toText.setText(this.to);
                }
                
                if(this.body != null) {
                    bodyText.setText(this.body);
                }
                
                final Button sendButton = (Button) v.findViewById(R.id.compose_personal_send);
                sendButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        send(new Zephyrgram(toText.getText().toString(),
                                            bodyText.getText().toString()));
                        
                    }
                });
            }
            else {
                throw new RuntimeException("TabContentFactory got invalid tag");
            }
            
            return v;
        }
        
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose);
        
        // load in pre-filled fields from extras
        Bundle extras = getIntent().getExtras();
        
        String cls = null;
        String instance = null;
        String to = null;
        String body = null;
        boolean selectPersonal = false;
        
        if (extras != null) {
            cls = extras.getString(CLASS_EXTRA);
            instance = extras.getString(INSTANCE_EXTRA);
            to = extras.getString(PERSONAL_TO_EXTRA);
            body = extras.getString(BODY_EXTRA);
            selectPersonal = extras.getBoolean(SELECT_PERSONAL_EXTRA);
        }
        
        // build tabs
        TabContentFactory factory = new ComposeTabContentFactory(cls, instance, to, body);
        
        Resources res = getResources();
        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;
        
        spec = tabHost.newTabSpec(CLASS_TAB_TAG)
                      .setIndicator(getString(R.string.compose_class_tab_label),
                                    res.getDrawable(R.drawable.ic_tab_class))
                      .setContent(factory); 
        tabHost.addTab(spec);
        
        spec = tabHost.newTabSpec(PERSONAL_TAB_TAG)
                      .setIndicator(getString(R.string.compose_personal_tab_label),
                                    res.getDrawable(R.drawable.ic_tab_personal))
                      .setContent(factory);
        tabHost.addTab(spec);
        
        if(selectPersonal) {
            tabHost.setCurrentTabByTag(PERSONAL_TAB_TAG);
        }
        else {
            tabHost.setCurrentTabByTag(CLASS_TAB_TAG);
        }
    }
    
    private void send(final Zephyrgram z) {
        final Toast toast = Toast.makeText(this, getString(R.string.send_start_toast), Toast.LENGTH_SHORT);
        
        // disable send buttons to prevent double sending
        runOnUiThread(new Runnable() {
            public void run() {
                Button personalSendButton = (Button) findViewById(R.id.compose_personal_send);
                personalSendButton.setEnabled(false);
                Button classSendButton = (Button) findViewById(R.id.compose_class_send);
                classSendButton.setEnabled(false);
            }
        });
        
        
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder, final Runnable onComplete) {
                binder.send(z, new ZephyrStatusCallback() {
                    
                    public void onSuccess() {
                        onComplete.run();
                        toast.cancel();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(ComposeActivity.this,
                                           getString(R.string.send_success_toast),
                                           Toast.LENGTH_SHORT).show();
                            }
                        });
                        finish();
                    }
                    
                    public void onFailure() {
                        Log.w("ComposeActivity", "got failure");
                        
                        onComplete.run();
                        toast.cancel();
                        onSendFailure();
                    }
                    
                    public void onError(Throwable e) {
                        Log.e("ComposeActivity", "got error", e);
                        
                        onComplete.run();
                        toast.cancel();
                        onSendFailure();
                    }
                });
            }
        });
    }
    
    private void onSendFailure() {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ComposeActivity.this,
                               getString(R.string.send_fail_toast),
                               Toast.LENGTH_SHORT).show();
                
                Button personalSendButton = (Button) findViewById(R.id.compose_personal_send);
                personalSendButton.setEnabled(true);
                Button classSendButton = (Button) findViewById(R.id.compose_class_send);
                classSendButton.setEnabled(true);
            }
        });
    }
}
