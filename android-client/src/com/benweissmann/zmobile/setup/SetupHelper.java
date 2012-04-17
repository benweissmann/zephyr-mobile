package com.benweissmann.zmobile.setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.benweissmann.zmobile.R;
import com.benweissmann.zmobile.auth.AuthHelper;
import com.benweissmann.zmobile.auth.Credentials;
import com.benweissmann.zmobile.auth.CredentialsCallback;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SetupHelper {
    private static final String BOOTSTRAP_COMMAND = "/mit/zmobile/bin/zserv-bootstrap";
    
    public static ZServ getZServ(Context ctx) throws NoStoredZServException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        
        String server = prefs.getString(getServerKey(ctx), null);
        int port = prefs.getInt(getPortKey(ctx), -1);
        String keyStore = prefs.getString(getKeyStoreKey(ctx), null);
        
        if(server == null) {
            throw new NoStoredZServException("No stored server");
        }
        
        if(port < 0) {
            throw new NoStoredZServException("No stored port");
        }
        
        if(keyStore == null) {
            throw new NoStoredZServException("No stored keystore");
        }
        
        return new ZServ(server, port, keyStore);
    }
    
    public static void getZServOrPrompt(final Activity activity, final ZServCallback callback) {
        try {
            callback.run(SetupHelper.getZServ(activity));
        }
        catch(NoStoredZServException e) {
            promptForZServ(activity, callback, false);
        }
    }
    
    public static void promptForZServ(final Activity activity, final ZServCallback callback, final boolean reset) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View layout = inflater.inflate(R.layout.zserv_prompt,
                                               (ViewGroup) activity.findViewById(R.id.zserv_prompt_root));
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setView(layout)
                       .setTitle(R.string.zserv_prompt_title)
                       .setCancelable(false)
                       .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                EditText hostNameField = (EditText) layout.findViewById(R.id.zserv_dialup_entry);
                                String hostName = hostNameField.getText().toString();
                                SharedPreferences.Editor prefs =
                                        PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).edit();
                                prefs.putString(getServerKey(activity), hostName);
                                prefs.commit();
                                
                                dialog.dismiss();
                                
                                startServer(activity, callback, reset);
                           }
                       });
                builder.show();
            }
        });
    }
    
    public static void startServer(final Activity activity, final ZServCallback callback, final boolean reset) {
        AuthHelper.getCredentialsOrPrompt(activity, new CredentialsCallback() {
            public void run(Credentials credentials) {
                try {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
                    String server = prefs.getString(getServerKey(activity), null);
                    if(server == null) {
                        throw new NoStoredZServException("No server set");
                    }
                    JSch jsch = new JSch();
                    jsch.setKnownHosts(activity.getResources().openRawResource(R.raw.known_hosts));
                    final Session session = jsch.getSession(credentials.getUsername(), server);
                    session.setPassword(credentials.getPassword());
                    session.connect();
                    Channel channel=session.openChannel("exec");
                    BufferedWriter toServer = new BufferedWriter(new OutputStreamWriter(channel.getOutputStream()));
                    BufferedReader fromServer = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                    ((ChannelExec)channel).setCommand(BOOTSTRAP_COMMAND);
                    channel.connect();
                    int port;
                    String cert;
                    
                    try {
                        Log.i("SetupHelper", "reading hello");
                        String hello = fromServer.readLine();
                        Log.i("SetupHelper", "got hello: " + hello);
                        if(!hello.contains("!ZSERV HELLO")) {
                            throw new ZServException("Didn't get hello");
                        }
                        
                        if(reset) {
                            toServer.write("RESET\n");
                            toServer.flush();
                            String resetResponse = fromServer.readLine();
                            if(!resetResponse.contains("!ZSERV RESET DONE")) {
                                throw new ZServException("Reset didn't get a done response");
                            }
                        }
                        
                        Log.i("SetupHelper", "writing start");
                        toServer.write("START\n");
                        toServer.flush();
                        Log.i("SetupHelper", "reading start response");
                        String startResponse = fromServer.readLine();
                        Log.i("SetupHelper", "got start response: " + startResponse);
                        if(!startResponse.contains("!ZSERV START SUCCESS")) {
                            throw new ZServException("Start didn't get a success response");
                        }
                        
                        toServer.write("GET PORT\n");
                        toServer.flush();
                        String portResponse = fromServer.readLine();
                        if(!portResponse.contains("!ZSERV PORT")) {
                            throw new ZServException("GET PORT failed");
                        }
                        
                        port = Integer.parseInt(portResponse.split(" ")[2]);
                        
                        toServer.write("GET CERT PEM\n");
                        toServer.flush();
                        String certResponse = fromServer.readLine();
                        if(!certResponse.contains("!ZSERV CERT BEGIN")) {
                            throw new ZServException("GET CERT failed");
                        }
                        
                        cert = "";
                        while(true) {
                            String certPart = fromServer.readLine();
                            if(certPart.contains("!ZSERV CERT END")) {
                                break;
                            }
                            else {
                                cert += certPart;
                                cert += "\n";
                            }
                        }
                        
                        toServer.write("BYE\n");
                    }
                    finally {
                        toServer.close();
                        fromServer.close();
                        channel.disconnect();
                        session.disconnect();
                    }
                    
                    SharedPreferences.Editor prefsEditor =
                            PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).edit();
                    prefsEditor.putInt(getPortKey(activity), port);
                    prefsEditor.putString(getKeyStoreKey(activity), cert);
                    prefsEditor.commit();
                    
                    callback.run(new ZServ(server, port, cert));
                }
                catch(NoStoredZServException e) {
                    alertRetry(activity);
                    Log.e("SetupHelper", "startServer got no zserv exception", e);
                    SetupHelper.promptForZServ(activity, callback, reset);
                }
                catch(JSchException e) {
                    alertRetry(activity);
                    Log.e("SetupHelper", "startServer got jsch exception", e);
                    // this could mean we have an incorrect password. re-ask
                    // for password.
                    AuthHelper.promptForCredentials(activity, new CredentialsCallback() {
                        public void run(Credentials credentials) {
                            SetupHelper.promptForZServ(activity, callback, reset);
                        }
                    });
                    
                }
                catch(IOException e) {
                    alertRetry(activity);
                    Log.e("SetupHelper", "got IOException", e);
                    SetupHelper.promptForZServ(activity, callback, reset);
                }
                catch(ZServException e) {
                    if(reset) {
                        Log.e("SetupHelper", "got zserv exception while resetting", e);
                        // we already tried resetting, we're totally fucked
                        callback.onError(new ZServException("Reset failed, aborting", e));
                    }
                    // let's try that again with a reset...
                    alertRetry(activity);
                    Log.e("SetupHelper", "got zserv exception, going to try resetting", e);
                    SetupHelper.startServer(activity, callback, true);
                }
            }
        });

    }
    
    private static void alertRetry(final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                CharSequence text = "Could not connect to server. Retrying.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(activity, text, duration);
                toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }
    
    private static String getServerKey(Context ctx) {
        return ctx.getString(R.string.pref_zserv_server);
    }
    
    private static String getPortKey(Context ctx) {
        return ctx.getString(R.string.pref_zserv_port);
    }
    
    private static String getKeyStoreKey(Context ctx) {
        return ctx.getString(R.string.pref_zserv_keystore);
    }
}
