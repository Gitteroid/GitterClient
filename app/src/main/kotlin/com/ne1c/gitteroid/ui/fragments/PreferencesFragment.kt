package com.ne1c.gitteroid.ui.fragments


import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.PreferenceFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ne1c.gitteroid.R

class PreferencesFragment : PreferenceFragment() {
    private var mNumberLoadEdit: EditTextPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_app)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        mNumberLoadEdit = findPreference("number_load_mess") as EditTextPreference

        return v
    }

    override fun onDestroy() {
        val number = Integer.parseInt(mNumberLoadEdit!!.text)

        if (number > 25) {
            mNumberLoadEdit!!.text = "25"
        } else if (number < 10) {
            mNumberLoadEdit!!.text = "10"
        }

        super.onDestroy()
    }
}
