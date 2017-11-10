package com.dp.logcatapp.fragments.settings

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import com.dp.logcatapp.R

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        val TAG = SettingsFragment::class.qualifiedName
    }
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
    }

}
