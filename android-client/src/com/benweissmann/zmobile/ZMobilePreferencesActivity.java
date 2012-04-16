package com.benweissmann.zmobile;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class ZMobilePreferencesActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
