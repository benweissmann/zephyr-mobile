package com.benweissmann.zmobile.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import de.timroes.axmlrpc.XMLRPCCallback;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCServerException;

public class XMLRPCHelper {
    private final XMLRPCClient client;
    private final static int TIMEOUT_SECONDS = 15;
    private final static int MIN_SERVER_VERSION = 1;
    private String authToken = null;
    
    public XMLRPCHelper(XMLRPCClient client) {
        this.client = client;
        this.authToken = "foo";
    }
    
    private Object[] addExtraParams(Object[] params) {
        Object[] newParams = new Object[params.length + 2];
        
        newParams[0] = MIN_SERVER_VERSION;
        newParams[1] = this.authToken;
        for(int i = 0; i < params.length; i++) {
            newParams[i+2] = params[i];
        }
        
        return newParams;
    }
    
    public void callAsync(XMLRPCCallback callback, String method, Object... params) {
        new AsyncCall(callback, method, addExtraParams(params)).run();
    }
    
    private class AsyncCall {
        private final XMLRPCCallback callback;
        private final String method;
        private final Object[] params;
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> future = null;
        private long asyncCallId;
        
        public AsyncCall(XMLRPCCallback callback, String method, Object[] params) {
            this.callback = callback;
            this.method = method;
            this.params = params;
        }
        
        private void finish() {
            Log.i("XMLRPCHelper", "finished");
            if(future != null) {
                future.cancel(true);
            }
        }
        
        public void run() {
            this.asyncCallId = client.callAsync(new XMLRPCCallback() {
                public void onServerError(long id, XMLRPCServerException error) {
                    finish();
                    callback.onServerError(id, error);
                }
                
                public void onResponse(long id, Object result) {
                    finish();
                    callback.onResponse(id, result);
                }
                
                public void onError(long id, XMLRPCException error) {
                    finish();
                    callback.onError(id, error);
                }
            }, this.method, this.params);
            
            // timeout
            this.future = scheduler.schedule(new Runnable() {
                public void run() {
                    Log.i("XMLRPCHelper", "Future executed");
                    client.cancel(asyncCallId);
                    callback.onError(asyncCallId, new XMLRPCTimeoutException("Request timed out"));
                }
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        
    }
    
    public static class XMLRPCTimeoutException extends XMLRPCException {
        private static final long serialVersionUID = 1L;

        public XMLRPCTimeoutException(Exception ex) {
            super(ex);
        }

        public XMLRPCTimeoutException(String msg, Exception ex) {
            super(msg, ex);
        }

        public XMLRPCTimeoutException(String ex) {
            super(ex);
        }
    }
}
