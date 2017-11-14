package com.dp.logcat

import android.support.annotation.MainThread
import android.support.annotation.WorkerThread

interface LogcatEventListener {

    @MainThread
    fun onStartEvent()

    @MainThread
    fun onStartFailedEvent()

    @WorkerThread
    fun onPreLogEvent(log: Log)

    @MainThread
    fun onLogEvent(log: Log)

    @MainThread
    fun onLogEvents(logs: List<Log>)

    @MainThread
    fun onStopEvent()

}