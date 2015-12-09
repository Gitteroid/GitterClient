package com.ne1c.developerstalk.ui.fragments;


import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ne1c.developerstalk.R;

public class PreferencesFragment extends PreferenceFragment {
    private EditTextPreference mNumberLoadEdit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_app);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        mNumberLoadEdit = (EditTextPreference) findPreference("number_load_mess");

        return v;
    }

    @Override
    public void onDestroy() {
        int number = Integer.parseInt(mNumberLoadEdit.getText());

        if (number > 25) {
            mNumberLoadEdit.setText("25");
        } else if (number < 10) {
            mNumberLoadEdit.setText("10");
        }

        super.onDestroy();
    }
}
