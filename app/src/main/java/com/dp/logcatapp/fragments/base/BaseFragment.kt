package com.dp.logcatapp.fragments.base

import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment

open class BaseFragment : Fragment() {
    protected val handler: Handler = Handler(Looper.getMainLooper())

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
