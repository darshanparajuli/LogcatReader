package com.dp.logcat

import androidx.annotation.MainThread

interface LogcatEventListener {

    @MainThread
    fun onLogEvents(logs: List<Log>)
}