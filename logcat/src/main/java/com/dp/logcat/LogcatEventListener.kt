package com.dp.logcat

import android.support.annotation.MainThread
import android.support.annotation.WorkerThread

interface LogcatEventListener {

    @MainThread
    fun onStartEvent()

    @WorkerThread
    fun onLogEvent(log: Log)
    
    @MainThread
    fun onFailEvent()

    @MainThread
    fun onStopEvent()
}