package com.dp.logcatapp.services

import android.app.Service
import android.content.SharedPreferences
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.setTheme

abstract class BaseService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreate() {
        setTheme()
        super.onCreate()
        getDefaultSharedPreferences().registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        getDefaultSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    }
}