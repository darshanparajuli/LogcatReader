package com.dp.logcat

import androidx.annotation.MainThread

interface LogcatEventListener {

    @MainThread
    fun onLogEvent(logs: List<Log>)
}