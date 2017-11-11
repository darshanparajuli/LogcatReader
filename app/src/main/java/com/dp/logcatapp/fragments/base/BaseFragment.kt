package com.dp.logcatapp.fragments.base

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.preference.PreferenceManager

open class BaseFragment : Fragment() {
    protected val handler: Handler = Handler(Looper.getMainLooper())
    protected var sharedPreferences: SharedPreferences? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
