package com.benweissmann.zmobile.auth;

import com.benweissmann.zmobile.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

public class AuthHelper {
    private static String username = null;
    
    public static Credentials getCredentials(Context ctx) throws NoStoredCredentialsException {
        Log.i("AuthHelper", "context: "+ctx);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        
        String username = prefs.getString(getUsernameKey(ctx), null);
        String password = prefs.getString(getPasswordKey(ctx), null);
        
        if(username == null) {
            throw new NoStoredCredentialsException("No stored username");
        }
        
        if(password == null) {
            throw new NoStoredCredentialsException("No stored password");
        }
        
        AuthHelper.username = username;
        
        return new Credentials(username, password);
    }
    
    public static void getCredentialsOrPrompt(final Activity activity, final CredentialsCallback callback) {
        try {
            callback.run(AuthHelper.getCredentials(activity));
        }
        catch(NoStoredCredentialsException e) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View layout = inflater.inflate(R.layout.credentials_prompt,
                                                   (ViewGroup) activity.findViewById(R.id.credentials_prompt_root));
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setView(layout)
                           .setTitle(R.string.credentials_prompt_title)
                           .setCancelable(false)
                           .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                    EditText usernameField = (EditText) layout.findViewById(R.id.credentials_username_entry);
                                    EditText passwordField = (EditText) layout.findViewById(R.id.credentials_password_entry);
                                    CheckBox rememberMe = (CheckBox) layout.findViewById(R.id.remember_credentials);
                                    
                                    String username = usernameField.getText().toString();
                                    String password = passwordField.getText().toString();
                                    
                                    storeCredentials(activity, username, password, rememberMe.isChecked());
                                    
                                    dialog.dismiss();
                                    callback.run(new Credentials(username, password));
                               }
                           });
                    builder.show();
                }
            });
        }
    }
    
    public static void clearCredentials(Context ctx) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext()).edit();
        
        prefs.remove(getUsernameKey(ctx));
        prefs.remove(getPasswordKey(ctx));
        
        prefs.commit();
    }
    
    public static boolean rememberMeSet(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        return prefs.getBoolean(getRememberMeKey(ctx), false);
    }
    
    public static void storeCredentials(Context ctx, String username, String password, boolean rememberMe) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext()).edit();
        
        prefs.putBoolean(getRememberMeKey(ctx), rememberMe);
        
        if(rememberMe) {
            prefs.putString(getUsernameKey(ctx), username);
            prefs.putString(getPasswordKey(ctx), password);
        }
        else {
            AuthHelper.clearCredentials(ctx);
        }
        
        prefs.commit();
    }
    
    public static String getUsername() {
        return AuthHelper.username;
    }
    
    private static String getUsernameKey(Context ctx) {
        return ctx.getString(R.string.pref_kerberos_username);
    }
    
    private static String getPasswordKey(Context ctx) {
        return ctx.getString(R.string.pref_kerberos_password);
    }
    
    private static String getRememberMeKey(Context ctx) {
        return ctx.getString(R.string.pref_remember_me);
    }
}
