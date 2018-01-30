package com.dp.logcat

interface LogcatFilter {

    fun filter(log: Log): Boolean
}