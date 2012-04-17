package com.benweissmann.zmobile.service;

import java.net.ConnectException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.benweissmann.zmobile.R;
import com.benweissmann.zmobile.auth.AuthHelper;
import com.benweissmann.zmobile.auth.Credentials;
import com.benweissmann.zmobile.auth.CredentialsCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrStatusCallback;
import com.benweissmann.zmobile.setup.NoStoredZServException;
import com.benweissmann.zmobile.setup.SetupHelper;
import com.benweissmann.zmobile.setup.ZServ;
import com.benweissmann.zmobile.setup.ZServCallback;
import com.benweissmann.zmobile.setup.ZServException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import de.timroes.axmlrpc.XMLRPCCallback;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCServerException;

public class XMLRPCHelper {
    private final static int TIMEOUT_SECONDS = 15;
    private final static int MIN_SERVER_VERSION = 1;
    private XMLRPCClient client = null;
    
    private Object[] addExtraParams(Activity activity, Object[] params) {
        Object[] newParams = new Object[params.length + 2];
        
        newParams[0] = MIN_SERVER_VERSION;
        newParams[1] = this.getToken(activity);
        for(int i = 0; i < params.length; i++) {
            newParams[i+2] = params[i];
        }
        
        return newParams;
    }
    
    private void initClientIfNeeded(Activity activity, RunnableWithError onComplete) {
        if(client == null) {
            this.initClient(activity, onComplete);
        }
        else {
            onComplete.run();
        }
    }
    
    private void initClient(Activity activity, final RunnableWithError onComplete) {
        SetupHelper.getZServOrPrompt(activity, new ZServCallback() {
            public void run(ZServ zServ) {
                try {
                    updateClient(zServ);
                    onComplete.run();
                }
                catch (NoStoredZServException e) {
                    onComplete.onError(e);
                }
            }
            
            public void onError(ZServException e) {
                onComplete.onError(e);
            }
        });
    }
    
    private void updateClient(ZServ server) throws NoStoredZServException {
        client = new XMLRPCClient(server.getURL(), server.getKeyStore(),
                                  XMLRPCClient.FLAGS_NIL |
                                  XMLRPCClient.FLAGS_SSL_IGNORE_INVALID_HOST);
    }
    
    public void callAsync(final Activity activity, final XMLRPCCallback callback, final String method, final Object... params) {
        initClientIfNeeded(activity, new RunnableWithError() {
            public void run() {
                new AsyncCall(activity, callback, method, addExtraParams(activity, params)).run();
            }
            public void onError(Exception e) {
                SetupHelper.promptForZServ(activity, new ZServCallback() {
                    public void run(ZServ zServ) {
                        try {
                            updateClient(zServ);
                        }
                        catch (NoStoredZServException e) {
                            callback.onError(0, new XMLRPCException("Could not contact server", e));
                        }
                        callAsync(activity, callback, method, params);
                    }
                    
                    public void onError(ZServException e) {
                       callback.onError(0, new XMLRPCException("Could not contact server", e));
                    }
                }, false);
            }
        });
    }
    
    private void auth(final Activity activity, final ZephyrStatusCallback callback) {
        AuthHelper.getCredentialsOrPrompt(activity, new CredentialsCallback() {
            public void run(Credentials credentials) {
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
                            storeToken(activity, (String) result);
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
                    
                    if(error.getCause() instanceof ConnectException) {
                        // couldn't connect, try restarting
                        showRestartToast(activity);
                        SetupHelper.startServer(activity, new ZServCallback() {
                            public void run(ZServ zServ) {
                                try {
                                    updateClient(zServ);
                                }
                                catch (NoStoredZServException e) {
                                    callback.onError(0, new XMLRPCException("Could not start server"));
                                }
                                AsyncCall.this.run();
                            }
                            
                            public void onError(ZServException e) {
                                callback.onError(0, new XMLRPCException("Could not start server"));
                            }
                        }, false);
                        return;
                    }
                    
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
                    params[TOKEN_ARG_INDEX] = getToken(activity);
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
    
    private static interface RunnableWithError extends Runnable {
        public void onError(Exception e);
    }
    
    private void storeToken(Context ctx, String token) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext()).edit();
        prefs.putString(ctx.getString(R.string.pref_zserv_token), token);
        prefs.commit();
    }
    
    private String getToken(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        return prefs.getString(ctx.getString(R.string.pref_zserv_token), "");
    }
    
    private void showRestartToast(final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                CharSequence text = "Could not connect to server. Restarting.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(activity, text, duration);
                toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }
}
