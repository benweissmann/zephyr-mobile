package com.benweissmann.zmobile.service;

import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.BinderCallback;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * A bridge to the ZephyrService. Android isn't great at letting apps interact
 * cleanly with long-running services like the ZephyrService, so this provides a
 * nice API.
 * 
 * ZephyrServiceBridge has one public method: getBinder. Call getBinder with
 * your context and a BinderCallback, and ZephyrServiceBridge will either use
 * its existing ZephyrBinder or create a new one and pass it to your callback's
 * run method.
 * 
 * @author Ben Weissmann <bsw@mit.edu>
 */
public class ZephyrServiceBridge {
    
    /**
     * Returns a living ZephyrBinder
     * 
     * @param context
     *            Context used to start the ZephyrService if needed
     * @param callback
     *            Callback to pass the ZephyrBinder to.
     */
    public static void getBinder(Activity activity, BinderCallback callback) {
        Intent intent = new Intent(activity, ZephyrService.class);
        activity.getApplicationContext().startService(intent);
        activity.bindService(intent,
                             new ZephyrServiceConnection(callback, activity),
                             Context.BIND_AUTO_CREATE);
    }
    
    // ServiceConnection that invokes a Runnable after a service connects
    private static class ZephyrServiceConnection implements ServiceConnection {
        private BinderCallback callback;
        private Context ctx;
        
        public ZephyrServiceConnection(BinderCallback callback, Context context) {
            this.callback = callback;
            this.ctx = context;
        }
        
        public void onServiceConnected(ComponentName className, IBinder service) {
            this.callback.run((ZephyrBinder) service);
            ctx.unbindService(ZephyrServiceConnection.this);
        }
        
        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e("ZephyrServiceBridge", "onServiceDisconnected");
        }
    };
}
