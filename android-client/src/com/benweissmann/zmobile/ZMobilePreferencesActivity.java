package com.benweissmann.zmobile;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.os.Bundle;

public class ZMobilePreferencesActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
    }
}
