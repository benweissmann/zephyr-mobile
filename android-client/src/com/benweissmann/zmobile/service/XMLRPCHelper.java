package com.benweissmann.zmobile.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.benweissmann.zmobile.auth.AuthHelper;
import com.benweissmann.zmobile.auth.Credentials;
import com.benweissmann.zmobile.auth.CredentialsCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrStatusCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
        this.authToken = "";
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
    
    public void callAsync(Activity activity, XMLRPCCallback callback, String method, Object... params) {
        new AsyncCall(activity, callback, method, addExtraParams(params)).run();
    }
    
    private void auth(final Activity activity, final ZephyrStatusCallback callback) {
        AuthHelper.getCredentialsOrPrompt(activity, new CredentialsCallback() {
            public void run(Credentials credentials) {
                Log.i("XMLRPCHelper", "got credentials" + credentials);
                new AsyncCall(activity, new XMLRPCCallback() {
                    public void onServerError(long id, XMLRPCServerException error) {
                        Log.e("XMLRPCHelper", "auth got server error", error);
                        ServerError serverError = getServerError((XMLRPCServerException) error);
                        
                        if(serverError == ServerError.INVALID_AUTHENTICATION) {
                            callback.onFailure();
                        }
                        else {
                            callback.onError(error);
                        }
                    }
                    
                    public void onResponse(long id, Object result) {
                        try {
                            Log.i("XMLRPCHelper", "Got Token " + result);
                            authToken = (String) result;
                        }
                        catch(ClassCastException e) {
                            callback.onError(new MalformedServerResponseException(e));
                        }
                        
                        callback.onSuccess();
                    }
                    
                    public void onError(long id, XMLRPCException error) {
                        Log.i("XMLRPCHelper", "auth got error");
                        callback.onError(error);
                    }
                }, "authenticate", true, new Object[]{MIN_SERVER_VERSION, credentials.getUsername(), credentials.getPassword()}).run();
            }
        });
    }
    
    private class AsyncCall {
        private static final int TOKEN_ARG_INDEX = 1;
        
        private final XMLRPCCallback callback;
        private final String method;
        private final Object[] params;
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> future = null;
        private long asyncCallId;
        private Activity activity;
        private boolean noReauth;
        
        public AsyncCall(Activity activity, XMLRPCCallback callback, String method, Object[] params) {
            this(activity, callback, method, false, params);
        }
        
        public AsyncCall(Activity activity, XMLRPCCallback callback, String method, Boolean noReauth, Object[] params) {
            Log.i("XMLRPCHelper", "Creating async call " + method + " in ctx " + activity);
            this.callback = callback;
            this.method = method;
            this.params = params;
            this.activity = activity;
            this.noReauth = noReauth;
        }
        
        private void finish() {
            if(future != null) {
                future.cancel(true);
            }
        }
        
        public void run() {
            this.asyncCallId = client.callAsync(new XMLRPCCallback() {
                public void onServerError(long id, XMLRPCServerException error) {
                    finish();
                    ServerError serverError = getServerError((XMLRPCServerException) error);
                    
                    if(!noReauth && (serverError == ServerError.INVALID_AUTHENTICATION)) {
                        Log.i("XMLRPCHelper", "Got auth error");
                        onAuthError(id);
                    }
                    else {
                        callback.onServerError(id, error);
                    }
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
        
        private void onAuthError(final long errorId) {
            auth(activity, new ZephyrStatusCallback() {
                public void onSuccess() {
                    Log.i("XMLRPCHelper", "Re-auth suceeded");
                    
                    // make sure to update auth token before re-trying!
                    params[TOKEN_ARG_INDEX] = authToken;
                    run();
                }
    
                public void onFailure() {
                    Log.i("XMLRPCHelper", "Re-auth failed");
                    AuthHelper.clearCredentials(activity);
                    
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setMessage("Login information incorrect")
                                   .setCancelable(false)
                                   .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                            onAuthError(errorId);
                                       }
                                   });
                            builder.show();
                        }
                    });
                }
    
                public void onError(Exception e) {
                    Log.i("XMLRPCHelper", "Re-auth got an error");
                    callback.onError(errorId, new XMLRPCNoAuthException(e));
                }
            });
        }
    }
    
    
    
    private static ServerError getServerError(XMLRPCServerException ex) {
        switch(ex.getErrorNr()) {
        case 2: return ServerError.VERSION_MISMATCH;
        case 3: return ServerError.INVALID_AUTHENTICATION;
        default: return ServerError.UNKNOWN;
        }
    }
    
    
    public static enum ServerError {VERSION_MISMATCH, INVALID_AUTHENTICATION, UNKNOWN};
    
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
    
    public static class XMLRPCNoAuthException extends XMLRPCException {
        private static final long serialVersionUID = 1L;

        public XMLRPCNoAuthException() {
            super();
        }

        public XMLRPCNoAuthException(Exception ex) {
            super(ex);
        }

        public XMLRPCNoAuthException(String msg, Exception ex) {
            super(msg, ex);
        }

        public XMLRPCNoAuthException(String ex) {
            super(ex);
        }
    }
}
