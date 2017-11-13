package com.dp.logcat

import android.support.annotation.MainThread
import android.support.annotation.WorkerThread

interface LogcatEventListener {

    @MainThread
    fun onStartEvent()

    @MainThread
    fun onStartFailedEvent()

    @WorkerThread
    fun onLogEvent(log: Log)

    @MainThread
    fun onLogsEvent(logs: List<Log>)

    @MainThread
    fun onStopEvent()

}