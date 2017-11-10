package com.dp.logcatapp.fragments.base

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager

open class BaseFragment : Fragment() {

    protected var parentActivity: AppCompatActivity? = null
        private set
    protected val handler: Handler = Handler(Looper.getMainLooper())
    protected var sharedPreferences: SharedPreferences? = null
        private set

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        parentActivity = context as AppCompatActivity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onDetach() {
        super.onDetach()
        parentActivity = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
