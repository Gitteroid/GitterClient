package com.ne1c.gitterclient.Fragments;


import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.ne1c.gitterclient.R;

public class PreferencesFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_app);
    }
}
