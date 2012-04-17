package com.benweissmann.zmobile.auth;

import com.benweissmann.zmobile.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

public class AuthHelper {
    private static Credentials currentCredentials = null;
    
    public static Credentials getCredentials(Context ctx) throws NoStoredCredentialsException {
        if(currentCredentials != null) {
            return currentCredentials;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        
        String username = prefs.getString(getUsernameKey(ctx), null);
        String password = prefs.getString(getPasswordKey(ctx), null);
        
        if(username == null) {
            throw new NoStoredCredentialsException("No stored username");
        }
        
        if(password == null) {
            throw new NoStoredCredentialsException("No stored password");
        }
        
        currentCredentials = new Credentials(username, password);
        return currentCredentials;
    }
    
    public static void getCredentialsOrPrompt(final Activity activity, final CredentialsCallback callback) {
        try {
            callback.run(AuthHelper.getCredentials(activity));
        }
        catch(NoStoredCredentialsException e) {
            promptForCredentials(activity, callback);
        }
    }
    
    public static void promptForCredentials(final Activity activity, final CredentialsCallback callback) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View layout = inflater.inflate(R.layout.credentials_prompt,
                                               (ViewGroup) activity.findViewById(R.id.credentials_prompt_root));
                
                // pre-fill username and remember me
                EditText usernameField = (EditText) layout.findViewById(R.id.credentials_username_entry);
                CheckBox rememberMe = (CheckBox) layout.findViewById(R.id.remember_credentials);
                
                String username = loadUsername(activity);
                if(username != null) {
                    usernameField.setText(username);
                }
                
                rememberMe.setChecked(rememberMeSet(activity));
                
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
                                Credentials creds = new Credentials(username, password);
                                currentCredentials = creds;
                                callback.run(creds);
                           }
                       });
                builder.show();
            }
        });
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
    
    private static void storeCredentials(Context ctx, String username, String password, boolean rememberMe) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext()).edit();
        
        prefs.putBoolean(getRememberMeKey(ctx), rememberMe);
        prefs.putString(getUsernameKey(ctx), username);
        
        if(rememberMe) {
            prefs.putString(getPasswordKey(ctx), password);
        }
        else {
            AuthHelper.clearCredentials(ctx);
        }
        
        prefs.commit();
    }
    
    public static String getUsername() {
        if(currentCredentials != null) {
            return currentCredentials.getUsername();
        }
        else {
            return null;
        }
    }
    
    public static String loadUsername(Context ctx) {
        String username = getUsername();
        if(username != null) {
            return username;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        return prefs.getString(getUsernameKey(ctx), null);
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
