package com.dp.logcat

interface LogFilter {

    fun filter(log: Log): Boolean
}