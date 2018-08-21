package com.dp.logcat

import android.support.annotation.MainThread

interface LogcatEventListener {

    @MainThread
    fun onLogEvents(logs: List<Log>)
}