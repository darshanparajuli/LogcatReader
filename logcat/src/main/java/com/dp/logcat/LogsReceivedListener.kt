package com.dp.logcat

import androidx.annotation.MainThread

interface LogsReceivedListener {

    @MainThread
    fun onReceivedLogs(logs: List<Log>)
}