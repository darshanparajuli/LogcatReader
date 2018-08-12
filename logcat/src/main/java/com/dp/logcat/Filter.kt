package com.dp.logcat

interface Filter {

    fun apply(log: Log): Boolean
}