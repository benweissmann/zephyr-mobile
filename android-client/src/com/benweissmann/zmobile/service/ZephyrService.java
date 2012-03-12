package com.benweissmann.zmobile.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import com.benweissmann.zmobile.service.callbacks.StringCallback;

import de.timroes.axmlrpc.XMLRPCCallback;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCServerException;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ZephyrService extends Service {
    private static final String XML_RPC_SERVER_URL = "http://18.189.66.79:9000";
    private static boolean isRunning = false;
    private final IBinder binder = new ZephyrBinder();
    private XMLRPCClient xmlRpcClient;
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class ZephyrBinder extends Binder {
        public void testMethod(StringCallback callback) {
            ZephyrService.this.getAThing(callback);
        }
    }

    @Override
    public void onCreate() {
        ZephyrService.isRunning = true;
        try {
            this.xmlRpcClient = new XMLRPCClient(new URL(XML_RPC_SERVER_URL));
        }
        catch (MalformedURLException e) {
            Log.e("ZephyrService", "Failed to create URL for XML RPC server", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("ZephyrService", "Received start id " + startId + ": " + intent);
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        ZephyrService.isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    public void getAThing(final StringCallback callback) {
        Log.i("ZephyrService", "starting async request");
        
        XMLRPCCallback listener = new XMLRPCCallback() {
            public void onResponse(long id, Object result) {
                HashMap<String, Object> o = (HashMap<String, Object>) ((Object[]) result)[0];
                Log.i("ZephyrService", o.toString());
                Log.i("ZephyrService", o.getClass().toString());
                callback.run(o.toString());
            }
            public void onError(long id, XMLRPCException error) {
                Log.e("ZephyrService", "XMLRPC onError", error);
                callback.onError(error);
            }
            public void onServerError(long id, XMLRPCServerException error) {
                Log.e("ZephyrService", "XMLRPC onServerError", error);
                callback.onError(error);
            }
        };
        xmlRpcClient.callAsync(listener, "messenger.get");
    }

    public static boolean isRunning() {
        return ZephyrService.isRunning;
    }
}
