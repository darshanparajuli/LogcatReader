package com.dp.logcatapp.services.logcat

import com.dp.logcat.Log

interface OnNewLogListener {

    fun onNewLog(log: Log)

}