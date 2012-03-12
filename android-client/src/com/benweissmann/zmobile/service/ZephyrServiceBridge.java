package com.benweissmann.zmobile.service;

import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.BinderCallback;

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
    // singleton instance
    private static ZephyrServiceBridge instance = null;

    // the last binder we used
    private ZephyrBinder binder = null;

    /**
     * Returns a living ZephyrBinder, creating one if needed.
     * 
     * @param context Context used to start the ZephyrService if needed
     * @param callback Callback to pass the ZephyrBinder to.
     */
    public static void getBinder(Context context, BinderCallback callback) {
        ZephyrServiceBridge.getInstance().getMyBinder(context, callback);
    }

    // ServiceConnection that invokes a Runnable after a service connects
    private class ZephyrServiceConnection implements ServiceConnection {
        private BinderCallback callback;

        public ZephyrServiceConnection(BinderCallback callback) {
            this.callback = callback;
        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            ZephyrServiceBridge.this.binder = (ZephyrBinder) service;
            this.callback.run(ZephyrServiceBridge.this.binder);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e("ZephyrServiceBridge", "onServiceDisconnected");
            ZephyrServiceBridge.this.binder = null;
        }
    };

    // make the default constructor private
    private ZephyrServiceBridge() {
    };

    // Returns a ZephyrService Bridge, creating one if needed.
    private static ZephyrServiceBridge getInstance() {
        if (ZephyrServiceBridge.instance == null) {
            ZephyrServiceBridge.instance = new ZephyrServiceBridge();
        }

        return ZephyrServiceBridge.instance;
    }

    // creates a binder if we need one, and passes a living binder to the
    // callback
    public void getMyBinder(Context context, BinderCallback callback) {
        // if we already have a living binder, just send back that
        if ((this.binder != null) && this.binder.isBinderAlive()) {
            callback.run(this.binder);
        }

        // ensure service is running
        this.startService(context);

        // bind to the service and send back the binder
        Intent intent = new Intent(context, ZephyrService.class);
        context.bindService(intent, new ZephyrServiceConnection(callback), Context.BIND_AUTO_CREATE);
    }

    private void startService(Context context) {
        if (!ZephyrService.isRunning()) {
            Intent intent = new Intent(context, ZephyrService.class);
            context.startService(intent);
        }
    }
}
